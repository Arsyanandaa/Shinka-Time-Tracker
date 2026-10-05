package com.shinka.ui;

import java.util.Optional;

import com.shinka.controller.StudySessionController;
import com.shinka.model.StudySession;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class HistoryView extends VBox {

    private final StudySessionController sessionController;
    private final ObservableList<StudySession> sessions = FXCollections.observableArrayList();
    private TableView<StudySession> table;
    private Label heading;
    private Label hint;

    private TableColumn<StudySession, String> dateCol;
    private TableColumn<StudySession, String> subjectCol;
    private TableColumn<StudySession, String> titleCol;
    private TableColumn<StudySession, String> durationCol;
    private TableColumn<StudySession, String> notesCol;

    public HistoryView(StudySessionController sessionController) {
        this.sessionController = sessionController;
        buildUI();
    }

    @SuppressWarnings("unchecked")
    private void buildUI() {
        setSpacing(16);
        setPadding(new Insets(30));

        heading = new Label("📖 Study History");
        heading.setFont(Font.font("System", FontWeight.BOLD, 20));

        hint = new Label("All your recorded study sessions. Click a row to edit or delete.");

        table = new TableView<>(sessions);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        dateCol = col("Date", 90);
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate()));

        subjectCol = col("Subject", 110);
        subjectCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubjectName()));

        titleCol = col("Title", 160);
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));

        durationCol = col("Duration", 80);
        durationCol.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getFormattedDuration()));

        notesCol = col("Notes", 200);
        notesCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNotes()));

        TableColumn<StudySession, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setMinWidth(140);
        actionsCol.setMaxWidth(160);
        actionsCol.setCellFactory(tc -> new ActionCell());

        table.getColumns().addAll(dateCol, subjectCol, titleCol, durationCol, notesCol, actionsCol);

        Label emptyLabel = new Label("No study sessions recorded yet.");
        table.setPlaceholder(emptyLabel);

        getChildren().addAll(heading, hint, new Separator(), table);
        applyTheme();
    }

    public void refresh() {
        sessions.setAll(sessionController.getAllSessions());
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    public void applyTheme() {
        setStyle("-fx-background-color: " + ThemeManager.bgMain() + ";");
        heading.setStyle("-fx-text-fill: " + ThemeManager.textHeading() + ";");
        hint.setStyle("-fx-text-fill: " + ThemeManager.textSecondary() + ";");

        table.setStyle(
            "-fx-background-color: " + ThemeManager.bgCard() + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + ThemeManager.border() + ";" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;" +
            "-fx-table-cell-border-color: " + ThemeManager.border() + ";"
        );

        // ── Cell factories with correct text color
        dateCol    .setCellFactory(tc -> textCell());
        subjectCol .setCellFactory(tc -> textCell());
        titleCol   .setCellFactory(tc -> textCell());
        durationCol.setCellFactory(tc -> textCell());
        notesCol   .setCellFactory(tc -> textCell());

        // ── Row factory for alternating rows + hover
        table.setRowFactory(tv -> {
            TableRow<StudySession> row = new TableRow<>() {
                @Override
                protected void updateItem(StudySession item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: " + ThemeManager.bgCard() + ";");
                    } else {
                        boolean odd = getIndex() % 2 == 1;
                        String bg = odd
                            ? (ThemeManager.isDark() ? "#1c2128" : "#f6f8fa")
                            : ThemeManager.bgCard();
                        setStyle(
                            "-fx-background-color: " + bg + ";" +
                            "-fx-border-color: transparent transparent " +
                            ThemeManager.border() + " transparent;" +
                            "-fx-border-width: 0 0 1 0;"
                        );
                    }
                }
            };

            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) row.setStyle(
                    "-fx-background-color: " + (ThemeManager.isDark() ? "#2d333b" : "#e8f4fd") + ";" +
                    "-fx-border-color: transparent transparent " + ThemeManager.border() + " transparent;" +
                    "-fx-border-width: 0 0 1 0;"
                );
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) {
                    boolean odd = row.getIndex() % 2 == 1;
                    String bg = odd
                        ? (ThemeManager.isDark() ? "#1c2128" : "#f6f8fa")
                        : ThemeManager.bgCard();
                    row.setStyle(
                        "-fx-background-color: " + bg + ";" +
                        "-fx-border-color: transparent transparent " +
                        ThemeManager.border() + " transparent;" +
                        "-fx-border-width: 0 0 1 0;"
                    );
                }
            });
            return row;
        });

        // ── Header styling
        table.skinProperty().addListener((obs, o, n) -> styleHeaders());
        styleHeaders();

        table.refresh();
    }

    private TableCell<StudySession, String> textCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle(
                        "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 8 10;"
                    );
                }
            }
        };
    }

    private void styleHeaders() {
        String headerBg = ThemeManager.isDark() ? "#21262d" : "#f0f3f6";
        table.lookupAll(".column-header").forEach(node ->
            node.setStyle("-fx-background-color: " + headerBg + ";")
        );
        table.lookupAll(".column-header-background").forEach(node ->
            node.setStyle(
                "-fx-background-color: " + headerBg + ";" +
                "-fx-background-radius: 12 12 0 0;"
            )
        );
        table.lookupAll(".column-header .label").forEach(node ->
            node.setStyle(
                "-fx-text-fill: " + ThemeManager.textPrimary() + ";" +
                "-fx-font-weight: bold;"
            )
        );
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────

    private void openEditDialog(StudySession session) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Session");
        dialog.setHeaderText("Edit: " + session.getDate() + " — " + session.getSubjectName());

        Label titleLbl = new Label("Title:");
        titleLbl.setStyle("-fx-font-weight: bold;");
        TextField titleField = new TextField(session.getTitle());

        Label notesLbl = new Label("Notes:");
        notesLbl.setStyle("-fx-font-weight: bold;");
        TextArea notesArea = new TextArea(session.getNotes());
        notesArea.setPrefRowCount(6);
        notesArea.setWrapText(true);

        VBox form = new VBox(8, titleLbl, titleField, notesLbl, notesArea);
        form.setPadding(new Insets(10));
        form.setPrefWidth(420);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sessionController.updateSession(session.getId(), titleField.getText(), notesArea.getText());
            refresh();
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void handleDelete(StudySession session) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Session");
        confirm.setHeaderText("Delete this study session?");
        confirm.setContentText(
            session.getDate() + " · " + session.getSubjectName() +
            "\nThis action cannot be undone."
        );
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            sessionController.deleteSession(session.getId());
            sessions.remove(session);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> TableColumn<StudySession, T> col(String title, double minWidth) {
        TableColumn<StudySession, T> c = new TableColumn<>(title);
        c.setMinWidth(minWidth);
        return c;
    }

    // ── Action Cell ───────────────────────────────────────────────────────────

    private class ActionCell extends TableCell<StudySession, Void> {

        private final Button editBtn   = new Button("✏ Edit");
        private final Button deleteBtn = new Button("🗑 Delete");
        private final HBox   box       = new HBox(6, editBtn, deleteBtn);

        ActionCell() {
            styleButtons();
            box.setAlignment(Pos.CENTER_LEFT);
        }

        private void styleButtons() {
            editBtn.setStyle(
                ThemeManager.isDark()
                ? "-fx-background-color: #1f3d2a; -fx-text-fill: #3fb950;" +
                  "-fx-border-color: #238636; -fx-border-radius: 6;" +
                  "-fx-background-radius: 6; -fx-padding: 3 8;"
                : "-fx-background-color: #e8f5e9; -fx-text-fill: #2d6a4f;" +
                  "-fx-border-color: #a5d6a7; -fx-border-radius: 4;" +
                  "-fx-background-radius: 4; -fx-padding: 3 8;"
            );
            deleteBtn.setStyle(
                ThemeManager.isDark()
                ? "-fx-background-color: #3d1f1f; -fx-text-fill: #f85149;" +
                  "-fx-border-color: #8b1a1a; -fx-border-radius: 6;" +
                  "-fx-background-radius: 6; -fx-padding: 3 8;"
                : "-fx-background-color: #fff0f0; -fx-text-fill: #dc3545;" +
                  "-fx-border-color: #f5c6cb; -fx-border-radius: 4;" +
                  "-fx-background-radius: 4; -fx-padding: 3 8;"
            );
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                styleButtons();
                StudySession session = getTableView().getItems().get(getIndex());
                editBtn  .setOnAction(e -> openEditDialog(session));
                deleteBtn.setOnAction(e -> handleDelete(session));
                setStyle("-fx-background-color: transparent;");
                setGraphic(box);
            }
        }
    }
}