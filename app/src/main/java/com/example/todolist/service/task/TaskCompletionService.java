package com.example.todolist.service.task;

import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import java.util.List;

public class TaskCompletionService {
    
    private TaskRepository taskRepository;
    
    public TaskCompletionService() {
        this.taskRepository = new TaskRepository();
    }
    
    public void completeTask(Task task, boolean isCompleted, BaseRepository.DatabaseCallback<Boolean> callback) {
        task.setCompleted(isCompleted);
        
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
    
    public boolean isCompletedToday(Task task) {
        if (task.getCompletionDate() == null) return false;
        
        // Since completion date is now in dd/MM/yyyy format, compare directly
        String todayDateStr = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        return todayDateStr.equals(task.getCompletionDate());
    }
    
    public boolean isTaskCompletedToday(Task task) {
        return isCompletedToday(task);
    }
    
    private String formatDate(java.util.Date date) {
        return new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date);
    }
}
