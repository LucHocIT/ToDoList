package com.example.todolist.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;

public class NotificationReceiver extends BroadcastReceiver {
    
    public static final String ACTION_REMINDER = "reminder_notification";
    public static final String ACTION_DUE = "due_notification";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_REMINDER_TYPE = "reminder_type";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        
        if (taskId == -1) return;
        
        // Lấy thông tin task từ database
        new Thread(() -> {
            TodoDatabase database = TodoDatabase.getInstance(context);
            TodoTask task = database.todoDao().getTaskById(taskId);
            
            if (task != null && !task.isCompleted()) {
                NotificationHelper notificationHelper = new NotificationHelper(context);
                
                if (ACTION_REMINDER.equals(action)) {
                    String reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE);
                    String reminderText = getReminderText(reminderType);
                    notificationHelper.showReminderNotification(task, reminderText);
                } else if (ACTION_DUE.equals(action)) {
                    notificationHelper.showDueNotification(task);
                }
            }
        }).start();
    }
    
    private String getReminderText(String reminderType) {
        switch (reminderType) {
            case "5 phút trước":
                return "trong 5 phút";
            case "15 phút trước":
                return "trong 15 phút";
            case "30 phút trước":
                return "trong 30 phút";
            case "1 giờ trước":
                return "trong 1 giờ";
            case "1 ngày trước":
                return "vào ngày mai";
            default:
                return "sắp tới";
        }
    }
}
