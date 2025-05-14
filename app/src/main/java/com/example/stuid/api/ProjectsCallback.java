package com.example.stuid.api;

import com.example.stuid.models.Project;

import java.util.List;

public interface ProjectsCallback {
    void onSuccess(List<Project> projects);
    void onFailure(String error);
}
