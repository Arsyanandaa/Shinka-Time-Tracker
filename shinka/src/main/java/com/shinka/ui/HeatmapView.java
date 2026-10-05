package com.shinka.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.shinka.controller.HeatmapController;
import com.shinka.controller.StudySessionController;
import com.shinka.model.StudyDaySummary;
import com.shinka.model.StudySession;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Year heatmap of study activity.
 * Transparent background: the parent card provides the surface.
 */
public class HeatmapView extends VBox {

    private static final int CELL_SIZE = 12;
    private static final int CELL_GAP  = 3;
    private static final int WEEKS     = 53;

    // Level thresholds in seconds: <30m, <1h, <2h, >=2h
    private static final long[] THRESHOLDS = { 30 * 60, 60 * 60, 2 * 60 * 60 };

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);

    private final HeatmapController heatmapController;

    private Label    countLabel;
    private HBox     yearButtonBar;
    private GridPane grid;
    private HBox     legend;
    private VBox     detailPanel;

    private int       selectedYear = LocalDate.now().getYear();
    private LocalDate selectedDate = null;

    public HeatmapView(HeatmapController heatmapController) {
        this.heatmapController = heatmapController;
        buildUI();
        refresh();
    }

    private void buildUI() {
        setSpacing(0);
        setStyle("-fx-background-color: transparent;");

        countLabel = new Label();
        countLabel.setFont(Font.font(ThemeManager.FONT, FontWeight.NORMAL, 12));

        yearButtonBar = new HBox(4);
        yearButtonBar.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(yearButtonBar, Priority.ALWAYS);

        HBox topBar = new HBox(12, countLabel, yearButtonBar);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 16, 0));

        grid = new GridPane();
        grid.setHgap(CELL_GAP);
        grid.setVgap(CELL_GAP);

        legend = new HBox(4);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.setPadding(new Insets(14, 0, 0, 0));

        detailPanel = new VBox(8);
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);

        getChildren().addAll(topBar, grid, legend, detailPanel);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void refresh() {
        Map<String, StudyDaySummary> summaryMap = heatmapController.getYearSummary();

        countLabel.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + ";");
        rebuildYearButtons(summaryMap);
        rebuildLegend();
        rebuildGrid(summaryMap);

        if (selectedDate != null && selectedDate.getYear() == selectedYear) {
            showDetail(selectedDate, summaryMap.get(selectedDate.format(DATE_FMT)));
        } else {
            hideDetail();
        }
    }

    private void rebuildGrid(Map<String, StudyDaySummary> summaryMap) {
        grid.getChildren().clear();

        int       year  = selectedYear;
        LocalDate today = LocalDate.now();

        int sessions = 0;
        long seconds = 0;
        for (Map.Entry<String, StudyDaySummary> e : summaryMap.entrySet()) {
            if (e.getKey().startsWith(year + "-")) {
                sessions += e.getValue().getSessions().size();
                seconds  += e.getValue().getTotalSeconds();
            }
        }
        countLabel.setText(sessions == 0
            ? "No sessions in " + year
            : sessions + " session" + (sessions != 1 ? "s" : "") +
              " · " + StudySessionController.formatSeconds(seconds) + " in " + year);

        // Day labels
        String[] dayLabels = {"Mon", "", "Wed", "", "Fri", "", "Sun"};
        for (int row = 0; row < 7; row++) {
            Label lbl = smallMuted(dayLabels[row]);
            lbl.setMinWidth(28);
            grid.add(lbl, 0, row + 1);
        }

        LocalDate startDate = LocalDate.of(year, 1, 1).with(DayOfWeek.MONDAY);
        int lastMonth = -1;

        for (int week = 0; week < WEEKS; week++) {
            LocalDate weekStart = startDate.plusWeeks(week);

            // Month label on the first week that contains the 1st..7th of a month
            LocalDate labelDay = weekStart.plusDays(6);
            int month = labelDay.getMonthValue();
            if (month != lastMonth && labelDay.getYear() == year) {
                Label m = smallMuted(labelDay.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                m.setMinWidth(Region.USE_PREF_SIZE);
                grid.add(m, week + 1, 0, 3, 1);
                lastMonth = month;
            }

            for (int dow = 0; dow < 7; dow++) {
                LocalDate date = weekStart.plusDays(dow);
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                cell.setArcWidth(4);
                cell.setArcHeight(4);

                if (date.getYear() != year) {
                    cell.setFill(Color.TRANSPARENT);
                    grid.add(cell, week + 1, dow + 1);
                    continue;
                }

                // Future days: faint placeholder so the year keeps its shape
                if (date.isAfter(today)) {
                    cell.setFill(Color.web(ThemeManager.heatLevel(0)));
                    cell.setOpacity(0.4);
                    grid.add(cell, week + 1, dow + 1);
                    continue;
                }

                StudyDaySummary summary = summaryMap.get(date.format(DATE_FMT));
                long sec = summary == null ? 0 : summary.getTotalSeconds();
                cell.setFill(Color.web(ThemeManager.heatLevel(level(sec))));
                cell.setCursor(Cursor.HAND);

                if (date.equals(today) || date.equals(selectedDate)) {
                    cell.setStroke(Color.web(ThemeManager.textPrimary()));
                    cell.setStrokeWidth(1.2);
                    cell.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);
                }

                Tooltip tip = new Tooltip(tooltipText(date, summary));
                tip.setShowDelay(Duration.millis(120));
                tip.setStyle(
                    "-fx-font-family: '" + ThemeManager.FONT + "';" +
                    "-fx-font-size: 11;" +
                    "-fx-background-color: " + ThemeManager.textPrimary() + ";" +
                    "-fx-text-fill: " + ThemeManager.bgMain() + ";" +
                    "-fx-background-radius: 6;" +
                    "-fx-padding: 6 10;"
                );
                Tooltip.install(cell, tip);

                cell.setOnMouseEntered(e -> cell.setOpacity(0.75));
                cell.setOnMouseExited (e -> cell.setOpacity(1.0));
                cell.setOnMouseClicked(e -> {
                    selectedDate = date.equals(selectedDate) ? null : date;
                    refresh();
                });

                grid.add(cell, week + 1, dow + 1);
            }
        }
    }

    private static int level(long seconds) {
        if (seconds <= 0)              return 0;
        if (seconds < THRESHOLDS[0])   return 1;
        if (seconds < THRESHOLDS[1])   return 2;
        if (seconds < THRESHOLDS[2])   return 3;
        return 4;
    }

    // ── Year switcher ─────────────────────────────────────────────────────────

    private void rebuildYearButtons(Map<String, StudyDaySummary> summaryMap) {
        yearButtonBar.getChildren().clear();

        Set<Integer> years = new TreeSet<>();
        years.add(LocalDate.now().getYear());
        for (String date : summaryMap.keySet()) {
            try { years.add(Integer.parseInt(date.substring(0, 4))); }
            catch (Exception ignored) {}
        }

        // Only show the switcher when there is more than one year
        if (years.size() < 2) return;

        List<Integer> sorted = new ArrayList<>(years);
        Collections.sort(sorted, Collections.reverseOrder());
        for (int y : sorted) yearButtonBar.getChildren().add(yearButton(y));
    }

    private Button yearButton(int year) {
        Button btn = new Button(String.valueOf(year));
        btn.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 11));
        btn.setCursor(Cursor.HAND);
        styleYearButton(btn, year == selectedYear, false);

        btn.setOnMouseEntered(e -> styleYearButton(btn, year == selectedYear, true));
        btn.setOnMouseExited (e -> styleYearButton(btn, year == selectedYear, false));
        btn.setOnAction(e -> {
            selectedYear = year;
            selectedDate = null;
            refresh();
        });
        return btn;
    }

    private void styleYearButton(Button btn, boolean selected, boolean hover) {
        String bg = selected || hover ? ThemeManager.bgDarker() : "transparent";
        String fg = selected ? ThemeManager.textPrimary() : ThemeManager.textSecondary();
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 10;"
        );
    }

    // ── Legend ────────────────────────────────────────────────────────────────

    private void rebuildLegend() {
        legend.getChildren().clear();
        Label less = smallMuted("Less");
        less.setPadding(new Insets(0, 4, 0, 0));
        legend.getChildren().add(less);
        for (int i = 0; i <= 4; i++) {
            Rectangle r = new Rectangle(CELL_SIZE, CELL_SIZE);
            r.setArcWidth(4);
            r.setArcHeight(4);
            r.setFill(Color.web(ThemeManager.heatLevel(i)));
            legend.getChildren().add(r);
        }
        Label more = smallMuted("More");
        more.setPadding(new Insets(0, 0, 0, 4));
        legend.getChildren().add(more);
    }

    // ── Day detail ────────────────────────────────────────────────────────────

    private void showDetail(LocalDate date, StudyDaySummary summary) {
        detailPanel.getChildren().clear();
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        detailPanel.setPadding(new Insets(16, 0, 0, 0));

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMinHeight(1);
        divider.setStyle("-fx-background-color: " + ThemeManager.border() + ";");
        VBox.setMargin(divider, new Insets(0, 0, 8, 0));

        Label dateLbl = new Label(date.format(DISPLAY_FMT));
        dateLbl.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 13));
        dateLbl.setStyle("-fx-text-fill: " + ThemeManager.textPrimary() + ";");

        Label totalLbl = new Label(summary == null ? ""
            : StudySessionController.formatSeconds(summary.getTotalSeconds()) + " total");
        totalLbl.setFont(Font.font(ThemeManager.FONT, FontWeight.NORMAL, 12));
        totalLbl.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + ";");

        Button closeBtn = new Button("Close");
        closeBtn.setFont(Font.font(ThemeManager.FONT, FontWeight.NORMAL, 12));
        closeBtn.setCursor(Cursor.HAND);
        closeBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + ThemeManager.textSecondary() + ";" +
            "-fx-padding: 2 4;"
        );
        closeBtn.setOnAction(e -> { selectedDate = null; refresh(); });

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox header = new HBox(10, dateLbl, totalLbl, grow, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        detailPanel.getChildren().addAll(divider, header);

        if (summary == null || summary.getSessions().isEmpty()) {
            Label none = new Label("No study sessions on this day.");
            none.setFont(Font.font(ThemeManager.FONT, FontWeight.NORMAL, 12));
            none.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + ";");
            detailPanel.getChildren().add(none);
            return;
        }

        for (StudySession s : summary.getSessions()) {
            Region dot = new Region();
            dot.setMinSize(6, 6);
            dot.setMaxSize(6, 6);
            dot.setStyle(
                "-fx-background-color: " + ThemeManager.textAccent() + ";" +
                "-fx-background-radius: 3;"
            );

            Label subject = new Label(s.getSubjectName());
            subject.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 12));
            subject.setStyle("-fx-text-fill: " + ThemeManager.textPrimary() + ";");

            String t = (s.getTitle() == null || s.getTitle().isBlank()) ? "" : s.getTitle();
            Label title = new Label(t);
            title.setFont(Font.font(ThemeManager.FONT, FontWeight.NORMAL, 12));
            title.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + ";");
            HBox.setHgrow(title, Priority.ALWAYS);
            title.setMaxWidth(Double.MAX_VALUE);

            Label dur = new Label(s.getFormattedDuration());
            dur.setFont(Font.font(ThemeManager.FONT, FontWeight.SEMI_BOLD, 12));
            dur.setStyle("-fx-text-fill: " + ThemeManager.textPrimary() + ";");

            HBox row = new HBox(10, dot, subject, title, dur);
            row.setAlignment(Pos.CENTER_LEFT);
            detailPanel.getChildren().add(row);
        }
    }

    private void hideDetail() {
        detailPanel.getChildren().clear();
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String tooltipText(LocalDate date, StudyDaySummary summary) {
        String day = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH));
        if (summary == null) return "No study · " + day;
        int n = summary.getSessions().size();
        return StudySessionController.formatSeconds(summary.getTotalSeconds()) +
               " · " + n + " session" + (n != 1 ? "s" : "") + " · " + day;
    }

    private Label smallMuted(String text) {
        Label l = new Label(text);
        l.setFont(Font.font(ThemeManager.FONT, FontWeight.NORMAL, 10));
        l.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + ";");
        return l;
    }
}