package com.example.stuid.models;

public class Employee {
    private String fullName;
    private String email;
    private String about;

    public Employee(String fullName, String email, String about) {
        this.fullName = fullName;
        this.email = email;
        this.about = about;
    }

    // Геттеры
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getAbout() { return about; }
}
