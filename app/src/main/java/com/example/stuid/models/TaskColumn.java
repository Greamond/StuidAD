package com.example.stuid.models;

import java.util.List;

public class TaskColumn {
    private int id;
    private int projectId;
    private String name;
    private List<Task> tasks;

    public TaskColumn(int id,int projectId, String name, List<Task> tasks) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.tasks = tasks;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public int getProjectId() { return projectId; }
    public String getName() { return name; }
    public List<Task> getTasks() { return tasks; }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}