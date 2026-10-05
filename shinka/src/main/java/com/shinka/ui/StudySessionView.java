package com.shinka.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.shinka.controller.ProgressController;
import com.shinka.controller.ProgressController.SubjectProgress;
import com.shinka.controller.StudySessionController;
import com.shinka.model.Subject;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
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
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class StudySessionView extends VBox {

    /** Sessions shorter than this are not saved (accidental start/stop). */
    private static final long MIN_SAVE_SECONDS = 60;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private final StudySessionController sessionController;
    private final ProgressController     progressController = new ProgressController();
    private final ObservableList<Subject> subjects;
    private final Runnable onSessionSaved;

    // Timer state. Time is measured from the clock, not by counting ticks,
    // so it stays correct even if the UI lags or the laptop sleeps.
    private Timeline timer;
    private long     elapsedSeconds   = 0;
    private long     accumulatedNanos = 0;
    private long     segmentStartNanos = 0;
    private boolean  running = false;
    private boolean  paused  = false;
    private LocalTime startedAt;

    // Context of the selected subject (for live weekly-goal progress)
    private SubjectProgress selectedProgress;

    // UI
    private Label heading, hint;
    private VBox  timerCard, notesCard;
    private Label subjectLabel, stateLabel, timerGhost, timerLabel, subLabel, statusLabel;
    private ComboBox<Subject> subjectCombo;
    private HBox  contextBox;
    private Button startBtn, pauseBtn, stopBtn;
    private SVGPath startIcon;
    private Label notesTitle, notesHint;
    private TextField titleField;
    private TextArea  notesArea;

    public StudySessionView(StudySessionController sessionController,
                            ObservableList<Subject> subjects,
                            Runnable onSessionSaved) {
        this.sessionController = sessionController;
        this.subjects          = subjects;
        this.onSessionSaved    = onSessionSaved;
        buildUI();
        setupTimer();
        installCloseGuard();
        applyTheme();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void buildUI() {
        setSpacing(24);
        setPadding(new Insets(32, 40, 40, 40));
        setFillWidth(true);

        heading = label(26, FontWeight.SEMI_BOLD);
        heading.setText("Study");
        hint = label(13, FontWeight.NORMAL);
        hint.setText("Pick a subject and start the timer. You can write notes while it runs.");
        VBox header = new VBox(4, heading, hint);

        // ── Timer card
        subjectLabel = label(12, FontWeight.SEMI_BOLD);
        subjectLabel.setText("Subject");

        subjectCombo = new ComboBox<>(subjects);
        subjectCombo.setPromptText("Select a subject");
        subjectCombo.setPrefWidth(260);
        subjectCombo.setPrefHeight(36);
        subjectCombo.valueProperty().addListener((o, a, b) -> {
            updateContext();
            if (b != null && !running) setStatus("", null);
        });

        VBox subjectBox = new VBox(6, subjectLabel, subjectCombo);

        contextBox = new HBox(10);
        contextBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(contextBox, Priority.ALWAYS);

        HBox topRow = new HBox(16, subjectBox, contextBox);
        topRow.setAlignment(Pos.BOTTOM_LEFT);

        stateLabel = label(12, FontWeight.SEMI_BOLD);

        Font digital = Font.loadFont(
            getClass().getResourceAsStream("/fonts/DSEG7-Classic/DSEG7Classic-Regular.ttf"), 72);
        if (digital == null) digital = Font.font("Consolas", FontWeight.NORMAL, 72);

        // Faint "88:88:88" behind the digits, like a real 7-segment display
        timerGhost = new Label("88:88:88");
        timerGhost.setFont(digital);
        timerLabel = new Label("00:00:00");
        timerLabel.setFont(digital);
        StackPane timerStack = new StackPane(timerGhost, timerLabel);

        subLabel = label(12, FontWeight.NORMAL);

        startIcon = new SVGPath();
        startIcon.setContent("M0 0 L9 5.5 L0 11 Z");

        startBtn = button("Start", startIcon);
        pauseBtn = button("Pause", null);
        stopBtn  = button("Stop & save", null);

        startBtn.setOnAction(e -> handleStart());
        pauseBtn.setOnAction(e -> handlePause());
        stopBtn .setOnAction(e -> handleStop());

        startBtn.setOnMouseEntered(e -> styleButtons(startBtn));
        startBtn.setOnMouseExited (e -> styleButtons(null));
        pauseBtn.setOnMouseEntered(e -> styleButtons(pauseBtn));
        pauseBtn.setOnMouseExited (e -> styleButtons(null));
        stopBtn .setOnMouseEntered(e -> styleButtons(stopBtn));
        stopBtn .setOnMouseExited (e -> styleButtons(null));

        HBox btnRow = new HBox(10, startBtn, pauseBtn, stopBtn);
        btnRow.setAlignment(Pos.CENTER);

        statusLabel = label(12, FontWeight.NORMAL);
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        VBox center = new VBox(6, stateLabel, timerStack, subLabel);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(28, 0, 20, 0));

        VBox controls = new VBox(14, btnRow, statusLabel);
        controls.setAlignment(Pos.CENTER);

        timerCard = new VBox(topRow, center, controls);
        timerCard.setPadding(new Insets(24, 28, 28, 28));

        // ── Notes card
        notesTitle = label(15, FontWeight.SEMI_BOLD);
        notesTitle.setText("Session notes");
        notesHint = label(12, FontWeight.NORMAL);
        notesHint.setText("Optional. Saved with this session, editable later in History.");

        titleField = new TextField();
        titleField.setPromptText("What are you working on? e.g. Writing task 2");
        input(titleField);

        notesArea = new TextArea();
        notesArea.setPromptText("Anything worth remembering from this session");
        notesArea.setPrefRowCount(4);
        notesArea.setWrapText(true);
        input(notesArea);

        notesCard = new VBox(10, new VBox(2, notesTitle, notesHint), titleField, notesArea);
        notesCard.setPadding(new Insets(22, 24, 24, 24));

        getChildren().addAll(header, timerCard, notesCard);
        renderState();
    }

    private void setupTimer() {
        timer = new Timeline(new KeyFrame(Duration.millis(250), e -> tick()));
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    private void tick() {
        long nanos = accumulatedNanos + (running && !paused ? System.nanoTime() - segmentStartNanos : 0);
        long secs = nanos / 1_000_000_000L;
        if (secs != elapsedSeconds) {
            elapsedSeconds = secs;
            timerLabel.setText(StudySessionController.formatTimer(elapsedSeconds));
            updateContext();
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * One-click start from the Dashboard ("Continue <subject>").
     * Selects the subject and starts the timer. If a session is already
     * running, nothing is changed so the current session is never lost.
     */
    public void quickStart(int subjectId) {
        if (running) return;
        for (Subject s : subjects) {
            if (s.getId() == subjectId) {
                subjectCombo.setValue(s);
                handleStart();
                return;
            }
        }
        setStatus("That subject no longer exists. Pick one from the list.", ThemeManager.danger());
    }

    public boolean isRunning() { return running; }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void handleStart() {
        if (subjectCombo.getValue() == null) {
            setStatus("Pick a subject first.", ThemeManager.danger());
            subjectCombo.requestFocus();
            return;
        }
        setStatus("", null);
        running = true;
        paused  = false;
        accumulatedNanos  = 0;
        segmentStartNanos = System.nanoTime();
        startedAt = LocalTime.now();
        timer.play();
        renderState();
    }

    private void handlePause() {
        if (!running) return;
        if (!paused) {
            accumulatedNanos += System.nanoTime() - segmentStartNanos;
            paused = true;
        } else {
            segmentStartNanos = System.nanoTime();
            paused = false;
        }
        tick();
        renderState();
    }

    private void handleStop() {
        if (!running) return;
        tick();
        long seconds = elapsedSeconds;
        Subject subject = subjectCombo.getValue();

        if (subject == null) {
            resetForm();
            setStatus("No subject selected, so nothing was saved.", ThemeManager.danger());
            return;
        }
        if (seconds < MIN_SAVE_SECONDS) {
            resetForm();
            setStatus("Sessions under a minute aren't saved.", ThemeManager.textSecondary());
            return;
        }

        save(subject, seconds);
        resetForm();
        setStatus("Saved " + StudySessionController.formatSeconds(seconds) + " to " + subject.getName() + ".",
                  ThemeManager.textAccent());
        onSessionSaved.run();
    }

    private void save(Subject subject, long seconds) {
        sessionController.saveSession(subject.getId(), titleField.getText(), notesArea.getText(), seconds);
    }

    private void resetForm() {
        timer.stop();
        running = false;
        paused  = false;
        elapsedSeconds = 0;
        accumulatedNanos = 0;
        startedAt = null;
        timerLabel.setText("00:00:00");
        titleField.clear();
        notesArea.clear();
        renderState();
        updateContext();
    }

    // ── Close guard: never lose a running session ─────────────────────────────

    private void installCloseGuard() {
        sceneProperty().addListener((o, oldScene, scene) -> {
            if (scene == null) return;
            scene.windowProperty().addListener((ow, oldWin, win) -> attachGuard(win));
            attachGuard(scene.getWindow());
        });
    }

    private Window guardedWindow;

    private void attachGuard(Window win) {
        if (win == null || win == guardedWindow) return;
        guardedWindow = win;
        win.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, ev -> {
            if (!running) return;
            tick();
            Subject subject = subjectCombo.getValue();

            ButtonType saveType    = new ButtonType("Save and close", ButtonBar.ButtonData.OK_DONE);
            ButtonType discardType = new ButtonType("Close without saving", ButtonBar.ButtonData.OTHER);
            ButtonType keepType    = new ButtonType("Keep studying", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "", saveType, discardType, keepType);
            a.setTitle("Session still running");
            a.initOwner(win);

            DialogPane pane = a.getDialogPane();
            Label what = label(13, FontWeight.SEMI_BOLD);
            what.setText((subject != null ? subject.getName() : "Session") + " · " +
                         StudySessionController.formatSeconds(Math.max(elapsedSeconds, 1)));
            primary(what);
            Label why = label(13, FontWeight.NORMAL);
            why.setText("Your timer is still running. Save it before closing so this time counts.");
            why.setWrapText(true);
            why.setPrefWidth(420);
            why.setMinHeight(Region.USE_PREF_SIZE);
            muted(why);
            pane.setContent(new VBox(6, what, why));
            styleDialog(pane, "Save your session first?");
            styleDialogButton((Button) pane.lookupButton(saveType), "primary");
            styleDialogButton((Button) pane.lookupButton(discardType), "danger-ghost");
            styleDialogButton((Button) pane.lookupButton(keepType), "ghost");

            Optional<ButtonType> r = a.showAndWait();
            if (r.isEmpty() || r.get() == keepType) {
                ev.consume();                       // stay open
            } else if (r.get() == saveType) {
                if (subject != null && elapsedSeconds >= MIN_SAVE_SECONDS) save(subject, elapsedSeconds);
                timer.stop();
            } else {
                timer.stop();                       // close without saving
            }
        });
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderState() {
        startBtn.setVisible(!running);
        startBtn.setManaged(!running);
        pauseBtn.setVisible(running);
        pauseBtn.setManaged(running);
        stopBtn .setVisible(running);
        stopBtn .setManaged(running);
        pauseBtn.setText(paused ? "Resume" : "Pause");
        subjectCombo.setDisable(running);

        Subject s = subjectCombo.getValue();
        String state, color, sub;
        if (!running) {
            state = "READY";
            color = ThemeManager.textPrimary();
            sub   = s == null ? "Select a subject to begin" : "Press Start when you're ready";
        } else if (paused) {
            state = "PAUSED";
            color = ThemeManager.timerPaused();
            sub   = "Started at " + startedAt.format(CLOCK) + " · paused";
        } else {
            state = "STUDYING " + (s != null ? s.getName().toUpperCase() : "");
            color = ThemeManager.textAccent();
            sub   = "Started at " + startedAt.format(CLOCK);
        }
        stateLabel.setText(state);
        stateLabel.setStyle("-fx-text-fill: " + (running ? color : ThemeManager.textSecondary()) + ";");
        timerLabel.setStyle("-fx-text-fill: " + color + ";");
        subLabel.setText(sub);
        styleButtons(null);
    }

    /** Stage badge + live weekly-goal progress for the selected subject. */
    private void updateContext() {
        contextBox.getChildren().clear();
        Subject s = subjectCombo.getValue();
        if (s == null) { selectedProgress = null; return; }

        if (selectedProgress == null || selectedProgress.subject().getId() != s.getId() || !running) {
            selectedProgress = null;
            for (SubjectProgress p : progressController.getAll()) {
                if (p.subject().getId() == s.getId()) { selectedProgress = p; break; }
            }
        }
        SubjectProgress p = selectedProgress;
        if (p == null) return;

        long live  = running ? elapsedSeconds : 0;
        long total = p.totalSeconds() + live;
        long week  = p.weekSeconds() + live;

        ProgressController.Stage stage = ProgressController.stageFor(total);
        ProgressController.Stage next  = ProgressController.nextStage(stage);

        Label top = label(12, FontWeight.SEMI_BOLD);
        top.setText(stage.name() + " · " + fmt(total) + " total");
        primary(top);

        Label bottom = label(12, FontWeight.NORMAL);
        if (s.hasWeeklyGoal()) {
            long goal = s.getWeeklyGoalMinutes() * 60L;
            bottom.setText(week >= goal
                ? "Weekly goal done · " + fmt(week) + " of " + fmt(goal)
                : fmt(week) + " of " + fmt(goal) + " this week");
            bottom.setStyle("-fx-text-fill: " + (week >= goal ? ThemeManager.textAccent() : ThemeManager.textSecondary()) + ";");
        } else {
            bottom.setText(next == null ? "Final stage" : fmt(next.minSeconds() - total) + " to " + next.name());
            muted(bottom);
        }

        VBox text = new VBox(2, top, bottom);
        text.setAlignment(Pos.CENTER_RIGHT);
        top.setMaxWidth(Double.MAX_VALUE);
        bottom.setMaxWidth(Double.MAX_VALUE);
        top.setAlignment(Pos.CENTER_RIGHT);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        contextBox.getChildren().addAll(text, StageBadge.create(stage, next, total, 36));
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
        muted(subjectLabel);
        muted(subLabel);
        primary(notesTitle);
        muted(notesHint);

        timerCard.setStyle(ThemeManager.cardStyle());
        notesCard.setStyle(ThemeManager.cardStyle());
        timerGhost.setStyle("-fx-text-fill: " + ThemeManager.heatLevel(0) + ";");

        styleCombo(subjectCombo);
        styleInput(titleField);
        styleInput(notesArea);

        if (statusLabel.isVisible() && statusColor != null) {
            statusLabel.setStyle("-fx-text-fill: " + statusColor + ";");
        }
        renderState();
        updateContext();
    }

    private void styleButtons(Button hovered) {
        String base = "-fx-background-radius: 8; -fx-padding: 10 22;";
        String pBg = hovered == startBtn ? ThemeManager.btnPrimaryHover() : ThemeManager.btnPrimaryBg();
        startBtn.setStyle("-fx-background-color: " + pBg + "; -fx-text-fill: " + ThemeManager.btnPrimaryFg() + ";" +
                          base + "-fx-padding: 10 24 10 20;");
        startIcon.setStyle("-fx-fill: " + ThemeManager.btnPrimaryFg() + ";");

        String sBg = hovered == stopBtn ? ThemeManager.btnPrimaryHover() : ThemeManager.btnPrimaryBg();
        stopBtn.setStyle("-fx-background-color: " + sBg + "; -fx-text-fill: " + ThemeManager.btnPrimaryFg() + ";" + base);

        pauseBtn.setStyle(
            "-fx-background-color: " + (hovered == pauseBtn ? ThemeManager.bgDarker() : "transparent") + ";" +
            "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
            "-fx-border-color: " + ThemeManager.border() + ";" +
            "-fx-border-radius: 8;" + base
        );
    }

    private String statusColor;

    private void setStatus(String text, String color) {
        boolean show = text != null && !text.isEmpty();
        statusColor = color;
        statusLabel.setText(show ? text : "");
        statusLabel.setVisible(show);
        statusLabel.setManaged(show);
        if (show) statusLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    private void styleCombo(ComboBox<?> c) {
        String accent = ThemeManager.textAccent();
        c.setStyle(
            "-fx-font-family: '" + ThemeManager.FONT + "';" +
            "-fx-font-size: 13;" +
            "-fx-base: " + ThemeManager.bgCard() + ";" +
            "-fx-background: " + ThemeManager.bgCard() + ";" +
            "-fx-control-inner-background: " + ThemeManager.bgInput() + ";" +
            "-fx-text-base-color: " + ThemeManager.textPrimary() + ";" +
            "-fx-prompt-text-fill: " + ThemeManager.textSecondary() + ";" +
            "-fx-mark-color: " + ThemeManager.textSecondary() + ";" +
            "-fx-accent: " + ThemeManager.heatLevel(2) + ";" +
            "-fx-selection-bar: " + ThemeManager.heatLevel(2) + ";" +
            "-fx-selection-bar-non-focused: " + ThemeManager.bgDarker() + ";" +
            "-fx-cell-hover-color: " + ThemeManager.bgDarker() + ";" +
            "-fx-focus-color: " + accent + ";" +
            "-fx-faint-focus-color: transparent;" +
            "-fx-background-color: " + ThemeManager.borderInput() + ", " + ThemeManager.bgInput() + ";" +
            "-fx-background-insets: 0, 1;" +
            "-fx-background-radius: 8, 7;" +
            "-fx-opacity: 1;"
        );
    }

    private void input(TextInputControl c) {
        c.setFont(Font.font(ThemeManager.FONT, 13));
        c.focusedProperty().addListener((o, was, is) -> styleInput(c));
        if (c instanceof TextArea ta) {
            // Remove the default inner shadow of the text area's content
            ta.skinProperty().addListener((o, a, skin) -> {
                Node content = ta.lookup(".content");
                if (content != null) content.setStyle("-fx-background-color: transparent; -fx-effect: null;");
            });
        }
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

    private void styleDialog(DialogPane pane, String title) {
        Label t = label(17, FontWeight.SEMI_BOLD);
        t.setText(title);
        t.setWrapText(true);
        primary(t);
        VBox head = new VBox(t);
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
            case "danger-ghost" -> b.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + ThemeManager.danger() + ";" +
                "-fx-border-color: " + ThemeManager.border() + ";" +
                "-fx-border-radius: 8;" + base);
            default -> b.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + ThemeManager.textSecondary() + ";" +
                "-fx-border-color: " + ThemeManager.border() + ";" +
                "-fx-border-radius: 8;" + base);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Button button(String text, Node graphic) {
        Button b = new Button(text, graphic);
        b.setGraphicTextGap(10);
        b.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 14));
        b.setCursor(Cursor.HAND);
        b.setMinWidth(110);
        return b;
    }

    private static String fmt(long seconds) {
        return seconds <= 0 ? "0m" : StudySessionController.formatSeconds(seconds);
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
}