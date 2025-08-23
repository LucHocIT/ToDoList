package com.example.todolist.notification;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.example.todolist.model.Task;
import com.example.todolist.util.SettingsManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderScheduler {
    private static final String TAG = "ReminderScheduler";
    private Context context;
    private AlarmManager alarmManager;
    public ReminderScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void scheduleTaskReminder(Task task) {
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
            // Parse ngĂ y vĂ  giá»
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            String dateTimeString = dueDate + " " + dueTime;
            Date dueDateTime = dateTimeFormat.parse(dateTimeString);
            if (dueDateTime == null) return;
            Calendar dueCal = Calendar.getInstance();
            dueCal.setTime(dueDateTime);
            Calendar reminderCal = (Calendar) dueCal.clone();
            int reminderMinutes = getReminderMinutes(reminderType);
            reminderCal.add(Calendar.MINUTE, -reminderMinutes);
            Calendar now = Calendar.getInstance();
            if (reminderCal.getTimeInMillis() > now.getTimeInMillis()) {
                scheduleReminderNotification(task, reminderCal.getTimeInMillis(), reminderType);
            }
            if (dueCal.getTimeInMillis() > now.getTimeInMillis()) {
                scheduleDueNotification(task, dueCal.getTimeInMillis());
            }
        } catch (ParseException e) {
        }
    }

    private void scheduleReminderNotification(Task task, long triggerTime, String reminderType) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_REMINDER);
        intent.putExtra(NotificationReceiver.EXTRA_TASK_ID, task.getId());
        intent.putExtra(NotificationReceiver.EXTRA_REMINDER_TYPE, reminderType);
        int requestCode = task.getId().hashCode(); // Unique ID for reminder
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    private void scheduleDueNotification(Task task, long triggerTime) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_DUE);
        intent.putExtra(NotificationReceiver.EXTRA_TASK_ID, task.getId());
        int requestCode = task.getId().hashCode() + 1; // Unique ID for due notification
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // LĂªn lá»‹ch alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    public void cancelTaskReminders(int taskId) {
        // Há»§y reminder alarm
        cancelAlarm(taskId * 10);
        // Há»§y due alarm  
        cancelAlarm(taskId * 10 + 1);
    }

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
            default:
                return 5; 
        }
    }

    public void scheduleReminder(int taskIntId, String title, String dueDate, String dueTime) {
        // Convert parameters to Task object and schedule
        Task tempTask = new Task();
        tempTask.setId(String.valueOf(taskIntId));
        tempTask.setTitle(title);
        tempTask.setDueDate(dueDate);
        tempTask.setDueTime(dueTime);
        tempTask.setReminder("5 phút trước"); // Default reminder
        scheduleTaskReminder(tempTask);
    }

    public void cancelReminder(int taskIntId) {
        String taskId = String.valueOf(taskIntId);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_REMINDER);
        intent.putExtra(NotificationReceiver.EXTRA_TASK_ID, taskId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskIntId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
    public void rescheduleAllReminders() {
    }
}
