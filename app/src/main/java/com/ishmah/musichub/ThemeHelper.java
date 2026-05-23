package com.ishmah.musichub;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    private static final String PREFS = "musichub_prefs";

    public static void apply(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String themeName = prefs.getString("theme_name", "aurora");

        switch (themeName) {
            case "midnight":
                // MODE_NIGHT_NO so values-night/ is NOT used.
                // Then apply standalone Midnight style with hardcoded blue colors.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                activity.setTheme(R.style.Theme_MusicHub_Midnight);
                break;
            case "goldrush":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                // No setTheme needed — uses default light Theme.MusicHub
                break;
            case "aurora":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                // No setTheme needed — uses values-night/ Aurora colors
                break;
        }
    }

    public static String getThemeName(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("theme_name", "aurora");
    }

    public static boolean isMidnight(Context context) {
        return "midnight".equals(getThemeName(context));
    }

    public static boolean isGoldRush(Context context) {
        return "goldrush".equals(getThemeName(context));
    }

    public static int getAccentColor(Context context) {
        if (isMidnight(context)) return 0xFF4F6EF7;  // neon blue
        if (isGoldRush(context)) return 0xFF6B21A8;  // light-mode purple
        return 0xFF7C3AED;                            // Aurora deep purple
    }

    public static int getNavInactiveColor(Context context) {
        return isGoldRush(context) ? 0xFF9090B0 : 0xFFFFFFFF;
    }
}
