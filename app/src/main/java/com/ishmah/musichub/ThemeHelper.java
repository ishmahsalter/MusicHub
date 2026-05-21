package com.ishmah.musichub;

import android.app.Activity;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    private static final String PREFS = "musichub_prefs";

    public static void apply(Activity activity) {
        String themeName = getThemeName(activity);
        if ("midnight".equals(themeName)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            activity.setTheme(R.style.Theme_MusicHub_Midnight);
        } else if ("goldrush".equals(themeName)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
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
        if (isMidnight(context)) return 0xFF4F6EF7;
        if (isGoldRush(context)) return 0xFF6B21A8;
        return 0xFFA855F7;
    }

    public static int getNavInactiveColor(Context context) {
        return isGoldRush(context) ? 0xFF9090B0 : 0xFFFFFFFF;
    }
}
