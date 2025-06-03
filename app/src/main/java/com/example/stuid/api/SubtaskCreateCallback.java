package com.example.stuid.api;

import com.example.stuid.models.Subtask;

public interface SubtaskCreateCallback {
    void onSuccess(Subtask subtask);
    void onFailure(String error);
}
