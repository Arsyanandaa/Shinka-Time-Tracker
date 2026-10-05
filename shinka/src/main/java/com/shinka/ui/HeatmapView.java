package com.shinka.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class HeatmapView extends VBox {

    private static final int CELL_SIZE = 11;
    private static final int CELL_GAP  = 3;
    private static final int WEEKS     = 53;

    private static final String BG_COLOR     = "#161b22";
    private static final String BORDER_COLOR = "#30363d";
    private static final Color  COLOR_EMPTY  = Color.web("#21262d");
    private static final Color  COLOR_ACTIVE = Color.web("#39d353"); // 1 warna hijau terang

    private final HeatmapController heatmapController;
    private GridPane grid;
    private VBox     detailPanel;
    private Label    contributionCountLabel;

    // ── Year state
    private int selectedYear = LocalDate.now().getYear();
    private HBox yearButtonBar;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    public HeatmapView(HeatmapController heatmapController) {
        this.heatmapController = heatmapController;
        buildUI();
        refresh();
    }

    private void buildUI() {
        setSpacing(0);
        setPadding(new Insets(0));
        setStyle(
            "-fx-background-color: " + BG_COLOR + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1;"
        );

        // ── Top bar: count + year switcher
        contributionCountLabel = new Label("");
        contributionCountLabel.setStyle(
            "-fx-text-fill: #e6edf3;" +
            "-fx-font-size: 13;" +
            "-fx-font-weight: bold;"
        );

        yearButtonBar = new HBox(6);
        yearButtonBar.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(yearButtonBar, Priority.ALWAYS);

        HBox topBar = new HBox(12, contributionCountLabel, yearButtonBar);
        topBar.setPadding(new Insets(14, 16, 10, 16));
        topBar.setAlignment(Pos.CENTER_LEFT);

        // ── Grid
        grid = new GridPane();
        grid.setHgap(CELL_GAP);
        grid.setVgap(CELL_GAP);

        HBox gridWrapper = new HBox(grid);
        gridWrapper.setPadding(new Insets(4, 16, 8, 16));

        // ── Legend
        HBox legendBar = new HBox(buildLegend());
        legendBar.setPadding(new Insets(6, 16, 12, 16));
        legendBar.setAlignment(Pos.CENTER_RIGHT);

        // ── Detail panel
        detailPanel = new VBox(6);
        detailPanel.setPadding(new Insets(12, 16, 12, 16));
        detailPanel.setStyle(
            "-fx-background-color: #21262d;" +
            "-fx-border-color: " + BORDER_COLOR + ";" +
            "-fx-border-width: 1 0 0 0;"
        );
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);

        getChildren().addAll(topBar, gridWrapper, legendBar, detailPanel);
    }

    public void refresh() {
        grid.getChildren().clear();

        // ── Rebuild year buttons (realtime: years that have data)
        rebuildYearButtons();

        Map<String, StudyDaySummary> summaryMap = heatmapController.getYearSummary();

        // Filter only selected year
        LocalDate today     = LocalDate.now();
        int       year      = selectedYear;
        LocalDate yearStart = LocalDate.of(year, 1, 1).with(DayOfWeek.MONDAY);
        LocalDate yearEnd   = LocalDate.of(year, 12, 31);

        // If selected year is current year, end at today
        if (year == today.getYear()) {
            yearEnd = today;
        }

        // Count sessions in selected year
        int totalSessions = 0;
        for (Map.Entry<String, StudyDaySummary> entry : summaryMap.entrySet()) {
            String dateStr = entry.getKey();
            if (dateStr.startsWith(year + "")) {
                totalSessions += entry.getValue().getSessions().size();
            }
        }
        contributionCountLabel.setText(
            totalSessions + " study session" + (totalSessions != 1 ? "s" : "") +
            " in " + year
        );

        // ── Day-of-week labels
        String[] dayLabels = {"Mon", "", "Wed", "", "Fri", "", "Sun"};
        for (int row = 0; row < 7; row++) {
            Label lbl = new Label(dayLabels[row]);
            lbl.setStyle("-fx-font-size: 9; -fx-text-fill: #7d8590;");
            lbl.setMinWidth(26);
            lbl.setAlignment(Pos.CENTER_RIGHT);
            grid.add(lbl, 0, row + 1);
        }

        // ── Month labels + cells
        LocalDate startDate = LocalDate.of(year, 1, 1).with(DayOfWeek.MONDAY);
        int lastMonth = -1;

        for (int week = 0; week < WEEKS; week++) {
            LocalDate weekStart = startDate.plusWeeks(week);

            // Month label
            int month = weekStart.getMonthValue();
            if (month != lastMonth && weekStart.getYear() == year) {
                Label monthLbl = new Label(
                    weekStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                );
                monthLbl.setStyle("-fx-font-size: 9; -fx-text-fill: #7d8590;");
                grid.add(monthLbl, week + 1, 0);
                lastMonth = month;
            }

            for (int dow = 0; dow < 7; dow++) {
                LocalDate cellDate = weekStart.plusDays(dow);

                // Only show current year
                if (cellDate.getYear() != year) {
                    Rectangle ph = new Rectangle(CELL_SIZE, CELL_SIZE);
                    ph.setFill(Color.TRANSPARENT);
                    grid.add(ph, week + 1, dow + 1);
                    continue;
                }

                // Skip future dates
                if (cellDate.isAfter(today)) {
                    Rectangle ph = new Rectangle(CELL_SIZE, CELL_SIZE);
                    ph.setFill(Color.TRANSPARENT);
                    grid.add(ph, week + 1, dow + 1);
                    continue;
                }

                String          dateStr = cellDate.format(DATE_FMT);
                StudyDaySummary summary = summaryMap.get(dateStr);

                // 1 warna: abu kalau tidak belajar, hijau terang kalau belajar
                Color cellColor = (summary != null) ? COLOR_ACTIVE : COLOR_EMPTY;

                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                cell.setFill(cellColor);
                cell.setArcWidth(2);
                cell.setArcHeight(2);
                cell.setCursor(Cursor.HAND);

                // Highlight today
                if (cellDate.equals(today)) {
                    cell.setStroke(Color.web("#58a6ff"));
                    cell.setStrokeWidth(1.5);
                }

                // Tooltip
                Tooltip tip = new Tooltip(buildTooltip(cellDate, summary));
                tip.setStyle(
                    "-fx-font-size: 11;" +
                    "-fx-background-color: #1c2128;" +
                    "-fx-text-fill: #e6edf3;" +
                    "-fx-border-color: #30363d;" +
                    "-fx-border-width: 1;"
                );
                Tooltip.install(cell, tip);

                final LocalDate       fd = cellDate;
                final StudyDaySummary fs = summary;
                cell.setOnMouseClicked(e -> showDetail(fd, fs));
                cell.setOnMouseEntered(e -> cell.setOpacity(0.7));
                cell.setOnMouseExited (e -> cell.setOpacity(1.0));

                grid.add(cell, week + 1, dow + 1);
            }
        }
    }

    // ── Year buttons ──────────────────────────────────────────────────────────

    private void rebuildYearButtons() {
        yearButtonBar.getChildren().clear();

        Map<String, StudyDaySummary> summaryMap = heatmapController.getYearSummary();
        int currentYear = LocalDate.now().getYear();

        // Collect years that have data + always include current year
        java.util.Set<Integer> years = new java.util.TreeSet<>();
        years.add(currentYear);
        for (String date : summaryMap.keySet()) {
            try {
                years.add(Integer.parseInt(date.substring(0, 4)));
            } catch (Exception ignored) {}
        }

        // Show buttons newest first
        java.util.List<Integer> sortedYears = new java.util.ArrayList<>(years);
        java.util.Collections.sort(sortedYears, java.util.Collections.reverseOrder());

        for (int y : sortedYears) {
            Button btn = yearButton(y);
            yearButtonBar.getChildren().add(btn);
        }
    }

    private Button yearButton(int year) {
        Button btn = new Button(String.valueOf(year));
        btn.setFont(Font.font("System", FontWeight.BOLD, 11));

        boolean isSelected = (year == selectedYear);
        btn.setStyle(
            isSelected
            ? "-fx-background-color: #1f6feb;" +
              "-fx-text-fill: white;" +
              "-fx-background-radius: 6;" +
              "-fx-padding: 3 10;" +
              "-fx-cursor: hand;"
            : "-fx-background-color: transparent;" +
              "-fx-text-fill: #7d8590;" +
              "-fx-background-radius: 6;" +
              "-fx-border-color: #30363d;" +
              "-fx-border-radius: 6;" +
              "-fx-border-width: 1;" +
              "-fx-padding: 3 10;" +
              "-fx-cursor: hand;"
        );

        btn.setOnMouseEntered(e -> {
            if (year != selectedYear) {
                btn.setStyle(
                    "-fx-background-color: #21262d;" +
                    "-fx-text-fill: #e6edf3;" +
                    "-fx-background-radius: 6;" +
                    "-fx-border-color: #58a6ff;" +
                    "-fx-border-radius: 6;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 3 10;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        btn.setOnMouseExited(e -> {
            if (year != selectedYear) {
                btn.setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-text-fill: #7d8590;" +
                    "-fx-background-radius: 6;" +
                    "-fx-border-color: #30363d;" +
                    "-fx-border-radius: 6;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 3 10;" +
                    "-fx-cursor: hand;"
                );
            }
        });

        btn.setOnAction(e -> {
            selectedYear = year;
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
            refresh();
        });

        return btn;
    }

    // ── Detail panel ──────────────────────────────────────────────────────────

    private void showDetail(LocalDate date, StudyDaySummary summary) {
    detailPanel.getChildren().clear();
    detailPanel.setVisible(true);
    detailPanel.setManaged(true);

    // ── Close button
    Button closeBtn = new Button("✕");
    closeBtn.setStyle(
        "-fx-background-color: transparent;" +
        "-fx-text-fill: #7d8590;" +
        "-fx-font-size: 13;" +
        "-fx-padding: 0 4;" +
        "-fx-cursor: hand;"
    );
    closeBtn.setOnAction(e -> {
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
    });

    HBox headerRow = new HBox();
    headerRow.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(headerRow, Priority.ALWAYS);

        Label dateLbl = new Label(date.format(DISPLAY_FMT));
dateLbl.setFont(Font.font("System", FontWeight.BOLD, 12));
dateLbl.setStyle("-fx-text-fill: #e6edf3;");
HBox.setHgrow(dateLbl, Priority.ALWAYS);

headerRow.getChildren().addAll(dateLbl, closeBtn);
detailPanel.getChildren().add(headerRow);

        if (summary == null || summary.getSessions().isEmpty()) {
            Label noStudy = new Label("No study sessions on this day.");
            noStudy.setStyle("-fx-text-fill: #7d8590; -fx-font-size: 12;");
            detailPanel.getChildren().add(noStudy);
        } else {
            for (StudySession s : summary.getSessions()) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Label bullet = new Label("▸");
                bullet.setStyle("-fx-text-fill: #39d353; -fx-font-weight: bold;");

                String t = (s.getTitle() == null || s.getTitle().isEmpty())
                    ? "(no title)" : s.getTitle();

                Label sessionLbl = new Label(s.getSubjectName() + " — " + t);
                sessionLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");
                HBox.setHgrow(sessionLbl, Priority.ALWAYS);

                Label timeLbl = new Label(s.getFormattedDuration());
                timeLbl.setStyle(
                    "-fx-font-size: 11; -fx-text-fill: #0d1117;" +
                    "-fx-background-color: #39d353;" +
                    "-fx-background-radius: 10; -fx-padding: 1 8;"
                );

                row.getChildren().addAll(bullet, sessionLbl, timeLbl);
                detailPanel.getChildren().add(row);
            }

            Label sep = new Label("──────────────────────────");
            sep.setStyle("-fx-text-fill: #30363d; -fx-font-size: 10;");

            Label totalLbl = new Label(
                "Total: " + StudySessionController.formatSeconds(summary.getTotalSeconds())
            );
            totalLbl.setFont(Font.font("System", FontWeight.BOLD, 12));
            totalLbl.setStyle("-fx-text-fill: #39d353;");

            detailPanel.getChildren().addAll(sep, totalLbl);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildTooltip(LocalDate date, StudyDaySummary summary) {
        if (summary == null) {
            return "No study on " + date.format(DISPLAY_FMT);
        }
        return summary.getSessions().size() + " session(s) on " +
               date.format(DISPLAY_FMT) + "\n" +
               StudySessionController.formatSeconds(summary.getTotalSeconds()) + " studied";
    }

    private HBox buildLegend() {
        Label lessLbl = new Label("Less");
        lessLbl.setStyle("-fx-font-size: 10; -fx-text-fill: #7d8590;");
        Label moreLbl = new Label("More");
        moreLbl.setStyle("-fx-font-size: 10; -fx-text-fill: #7d8590;");

        HBox legend = new HBox(3, lessLbl, legendBox(COLOR_EMPTY), legendBox(COLOR_ACTIVE), moreLbl);
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    private Rectangle legendBox(Color color) {
        Rectangle r = new Rectangle(CELL_SIZE, CELL_SIZE);
        r.setFill(color);
        r.setArcWidth(2);
        r.setArcHeight(2);
        return r;
    }
}