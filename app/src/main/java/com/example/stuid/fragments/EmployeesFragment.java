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
import com.example.stuid.models.Employee;
import com.example.stuid.models.EmployeeAdapter;

import java.util.ArrayList;
import java.util.List;

public class EmployeesFragment extends Fragment {
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_employees, container, false);

        recyclerView = view.findViewById(R.id.rvEmployees);
        setupRecyclerView();

        return view;
    }

    private void setupRecyclerView() {
        // Тестовые данные
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee("Иванов", "Иван", "Иваныч", "ivanov@edu.ru", "Android разработчик"));
        employees.add(new Employee("Петрова", "Анна", "Фёдоровна", "petrova@edu.ru", "Дизайнер интерфейсов"));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new EmployeeAdapter(employees));
    }
}
