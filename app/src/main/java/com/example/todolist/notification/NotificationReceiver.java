package com.example.todolist.notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import com.example.todolist.repository.BaseRepository;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    public static final String ACTION_REMINDER = "com.example.todolist.action.REMINDER_NOTIFICATION";
    public static final String ACTION_DUE = "com.example.todolist.action.DUE_NOTIFICATION";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_REMINDER_TYPE = "reminder_type";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NotificationReceiver.onReceive called");
        String action = intent.getAction();
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        
        Log.d(TAG, "Action: " + action + ", TaskId: " + taskId);
        
        if (taskId == null || taskId.isEmpty() || action == null) {
            Log.e(TAG, "Missing required data - action or taskId is null");
            return;
        }
        
        TaskService taskService = new TaskService(context, new TaskService.TaskUpdateListener() {
            @Override
            public void onTasksUpdated() {}
            @Override
            public void onError(String error) {}
        });
        
        taskService.getUncompletedTasks(new BaseRepository.RepositoryCallback<java.util.List<Task>>() {
            @Override
            public void onSuccess(java.util.List<Task> tasks) {
                for (Task task : tasks) {
                    if (task.getId().equals(taskId)) {
                        Log.d(TAG, "Found task: " + task.getTitle() + ", isCompleted: " + task.isCompleted());
                        if (!task.isCompleted()) {
                            NotificationHelper notificationHelper = new NotificationHelper(context);
                            if (ACTION_REMINDER.equals(action)) {
                                Log.d(TAG, "Showing reminder notification");
                                notificationHelper.showReminderNotification(task);
                            } else if (ACTION_DUE.equals(action)) {
                                Log.d(TAG, "Showing due notification");
                                notificationHelper.showDueNotification(task);
                            }
                        } else {
                            Log.d(TAG, "Task is completed, skipping notification");
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
            case "1 ngày trước":
                return "trong 1 ngày";
            default:
                return "sắp tới";
        }
    }
}
