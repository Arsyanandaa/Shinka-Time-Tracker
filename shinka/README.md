# 🌱 Shinka – Time to Master

A clean, minimal desktop learning time tracker built with **Java 21 + JavaFX + SQLite**.

Track your study sessions, manage subjects, write notes, and watch your progress grow.

---

## 📁 Project Structure

```
shinka/
├── pom.xml                          ← Maven build file
└── src/main/java/com/shinka/
    ├── model/
    │   ├── Subject.java             ← Data class for subjects
    │   └── StudySession.java        ← Data class for study sessions
    ├── database/
    │   └── DatabaseManager.java     ← All SQLite CRUD operations
    ├── controller/
    │   ├── SubjectController.java   ← Business logic for subjects
    │   └── StudySessionController.java ← Business logic + stats
    ├── ui/
    │   ├── DashboardView.java       ← Stats overview screen
    │   ├── SubjectsView.java        ← Manage subjects
    │   ├── StudySessionView.java    ← Timer + session recorder
    │   └── HistoryView.java         ← Browse, edit, delete sessions
    └── main/
        └── MainApp.java             ← JavaFX entry point
```

---

## ✅ Prerequisites

| Tool    | Version  | Check command      |
|---------|----------|--------------------|
| Java    | 17 or 21 | `java --version`   |
| Maven   | 3.8+     | `mvn --version`    |

Maven will automatically download JavaFX and SQLite JDBC on first build.

---

## 🚀 How to Run

### Option A — Maven (recommended)

```bash
# 1. Clone or unzip the project
cd shinka

# 2. Run directly with the JavaFX Maven plugin
mvn javafx:run
```

### Option B — Build a fat JAR, then run

```bash
# 1. Package everything into a single runnable JAR
mvn clean package -q

# 2. Run the fat JAR
java -jar target/shinka-1.0.0.jar
```

> **Note:** The fat JAR bundles JavaFX and SQLite inside it, so no extra flags are needed.

### Option C — IDE (IntelliJ IDEA / Eclipse)

1. Open the project root folder as a Maven project.
2. Wait for Maven to download dependencies.
3. Run `com.shinka.main.MainApp` as a Java application.

---

## 🗄 Database

- SQLite database file is created automatically at: `~/shinka.db`  
  (i.e., your home directory — no setup required).
- Tables `subjects` and `study_sessions` are created on first launch.

### Schema

```sql
CREATE TABLE subjects (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL
);

CREATE TABLE study_sessions (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id       INTEGER NOT NULL,
    title            TEXT,
    notes            TEXT,
    date             TEXT NOT NULL,          -- "YYYY-MM-DD"
    duration_minutes INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);
```

---

## 🖥 Features

| Feature | Description |
|---|---|
| **Dashboard** | Today / week / month / all-time stats + per-subject breakdown |
| **Subjects** | Add and delete learning subjects |
| **Study Timer** | Live HH:MM:SS timer with Start / Stop & Save |
| **Notes** | Title + multi-line notes saved with every session |
| **History** | Table view of all sessions — edit title/notes, delete sessions |

---

## 💡 Tips

- Press **Enter** in the Subject name field to add it quickly.
- You can start typing notes *while* the timer is running.
- Deleting a subject also deletes all its study sessions (CASCADE).
- The database file (`~/shinka.db`) can be opened with any SQLite browser (e.g. [DB Browser for SQLite](https://sqlitebrowser.org/)).

---

## 🏗 Architecture

```
UI Layer  (com.shinka.ui)
    │  calls
    ▼
Controller Layer  (com.shinka.controller)
    │  calls
    ▼
Database Layer  (com.shinka.database)
    │  reads/writes
    ▼
SQLite file  (~/ shinka.db)
```

The UI never talks to the database directly — always through a controller.  
Controllers hold all business logic (validation, date formatting, stats calculations).  
`DatabaseManager` contains only pure SQL — no business logic.

---

## 📦 Dependencies

| Library | Version | Purpose |
|---|---|---|
| [JavaFX](https://openjfx.io/) | 21.0.2 | Desktop GUI framework |
| [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) | 3.45.1.0 | SQLite driver (no server needed) |

Both are fetched automatically by Maven from Maven Central.

---

*Built with ❤️ — Keep studying, keep growing! 🌱*
