package com.example.stuid.api;

import com.example.stuid.models.User;

public interface AuthCallback {
    void onSuccess(Object response, User user);
    void onFailure(String error);
}
