package com.example.todolist.test;

import android.content.Context;
import android.util.Log;
import com.example.todolist.model.Task;
import com.example.todolist.notification.ReminderScheduler;
import com.example.todolist.service.TaskService;
import com.example.todolist.repository.BaseRepository;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Class để test và debug logic thông báo
 * Sử dụng để kiểm tra tại sao thông báo không hoạt động
 */
public class NotificationDebugHelper {
    private static final String TAG = "NotificationDebug";
    private Context context;
    
    public NotificationDebugHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Kiểm tra tất cả các task có reminder và in ra thông tin debug
     */
    public void debugAllTaskReminders() {
        TaskService taskService = new TaskService(context, new TaskService.TaskUpdateListener() {
            @Override
            public void onTasksUpdated() {}
            @Override
            public void onError(String error) {}
        });
        
        taskService.getUncompletedTasks(new BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                Log.d(TAG, "=== NOTIFICATION DEBUG ===");
                Log.d(TAG, "Total uncompleted tasks: " + tasks.size());
                
                int tasksWithReminder = 0;
                int validReminders = 0;
                int futureReminders = 0;
                
                for (Task task : tasks) {
                    if (task.isHasReminder()) {
                        tasksWithReminder++;
                        Log.d(TAG, "Task với reminder: " + task.getTitle());
                        Log.d(TAG, "  - ID: " + task.getId());
                        Log.d(TAG, "  - Due Date: " + task.getDueDate());
                        Log.d(TAG, "  - Due Time: " + task.getDueTime());
                        Log.d(TAG, "  - Reminder Type: " + task.getReminderType());
                        Log.d(TAG, "  - Is Completed: " + task.isCompleted());
                        Log.d(TAG, "  - Has Reminder: " + task.isHasReminder());
                        
                        // Kiểm tra tính hợp lệ của reminder
                        if (isValidReminderData(task)) {
                            validReminders++;
                            if (isReminderInFuture(task)) {
                                futureReminders++;
                                Log.d(TAG, "  - ✓ Reminder hợp lệ và trong tương lai");
                            } else {
                                Log.d(TAG, "  - ✗ Reminder đã qua thời gian");
                            }
                        } else {
                            Log.d(TAG, "  - ✗ Dữ liệu reminder không hợp lệ");
                        }
                        Log.d(TAG, "  ---");
                    }
                }
                
                Log.d(TAG, "Summary:");
                Log.d(TAG, "  - Tasks với reminder: " + tasksWithReminder);
                Log.d(TAG, "  - Reminder hợp lệ: " + validReminders);
                Log.d(TAG, "  - Reminder trong tương lai: " + futureReminders);
                Log.d(TAG, "=========================");
                
                taskService.cleanup();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting tasks: " + error);
                taskService.cleanup();
            }
        });
    }
    
    /**
     * Kiểm tra dữ liệu reminder có hợp lệ không
     */
    private boolean isValidReminderData(Task task) {
        String dueDate = task.getDueDate();
        String dueTime = task.getDueTime();
        String reminderType = task.getReminderType();
        
        return dueDate != null && !dueDate.equals("Không") &&
               dueTime != null && !dueTime.equals("Không") &&
               reminderType != null && !reminderType.equals("Không");
    }
    
    /**
     * Kiểm tra reminder có trong tương lai không
     */
    private boolean isReminderInFuture(Task task) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dateTimeString = task.getDueDate() + " " + task.getDueTime();
            Date dueDateTime = dateTimeFormat.parse(dateTimeString);
            
            if (dueDateTime == null) return false;
            
            Calendar dueCal = Calendar.getInstance();
            dueCal.setTime(dueDateTime);
            
            Calendar reminderCal = (Calendar) dueCal.clone();
            int reminderMinutes = getReminderMinutes(task.getReminderType());
            reminderCal.add(Calendar.MINUTE, -reminderMinutes);
            
            Calendar now = Calendar.getInstance();
            return reminderCal.getTimeInMillis() > now.getTimeInMillis() ||
                   dueCal.getTimeInMillis() > now.getTimeInMillis();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date/time", e);
            return false;
        }
    }
    
    /**
     * Test schedule một reminder cụ thể
     */
    public void testScheduleReminder(String taskId) {
        TaskService taskService = new TaskService(context, new TaskService.TaskUpdateListener() {
            @Override
            public void onTasksUpdated() {}
            @Override
            public void onError(String error) {}
        });
        
        taskService.getUncompletedTasks(new BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                for (Task task : tasks) {
                    if (task.getId().equals(taskId)) {
                        Log.d(TAG, "Testing schedule reminder for task: " + task.getTitle());
                        ReminderScheduler scheduler = new ReminderScheduler(context);
                        scheduler.scheduleTaskReminder(task);
                        break;
                    }
                }
                taskService.cleanup();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting task: " + error);
                taskService.cleanup();
            }
        });
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
}
