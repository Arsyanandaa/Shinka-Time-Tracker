package com.shinka.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.shinka.model.StudySession;
import com.shinka.model.Subject;

public class DatabaseManager {

    private static final String DB_URL =
            "jdbc:sqlite:" + System.getProperty("user.home") + "/shinka.db";

    // ── Initialization ────────────────────────────────────────────────────────

    public static void createTablesIfNeeded() {
        String createSubjects =
            "CREATE TABLE IF NOT EXISTS subjects (" +
            "  id    INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name  TEXT NOT NULL," +
            "  title TEXT DEFAULT ''," +
            "  notes TEXT DEFAULT ''" +
            ");";

        String createSessions =
            "CREATE TABLE IF NOT EXISTS study_sessions (" +
            "  id               INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  subject_id       INTEGER NOT NULL," +
            "  title            TEXT," +
            "  notes            TEXT," +
            "  date             TEXT NOT NULL," +
            "  duration_seconds INTEGER NOT NULL DEFAULT 0," +
            "  FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE" +
            ");";

        // Migration: tambah kolom title & notes ke subjects jika belum ada
        String addTitleCol = "ALTER TABLE subjects ADD COLUMN title TEXT DEFAULT ''";
        String addNotesCol = "ALTER TABLE subjects ADD COLUMN notes TEXT DEFAULT ''";

        try (Connection conn = connect();
             Statement  stmt = conn.createStatement()) {
            stmt.execute(createSubjects);
            stmt.execute(createSessions);
            // Abaikan error kalau kolom sudah ada
            try { stmt.execute(addTitleCol); } catch (SQLException ignored) {}
            try { stmt.execute(addNotesCol); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ── Subject CRUD ──────────────────────────────────────────────────────────

    public static int insertSubject(String name) {
        String sql = "INSERT INTO subjects (name) VALUES (?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("insertSubject error: " + e.getMessage());
        }
        return -1;
    }

    public static List<Subject> getAllSubjects() {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT id, name, title, notes FROM subjects ORDER BY name";
        try (Connection conn = connect();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Subject(
                    rs.getInt   ("id"),
                    rs.getString("name"),
                    rs.getString("title"),
                    rs.getString("notes")
                ));
            }
        } catch (SQLException e) {
            System.err.println("getAllSubjects error: " + e.getMessage());
        }
        return list;
    }

    public static void updateSubject(int id, String title, String notes) {
        String sql = "UPDATE subjects SET title = ?, notes = ? WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, notes);
            ps.setInt   (3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateSubject error: " + e.getMessage());
        }
    }

    public static void deleteSubject(int subjectId) {
        String fk  = "PRAGMA foreign_keys = ON";
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = connect();
             Statement  pragma = conn.createStatement();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            pragma.execute(fk);
            ps.setInt(1, subjectId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteSubject error: " + e.getMessage());
        }
    }

    // ── StudySession CRUD ─────────────────────────────────────────────────────

    public static int insertSession(StudySession session) {
        String sql =
            "INSERT INTO study_sessions (subject_id, title, notes, date, duration_seconds)" +
            " VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, session.getSubjectId());
            ps.setString(2, session.getTitle());
            ps.setString(3, session.getNotes());
            ps.setString(4, session.getDate());
            ps.setLong  (5, session.getDurationSeconds());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("insertSession error: " + e.getMessage());
        }
        return -1;
    }

    public static List<StudySession> getAllSessions() {
        List<StudySession> list = new ArrayList<>();
        String sql =
            "SELECT ss.id, ss.subject_id, sub.name AS subject_name," +
            "       ss.title, ss.notes, ss.date, ss.duration_seconds" +
            "  FROM study_sessions ss" +
            "  JOIN subjects sub ON ss.subject_id = sub.id" +
            " ORDER BY ss.date DESC, ss.id DESC";
        try (Connection conn = connect();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("getAllSessions error: " + e.getMessage());
        }
        return list;
    }

    public static void updateSession(int sessionId, String title, String notes) {
        String sql = "UPDATE study_sessions SET title = ?, notes = ? WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, notes);
            ps.setInt   (3, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateSession error: " + e.getMessage());
        }
    }

    public static void deleteSession(int sessionId) {
        String sql = "DELETE FROM study_sessions WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteSession error: " + e.getMessage());
        }
    }

    // ── Statistics Queries ────────────────────────────────────────────────────

    public static int getSecondsToday(String today) {
        return sumSeconds("WHERE date = '" + today + "'");
    }

    public static int getSecondsThisWeek(String weekStart, String weekEnd) {
        return sumSeconds("WHERE date BETWEEN '" + weekStart + "' AND '" + weekEnd + "'");
    }

    public static int getSecondsThisMonth(String yearMonth) {
        return sumSeconds("WHERE date LIKE '" + yearMonth + "-%'");
    }

    public static int getTotalSeconds() {
        return sumSeconds("");
    }

    public static Map<String, Integer> getSecondsPerSubject() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql =
            "SELECT sub.name, SUM(ss.duration_seconds) AS total" +
            "  FROM study_sessions ss" +
            "  JOIN subjects sub ON ss.subject_id = sub.id" +
            " GROUP BY sub.name" +
            " ORDER BY total DESC";
        try (Connection conn = connect();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("name"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            System.err.println("getSecondsPerSubject error: " + e.getMessage());
        }
        return map;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int sumSeconds(String whereClause) {
        String sql = "SELECT COALESCE(SUM(duration_seconds), 0) FROM study_sessions " + whereClause;
        try (Connection conn = connect();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("sumSeconds error: " + e.getMessage());
        }
        return 0;
    }

    private static StudySession mapRow(ResultSet rs) throws SQLException {
        return new StudySession(
            rs.getInt   ("id"),
            rs.getInt   ("subject_id"),
            rs.getString("subject_name"),
            rs.getString("title"),
            rs.getString("notes"),
            rs.getString("date"),
            rs.getLong  ("duration_seconds")
        );
    }
}