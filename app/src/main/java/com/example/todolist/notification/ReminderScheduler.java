package com.example.todolist.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.todolist.model.TodoTask;
import com.example.todolist.util.SettingsManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Class quản lý việc lên lịch thông báo cho các nhiệm vụ
 */
public class ReminderScheduler {
    
    private static final String TAG = "ReminderScheduler";
    private Context context;
    private AlarmManager alarmManager;

    public ReminderScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Lên lịch thông báo cho một nhiệm vụ
     */
    public void scheduleTaskReminder(TodoTask task) {
        if (!task.isHasReminder() || task.isCompleted()) {
            return;
        }

        String dueDate = task.getDueDate();
        String dueTime = task.getDueTime();
        String reminderType = task.getReminderType();

        if (dueDate == null || dueTime == null || reminderType == null ||
            dueDate.equals("Không") || dueTime.equals("Không") || reminderType.equals("Không")) {
            return;
        }

        try {
            // Parse ngày và giờ
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            String dateTimeString = dueDate + " " + dueTime;
            Date dueDateTime = dateTimeFormat.parse(dateTimeString);
            
            if (dueDateTime == null) return;

            Calendar dueCal = Calendar.getInstance();
            dueCal.setTime(dueDateTime);

            // Tính toán thời gian thông báo reminder
            Calendar reminderCal = (Calendar) dueCal.clone();
            
            // Sử dụng thời gian nhắc nhở từ settings nếu không có reminder type cụ thể
            int reminderMinutes;
            if (reminderType.equals("Theo cài đặt") || reminderType.isEmpty()) {
                reminderMinutes = SettingsManager.getNotificationTimeInMinutes(context);
            } else {
                reminderMinutes = getReminderMinutes(reminderType);
            }
            
            reminderCal.add(Calendar.MINUTE, -reminderMinutes);

            // Chỉ lên lịch nếu thời gian thông báo chưa qua
            Calendar now = Calendar.getInstance();
            
            if (reminderCal.getTimeInMillis() > now.getTimeInMillis()) {
                scheduleReminderNotification(task, reminderCal.getTimeInMillis(), reminderType);
            }

            // Lên lịch thông báo khi đến hạn
            if (dueCal.getTimeInMillis() > now.getTimeInMillis()) {
                scheduleDueNotification(task, dueCal.getTimeInMillis());
            }

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date/time for task: " + task.getTitle(), e);
        }
    }

    /**
     * Lên lịc thông báo reminder
     */
    private void scheduleReminderNotification(TodoTask task, long triggerTime, String reminderType) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_REMINDER);
        intent.putExtra(NotificationReceiver.EXTRA_TASK_ID, task.getId());
        intent.putExtra(NotificationReceiver.EXTRA_REMINDER_TYPE, reminderType);

        int requestCode = task.getId() * 10; // Unique ID for reminder
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Lên lịch alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.d(TAG, "Scheduled reminder for task: " + task.getTitle() + " at " + new Date(triggerTime));
    }

    /**
     * Lên lịch thông báo khi đến hạn
     */
    private void scheduleDueNotification(TodoTask task, long triggerTime) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_DUE);
        intent.putExtra(NotificationReceiver.EXTRA_TASK_ID, task.getId());

        int requestCode = task.getId() * 10 + 1; // Unique ID for due notification
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Lên lịch alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.d(TAG, "Scheduled due notification for task: " + task.getTitle() + " at " + new Date(triggerTime));
    }

    /**
     * Hủy tất cả thông báo của một task
     */
    public void cancelTaskReminders(int taskId) {
        // Hủy reminder alarm
        cancelAlarm(taskId * 10);
        // Hủy due alarm  
        cancelAlarm(taskId * 10 + 1);
        
        Log.d(TAG, "Cancelled all reminders for task ID: " + taskId);
    }

    /**
     * Hủy một alarm theo request code
     */
    private void cancelAlarm(int requestCode) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    /**
     * Chuyển đổi reminder type thành số phút
     */
    private int getReminderMinutes(String reminderType) {
        switch (reminderType) {
            case "5 phút trước":
                return 5;
            case "15 phút trước":
                return 15;
            case "30 phút trước":
                return 30;
            case "1 giờ trước":
                return 60;
            case "1 ngày trước":
                return 24 * 60; // 1440 phút
            default:
                return 5; // Mặc định 5 phút
        }
    }

    /**
     * Lên lịch lại tất cả thông báo cho các task chưa hoàn thành và có reminder
     */
    public void rescheduleAllReminders() {
        // Được gọi khi khởi động lại ứng dụng để restore các thông báo
        // Implementation sẽ được thêm vào TaskManager hoặc Application class
    }
}
