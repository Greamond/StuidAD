package com.example.stuid.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.stuid.R;
import com.example.stuid.activity.SignInActivity;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.EmployeesCallback;
import com.example.stuid.api.ParticipantsCallback;
import com.example.stuid.api.ProjectCreateCallback;
import com.example.stuid.api.ProjectsCallback;
import com.example.stuid.models.Employee;
import com.example.stuid.models.Project;
import com.example.stuid.models.ProjectAdapter;

import java.util.ArrayList;
import java.util.List;

public class ProjectsFragment extends Fragment implements ProjectAdapter.OnTaskButtonClickListener {
    private RecyclerView recyclerView;
    private ApiClient apiClient;
    private SharedPreferences prefs;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ProjectAdapter adapter;
    private ImageButton btnAdd;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projects, container, false);

        // Инициализация View элементов
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.rvProjects);

        btnAdd = view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> {showAddProjectDialog();});

        // Настройка адаптера
        adapter = new ProjectAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Настройка SwipeRefresh
        swipeRefresh.setOnRefreshListener(this::loadProjects);

        apiClient = new ApiClient();
        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        loadProjects();

        return view;
    }

    private void loadProjects() {
        // Показываем индикатор загрузки (если это не свайп-обновление)
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            hideLoaders();
            showError("Not authenticated");
            redirectToLogin();
            return;
        }

        apiClient.getProjects(token, new ProjectsCallback() {
            @Override
            public void onSuccess(List<Project> projects) {
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) { // Проверка что фрагмент прикреплен к Activity
                        hideLoaders();
                        adapter.updateProjects(projects);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        hideLoaders();
                        showError(error);
                        if (error.contains("expired")) {
                            redirectToLogin();
                        }
                    }
                });
            }
        });
    }

    private void showAddProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Новый проект");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_project, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etProjectName);
        EditText etDescription = dialogView.findViewById(R.id.etProjectDescription);
        CheckBox cbIsPublic = dialogView.findViewById(R.id.cbIsPublic);
        AutoCompleteTextView actvEmployeeSearch = dialogView.findViewById(R.id.actvEmployeeSearch);
        LinearLayout llSelectedEmployees = dialogView.findViewById(R.id.llSelectedEmployees);

        // Загружаем список сотрудников для выбора
        loadEmployeesForDialog(actvEmployeeSearch, llSelectedEmployees);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String name = etName.getText().toString();
            String description = etDescription.getText().toString();
            boolean isPublic = cbIsPublic.isChecked();

            if (!name.isEmpty()) {
                createNewProject(name, description, isPublic, getSelectedEmployeeIds(llSelectedEmployees));
            } else {
                Toast.makeText(requireContext(),
                        "Введите название проекта", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private List<Employee> allEmployees = new ArrayList<>(); // Добавляем поле класса

    private void loadEmployeesForDialog(AutoCompleteTextView actvEmployeeSearch, LinearLayout llSelectedEmployees) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            redirectToLogin();
            return;
        }

        apiClient.getEmployees(token, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> employees) {
                requireActivity().runOnUiThread(() -> {
                    allEmployees = new ArrayList<>(employees); // Сохраняем копию списка

                    ArrayAdapter<Employee> adapter = new ArrayAdapter<Employee>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            allEmployees // Используем сохраненный список
                    ) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            Employee employee = getItem(position);
                            if (employee != null) {
                                ((TextView) view).setText(employee.getFullName() + " (" + employee.getEmail() + ")");
                            }
                            return view;
                        }

                        @Override
                        public Filter getFilter() {
                            return new Filter() {
                                @Override
                                protected FilterResults performFiltering(CharSequence constraint) {
                                    FilterResults results = new FilterResults();
                                    List<Employee> filteredList = new ArrayList<>();

                                    if (constraint == null || constraint.length() == 0) {
                                        filteredList.addAll(allEmployees);
                                    } else {
                                        String filterPattern = constraint.toString().toLowerCase().trim();
                                        for (Employee employee : allEmployees) {
                                            if (employee.getFullName().toLowerCase().contains(filterPattern) ||
                                                    employee.getEmail().toLowerCase().contains(filterPattern)) {
                                                filteredList.add(employee);
                                            }
                                        }
                                    }

                                    results.values = filteredList;
                                    results.count = filteredList.size();
                                    return results;
                                }

                                @Override
                                protected void publishResults(CharSequence constraint, FilterResults results) {
                                    clear();
                                    if (results.values != null) {
                                        addAll((List<Employee>) results.values);
                                    }
                                    notifyDataSetChanged();
                                }
                            };
                        }
                    };

                    actvEmployeeSearch.setAdapter(adapter);
                    actvEmployeeSearch.setOnItemClickListener((parent, view, position, id) -> {
                        Employee selectedEmployee = adapter.getItem(position);
                        if (selectedEmployee != null && !isEmployeeAlreadyAdded(selectedEmployee.getEmployeeId(), llSelectedEmployees)) {
                            addSelectedEmployee(selectedEmployee, llSelectedEmployees);
                            actvEmployeeSearch.setText("");
                        }
                    });
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Ошибка загрузки сотрудников: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Проверяем, не добавлен ли уже этот сотрудник
    private boolean isEmployeeAlreadyAdded(int employeeId, LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            Integer id = (Integer) child.getTag();
            if (id != null && id == employeeId) {
                Toast.makeText(requireContext(), "Этот сотрудник уже добавлен", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }

    private void addSelectedEmployee(Employee employee, LinearLayout container) {
        View employeeView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_selected_employee, container, false);

        TextView tvName = employeeView.findViewById(R.id.tvEmployeeName);
        ImageButton btnRemove = employeeView.findViewById(R.id.btnRemoveEmployee);

        tvName.setText(employee.getFullName());
        btnRemove.setOnClickListener(v -> container.removeView(employeeView));

        // Сохраняем ID сотрудника в тег view
        employeeView.setTag(employee.getEmployeeId());

        container.addView(employeeView);
    }

    private List<Integer> getSelectedEmployeeIds(LinearLayout container) {
        List<Integer> selectedIds = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            Integer employeeId = (Integer) child.getTag();
            if (employeeId != null) {
                selectedIds.add(employeeId);
            }
        }
        return selectedIds;
    }

    private void createNewProject(String name, String description, boolean isPublic, List<Integer> participantIds) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            redirectToLogin();
            return;
        }

        // Создаем проект без указания Creator (сервер добавит сам)
        Project newProject = new Project(0, name, description, isPublic, 0);
        progressBar.setVisibility(View.VISIBLE);

        apiClient.createProject(token, newProject, new ProjectCreateCallback() {
            @Override
            public void onSuccess(Project createdProject) {
                // После успешного создания проекта добавляем участников
                if (!participantIds.isEmpty()) {
                    addParticipantsToProject(token, createdProject.getId(), participantIds);
                } else {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Проект '" + createdProject.getName() + "' создан",
                                Toast.LENGTH_SHORT).show();

                        // Добавляем проект в список
                        adapter.addProject(createdProject);
                        recyclerView.smoothScrollToPosition(0);
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showError("Ошибка создания проекта: " + error);

                    // Для ошибок авторизации перенаправляем на логин
                    if (error.contains("401") || error.contains("Unauthorized")) {
                        redirectToLogin();
                    }
                });
            }
        });
    }

    private void addParticipantsToProject(String token, int projectId, List<Integer> participantIds) {
        apiClient.addParticipants(token, projectId, participantIds, new ParticipantsCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Проект создан и участники добавлены",
                            Toast.LENGTH_SHORT).show();
                    loadProjects(); // Обновляем список проектов
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Проект создан, но не удалось добавить участников: " + error,
                            Toast.LENGTH_LONG).show();
                    loadProjects(); // Все равно обновляем список проектов
                });
            }
        });
    }

    private void hideLoaders() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), "Error: " + message, Toast.LENGTH_LONG).show();
    }

    private void redirectToLogin() {
        startActivity(new Intent(requireActivity(), SignInActivity.class));
        requireActivity().finish();
    }

    @Override
    public void onTaskButtonClick(int position) {
        // Реализация перехода к задачам проекта
        Project project = adapter.getProjects().get(position);

        // Раскомментируйте когда будете реализовывать TasksFragment
        /*
        Bundle args = new Bundle();
        args.putString("project_number", project.getNumber());

        TasksFragment tasksFragment = new TasksFragment();
        tasksFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, tasksFragment)
                .addToBackStack(null)
                .commit();
        */
    }
}