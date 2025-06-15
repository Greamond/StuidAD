package com.example.stuid.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.EmployeesCallback;
import com.example.stuid.api.SafeCallManager;
import com.example.stuid.api.TasksCallback;
import com.example.stuid.models.Employee;
import com.example.stuid.models.MyTaskAdapter;
import com.example.stuid.models.Task;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

public class MyTasksFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private MyTaskAdapter adapter;
    private List<Employee> employees = new ArrayList<>();
    private ApiClient apiClient;
    private SharedPreferences prefs;
    private int currentUserId;
    private SearchView searchView;
    private List<Task> allTasks = new ArrayList<>();
    private final SafeCallManager callManager = new SafeCallManager();

    @Override
    public void onStop() {
        super.onStop();
        callManager.cancelAll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        callManager.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_tasks, container, false);

        recyclerView = view.findViewById(R.id.rvMyTasks);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        searchView = view.findViewById(R.id.searchView);
        setupSearchView();

        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("employee_id", -1);
        apiClient = new ApiClient();

        // Инициализация адаптера с пустыми списками
        adapter = new MyTaskAdapter(new ArrayList<>(), new ArrayList<>(), currentUserId, apiClient, prefs.getString("jwt_token", null));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::refreshData);

        progressBar.setVisibility(View.VISIBLE);
        loadInitialData();

        return view;
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTasks(newText);
                return true;
            }
        });
    }

    private void filterTasks(String query) {
        List<Task> filteredList = new ArrayList<>();

        if (query.isEmpty()) {
            filteredList.addAll(allTasks);
        } else {
            String searchPattern = query.toLowerCase().trim();
            for (Task task : allTasks) {
                if (task.getName().toLowerCase().contains(searchPattern)) {
                    filteredList.add(task);
                }
            }
        }

        adapter.updateTasks(filteredList);

        // Показываем сообщение, если ничего не найдено
        TextView emptyView = requireView().findViewById(R.id.tvEmptyState);
        emptyView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadInitialData() {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Сначала загружаем пользователей
        Call call = apiClient.getEmployees(token, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> loadedEmployees) {
                employees.clear();
                employees.addAll(loadedEmployees);

                // Затем загружаем задачи
                loadTasks(token);
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ошибка загрузки пользователей: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
        callManager.add(call);
    }

    private void loadTasks(String token) {
        Call call = apiClient.getTasksByAssignee(token, currentUserId, new TasksCallback() {
            @Override
            public void onSuccess(List<Task> tasks) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);

                    allTasks.clear();
                    allTasks.addAll(tasks); // Сохраняем все задачи
                    filterTasks(searchView.getQuery().toString());

                    // Обновляем адаптер с задачами и пользователями
                    adapter.updateData(tasks, employees);

                    TextView emptyView = requireView().findViewById(R.id.tvEmptyState);
                    emptyView.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), "Ошибка загрузки задач: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
        callManager.add(call);
    }

    private void refreshData() {
        loadInitialData();
    }
}
