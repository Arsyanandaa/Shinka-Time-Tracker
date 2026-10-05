package com.shinka.ui;

import java.util.Optional;

import com.shinka.controller.SubjectController;
import com.shinka.model.Subject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SubjectsView extends VBox {

    private final SubjectController       subjectController;
    private final ObservableList<Subject> subjects = FXCollections.observableArrayList();
    private ListView<Subject> listView;
    private TextField         nameField;
    private Label             statusLabel;
    private Label             heading;
    private Label             hint;
    private Button            addBtn;

    public SubjectsView(SubjectController subjectController) {
        this.subjectController = subjectController;
        buildUI();
        loadSubjects();
    }

    private void buildUI() {
        setSpacing(18);
        setPadding(new Insets(30));

        heading = new Label("📚 Subjects");
        heading.setFont(Font.font("System", FontWeight.BOLD, 20));

        hint = new Label("Create subjects and add title & notes for each one.");

        nameField = new TextField();
        nameField.setPromptText("e.g. IELTS, Coding, Machine Learning…");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameField.setOnAction(e -> handleAdd());

        addBtn = new Button("＋ Add Subject");
        addBtn.setFont(Font.font("System", FontWeight.BOLD, 13));
        addBtn.setOnAction(e -> handleAdd());

        HBox addRow = new HBox(10, nameField, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label();

        listView = new ListView<>(subjects);
        listView.setPrefHeight(500);
        listView.setCellFactory(lv -> new SubjectCell());
        VBox.setVgrow(listView, Priority.ALWAYS);

        Label emptyLabel = new Label("No subjects yet. Add one above! 👆");
        listView.setPlaceholder(emptyLabel);

        getChildren().addAll(heading, hint, new Separator(), addRow, statusLabel, listView);
        applyTheme();
    }

    public void loadSubjects() {
        subjects.setAll(subjectController.getAllSubjects());
    }

    private void handleAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Please enter a subject name.");
            return;
        }
        Subject added = subjectController.addSubject(name);
        if (added == null) {
            statusLabel.setText("Could not save subject.");
            return;
        }
        subjects.add(added);
        nameField.clear();
        statusLabel.setText("");
    }

    private void handleDelete(Subject subject) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Subject");
        confirm.setHeaderText("Delete \"" + subject.getName() + "\"?");
        confirm.setContentText("All study sessions linked to this subject will also be deleted.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            subjectController.deleteSubject(subject.getId());
            subjects.remove(subject);
        }
    }

    private void openEditDialog(Subject subject) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Subject");
        dialog.setHeaderText("✏  " + subject.getName());

        Label titleLbl = new Label("Title:");
        titleLbl.setStyle("-fx-font-weight: bold;");
        TextField titleField = new TextField(subject.getTitle());
        titleField.setPromptText("e.g. Target band 7.5, Learn Spring Boot...");

        Label notesLbl = new Label("Notes:");
        notesLbl.setStyle("-fx-font-weight: bold;");
        TextArea notesArea = new TextArea(subject.getNotes());
        notesArea.setPromptText("Write your goals, resources, or anything about this subject...");
        notesArea.setPrefRowCount(5);
        notesArea.setWrapText(true);

        VBox form = new VBox(8, titleLbl, titleField, notesLbl, notesArea);
        form.setPadding(new Insets(10));
        form.setPrefWidth(400);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            subject.setTitle(titleField.getText().trim());
            subject.setNotes(notesArea.getText().trim());
            subjectController.updateSubject(subject.getId(), subject.getTitle(), subject.getNotes());
            listView.refresh();
        }
    }

    public ObservableList<Subject> getSubjects() { return subjects; }

    public void applyTheme() {
        setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");

        heading.setStyle("-fx-text-fill: " + ThemeManager.textHeading() + ";");
        hint.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + ";");
        statusLabel.setStyle("-fx-text-fill: #dc3545;");

        nameField.setStyle(ThemeManager.inputStyle());

        addBtn.setStyle(
            "-fx-background-color: " + ThemeManager.btnPrimaryBg() + ";" +
            "-fx-text-fill: " + ThemeManager.btnPrimaryFg() + ";" +
            "-fx-padding: 8 18;" +
            "-fx-background-radius: 6;" +
            "-fx-font-weight: bold;"
        );

        listView.setStyle(
            "-fx-background-color: " + ThemeManager.bgCard() + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + ThemeManager.border() + ";" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;"
        );

        // Row factory for dark mode cells
        listView.setCellFactory(lv -> new SubjectCell());
        listView.refresh();
    }

    // ── Custom list cell ──────────────────────────────────────────────────────

    private class SubjectCell extends ListCell<Subject> {

        private final HBox   content;
        private final VBox   infoBox;
        private final Label  nameLbl;
        private final Label  titleLbl;
        private final Button editBtn;
        private final Button deleteBtn;

        SubjectCell() {
            nameLbl = new Label();
            nameLbl.setFont(Font.font("System", FontWeight.BOLD, 14));

            titleLbl = new Label();
            titleLbl.setStyle("-fx-font-size: 12;");

            infoBox = new VBox(2, nameLbl, titleLbl);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            editBtn   = new Button("✏  Edit Notes");
            deleteBtn = new Button("🗑  Delete");

            content = new HBox(10, infoBox, editBtn, deleteBtn);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(10, 12, 10, 12));
        }

        @Override
        protected void updateItem(Subject subject, boolean empty) {
            super.updateItem(subject, empty);
            if (empty || subject == null) {
                setGraphic(null);
                setStyle("-fx-background-color: " + ThemeManager.bgCard() + ";");
            } else {
                // Cell background
                setStyle(
                    "-fx-background-color: " + ThemeManager.bgCard() + ";" +
                    "-fx-border-color: transparent transparent " + ThemeManager.border() + " transparent;" +
                    "-fx-border-width: 0 0 1 0;"
                );

                nameLbl.setText(subject.getName());
                nameLbl.setStyle(
                    "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
                    "-fx-font-weight: bold; -fx-font-size: 14;"
                );

                titleLbl.setText(
                    subject.getTitle() == null || subject.getTitle().isEmpty()
                        ? "No title yet"
                        : subject.getTitle()
                );
                titleLbl.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + "; -fx-font-size: 12;");

                editBtn.setStyle(
                    ThemeManager.isDark()
                    ? "-fx-background-color: #1f3d2a; -fx-text-fill: #3fb950;" +
                      "-fx-border-color: #238636; -fx-border-radius: 6;" +
                      "-fx-background-radius: 6; -fx-padding: 4 10;"
                    : "-fx-background-color: #e8f5e9; -fx-text-fill: #2d6a4f;" +
                      "-fx-border-color: #a5d6a7; -fx-border-radius: 6;" +
                      "-fx-background-radius: 6; -fx-padding: 4 10;"
                );

                deleteBtn.setStyle(
                    ThemeManager.isDark()
                    ? "-fx-background-color: #3d1f1f; -fx-text-fill: #f85149;" +
                      "-fx-border-color: #8b1a1a; -fx-border-radius: 6;" +
                      "-fx-background-radius: 6; -fx-padding: 4 10;"
                    : "-fx-background-color: #fff0f0; -fx-text-fill: #dc3545;" +
                      "-fx-border-color: #f5c6cb; -fx-border-radius: 6;" +
                      "-fx-background-radius: 6; -fx-padding: 4 10;"
                );

                editBtn  .setOnAction(e -> openEditDialog(subject));
                deleteBtn.setOnAction(e -> handleDelete(subject));
                setGraphic(content);
            }
        }
    }
}