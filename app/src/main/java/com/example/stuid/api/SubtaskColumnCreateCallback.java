package com.example.stuid.api;

import com.example.stuid.models.SubtaskColumn;
import com.example.stuid.models.TaskColumn;

public interface SubtaskColumnCreateCallback {
    void onSuccess(SubtaskColumn column);
    void onFailure(String error);
}