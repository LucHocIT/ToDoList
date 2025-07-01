package com.example.todolist.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.util.Log;

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
        validateNotificationSettings(); // Kiểm tra và đồng bộ cài đặt
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
            
            // Tắt âm thanh channel mặc định để app tự quản lý âm thanh
            channel.setSound(null, null);
            
            // Tắt rung channel mặc định để app tự quản lý rung
            channel.enableVibration(false);
            
            // Cấu hình đèn LED - luôn bật để dễ nhận biết
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.BLUE);
            
            // Hiển thị trên màn hình khóa
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            
            // Cho phép hiển thị badge
            channel.setShowBadge(true);
            
            // Bypass Do Not Disturb cho thông báo quan trọng
            channel.setBypassDnd(true);
            
            notificationManager.createNotificationChannel(channel);
        }
    }
      /**
     * Hiển thị thông báo khi task đến hạn đúng giờ
     */
    public void showDueNotification(TodoTask task) {
        // Kiểm tra xem notifications có được bật không
        if (!SettingsManager.isNotificationsEnabled(context)) {
            return;
        }
        
        String title = "⏰ Task đến hạn!";
        String content = task.getTitle();
        String expandedContent = task.getTitle();
        
        if (task.getDueDate() != null && !task.getDueDate().equals("Không")) {
            expandedContent += "\n📅 Hạn: " + task.getDueDate();
        }
        if (task.getDueTime() != null && !task.getDueTime().equals("Không")) {
            expandedContent += " ⏱️ " + task.getDueTime();
        }
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            expandedContent += "\n📝 " + task.getDescription();
        }
        
        int notificationId = ("due_" + task.getId()).hashCode();
        showNotification(notificationId, title, content, expandedContent, task, false);
    }

    /**
     * Hiển thị thông báo nhắc nhở task (trước khi đến hạn)
     */
    public void showReminderNotification(TodoTask task) {
        // Kiểm tra xem notifications có được bật không
        if (!SettingsManager.isNotificationsEnabled(context)) {
            return;
        }
        
        String title = "🔔 Nhắc nhở task";
        String content = task.getTitle();
        String expandedContent = task.getTitle();
        
        if (task.getDueDate() != null && !task.getDueDate().equals("Không")) {
            expandedContent += "\n📅 Sẽ đến hạn: " + task.getDueDate();
        }
        if (task.getDueTime() != null && !task.getDueTime().equals("Không")) {
            expandedContent += " ⏱️ " + task.getDueTime();
        }
        if (task.getReminderType() != null && !task.getReminderType().equals("Không")) {
            expandedContent += "\n⏰ Nhắc: " + task.getReminderType();
        }
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            expandedContent += "\n📝 " + task.getDescription();
        }
        
        int notificationId = ("reminder_" + task.getId()).hashCode();
        showNotification(notificationId, title, content, expandedContent, task, true);
    }
    
    /**
     * Hiển thị notification cơ bản với giao diện cải thiện
     */
    private void showNotification(int notificationId, String title, String content, String expandedContent, TodoTask task, boolean isReminder) {
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

        // Chọn icon dựa trên loại thông báo
        int iconRes = isReminder ? R.drawable.ic_schedule : R.drawable.ic_notifications;
        
        // Chọn màu dựa trên độ ưu tiên
        int color = task.isImportant() ? 0xFFFF5722 : 0xFF4CAF50; // Đỏ cho quan trọng, xanh cho bình thường

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(expandedContent)
                    .setBigContentTitle(title))
                .setPriority(task.isImportant() ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setColor(color)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setContentIntent(pendingIntent);

        // Apply sound settings
        if (SettingsManager.isSoundEnabled(context)) {
            String ringtoneUri = SettingsManager.getRingtoneUri(context);
            if (ringtoneUri != null && !ringtoneUri.isEmpty()) {
                try {
                    android.net.Uri soundUri = android.net.Uri.parse(ringtoneUri);
                    builder.setSound(soundUri);
                } catch (Exception e) {
                    // Fallback to default sound if custom sound fails
                    builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
                }
            } else {
                // Sử dụng âm thanh mặc định khi không có URI được lưu
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
            }
        } else {
            // Tắt âm thanh hoàn toàn
            builder.setSound(null);
        }
        
        // Apply vibration settings
        if (SettingsManager.isVibrationEnabled(context)) {
            if (task.isImportant()) {
                // Vibration pattern cho task quan trọng (dài hơn, mạnh hơn)
                builder.setVibrate(new long[]{0, 300, 100, 300, 100, 300, 100, 300});
            } else {
                // Vibration pattern cho task bình thường (ngắn gọn)
                builder.setVibrate(new long[]{0, 250, 250, 250});
            }
        } else {
            // Tắt rung hoàn toàn
            builder.setVibrate(null);
        }
        
        // Apply lights (luôn bật để dễ nhận biết)
        builder.setLights(task.isImportant() ? 0xFFFF0000 : 0xFF0000FF, 1000, 1000);

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
    
    /**
     * Kiểm tra và áp dụng tất cả cài đặt thông báo
     */
    public void validateNotificationSettings() {
        // Kiểm tra xem notification có được bật không
        boolean notificationsEnabled = SettingsManager.isNotificationsEnabled(context);
        boolean soundEnabled = SettingsManager.isSoundEnabled(context);
        boolean vibrationEnabled = SettingsManager.isVibrationEnabled(context);
        
        Log.d("NotificationHelper", "Notification Settings Check:");
        Log.d("NotificationHelper", "- Notifications enabled: " + notificationsEnabled);
        Log.d("NotificationHelper", "- Sound enabled: " + soundEnabled);
        Log.d("NotificationHelper", "- Vibration enabled: " + vibrationEnabled);
        
        String ringtoneUri = SettingsManager.getRingtoneUri(context);
        String ringtoneName = SettingsManager.getRingtoneName(context);
        
        Log.d("NotificationHelper", "- Ringtone URI: " + (ringtoneUri != null ? ringtoneUri : "Default"));
        Log.d("NotificationHelper", "- Ringtone Name: " + ringtoneName);
        
        // Tự động tắt sound và vibration nếu notification bị tắt
        if (!notificationsEnabled) {
            if (soundEnabled) {
                SettingsManager.setSoundEnabled(context, false);
                Log.d("NotificationHelper", "Auto-disabled sound because notifications are disabled");
            }
            if (vibrationEnabled) {
                SettingsManager.setVibrationEnabled(context, false);
                Log.d("NotificationHelper", "Auto-disabled vibration because notifications are disabled");
            }
        }
    }
}
