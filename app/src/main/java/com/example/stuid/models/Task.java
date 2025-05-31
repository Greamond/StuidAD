package com.example.stuid.models;

import java.util.List;

public class Task {
    private int id;
    private int projectId;
    private String name;
    private String description;
    private int chapter;
    private List<Integer> assigneeIds;
    private int creatorId;

    public Task(int id, int projectId, String name, String description, int chapter, int creatorId) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.chapter = chapter;
        this.creatorId = creatorId;
    }

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
    public List<Integer> getAssigneeIds() { return assigneeIds; }

    public int getCreatorId() { return creatorId; }

    public void setAssigneeIds(List<Integer> assigneeIds) { this.assigneeIds = assigneeIds; }

    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }

    public void setChapter(int chapter) {
        this.chapter = chapter;
    }
}