package com.example.todolist.notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.model.Task;
import com.example.todolist.util.SettingsManager;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
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
        SettingsManager.fixNotificationSettings(context);
        validateNotificationSettings(); // Kiá»ƒm tra vĂ  Ä‘á»“ng bá»™ cĂ i Ä‘áº·t
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.BLUE);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.setShowBadge(true);
            channel.setBypassDnd(true);
            notificationManager.createNotificationChannel(channel);
        }
    }
    public void showDueNotification(Task task) {
        Log.d(TAG, "showDueNotification called for task: " + task.getTitle());
        if (!SettingsManager.isNotificationsEnabled(context)) {
            Log.d(TAG, "Notifications disabled in settings");
            return;
        }
        
        String title = "🔔 Task đến hạn!";
        String content = task.getTitle();
        String expandedContent = task.getTitle();
        if (task.getDueDate() != null && !task.getDueDate().equals("Không")) {
            expandedContent += "\n🗓️ Hạn: " + task.getDueDate();
        }
        if (task.getDueTime() != null && !task.getDueTime().equals("Không")) {
            expandedContent += " ⏰ " + task.getDueTime();
        }
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            expandedContent += "\n📝 " + task.getDescription();
        }
        int notificationId = ("due_" + task.getId()).hashCode();
        showNotification(notificationId, title, content, expandedContent, task, false);
    }

    public void showReminderNotification(Task task) {
        Log.d(TAG, "showReminderNotification called for task: " + task.getTitle());
        if (!SettingsManager.isNotificationsEnabled(context)) {
            Log.d(TAG, "Notifications disabled in settings");
            return;
        }
        
        String title = "🕒 Nhắc nhở task";
        String content = task.getTitle();
        String expandedContent = task.getTitle();
        if (task.getDueDate() != null && !task.getDueDate().equals("Không")) {
            expandedContent += "\n🗓️ Sẽ đến hạn: " + task.getDueDate();
        }
        if (task.getDueTime() != null && !task.getDueTime().equals("Không")) {
            expandedContent += " ⏰ " + task.getDueTime();
        }
        if (task.getReminderType() != null && !task.getReminderType().equals("Không")) {
            expandedContent += "\n🕒 Nhắc: " + task.getReminderType();
        }
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            expandedContent += "\n📝 " + task.getDescription();
        }
        int notificationId = ("reminder_" + task.getId()).hashCode();
        showNotification(notificationId, title, content, expandedContent, task, true);
    }

    private void showNotification(int notificationId, String title, String content, String expandedContent, Task task, boolean isReminder) {
        Log.d(TAG, "showNotification called - id: " + notificationId + ", title: " + title);
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
        int iconRes = isReminder ? R.drawable.ic_schedule : R.drawable.ic_notifications;
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
        if (SettingsManager.isNotificationsEnabled(context) && SettingsManager.isSoundEnabled(context)) {
            String ringtoneUri = SettingsManager.getRingtoneUri(context);
            if (ringtoneUri != null && !ringtoneUri.isEmpty()) {
                try {
                    android.net.Uri soundUri = android.net.Uri.parse(ringtoneUri);
                    builder.setSound(soundUri);
                } catch (Exception e) {
                    builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
                }
            } else {

                builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
            }
        } else {
            builder.setSound(null);
        }
        builder.setVibrate(null);
        builder.setLights(task.isImportant() ? 0xFFFF0000 : 0xFF0000FF, 1000, 1000);
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification displayed successfully with id: " + notificationId);
    }

    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }

    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }

    public void cancelTaskNotifications(String taskId) {
        int dueNotificationId = ("due_" + taskId).hashCode();
        cancelNotification(dueNotificationId);
        int reminderNotificationId = ("reminder_" + taskId).hashCode();
        cancelNotification(reminderNotificationId);
    }

    public void validateNotificationSettings() {
        boolean notificationsEnabled = SettingsManager.isNotificationsEnabled(context);
        boolean soundEnabled = SettingsManager.isSoundEnabled(context);
        String ringtoneUri = SettingsManager.getRingtoneUri(context);
        String ringtoneName = SettingsManager.getRingtoneName(context);
        if (!notificationsEnabled) {
            if (soundEnabled) {
                SettingsManager.setSoundEnabled(context, false);
            }
        }
    }
}
