package com.example.stuid.models;

public class Project {
    private int Id;
    private String Name;
    private String Description;
    private boolean IsPublic;
    private String Creator;

    public Project() {
    }

    public Project(int id, String name, String description, boolean isPublic, String creator) {
        Id = id;
        Name = name;
        Description = description;
        IsPublic = isPublic;
        Creator = creator;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public boolean isPublic() {
        return IsPublic;
    }

    public void setPublic(boolean aPublic) {
        IsPublic = aPublic;
    }

    public String getCreator() {
        return Creator;
    }

    public void setCreator(String creator) {
        Creator = creator;
    }
}
