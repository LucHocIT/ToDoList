package com.example.todolist.cache;

import android.os.Handler;
import android.os.Looper;
import com.example.todolist.model.Task;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TaskCache {
    
    private static TaskCache instance;
    private final Map<String, Task> taskMap = new ConcurrentHashMap<>();
    private final Set<TaskCacheListener> listeners = new HashSet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // State flags
    private boolean isInitialized = false;
    private boolean isLoading = false;
    
    public static TaskCache getInstance() {
        if (instance == null) {
            synchronized (TaskCache.class) {
                if (instance == null) {
                    instance = new TaskCache();
                }
            }
        }
        return instance;
    }
    
    private TaskCache() {}
    public interface TaskCacheListener {
        void onTasksUpdated(List<Task> tasks);
        void onTaskAdded(Task task);
        void onTaskUpdated(Task task);
        void onTaskDeleted(String taskId);
    }
    
    // === LISTENERS ===
    public void addListener(TaskCacheListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(TaskCacheListener listener) {
        listeners.remove(listener);
    }
    
    // === OPTIMISTIC OPERATIONS ===
    
    /**
     * Thêm task mới - optimistic update
     */
    public void addTaskOptimistic(Task task) {
        // 1. Update local cache ngay lập tức
        taskMap.put(task.getId(), task);
        
        // 2. Notify UI update
        notifyTaskAdded(task);
        notifyTasksUpdated();
    }
    
    /**
     * Cập nhật task - optimistic update
     */
    public void updateTaskOptimistic(Task updatedTask) {
        // 1. Update local cache ngay lập tức
        taskMap.put(updatedTask.getId(), updatedTask);
        
        // 2. Notify UI update
        notifyTaskUpdated(updatedTask);
        notifyTasksUpdated();
    }
    
    /**
     * Xóa task - optimistic update
     */
    public void deleteTaskOptimistic(String taskId) {
        // 1. Remove từ local cache ngay lập tức
        Task removedTask = taskMap.remove(taskId);
        
        // 2. Notify UI update
        if (removedTask != null) {
            notifyTaskDeleted(taskId);
            notifyTasksUpdated();
        }
    }
    
    // === FIREBASE SYNC ===
    
    /**
     * Load initial data từ Firebase và cache
     */
    public void loadFromFirebase(List<Task> firebaseTasks) {
        // Clear cache và load fresh data
        taskMap.clear();
        for (Task task : firebaseTasks) {
            taskMap.put(task.getId(), task);
        }
        
        isInitialized = true;
        isLoading = false;
        
        // Notify UI
        notifyTasksUpdated();
    }

    public void syncFromFirebase(List<Task> firebaseTasks) {
        // Merge Firebase data với local cache
        Map<String, Task> newTaskMap = new ConcurrentHashMap<>();
        for (Task task : firebaseTasks) {
            newTaskMap.put(task.getId(), task);
        }
        
        // Replace cache
        taskMap.clear();
        taskMap.putAll(newTaskMap);
        
        // Notify UI update
        notifyTasksUpdated();
    }
    
    // === GETTERS ===
    
    /**
     * Get all tasks từ cache
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(taskMap.values());
    }
    
    /**
     * Get tasks cho ngày cụ thể
     */
    public List<Task> getTasksForDate(String date) {
        List<Task> tasksForDate = new ArrayList<>();
        for (Task task : taskMap.values()) {
            if (isTaskOnDate(task, date)) {
                tasksForDate.add(task);
            }
        }
        return tasksForDate;
    }
    
    /**
     * Get single task by ID
     */
    public Task getTask(String taskId) {
        return taskMap.get(taskId);
    }
    
    // === STATE ===
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public boolean isLoading() {
        return isLoading;
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
    }
    
    // === NOTIFICATIONS ===
    
    private void notifyTasksUpdated() {
        mainHandler.post(() -> {
            List<Task> allTasks = getAllTasks();
            for (TaskCacheListener listener : listeners) {
                listener.onTasksUpdated(allTasks);
            }
        });
    }
    
    private void notifyTaskAdded(Task task) {
        mainHandler.post(() -> {
            for (TaskCacheListener listener : listeners) {
                listener.onTaskAdded(task);
            }
        });
    }
    
    private void notifyTaskUpdated(Task task) {
        mainHandler.post(() -> {
            for (TaskCacheListener listener : listeners) {
                listener.onTaskUpdated(task);
            }
        });
    }
    
    private void notifyTaskDeleted(String taskId) {
        mainHandler.post(() -> {
            for (TaskCacheListener listener : listeners) {
                listener.onTaskDeleted(taskId);
            }
        });
    }
    
    // === HELPER ===
    
    private boolean isTaskOnDate(Task task, String targetDate) {
        // Simple date check - có thể optimize thêm
        if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
            return false;
        }
        
        try {
            String[] taskDateParts = task.getDueDate().split("/");
            String[] targetDateParts = targetDate.split("/");
            
            if (taskDateParts.length != 3 || targetDateParts.length != 3) {
                return false;
            }
            
            Calendar taskDate = Calendar.getInstance();
            taskDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(taskDateParts[0]));
            taskDate.set(Calendar.MONTH, Integer.parseInt(taskDateParts[1]) - 1);
            taskDate.set(Calendar.YEAR, Integer.parseInt(taskDateParts[2]));

            Calendar targetCalendar = Calendar.getInstance();
            targetCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(targetDateParts[0]));
            targetCalendar.set(Calendar.MONTH, Integer.parseInt(targetDateParts[1]) - 1);
            targetCalendar.set(Calendar.YEAR, Integer.parseInt(targetDateParts[2]));

            if (!task.isRepeating() || task.getRepeatType() == null || task.getRepeatType().equals("Không có")) {
                return task.getDueDate().equals(targetDate);
            }

            // Handle repeating tasks
            if (targetCalendar.before(taskDate)) {
                return false;
            }

            switch (task.getRepeatType()) {
                case "Hằng ngày":
                    return !targetCalendar.before(taskDate);
                case "Hằng tuần":
                    if (targetCalendar.get(Calendar.DAY_OF_WEEK) == taskDate.get(Calendar.DAY_OF_WEEK)) {
                        long diffInMillis = targetCalendar.getTimeInMillis() - taskDate.getTimeInMillis();
                        long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                        return diffInDays >= 0 && diffInDays % 7 == 0;
                    }
                    return false;
                case "Hằng tháng":
                    if (targetCalendar.get(Calendar.DAY_OF_MONTH) == taskDate.get(Calendar.DAY_OF_MONTH)) {
                        int taskYear = taskDate.get(Calendar.YEAR);
                        int taskMonth = taskDate.get(Calendar.MONTH);
                        int targetYear = targetCalendar.get(Calendar.YEAR);
                        int targetMonth = targetCalendar.get(Calendar.MONTH);
                        int monthDiff = (targetYear - taskYear) * 12 + (targetMonth - taskMonth);
                        return monthDiff >= 0;
                    }
                    return false;
                default:
                    return task.getDueDate().equals(targetDate);
            }
        } catch (Exception e) {
            return task.getDueDate().equals(targetDate);
        }
    }
    
    /**
     * Clear cache (for logout/reset)
     */
    public void clear() {
        taskMap.clear();
        listeners.clear();
        isInitialized = false;
        isLoading = false;
    }
}
