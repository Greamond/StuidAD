package com.example.stuid.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class ProjectsFragment extends Fragment implements ProjectAdapter.OnTaskButtonClickListener {
    private List<Employee> allEmployees = new ArrayList<>();
    private RecyclerView recyclerView;
    private ApiClient apiClient;
    private SharedPreferences prefs;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ProjectAdapter adapter;
    private ImageButton btnAdd;
    private int currentUserId;

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

        // Настройка SwipeRefresh
        swipeRefresh.setOnRefreshListener(this::loadInitialData);

        apiClient = new ApiClient();
        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("employee_id", -1);


        // Настройка адаптера
        adapter = new ProjectAdapter(new ArrayList<>(), this, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadInitialData();

        return view;
    }

    private void loadInitialData() {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            redirectToLogin();
            return;
        }

        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Сначала загружаем сотрудников, затем проекты
        apiClient.getEmployees(token, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> employees) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setEmployees(employees);
                    loadUserProjects(); // Теперь загружаем проекты
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Ошибка загрузки сотрудников: " + error,
                            Toast.LENGTH_SHORT).show();
                    loadUserProjects(); // Все равно пытаемся загрузить проекты
                });
            }
        });
    }

    private void loadUserProjects() {
        String token = prefs.getString("jwt_token", null);
        int currentUserId = prefs.getInt("employee_id", -1);

        if (token == null || currentUserId == -1) {
            redirectToLogin();
            return;
        }

        apiClient.getUserProjects(token, currentUserId, new ProjectsCallback() {
            @Override
            public void onSuccess(List<Project> projects) {
                requireActivity().runOnUiThread(() -> {
                    adapter.updateProjects(projects);
                    hideLoaders(); // Скрываем все индикаторы загрузки
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    hideLoaders();
                    showError(error);
                });
            }
        });
    }

    private void showAddProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Новый проект");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_project, null);
        builder.setView(dialogView);

        TextInputEditText etName = dialogView.findViewById(R.id.etProjectName);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etProjectDescription);
        CheckBox cbIsPublic = dialogView.findViewById(R.id.cbIsPublic);
        AutoCompleteTextView actvEmployeeSearch = dialogView.findViewById(R.id.actvEmployeeSearch);
        LinearLayout llSelectedEmployees = dialogView.findViewById(R.id.llSelectedEmployees);
        LinearLayout participantsContainer = dialogView.findViewById(R.id.participantsContainer);
        TextView tvAllParticipants = dialogView.findViewById(R.id.tvAllParticipants);
        TextInputLayout tilProjectName = dialogView.findViewById(R.id.tilProjectName);

        addClearErrorTextWatcher(etName,tilProjectName);

        // Обработчик изменения состояния чекбокса
        cbIsPublic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                participantsContainer.setVisibility(View.GONE);
                tvAllParticipants.setVisibility(View.VISIBLE);
                llSelectedEmployees.removeAllViews();
            } else {
                participantsContainer.setVisibility(View.VISIBLE);
                tvAllParticipants.setVisibility(View.GONE);
            }
        });

        // Загружаем сотрудников только если проект не публичный
        if (!cbIsPublic.isChecked()) {
            loadEmployeesForDialog(actvEmployeeSearch, llSelectedEmployees);
        }

        // Добавляем кнопки
        builder.setNegativeButton("Отмена", null);
        builder.setPositiveButton("Создать", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            boolean isPublic = cbIsPublic.isChecked();

            tilProjectName.setError(null); // Сброс ошибок
            TextView tvParticipantError = dialogView.findViewById(R.id.tvParticipantError);

            boolean isValid = true;

            // Проверка названия
            if (name.isEmpty()) {
                tilProjectName.setError("Введите название проекта");
                isValid = false;
            } else if (!isValidProjectName(name)) {
                tilProjectName.setError("Название должно начинаться с заглавной буквы и содержать только русские символы");
                isValid = false;
            }

            // Проверка участников
            if (!isPublic) {
                List<Integer> participantIds = getSelectedEmployeeIds(llSelectedEmployees);
                if (participantIds.isEmpty()) {
                    tvParticipantError.setVisibility(View.VISIBLE);
                    isValid = false;
                } else {
                    tvParticipantError.setVisibility(View.GONE);
                }
            }

            if (!isValid) return;

            // Если всё ок, создаём проект
            List<Integer> participantIds = isPublic ?
                    new ArrayList<>() :
                    getSelectedEmployeeIds(llSelectedEmployees);

            createNewProject(name, description, isPublic, participantIds);
            dialog.dismiss();
        });
    }

    private boolean isValidProjectName(String name) {
        return name != null && name.matches("^[А-Я][а-я]*(\\s[А-Яа-я][а-я]*)*$");
    }

    private void addClearErrorTextWatcher(EditText editText, TextInputLayout textInputLayout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

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

        Project newProject = new Project(0, name, description, isPublic, 0);
        progressBar.setVisibility(View.VISIBLE);

        apiClient.createProject(token, newProject, new ProjectCreateCallback() {
            @Override
            public void onSuccess(Project createdProject) {
                if (!isPublic && !participantIds.isEmpty()) {
                    addParticipantsToProject(token, createdProject.getId(), participantIds);
                } else {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        String message = isPublic ?
                                "Публичный проект '" + createdProject.getName() + "' создан" :
                                "Проект '" + createdProject.getName() + "' создан";
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        adapter.addProject(createdProject);
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
                    loadUserProjects(); // Обновляем список проектов
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Проект создан, но не удалось добавить участников: " + error,
                            Toast.LENGTH_LONG).show();
                    loadUserProjects(); // Все равно обновляем список проектов
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
    public void onProjectClick(int position) {
        Project project = adapter.getProjects().get(position);
        showProjectDetailsDialog(project);
    }

    private void showProjectDetailsDialog(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Информация о проекте");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_view_project, null);
        builder.setView(dialogView);

        TextInputEditText etName = dialogView.findViewById(R.id.tvProjectName);
        TextInputEditText etDescription = dialogView.findViewById(R.id.tvProjectDescription);
        TextInputEditText etIsPublic = dialogView.findViewById(R.id.tvIsPublic);
        LinearLayout llParticipants = dialogView.findViewById(R.id.llParticipants);

        // Заполняем данные проекта
        etName.setText(project.getName());
        etDescription.setText(project.getDescription());
        etIsPublic.setText(project.isPublic() ? "Публичный проект" : "Приватный проект");

        // Загружаем участников проекта
        loadParticipantsForProject(project.getId(), llParticipants);

        builder.setPositiveButton("Закрыть", null);

        // Для создателя проекта добавляем кнопку редактирования
        if (project.getCreator() == prefs.getInt("employee_id",-1)) {
            builder.setNeutralButton("Редактировать", (dialog, which) -> {
                showEditProjectDialog(project);
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showEditProjectDialog(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать проект");
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_project, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etProjectName);
        EditText etDescription = dialogView.findViewById(R.id.etProjectDescription);
        CheckBox cbIsPublic = dialogView.findViewById(R.id.cbIsPublic);
        AutoCompleteTextView actvEmployeeSearch = dialogView.findViewById(R.id.actvEmployeeSearch);
        LinearLayout llSelectedEmployees = dialogView.findViewById(R.id.llSelectedEmployees);
        LinearLayout participantsContainer = dialogView.findViewById(R.id.participantsContainer); // контейнер с полями
        TextView tvAllParticipants = dialogView.findViewById(R.id.tvAllParticipants); // текст "Все"
        TextInputLayout tilProjectName = dialogView.findViewById(R.id.tilProjectName);
        TextView tvParticipantError = dialogView.findViewById(R.id.tvParticipantError);

        // Заполняем поля данными проекта
        etName.setText(project.getName());
        etDescription.setText(project.getDescription());
        cbIsPublic.setChecked(project.isPublic());

        // Настройка текстового ворчера для очистки ошибок
        addClearErrorTextWatcher(etName, tilProjectName);

        // Загружаем текущих участников проекта
        loadCurrentParticipants(project.getId(), llSelectedEmployees);

        // Загружаем список сотрудников для выбора
        loadEmployeesForDialog(actvEmployeeSearch, llSelectedEmployees);

        // Обработчик изменения состояния чекбокса
        cbIsPublic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                participantsContainer.setVisibility(View.GONE);
                tvAllParticipants.setVisibility(View.VISIBLE);
                llSelectedEmployees.removeAllViews(); // Очистить выбранных участников
            } else {
                participantsContainer.setVisibility(View.VISIBLE);
                tvAllParticipants.setVisibility(View.GONE);
            }
        });

        // Устанавливаем начальное состояние видимости
        if (project.isPublic()) {
            participantsContainer.setVisibility(View.GONE);
            tvAllParticipants.setVisibility(View.VISIBLE);
        } else {
            participantsContainer.setVisibility(View.VISIBLE);
            tvAllParticipants.setVisibility(View.GONE);
        }

        builder.setPositiveButton("Сохранить", null);
        builder.setNegativeButton("Отмена", null);
        builder.setNeutralButton("Удалить", (dialog, which) -> {
            showDeleteConfirmationDialog(project.getId());
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            boolean isPublic = cbIsPublic.isChecked();

            tilProjectName.setError(null); // Сброс ошибок
            tvParticipantError.setVisibility(View.GONE); // Сброс ошибки участников
            boolean isValid = true;

            // Проверка названия
            if (name.isEmpty()) {
                tilProjectName.setError("Введите название проекта");
                isValid = false;
            } else if (!isValidProjectName(name)) {
                tilProjectName.setError("Название должно начинаться с заглавной буквы и содержать только русские символы");
                isValid = false;
            }

            // Проверка участников
            if (!isPublic) {
                List<Integer> participantIds = getSelectedEmployeeIds(llSelectedEmployees);
                if (participantIds.isEmpty()) {
                    tvParticipantError.setVisibility(View.VISIBLE);
                    isValid = false;
                }
            }

            if (!isValid) return;

            // Если всё ок, сохраняем изменения
            List<Integer> participantIds = isPublic ?
                    new ArrayList<>() :
                    getSelectedEmployeeIds(llSelectedEmployees);

            updateProject(project.getId(), name, description, isPublic, participantIds);
            dialog.dismiss();
        });
    }

    private void loadCurrentParticipants(int projectId, LinearLayout container) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            redirectToLogin();
            return;
        }

        container.removeAllViews();
        ProgressBar progressBar = new ProgressBar(requireContext());
        container.addView(progressBar);

        apiClient.getProjectParticipants(token, projectId, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> participants) {
                requireActivity().runOnUiThread(() -> {
                    container.removeAllViews();
                    for (Employee participant : participants) {
                        addSelectedEmployee(participant, container);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    container.removeAllViews();
                    TextView errorView = new TextView(requireContext());
                    errorView.setText("Ошибка загрузки участников");
                    errorView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                    container.addView(errorView);
                });
            }
        });
    }

    private void updateProject(int projectId, String name, String description,
                               boolean isPublic, List<Integer> participantIds) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            redirectToLogin();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Project updatedProject = new Project(projectId, name, description, isPublic, 0);

        apiClient.updateProject(token, updatedProject, new ProjectCreateCallback() {
            @Override
            public void onSuccess(Project project) {
                // После успешного обновления проекта обновляем участников
                updateProjectParticipants(token, projectId, participantIds);
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showError("Ошибка обновления проекта: " + error);
                });
            }
        });
    }

    private void updateProjectParticipants(String token, int projectId, List<Integer> newParticipantIds) {
        apiClient.updateProjectParticipants(token, projectId, newParticipantIds, new ParticipantsCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Проект успешно обновлен", Toast.LENGTH_SHORT).show();
                    loadUserProjects(); // Обновляем список проектов
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Проект обновлен, но не удалось обновить участников: " + error,
                            Toast.LENGTH_LONG).show();
                    loadUserProjects(); // Все равно обновляем список проектов
                });
            }
        });
    }

    private void showDeleteConfirmationDialog(int projectId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удаление проекта")
                .setMessage("Вы уверены, что хотите удалить этот проект?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteProject(projectId);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteProject(int projectId) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            redirectToLogin();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        apiClient.deleteProject(token, projectId, new ParticipantsCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Проект успешно удален", Toast.LENGTH_SHORT).show();
                    loadUserProjects(); // Обновляем список проектов
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showError("Ошибка удаления проекта: " + error);
                });
            }
        });
    }

    private void loadParticipantsForProject(int projectId, LinearLayout container) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            redirectToLogin();
            return;
        }

        apiClient.getProjectParticipants(token, projectId, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> participants) {
                requireActivity().runOnUiThread(() -> {
                    container.removeAllViews();
                    for (Employee participant : participants) {
                        TextView participantView = new TextView(requireContext());
                        participantView.setText(participant.getFullName());
                        participantView.setPadding(0, 8, 0, 8);
                        container.addView(participantView);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Ошибка загрузки участников: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onTaskButtonClick(int position) {
        Project project = adapter.getProjects().get(position);

        Log.d("NAVIGATION", "Opening tasks for project ID: " + project.getId());

        Bundle args = new Bundle();
        args.putBoolean("isPublicProject", project.isPublic());
        args.putInt("projectId", project.getId());
        args.putInt("creatorId", project.getCreator());

        Navigation.findNavController(requireView())
                .navigate(R.id.action_projectsFragment_to_tasksDetailFragment, args);
    }
}