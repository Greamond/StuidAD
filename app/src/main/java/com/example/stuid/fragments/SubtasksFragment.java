package com.example.stuid.fragments;

import androidx.fragment.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
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
import com.example.stuid.api.SubtaskColumnCreateCallback;
import com.example.stuid.api.SubtaskColumnsCallback;
import com.example.stuid.api.SubtaskCreateCallback;
import com.example.stuid.api.SubtasksCallback;
import com.example.stuid.api.TaskCreateCallback;
import com.example.stuid.api.TaskDeleteCallback;
import com.example.stuid.api.TasksCallback;
import com.example.stuid.models.ColumnsAdapter;
import com.example.stuid.models.Employee;
import com.example.stuid.models.Subtask;
import com.example.stuid.models.SubtaskColumn;
import com.example.stuid.models.SubtaskColumnsAdapter;
import com.example.stuid.models.Task;
import com.example.stuid.models.TaskAdapter;
import com.example.stuid.models.TaskColumn;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SubtasksFragment extends Fragment {
    private int projectId;
    private int projectCreatorId;
    private int taskId;
    private List<Employee> projectParticipants = new ArrayList<>();
    private Employee selectedAssignee = null;
    private SharedPreferences prefs;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ApiClient apiClient;
    private List<Task> tasks = new ArrayList<>();
    private int currentUserId;
    private boolean canEditTask = false;
    private List<Employee> currentTaskAssignees = new ArrayList<>();

    private RecyclerView columnsRecyclerView;
    private SubtaskColumnsAdapter columnsAdapter;
    private List<SubtaskColumn> columns = new ArrayList<>();
    private static Subtask draggedTask;

    public static void setDraggedSubtask (Subtask subtask) {
        draggedTask = subtask;
    }

    public static Subtask getDraggedSubtask() {
        return draggedTask;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("employee_id", -1);

        if (getArguments() != null) {
            projectId = getArguments().getInt("projectId", -1);
            projectCreatorId = getArguments().getInt("creatorId", -1);
            taskId = getArguments().getInt("taskId", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subtasks, container, false);

        // Инициализация элементов UI
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        apiClient = new ApiClient();

        // Настройка RecyclerView для колонок
        columnsRecyclerView = view.findViewById(R.id.rvColumns);
        columnsRecyclerView.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false));

        columnsAdapter = new SubtaskColumnsAdapter(
                columns,
                projectParticipants,
                currentUserId,
                apiClient,
                prefs.getString("jwt_token", null),
                this::onSubtaskClicked,
                this::onAddSubtaskClicked,
                (subtask, newChapterId) -> updateSubtaskChapter(subtask.getId(), newChapterId),
                this);
        columnsRecyclerView.setAdapter(columnsAdapter);

        // Кнопка добавления колонки
        view.findViewById(R.id.btnAddColumn).setOnClickListener(v -> showAddColumnDialog());

        // Обновление данных
        swipeRefresh.setOnRefreshListener(this::refreshData);
        loadInitialData();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Загрузка задач
        loadProjectParticipants();
        loadSubtasks();
    }

    private void loadInitialData() {
        progressBar.setVisibility(View.VISIBLE);

        // Загрузка участников проекта
        loadProjectParticipants(() -> {
            // После загрузки участников загружаем колонки и задачи
            loadColumns();
        });
    }

    private void refreshData() {
        loadProjectParticipants(() -> {
            loadSubtasks();
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

    private void loadColumns() {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        apiClient.getSubtaskColumnsForTask(token, taskId, new SubtaskColumnsCallback() {
            @Override
            public void onSuccess(List<SubtaskColumn> serverColumns) {
                requireActivity().runOnUiThread(() -> {
                    columns.clear();
                    columns.addAll(serverColumns);
                    columnsAdapter.notifyDataSetChanged();
                    loadSubtasks();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Ошибка загрузки колонок: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadSubtasks() {
        String token = prefs.getString("jwt_token", null);
        if (token == null || taskId == -1) return;

        apiClient.getTaskSubtasks(token, taskId, new SubtasksCallback() {
            @Override
            public void onSuccess(List<Subtask> loadedSubtasks) {
                requireActivity().runOnUiThread(() -> {

                    // Очищаем задачи во всех колонках
                    for (SubtaskColumn column : columns) {
                        column.getSubtasks().clear();
                    }

                    // Распределяем задачи по колонкам
                    Map<Integer, List<Subtask>> subtasksByColumn = new HashMap<>();
                    for (Subtask subtask : loadedSubtasks) {
                        subtasksByColumn.computeIfAbsent(subtask.getChapterId(), k -> new ArrayList<>()).add(subtask);
                    }

                    // Сортируем и привязываем
                    for (SubtaskColumn column : columns) {
                        List<Subtask> columnSubtasks = subtasksByColumn.getOrDefault(column.getId(), new ArrayList<>());
                        Collections.sort(columnSubtasks, Comparator.comparingInt(Subtask::getPosition));
                        column.setSubtasks(columnSubtasks);
                        Log.d("SubtasksFragment", "Колонка '" + column.getName() + "' получила " + columnSubtasks.size() + " подзадач");
                    }

                    columnsAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),"Ошибка загрузки подзадач: " + error,Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onSubtaskClicked(Subtask subtask) {
        checkEditPermission(subtask, () -> {
            if (canEditTask) {
                draggedTask = subtask;
                showTaskDialog(subtask);
            } else {
                Toast.makeText(requireContext(), "Нет прав на редактирование", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onAddSubtaskClicked(int columnId) {
        showAddTaskDialog(columnId);
    }

    private void showAddColumnDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Создать новую колонку");
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String columnName = input.getText().toString().trim();
            if (!columnName.isEmpty()) {
                createSubtaskColumn(columnName);
            } else {
                Toast.makeText(requireContext(), "Введите название колонки", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void createSubtaskColumn(String name) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            Toast.makeText(requireContext(), "Не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        // Убедитесь, что projectId доступен в этом контексте
        if (taskId == -1) {
            Toast.makeText(requireContext(), "Неверный проект", Toast.LENGTH_SHORT).show();
            return;
        }

        apiClient.createSubtaskColumn(token, name, taskId, new SubtaskColumnCreateCallback() {
            @Override
            public void onSuccess(SubtaskColumn column) {
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

    private void showAddTaskDialog(int columnId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Новая задача");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etTaskName);
        EditText etDescription = dialogView.findViewById(R.id.etTaskDescription);
        AutoCompleteTextView actvAssigneeSearch = dialogView.findViewById(R.id.actvAssigneeSearch);
        LinearLayout llSelectedAssignees = dialogView.findViewById(R.id.llSelectedAssignees);

        llSelectedAssignees.removeAllViews();

        // Настройка автодополнения для ответственных
        ArrayAdapter<Employee> adapter = new ArrayAdapter<Employee>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                projectParticipants
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Employee employee = getItem(position);
                if (employee != null) {
                    ((TextView) view).setText(employee.getFullName());
                }
                return view;
            }
        };
        actvAssigneeSearch.setAdapter(adapter);

        actvAssigneeSearch.setOnItemClickListener((parent, view, position, id) -> {
            Employee selected = adapter.getItem(position);
            if (selected != null) {
                selectedAssignee = selected;
                addAssigneeView(selected, llSelectedAssignees, true);
                actvAssigneeSearch.setText(""); // Очистка поля ввода
            }
        });

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название подзадачи", Toast.LENGTH_SHORT).show();
                return;
            }

            int creatorId = prefs.getInt("employee_id", -1);
            if (creatorId == -1) {
                Toast.makeText(requireContext(), "Ошибка: не удалось определить создателя подзадачи",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            int assigneeId = -1; // По умолчанию не назначен
            if (selectedAssignee != null) {
                assigneeId = selectedAssignee.getEmployeeId();
            }

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("Name", name);
                jsonBody.put("TaskId", taskId);
                jsonBody.put("Description", description);
                jsonBody.put("ChapterId", columnId);
                jsonBody.put("CreatorId", creatorId);
                jsonBody.put("Responsible", assigneeId);

            } catch (JSONException e) {
                Toast.makeText(requireContext(), "Ошибка формирования запроса", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            String token = prefs.getString("jwt_token", null);
            if (token == null) {
                Toast.makeText(requireContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
                return;
            }

            apiClient.createSubtask(token, jsonBody, new SubtaskCreateCallback() {
                @Override
                public void onSuccess(Subtask subtask) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        for (SubtaskColumn column : columns) {
                            if (column.getId() == subtask.getChapterId()) {
                                column.getSubtasks().add(subtask);
                                columnsAdapter.notifyItemChanged(columns.indexOf(column));
                                break;
                            }
                        }

                        selectedAssignee = null;
                        Toast.makeText(requireContext(), "Подзадача создана", Toast.LENGTH_SHORT).show();
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

        builder.setNegativeButton("Отмена", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    private void addAssigneeView(Employee employee, LinearLayout container, boolean removable) {
        container.removeAllViews(); // Очищаем, чтобы был только один

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_selected_employee, container, false);

        TextView tvName = view.findViewById(R.id.tvEmployeeName);
        ImageButton btnRemove = view.findViewById(R.id.btnRemoveEmployee);

        tvName.setText(employee.getFullName());
        btnRemove.setVisibility(removable ? View.VISIBLE : View.GONE);

        if (removable) {
            btnRemove.setOnClickListener(v -> {
                selectedAssignee = null;
                container.removeAllViews();
            });
        }

        container.addView(view);
    }

    private void loadProjectParticipants() {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        apiClient.getProjectParticipants(token, projectId, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> participants) {
                requireActivity().runOnUiThread(() -> {
                    projectParticipants.clear();
                    projectParticipants.addAll(participants);
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e("TasksFragment", "Error loading participants: " + error);
            }
        });
    }

    private void checkEditPermission(Subtask subtask, Runnable onSuccess) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        // Проверяем, является ли пользователь создателем проекта
        boolean isProjectCreator = currentUserId == projectCreatorId;
        // Проверяем, является ли текущий пользователь создателем задачи
        boolean isTaskCreator  = subtask.getCreatorId() == currentUserId;

        if (isProjectCreator || isTaskCreator) {
            // Если пользователь создатель - сразу разрешаем редактирование
            requireActivity().runOnUiThread(() -> {
                canEditTask = true;
                onSuccess.run();
            });
            return;
        }

        // Если не создатель - проверяем, является ли ответственным
        apiClient.getTaskAssignees(token, subtask.getId(), new EmployeesCallback() {
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

    private void setupAssigneeSearch(AutoCompleteTextView actvAssigneeSearch,
                                     LinearLayout llSelectedAssignees) {
        Log.d("SubtasksFragment", "Setting up assignee search with " + projectParticipants.size() + " participants");

        ArrayAdapter<Employee> adapter = new ArrayAdapter<Employee>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                projectParticipants
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Employee employee = getItem(position);
                if (employee != null) {
                    ((TextView) view).setText(employee.getFullName());
                }
                return view;
            }
        };

        actvAssigneeSearch.setAdapter(adapter);
        actvAssigneeSearch.setOnItemClickListener((parent, view, position, id) -> {
            Employee selected = adapter.getItem(position);
            if (selected != null) {
                selectedAssignee = selected;
                addAssigneeView(selected, llSelectedAssignees, true);
                actvAssigneeSearch.setText(""); // Очистка поля ввода
            }
        });
    }

    private void showTaskDialog(Subtask subtask) {
        // Сначала проверяем права
        checkEditPermission(subtask, () -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(subtask.getName());

            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_task, null);
            builder.setView(dialogView);

            EditText etName = dialogView.findViewById(R.id.etTaskName);
            EditText etDescription = dialogView.findViewById(R.id.etTaskDescription);
            AutoCompleteTextView actvAssigneeSearch = dialogView.findViewById(R.id.actvAssigneeSearch);
            LinearLayout llSelectedAssignees = dialogView.findViewById(R.id.llSelectedAssignees);

            // Заполняем поля
            etName.setText(subtask.getName());
            etDescription.setText(subtask.getDescription());

            // Настраиваем доступность
            etName.setEnabled(canEditTask);
            etDescription.setEnabled(canEditTask);
            actvAssigneeSearch.setEnabled(canEditTask);

            // Показываем создателя задачи
            TextView tvCreator = dialogView.findViewById(R.id.tvCreator);
            if (tvCreator != null) {
                tvCreator.setText("Создатель: загрузка...");
                getCreatorName(subtask.getCreatorId(), name -> {
                    tvCreator.setText("Создатель: " + name);
                });
            }

            // Показываем текущего ответственного
            int responsibleId = subtask.getResponsibleId();
            if (responsibleId != -1) { // Предположим, что -1 — значение по умолчанию
                for (Employee employee : projectParticipants) {
                    if (employee.getEmployeeId() == responsibleId) {
                        addAssigneeView(employee, llSelectedAssignees, false); // false — нельзя удалить
                        break;
                    }
                }
            }

            // Настройка автодополнения для ответственного при редактировании
            if (canEditTask) {
                setupAssigneeSearch(actvAssigneeSearch, llSelectedAssignees);
            }

            // Кнопки диалога
            builder.setPositiveButton("Закрыть", null);

            if (canEditTask && (currentUserId == subtask.getCreatorId() || currentUserId == projectCreatorId)) {
                builder.setNegativeButton("Удалить", (dialog, which) -> {
                    showDeleteConfirmationDialog(subtask);
                });
            }

            if (canEditTask) {
                builder.setNeutralButton("Сохранить", (dialog, which) -> {
                    updateSubtask(subtask.getId(),
                            etName.getText().toString(),
                            etDescription.getText().toString());
                });
            }

            builder.show();
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

    private void updateSubtask(int subtaskId, String name, String description) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        int assigneeId = -1; // По умолчанию не назначен
        if (selectedAssignee != null) {
            assigneeId = selectedAssignee.getEmployeeId();
        }

        // Находим текущую колонку задачи
        int currentChapterId = 1; // значение по умолчанию
        for (SubtaskColumn column : columns) {
            for (Subtask task : column.getSubtasks()) {
                if (task.getId() == subtaskId) {
                    currentChapterId = column.getId();
                    break;
                }
            }
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Name", name);
            jsonBody.put("Description", description);
            jsonBody.put("ChapterId", currentChapterId);
            jsonBody.put("Responsible", assigneeId);
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Ошибка формирования запроса",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        apiClient.updateSubtask(token, subtaskId, jsonBody, new SubtaskCreateCallback() {
            @Override
            public void onSuccess(Subtask updatedTask) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    // Обновляем задачу в списке
                    for (SubtaskColumn column : columns) {
                        for (int i = 0; i < column.getSubtasks().size(); i++) {
                            if (column.getSubtasks().get(i).getId() == updatedTask.getId()) {
                                column.getSubtasks().set(i, updatedTask);
                                break;
                            }
                        }
                    }

                    // Обновляем список задач
                    loadSubtasks();
                    Toast.makeText(requireContext(), "Подзадача обновлена",
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

    private void showDeleteConfirmationDialog(Subtask subtask) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Подтверждение удаления")
                .setMessage("Вы уверены, что хотите удалить эту подзадачу?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteSubtask(subtask);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteSubtask(Subtask subtask) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        progressBar.setVisibility(View.VISIBLE);

        apiClient.deleteSubtask(token, subtask.getId(), new TaskDeleteCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    // Удаляем задачу из адаптера
                    columnsAdapter.removeSubtask(subtask);

                    Toast.makeText(requireContext(), "Подзадача удалена", Toast.LENGTH_SHORT).show();
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

    private void updateSubtaskChapter(int subtaskId, int chapterId) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            showToast("Authorization required");
            return;
        }

        apiClient.updateSubtaskChapter(token, subtaskId, chapterId, new ProfileUpdateCallback() {
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

    public void sendNewOrderToServer(int projectId, int columnId, List<Subtask> orderedTasks) {
        String token = prefs.getString("jwt_token", null);
        if (token == null || projectId == -1) return;

        apiClient.updateSubtaskOrder(token, projectId, columnId, orderedTasks, new ParticipantsCallback() {
            @Override
            public void onSuccess() {
                Log.d("SubtasksFragment", "Порядок подзадач сохранён");
            }

            @Override
            public void onFailure(String error) {
                Log.e("SubtasksFragment", "Ошибка сохранения порядка: " + error);
            }
        });
    }
}
