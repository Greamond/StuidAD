package com.example.stuid.api;

import com.example.stuid.models.Employee;

public interface AuthCallback {
    void onSuccess(Employee employee);
    void onFailure(String error);
}
