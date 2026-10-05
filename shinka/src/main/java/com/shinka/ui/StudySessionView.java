package com.shinka.ui;

import com.shinka.controller.StudySessionController;
import com.shinka.model.Subject;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class StudySessionView extends VBox {

    private final StudySessionController sessionController;
    private final ObservableList<Subject> subjects;
    private final Runnable onSessionSaved;

    private Timeline timer;
    private long     elapsedSeconds = 0;
    private boolean  running        = false;
    private boolean  paused         = false;

    private ComboBox<Subject> subjectCombo;
    private Label     timerLabel;
    private Button    startBtn;
    private Button    pauseBtn;
    private Button    stopBtn;
    private Label     statusLabel;

    private static final String STYLE_IDLE    =
    "-fx-text-fill: #3fb950;" +
    "-fx-effect: dropshadow(gaussian, rgba(63,185,80,0.4), 10, 0, 0, 3);";
        private static final String STYLE_RUNNING =
    "-fx-text-fill: #3fb950;" +
    "-fx-effect: dropshadow(gaussian, rgba(63,185,80,0.4), 10, 0, 0, 3);";
        private static final String STYLE_PAUSED  =
    "-fx-text-fill: #3fb950;" +
    "-fx-effect: dropshadow(gaussian, rgba(63,185,80,0.4), 10, 0, 0, 3);";

    public StudySessionView(StudySessionController sessionController,
                            ObservableList<Subject> subjects,
                            Runnable onSessionSaved) {
        this.sessionController = sessionController;
        this.subjects          = subjects;
        this.onSessionSaved    = onSessionSaved;
        buildUI();
        setupTimer();
    }

    private void buildUI() {
        setSpacing(24);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: #f8f9fa;");
        setAlignment(Pos.TOP_CENTER);

        Label heading = new Label("⏱ Study Session");
        heading.setFont(Font.font("System", FontWeight.BOLD, 22));
        heading.setStyle("-fx-text-fill: #199f24;");

        Label hint = new Label("Select a subject and start the timer.");
        hint.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13;");

        VBox headerBox = new VBox(4, heading, hint);
        headerBox.setAlignment(Pos.CENTER);

        subjectCombo = new ComboBox<>(subjects);
        subjectCombo.setPromptText("Select a subject…");
        subjectCombo.setPrefWidth(280);
        subjectCombo.setStyle(
            "-fx-font-size: 13;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;"
        );

        VBox subjectBox = new VBox(6, fieldLabel("Subject *"), subjectCombo);
        subjectBox.setAlignment(Pos.CENTER);

        Font digitalFont = Font.loadFont(
    getClass().getResourceAsStream("/fonts/DSEG7-Classic/DSEG7Classic-Bold.ttf"),
    86
);
if (digitalFont == null) {
    digitalFont = Font.font("Courier New", FontWeight.BOLD, 86);
}
timerLabel = new Label("00:00:00");
timerLabel.setFont(digitalFont);
timerLabel.setStyle(STYLE_IDLE);
timerLabel.setAlignment(Pos.CENTER);

        startBtn = makeButton("▶  Start",       "#2d6a4f", "white");
        pauseBtn = makeButton("⏸  Pause",       "#e67e22", "white");
        stopBtn  = makeButton("⏹  Stop & Save", "#dc3545", "white");

        pauseBtn.setDisable(true);
        stopBtn .setDisable(true);

        startBtn.setOnAction(e -> handleStart());
        pauseBtn.setOnAction(e -> handlePause());
        stopBtn .setOnAction(e -> handleStop());

        HBox btnRow = new HBox(14, startBtn, pauseBtn, stopBtn);
        btnRow.setAlignment(Pos.CENTER);

        VBox timerCard = new VBox(20, timerLabel, btnRow);
        timerCard.setAlignment(Pos.CENTER);
        timerCard.setPadding(new Insets(36, 40, 36, 40));
        timerCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);"
        );

        statusLabel = new Label();
        statusLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(headerBox, subjectBox, timerCard, statusLabel);
    }

    private void setupTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedSeconds++;
            timerLabel.setText(StudySessionController.formatTimer(elapsedSeconds));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    private void handleStart() {
        if (subjectCombo.getValue() == null) {
            showError("Please select a subject before starting.");
            return;
        }
        statusLabel.setText("");
        running = true;
        paused  = false;
        timer.play();

        startBtn.setDisable(true);
        pauseBtn.setDisable(false);
        stopBtn .setDisable(false);
        pauseBtn.setText("⏸  Pause");

        subjectCombo.setDisable(true);
        timerLabel.setStyle(STYLE_RUNNING);
    }

    private void handlePause() {
        if (!running) return;
        if (!paused) {
            timer.pause();
            paused = true;
            pauseBtn.setText("▶  Resume");
            timerLabel.setStyle(STYLE_PAUSED);
            showInfo("Paused. Click Resume to continue.");
        } else {
            timer.play();
            paused = false;
            pauseBtn.setText("⏸  Pause");
            timerLabel.setStyle(STYLE_RUNNING);
            statusLabel.setText("");
        }
    }

    private void handleStop() {
        if (!running) return;

        timer.stop();
        running = false;
        paused  = false;

        Subject subject = subjectCombo.getValue();
        if (subject == null) {
            showError("No subject selected.");
            resetForm();
            return;
        }

        sessionController.saveSession(
            subject.getId(),
            "",
            "",
            elapsedSeconds
        );

        showSuccess("Session saved! Great work 🎉");
        onSessionSaved.run();
        resetForm();
    }

    private void resetForm() {
        timer.stop();
        elapsedSeconds = 0;
        running        = false;
        paused         = false;

        timerLabel.setText("00:00:00");
        timerLabel.setStyle(STYLE_IDLE);

        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
        stopBtn .setDisable(true);
        pauseBtn.setText("⏸  Pause");

        subjectCombo.setDisable(false);
        subjectCombo.setValue(null);
    }

    private Button makeButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setFont(Font.font("System", FontWeight.BOLD, 13));
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-padding: 10 24;" +
            "-fx-background-radius: 8;"
        );
        return btn;
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12;");
        statusLabel.setText("⚠  " + msg);
    }

    private void showSuccess(String msg) {
        statusLabel.setStyle("-fx-text-fill: #2d6a4f; -fx-font-weight: bold; -fx-font-size: 12;");
        statusLabel.setText("✔  " + msg);
    }

    private void showInfo(String msg) {
        statusLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 12;");
        statusLabel.setText("⏸  " + msg);
    }

    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #343a40; -fx-font-weight: bold; -fx-font-size: 13;");
        return lbl;
    }
    public void applyTheme() {
    setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");

    // Update timer card
    for (javafx.scene.Node node : getChildren()) {
        if (node instanceof VBox card) {
            String style = card.getStyle();
            if (style.contains("background-radius: 16")) {
                card.setStyle(
                    "-fx-background-color: " + ThemeManager.bgCard() + ";" +
                    "-fx-background-radius: 16;" +
                    (ThemeManager.isDark()
                        ? "-fx-border-color: " + ThemeManager.border() + "; -fx-border-radius: 16; -fx-border-width: 1;"
                        : "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);")
                );
            }
        }
    }

    // Update timer color
    String timerColor = ThemeManager.timerIdle();
    timerLabel.setStyle(
        "-fx-text-fill: " + timerColor + ";" +
        "-fx-effect: dropshadow(gaussian, rgba(45,106,79,0.35), 10, 0, 0, 3);"
    );
}
}