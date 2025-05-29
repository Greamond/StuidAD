package com.example.stuid.api;

import com.example.stuid.models.Employee;

public interface EmployeeCallback {
    void onSuccess(Employee employee);
    void onFailure(String error);
}
