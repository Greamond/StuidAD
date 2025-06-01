package com.example.stuid.models;

import java.util.List;

public class Task {
    private int id;
    private int projectId;
    private String name;
    private String description;
    private int chapter; // статус/колонка
    private List<Integer> assigneeIds;
    private int creatorId;
    private int position; // новое поле

    // Конструктор для задачи с creatorId
    public Task(int id, int projectId, String name, String description, int chapter, int creatorId) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.chapter = chapter;
        this.creatorId = creatorId;
    }

    // Конструктор без creatorId (например, при загрузке из API)
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
    public int getPosition() { return position; }

    // Сеттеры
    public void setAssigneeIds(List<Integer> assigneeIds) { this.assigneeIds = assigneeIds; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }
    public void setChapter(int chapter) { this.chapter = chapter; }
    public void setPosition(int position) { this.position = position; }
}