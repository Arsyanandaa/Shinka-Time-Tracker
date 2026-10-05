package com.shinka.model;

public class Subject {

    private int    id;
    private String name;
    private String title;
    private String notes;

    // Goals (0 / "" = not set)
    private int    weeklyGoalMinutes;
    private int    targetMinutes;
    private String targetDate;          // "YYYY-MM-DD"

    public Subject(String name) {
        this.name  = name;
        this.title = "";
        this.notes = "";
        this.targetDate = "";
    }

    public Subject(int id, String name, String title, String notes) {
        this(id, name, title, notes, 0, 0, "");
    }

    public Subject(int id, String name, String title, String notes,
                   int weeklyGoalMinutes, int targetMinutes, String targetDate) {
        this.id    = id;
        this.name  = name;
        this.title = title == null ? "" : title;
        this.notes = notes == null ? "" : notes;
        this.weeklyGoalMinutes = Math.max(0, weeklyGoalMinutes);
        this.targetMinutes     = Math.max(0, targetMinutes);
        this.targetDate        = targetDate == null ? "" : targetDate;
    }

    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }
    public String getName()                 { return name; }
    public void setName(String name)        { this.name = name; }
    public String getTitle()                { return title; }
    public void setTitle(String title)      { this.title = title; }
    public String getNotes()                { return notes; }
    public void setNotes(String notes)      { this.notes = notes; }

    public int getWeeklyGoalMinutes()             { return weeklyGoalMinutes; }
    public void setWeeklyGoalMinutes(int minutes) { this.weeklyGoalMinutes = Math.max(0, minutes); }
    public int getTargetMinutes()                 { return targetMinutes; }
    public void setTargetMinutes(int minutes)     { this.targetMinutes = Math.max(0, minutes); }
    public String getTargetDate()                 { return targetDate; }
    public void setTargetDate(String date)        { this.targetDate = date == null ? "" : date; }

    public boolean hasWeeklyGoal() { return weeklyGoalMinutes > 0; }
    public boolean hasTarget()     { return targetMinutes > 0 && targetDate != null && !targetDate.isBlank(); }

    @Override
    public String toString() { return name; }
}
