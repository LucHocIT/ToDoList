package com.example.todolist.service.task;

import android.content.Context;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.notification.ReminderScheduler;
public class TaskManager {
    
    private Context context;
    private TaskRepository taskRepository;
    
    public TaskManager(Context context) {
        this.context = context;
        this.taskRepository = new TaskRepository(context);
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
        try {
            ReminderScheduler scheduler = new ReminderScheduler(context);
            scheduler.scheduleReminder(
                Integer.parseInt(task.getId().hashCode() + ""),
                task.getTitle(),
                task.getDueDate(),
                task.getDueTime()
            );
        } catch (Exception e) {
            // Handle silently
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
            scheduler.cancelReminder(Integer.parseInt(task.getId().hashCode() + ""));
        } catch (Exception e) {
        }
    }
}
