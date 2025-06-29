package com.example.todolist.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.SettingsManager;

/**
 * Helper class để quản lý notification
 */
public class NotificationHelper {
    private static final String CHANNEL_ID = "todolist_notifications";
    private static final String CHANNEL_NAME = "TodoList Notifications";
    private static final String CHANNEL_DESC = "Thông báo từ ứng dụng TodoList";
    
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String ACTION_DUE = "task_due";
    public static final String ACTION_REMINDER = "task_reminder";
    
    private Context context;
    private NotificationManager notificationManager;
    
    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    
    /**
     * Tạo notification channel cho Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.enableVibration(true);
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Hiển thị thông báo khi task sắp đến hạn
     */
    public void showDueNotification(TodoTask task) {
        // Kiểm tra xem notifications có được bật không
        if (!SettingsManager.isNotificationsEnabled(context)) {
            return;
        }
        
        String title = "Task sắp đến hạn!";
        String content = task.getTitle();
        if (task.getDueDate() != null && !task.getDueDate().equals("Không")) {
            content += "\nHạn: " + task.getDueDate();
        }
        
        int notificationId = ("due_" + task.getId()).hashCode();
        showNotification(notificationId, title, content, task, false);
    }
    
    /**
     * Hiển thị thông báo nhắc nhở task
     */
    public void showReminderNotification(TodoTask task) {
        // Kiểm tra xem notifications có được bật không
        if (!SettingsManager.isNotificationsEnabled(context)) {
            return;
        }
        
        String title = "Nhắc nhở task";
        String content = task.getTitle();
        if (task.getDueDate() != null && !task.getDueDate().equals("Không")) {
            content += "\nHạn: " + task.getDueDate();
        }
        
        int notificationId = ("reminder_" + task.getId()).hashCode();
        showNotification(notificationId, title, content, task, true);
    }
    
    /**
     * Hiển thị notification cơ bản
     */
    private void showNotification(int notificationId, String title, String content, TodoTask task, boolean isReminder) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_TASK_ID, task.getId());
        intent.putExtra("action", isReminder ? ACTION_REMINDER : ACTION_DUE);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Apply sound settings
        if (SettingsManager.isSoundEnabled(context)) {
            String ringtoneUri = SettingsManager.getRingtoneUri(context);
            if (ringtoneUri != null) {
                builder.setSound(android.net.Uri.parse(ringtoneUri));
            } else {
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
            }
        }
        
        // Apply vibration settings
        if (SettingsManager.isVibrationEnabled(context)) {
            builder.setVibrate(new long[]{0, 250, 250, 250}); // Pattern: wait, vibrate, wait, vibrate
        }
        
        // Apply lights
        builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS);

        // Thêm thông tin về thời gian và danh mục nếu có
        if (task.getDueTime() != null && !task.getDueTime().equals("Không")) {
            builder.setSubText("Thời gian: " + task.getDueTime());
        }

        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Hủy thông báo theo ID
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }
    
    /**
     * Hủy tất cả thông báo
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
    
    /**
     * Hủy thông báo cho một task cụ thể
     */
    public void cancelTaskNotifications(String taskId) {
        // Hủy thông báo due
        int dueNotificationId = ("due_" + taskId).hashCode();
        cancelNotification(dueNotificationId);
        
        // Hủy thông báo reminder
        int reminderNotificationId = ("reminder_" + taskId).hashCode();
        cancelNotification(reminderNotificationId);
    }
}
