package com.example.stuid.api;

import com.example.stuid.models.Employee;

public interface AuthCallback {
    void onSuccess(Employee employee, String token);
    void onFailure(String error);
}
