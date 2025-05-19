package com.example.stuid.models;

public class Employee {
    private int EmployeeId;
    private String LastName;
    private String FirstName;
    private String MiddleName;
    private String Email;
    private String Description;
    private String Photo;

    public Employee() {
    }

    public Employee(String lastName, String firstName, String middleName, String email, String description) {
        LastName = lastName;
        FirstName = firstName;
        MiddleName = middleName;
        Email = email;
        Description = description;
    }

    public int getEmployeeId() {
        return EmployeeId;
    }

    public String getFullName() {
        return LastName + " " + FirstName + " " + MiddleName;
    }

    public String getEmail() {
        return Email;
    }

    public String getDescription() {
        return Description;
    }

    public String getPhoto() { return Photo; }

    public void setEmployeeId(int employeeId) {
        EmployeeId = employeeId;
    }

    public void setLastName(String lastName) {
        LastName = lastName;
    }

    public void setFirstName(String firstName) {
        FirstName = firstName;
    }

    public void setMiddleName(String middleName) {
        MiddleName = middleName;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public void setPhoto(String photo) { Photo = photo; }
}
