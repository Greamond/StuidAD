package com.example.stuid.models;

public class Project {
    private String number;
    private String name;
    private String creator;

    public Project(String number, String name, String creator) {
        this.number = number;
        this.name = name;
        this.creator = creator;
    }

    // Геттеры
    public String getNumber() { return number; }
    public String getName() { return name; }
    public String getCreator() { return creator; }
}
