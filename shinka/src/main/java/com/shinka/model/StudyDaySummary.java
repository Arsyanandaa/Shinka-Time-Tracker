package com.shinka.model;

import java.util.List;

/**
 * Represents study activity summary for a single day.
 * Used by the heatmap to determine color intensity and show details on click.
 */
public class StudyDaySummary {

    private String date;           // "YYYY-MM-DD"
    private long   totalSeconds;   // total study time for this day
    private List<StudySession> sessions; // all sessions on this day

    public StudyDaySummary(String date, long totalSeconds, List<StudySession> sessions) {
        this.date         = date;
        this.totalSeconds = totalSeconds;
        this.sessions     = sessions;
    }

    public String getDate()                        { return date; }
    public long getTotalSeconds()                  { return totalSeconds; }
    public List<StudySession> getSessions()        { return sessions; }

    /**
     * Heatmap intensity level based on study time:
     * 0 = no study
     * 1 = 1s – 30min
     * 2 = 30min – 60min
     * 3 = 60min+
     */
    public int getIntensityLevel() {
        if (totalSeconds <= 0)          return 0;
        if (totalSeconds < 1800)        return 1; // < 30 min
        if (totalSeconds < 3600)        return 2; // 30–60 min
        return 3;                                  // 60 min+
    }
}