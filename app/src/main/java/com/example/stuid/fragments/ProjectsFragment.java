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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.stuid.R;
import com.example.stuid.activity.SignInActivity;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.ProjectsCallback;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projects, container, false);

        // Инициализация View элементов
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.rvProjects);

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