package com.example.stuid.api;

import com.example.stuid.models.Employee;

import java.util.List;

public interface EmployeesCallback {
    void onSuccess(List<Employee> employees);
    void onFailure(String error);
}
