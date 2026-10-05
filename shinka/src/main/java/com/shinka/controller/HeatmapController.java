package com.shinka.controller;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.shinka.database.DatabaseManager;
import com.shinka.model.StudyDaySummary;
import com.shinka.model.StudySession;

/**
 * HeatmapController provides data for the heatmap calendar.
 * Fetches per-day study summaries for the past 365 days.
 */
public class HeatmapController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Returns a map of date → StudyDaySummary for the past 365 days.
     * Days with no study will NOT be in the map.
     */
    public Map<String, StudyDaySummary> getYearSummary() {
        // Get all sessions from DB
        List<StudySession> allSessions = DatabaseManager.getAllSessions();

        // Group sessions by date
        Map<String, List<StudySession>> byDate = new LinkedHashMap<>();
        for (StudySession s : allSessions) {
            byDate.computeIfAbsent(s.getDate(), k -> new ArrayList<>()).add(s);
        }

        // Build summary map
        Map<String, StudyDaySummary> summaryMap = new HashMap<>();
        for (Map.Entry<String, List<StudySession>> entry : byDate.entrySet()) {
            String date     = entry.getKey();
            List<StudySession> sessions = entry.getValue();
            long total = sessions.stream()
                                 .mapToLong(StudySession::getDurationSeconds)
                                 .sum();
            summaryMap.put(date, new StudyDaySummary(date, total, sessions));
        }

        return summaryMap;
    }

    /**
     * Returns summary for a specific date (or null if no study that day).
     */
    public StudyDaySummary getSummaryForDate(String date) {
        Map<String, StudyDaySummary> map = getYearSummary();
        return map.get(date);
    }
}