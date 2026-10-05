package com.shinka.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.shinka.controller.ProgressController;
import com.shinka.controller.ProgressController.SubjectProgress;
import com.shinka.controller.StudySessionController;
import com.shinka.controller.SubjectController;
import com.shinka.model.Subject;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SubjectsView extends VBox {

    private final SubjectController       subjectController;
    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private final ProgressController      progressController = new ProgressController();

    // Header
    private Label heading, hint;

    // Add form
    private VBox      addCard;
    private TextField nameField;
    private Button    addBtn;
    private SVGPath   plusIcon;
    private Label     statusLabel;

    // List
    private VBox  listCard;
    private Label listTitle, countLabel;
    private VBox  rowsBox;

    public SubjectsView(SubjectController subjectController) {
        this.subjectController = subjectController;
        buildUI();
        subjects.addListener((ListChangeListener<Subject>) c -> renderRows());
        loadSubjects();
        applyTheme();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void buildUI() {
        setSpacing(24);
        setPadding(new Insets(32, 40, 40, 40));
        setFillWidth(true);

        // Header
        heading = label(26, FontWeight.SEMI_BOLD);
        heading.setText("Subjects");
        hint = label(13, FontWeight.NORMAL);
        hint.setText("Create subjects, then give each one a title and notes.");
        VBox header = new VBox(4, heading, hint);

        // Add form
        nameField = new TextField();
        nameField.setPromptText("New subject, e.g. IELTS, Calculus, Spring Boot");
        nameField.setFont(Font.font(ThemeManager.FONT, 13));
        nameField.setPrefHeight(38);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameField.setOnAction(e -> handleAdd());
        nameField.focusedProperty().addListener((o, was, is) -> styleInput(nameField));
        nameField.textProperty().addListener((o, a, b) -> setStatus(""));

        plusIcon = new SVGPath();
        plusIcon.setContent("M5 0 H7 V5 H12 V7 H7 V12 H5 V7 H0 V5 H5 Z");

        addBtn = new Button("Add subject", plusIcon);
        addBtn.setGraphicTextGap(8);
        addBtn.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 13));
        addBtn.setPrefHeight(38);
        addBtn.setCursor(Cursor.HAND);
        addBtn.setOnAction(e -> handleAdd());
        addBtn.setOnMouseEntered(e -> styleAddBtn(true));
        addBtn.setOnMouseExited (e -> styleAddBtn(false));

        HBox addRow = new HBox(10, nameField, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);

        statusLabel = label(12, FontWeight.NORMAL);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        addCard = new VBox(10, addRow, statusLabel);
        addCard.setPadding(new Insets(18, 20, 18, 20));

        // List
        listTitle = label(15, FontWeight.SEMI_BOLD);
        listTitle.setText("All subjects");
        countLabel = label(12, FontWeight.NORMAL);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox listHead = new HBox(listTitle, grow, countLabel);
        listHead.setAlignment(Pos.BASELINE_LEFT);
        listHead.setPadding(new Insets(0, 8, 8, 8));

        rowsBox = new VBox(2);

        listCard = new VBox(6, listHead, rowsBox);
        listCard.setPadding(new Insets(22, 16, 16, 16));

        getChildren().addAll(header, addCard, listCard);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    public void loadSubjects() {
        subjects.setAll(subjectController.getAllSubjects());
    }

    public ObservableList<Subject> getSubjects() { return subjects; }

    private void handleAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            setStatus("Type a subject name first.");
            nameField.requestFocus();
            return;
        }
        Subject added = subjectController.addSubject(name);
        if (added == null) {
            setStatus("Could not save the subject. Please try again.");
            return;
        }
        subjects.add(added);
        nameField.clear();
        setStatus("");
    }

    private void handleDelete(Subject subject) {
        ButtonType deleteType = new ButtonType("Delete subject", ButtonBar.ButtonData.OK_DONE);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "", deleteType, ButtonType.CANCEL);
        confirm.setTitle("Delete subject");
        if (getScene() != null) confirm.initOwner(getScene().getWindow());

        DialogPane pane = confirm.getDialogPane();
        Label body = label(13, FontWeight.NORMAL);
        body.setText("All study sessions linked to this subject will also be deleted. This can't be undone.");
        body.setWrapText(true);
        body.setPrefWidth(400);
        body.setMinHeight(Region.USE_PREF_SIZE);
        muted(body);
        pane.setContent(body);
        styleDialog(pane, null, "Delete \"" + subject.getName() + "\"?");

        styleDialogButton((Button) pane.lookupButton(deleteType), "danger");
        styleDialogButton((Button) pane.lookupButton(ButtonType.CANCEL), "ghost");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == deleteType) {
            subjectController.deleteSubject(subject.getId());
            subjects.remove(subject);
        }
    }

    private void openEditDialog(Subject subject) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit subject");
        if (getScene() != null) dialog.initOwner(getScene().getWindow());

        // ── About
        TextField titleField = new TextField(subject.getTitle());
        titleField.setPromptText("e.g. Target band 7.5, Finish chapter 3");
        input(titleField);

        TextArea notesArea = new TextArea(subject.getNotes());
        notesArea.setPromptText("Goals, resources, or anything about this subject");
        notesArea.setPrefRowCount(4);
        notesArea.setWrapText(true);
        input(notesArea);

        // ── Goals
        TextField weeklyField = new TextField(hoursText(subject.getWeeklyGoalMinutes()));
        weeklyField.setPromptText("e.g. 5");
        weeklyField.setPrefWidth(90);
        input(weeklyField);

        TextField targetField = new TextField(hoursText(subject.getTargetMinutes()));
        targetField.setPromptText("e.g. 50");
        targetField.setPrefWidth(90);
        input(targetField);

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Pick a date");
        datePicker.setPrefWidth(170);
        try {
            if (!subject.getTargetDate().isBlank()) datePicker.setValue(LocalDate.parse(subject.getTargetDate()));
        } catch (Exception ignored) { }
        styleDatePicker(datePicker);

        HBox weeklyRow = new HBox(10, weeklyField, mutedText("hours per week"));
        weeklyRow.setAlignment(Pos.CENTER_LEFT);

        HBox targetRow = new HBox(10, targetField, mutedText("hours in total, by"), datePicker);
        targetRow.setAlignment(Pos.CENTER_LEFT);

        Label goalsHint = mutedText("Leave a field empty for no goal. Total includes all the time you've already logged.");
        goalsHint.setWrapText(true);
        goalsHint.setFont(Font.font(ThemeManager.FONT, 12));
        goalsHint.setMinHeight(Region.USE_PREF_SIZE);

        Label error = label(12, FontWeight.NORMAL);
        error.setStyle("-fx-text-fill: " + ThemeManager.danger() + ";");
        error.setWrapText(true);
        error.setMinHeight(Region.USE_PREF_SIZE);
        error.setVisible(false);
        error.setManaged(false);

        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: " + ThemeManager.separator() + ";");
        VBox.setMargin(divider, new Insets(10, 0, 6, 0));

        VBox form = new VBox(6,
            fieldLabel("Title"), titleField,
            gap(6),
            fieldLabel("Notes"), notesArea,
            divider,
            fieldLabel("Weekly goal"), weeklyRow,
            gap(6),
            fieldLabel("Target"), targetRow,
            gap(2),
            goalsHint,
            error
        );
        form.setPrefWidth(480);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(form);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialog(pane, "Edit subject", subject.getName());

        Button saveBtn = (Button) pane.lookupButton(saveType);
        styleDialogButton(saveBtn, "primary");
        styleDialogButton((Button) pane.lookupButton(ButtonType.CANCEL), "ghost");

        // Validate before closing
        int[] parsed = new int[2];
        saveBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            String problem = null;
            int weekly = parseHours(weeklyField.getText());
            int target = parseHours(targetField.getText());
            LocalDate due = datePicker.getValue();

            if (weekly < 0)                        problem = "Weekly goal must be a number of hours, like 5 or 1.5.";
            else if (weekly > 7 * 24 * 60)         problem = "A week only has 168 hours.";
            else if (target < 0)                   problem = "Target must be a number of hours, like 50.";
            else if (target > 0 && due == null)    problem = "Pick a date for the target.";
            else if (target == 0 && due != null)   problem = "Add how many hours you want to reach by that date.";
            else if (due != null && due.isBefore(LocalDate.now())) problem = "The target date is in the past.";

            if (problem != null) {
                error.setText(problem);
                error.setVisible(true);
                error.setManaged(true);
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
                ev.consume();
            } else {
                parsed[0] = weekly;
                parsed[1] = target;
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            subject.setTitle(titleField.getText().trim());
            subject.setNotes(notesArea.getText().trim());
            subjectController.updateSubject(subject.getId(), subject.getTitle(), subject.getNotes());

            LocalDate due = datePicker.getValue();
            subject.setWeeklyGoalMinutes(parsed[0]);
            subject.setTargetMinutes(parsed[1]);
            subject.setTargetDate(parsed[1] > 0 && due != null ? due.toString() : "");
            subjectController.updateGoals(subject.getId(), subject.getWeeklyGoalMinutes(),
                                          subject.getTargetMinutes(), subject.getTargetDate());
            renderRows();
        }
    }

    /** "" → 0 (no goal); "5" / "1.5" / "1,5" → minutes; invalid → -1. */
    static int parseHours(String text) {
        if (text == null || text.isBlank()) return 0;
        String t = text.trim().toLowerCase().replace(',', '.').replace("h", "").trim();
        try {
            double h = Double.parseDouble(t);
            if (h < 0 || h > 100_000 || Double.isNaN(h) || Double.isInfinite(h)) return -1;
            return (int) Math.round(h * 60);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String hoursText(int minutes) {
        if (minutes <= 0) return "";
        double h = minutes / 60.0;
        return h == Math.floor(h) ? String.valueOf((long) h) : String.valueOf(Math.round(h * 100) / 100.0);
    }

    // ── Rows ──────────────────────────────────────────────────────────────────

    private void renderRows() {
        rowsBox.getChildren().clear();
        int n = subjects.size();
        countLabel.setText(n == 0 ? "" : n + " subject" + (n != 1 ? "s" : ""));

        if (n == 0) {
            Label empty = label(13, FontWeight.NORMAL);
            empty.setText("No subjects yet. Add your first one above.");
            muted(empty);
            empty.setPadding(new Insets(12, 8, 8, 8));
            rowsBox.getChildren().add(empty);
            return;
        }

        Map<Integer, SubjectProgress> progress = new HashMap<>();
        for (SubjectProgress p : progressController.getFor(List.copyOf(subjects))) {
            progress.put(p.subject().getId(), p);
        }

        for (int i = 0; i < n; i++) {
            if (i > 0) rowsBox.getChildren().add(divider());
            Subject s = subjects.get(i);
            rowsBox.getChildren().add(row(s, progress.get(s.getId())));
        }
    }

    private HBox row(Subject subject, SubjectProgress p) {
        String name = subject.getName() == null ? "" : subject.getName().trim();

        // Stage badge
        StackPane badge = StageBadge.create(p.stage(), p.next(), p.totalSeconds(), 36);

        // Text
        Label nameLbl = label(14, FontWeight.SEMI_BOLD);
        nameLbl.setText(name);
        primary(nameLbl);

        boolean hasTitle = subject.getTitle() != null && !subject.getTitle().isBlank();
        Label titleLbl = label(12, FontWeight.NORMAL);
        titleLbl.setText(hasTitle ? subject.getTitle() : "No title yet");
        titleLbl.setStyle("-fx-text-fill: " +
            (hasTitle ? ThemeManager.textPrimary() : ThemeManager.textSecondary()) + ";" +
            (hasTitle ? "" : "-fx-font-style: italic;"));

        VBox info = new VBox(2, nameLbl, titleLbl);

        String goals = goalSummary(subject);
        String notes = subject.getNotes() == null ? "" : subject.getNotes().trim();
        if (!goals.isEmpty()) {
            Label g = label(12, FontWeight.NORMAL);
            g.setText(goals);
            g.setStyle("-fx-text-fill: " + ThemeManager.textAccent() + ";");
            info.getChildren().add(g);
        } else if (!notes.isEmpty()) {
            Label notesLbl = label(12, FontWeight.NORMAL);
            notesLbl.setText(notes.lines().findFirst().orElse(""));
            notesLbl.setMaxWidth(420);
            muted(notesLbl);
            info.getChildren().add(notesLbl);
        }
        info.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Evolution column
        VBox evo = evolutionColumn(p);

        // Actions
        Button editBtn   = ghostButton("Edit", false);
        Button deleteBtn = ghostButton("Delete", true);
        editBtn  .setOnAction(e -> openEditDialog(subject));
        deleteBtn.setOnAction(e -> handleDelete(subject));

        HBox row = new HBox(14, badge, info, evo, editBtn, deleteBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 8, 12, 8));
        String base = "-fx-background-radius: 8;";
        row.setStyle("-fx-background-color: transparent;" + base);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + ThemeManager.bgDarker() + ";" + base));
        row.setOnMouseExited (e -> row.setStyle("-fx-background-color: transparent;" + base));
        return row;
    }

    private VBox evolutionColumn(SubjectProgress p) {
        Label stageLbl = label(12, FontWeight.SEMI_BOLD);
        stageLbl.setText(p.stage().name());
        primary(stageLbl);

        Label totalLbl = label(12, FontWeight.NORMAL);
        totalLbl.setText(fmt(p.totalSeconds()));
        muted(totalLbl);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox top = new HBox(6, stageLbl, grow, totalLbl);
        top.setAlignment(Pos.BASELINE_LEFT);

        Region track = new Region();
        track.setPrefHeight(4);
        track.setMaxHeight(4);
        track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle("-fx-background-color: " + ThemeManager.heatLevel(0) + "; -fx-background-radius: 2;");
        Region fill = new Region();
        fill.setPrefHeight(4);
        fill.setMaxHeight(4);
        fill.maxWidthProperty().bind(track.widthProperty().multiply(p.stageProgress()));
        fill.setStyle("-fx-background-color: " + ThemeManager.textAccent() + "; -fx-background-radius: 2;");
        StackPane bar = new StackPane(track, fill);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label nextLbl = label(11, FontWeight.NORMAL);
        nextLbl.setText(p.next() == null
            ? "Final stage"
            : fmt(p.next().minSeconds() - p.totalSeconds()) + " to " + p.next().name());
        muted(nextLbl);

        VBox col = new VBox(5, top, bar, nextLbl);
        col.setMinWidth(170);
        col.setPrefWidth(170);
        col.setMaxWidth(170);
        col.setPadding(new Insets(0, 12, 0, 0));
        return col;
    }

    private static String goalSummary(Subject s) {
        StringBuilder sb = new StringBuilder();
        if (s.hasWeeklyGoal()) sb.append(hoursText(s.getWeeklyGoalMinutes())).append("h per week");
        if (s.hasTarget()) {
            if (sb.length() > 0) sb.append("  ·  ");
            String due = s.getTargetDate();
            try { due = LocalDate.parse(due).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)); }
            catch (Exception ignored) { }
            sb.append(hoursText(s.getTargetMinutes())).append("h by ").append(due);
        }
        return sb.toString();
    }

    private Button ghostButton(String text, boolean danger) {
        Button b = new Button(text);
        b.setFont(Font.font(ThemeManager.FONT, FontWeight.NORMAL, 12));
        b.setCursor(Cursor.HAND);
        String base = "-fx-background-radius: 6; -fx-padding: 5 12;";
        String idle = "-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.textSecondary() + ";" + base;
        String hover = danger
            ? "-fx-background-color: " + dangerSoft() + "; -fx-text-fill: " + danger() + ";" + base
            : "-fx-background-color: " + ThemeManager.bgCard() + "; -fx-text-fill: " + ThemeManager.textPrimary() + ";" + base;
        b.setStyle(idle);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited (e -> b.setStyle(idle));
        return b;
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    public void applyTheme() {
        setStyle(
            "-fx-background-color: " + ThemeManager.bgMain() + ";" +
            "-fx-focus-color: " + ThemeManager.textAccent() + ";" +
            "-fx-faint-focus-color: transparent;"
        );

        primary(heading);
        muted(hint);

        addCard .setStyle(ThemeManager.cardStyle());
        listCard.setStyle(ThemeManager.cardStyle());

        styleInput(nameField);
        styleAddBtn(false);
        statusLabel.setStyle("-fx-text-fill: " + danger() + ";");

        primary(listTitle);
        muted(countLabel);

        renderRows();
    }

    private void styleAddBtn(boolean hover) {
        String bg = hover ? ThemeManager.btnPrimaryHover() : ThemeManager.btnPrimaryBg();
        addBtn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + ThemeManager.btnPrimaryFg() + ";" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 0 16 0 14;"
        );
        plusIcon.setStyle("-fx-fill: " + ThemeManager.btnPrimaryFg() + ";");
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

    private void setStatus(String text) {
        statusLabel.setText(text);
        boolean show = text != null && !text.isEmpty();
        statusLabel.setVisible(show);
        statusLabel.setManaged(show);
    }

    // ── Dialog styling ────────────────────────────────────────────────────────

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
                "-fx-background-color: " + danger() + ";" +
                "-fx-text-fill: " + ThemeManager.bgCard() + ";" + base);
            default -> b.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + ThemeManager.textSecondary() + ";" +
                "-fx-border-color: " + ThemeManager.border() + ";" +
                "-fx-border-radius: 8;" + base);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String danger()     { return ThemeManager.danger(); }
    private static String dangerSoft() { return ThemeManager.dangerSoft(); }

    private static String fmt(long seconds) {
        return StudySessionController.formatSeconds(Math.max(0, seconds));
    }

    private static Label fieldLabel(String text) {
        Label l = label(12, FontWeight.SEMI_BOLD);
        l.setText(text);
        muted(l);
        return l;
    }

    private static Label mutedText(String text) {
        Label l = label(13, FontWeight.NORMAL);
        l.setText(text);
        muted(l);
        return l;
    }

    private void input(TextInputControl c) {
        c.setFont(Font.font(ThemeManager.FONT, 13));
        c.focusedProperty().addListener((o, was, is) -> styleInput(c));
        styleInput(c);
    }

    private void styleDatePicker(DatePicker dp) {
        String accent = ThemeManager.textAccent();
        dp.setStyle(
            "-fx-font-family: '" + ThemeManager.FONT + "';" +
            "-fx-font-size: 13;" +
            "-fx-base: " + ThemeManager.bgCard() + ";" +
            "-fx-background: " + ThemeManager.bgCard() + ";" +
            "-fx-control-inner-background: " + ThemeManager.bgInput() + ";" +
            "-fx-text-base-color: " + ThemeManager.textPrimary() + ";" +
            "-fx-mark-color: " + ThemeManager.textSecondary() + ";" +
            "-fx-accent: " + accent + ";" +
            "-fx-focus-color: " + accent + ";" +
            "-fx-faint-focus-color: transparent;" +
            "-fx-selection-bar: " + ThemeManager.heatLevel(2) + ";" +
            "-fx-selection-bar-non-focused: " + ThemeManager.bgDarker() + ";" +
            "-fx-background-color: " + ThemeManager.borderInput() + ", " + ThemeManager.bgInput() + ";" +
            "-fx-background-insets: 0, 1;" +
            "-fx-background-radius: 8, 7;"
        );
        styleInput(dp.getEditor());
        dp.getEditor().setStyle(dp.getEditor().getStyle() + "-fx-background-color: transparent;");
        dp.getEditor().focusedProperty().addListener((o, was, is) -> {
            styleInput(dp.getEditor());
            dp.getEditor().setStyle(dp.getEditor().getStyle() + "-fx-background-color: transparent;");
        });
    }

    private static Label label(double size, FontWeight weight) {
        Label l = new Label();
        l.setFont(Font.font(ThemeManager.FONT, weight, size));
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
        VBox.setMargin(d, new Insets(0, 8, 0, 58));
        return d;
    }

    private static Region gap(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }
}
