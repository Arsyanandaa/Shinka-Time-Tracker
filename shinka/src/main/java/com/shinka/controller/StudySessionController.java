package com.shinka.controller;

import com.shinka.database.DatabaseManager;
import com.shinka.model.StudySession;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

public class StudySessionController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StudySession saveSession(int subjectId, String title,
                                    String notes, long durationSeconds) {
        String today = LocalDate.now().format(DATE_FMT);

        StudySession session = new StudySession(
                subjectId,
                title == null ? "" : title.trim(),
                notes == null ? "" : notes.trim(),
                today,
                durationSeconds   // simpan detik apa adanya
        );

        int id = DatabaseManager.insertSession(session);
        session.setId(id);
        return session;
    }

    public void updateSession(int sessionId, String title, String notes) {
        DatabaseManager.updateSession(
                sessionId,
                title == null ? "" : title.trim(),
                notes == null ? "" : notes.trim()
        );
    }

    public void deleteSession(int sessionId) {
        DatabaseManager.deleteSession(sessionId);
    }

    public List<StudySession> getAllSessions() {
        return DatabaseManager.getAllSessions();
    }

    public int getSecondsToday() {
        String today = LocalDate.now().format(DATE_FMT);
        return DatabaseManager.getSecondsToday(today);
    }

    public int getSecondsThisWeek() {
        LocalDate today     = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return DatabaseManager.getSecondsThisWeek(
                weekStart.format(DATE_FMT),
                today.format(DATE_FMT)
        );
    }

    public int getSecondsThisMonth() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return DatabaseManager.getSecondsThisMonth(yearMonth);
    }

    public int getTotalSeconds() {
        return DatabaseManager.getTotalSeconds();
    }

    public Map<String, Integer> getSecondsPerSubject() {
        return DatabaseManager.getSecondsPerSubject();
    }

    // Format detik → "10s", "1m 30s", "1h 20m", "100h", "1000h 30m"
    public static String formatSeconds(long totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;

        if (h > 0 && m == 0 && s == 0) return h + "h";
        if (h > 0 && s == 0)           return h + "h " + m + "m";
        if (h > 0)                     return h + "h " + m + "m";
        if (m > 0 && s == 0)           return m + "m";
        if (m > 0)                     return m + "m " + s + "s";
        return s + "s";
    }

    public static String formatTimer(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}