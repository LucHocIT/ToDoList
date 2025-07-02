package com.example.todolist;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.res.Configuration;
import android.os.Build;

import com.example.todolist.notification.NotificationHelper;
import com.example.todolist.util.SettingsManager;

import java.util.Locale;

public class TodoApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Apply saved language globally
        applyLanguageFromSettings();
        
        // Tạo notification channel khi ứng dụng khởi động
        createNotificationChannels();
    }
    
    private void applyLanguageFromSettings() {
        String savedLanguage = SettingsManager.getLanguage(this);
        String languageCode;
        
        if (savedLanguage.equals("English")) {
            languageCode = "en";
        } else {
            languageCode = "vi";
        }
        
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "todo_reminders",
                    "Lời nhắc nhiệm vụ",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo lời nhắc cho các nhiệm vụ sắp tới hạn");
            channel.enableLights(true);
            channel.enableVibration(false);
            channel.setLightColor(android.graphics.Color.BLUE);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
