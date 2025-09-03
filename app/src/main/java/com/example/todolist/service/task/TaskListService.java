package com.example.todolist.service.task;

import android.content.Context;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import java.util.*;
import java.text.SimpleDateFormat;

public class TaskListService {
    
    private TaskRepository taskRepository;
    private SimpleDateFormat dateFormat;
    
    // Categorized lists
    private List<Task> overdueTasks;
    private List<Task> todayTasks;
    private List<Task> futureTasks;
    private List<Task> completedTodayTasks;
    
    public TaskListService() {
        // Constructor without context - will use null context for repository
        // This is for compatibility with existing code that doesn't pass context
        this.taskRepository = null; // Will be set when needed
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        initializeLists();
    }
    
    public TaskListService(Context context) {
        this.taskRepository = new TaskRepository(context);
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        initializeLists();
    }
    
    private void initializeLists() {
        overdueTasks = new ArrayList<>();
        todayTasks = new ArrayList<>();
        futureTasks = new ArrayList<>();
        completedTodayTasks = new ArrayList<>();
    }
    
    public void categorizeTasks(List<Task> allTasks) {
        clearLists();
        String todayDateStr = dateFormat.format(new Date());
        
        for (Task task : allTasks) {
            if (task.isCompleted()) {
                if (isTaskCompletedToday(task, todayDateStr)) {
                    completedTodayTasks.add(task);
                }
            } else {
                categorizeTaskByDate(task, todayDateStr);
            }
        }
    }
    
    // Add missing updateTasks method
    public void updateTasks(List<Task> tasks) {
        categorizeTasks(tasks);
    }
    
    private void categorizeTaskByDate(Task task, String todayDateStr) {
        int timeCategory = getTaskTimeCategory(task, todayDateStr);
        
        switch (timeCategory) {
            case -1: overdueTasks.add(task); break;    
            case 0:  todayTasks.add(task); break;     
            case 1:  futureTasks.add(task); break;     
        }
    }
    
    private int getTaskTimeCategory(Task task, String todayDateStr) {
        if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
            return 1; 
        }

        try {
            Date taskDate = parseTaskDate(task.getDueDate());
            Date today = dateFormat.parse(todayDateStr);
            
            if (taskDate.before(today)) {
                return -1; 
            } else if (taskDate.equals(today)) {
                return 0; 
            } else {
                return 1;
            }
        } catch (Exception e) {
            return 1;
        }
    }    

    private Date parseTaskDate(String dateString) throws Exception {
        try {
            return dateFormat.parse(dateString);
        } catch (Exception e) {
            SimpleDateFormat legacyFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            return legacyFormat.parse(dateString);
        }
    }
    
    private boolean isTaskCompletedToday(Task task, String todayDateStr) {
        if (task.getCompletionDate() == null) return false;
        
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String todayDisplayDate = displayFormat.format(new Date());
        return todayDisplayDate.equals(task.getCompletionDate());
    }
    
    public void getTasksByDate(String date, BaseRepository.RepositoryCallback<List<Task>> callback) {
        taskRepository.getAllTasks(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                List<Task> filteredTasks = new ArrayList<>();
                for (Task task : tasks) {
                    if (isDateMatch(date, task.getDueDate())) {
                        filteredTasks.add(task);
                    }
                }
                callback.onSuccess(filteredTasks);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    private boolean isDateMatch(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        if (date1.equals(date2)) return true;
        
        try {
            Date d1 = parseTaskDate(date1);
            Date d2 = parseTaskDate(date2);
            return d1.equals(d2);
        } catch (Exception e) {
            return false;
        }
    }
    
    public void getTasksByCategory(String categoryId, BaseRepository.RepositoryCallback<List<Task>> callback) {
        taskRepository.getTasksByCategory(categoryId, callback);
    }
    
    private void clearLists() {
        overdueTasks.clear();
        todayTasks.clear();
        futureTasks.clear();
        completedTodayTasks.clear();
    }
    
    // Getters
    public List<Task> getOverdueTasks() { return new ArrayList<>(overdueTasks); }
    public List<Task> getTodayTasks() { return new ArrayList<>(todayTasks); }
    public List<Task> getFutureTasks() { return new ArrayList<>(futureTasks); }
    public List<Task> getCompletedTodayTasks() { return new ArrayList<>(completedTodayTasks); }
}
