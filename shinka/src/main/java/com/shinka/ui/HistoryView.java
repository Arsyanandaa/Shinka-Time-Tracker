package com.shinka.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.shinka.controller.ProgressController;
import com.shinka.controller.ProgressController.SubjectProgress;
import com.shinka.controller.StudySessionController;
import com.shinka.model.StudySession;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Study history as a journal: sessions grouped by day, newest first,
 * with search, edit and delete.
 */
public class HistoryView extends VBox {

    private static final int DAYS_PER_PAGE = 20;

    private static final DateTimeFormatter DAY_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_FMT_YEAR =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_FMT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final StudySessionController       sessionController;
    private final ProgressController           progressController = new ProgressController();
    private final ObservableList<StudySession> sessions = FXCollections.observableArrayList();

    private Label     heading, summary;
    private TextField searchField;
    private VBox      daysBox;
    private Button    moreBtn;

    private int daysShown = DAYS_PER_PAGE;

    public HistoryView(StudySessionController sessionController) {
        this.sessionController = sessionController;
        buildUI();
        applyTheme();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void buildUI() {
        setSpacing(24);
        setPadding(new Insets(32, 40, 40, 40));
        setFillWidth(true);

        heading = label(26, FontWeight.SEMI_BOLD);
        heading.setText("History");
        summary = label(13, FontWeight.NORMAL);

        VBox titles = new VBox(4, heading, summary);
        HBox.setHgrow(titles, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Search subject, title or notes");
        searchField.setFont(Font.font(ThemeManager.FONT, 13));
        searchField.setPrefWidth(260);
        searchField.setPrefHeight(36);
        searchField.focusedProperty().addListener((o, was, is) -> styleInput(searchField));
        searchField.textProperty().addListener((o, a, b) -> {
            daysShown = DAYS_PER_PAGE;
            render();
        });

        HBox header = new HBox(16, titles, searchField);
        header.setAlignment(Pos.BOTTOM_LEFT);

        daysBox = new VBox(28);

        moreBtn = new Button("Show older sessions");
        moreBtn.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 13));
        moreBtn.setCursor(Cursor.HAND);
        moreBtn.setOnAction(e -> {
            daysShown += DAYS_PER_PAGE;
            render();
        });
        moreBtn.setOnMouseEntered(e -> styleGhost(moreBtn, true));
        moreBtn.setOnMouseExited (e -> styleGhost(moreBtn, false));

        HBox moreRow = new HBox(moreBtn);
        moreRow.setAlignment(Pos.CENTER);

        getChildren().addAll(header, daysBox, moreRow);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    public void refresh() {
        sessions.setAll(sessionController.getAllSessions());
        render();
    }

    public void applyTheme() {
        setStyle(
            "-fx-background-color: " + ThemeManager.bgMain() + ";" +
            "-fx-focus-color: " + ThemeManager.textAccent() + ";" +
            "-fx-faint-focus-color: transparent;"
        );
        primary(heading);
        muted(summary);
        styleInput(searchField);
        styleGhost(moreBtn, false);
        render();
    }

    private void render() {
        daysBox.getChildren().clear();

        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<StudySession> filtered = new ArrayList<>();
        for (StudySession s : sessions) if (q.isEmpty() || matches(s, q)) filtered.add(s);

        long total = filtered.stream().mapToLong(StudySession::getDurationSeconds).sum();
        if (sessions.isEmpty()) {
            summary.setText("Every session you record shows up here.");
        } else if (q.isEmpty()) {
            summary.setText(count(filtered.size(), "session") + " · " + fmt(total) + " logged");
        } else {
            summary.setText(count(filtered.size(), "result") + (filtered.isEmpty() ? "" : " · " + fmt(total)));
        }

        if (filtered.isEmpty()) {
            Label empty = label(13, FontWeight.NORMAL);
            empty.setText(sessions.isEmpty()
                ? "No sessions yet. Start a session from the Dashboard or the Study tab."
                : "Nothing matches \"" + searchField.getText().trim() + "\".");
            empty.setWrapText(true);
            muted(empty);
            VBox card = card(new VBox(empty));
            daysBox.getChildren().add(card);
            setMoreVisible(false);
            return;
        }

        // Group by day, keep newest-first order
        Map<String, List<StudySession>> byDay = new LinkedHashMap<>();
        for (StudySession s : filtered) byDay.computeIfAbsent(s.getDate(), k -> new ArrayList<>()).add(s);

        Map<Integer, SubjectProgress> progress = new HashMap<>();
        for (SubjectProgress p : progressController.getAll()) progress.put(p.subject().getId(), p);

        int shown = 0;
        for (Map.Entry<String, List<StudySession>> day : byDay.entrySet()) {
            if (shown++ >= daysShown) break;
            daysBox.getChildren().add(dayGroup(day.getKey(), day.getValue(), progress));
        }
        setMoreVisible(byDay.size() > daysShown);
    }

    private static boolean matches(StudySession s, String q) {
        return contains(s.getSubjectName(), q) || contains(s.getTitle(), q) || contains(s.getNotes(), q);
    }

    private static boolean contains(String text, String q) {
        return text != null && text.toLowerCase().contains(q);
    }

    // ── Day group ─────────────────────────────────────────────────────────────

    private VBox dayGroup(String date, List<StudySession> list, Map<Integer, SubjectProgress> progress) {
        long dayTotal = list.stream().mapToLong(StudySession::getDurationSeconds).sum();

        Label dayLbl = label(14, FontWeight.SEMI_BOLD);
        dayLbl.setText(dayTitle(date));
        primary(dayLbl);

        Label dayMeta = label(12, FontWeight.NORMAL);
        dayMeta.setText(fmt(dayTotal) + " · " + count(list.size(), "session"));
        muted(dayMeta);

        HBox head = new HBox(dayLbl, spacer(), dayMeta);
        head.setAlignment(Pos.BASELINE_LEFT);
        head.setPadding(new Insets(0, 4, 0, 4));

        VBox rows = new VBox(2);
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) rows.getChildren().add(divider());
            StudySession s = list.get(i);
            rows.getChildren().add(row(s, progress.get(s.getSubjectId())));
        }

