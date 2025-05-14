package com.example.stuid.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.EmployeesCallback;
import com.example.stuid.models.Employee;
import com.example.stuid.models.EmployeeAdapter;

import java.util.ArrayList;
import java.util.List;

public class EmployeesFragment extends Fragment {
    private RecyclerView recyclerView;
    private ApiClient apiClient;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_employees, container, false);

        apiClient = new ApiClient();
        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        recyclerView = view.findViewById(R.id.rvEmployees);

        loadEmployees();

        return view;
    }

    private void loadEmployees() {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            showError("Not authenticated");
            return;
        }

        apiClient.getEmployees(token, new EmployeesCallback() {
            @Override
            public void onSuccess(List<Employee> employees) {
                requireActivity().runOnUiThread(() -> {
                    setupRecyclerView(employees);
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    showError(error);
                });
            }
        });
    }

    private void setupRecyclerView(List<Employee> employees) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new EmployeeAdapter(employees));
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), "Error: " + message, Toast.LENGTH_LONG).show();
    }
}
