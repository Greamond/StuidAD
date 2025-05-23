package com.example.stuid.api;

import com.example.stuid.models.Task;

import java.util.List;

public interface TasksCallback {
    void onSuccess(List<Task> tasks);
    void onFailure(String error);
}
