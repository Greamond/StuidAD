package com.example.stuid.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.stuid.R;
import com.example.stuid.activity.SignInActivity;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.EmployeesCallback;
import com.example.stuid.models.Employee;
import com.example.stuid.models.EmployeeAdapter;

import java.util.ArrayList;
import java.util.List;

public class EmployeesFragment extends Fragment {

    private RecyclerView recyclerView;
    private EmployeeAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private ApiClient apiClient;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_employees, container, false);

        // Инициализация элементов UI
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        recyclerView = view.findViewById(R.id.rvEmployees);

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EmployeeAdapter(new ArrayList<>(), requireContext());
        recyclerView.setAdapter(adapter);

        // Настройка SwipeRefresh
        swipeRefresh.setColorSchemeResources(R.color.blue);
        swipeRefresh.setOnRefreshListener(this::loadEmployees);

        // Инициализация API клиента
        apiClient = new ApiClient();
        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        // Загрузка данных
        loadInitialData();

        return view;
    }

    private void loadInitialData() {
        progressBar.setVisibility(View.VISIBLE);
        loadEmployees();
    }

    private void loadEmployees() {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            handleAuthError();
            return;
        }

        apiClient.getEmployees(token, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> employees) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        hideLoaders();
                        if (employees.isEmpty()) {
                            showEmptyState();
                        } else {
                            adapter.updateEmployees(employees);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        hideLoaders();
                        showError(error);
                        if (error.toLowerCase().contains("expired") || error.contains("401")) {
                            redirectToLogin();
                        }
                    });
                }
            }
        });
    }

    private void hideLoaders() {
        progressBar.setVisibility(View.GONE);
        if (swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void handleAuthError() {
        hideLoaders();
        showError("Требуется авторизация");
        redirectToLogin();
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), "Ошибка: " + message, Toast.LENGTH_LONG).show();
    }

    private void showEmptyState() {
        Toast.makeText(requireContext(), "Нет данных о сотрудниках", Toast.LENGTH_SHORT).show();
    }

    private void redirectToLogin() {
        startActivity(new Intent(requireActivity(), SignInActivity.class));
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очистка ссылок для предотвращения утечек памяти
        swipeRefresh = null;
        progressBar = null;
        recyclerView = null;
    }
}