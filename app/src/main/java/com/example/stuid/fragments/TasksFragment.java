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
import com.example.stuid.api.EmployeesCallback;
import com.example.stuid.api.TaskCreateCallback;
import com.example.stuid.api.TasksCallback;
import com.example.stuid.models.Employee;
import com.example.stuid.models.Task;
import com.example.stuid.models.TaskAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment {
    private int projectId;
    private List<Employee> projectParticipants = new ArrayList<>();
    private List<Employee> selectedAssignees = new ArrayList<>();
    private SharedPreferences prefs;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ApiClient apiClient;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> tasks = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        if (getArguments() != null) {
            projectId = getArguments().getInt("projectId", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // Инициализация RecyclerView
        recyclerView = view.findViewById(R.id.rvTasks);
        adapter = new TaskAdapter(tasks, recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        apiClient = new ApiClient();

        // Настройка SwipeRefresh
        swipeRefresh.setOnRefreshListener(this::loadTasks);

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
                addAssigneeView(selected, llSelectedAssignees);
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

    private void addAssigneeView(Employee employee, LinearLayout container) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_selected_employee, container, false);

        TextView tvName = view.findViewById(R.id.tvEmployeeName);
        ImageButton btnRemove = view.findViewById(R.id.btnRemoveEmployee);

        tvName.setText(employee.getFullName());
        btnRemove.setOnClickListener(v -> {
            container.removeView(view);
            selectedAssignees.remove(employee);
        });

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

        // Создаем JSON объект для отправки
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("Name", name);
            jsonBody.put("Description", description);
            jsonBody.put("ProjectId", projectId);
            jsonBody.put("AssigneeIds", new JSONArray(assigneeIds));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        progressBar.setVisibility(View.VISIBLE);

        apiClient.createTask(token, jsonBody, new TaskCreateCallback() {
            @Override
            public void onSuccess(Task task) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    adapter.addTask(task);
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
}