package com.example.todolist.notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import com.example.todolist.repository.BaseRepository;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            rescheduleAllReminders(context);
        }
    }
    private void rescheduleAllReminders(Context context) {
        TaskService taskService = new TaskService(context, new TaskService.TaskUpdateListener() {
            @Override
            public void onTasksUpdated() {
                // Not needed for this use case
            }
            @Override
            public void onError(String error) {
            }
        });
        taskService.getUncompletedTasks(new BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                ReminderScheduler scheduler = new ReminderScheduler(context);
                for (Task task : tasks) {
                    if (task.isHasReminder() && !task.isCompleted()) {
                        // Sử dụng scheduleTaskReminder thay vì scheduleReminder để đảm bảo tính nhất quán
                        scheduler.scheduleTaskReminder(task);
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
}
