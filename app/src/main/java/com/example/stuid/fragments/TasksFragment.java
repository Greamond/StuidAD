package com.example.stuid.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.EmployeeCallback;
import com.example.stuid.api.EmployeesCallback;
import com.example.stuid.api.TaskCreateCallback;
import com.example.stuid.api.TaskDeleteCallback;
import com.example.stuid.api.TasksCallback;
import com.example.stuid.models.Employee;
import com.example.stuid.models.Task;
import com.example.stuid.models.TaskAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TasksFragment extends Fragment {
    private int projectId;
    private int projectCreatorId;
    private List<Employee> projectParticipants = new ArrayList<>();
    private List<Employee> selectedAssignees = new ArrayList<>();
    private SharedPreferences prefs;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ApiClient apiClient;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> tasks = new ArrayList<>();
    private int currentUserId;
    private boolean canEditTask = false;
    private List<Employee> currentTaskAssignees = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("employee_id", -1);

        if (getArguments() != null) {
            projectId = getArguments().getInt("projectId", -1);
            projectCreatorId = getArguments().getInt("creatorId", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // Инициализация RecyclerView
        recyclerView = view.findViewById(R.id.rvTasks);
        apiClient = new ApiClient();
        adapter = new TaskAdapter(tasks, recyclerView, projectParticipants, currentUserId, apiClient, prefs.getString("jwt_token", null));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);


        // Инициализация SwipeRefresh
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(() -> {
            loadTasks();
            loadProjectParticipants();
        });

        loadTasks();
        loadProjectParticipants();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Загрузка задач
        loadTasks();
        loadProjectParticipants();

        // Кнопка добавления задачи
        view.findViewById(R.id.btnAddTask).setOnClickListener(v -> showAddTaskDialog());

        adapter.setOnTaskClickListener((position, task) -> {
            // Проверяем, является ли пользователь ответственным за задачу
            checkEditPermission(task, () -> showTaskDialog(task));
        });
    }

    private void loadTasks() {
        String token = prefs.getString("jwt_token", null);
        if (token == null || projectId == -1) return;

        progressBar.setVisibility(View.VISIBLE);

        apiClient.getProjectTasks(token, projectId, new TasksCallback() {
            @Override
            public void onSuccess(List<Task> loadedTasks) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    tasks.clear();
                    tasks.addAll(loadedTasks);
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), "Ошибка загрузки задач: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Новая задача");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etTaskName);
        EditText etDescription = dialogView.findViewById(R.id.etTaskDescription);
        AutoCompleteTextView actvAssigneeSearch = dialogView.findViewById(R.id.actvAssigneeSearch);
        LinearLayout llSelectedAssignees = dialogView.findViewById(R.id.llSelectedAssignees);

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
            if (selected != null && !selectedAssignees.contains(selected)) {
                selectedAssignees.add(selected);
                addAssigneeView(selected, llSelectedAssignees, true);
                actvAssigneeSearch.setText("");
            }
        });

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String name = etName.getText().toString();
            String description = etDescription.getText().toString();

            if (!name.isEmpty()) {
                createTask(name, description);
            } else {
                Toast.makeText(requireContext(), "Введите название задачи", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
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

    private void createTask(String name, String description) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) return;

        List<Integer> assigneeIds = new ArrayList<>();
        for (Employee assignee : selectedAssignees) {
            assigneeIds.add(assignee.getEmployeeId());
        }

        // Получаем ID текущего пользователя
        int creatorId = prefs.getInt("employee_id", -1);
        if (creatorId == -1) {
            Toast.makeText(requireContext(), "Ошибка: не удалось определить создателя задачи",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаем JSON объект для отправки
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Name", name);
            jsonBody.put("Description", description);
            jsonBody.put("ProjectId", projectId);
            jsonBody.put("CreatorId", creatorId); // Добавляем creatorId
            jsonBody.put("AssigneeIds", new JSONArray(assigneeIds));
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Ошибка создания задачи", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        apiClient.createTask(token, jsonBody, new TaskCreateCallback() {
            @Override
            public void onSuccess(Task task) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    adapter.addTask(task);
                    selectedAssignees.clear(); // Очищаем выбранных ответственных
                    Toast.makeText(requireContext(), "Задача создана", Toast.LENGTH_SHORT).show();
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

    private void setupAssigneeSearch(AutoCompleteTextView actvAssigneeSearch,
                                     LinearLayout llSelectedAssignees) {
        Log.d("TasksFragment", "Setting up assignee search with " + projectParticipants.size() + " participants");

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
            if (selected != null && !selectedAssignees.contains(selected)) {
                Log.d("TasksFragment", "Assignee selected: " + selected.getFullName());
                selectedAssignees.add(selected);
                addAssigneeView(selected, llSelectedAssignees, true);
                actvAssigneeSearch.setText("");
            }
        });
    }

    private void showTaskDialog(Task task) {
        // Сначала проверяем права
        checkEditPermission(task, () -> {
            // Теперь canEditTask установлен правильно

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(task.getName());

            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_task, null);
            builder.setView(dialogView);

            EditText etName = dialogView.findViewById(R.id.etTaskName);
            EditText etDescription = dialogView.findViewById(R.id.etTaskDescription);
            TextView tvStatus = dialogView.findViewById(R.id.tvTaskStatus);
            AutoCompleteTextView actvAssigneeSearch = dialogView.findViewById(R.id.actvAssigneeSearch);
            LinearLayout llSelectedAssignees = dialogView.findViewById(R.id.llSelectedAssignees);

            // Заполняем поля
            etName.setText(task.getName());
            etDescription.setText(task.getDescription());
            tvStatus.setText("Статус: " + adapter.getStatusText(task.getChapter()));

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
                    setupAssigneeSearch(actvAssigneeSearch, llSelectedAssignees);
                }
            });

            builder.setPositiveButton("Закрыть", null);

            if (canEditTask && (currentUserId == task.getCreatorId() || currentUserId == projectCreatorId)) {
                builder.setNegativeButton("Удалить", (dialog, which) -> {
                    showDeleteConfirmationDialog(task);
                });
            }

            builder.setPositiveButton("Закрыть", null);

            if (canEditTask) {
                builder.setNeutralButton("Сохранить", (dialog, which) -> {
                    updateTask(task.getId(),
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

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Name", name);
            jsonBody.put("Description", description);
            jsonBody.put("ChapterId", 1);
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
                    for (int i = 0; i < tasks.size(); i++) {
                        if (tasks.get(i).getId() == updatedTask.getId()) {
                            tasks.set(i, updatedTask);
                            adapter.notifyItemChanged(i);
                            break;
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
                    // Удаляем задачу из списка
                    int position = tasks.indexOf(task);
                    if (position != -1) {
                        tasks.remove(position);
                        adapter.notifyItemRemoved(position);
                    }
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
}