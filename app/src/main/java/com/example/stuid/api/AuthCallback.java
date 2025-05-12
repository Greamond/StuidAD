package com.example.stuid.api;

import com.example.stuid.models.User;

public interface AuthCallback {
    void onSuccess(User user);
    void onFailure(String error);
}
