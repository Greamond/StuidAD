package com.example.stuid.models;

public class Subtask {
    private int id;
    private int taskId;
    private String name;
    private String description;
    private int responsibleId;
    private int creatorId;
    private int chapterId;
    private int position;

    public Subtask(int id, int taskId, String name, String description, int responsibleId,int creatorId, int chapterId) {
        this.id = id;
        this.taskId = taskId;
        this.name = name;
        this.description = description;
        this.responsibleId = responsibleId;
        this.creatorId = creatorId;
        this.chapterId = chapterId;
    }

    public Subtask(int id, int taskId, String name, String description, int responsibleId,int chapterId) {
        this.id = id;
        this.taskId = taskId;
        this.name = name;
        this.description = description;
        this.responsibleId = responsibleId;
        this.chapterId = chapterId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getResponsibleId() {
        return responsibleId;
    }

    public void setResponsibleId(int responsibleId) {
        this.responsibleId = responsibleId;
    }

    public int getChapterId() {
        return chapterId;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }
}
