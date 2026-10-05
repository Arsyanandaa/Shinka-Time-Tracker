package com.shinka.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.TreeSet;

import com.shinka.database.DatabaseManager;
import com.shinka.model.StudySession;

/**
 * Weekly streak: a week (Monday–Sunday) counts as active when it has
 * at least one session longer than MIN_SESSION_SECONDS.
 *
 * The streak is forgiving on purpose:
 * - Missing days inside a week never breaks it.
 * - The current week is "still open": if you haven't studied yet this week,
 *   the streak from previous weeks is kept until the week ends.
 */
public class StreakController {

    /** Sessions shorter than this don't count (filters accidental start/stop). */
    public static final long MIN_SESSION_SECONDS = 60;

    public record StreakInfo(
        int       currentWeeks,      // consecutive active weeks (incl. this week if active)
        int       bestWeeks,         // longest streak ever
        boolean   activeThisWeek,    // studied at least once this week
        boolean[] daysThisWeek,      // Mon..Sun, true = studied that day
        int       todayIndex,        // 0 = Monday .. 6 = Sunday
        LocalDate lastStudyDate,     // null if never studied
        int       weeksSinceLastStudy, // 0 if studied this week
        int       breakWeeksBefore   // weeks off right before the current streak began (0 = none)
    ) {
        public boolean hasAnyStudy() { return lastStudyDate != null; }
    }

    public StreakInfo getStreak() {
        return compute(DatabaseManager.getAllSessions(), LocalDate.now());
    }

    /** Pure function so it can be tested without the database. */
    public static StreakInfo compute(Collection<StudySession> sessions, LocalDate today) {
        LocalDate thisWeek = weekStart(today);

        TreeSet<LocalDate> studyDays = new TreeSet<>();
        for (StudySession s : sessions) {
            if (s.getDurationSeconds() < MIN_SESSION_SECONDS) continue;
            try {
                LocalDate d = LocalDate.parse(s.getDate());
                if (!d.isAfter(today)) studyDays.add(d);
            } catch (Exception ignored) { }
        }

        TreeSet<LocalDate> activeWeeks = new TreeSet<>();
        for (LocalDate d : studyDays) activeWeeks.add(weekStart(d));

        boolean activeThisWeek = activeWeeks.contains(thisWeek);

        // Current streak: start from this week if active, otherwise from last week
        LocalDate cursor = activeThisWeek ? thisWeek : thisWeek.minusWeeks(1);
        int current = 0;
        while (activeWeeks.contains(cursor)) {
            current++;
            cursor = cursor.minusWeeks(1);
        }

        // Break before the current streak (cursor is now the first inactive week)
        int breakWeeks = 0;
        if (current > 0) {
            LocalDate streakStart = cursor.plusWeeks(1);
            LocalDate previous = activeWeeks.lower(streakStart);
            if (previous != null) {
                breakWeeks = (int) ChronoUnit.WEEKS.between(previous, streakStart) - 1;
            }
        }

        // Best streak ever
        int best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate w : activeWeeks) {
            run = (prev != null && prev.plusWeeks(1).equals(w)) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = w;
        }

        // Days of this week
        boolean[] days = new boolean[7];
        for (int i = 0; i < 7; i++) days[i] = studyDays.contains(thisWeek.plusDays(i));

        LocalDate last = studyDays.isEmpty() ? null : studyDays.last();
        int weeksSince = last == null ? 0
            : (int) ChronoUnit.WEEKS.between(weekStart(last), thisWeek);

        return new StreakInfo(
            current, best, activeThisWeek, days,
            today.getDayOfWeek().getValue() - 1,
            last, weeksSince, breakWeeks
        );
    }

    private static LocalDate weekStart(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}