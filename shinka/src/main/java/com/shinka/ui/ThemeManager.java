package com.shinka.ui;

/**
 * ThemeManager holds all color tokens for Light and Dark themes.
 * All UI components read colors from here so toggling theme is centralized.
 *
 * Palette: warm neutral surfaces + one muted sage accent.
 * The accent is used sparingly: primary actions, active states, and data.
 */
public class ThemeManager {

    public enum Theme { LIGHT, DARK }

    private static Theme current = Theme.LIGHT;

    public static void setTheme(Theme theme) { current = theme; }
    public static Theme getTheme()           { return current; }
    public static boolean isDark()           { return current == Theme.DARK; }

    // ── Typography
    public static final String FONT = "Segoe UI";

    // ── Surfaces
    public static String bgMain()   { return isDark() ? "#141513" : "#F5F4EF"; }
    public static String bgCard()   { return isDark() ? "#1B1C1A" : "#FFFFFF"; }
    public static String bgDarker() { return isDark() ? "#232421" : "#EDEBE4"; }
    public static String bgInput()  { return isDark() ? "#111210" : "#FFFFFF"; }

    // ── Text
    public static String textPrimary()   { return isDark() ? "#ECEAE3" : "#1E1F1C"; }
    public static String textSecondary() { return isDark() ? "#8C8A82" : "#6F6D66"; }
    public static String textHeading()   { return isDark() ? "#ECEAE3" : "#1E1F1C"; }
    public static String textAccent()    { return isDark() ? "#9CC598" : "#3E6A47"; }

    // ── Border
    public static String border()      { return isDark() ? "#2A2B28" : "#E3E0D7"; }
    public static String borderInput() { return isDark() ? "#34352F" : "#D6D3C9"; }

    // ── Button primary
    public static String btnPrimaryBg()    { return isDark() ? "#9CC598" : "#3E6A47"; }
    public static String btnPrimaryFg()    { return isDark() ? "#121510" : "#FFFFFF"; }
    public static String btnPrimaryHover() { return isDark() ? "#AFD2AB" : "#33583B"; }

    // ── Separator
    public static String separator() { return isDark() ? "#262724" : "#E8E6DE"; }

    // ── Tab pane
    public static String tabBg()       { return isDark() ? "#111210" : "#F5F4EF"; }
    public static String tabSelected() { return isDark() ? "#1B1C1A" : "#FFFFFF"; }
    public static String tabText()     { return isDark() ? "#ECEAE3" : "#3A3A36"; }

    // ── Timer
    public static String timerIdle()    { return textAccent(); }
    public static String timerRunning() { return isDark() ? "#E8836A" : "#C8553D"; }
    public static String timerPaused()  { return isDark() ? "#E2B062" : "#B7832F"; }

    // ── Status
    public static String danger()      { return isDark() ? "#E8836A" : "#C8553D"; }
    public static String dangerSoft()  { return isDark() ? "#3A2620" : "#F8E6E1"; }
    public static String warning()     { return isDark() ? "#E2B062" : "#9A6B1F"; }

    // ── Heatmap (level 0 = no study, 4 = most)
    public static String heatLevel(int level) {
        String[] dark  = { "#232421", "#2F422E", "#4A6D47", "#709C6C", "#A3CD9F" };
        String[] light = { "#ECEAE3", "#D3E3CF", "#A2C59E", "#669968", "#3E6A47" };
        int i = Math.max(0, Math.min(4, level));
        return isDark() ? dark[i] : light[i];
    }

    // ── Full style strings
    public static String cardStyle() {
        return "-fx-background-color: " + bgCard() + ";" +
               "-fx-background-radius: 12;" +
               "-fx-border-color: " + border() + ";" +
               "-fx-border-radius: 12;" +
               "-fx-border-width: 1;" +
               (isDark()
                ? ""
                : "-fx-effect: dropshadow(gaussian, rgba(40,35,20,0.05), 10, 0, 0, 2);");
    }

    public static String inputStyle() {
        return "-fx-background-color: " + bgInput() + ";" +
               "-fx-text-fill: " + textPrimary() + ";" +
               "-fx-prompt-text-fill: " + textSecondary() + ";" +
               "-fx-padding: 8 12;" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + borderInput() + ";" +
               "-fx-border-radius: 8;";
    }
}
