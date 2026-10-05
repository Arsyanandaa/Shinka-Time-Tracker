package com.shinka.model;

public class Subject {

    private int    id;
    private String name;
    private String title;
    private String notes;

    public Subject(String name) {
        this.name  = name;
        this.title = "";
        this.notes = "";
    }

    public Subject(int id, String name, String title, String notes) {
        this.id    = id;
        this.name  = name;
        this.title = title == null ? "" : title;
        this.notes = notes == null ? "" : notes;
    }

    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }
    public String getName()                 { return name; }
    public void setName(String name)        { this.name = name; }
    public String getTitle()                { return title; }
    public void setTitle(String title)      { this.title = title; }
    public String getNotes()                { return notes; }
    public void setNotes(String notes)      { this.notes = notes; }

    @Override
    public String toString() { return name; }
}