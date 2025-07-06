package com.example.todolist.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;

import java.util.List;

/**
 * Receiver để lên lịch lại thông báo khi thiết bị khởi động lại
 */
public class BootReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            
            // Lên lịch lại tất cả thông báo
            rescheduleAllReminders(context);
        }
    }
    
    private void rescheduleAllReminders(Context context) {
        new Thread(() -> {
            TodoDatabase database = TodoDatabase.getInstance(context);
            List<TodoTask> allTasks = database.todoDao().getAllTasks();
            ReminderScheduler scheduler = new ReminderScheduler(context);
            
            for (TodoTask task : allTasks) {
                if (!task.isCompleted() && task.isHasReminder()) {
                    scheduler.scheduleTaskReminder(task);
                }
            }
        }).start();
    }
}
