package com.example.todolist.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    
    private static final String PREFS_NAME = "TodoListSettings";
    
    // Notification Settings Keys
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_SOUND_ENABLED = "sound_enabled";
    public static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    public static final String KEY_NOTIFICATION_TIME = "notification_time";
    public static final String KEY_RINGTONE_URI = "ringtone_uri";
    public static final String KEY_RINGTONE_NAME = "ringtone_name";
    
    // Appearance Settings Keys
    public static final String KEY_DARK_MODE_ENABLED = "dark_mode_enabled";
    public static final String KEY_THEME_COLOR = "theme_color";
    
    // General Settings Keys
    public static final String KEY_LANGUAGE = "language";
    
    // Data Settings Keys
    public static final String KEY_LAST_BACKUP_TIME = "last_backup_time";
    public static final String KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled";
    
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // Notification Settings
    public static boolean isNotificationsEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }
    
    public static void setNotificationsEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }
    
    public static boolean isSoundEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_SOUND_ENABLED, true);
    }
    
    public static void setSoundEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }
    
    public static boolean isVibrationEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_VIBRATION_ENABLED, true);
    }
    
    public static void setVibrationEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }
    
    public static String getNotificationTime(Context context) {
        return getSharedPreferences(context).getString(KEY_NOTIFICATION_TIME, "30 phút trước");
    }
    
    public static void setNotificationTime(Context context, String time) {
        getSharedPreferences(context).edit().putString(KEY_NOTIFICATION_TIME, time).apply();
    }
    
    public static String getRingtoneUri(Context context) {
        return getSharedPreferences(context).getString(KEY_RINGTONE_URI, null);
    }
    
    public static void setRingtoneUri(Context context, String uri) {
        getSharedPreferences(context).edit().putString(KEY_RINGTONE_URI, uri).apply();
    }
    
    public static String getRingtoneName(Context context) {
        return getSharedPreferences(context).getString(KEY_RINGTONE_NAME, "Mặc định");
    }
    
    public static void setRingtoneName(Context context, String name) {
        getSharedPreferences(context).edit().putString(KEY_RINGTONE_NAME, name).apply();
    }
    
    // Appearance Settings
    public static boolean isDarkModeEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_DARK_MODE_ENABLED, false);
    }
    
    public static void setDarkModeEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(KEY_DARK_MODE_ENABLED, enabled).apply();
    }
    
    public static String getThemeColor(Context context) {
        return getSharedPreferences(context).getString(KEY_THEME_COLOR, "BLUE");
    }
    
    public static void setThemeColor(Context context, String color) {
        getSharedPreferences(context).edit().putString(KEY_THEME_COLOR, color).apply();
    }
    
    // General Settings
    public static String getLanguage(Context context) {
        return getSharedPreferences(context).getString(KEY_LANGUAGE, "Tiếng Việt");
    }
    
    public static void setLanguage(Context context, String language) {
        getSharedPreferences(context).edit().putString(KEY_LANGUAGE, language).apply();
    }
    
    // Data Settings
    public static long getLastBackupTime(Context context) {
        return getSharedPreferences(context).getLong(KEY_LAST_BACKUP_TIME, 0);
    }
    
    public static void setLastBackupTime(Context context, long time) {
        getSharedPreferences(context).edit().putLong(KEY_LAST_BACKUP_TIME, time).apply();
    }
    
    public static boolean isAutoBackupEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, false);
    }
    
    public static void setAutoBackupEnabled(Context context, boolean enabled) {
        getSharedPreferences(context).edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply();
    }
    
    // Helper method to get notification time in minutes
    public static int getNotificationTimeInMinutes(Context context) {
        String timeText = getNotificationTime(context);
        switch (timeText) {
            case "5 phút trước":
                return 5;
            case "10 phút trước":
                return 10;
            case "15 phút trước":
                return 15;
            case "30 phút trước":
                return 30;
            case "1 giờ trước":
                return 60;
            case "2 giờ trước":
                return 120;
            case "1 ngày trước":
                return 1440;
            default:
                return 30;
        }
    }
    
    // Reset all settings to default
    public static void resetAllSettings(Context context) {
        getSharedPreferences(context).edit().clear().apply();
    }
}
