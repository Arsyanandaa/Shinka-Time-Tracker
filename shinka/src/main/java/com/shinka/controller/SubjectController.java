package com.shinka.controller;

import com.shinka.database.DatabaseManager;
import com.shinka.model.Subject;

import java.util.List;

public class SubjectController {

    public Subject addSubject(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String trimmed = name.trim();
        int id = DatabaseManager.insertSubject(trimmed);
        if (id < 0) return null;
        return new Subject(id, trimmed, "", "");
    }

    public void deleteSubject(int subjectId) {
        DatabaseManager.deleteSubject(subjectId);
    }

    public void updateSubject(int id, String title, String notes) {
        DatabaseManager.updateSubject(id, title, notes);
    }

    public List<Subject> getAllSubjects() {
        return DatabaseManager.getAllSubjects();
    }
}