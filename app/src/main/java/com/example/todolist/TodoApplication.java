package com.example.todolist;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.google.firebase.database.FirebaseDatabase;

public class TodoApplication extends Application {
    
    public static final String NOTIFICATION_CHANNEL_ID = "task_reminders";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase first
        initializeFirebase();
        
        // Create notification channel
        createNotificationChannel();
    }
    
    private void initializeFirebase() {
        try {
            // Enable Firebase offline persistence
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Handle exception if persistence is already enabled
            e.printStackTrace();
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for task reminders");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
