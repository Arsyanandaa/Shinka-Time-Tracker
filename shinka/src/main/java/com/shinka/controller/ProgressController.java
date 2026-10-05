package com.shinka.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.shinka.database.DatabaseManager;
import com.shinka.model.StudySession;
import com.shinka.model.Subject;

/**
 * Per-subject progress: evolution stage, weekly goal, and target pace.
 */
public class ProgressController {

    // ── Evolution stages (進化) ───────────────────────────────────────────────

    public record Stage(int index, String kanji, String name, long minSeconds) {}

    public static final List<Stage> STAGES = List.of(
        new Stage(0, "種", "Seed",     0),
        new Stage(1, "芽", "Sprout",   1L   * 3600),
        new Stage(2, "苗", "Seedling", 10L  * 3600),
        new Stage(3, "木", "Tree",     30L  * 3600),
        new Stage(4, "林", "Grove",    100L * 3600),
        new Stage(5, "森", "Forest",   300L * 3600)
    );

    public static Stage stageFor(long totalSeconds) {
        Stage current = STAGES.get(0);
        for (Stage s : STAGES) if (totalSeconds >= s.minSeconds()) current = s;
        return current;
    }

    public static Stage nextStage(Stage stage) {
        int i = stage.index() + 1;
        return i < STAGES.size() ? STAGES.get(i) : null;
    }

    // ── Target status ─────────────────────────────────────────────────────────

    public enum TargetStatus { NONE, ON_TRACK, BEHIND, REACHED, OVERDUE }

    /** Pace is compared against the average of the last PACE_WINDOW_DAYS days. */
    public static final int PACE_WINDOW_DAYS = 14;

    public record SubjectProgress(
        Subject      subject,
        long         totalSeconds,
        long         weekSeconds,       // Monday..today
        Stage        stage,
        Stage        next,              // null at the final stage
        double       stageProgress,     // 0..1 towards next stage (1 at final stage)
        // Target
        TargetStatus targetStatus,
        long         daysLeft,          // includes today; 0 if none/overdue
        long         remainingSeconds,  // to reach the target total
        long         neededPerDay,      // seconds/day needed from today
        long         recentPerDay       // average seconds/day over the last 14 days
    ) {
        public long weeklyGoalSeconds() { return subject.getWeeklyGoalMinutes() * 60L; }
        public long targetSeconds()     { return subject.getTargetMinutes() * 60L; }
        public boolean weeklyDone()     { return subject.hasWeeklyGoal() && weekSeconds >= weeklyGoalSeconds(); }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public List<SubjectProgress> getAll() {
        return compute(DatabaseManager.getAllSubjects(), DatabaseManager.getAllSessions(), LocalDate.now());
    }

    /** Progress for a given list of subjects (e.g. the list shown in Subjects). */
    public List<SubjectProgress> getFor(List<Subject> subjects) {
        return compute(subjects, DatabaseManager.getAllSessions(), LocalDate.now());
    }

    /** Most recent session (sessions come ordered newest first). */
    public Optional<StudySession> getLastSession() {
        List<StudySession> all = DatabaseManager.getAllSessions();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    /** Pure function so it can be tested without the database. */
    public static List<SubjectProgress> compute(List<Subject> subjects,
                                                List<StudySession> sessions,
                                                LocalDate today) {
        LocalDate weekStart   = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate recentStart = today.minusDays(PACE_WINDOW_DAYS - 1);

        Map<Integer, long[]> sums = new HashMap<>(); // [total, week, recent]
        for (StudySession s : sessions) {
            long[] a = sums.computeIfAbsent(s.getSubjectId(), k -> new long[3]);
            long sec = s.getDurationSeconds();
            a[0] += sec;
            try {
                LocalDate d = LocalDate.parse(s.getDate());
                if (!d.isBefore(weekStart)   && !d.isAfter(today)) a[1] += sec;
                if (!d.isBefore(recentStart) && !d.isAfter(today)) a[2] += sec;
            } catch (Exception ignored) { }
        }

        List<SubjectProgress> out = new ArrayList<>();
        for (Subject sub : subjects) {
            long[] a = sums.getOrDefault(sub.getId(), new long[3]);
            long total = a[0], week = a[1];
            long recentPerDay = a[2] / PACE_WINDOW_DAYS;

            Stage stage = stageFor(total);
            Stage next  = nextStage(stage);
            double prog = next == null ? 1.0
                : (double) (total - stage.minSeconds()) / (next.minSeconds() - stage.minSeconds());

            TargetStatus status = TargetStatus.NONE;
            long daysLeft = 0, remaining = 0, needed = 0;

            if (sub.hasTarget()) {
                long target = sub.getTargetMinutes() * 60L;
                remaining = Math.max(0, target - total);
                LocalDate due = null;
                try { due = LocalDate.parse(sub.getTargetDate()); } catch (Exception ignored) { }

                if (remaining == 0) {
                    status = TargetStatus.REACHED;
                } else if (due == null || due.isBefore(today)) {
                    status = TargetStatus.OVERDUE;
                } else {
                    daysLeft = ChronoUnit.DAYS.between(today, due) + 1;
                    needed   = (long) Math.ceil((double) remaining / daysLeft);
                    status   = recentPerDay >= needed ? TargetStatus.ON_TRACK : TargetStatus.BEHIND;
                }
            }

            out.add(new SubjectProgress(sub, total, week, stage, next, Math.min(1.0, prog),
                                        status, daysLeft, remaining, needed, recentPerDay));
        }
        return out;
    }
}
