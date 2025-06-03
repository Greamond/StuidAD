package com.example.stuid.api;

import com.example.stuid.models.SubtaskColumn;
import com.example.stuid.models.TaskColumn;

import java.util.List;

public interface SubtaskColumnsCallback {
    void onSuccess(List<SubtaskColumn> columns);
    void onFailure(String error);
}
