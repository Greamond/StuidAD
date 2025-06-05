package com.example.stuid.api;

import org.json.JSONObject;

public interface AuthPasswordResetCallback {
    void onSuccess(String message, JSONObject data);
    void onFailure(String error);
}