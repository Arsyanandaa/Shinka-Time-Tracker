package com.shinka.ui;

import java.util.Map;

import com.shinka.controller.HeatmapController;
import com.shinka.controller.StudySessionController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardView extends VBox {

    private final StudySessionController sessionController;
    private final HeatmapController      heatmapController;
    private final Runnable               onStartStudy;

    private Label       todayValue, weekValue, monthValue, totalValue;
    private VBox        subjectBreakdown;
    private HeatmapView heatmapView;
    private VBox        heatmapCard;
    private VBox        subjectCard;
    private Button      startBtn;
    private Label       appTitle, subtitle, statsHeading, heatmapHeading, breakdownHeading;
    private Separator   sep1, sep2, sep3;

    public DashboardView(StudySessionController sessionController,
                         HeatmapController heatmapController,
                         Runnable onStartStudy) {
        this.sessionController = sessionController;
        this.heatmapController = heatmapController;
        this.onStartStudy      = onStartStudy;
        buildUI();
        refresh();
    }

    private void buildUI() {
        setSpacing(20);
        setPadding(new Insets(30));

        appTitle = new Label("🌱 Shinka – Time to Master");
        appTitle.setFont(Font.font("System", FontWeight.BOLD, 22));

        subtitle = new Label("Your learning journey, one session at a time.");

        statsHeading    = sectionHeading("📊 Study Stats");
        heatmapHeading  = sectionHeading("🟩 Study Heatmap");
        breakdownHeading= sectionHeading("📚 Time Per Subject");

        todayValue = new Label(); weekValue = new Label();
        monthValue = new Label(); totalValue = new Label();

        HBox cards = new HBox(15,
            statCard("Today",      todayValue),
            statCard("This Week",  weekValue),
            statCard("This Month", monthValue),
            statCard("All Time",   totalValue)
        );
        cards.setAlignment(Pos.CENTER_LEFT);

        heatmapView = new HeatmapView(heatmapController);
        heatmapCard = new VBox(10, heatmapHeading, heatmapView);
        heatmapCard.setPadding(new Insets(16));

        subjectBreakdown = new VBox(8);
        subjectCard = new VBox(10, subjectBreakdown);
        subjectCard.setPadding(new Insets(16));

        startBtn = new Button("▶  Start Study Session");
        startBtn.setFont(Font.font("System", FontWeight.BOLD, 14));
        startBtn.setOnAction(e -> onStartStudy.run());

        sep1 = new Separator(); sep2 = new Separator(); sep3 = new Separator();

        getChildren().addAll(
            appTitle, subtitle, sep1,
            statsHeading, cards, sep2,
            heatmapCard, sep3,
            breakdownHeading, subjectCard,
            startBtn
        );

        applyTheme();
    }

    public void applyTheme() {
        setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");
        appTitle.setStyle("-fx-text-fill: " + ThemeManager.textHeading() + ";");
        subtitle.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + ";");
        statsHeading.setStyle("-fx-text-fill: " + ThemeManager.textPrimary() + "; -fx-font-weight: bold; -fx-font-size: 15;");
        heatmapHeading.setStyle("-fx-text-fill: " + ThemeManager.textPrimary() + "; -fx-font-weight: bold; -fx-font-size: 15;");
        breakdownHeading.setStyle("-fx-text-fill: " + ThemeManager.textPrimary() + "; -fx-font-weight: bold; -fx-font-size: 15;");

        heatmapCard.setStyle(ThemeManager.cardStyle() + " -fx-padding: 16;");
        subjectCard.setStyle(ThemeManager.cardStyle() + " -fx-padding: 16;");

        startBtn.setStyle(
            "-fx-background-color: " + ThemeManager.btnPrimaryBg() + ";" +
            "-fx-text-fill: " + ThemeManager.btnPrimaryFg() + ";" +
            "-fx-padding: 12 28; -fx-background-radius: 8; -fx-font-weight: bold;"
        );

        // Update stat card labels
        for (javafx.scene.Node node : ((HBox) getChildren().get(4)).getChildren()) {
            if (node instanceof VBox card) {
                card.setStyle(ThemeManager.cardStyle() + " -fx-min-width: 120; -fx-padding: 16 20;");
                for (javafx.scene.Node child : card.getChildren()) {
                    if (child instanceof Label lbl) {
                        String txt = lbl.getText();
                        boolean isValue = lbl.getFont().getSize() > 15;
                        lbl.setStyle(isValue
                            ? "-fx-text-fill: " + ThemeManager.textAccent() + "; -fx-font-weight: bold;"
                            : "-fx-text-fill: " + ThemeManager.textSecondary() + "; -fx-font-size: 11;"
                        );
                    }
                }
            }
        }

        if (heatmapView != null) heatmapView.refresh();
        refresh();
    }

    public void refresh() {
        if (todayValue == null) return;
        todayValue.setText(StudySessionController.formatSeconds(sessionController.getSecondsToday()));
        weekValue .setText(StudySessionController.formatSeconds(sessionController.getSecondsThisWeek()));
        monthValue.setText(StudySessionController.formatSeconds(sessionController.getSecondsThisMonth()));
        totalValue.setText(StudySessionController.formatSeconds(sessionController.getTotalSeconds()));

        subjectBreakdown.getChildren().clear();
        Map<String, Integer> perSubject = sessionController.getSecondsPerSubject();

        if (perSubject.isEmpty()) {
            Label empty = new Label("No sessions recorded yet. Start studying! 🚀");
            empty.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + "; -fx-font-size: 13;");
            subjectBreakdown.getChildren().add(empty);
        } else {
            for (Map.Entry<String, Integer> entry : perSubject.entrySet()) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);

                Label dot = new Label("●");
                dot.setStyle("-fx-text-fill: " + ThemeManager.textAccent() + "; -fx-font-size: 10;");

                Label nameLbl = new Label(entry.getKey());
                nameLbl.setStyle("-fx-text-fill: " + ThemeManager.textPrimary() + "; -fx-font-size: 13;");
                nameLbl.setMinWidth(160);
                HBox.setHgrow(nameLbl, Priority.ALWAYS);

                Label timeLbl = new Label(StudySessionController.formatSeconds(entry.getValue()));
                timeLbl.setStyle(
                    "-fx-font-size: 12; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + ThemeManager.btnPrimaryFg() + ";" +
                    "-fx-background-color: " + ThemeManager.textAccent() + ";" +
                    "-fx-background-radius: 20; -fx-padding: 2 10;"
                );

                row.getChildren().addAll(dot, nameLbl, timeLbl);
                subjectBreakdown.getChildren().add(row);
            }
        }

        if (heatmapView != null) heatmapView.refresh();
    }

    private VBox statCard(String title, Label valueLabel) {
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        Label titleLabel = new Label(title);
        VBox card = new VBox(4, valueLabel, titleLabel);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setMinWidth(120);
        return card;
    }

    private Label sectionHeading(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 15));
        return lbl;
    }
}