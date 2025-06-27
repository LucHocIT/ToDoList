package com.example.todolist;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.example.todolist.notification.NotificationHelper;

public class TodoApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Tạo notification channel khi ứng dụng khởi động
        createNotificationChannels();
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
            channel.enableVibration(true);
            channel.setLightColor(android.graphics.Color.BLUE);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
