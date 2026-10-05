package com.shinka.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

import com.shinka.controller.HeatmapController;
import com.shinka.controller.ProgressController;
import com.shinka.controller.ProgressController.SubjectProgress;
import com.shinka.controller.StreakController;
import com.shinka.controller.StreakController.StreakInfo;
import com.shinka.controller.StudySessionController;
import com.shinka.model.StudySession;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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
    private static final DateTimeFormatter TARGET_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final StudySessionController sessionController;
    private final HeatmapController      heatmapController;
    private final Runnable               onStartStudy;
    private final IntConsumer            onContinue;
    private final StreakController       streakController   = new StreakController();
    private final ProgressController     progressController = new ProgressController();

    // Header
    private Label   dateLabel, titleLabel;
    private Button  continueBtn, newBtn;
    private SVGPath playIcon;
    private int     continueSubjectId = -1;

    // Stats
    private VBox     statsCard;
    private Label    todayCaption, todayValue, todayHint;
    private Label[]  statCaptions = new Label[3];
    private Label[]  statValues   = new Label[3];
    private Region[] statDividers = new Region[3];

    // Streak
    private VBox   streakCard;
    private Label  streakCaption, streakValue, streakBest, streakMessage, streakSub;
    private Region streakDivider;
    private HBox   weekDots;

    // Goals
    private VBox  goalsCard;
    private Label goalsTitle, goalsHint;
    private VBox  goalsBody;

    // Activity
    private VBox        activityCard;
    private Label       activityTitle;
    private HeatmapView heatmapView;

    // Subjects
    private VBox     subjectCard;
    private Label    subjectTitle, subjectTotal;
    private GridPane subjectGrid;

    /** Old constructor kept for compatibility: "Continue" just opens the Study tab. */
    public DashboardView(StudySessionController sessionController,
                         HeatmapController heatmapController,
                         Runnable onStartStudy) {
        this(sessionController, heatmapController, onStartStudy, id -> onStartStudy.run());
    }

    public DashboardView(StudySessionController sessionController,
                         HeatmapController heatmapController,
                         Runnable onStartStudy,
                         IntConsumer onContinue) {
        this.sessionController = sessionController;
        this.heatmapController = heatmapController;
        this.onStartStudy      = onStartStudy;
        this.onContinue        = onContinue;
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
            buildGoalsCard(),
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

        continueBtn = new Button("Continue", playIcon);
        continueBtn.setGraphicTextGap(10);
        continueBtn.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 13));
        continueBtn.setCursor(Cursor.HAND);
        continueBtn.setOnAction(e -> {
            if (continueSubjectId >= 0) onContinue.accept(continueSubjectId);
            else onStartStudy.run();
        });
        continueBtn.setOnMouseEntered(e -> stylePrimary(continueBtn, true));
        continueBtn.setOnMouseExited (e -> stylePrimary(continueBtn, false));

        newBtn = new Button("New session");
        newBtn.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 13));
        newBtn.setCursor(Cursor.HAND);
        newBtn.setOnAction(e -> onStartStudy.run());
        newBtn.setOnMouseEntered(e -> styleGhost(newBtn, true));
        newBtn.setOnMouseExited (e -> styleGhost(newBtn, false));

        HBox header = new HBox(10, titles, newBtn, continueBtn);
        header.setAlignment(Pos.BOTTOM_LEFT);
        return header;
    }

    private VBox buildStatsCard() {
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
            statDividers[i] = vDivider(56);
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

        streakDivider = vDivider(64);

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

    private VBox buildGoalsCard() {
        goalsTitle = label(15, FontWeight.SEMI_BOLD);
        goalsTitle.setText("Goals");
        goalsHint = label(12, FontWeight.NORMAL);
        goalsHint.setText("Weekly goals reset every Monday");

        HBox head = new HBox(goalsTitle, spacer(), goalsHint);
        head.setAlignment(Pos.BASELINE_LEFT);

        goalsBody = new VBox(18);

        goalsCard = new VBox(16, head, goalsBody);
        goalsCard.setPadding(new Insets(22, 24, 24, 24));
        return goalsCard;
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
        subjectGrid.setVgap(12);
        subjectGrid.getColumnConstraints().addAll(
            col(150, 200, false, HPos.LEFT),
            col(80, -1, true, HPos.LEFT),
            col(72, -1, false, HPos.RIGHT),
            col(40, -1, false, HPos.RIGHT)
        );

        subjectCard = new VBox(18, head, subjectGrid);
        subjectCard.setPadding(new Insets(22, 24, 24, 24));
        return subjectCard;
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    public void applyTheme() {
        setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");

        muted(dateLabel);
        primary(titleLabel);

        for (VBox card : new VBox[] {statsCard, streakCard, goalsCard, activityCard, subjectCard}) {
            card.setStyle(ThemeManager.cardStyle());
        }

        muted(todayCaption);
        primary(todayValue);
        muted(todayHint);
        for (int i = 0; i < 3; i++) {
            muted(statCaptions[i]);
            primary(statValues[i]);
            statDividers[i].setStyle("-fx-background-color: " + ThemeManager.border() + ";");
        }

        streakDivider.setStyle("-fx-background-color: " + ThemeManager.border() + ";");
        muted(streakCaption);
        muted(streakBest);
        primary(streakMessage);
        muted(streakSub);

        primary(goalsTitle);
        muted(goalsHint);
        primary(activityTitle);
        primary(subjectTitle);
        muted(subjectTotal);

        refresh();
    }

    private void stylePrimary(Button b, boolean hover) {
        String bg = hover ? ThemeManager.btnPrimaryHover() : ThemeManager.btnPrimaryBg();
        b.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + ThemeManager.btnPrimaryFg() + ";" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 18 10 16;"
        );
        if (b == continueBtn) playIcon.setStyle("-fx-fill: " + ThemeManager.btnPrimaryFg() + ";");
    }

    private void styleGhost(Button b, boolean hover) {
        b.setStyle(
            "-fx-background-color: " + (hover ? ThemeManager.bgDarker() : "transparent") + ";" +
            "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
            "-fx-border-color: " + ThemeManager.border() + ";" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 9 16;"
        );
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

        List<SubjectProgress> progress = progressController.getAll();

        renderHeaderButtons(progress);
        renderStreak(streakController.getStreak());
        renderGoals(progress);
        renderSubjects(progress, totalSec);

        if (heatmapView != null) heatmapView.refresh();
    }

    // ── Continue button ───────────────────────────────────────────────────────

    private void renderHeaderButtons(List<SubjectProgress> progress) {
        continueSubjectId = -1;
        String name = null;

        Optional<StudySession> last = progressController.getLastSession();
        if (last.isPresent()) {
            int id = last.get().getSubjectId();
            for (SubjectProgress p : progress) {
                if (p.subject().getId() == id) { continueSubjectId = id; name = p.subject().getName(); break; }
            }
        }

        boolean canContinue = continueSubjectId >= 0;
        continueBtn.setText(canContinue ? "Continue " + name : "Start session");
        continueBtn.setTooltip(canContinue ? new Tooltip("Start the timer for " + name + " right away") : null);
        continueBtn.setMaxWidth(320);
        newBtn.setVisible(canContinue);
        newBtn.setManaged(canContinue);

        stylePrimary(continueBtn, false);
        styleGhost(newBtn, false);
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

            VBox colBox = new VBox(5, dot, day);
            colBox.setAlignment(Pos.CENTER);
            weekDots.getChildren().add(colBox);
        }
    }

    // ── Goals: weekly + targets ───────────────────────────────────────────────

    private void renderGoals(List<SubjectProgress> progress) {
        goalsBody.getChildren().clear();

        List<SubjectProgress> weekly  = progress.stream().filter(p -> p.subject().hasWeeklyGoal()).toList();
        List<SubjectProgress> targets = progress.stream().filter(p -> p.subject().hasTarget()).toList();

        if (weekly.isEmpty() && targets.isEmpty()) {
            Label empty = label(13, FontWeight.NORMAL);
            empty.setText("No goals yet. Open Subjects, click Edit on a subject, and set a weekly goal or a target date.");
            empty.setWrapText(true);
            muted(empty);
            goalsBody.getChildren().add(empty);
            return;
        }

        if (!weekly.isEmpty()) {
            GridPane g = goalGrid();
            int r = 0;
            for (SubjectProgress p : weekly) {
                long goal = p.weeklyGoalSeconds();
                g.add(nameCell(p, 22), 0, r);
                g.add(bar((double) p.weekSeconds() / goal, p.weeklyDone()), 1, r);
                g.add(valueLabel(fmt(p.weekSeconds()) + " of " + fmt(goal)), 2, r);
                g.add(p.weeklyDone()
                        ? statusLabel("Done", ThemeManager.textAccent())
                        : statusLabel(fmt(goal - p.weekSeconds()) + " left", ThemeManager.textSecondary()), 3, r);
                r++;
            }
            goalsBody.getChildren().add(section("This week", g));
        }

        if (!targets.isEmpty()) {
            GridPane g = goalGrid();
            int r = 0;
            for (SubjectProgress p : targets) {
                long target = p.targetSeconds();
                boolean reached = p.targetStatus() == ProgressController.TargetStatus.REACHED;

                g.add(nameCell(p, 22), 0, r);
                g.add(bar((double) p.totalSeconds() / target, reached), 1, r);
                g.add(valueLabel(fmt(p.totalSeconds()) + " of " + fmt(target)), 2, r);
                g.add(targetStatusLabel(p), 3, r);
                r++;

                Label detail = label(12, FontWeight.NORMAL);
                detail.setText(targetDetail(p));
                detail.setWrapText(true);
                muted(detail);
                GridPane.setMargin(detail, new Insets(-6, 0, 4, 0));
                g.add(detail, 1, r, 3, 1);
                r++;
            }
            goalsBody.getChildren().add(section("Targets", g));
        }
    }

    private String targetDetail(SubjectProgress p) {
        String due = p.subject().getTargetDate();
        try { due = LocalDate.parse(due).format(TARGET_DATE); } catch (Exception ignored) { }

        return switch (p.targetStatus()) {
            case REACHED  -> "Target of " + fmt(p.targetSeconds()) + " reached. Set a new one in Subjects.";
            case OVERDUE  -> "Due " + due + " · " + fmt(p.remainingSeconds()) + " still to go. Pick a new date in Subjects.";
            case ON_TRACK, BEHIND -> "Due " + due + " · " + p.daysLeft() + (p.daysLeft() == 1 ? " day" : " days") + " left · " +
                "need " + pace(p.neededPerDay()) + "/day, your last " + ProgressController.PACE_WINDOW_DAYS +
                " days averaged " + pace(p.recentPerDay()) + "/day";
            default -> "";
        };
    }

    private Label targetStatusLabel(SubjectProgress p) {
        return switch (p.targetStatus()) {
            case ON_TRACK -> statusLabel("On track", ThemeManager.textAccent());
            case BEHIND   -> statusLabel("Behind",   ThemeManager.warning());
            case REACHED  -> statusLabel("Reached",  ThemeManager.textAccent());
            case OVERDUE  -> statusLabel("Overdue",  ThemeManager.danger());
            default       -> statusLabel("", ThemeManager.textSecondary());
        };
    }

    private VBox section(String title, Node content) {
        Label t = label(12, FontWeight.SEMI_BOLD);
        t.setText(title);
        muted(t);
        return new VBox(10, t, content);
    }

    private GridPane goalGrid() {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(12);
        g.getColumnConstraints().addAll(
            col(150, 200, false, HPos.LEFT),
            col(80, -1, true, HPos.LEFT),
            col(120, -1, false, HPos.RIGHT),
            col(80, -1, false, HPos.RIGHT)
        );
        return g;
    }

    // ── By subject ────────────────────────────────────────────────────────────

    private void renderSubjects(List<SubjectProgress> progress, int totalSec) {
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

        Map<String, SubjectProgress> byName = new HashMap<>();
        for (SubjectProgress p : progress) byName.putIfAbsent(p.subject().getName(), p);

        subjectTotal.setText(perSubject.size() + " subject" + (perSubject.size() != 1 ? "s" : ""));

        int max = perSubject.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int row = 0;

        for (Map.Entry<String, Integer> entry : perSubject.entrySet()) {
            int sec = entry.getValue();
            SubjectProgress p = byName.get(entry.getKey());

            Node nameNode;
            if (p != null) {
                nameNode = nameCell(p, 22);
            } else {
                Label name = label(13, FontWeight.NORMAL);
                name.setText(entry.getKey());
                primary(name);
                nameNode = name;
            }

            Label pct = label(12, FontWeight.NORMAL);
            pct.setText(totalSec > 0 ? Math.round(sec * 100.0 / totalSec) + "%" : "–");
            muted(pct);

            subjectGrid.add(nameNode, 0, row);
            subjectGrid.add(bar(max > 0 ? (double) sec / max : 0, false), 1, row);
            subjectGrid.add(valueLabel(fmt(sec)), 2, row);
            subjectGrid.add(pct, 3, row);
            row++;
        }
    }

    // ── Shared pieces ─────────────────────────────────────────────────────────

    private HBox nameCell(SubjectProgress p, double badgeSize) {
        StackPane badge = StageBadge.create(p.stage(), p.next(), p.totalSeconds(), badgeSize);
        Label name = label(13, FontWeight.NORMAL);
        name.setText(p.subject().getName());
        name.setMaxWidth(160);
        primary(name);
        HBox box = new HBox(10, badge, name);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private StackPane bar(double ratio, boolean complete) {
        double r = Math.max(0, Math.min(1, ratio));

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
        fill.setMinWidth(r > 0 ? 6 : 0);
        fill.maxWidthProperty().bind(track.widthProperty().multiply(r));
        fill.setStyle(
            "-fx-background-color: " + (complete ? ThemeManager.btnPrimaryBg() : ThemeManager.textAccent()) + ";" +
            "-fx-background-radius: 3;" +
            (r > 0 ? "" : "-fx-opacity: 0;")
        );

        StackPane bar = new StackPane(track, fill);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMinHeight(18);
        return bar;
    }

    private Label valueLabel(String text) {
        Label l = label(13, FontWeight.SEMI_BOLD);
        l.setText(text);
        primary(l);
        return l;
    }

    private Label statusLabel(String text, String color) {
        Label l = label(12, FontWeight.SEMI_BOLD);
        l.setText(text);
        l.setStyle("-fx-text-fill: " + color + ";");
        return l;
    }

    private static ColumnConstraints col(double min, double pref, boolean grow, HPos align) {
        ColumnConstraints c = new ColumnConstraints();
        c.setMinWidth(min);
        if (pref > 0) c.setPrefWidth(pref);
        if (grow) c.setHgrow(Priority.ALWAYS);
        c.setHalignment(align);
        return c;
    }

    private static Region vDivider(double height) {
        Region d = new Region();
        d.setMinWidth(1);
        d.setMaxWidth(1);
        d.setPrefHeight(height);
        d.setMaxHeight(height);
        return d;
    }

    private static String fmt(long seconds) {
        return seconds <= 0 ? "0m" : StudySessionController.formatSeconds(seconds);
    }

    /** Pace values rounded up to whole minutes ("41m/day", not "40m 37s/day"). */
    private static String pace(long secondsPerDay) {
        if (secondsPerDay <= 0) return "0m";
        return StudySessionController.formatSeconds((long) Math.ceil(secondsPerDay / 60.0) * 60);
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
