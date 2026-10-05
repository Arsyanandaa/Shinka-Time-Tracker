package com.shinka.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import com.shinka.controller.HeatmapController;
import com.shinka.controller.StreakController;
import com.shinka.controller.StreakController.StreakInfo;
import com.shinka.controller.StudySessionController;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardView extends VBox {

    private static final DateTimeFormatter HEADER_DATE =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);

    private final StudySessionController sessionController;
    private final HeatmapController      heatmapController;
    private final Runnable               onStartStudy;
    private final StreakController       streakController = new StreakController();

    // Header
    private Label   dateLabel, titleLabel;
    private Button  startBtn;
    private SVGPath playIcon;

    // Stats
    private VBox    statsCard;
    private Label   todayCaption, todayValue, todayHint;
    private Label[] statCaptions = new Label[3];
    private Label[] statValues   = new Label[3];
    private Region[] statDividers = new Region[3];

    // Streak
    private VBox      streakCard;
    private Label     streakCaption, streakValue, streakBest, streakMessage, streakSub;
    private Region    streakDivider;
    private HBox      weekDots;

    // Activity
    private VBox        activityCard;
    private Label       activityTitle;
    private HeatmapView heatmapView;

    // Subjects
    private VBox     subjectCard;
    private Label    subjectTitle, subjectTotal;
    private GridPane subjectGrid;

    public DashboardView(StudySessionController sessionController,
                         HeatmapController heatmapController,
                         Runnable onStartStudy) {
        this.sessionController = sessionController;
        this.heatmapController = heatmapController;
        this.onStartStudy      = onStartStudy;
        buildUI();
        applyTheme();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void buildUI() {
        setSpacing(24);
        setPadding(new Insets(32, 40, 40, 40));
        setFillWidth(true);

        getChildren().addAll(
            buildHeader(),
            buildStatsCard(),
            buildStreakCard(),
            buildActivityCard(),
            buildSubjectCard()
        );
    }

    private HBox buildHeader() {
        dateLabel  = label(12, FontWeight.NORMAL);
        titleLabel = label(26, FontWeight.SEMI_BOLD);
        titleLabel.setText("Overview");

        VBox titles = new VBox(2, dateLabel, titleLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        playIcon = new SVGPath();
        playIcon.setContent("M0 0 L9 5.5 L0 11 Z");

        startBtn = new Button("Start session", playIcon);
        startBtn.setGraphicTextGap(10);
        startBtn.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 13));
        startBtn.setCursor(Cursor.HAND);
        startBtn.setOnAction(e -> onStartStudy.run());
        startBtn.setOnMouseEntered(e -> styleStartBtn(true));
        startBtn.setOnMouseExited (e -> styleStartBtn(false));

        HBox header = new HBox(16, titles, startBtn);
        header.setAlignment(Pos.BOTTOM_LEFT);
        return header;
    }

    private VBox buildStatsCard() {
        // Hero: today
        todayCaption = label(12, FontWeight.NORMAL);
        todayCaption.setText("Today");
        todayValue   = label(38, FontWeight.SEMI_BOLD);
        todayHint    = label(12, FontWeight.NORMAL);

        VBox hero = new VBox(4, todayCaption, todayValue, todayHint);
        hero.setMinWidth(220);
        hero.setPadding(new Insets(0, 28, 0, 0));

        HBox row = new HBox(hero);
        row.setAlignment(Pos.CENTER_LEFT);

        String[] captions = {"This week", "This month", "All time"};
        for (int i = 0; i < 3; i++) {
            statDividers[i] = new Region();
            statDividers[i].setMinWidth(1);
            statDividers[i].setMaxWidth(1);
            statDividers[i].setPrefHeight(56);
            statDividers[i].setMaxHeight(56);

            statCaptions[i] = label(12, FontWeight.NORMAL);
            statCaptions[i].setText(captions[i]);
            statValues[i]   = label(22, FontWeight.SEMI_BOLD);

            VBox stat = new VBox(6, statCaptions[i], statValues[i]);
            stat.setPadding(new Insets(0, 24, 0, 24));
            stat.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(stat, Priority.ALWAYS);

            row.getChildren().addAll(statDividers[i], stat);
        }

        statsCard = new VBox(row);
        statsCard.setPadding(new Insets(24, 28, 24, 28));
        return statsCard;
    }

    private VBox buildStreakCard() {
        streakCaption = label(12, FontWeight.NORMAL);
        streakCaption.setText("Weekly streak");
        streakValue = label(26, FontWeight.SEMI_BOLD);
        streakBest  = label(12, FontWeight.NORMAL);

        VBox left = new VBox(4, streakCaption, streakValue, streakBest);
        left.setMinWidth(170);
        left.setPadding(new Insets(0, 24, 0, 0));

        streakDivider = new Region();
        streakDivider.setMinWidth(1);
        streakDivider.setMaxWidth(1);
        streakDivider.setPrefHeight(64);
        streakDivider.setMaxHeight(64);

        streakMessage = label(14, FontWeight.SEMI_BOLD);
        streakMessage.setWrapText(true);
        streakSub = label(12, FontWeight.NORMAL);
        streakSub.setWrapText(true);

        VBox middle = new VBox(4, streakMessage, streakSub);
        middle.setAlignment(Pos.CENTER_LEFT);
        middle.setPadding(new Insets(0, 24, 0, 24));
        middle.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(middle, Priority.ALWAYS);

        weekDots = new HBox(6);
        weekDots.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(left, streakDivider, middle, weekDots);
        row.setAlignment(Pos.CENTER_LEFT);

        streakCard = new VBox(row);
        streakCard.setPadding(new Insets(22, 28, 22, 28));
        return streakCard;
    }

    private VBox buildActivityCard() {
        activityTitle = label(15, FontWeight.SEMI_BOLD);
        activityTitle.setText("Activity");

        heatmapView = new HeatmapView(heatmapController);

        activityCard = new VBox(4, activityTitle, heatmapView);
        activityCard.setPadding(new Insets(22, 24, 18, 24));
        return activityCard;
    }

    private VBox buildSubjectCard() {
        subjectTitle = label(15, FontWeight.SEMI_BOLD);
        subjectTitle.setText("By subject");
        subjectTotal = label(12, FontWeight.NORMAL);

        HBox head = new HBox(subjectTitle, spacer(), subjectTotal);
        head.setAlignment(Pos.BASELINE_LEFT);

        subjectGrid = new GridPane();
        subjectGrid.setHgap(16);
        subjectGrid.setVgap(14);

        ColumnConstraints name = new ColumnConstraints();
        name.setMinWidth(150);
        name.setPrefWidth(190);

        ColumnConstraints bar = new ColumnConstraints();
        bar.setHgrow(Priority.ALWAYS);
        bar.setMinWidth(80);

        ColumnConstraints time = new ColumnConstraints();
        time.setMinWidth(72);
        time.setHalignment(HPos.RIGHT);

        ColumnConstraints pct = new ColumnConstraints();
        pct.setMinWidth(40);
        pct.setHalignment(HPos.RIGHT);

        subjectGrid.getColumnConstraints().addAll(name, bar, time, pct);

        subjectCard = new VBox(18, head, subjectGrid);
        subjectCard.setPadding(new Insets(22, 24, 24, 24));
        return subjectCard;
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    public void applyTheme() {
        setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");

        muted(dateLabel);
        primary(titleLabel);
        styleStartBtn(false);

        statsCard   .setStyle(ThemeManager.cardStyle());
        streakCard  .setStyle(ThemeManager.cardStyle());
        streakDivider.setStyle("-fx-background-color: " + ThemeManager.border() + ";");
        muted(streakCaption);
        muted(streakBest);
        primary(streakMessage);
        muted(streakSub);
        activityCard.setStyle(ThemeManager.cardStyle());
        subjectCard .setStyle(ThemeManager.cardStyle());

        muted(todayCaption);
        primary(todayValue);
        muted(todayHint);
        for (int i = 0; i < 3; i++) {
            muted(statCaptions[i]);
            primary(statValues[i]);
            statDividers[i].setStyle("-fx-background-color: " + ThemeManager.border() + ";");
        }

        primary(activityTitle);
        primary(subjectTitle);
        muted(subjectTotal);

        refresh();
    }

    private void styleStartBtn(boolean hover) {
        String bg = hover ? ThemeManager.btnPrimaryHover() : ThemeManager.btnPrimaryBg();
        startBtn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + ThemeManager.btnPrimaryFg() + ";" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 18 10 16;"
        );
        playIcon.setStyle("-fx-fill: " + ThemeManager.btnPrimaryFg() + ";");
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    public void refresh() {
        if (todayValue == null) return;

        LocalDate today = LocalDate.now();
        dateLabel.setText(today.format(HEADER_DATE));

        int todaySec = sessionController.getSecondsToday();
        int weekSec  = sessionController.getSecondsThisWeek();
        int monthSec = sessionController.getSecondsThisMonth();
        int totalSec = sessionController.getTotalSeconds();

        todayValue.setText(todaySec > 0 ? fmt(todaySec) : "0m");
        int daysSoFar = today.getDayOfWeek().getValue(); // Mon = 1
        todayHint.setText(weekSec > 0
            ? "Avg " + fmt(weekSec / daysSoFar) + " per day this week"
            : "No study yet this week");

        statValues[0].setText(fmt(weekSec));
        statValues[1].setText(fmt(monthSec));
        statValues[2].setText(fmt(totalSec));

        buildSubjectRows(totalSec);
        renderStreak(streakController.getStreak());

        if (heatmapView != null) heatmapView.refresh();
    }

    // ── Streak ────────────────────────────────────────────────────────────────

    private void renderStreak(StreakInfo s) {
        int cur = s.currentWeeks();
        streakValue.setText(cur + (cur == 1 ? " week" : " weeks"));
        streakValue.setStyle("-fx-text-fill: " +
            (s.activeThisWeek() ? ThemeManager.textAccent() : ThemeManager.textPrimary()) + ";");
        streakBest.setText(s.bestWeeks() > 0
            ? "Best: " + s.bestWeeks() + (s.bestWeeks() == 1 ? " week" : " weeks")
            : "Best: –");

        String msg, sub;
        if (!s.hasAnyStudy()) {
            msg = "Start your first streak";
            sub = "Study on any day this week. One session is enough to count the week.";
        } else if (s.activeThisWeek()) {
            if (cur == 1 && s.breakWeeksBefore() >= 2) {
                msg = "Welcome back";
                sub = "First active week after " + s.breakWeeksBefore() + " weeks off. Study next week to make it 2.";
            } else if (cur == 1) {
                msg = "This week counts";
                sub = "Study again next week to make it 2 weeks in a row.";
            } else if (cur >= s.bestWeeks()) {
                msg = cur + " weeks in a row, your best so far";
                sub = "Every new week from here sets a new record.";
            } else {
                msg = cur + " weeks in a row";
                sub = (s.bestWeeks() - cur) + " more week" + (s.bestWeeks() - cur == 1 ? "" : "s") +
                      " to match your best of " + s.bestWeeks() + ".";
            }
        } else if (cur > 0) {
            msg = "Keep it going this week";
            sub = "One session before Sunday makes it " + (cur + 1) + " weeks in a row.";
        } else {
            int w = s.weeksSinceLastStudy();
            msg = "Ready for a fresh start";
            sub = "Your last session was " + w + " week" + (w == 1 ? "" : "s") +
                  " ago. One session this week starts a new streak.";
        }
        streakMessage.setText(msg);
        streakSub.setText(sub);

        // Mon..Sun dots
        weekDots.getChildren().clear();
        String[] letters = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < 7; i++) {
            boolean studied = s.daysThisWeek()[i];
            boolean isToday = i == s.todayIndex();
            boolean future  = i > s.todayIndex();

            Region dot = new Region();
            dot.setMinSize(22, 22);
            dot.setMaxSize(22, 22);
            String fill = studied ? ThemeManager.textAccent() : ThemeManager.heatLevel(0);
            String border = isToday && !studied
                ? "-fx-border-color: " + ThemeManager.textAccent() + "; -fx-border-radius: 6; -fx-border-width: 1.5;"
                : "";
            dot.setStyle(
                "-fx-background-color: " + fill + ";" +
                "-fx-background-radius: 6;" + border +
                (future ? "-fx-opacity: 0.45;" : "")
            );

            Label day = label(10, isToday ? FontWeight.SEMI_BOLD : FontWeight.NORMAL);
            day.setText(letters[i]);
            day.setStyle("-fx-text-fill: " +
                (isToday ? ThemeManager.textPrimary() : ThemeManager.textSecondary()) + ";");

            VBox col = new VBox(5, dot, day);
            col.setAlignment(Pos.CENTER);
            weekDots.getChildren().add(col);
        }
    }

    private void buildSubjectRows(int totalSec) {
        subjectGrid.getChildren().clear();
        Map<String, Integer> perSubject = sessionController.getSecondsPerSubject();

        if (perSubject.isEmpty()) {
            subjectTotal.setText("");
            Label empty = label(13, FontWeight.NORMAL);
            empty.setText("No sessions yet. Start a session and your subjects will show up here.");
            empty.setWrapText(true);
            muted(empty);
            subjectGrid.add(empty, 0, 0, 4, 1);
            return;
        }

        subjectTotal.setText(perSubject.size() + " subject" + (perSubject.size() != 1 ? "s" : ""));

        int max = perSubject.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int row = 0;

        for (Map.Entry<String, Integer> entry : perSubject.entrySet()) {
            int sec = entry.getValue();

            Label name = label(13, FontWeight.NORMAL);
            name.setText(entry.getKey());
            name.setMaxWidth(190);
            primary(name);

            Label time = label(13, FontWeight.SEMI_BOLD);
            time.setText(fmt(sec));
            primary(time);

            Label pct = label(12, FontWeight.NORMAL);
            pct.setText(totalSec > 0 ? Math.round(sec * 100.0 / totalSec) + "%" : "–");
            muted(pct);

            subjectGrid.add(name, 0, row);
            subjectGrid.add(bar(max > 0 ? (double) sec / max : 0), 1, row);
            subjectGrid.add(time, 2, row);
            subjectGrid.add(pct,  3, row);
            row++;
        }
    }

    private StackPane bar(double ratio) {
        Region track = new Region();
        track.setPrefHeight(6);
        track.setMaxHeight(6);
        track.setMaxWidth(Double.MAX_VALUE);
        track.setStyle(
            "-fx-background-color: " + ThemeManager.heatLevel(0) + ";" +
            "-fx-background-radius: 3;"
        );

        Region fill = new Region();
        fill.setPrefHeight(6);
        fill.setMaxHeight(6);
        fill.setMinWidth(6);
        fill.maxWidthProperty().bind(track.widthProperty().multiply(ratio));
        fill.setStyle(
            "-fx-background-color: " + ThemeManager.textAccent() + ";" +
            "-fx-background-radius: 3;"
        );

        StackPane bar = new StackPane(track, fill);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMinHeight(18);
        return bar;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String fmt(long seconds) {
        return StudySessionController.formatSeconds(seconds);
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

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}