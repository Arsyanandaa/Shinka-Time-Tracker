package com.shinka.ui;

import java.util.Optional;

import com.shinka.controller.SubjectController;
import com.shinka.model.Subject;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SubjectsView extends VBox {

    private final SubjectController       subjectController;
    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();

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

        Label titleLbl = label(12, FontWeight.SEMI_BOLD);
        titleLbl.setText("Title");
        muted(titleLbl);
        TextField titleField = new TextField(subject.getTitle());
        titleField.setPromptText("e.g. Target band 7.5, Finish chapter 3");
        titleField.setFont(Font.font(ThemeManager.FONT, 13));
        titleField.focusedProperty().addListener((o, was, is) -> styleInput(titleField));
        styleInput(titleField);

        Label notesLbl = label(12, FontWeight.SEMI_BOLD);
        notesLbl.setText("Notes");
        muted(notesLbl);
        TextArea notesArea = new TextArea(subject.getNotes());
        notesArea.setPromptText("Goals, resources, or anything about this subject");
        notesArea.setFont(Font.font(ThemeManager.FONT, 13));
        notesArea.setPrefRowCount(6);
        notesArea.setWrapText(true);
        notesArea.focusedProperty().addListener((o, was, is) -> styleInput(notesArea));
        styleInput(notesArea);

        VBox form = new VBox(6, titleLbl, titleField, gap(10), notesLbl, notesArea);
        form.setPrefWidth(440);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(form);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        styleDialog(pane, "Edit subject", subject.getName());

        styleDialogButton((Button) pane.lookupButton(saveType), "primary");
        styleDialogButton((Button) pane.lookupButton(ButtonType.CANCEL), "ghost");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            subject.setTitle(titleField.getText().trim());
            subject.setNotes(notesArea.getText().trim());
            subjectController.updateSubject(subject.getId(), subject.getTitle(), subject.getNotes());
            renderRows();
        }
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

        for (int i = 0; i < n; i++) {
            if (i > 0) rowsBox.getChildren().add(divider());
            rowsBox.getChildren().add(row(subjects.get(i)));
        }
    }

    private HBox row(Subject subject) {
        // Initial badge
        String name = subject.getName() == null ? "" : subject.getName().trim();
        Label initial = label(14, FontWeight.SEMI_BOLD);
        initial.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: " + ThemeManager.textAccent() + ";");
        StackPane badge = new StackPane(initial);
        badge.setMinSize(36, 36);
        badge.setMaxSize(36, 36);
        badge.setStyle(
            "-fx-background-color: " + ThemeManager.heatLevel(1) + ";" +
            "-fx-background-radius: 8;"
        );

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

        String notes = subject.getNotes() == null ? "" : subject.getNotes().trim();
        if (!notes.isEmpty()) {
            Label notesLbl = label(12, FontWeight.NORMAL);
            notesLbl.setText(notes.lines().findFirst().orElse(""));
            notesLbl.setMaxWidth(520);
            muted(notesLbl);
            info.getChildren().add(notesLbl);
        }
        info.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Actions
        Button editBtn   = ghostButton("Edit", false);
        Button deleteBtn = ghostButton("Delete", true);
        editBtn  .setOnAction(e -> openEditDialog(subject));
        deleteBtn.setOnAction(e -> handleDelete(subject));

        HBox row = new HBox(14, badge, info, editBtn, deleteBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 8, 12, 8));
        String base = "-fx-background-radius: 8;";
        row.setStyle("-fx-background-color: transparent;" + base);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + ThemeManager.bgDarker() + ";" + base));
        row.setOnMouseExited (e -> row.setStyle("-fx-background-color: transparent;" + base));
        return row;
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

    private static String danger()     { return ThemeManager.isDark() ? "#E8836A" : "#C8553D"; }
    private static String dangerSoft() { return ThemeManager.isDark() ? "#3A2620" : "#F8E6E1"; }

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