package com.example.stuid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;
import com.example.stuid.models.Task;
import com.example.stuid.models.TaskAdapter;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment {
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);
        recyclerView = view.findViewById(R.id.rvTasks);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        // Тестовые данные
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task("101", "Разработать интерфейс", "Иванов И.И.", "В работе"));
        tasks.add(new Task("102", "Написать API", "Петрова А.С.", "Новая"));
        tasks.add(new Task("103", "Протестировать модуль", "Сидоров П.Д.", "Завершена"));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new TaskAdapter(tasks));
    }
}
