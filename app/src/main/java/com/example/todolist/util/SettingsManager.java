package com.example.todolist.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    
    private static final String PREFS_NAME = "TodoListSettings";
    
    // Notification Settings Keys
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_SOUND_ENABLED = "sound_enabled";
    public static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    public static final String KEY_RINGTONE_URI = "ringtone_uri";
    public static final String KEY_RINGTONE_NAME = "ringtone_name";
    
    // General Settings Keys
    public static final String KEY_LANGUAGE = "language";
    
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
    
    // General Settings
    public static String getLanguage(Context context) {
        return getSharedPreferences(context).getString(KEY_LANGUAGE, "Tiếng Việt");
    }
    
    public static void setLanguage(Context context, String language) {
        getSharedPreferences(context).edit().putString(KEY_LANGUAGE, language).apply();
    }
    
    // Reset all settings to default
    public static void resetAllSettings(Context context) {
        getSharedPreferences(context).edit().clear().apply();
    }
    
    /**
     * Kiểm tra logic cài đặt thông báo có đúng không
     */
    public static boolean isNotificationLogicValid(Context context) {
        boolean notificationsEnabled = isNotificationsEnabled(context);
        boolean soundEnabled = isSoundEnabled(context);
        boolean vibrationEnabled = isVibrationEnabled(context);
        
        // Nếu notifications tắt thì sound và vibration cũng phải tắt
        if (!notificationsEnabled && (soundEnabled || vibrationEnabled)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Sửa logic cài đặt nếu có vấn đề
     */
    public static void fixNotificationSettings(Context context) {
        if (!isNotificationLogicValid(context)) {
            boolean notificationsEnabled = isNotificationsEnabled(context);
            if (!notificationsEnabled) {
                // Tắt sound và vibration nếu notifications bị tắt
                setSoundEnabled(context, false);
                setVibrationEnabled(context, false);
            }
        }
    }
}
