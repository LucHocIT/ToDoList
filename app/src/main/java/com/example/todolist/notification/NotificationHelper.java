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

public class NotificationHelper {

    private static final String CHANNEL_ID = "todo_reminders";
    private static final String CHANNEL_NAME = "Lời nhắc nhiệm vụ";
    private static final String CHANNEL_DESCRIPTION = "Thông báo lời nhắc cho các nhiệm vụ sắp tới hạn";
    
    // Notification IDs
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String ACTION_REMINDER = "reminder";
    public static final String ACTION_DUE = "due";

    private Context context;
    private NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLightColor(android.graphics.Color.BLUE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Hiển thị thông báo lời nhắc trước khi nhiệm vụ đến hạn
     */
    public void showReminderNotification(TodoTask task, String reminderTime) {
        String title = "⏰ Lời nhắc nhiệm vụ";
        String content = "\"" + task.getTitle() + "\" sẽ đến hạn " + reminderTime;
        
        showNotification(task.getId() * 10, title, content, task, true);
    }

    /**
     * Hiển thị thông báo khi nhiệm vụ đến hạn
     */
    public void showDueNotification(TodoTask task) {
        String title = "🔔 Nhiệm vụ đến hạn";
        String content = "\"" + task.getTitle() + "\" đã đến hạn";
        
        showNotification(task.getId() * 10 + 1, title, content, task, false);
    }

    /**
     * Hiển thị thông báo chung
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
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

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
     * Hủy tất cả thông báo của một task
     */
    public void cancelTaskNotifications(int taskId) {
        // Hủy thông báo reminder
        notificationManager.cancel(taskId * 10);
        // Hủy thông báo due
        notificationManager.cancel(taskId * 10 + 1);
    }
}
