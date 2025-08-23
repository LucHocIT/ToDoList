package com.example.todolist.notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import com.example.todolist.repository.BaseRepository;
public class NotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_REMINDER = "reminder_notification";
    public static final String ACTION_DUE = "due_notification";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_REMINDER_TYPE = "reminder_type";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int taskIntId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        if (taskIntId == -1) return;
        // Convert int ID back to String by finding task
        TaskService taskService = new TaskService(context, new TaskService.TaskUpdateListener() {
            @Override
            public void onTasksUpdated() {}
            @Override
            public void onError(String error) {}
        });
        // Get all tasks and find the one with matching hash
        taskService.getUncompletedTasks(new BaseRepository.RepositoryCallback<java.util.List<Task>>() {
            @Override
            public void onSuccess(java.util.List<Task> tasks) {
                for (Task task : tasks) {
                    if (Math.abs(task.getId().hashCode()) == taskIntId) {
                        if (!task.isCompleted()) {
                            NotificationHelper notificationHelper = new NotificationHelper(context);
                            if (ACTION_REMINDER.equals(action)) {
                                String reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE);
                                notificationHelper.showReminderNotification(task);
                            } else if (ACTION_DUE.equals(action)) {
                                notificationHelper.showDueNotification(task);
                            }
                        }
                        break;
                    }
                }
                taskService.cleanup();
            }
            @Override
            public void onError(String error) {
                taskService.cleanup();
            }
        });
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
            default:
                return "sắp tới";
        }
    }
}
