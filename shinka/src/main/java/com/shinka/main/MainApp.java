package com.shinka.main;

import com.shinka.controller.HeatmapController;
import com.shinka.controller.StudySessionController;
import com.shinka.controller.SubjectController;
import com.shinka.database.DatabaseManager;
import com.shinka.ui.DashboardView;
import com.shinka.ui.HistoryView;
import com.shinka.ui.StudySessionView;
import com.shinka.ui.SubjectsView;
import com.shinka.ui.ThemeManager;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainApp extends Application {

    private DashboardView    dashboardView;
    private SubjectsView     subjectsView;
    private StudySessionView studySessionView;
    private HistoryView      historyView;
    private BorderPane       root;
    private Button           themeToggleBtn;
    private HBox             navbar;

    // Navbar tab buttons
    private Button[] navBtns;
    private int      activeTab = 0;

    // Content area
    private StackPane contentArea;
    private ScrollPane dashScroll, studyScroll;

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.createTablesIfNeeded();

        SubjectController      subjectController = new SubjectController();
        StudySessionController sessionController = new StudySessionController();
        HeatmapController      heatmapController = new HeatmapController();

        subjectsView = new SubjectsView(subjectController);
        historyView  = new HistoryView(sessionController);

        dashboardView = new DashboardView(
            sessionController,
            heatmapController,
            () -> switchTab(2)
        );

        studySessionView = new StudySessionView(
            sessionController,
            subjectsView.getSubjects(),
            () -> {
                historyView.refresh();
                dashboardView.refresh();
            }
        );

        dashScroll  = styledScroll(dashboardView);
        studyScroll = styledScroll(studySessionView);
        ScrollPane subjectScroll = styledScroll(subjectsView);
        ScrollPane historyScroll = styledScroll(historyView);

        // ── Content area
        contentArea = new StackPane(dashScroll);
        contentArea.setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");

        // ── Navbar
        navbar = buildNavbar();

        root = new BorderPane();
        root.setTop(navbar);
        root.setCenter(contentArea);
        root.setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");

        // Store scroll panes for switching
        dashScroll    .setUserData(0);
        subjectScroll .setUserData(1);
        studyScroll   .setUserData(2);
        historyScroll .setUserData(3);

        switchTab(0);

        Scene scene = new Scene(root, 980, 720);
        primaryStage.setTitle("Shinka – Time to Master");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(780);
        primaryStage.setMinHeight(560);
        primaryStage.show();

        dashboardView.refresh();
    }

    // ── Build Navbar ──────────────────────────────────────────────────────────

    private HBox buildNavbar() {
        // ── Logo (kiri)
        Label logo = new Label("🌱 Shinka");
        logo.setFont(Font.font("System", FontWeight.BOLD, 15));
        logo.setStyle("-fx-text-fill: " + ThemeManager.textHeading() + ";");
        logo.setCursor(javafx.scene.Cursor.HAND);
        logo.setOnMouseClicked(e -> switchTab(0));

        HBox logoBox = new HBox(logo);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(logoBox, Priority.ALWAYS);

        // ── Nav tabs (tengah)
        String[] labels = {"🏠 Dashboard", "📚 Subjects", "⏱ Study", "📖 History"};
        navBtns = new Button[labels.length];
        HBox navTabs = new HBox(4);
        navTabs.setAlignment(Pos.CENTER);

        for (int i = 0; i < labels.length; i++) {
            Button btn = new Button(labels[i]);
            btn.setFont(Font.font("System", FontWeight.NORMAL, 13));
            btn.setCursor(javafx.scene.Cursor.HAND);
            final int idx = i;
            btn.setOnAction(e -> switchTab(idx));
            navBtns[i] = btn;
            navTabs.getChildren().add(btn);
        }

        // ── Theme toggle (kanan)
        themeToggleBtn = new Button("🌙 Dark");
        themeToggleBtn.setFont(Font.font("System", FontWeight.BOLD, 12));
        themeToggleBtn.setCursor(javafx.scene.Cursor.HAND);
        themeToggleBtn.setOnAction(e -> toggleTheme());
        styleThemeBtn();

        HBox rightBox = new HBox(themeToggleBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        // ── Assemble navbar
        HBox nav = new HBox(20, logoBox, navTabs, rightBox);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(10, 20, 10, 20));
        styleNavbar(nav);

        styleNavBtns();
        return nav;
    }

    // ── Switch Tab ────────────────────────────────────────────────────────────

    private void switchTab(int idx) {
        activeTab = idx;
        styleNavBtns();

        javafx.scene.Node[] views = {dashScroll, styledScroll(subjectsView), studyScroll, styledScroll(historyView)};

        // Rebuild scrollpanes fresh to avoid stale refs
        ScrollPane dash    = styledScroll(dashboardView);
        ScrollPane subject = styledScroll(subjectsView);
        ScrollPane study   = styledScroll(studySessionView);
        ScrollPane history = styledScroll(historyView);

        contentArea.getChildren().setAll(
            idx == 0 ? dash :
            idx == 1 ? subject :
            idx == 2 ? study  : history
        );

        // Refresh data
        if (idx == 0) dashboardView.refresh();
        if (idx == 1) subjectsView.loadSubjects();
        if (idx == 3) historyView.refresh();
    }

    // ── Styling ───────────────────────────────────────────────────────────────

    private void styleNavbar(HBox nav) {
        nav.setStyle(
            "-fx-background-color: " + ThemeManager.bgCard() + ";" +
            "-fx-border-color: " + ThemeManager.border() + ";" +
            "-fx-border-width: 0 0 1 0;"
        );
    }

    private void styleNavBtns() {
        for (int i = 0; i < navBtns.length; i++) {
            boolean active = (i == activeTab);
            navBtns[i].setStyle(
                active
                ? "-fx-background-color: transparent;" +
                  "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
                  "-fx-border-color: transparent transparent " + ThemeManager.textHeading() + " transparent;" +
                  "-fx-border-width: 0 0 2 0;" +
                  "-fx-padding: 6 12;" +
                  "-fx-background-radius: 0; -fx-font-weight: bold;"
                : "-fx-background-color: transparent;" +
                  "-fx-text-fill: " + ThemeManager.textSecondary() + ";" +
                  "-fx-border-color: transparent;" +
                  "-fx-padding: 6 12;" +
                  "-fx-background-radius: 6;"
            );

            final int idx = i;
            navBtns[i].setOnMouseEntered(e -> {
                if (idx != activeTab) {
                    navBtns[idx].setStyle(
                        "-fx-background-color: " + ThemeManager.bgDarker() + ";" +
                        "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 6 12;" +
                        "-fx-background-radius: 6;"
                    );
                }
            });
            navBtns[i].setOnMouseExited(e -> {
                if (idx != activeTab) {
                    navBtns[idx].setStyle(
                        "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + ThemeManager.textSecondary() + ";" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 6 12;" +
                        "-fx-background-radius: 6;"
                    );
                }
            });
        }
    }

    private void styleThemeBtn() {
        themeToggleBtn.setStyle(
            ThemeManager.isDark()
            ? "-fx-background-color: " + ThemeManager.bgDarker() + ";" +
              "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
              "-fx-border-color: " + ThemeManager.border() + ";" +
              "-fx-border-radius: 6; -fx-background-radius: 6;" +
              "-fx-padding: 5 14;"
            : "-fx-background-color: #f0f0f0;" +
              "-fx-text-fill: #212529;" +
              "-fx-border-color: #ced4da;" +
              "-fx-border-radius: 6; -fx-background-radius: 6;" +
              "-fx-padding: 5 14;"
        );
    }

    private void toggleTheme() {
        if (ThemeManager.isDark()) {
            ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
            themeToggleBtn.setText("🌙 Dark");
        } else {
            ThemeManager.setTheme(ThemeManager.Theme.DARK);
            themeToggleBtn.setText("☀️ Light");
        }
        styleThemeBtn();
        applyThemeToAll();
    }

    private void applyThemeToAll() {
        styleNavbar(navbar);
        styleNavBtns();

        root.setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");
        contentArea.setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");

        // Update logo color
        HBox logoBox = (HBox) navbar.getChildren().get(0);
        Label logo = (Label) logoBox.getChildren().get(0);
        logo.setStyle("-fx-text-fill: " + ThemeManager.textHeading() + ";");

        dashboardView.applyTheme();
        subjectsView.applyTheme();
        studySessionView.applyTheme();
        historyView.applyTheme();
    }

    private ScrollPane styledScroll(javafx.scene.Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setFitToHeight(false);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return sp;
    }

    public static void main(String[] args) { launch(args); }
}