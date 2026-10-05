package com.shinka.ui;

import com.shinka.controller.ProgressController.Stage;
import com.shinka.controller.StudySessionController;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Small square badge showing a subject's evolution stage as a kanji
 * (種 → 芽 → 苗 → 木 → 林 → 森). Color deepens as the stage grows.
 */
public final class StageBadge {

    private static String kanjiFont;

    private StageBadge() { }

    public static StackPane create(Stage stage, Stage next, long totalSeconds, double size) {
        Label k = new Label(stage.kanji());
        k.setFont(Font.font(kanjiFont(), FontWeight.BOLD, Math.round(size * 0.48)));

        String bg, fg;
        int i = stage.index();
        if (i == 0)      { bg = ThemeManager.heatLevel(0); fg = ThemeManager.textSecondary(); }
        else if (i <= 2) { bg = ThemeManager.heatLevel(1); fg = ThemeManager.textAccent(); }
        else if (i == 3) { bg = ThemeManager.heatLevel(2); fg = ThemeManager.textPrimary(); }
        else             { bg = ThemeManager.btnPrimaryBg(); fg = ThemeManager.btnPrimaryFg(); }

        k.setStyle("-fx-text-fill: " + fg + ";");

        StackPane badge = new StackPane(k);
        badge.setMinSize(size, size);
        badge.setMaxSize(size, size);
        badge.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: " + Math.round(size * 0.24) + ";"
        );

        String tip = stage.name() + " (" + stage.kanji() + ") · " + fmt(totalSeconds) + " studied";
        if (next != null) tip += "\n" + fmt(next.minSeconds() - totalSeconds) + " to " + next.name() + " (" + next.kanji() + ")";
        else              tip += "\nFinal stage";
        Tooltip t = new Tooltip(tip);
        t.setShowDelay(Duration.millis(150));
        t.setStyle(
            "-fx-font-size: 11;" +
            "-fx-background-color: " + ThemeManager.textPrimary() + ";" +
            "-fx-text-fill: " + ThemeManager.bgMain() + ";" +
            "-fx-background-radius: 6; -fx-padding: 6 10;"
        );
        Tooltip.install(badge, t);
        return badge;
    }

    /** Picks a font that has Japanese glyphs; falls back to the app font. */
    static String kanjiFont() {
        if (kanjiFont == null) {
            kanjiFont = ThemeManager.FONT;
            for (String f : new String[] {"Yu Gothic UI", "Yu Gothic", "Meiryo UI", "Meiryo", "MS Gothic", "Noto Sans CJK JP", "IPAGothic"}) {
                if (Font.getFamilies().contains(f)) { kanjiFont = f; break; }
            }
        }
        return kanjiFont;
    }

    private static String fmt(long seconds) {
        return StudySessionController.formatSeconds(Math.max(0, seconds));
    }
}
