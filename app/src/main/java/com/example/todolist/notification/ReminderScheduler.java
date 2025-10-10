package com.example.todolist.notification;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.example.todolist.model.SharedUser;
import com.example.todolist.model.Task;
import com.example.todolist.model.TaskShare;
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
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dateTimeString = dueDate + " " + dueTime;
            Date dueDateTime = dateTimeFormat.parse(dateTimeString);
            if (dueDateTime == null) return;
            Calendar dueCal = Calendar.getInstance();
            dueCal.setTime(dueDateTime);
            Calendar reminderCal = Calendar.getInstance();
            String reminderDateTimeString = dueDate + " " + reminderType;
            Date reminderDateTime = dateTimeFormat.parse(reminderDateTimeString);
            if (reminderDateTime != null) {
                reminderCal.setTime(reminderDateTime);
                Calendar now = Calendar.getInstance();
                if (reminderCal.getTimeInMillis() > now.getTimeInMillis()) {
                    scheduleReminderNotification(task, reminderCal.getTimeInMillis(), reminderType);
                }
            }
            
            Calendar now = Calendar.getInstance();
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
        int requestCode = task.getId().hashCode() + 1; 
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

    public void cancelTaskReminders(int taskId) {
        // Sử dụng cùng logic với schedule để đảm bảo tính nhất quán
        String taskIdStr = String.valueOf(taskId);
        int reminderRequestCode = taskIdStr.hashCode();
        int dueRequestCode = taskIdStr.hashCode() + 1;
        
        cancelAlarm(reminderRequestCode);
        cancelAlarm(dueRequestCode);
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

    public void scheduleReminder(int taskIntId, String title, String dueDate, String dueTime) {
        Task tempTask = new Task();
        tempTask.setId(String.valueOf(taskIntId));
        tempTask.setTitle(title);
        tempTask.setDueDate(dueDate);
        tempTask.setDueTime(dueTime);
        // Đặt thời gian nhắc nhở cụ thể (ví dụ: "22:10") thay vì "5 phút trước"
        tempTask.setReminderType(dueTime); // hoặc thời gian cụ thể khác
        tempTask.setHasReminder(true);
        scheduleTaskReminder(tempTask);
    }

    public void cancelReminder(int taskIntId) {
        cancelTaskReminders(taskIntId);
    }
    
    public void rescheduleAllReminders() {
        // Implementation for reschedule all reminders if needed
    }
    
    /**
     * Schedule notification cho shared task - bao gồm owner và tất cả shared users
     */
    public void scheduleSharedTaskReminder(Task task, TaskShare taskShare) {
        if (task == null || taskShare == null) {
            Log.w(TAG, "Task or TaskShare is null, cannot schedule shared task reminder");
            return;
        }
        
        // Schedule cho owner (như bình thường)
        scheduleTaskReminder(task);
        Log.d(TAG, "Scheduled reminder for owner: " + taskShare.getOwnerEmail());
        
        // Schedule cho từng shared user
        if (taskShare.getSharedUsers() != null && !taskShare.getSharedUsers().isEmpty()) {
            for (SharedUser user : taskShare.getSharedUsers()) {
                scheduleReminderForUser(task, user);
                scheduleDueForUser(task, user);
                Log.d(TAG, "Scheduled reminder for shared user: " + user.getEmail());
            }
        }
    }
    
    /**
     * Schedule reminder notification cho một user cụ thể
     */
    public void scheduleReminderForUser(Task task, SharedUser user) {
        if (!task.isHasReminder() || task.isCompleted()) {
            return;
        }
        
        String dueDate = task.getDueDate();
        String reminderType = task.getReminderType();
        
        if (dueDate == null || reminderType == null ||
            dueDate.equals("Không") || reminderType.equals("Không")) {
            return;
        }
        
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String reminderDateTimeString = dueDate + " " + reminderType;
            Date reminderDateTime = dateTimeFormat.parse(reminderDateTimeString);
            
            if (reminderDateTime != null) {
                Calendar reminderCal = Calendar.getInstance();
                reminderCal.setTime(reminderDateTime);
                Calendar now = Calendar.getInstance();
                
                if (reminderCal.getTimeInMillis() > now.getTimeInMillis()) {
                    // Tạo unique requestCode cho mỗi user
                    String uniqueKey = task.getId() + "_reminder_" + user.getEmail();
                    int requestCode = uniqueKey.hashCode();
                    
                    Intent intent = new Intent(context, NotificationReceiver.class);
                    intent.setAction(NotificationReceiver.ACTION_REMINDER);
                    intent.putExtra(NotificationReceiver.EXTRA_TASK_ID, task.getId());
                    intent.putExtra(NotificationReceiver.EXTRA_REMINDER_TYPE, reminderType);
                    intent.putExtra("user_email", user.getEmail());
                    intent.putExtra("user_name", user.getName());
                    
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 
                            reminderCal.getTimeInMillis(), pendingIntent);
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, 
                            reminderCal.getTimeInMillis(), pendingIntent);
                    }
                    
                    Log.d(TAG, "Scheduled reminder for user " + user.getEmail() + 
                        " at " + reminderDateTimeString);
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error scheduling reminder for user: " + user.getEmail(), e);
        }
    }
    
    /**
     * Schedule due notification cho một user cụ thể
     */
    public void scheduleDueForUser(Task task, SharedUser user) {
        if (!task.isHasReminder() || task.isCompleted()) {
            return;
        }
        
        String dueDate = task.getDueDate();
        String dueTime = task.getDueTime();
        
        if (dueDate == null || dueTime == null ||
            dueDate.equals("Không") || dueTime.equals("Không")) {
            return;
        }
        
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dueDateTimeString = dueDate + " " + dueTime;
            Date dueDateTime = dateTimeFormat.parse(dueDateTimeString);
            
            if (dueDateTime != null) {
                Calendar dueCal = Calendar.getInstance();
                dueCal.setTime(dueDateTime);
                Calendar now = Calendar.getInstance();
                
                if (dueCal.getTimeInMillis() > now.getTimeInMillis()) {
                    // Tạo unique requestCode cho mỗi user
                    String uniqueKey = task.getId() + "_due_" + user.getEmail();
                    int requestCode = uniqueKey.hashCode();
                    
                    Intent intent = new Intent(context, NotificationReceiver.class);
                    intent.setAction(NotificationReceiver.ACTION_DUE);
                    intent.putExtra(NotificationReceiver.EXTRA_TASK_ID, task.getId());
                    intent.putExtra("user_email", user.getEmail());
                    intent.putExtra("user_name", user.getName());
                    
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 
                            dueCal.getTimeInMillis(), pendingIntent);
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, 
                            dueCal.getTimeInMillis(), pendingIntent);
                    }
                    
                    Log.d(TAG, "Scheduled due notification for user " + user.getEmail() + 
                        " at " + dueDateTimeString);
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error scheduling due notification for user: " + user.getEmail(), e);
        }
    }
    
    /**
     * Cancel notification cho một user cụ thể (khi xóa user khỏi shared task)
     */
    public void cancelNotificationForUser(String taskId, String userEmail) {
        String reminderKey = taskId + "_reminder_" + userEmail;
        String dueKey = taskId + "_due_" + userEmail;
        
        int reminderRequestCode = reminderKey.hashCode();
        int dueRequestCode = dueKey.hashCode();
        
        cancelAlarm(reminderRequestCode);
        cancelAlarm(dueRequestCode);
        
        Log.d(TAG, "Cancelled notifications for user: " + userEmail + " on task: " + taskId);
    }
    
    /**
     * Cancel tất cả notifications cho shared task (bao gồm owner và shared users)
     */
    public void cancelSharedTaskReminders(Task task, TaskShare taskShare) {
        if (task == null || taskShare == null) {
            return;
        }
        
        // Cancel cho owner
        cancelTaskReminders(task.getId().hashCode());
        
        // Cancel cho từng shared user
        if (taskShare.getSharedUsers() != null) {
            for (SharedUser user : taskShare.getSharedUsers()) {
                cancelNotificationForUser(task.getId(), user.getEmail());
            }
        }
        
        Log.d(TAG, "Cancelled all reminders for shared task: " + task.getId());
    }
}
