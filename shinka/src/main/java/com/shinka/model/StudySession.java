package com.shinka.model;

public class StudySession {

    private int    id;
    private int    subjectId;
    private String subjectName;
    private String title;
    private String notes;
    private String date;
    private long   durationSeconds;

    public StudySession(int subjectId, String title, String notes,
                        String date, long durationSeconds) {
        this.subjectId       = subjectId;
        this.title           = title;
        this.notes           = notes;
        this.date            = date;
        this.durationSeconds = durationSeconds;
    }

    public StudySession(int id, int subjectId, String subjectName,
                        String title, String notes,
                        String date, long durationSeconds) {
        this.id              = id;
        this.subjectId       = subjectId;
        this.subjectName     = subjectName;
        this.title           = title;
        this.notes           = notes;
        this.date            = date;
        this.durationSeconds = durationSeconds;
    }

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public int getSubjectId()                       { return subjectId; }
    public void setSubjectId(int subjectId)         { this.subjectId = subjectId; }
    public String getSubjectName()                  { return subjectName; }
    public void setSubjectName(String name)         { this.subjectName = name; }
    public String getTitle()                        { return title; }
    public void setTitle(String title)              { this.title = title; }
    public String getNotes()                        { return notes; }
    public void setNotes(String notes)              { this.notes = notes; }
    public String getDate()                         { return date; }
    public void setDate(String date)                { this.date = date; }
    public long getDurationSeconds()                { return durationSeconds; }
    public void setDurationSeconds(long s)          { this.durationSeconds = s; }

    public String getFormattedDuration() {
        long h = durationSeconds / 3600;
        long m = (durationSeconds % 3600) / 60;
        long s = durationSeconds % 60;

        if (h > 0 && m == 0) return h + "h";
        if (h > 0)           return h + "h " + m + "m";
        if (m > 0 && s == 0) return m + "m";
        if (m > 0)           return m + "m " + s + "s";
        return s + "s";
    }
}