package com.example.stuid.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.ColumnCreateCallback;
import com.example.stuid.api.ColumnsCallback;
import com.example.stuid.api.EmployeeCallback;
import com.example.stuid.api.EmployeesCallback;
import com.example.stuid.api.ParticipantsCallback;
import com.example.stuid.api.ProfileUpdateCallback;
import com.example.stuid.api.TaskCreateCallback;
import com.example.stuid.api.TaskDeleteCallback;
import com.example.stuid.api.TasksCallback;
import com.example.stuid.models.ColumnsAdapter;
import com.example.stuid.models.Employee;
import com.example.stuid.models.Task;
import com.example.stuid.models.TaskAdapter;
import com.example.stuid.models.TaskColumn;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TasksFragment extends Fragment {
    private int projectId;
    private int projectCreatorId;
    private List<Employee> allEmployees = new ArrayList<>();
    private List<Employee> projectParticipants = new ArrayList<>();
    private List<Employee> selectedAssignees = new ArrayList<>();
    private SharedPreferences prefs;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ApiClient apiClient;
    private List<Task> tasks = new ArrayList<>();
    private int currentUserId;
    private boolean canEditTask = false;
    private List<Employee> currentTaskAssignees = new ArrayList<>();

    private RecyclerView columnsRecyclerView;
    private ColumnsAdapter columnsAdapter;
    private List<TaskColumn> columns = new ArrayList<>();
    private static Task draggedTask;
    private boolean isPublicProject;

    public static void setDraggedTask(Task task) {
        draggedTask = task;
    }

    public static Task getDraggedTask() {
        return draggedTask;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("employee_id", -1);
        isPublicProject = getArguments().getBoolean("isPublicProject");

        if (getArguments() != null) {
            projectId = getArguments().getInt("projectId", -1);
            projectCreatorId = getArguments().getInt("creatorId", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // Инициализация элементов UI
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        apiClient = new ApiClient();

        // Настройка RecyclerView для колонок
        columnsRecyclerView = view.findViewById(R.id.rvColumns);
        columnsRecyclerView.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false));

        columnsAdapter = new ColumnsAdapter(
                columns,
                projectParticipants,
                currentUserId,
                apiClient,
                prefs.getString("jwt_token", null),
                this::onTaskClicked,
                this::onAddTaskClicked,
                (task, newChapterId) -> updateTaskChapter(task.getId(), newChapterId),
                this);
        columnsRecyclerView.setAdapter(columnsAdapter);

        // Кнопка добавления колонки
        view.findViewById(R.id.btnAddColumn).setOnClickListener(v -> showEditColumnDialog(null));

        // Обновление данных
        swipeRefresh.setOnRefreshListener(this::refreshData);
        loadInitialData();

        return view;
    }

    private void loadInitialData() {
        progressBar.setVisibility(View.VISIBLE);

        // Загрузка участников проекта
        loadProjectParticipants(() -> {
            // После загрузки участников загружаем колонки и задачи
            loadColumns();
            loadAllEmployee();
        });
    }

    private void refreshData() {
        loadProjectParticipants(() -> {
            loadTasks();
            loadAllEmployee();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void loadProjectParticipants(Runnable onComplete) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        apiClient.getProjectParticipants(token, projectId, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> participants) {
                requireActivity().runOnUiThread(() -> {
                    projectParticipants.clear();
                    projectParticipants.addAll(participants);
                    if (onComplete != null) onComplete.run();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Ошибка загрузки участников: " + error,
                            Toast.LENGTH_SHORT).show();
                    if (onComplete != null) onComplete.run();
                });
            }
        });
    }

    private void loadAllEmployee() {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        apiClient.getEmployees(token, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> employees) {
                requireActivity().runOnUiThread(() -> {
                    allEmployees.clear();
                    allEmployees.addAll(employees);
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), "Ошибка загрузки сотрудников", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadColumns() {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        apiClient.getColumnsForProject(token, projectId, new ColumnsCallback() {
            @Override
            public void onSuccess(List<TaskColumn> serverColumns) {
                requireActivity().runOnUiThread(() -> {
                    columns.clear();
                    columns.addAll(serverColumns);
                    columnsAdapter.notifyDataSetChanged();
                    loadTasks();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ошибка загрузки колонок: " + error, Toast.LENGTH_SHORT).show();
                    loadTasks();
                });
            }
        });
    }

    private void loadTasks() {
        String token = prefs.getString("jwt_token", null);
        if (token == null || projectId == -1) return;

        apiClient.getProjectTasks(token, projectId, new TasksCallback() {
            @Override
            public void onSuccess(List<Task> loadedTasks) {
                requireActivity().runOnUiThread(() -> {
                    // Очищаем задачи во всех колонках
                    for (TaskColumn column : columns) {
                        column.getTasks().clear();
                    }

                    // Распределяем задачи по колонкам
                    Map<Integer, List<Task>> tasksByColumn = new HashMap<>();
                    for (Task task : loadedTasks) {
                        tasksByColumn.computeIfAbsent(task.getChapter(), k -> new ArrayList<>()).add(task);
                    }

                    // Сортируем каждую группу по position
                    for (TaskColumn column : columns) {
                        List<Task> columnTasks = tasksByColumn.getOrDefault(column.getId(), new ArrayList<>());
                        Collections.sort(columnTasks, Comparator.comparingInt(Task::getPosition));
                        column.setTasks(columnTasks);
                    }

                    columnsAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),"Ошибка загрузки задач: " + error,Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onTaskClicked(Task task) {
        checkEditPermission(task, () -> {
            if (canEditTask) {
                draggedTask = task;
                showTaskDialog(task);
            } else {
                Toast.makeText(requireContext(), "Нет прав на редактирование", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onAddTaskClicked(int columnId) { showAddTaskDialog(columnId, isPublicProject); }

    public void showEditColumnDialog(TaskColumn columnToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(columnToEdit == null ? "Создать колонку" : "Редактировать колонку");

        // Создаем контейнер с TextInputLayout
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16,
                getResources().getDisplayMetrics()
        );
        container.setPadding(padding, padding, padding, 0);

        // Создаём TextInputLayout
        TextInputLayout textInputLayout = new TextInputLayout(requireContext());
        textInputLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textInputLayout.setErrorEnabled(true);
        textInputLayout.setHint("Название колонки");

        // EditText
        final TextInputEditText input = new TextInputEditText(textInputLayout.getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextSize(16);
        input.setPadding(20, 20, 20, 20);
        input.setText(columnToEdit != null ? columnToEdit.getName() : "");

        textInputLayout.addView(input);
        container.addView(textInputLayout);

        builder.setView(container);

        // Добавляем кнопки ДО создания диалога
        if (columnToEdit != null) {
            builder.setNegativeButton("Удалить", (dialog1, which) -> {
                showDeleteColumnConfirmationDialog(columnToEdit);
            });
        }

        builder.setNeutralButton("Отмена", null);
        builder.setPositiveButton(columnToEdit == null ? "Создать" : "Сохранить", null);

        // Теперь создаём диалог
        AlertDialog dialog = builder.create();

        // Назначаем обработчик только после создания диалога
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String name = input.getText().toString().trim();
                textInputLayout.setError(null); // очищаем ошибку

                if (name.isEmpty()) {
                    textInputLayout.setError("Введите название колонки");
                    return;
                }

                if (!isValidColumnName(name)) {
                    textInputLayout.setError("Название должно начинаться с заглавной буквы и содержать только русские символы");
                    return;
                }

                if (columnToEdit == null) {
                    createColumn(name);
                } else {
                    updateColumn(columnToEdit.getId(), name);
                }

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private boolean isValidColumnName(String name) {
        if (name == null || name.trim().isEmpty()) return false;

        String[] words = name.trim().split(" ");
        if (words.length == 0) return false;

        String firstWord = words[0];
        if (firstWord.isEmpty() || !Character.isUpperCase(firstWord.codePointAt(0))) {
            return false;
        }

        // Проверяем, что текст содержит только русские буквы и пробелы
        return name.matches("^[А-ЯЁ][а-яё\\s\\-]*$");
    }

    private void showDeleteColumnConfirmationDialog(TaskColumn column) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Подтвердите удаление")
                .setMessage("Вы уверены, что хотите удалить колонку \"" + column.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteColumn(column))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteColumn(TaskColumn column) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        progressBar.setVisibility(View.VISIBLE);

        apiClient.deleteColumn(token, column.getId(), new TaskDeleteCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    columns.remove(column);
                    columnsAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Колонка удалена", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ошибка удаления: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateColumn(int columnId, String newName) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Id", columnId);
            jsonBody.put("Name", newName);
            jsonBody.put("ProjectId", projectId);
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Ошибка формирования данных", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        apiClient.updateColumn(token, columnId, jsonBody, new ColumnCreateCallback() {
            @Override
            public void onSuccess(TaskColumn updatedColumn) {
                requireActivity().runOnUiThread(() -> {
                    for (int i = 0; i < columns.size(); i++) {
                        if (columns.get(i).getId() == updatedColumn.getId()) {
                            columns.set(i, updatedColumn);
                            columnsAdapter.notifyItemChanged(i);
                            break;
                        }
                    }

                    loadTasks();

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Колонка обновлена", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ошибка обновления: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void createColumn(String name) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            Toast.makeText(requireContext(), "Не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        // Убедитесь, что projectId доступен в этом контексте
        if (projectId == -1) {
            Toast.makeText(requireContext(), "Неверный проект", Toast.LENGTH_SHORT).show();
            return;
        }

        apiClient.createColumn(token, name, projectId, new ColumnCreateCallback() {
            @Override
            public void onSuccess(TaskColumn column) {
                requireActivity().runOnUiThread(() -> {
                    columns.add(column);
                    columnsAdapter.notifyItemInserted(columns.size() - 1);
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Ошибка создания колонки: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showAddTaskDialog(int columnId, boolean isPublicProject) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Новая задача");
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etTaskName);
        EditText etDescription = dialogView.findViewById(R.id.etTaskDescription);
        AutoCompleteTextView actvAssigneeSearch = dialogView.findViewById(R.id.actvAssigneeSearch);
        LinearLayout llSelectedAssignees = dialogView.findViewById(R.id.llSelectedAssignees);
        TextInputLayout tilTaskName = dialogView.findViewById(R.id.tilTaskName); // должен быть в разметке

        selectedAssignees.clear();
        llSelectedAssignees.removeAllViews();

        List<Employee> assignees = isPublicProject ? allEmployees : projectParticipants;

        ArrayAdapter<Employee> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, assignees);

        actvAssigneeSearch.setAdapter(adapter);

        actvAssigneeSearch.setOnItemClickListener((parent, view, position, id) -> {
            Employee selected = adapter.getItem(position);
            if (selected != null && !selectedAssignees.contains(selected)) {
                selectedAssignees.add(selected);
                addAssigneeView(selected, llSelectedAssignees, true);
                actvAssigneeSearch.setText("");
            }
        });

        builder.setPositiveButton("Создать", null); // Установим обработчик ниже
        builder.setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            int creatorId = prefs.getInt("employee_id", -1);

            tilTaskName.setError(null);
            boolean isValid = true;

            // Проверка имени
            if (name.isEmpty()) {
                tilTaskName.setError("Введите название задачи");
                isValid = false;
            } else if (!isValidTaskName(name)) {
                tilTaskName.setError("Название должно начинаться с заглавной буквы и содержать только русские символы");
                isValid = false;
            }

            // Проверка ответственных (только для приватных проектов)
            if (!isPublicProject && selectedAssignees.isEmpty()) {
                isValid = false;
            }

            if (!isValid) return;

            // Если всё ок, отправляем запрос
            List<Integer> assigneeIds = new ArrayList<>();
            for (Employee assignee : selectedAssignees) {
                assigneeIds.add(assignee.getEmployeeId());
            }

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("Name", name);
                jsonBody.put("Description", description);
                jsonBody.put("ProjectId", projectId);
                jsonBody.put("ChapterId", columnId);
                jsonBody.put("CreatorId", creatorId);
                jsonBody.put("AssigneeIds", new JSONArray(assigneeIds));
            } catch (JSONException e) {
                Toast.makeText(requireContext(), "Ошибка формирования запроса", Toast.LENGTH_SHORT).show();
                return;
            }

            String token = prefs.getString("jwt_token", null);
            if (token == null) return;

            progressBar.setVisibility(View.VISIBLE);
            apiClient.createTask(token, jsonBody, new TaskCreateCallback() {
                @Override
                public void onSuccess(Task task) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        for (TaskColumn column : columns) {
                            if (column.getId() == task.getChapter()) {
                                column.getTasks().add(task);
                                columnsAdapter.notifyItemChanged(columns.indexOf(column));
                                break;
                            }
                        }
                        selectedAssignees.clear();
                        Toast.makeText(requireContext(), "Задача создана", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }

                @Override
                public void onFailure(String error) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    private boolean isValidTaskName(String name) {
        if (name == null || name.trim().isEmpty()) return false;

        // Проверяем, что первое слово начинается с заглавной буквы
        String[] words = name.trim().split(" ");
        if (words.length == 0) return false;
        String firstWord = words[0];
        if (firstWord.isEmpty() || !Character.isUpperCase(firstWord.codePointAt(0))) {
            return false;
        }

        // Проверяем, что текст на русском (только кириллица + пробелы)
        return name.matches("^[А-ЯЁ][а-яё\\s\\-]*$");
    }

    private void addAssigneeView(Employee employee, LinearLayout container, boolean removable) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_selected_employee, container, false);

        TextView tvName = view.findViewById(R.id.tvEmployeeName);
        ImageButton btnRemove = view.findViewById(R.id.btnRemoveEmployee);

        tvName.setText(employee.getFullName());
        btnRemove.setVisibility(removable ? View.VISIBLE : View.GONE);

        if (removable) {
            btnRemove.setOnClickListener(v -> {
                container.removeView(view);
                selectedAssignees.remove(employee);
            });
        }

        container.addView(view);
    }

    private void checkEditPermission(Task task, Runnable onSuccess) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;
        // Проверяем, является ли пользователь создателем проекта
        boolean isProjectCreator = currentUserId == projectCreatorId;
        // Проверяем, является ли текущий пользователь создателем задачи
        boolean isTaskCreator  = task.getCreatorId() == currentUserId;
        if (isProjectCreator || isTaskCreator) {
            // Если пользователь создатель - сразу разрешаем редактирование
            requireActivity().runOnUiThread(() -> {
                canEditTask = true;
                onSuccess.run();
            });
            return;
        }
        // Если не создатель - проверяем, является ли ответственным
        apiClient.getTaskAssignees(token, task.getId(), new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> assignees) {
                requireActivity().runOnUiThread(() -> {
                    canEditTask = false;
                    for (Employee assignee : assignees) {
                        if (assignee.getEmployeeId() == currentUserId) {
                            canEditTask = true;
                            break;
                        }
                    }
                    onSuccess.run();
                });
            }
            @Override
            public void onFailure(String error) {
                Log.e("TasksFragment", "Error loading assignees: " + error);
                requireActivity().runOnUiThread(() -> {
                    canEditTask = false;
                    onSuccess.run();
                });
            }
        });
    }

    private void setupAssigneeSearch(AutoCompleteTextView actvAssigneeSearch, LinearLayout llSelectedAssignees, boolean isPublicProject) {
        List<Employee> assignees = isPublicProject ? allEmployees : projectParticipants;

        ArrayAdapter<Employee> adapter = new ArrayAdapter<Employee>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, assignees) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Employee employee = (Employee) getItem(position);
                if (employee != null) {
                    ((TextView) view).setText(employee.getFullName());
                }
                return view;
            }
        };

        actvAssigneeSearch.setAdapter(adapter);

        actvAssigneeSearch.setOnItemClickListener((parent, view, position, id) -> {
            Employee selected = adapter.getItem(position);
            if (selected != null && !selectedAssignees.contains(selected)) {
                selectedAssignees.add(selected);
                addAssigneeView(selected, llSelectedAssignees, true);
                actvAssigneeSearch.setText("");
            }
        });
    }

    private void showTaskDialog(Task task) {
        // Сначала проверяем права
        checkEditPermission(task, () -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(task.getName());

            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_task, null);
            builder.setView(dialogView);

            EditText etName = dialogView.findViewById(R.id.etTaskName);
            EditText etDescription = dialogView.findViewById(R.id.etTaskDescription);
            AutoCompleteTextView actvAssigneeSearch = dialogView.findViewById(R.id.actvAssigneeSearch);
            LinearLayout llSelectedAssignees = dialogView.findViewById(R.id.llSelectedAssignees);

            TextInputLayout tilTaskName = dialogView.findViewById(R.id.tilTaskName);

            // Заполняем поля
            etName.setText(task.getName());
            etDescription.setText(task.getDescription());

            // Настраиваем доступность
            etName.setEnabled(canEditTask);
            etDescription.setEnabled(canEditTask);
            actvAssigneeSearch.setEnabled(canEditTask);

            // Показываем создателя задачи
            TextView tvCreator = dialogView.findViewById(R.id.tvCreator);
            if (tvCreator != null) {
                tvCreator.setText("Создатель: загрузка...");
                getCreatorName(task.getCreatorId(), name -> {
                    tvCreator.setText("Создатель: " + name);
                });
            }

            // Загружаем текущих ответственных
            loadTaskAssignees(task.getId(), llSelectedAssignees, () -> {
                if (canEditTask) {
                    setupAssigneeSearch(actvAssigneeSearch, llSelectedAssignees, isPublicProject);
                }
            });

            builder.setNeutralButton("Сохранить", null);
            builder.setPositiveButton("Закрыть", null);

            if (canEditTask && (currentUserId == task.getCreatorId() || currentUserId == projectCreatorId)) {
                builder.setNegativeButton("Удалить", (dialog, which) -> {
                    showDeleteConfirmationDialog(task);
                });
            }

            AlertDialog dialog = builder.create();
            dialog.show();

            // Переопределяем обработчик положительной кнопки
            Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            neutralButton.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String description = etDescription.getText().toString().trim();

                tilTaskName.setError(null); // очистка предыдущей ошибки

                boolean isValid = true;

                // Валидация названия
                if (name.isEmpty()) {
                    tilTaskName.setError("Введите название задачи");
                    isValid = false;
                } else if (!isValidTaskName(name)) {
                    tilTaskName.setError("Название должно начинаться с заглавной буквы и содержать только русские символы");
                    isValid = false;
                }

                if (!isValid) return;

                // Если всё ок — сохраняем изменения
                updateTask(task.getId(), name, description);
                dialog.dismiss();
            });
        });
    }

    private void getCreatorName(int creatorId, Consumer<String> callback) {
        // Сначала проверяем локальные данные
        for (Employee employee : projectParticipants) {
            if (employee.getEmployeeId() == creatorId) {
                callback.accept(employee.getFullName());
                return;
            }
        }

        if (creatorId == currentUserId) {
            callback.accept("Вы");
            return;
        }

        // Если не нашли локально, запрашиваем с сервера
        String token = prefs.getString("jwt_token", null);
        if (token != null) {
            apiClient.getEmployeeInfo(token, creatorId, new EmployeeCallback() {
                @Override
                public void onSuccess(Employee employee) {
                    callback.accept(employee.getFullName());
                }

                @Override
                public void onFailure(String error) {
                    callback.accept("Неизвестный");
                }
            });
        } else {
            callback.accept("Неизвестный");
        }
    }

    private void loadTaskAssignees(int taskId, LinearLayout container, Runnable onComplete) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            Toast.makeText(requireContext(), "Требуется авторизация", Toast.LENGTH_SHORT).show();
            return;
        }

        container.removeAllViews();
        ProgressBar progressBar = new ProgressBar(requireContext());
        container.addView(progressBar);

        apiClient.getTaskAssignees(token, taskId, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> assignees) {
                requireActivity().runOnUiThread(() -> {
                    container.removeAllViews();
                    currentTaskAssignees.clear();
                    currentTaskAssignees.addAll(assignees);
                    selectedAssignees.clear();
                    selectedAssignees.addAll(assignees);

                    if (assignees.isEmpty()) {
                        TextView noAssignees = new TextView(requireContext());
                        noAssignees.setText("Нет ответственных");
                        container.addView(noAssignees);
                    } else {
                        for (Employee assignee : assignees) {
                            addAssigneeView(assignee, container, canEditTask);
                        }
                    }

                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    container.removeAllViews();
                    TextView errorView = new TextView(requireContext());
                    errorView.setText("Ошибка загрузки ответственных");
                    container.addView(errorView);

                    Log.e("TasksFragment", "Error loading assignees: " + error);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            }
        });
    }

    private void updateTask(int taskId, String name, String description) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        List<Integer> assigneeIds = new ArrayList<>();
        for (Employee assignee : selectedAssignees) {
            assigneeIds.add(assignee.getEmployeeId());
        }

        // Находим текущую колонку задачи
        int currentChapterId = 1; // значение по умолчанию
        for (TaskColumn column : columns) {
            for (Task task : column.getTasks()) {
                if (task.getId() == taskId) {
                    currentChapterId = column.getId();
                    break;
                }
            }
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Name", name);
            jsonBody.put("Description", description);
            jsonBody.put("ChapterId", currentChapterId); // Используем текущий ChapterId
            jsonBody.put("AssigneeIds", new JSONArray(assigneeIds));
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Ошибка формирования запроса",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        apiClient.updateTask(token, taskId, jsonBody, new TaskCreateCallback() {
            @Override
            public void onSuccess(Task updatedTask) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    selectedAssignees.clear();

                    // Обновляем задачу в списке
                    for (TaskColumn column : columns) {
                        for (int i = 0; i < column.getTasks().size(); i++) {
                            if (column.getTasks().get(i).getId() == updatedTask.getId()) {
                                column.getTasks().set(i, updatedTask);
                                break;
                            }
                        }
                    }

                    // Обновляем список задач
                    loadTasks();
                    Toast.makeText(requireContext(), "Задача обновлена",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ошибка: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showDeleteConfirmationDialog(Task task) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Подтверждение удаления")
                .setMessage("Вы уверены, что хотите удалить эту задачу?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteTask(task);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteTask(Task task) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        progressBar.setVisibility(View.VISIBLE);

        apiClient.deleteTask(token, task.getId(), new TaskDeleteCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    // Удаляем задачу из адаптера
                    columnsAdapter.removeTask(task);

                    Toast.makeText(requireContext(), "Задача удалена", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateTaskChapter(int taskId, int chapterId) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            showToast("Authorization required");
            return;
        }

        apiClient.updateTaskChapter(token, taskId, chapterId, new ProfileUpdateCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    showToast("Chapter updated successfully");
                    Log.e("Server", "Chapter updated successfully");
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    showToast("Update failed: " + error);
                    Log.e("Server","Update failed: " + error);
                });
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void sendNewOrderToServer(int projectId, int columnId, List<Task> orderedTasks) {
        String token = prefs.getString("jwt_token", null);
        if (token == null || projectId == -1) return;

        apiClient.updateTaskOrder(token, projectId, columnId, orderedTasks, new ParticipantsCallback() {
            @Override
            public void onSuccess() {
                Log.d("TasksFragment", "Порядок задач сохранён");
            }

            @Override
            public void onFailure(String error) {
                Log.e("TasksFragment", "Ошибка сохранения порядка: " + error);
            }
        });
    }
}