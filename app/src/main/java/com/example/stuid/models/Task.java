package com.example.stuid.models;

public class Task {
    private String number;
    private String name;
    private String assignee;
    private String status;

    public Task(String number, String name, String assignee, String status) {
        this.number = number;
        this.name = name;
        this.assignee = assignee;
        this.status = status;
    }

    // Геттеры
    public String getNumber() { return number; }
    public String getName() { return name; }
    public String getAssignee() { return assignee; }
    public String getStatus() { return status; }
}