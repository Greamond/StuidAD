package com.example.stuid.api;

import com.example.stuid.models.Task;

public interface TaskCreateCallback {
    void onSuccess(Task task);
    void onFailure(String error);
}
