package com.example.stuid.models;

import java.util.List;

public class SubtaskColumn {
    private int id;
    private int taskId;
    private String name;
    private List<Subtask> subtasks;

    public SubtaskColumn(int id, int taskId, String name, List<Subtask> subtasks) {
        this.id = id;
        this.taskId = taskId;
        this.name = name;
        this.subtasks = subtasks;
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

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }
}
