package com.example.stuid.models;

public class Task {
    private int id;
    private int projectId;
    private String name;
    private String description;
    private int chapter;

    public Task(int id, int projectId, String name, String description, int chapter) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.chapter = chapter;
    }

    // Геттеры
    public int getId() { return id; }
    public int getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getChapter() { return chapter; }
}