        VBox card = card(rows);
        card.setPadding(new Insets(8, 12, 8, 12));
        return new VBox(10, head, card);
    }

    private HBox row(StudySession s, SubjectProgress p) {
        Node badge;
        if (p != null) {
            badge = StageBadge.create(p.stage(), p.next(), p.totalSeconds(), 32);
        } else {
            Region r = new Region();
            r.setMinSize(32, 32);
            badge = r;
        }

        Label subject = label(13, FontWeight.SEMI_BOLD);
        subject.setText(s.getSubjectName());
        primary(subject);

        boolean hasTitle = s.getTitle() != null && !s.getTitle().isBlank();
        Label title = label(13, FontWeight.NORMAL);
        title.setText(hasTitle ? s.getTitle() : "");
        primary(title);

        HBox line1 = new HBox(6, subject);
        if (hasTitle) {
            Label dot = label(13, FontWeight.NORMAL);
            dot.setText("·");
            muted(dot);
            line1.getChildren().addAll(dot, title);
        }
        line1.setAlignment(Pos.BASELINE_LEFT);

        VBox info = new VBox(2, line1);
        String notes = s.getNotes() == null ? "" : s.getNotes().trim();
        if (!notes.isEmpty()) {
            Label n = label(12, FontWeight.NORMAL);
            n.setText(notes.lines().findFirst().orElse(""));
            n.setMaxWidth(460);
            muted(n);
            info.getChildren().add(n);
        } else if (!hasTitle) {
            Label n = label(12, FontWeight.NORMAL);
            n.setText("No title or notes");
            n.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + "; -fx-font-style: italic;");
            info.getChildren().add(n);
        }
        info.setMaxWidth(Double.MAX_VALUE);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label dur = label(13, FontWeight.SEMI_BOLD);
        dur.setText(fmt(s.getDurationSeconds()));
        primary(dur);
        dur.setMinWidth(70);
        dur.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn   = ghostButton("Edit", false);
        Button deleteBtn = ghostButton("Delete", true);
        editBtn  .setOnAction(e -> openEditDialog(s));
        deleteBtn.setOnAction(e -> handleDelete(s));

        HBox row = new HBox(14, badge, info, dur, editBtn, deleteBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 8, 10, 8));
        String base = "-fx-background-radius: 8;";
        row.setStyle("-fx-background-color: transparent;" + base);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + ThemeManager.bgDarker() + ";" + base));
        row.setOnMouseExited (e -> row.setStyle("-fx-background-color: transparent;" + base));
        return row;
    }

    private static String dayTitle(String date) {
        try {
            LocalDate d = LocalDate.parse(date);
            LocalDate today = LocalDate.now();
            if (d.equals(today))              return "Today";
            if (d.equals(today.minusDays(1))) return "Yesterday";
            return d.getYear() == today.getYear() ? d.format(DAY_FMT) : d.format(DAY_FMT_YEAR);
        } catch (Exception e) {
            return date;
        }
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    private void openEditDialog(StudySession session) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit session");
        if (getScene() != null) dialog.initOwner(getScene().getWindow());

        TextField titleField = new TextField(session.getTitle());
        titleField.setPromptText("What did you work on? e.g. Writing task 2");
        input(titleField);

        TextArea notesArea = new TextArea(session.getNotes());
        notesArea.setPromptText("Anything worth remembering from this session");
        notesArea.setPrefRowCount(5);
        notesArea.setWrapText(true);
        input(notesArea);

        Label meta = label(12, FontWeight.NORMAL);
        meta.setText(shortDate(session.getDate()) + " · " + fmt(session.getDurationSeconds()));
        muted(meta);

        VBox form = new VBox(6,
            meta, gap(8),
            fieldLabel("Title"), titleField,
            gap(6),
            fieldLabel("Notes"), notesArea
        );
        form.setPrefWidth(440);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(form);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialog(pane, "Edit session", session.getSubjectName());

        styleDialogButton((Button) pane.lookupButton(saveType), "primary");
        styleDialogButton((Button) pane.lookupButton(ButtonType.CANCEL), "ghost");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            sessionController.updateSession(session.getId(), titleField.getText(), notesArea.getText());
            refresh();
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void handleDelete(StudySession session) {
        ButtonType deleteType = new ButtonType("Delete session", ButtonBar.ButtonData.OK_DONE);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "", deleteType, ButtonType.CANCEL);
        confirm.setTitle("Delete session");
        if (getScene() != null) confirm.initOwner(getScene().getWindow());

        DialogPane pane = confirm.getDialogPane();
        Label what = label(13, FontWeight.SEMI_BOLD);
        what.setText(session.getSubjectName() + " · " + shortDate(session.getDate()) + " · " +
                     fmt(session.getDurationSeconds()));
        primary(what);

        Label why = label(13, FontWeight.NORMAL);
        why.setText("This time will be removed from your stats, streak and goals. This can't be undone.");
        why.setWrapText(true);
        why.setPrefWidth(400);
        why.setMinHeight(Region.USE_PREF_SIZE);
        muted(why);

        pane.setContent(new VBox(6, what, why));
        styleDialog(pane, null, "Delete this session?");

        styleDialogButton((Button) pane.lookupButton(deleteType), "danger");
        styleDialogButton((Button) pane.lookupButton(ButtonType.CANCEL), "ghost");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == deleteType) {
            sessionController.deleteSession(session.getId());
            refresh();
        }
    }

    // ── Styling helpers (same look as SubjectsView) ───────────────────────────

    private VBox card(Node content) {
        VBox c = new VBox(content);
        c.setPadding(new Insets(18, 20, 18, 20));
        c.setStyle(ThemeManager.cardStyle());
        return c;
    }

    private Button ghostButton(String text, boolean danger) {
        Button b = new Button(text);
        b.setFont(Font.font(ThemeManager.FONT, FontWeight.NORMAL, 12));
        b.setCursor(Cursor.HAND);
        String base = "-fx-background-radius: 6; -fx-padding: 5 12;";
        String idle = "-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.textSecondary() + ";" + base;
        String hover = danger
            ? "-fx-background-color: " + ThemeManager.dangerSoft() + "; -fx-text-fill: " + ThemeManager.danger() + ";" + base
            : "-fx-background-color: " + ThemeManager.bgCard() + "; -fx-text-fill: " + ThemeManager.textPrimary() + ";" + base;
        b.setStyle(idle);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited (e -> b.setStyle(idle));
        return b;
    }

    private void styleGhost(Button b, boolean hover) {
        b.setStyle(
            "-fx-background-color: " + (hover ? ThemeManager.bgDarker() : "transparent") + ";" +
            "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
            "-fx-border-color: " + ThemeManager.border() + ";" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 9 18;"
        );
    }

    private void setMoreVisible(boolean show) {
        moreBtn.getParent().setVisible(show);
        moreBtn.getParent().setManaged(show);
    }

    private void input(TextInputControl c) {
        c.setFont(Font.font(ThemeManager.FONT, 13));
        c.focusedProperty().addListener((o, was, is) -> styleInput(c));
        styleInput(c);
    }

    private void styleInput(TextInputControl input) {
        String borderColor = input.isFocused() ? ThemeManager.textAccent() : ThemeManager.borderInput();
        input.setStyle(
            "-fx-control-inner-background: " + ThemeManager.bgInput() + ";" +
            "-fx-background-color: " + borderColor + ", " + ThemeManager.bgInput() + ";" +
            "-fx-background-insets: 0, 1;" +
            "-fx-background-radius: 8, 7;" +
            "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
            "-fx-prompt-text-fill: " + ThemeManager.textSecondary() + ";" +
            "-fx-highlight-fill: " + ThemeManager.heatLevel(2) + ";" +
            "-fx-highlight-text-fill: " + ThemeManager.textPrimary() + ";" +
            (input instanceof TextArea ? "-fx-padding: 2;" : "-fx-padding: 8 12;")
        );
    }

    private void styleDialog(DialogPane pane, String eyebrow, String title) {
        Label t = label(17, FontWeight.SEMI_BOLD);
        t.setText(title);
        t.setWrapText(true);
        primary(t);
        VBox head = new VBox(2);
        if (eyebrow != null) {
            Label e = label(12, FontWeight.NORMAL);
            e.setText(eyebrow);
            muted(e);
            head.getChildren().add(e);
        }
        head.getChildren().add(t);
        head.setPadding(new Insets(22, 24, 4, 24));
        pane.setHeader(head);
        pane.setGraphic(null);

        Node content = pane.getContent();
        if (content instanceof Region r) r.setPadding(new Insets(10, 24, 8, 24));

        pane.setStyle(
            "-fx-background-color: " + ThemeManager.bgCard() + ";" +
            "-fx-font-family: '" + ThemeManager.FONT + "';" +
            "-fx-focus-color: " + ThemeManager.textAccent() + ";" +
            "-fx-faint-focus-color: transparent;" +
            "-fx-padding: 0 16 14 0;"
        );
    }

    private void styleDialogButton(Button b, String kind) {
        if (b == null) return;
        b.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 13));
        b.setCursor(Cursor.HAND);
        String base = "-fx-background-radius: 8; -fx-padding: 8 16;";
        switch (kind) {
            case "primary" -> b.setStyle(
                "-fx-background-color: " + ThemeManager.btnPrimaryBg() + ";" +
                "-fx-text-fill: " + ThemeManager.btnPrimaryFg() + ";" + base);
            case "danger" -> b.setStyle(
                "-fx-background-color: " + ThemeManager.danger() + ";" +
                "-fx-text-fill: " + ThemeManager.bgCard() + ";" + base);
            default -> b.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + ThemeManager.textSecondary() + ";" +
                "-fx-border-color: " + ThemeManager.border() + ";" +
                "-fx-border-radius: 8;" + base);
        }
    }

    // ── Small helpers ─────────────────────────────────────────────────────────

    private static String fmt(long seconds) {
        return seconds <= 0 ? "0m" : StudySessionController.formatSeconds(seconds);
    }

    private static String count(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }

    private static String shortDate(String date) {
        try { return LocalDate.parse(date).format(SHORT_FMT); }
        catch (Exception e) { return date; }
    }

    private static Label label(double size, FontWeight weight) {
        Label l = new Label();
        l.setFont(Font.font(ThemeManager.FONT, weight, size));
        return l;
    }

    private static Label fieldLabel(String text) {
        Label l = label(12, FontWeight.SEMI_BOLD);
        l.setText(text);
        muted(l);
        return l;
    }

    private static void primary(Label l) {
        l.setStyle("-fx-text-fill: " + ThemeManager.textPrimary() + ";");
    }

    private static void muted(Label l) {
        l.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + ";");
    }

    private Region divider() {
        Region d = new Region();
        d.setMinHeight(1);
        d.setMaxHeight(1);
        d.setStyle("-fx-background-color: " + ThemeManager.separator() + ";");
        VBox.setMargin(d, new Insets(0, 8, 0, 54));
        return d;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static Region gap(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }
}