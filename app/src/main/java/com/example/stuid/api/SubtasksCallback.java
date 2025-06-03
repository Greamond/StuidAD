package com.example.stuid.api;

import com.example.stuid.models.Subtask;
import com.example.stuid.models.Task;

import java.util.List;

public interface SubtasksCallback {
    void onSuccess(List<Subtask> subtasks);
    void onFailure(String error);
}
