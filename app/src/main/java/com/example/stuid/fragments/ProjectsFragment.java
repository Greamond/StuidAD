package com.example.stuid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;
import com.example.stuid.models.Project;
import com.example.stuid.models.ProjectAdapter;

import java.util.ArrayList;
import java.util.List;

public class ProjectsFragment extends Fragment implements ProjectAdapter.OnTaskButtonClickListener {
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projects, container, false);
        recyclerView = view.findViewById(R.id.rvProjects);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        // Тестовые данные
        List<Project> projects = new ArrayList<>();
        projects.add(new Project("101", "Мобильное приложение", "Иванов И.И."));
        projects.add(new Project("102", "Веб-портал", "Петрова А.С."));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new ProjectAdapter(projects, this));
    }

    @Override
    public void onTaskButtonClick(int position) {
        /*
        // Обработка нажатия на кнопку задач
        Project project = ((ProjectAdapter)recyclerView.getAdapter()).getProjects().get(position);

        // Переход к задачам проекта
        TasksFragment tasksFragment = TasksFragment.newInstance(project.getNumber());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, tasksFragment)
                .addToBackStack(null)
                .commit();

         */
    }
}
