package com.example.stuid.api;

import com.example.stuid.models.TaskColumn;

import java.util.List;

public interface ColumnsCallback {
    void onSuccess(List<TaskColumn> columns);
    void onFailure(String error);
}
