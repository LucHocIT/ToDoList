package com.example.todolist.service.task;

import android.content.Context;
import android.util.Log;
import com.example.todolist.model.Task;
import com.example.todolist.model.TaskShare;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.notification.ReminderScheduler;
import com.example.todolist.service.sharing.TaskSharingService;
public class TaskManager {
    private static final String TAG = "TaskManager";
    
    private Context context;
    private TaskRepository taskRepository;
    private TaskSharingService taskSharingService;
    
    public TaskManager(Context context) {
        this.context = context;
        this.taskRepository = new TaskRepository(context);
        this.taskSharingService = TaskSharingService.getInstance();
        this.taskSharingService.initialize(context);
    }
    
    public void addTask(Task task, BaseRepository.DatabaseCallback<String> callback) {
        taskRepository.addTask(task, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String taskId) {
                task.setId(taskId);
                if (task.isHasReminder()) {
                    scheduleReminder(task);
                }
                if (callback != null) callback.onSuccess(taskId);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    public void updateTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        taskRepository.updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                updateReminder(task);
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    public void deleteTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        taskRepository.deleteTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                cancelReminder(task);
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    public void getTaskById(String taskId, BaseRepository.RepositoryCallback<Task> callback) {
        taskRepository.getTaskById(taskId, callback);
    }
    
    public void searchTasks(String query, BaseRepository.RepositoryCallback<java.util.List<Task>> callback) {
        taskRepository.getAllTasks(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(java.util.List<Task> tasks) {
                java.util.List<Task> filtered = new java.util.ArrayList<>();
                String lowercaseQuery = query.toLowerCase();
                
                for (Task task : tasks) {
                    if (task.getTitle().toLowerCase().contains(lowercaseQuery) ||
                        (task.getDescription() != null && task.getDescription().toLowerCase().contains(lowercaseQuery))) {
                        filtered.add(task);
                    }
                }
                callback.onSuccess(filtered);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    private void scheduleReminder(Task task) {
        if (!task.isHasReminder()) {
            return;
        }
        
        try {
            ReminderScheduler scheduler = new ReminderScheduler(context);
            
            // Check nếu task được share
            if (task.isShared()) {
                Log.d(TAG, "Scheduling reminder for shared task: " + task.getId());
                
                // Lấy TaskShare để schedule cho tất cả users
                taskSharingService.getTaskShare(task.getId(), new TaskSharingService.TaskShareCallback() {
                    @Override
                    public void onTaskShareLoaded(TaskShare taskShare) {
                        if (taskShare != null) {
                            scheduler.scheduleSharedTaskReminder(task, taskShare);
                            Log.d(TAG, "Scheduled reminders for owner and " + 
                                (taskShare.getSharedUsers() != null ? taskShare.getSharedUsers().size() : 0) + 
                                " shared users");
                        } else {
                            // Fallback: schedule bình thường nếu không lấy được TaskShare
                            scheduler.scheduleTaskReminder(task);
                            Log.w(TAG, "TaskShare not found, scheduled reminder for owner only");
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        // Fallback: schedule bình thường nếu có lỗi
                        scheduler.scheduleTaskReminder(task);
                        Log.e(TAG, "Error getting TaskShare: " + error + ", scheduled reminder for owner only");
                    }
                });
            } else {
                // Task không share → schedule bình thường (chỉ cho owner)
                scheduler.scheduleTaskReminder(task);
                Log.d(TAG, "Scheduled reminder for non-shared task: " + task.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder", e);
        }
    }
    
    private void updateReminder(Task task) {
        if (task.isHasReminder()) {
            scheduleReminder(task);
        } else {
            cancelReminder(task);
        }
    }
    
    private void cancelReminder(Task task) {
        try {
            ReminderScheduler scheduler = new ReminderScheduler(context);
            
            // Check nếu task được share
            if (task.isShared()) {
                Log.d(TAG, "Cancelling reminders for shared task: " + task.getId());
                
                // Lấy TaskShare để cancel cho tất cả users
                taskSharingService.getTaskShare(task.getId(), new TaskSharingService.TaskShareCallback() {
                    @Override
                    public void onTaskShareLoaded(TaskShare taskShare) {
                        if (taskShare != null) {
                            scheduler.cancelSharedTaskReminders(task, taskShare);
                            Log.d(TAG, "Cancelled reminders for owner and " + 
                                (taskShare.getSharedUsers() != null ? taskShare.getSharedUsers().size() : 0) + 
                                " shared users");
                        } else {
                            // Fallback: cancel bình thường
                            scheduler.cancelTaskReminders(task.getId().hashCode());
                            Log.w(TAG, "TaskShare not found, cancelled reminder for owner only");
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        // Fallback: cancel bình thường nếu có lỗi
                        scheduler.cancelTaskReminders(task.getId().hashCode());
                        Log.e(TAG, "Error getting TaskShare: " + error + ", cancelled reminder for owner only");
                    }
                });
            } else {
                // Task không share → cancel bình thường
                scheduler.cancelTaskReminders(task.getId().hashCode());
                Log.d(TAG, "Cancelled reminder for non-shared task: " + task.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling reminder", e);
        }
    }
}
