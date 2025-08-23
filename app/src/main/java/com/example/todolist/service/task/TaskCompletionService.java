package com.example.todolist.service.task;

import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import java.util.List;

/**
 * Task completion and status management
 */
public class TaskCompletionService {
    
    private TaskRepository taskRepository;
    
    public TaskCompletionService() {
        this.taskRepository = new TaskRepository();
    }
    
    public void completeTask(Task task, boolean isCompleted, BaseRepository.DatabaseCallback<Boolean> callback) {
        // Update local state
        task.setCompleted(isCompleted);
        if (isCompleted) {
            task.setCompletionDate(String.valueOf(System.currentTimeMillis()));
        } else {
            task.setCompletionDate(null);
        }
        
        // Update in database
        taskRepository.updateTask(task, callback);
    }
    
    public void toggleTaskImportance(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        task.setIsImportant(!task.isImportant());
        taskRepository.updateTask(task, callback);
    }
    
    public void getCompletedTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        taskRepository.getAllTasks(new BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                List<Task> completedTasks = new java.util.ArrayList<>();
                for (Task task : tasks) {
                    if (task.isCompleted()) {
                        completedTasks.add(task);
                    }
                }
                callback.onSuccess(completedTasks);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public void getIncompleteTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        taskRepository.getAllTasks(new BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                List<Task> incompleteTasks = new java.util.ArrayList<>();
                for (Task task : tasks) {
                    if (!task.isCompleted()) {
                        incompleteTasks.add(task);
                    }
                }
                callback.onSuccess(incompleteTasks);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    public boolean isTaskCompletedToday(Task task) {
        if (task.getCompletionDate() == null) return false;
        
        try {
            long completionTime = Long.parseLong(task.getCompletionDate());
            String completionDateStr = formatDate(new java.util.Date(completionTime));
            String todayDateStr = formatDate(new java.util.Date());
            return todayDateStr.equals(completionDateStr);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private String formatDate(java.util.Date date) {
        return new java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(date);
    }
}
