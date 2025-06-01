package com.example.stuid.api;

import com.example.stuid.models.TaskColumn;

public interface ColumnCreateCallback {
    void onSuccess(TaskColumn column);
    void onFailure(String error);
}
