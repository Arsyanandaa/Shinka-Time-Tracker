package com.shinka.ui;

/**
 * ThemeManager holds all color tokens for Light and Dark (Dracula) themes.
 * All UI components read colors from here so toggling theme is centralized.
 */
public class ThemeManager {

    public enum Theme { LIGHT, DARK }

    private static Theme current = Theme.LIGHT;

    public static void setTheme(Theme theme) { current = theme; }
    public static Theme getTheme()           { return current; }
    public static boolean isDark()           { return current == Theme.DARK; }

    public static String bgMain()   { return isDark() ? "#0d1117" : "#f8f9fa"; }
public static String bgCard()   { return isDark() ? "#161b22" : "#ffffff"; }
public static String bgDarker() { return isDark() ? "#0d1117" : "#f1f3f5"; }
public static String bgInput()  { return isDark() ? "#0d1117" : "#ffffff"; }

// ── Text
public static String textPrimary()  { return isDark() ? "#e6edf3" : "#212529"; }
public static String textSecondary(){ return isDark() ? "#7d8590" : "#6c757d"; }
public static String textHeading()  { return isDark() ? "#3fb950" : "#2d6a4f"; }
public static String textAccent()   { return isDark() ? "#3fb950" : "#2d6a4f"; }

// ── Border
public static String border()      { return isDark() ? "#30363d" : "#dee2e6"; }
public static String borderInput() { return isDark() ? "#30363d" : "#ced4da"; }

// ── Button primary
public static String btnPrimaryBg()    { return isDark() ? "#238636" : "#2d6a4f"; }
public static String btnPrimaryFg()    { return isDark() ? "#ffffff" : "#ffffff"; }
public static String btnPrimaryHover() { return isDark() ? "#2ea043" : "#1b4332"; }

// ── Separator
public static String separator() { return isDark() ? "#30363d" : "#dee2e6"; }

// ── Tab pane
public static String tabBg()       { return isDark() ? "#010409" : "#f8f9fa"; }
public static String tabSelected() { return isDark() ? "#161b22" : "#ffffff"; }
public static String tabText()     { return isDark() ? "#e6edf3" : "#495057"; }

// ── Timer
public static String timerIdle()    { return isDark() ? "#3fb950" : "#2d6a4f"; }
public static String timerRunning() { return "#e63946"; }
public static String timerPaused()  { return "#e67e22"; }

    // ── Full style strings
    public static String cardStyle() {
        return "-fx-background-color: " + bgCard() + ";" +
               "-fx-background-radius: 10;" +
               (isDark()
                ? "-fx-border-color: " + border() + "; -fx-border-radius: 10; -fx-border-width: 1;"
                : "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 2);");
    }

    public static String inputStyle() {
        return "-fx-background-color: " + bgInput() + ";" +
               "-fx-text-fill: " + textPrimary() + ";" +
               "-fx-padding: 8 12;" +
               "-fx-background-radius: 6;" +
               "-fx-border-color: " + borderInput() + ";" +
               "-fx-border-radius: 6;";
    }
}