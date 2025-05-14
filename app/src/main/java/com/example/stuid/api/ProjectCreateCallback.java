package com.example.stuid.api;

import com.example.stuid.models.Project;

public interface ProjectCreateCallback {
    void onSuccess(Project createdProject);
    void onFailure(String errorMessage);
}