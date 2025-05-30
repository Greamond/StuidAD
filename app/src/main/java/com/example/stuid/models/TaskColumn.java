package com.example.stuid.models;

import java.util.List;

public class TaskColumn {
    private int id;
    private String name;
    private List<Task> tasks;

    public TaskColumn(int id, String name, List<Task> tasks) {
        this.id = id;
        this.name = name;
        this.tasks = tasks;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public String getName() { return name; }
    public List<Task> getTasks() { return tasks; }
}