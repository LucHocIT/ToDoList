package com.example.todolist.cache;

import android.os.Handler;
import android.os.Looper;
import com.example.todolist.model.Task;
import com.example.todolist.helper.calendar.CalendarUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TaskCache {
    
    private static TaskCache instance;
    private final Map<String, Task> taskMap = new ConcurrentHashMap<>();
    private final Set<TaskCacheListener> listeners = new HashSet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
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

    public void addTaskOptimistic(Task task) {
        if (task != null && task.getId() != null) {
            taskMap.put(task.getId(), task);
            notifyTaskAdded(task);
            notifyTasksUpdated();
        }
    }

    public void updateTaskOptimistic(Task updatedTask) {
        if (updatedTask != null && updatedTask.getId() != null) {
            taskMap.put(updatedTask.getId(), updatedTask);
            notifyTaskUpdated(updatedTask);
            notifyTasksUpdated();
        }
    }

    public void deleteTaskOptimistic(String taskId) {
        if (taskId != null) {
            Task removedTask = taskMap.remove(taskId);
            if (removedTask != null) {
                notifyTaskDeleted(taskId);
                notifyTasksUpdated();
            }
        }
    }

    public void loadFromFirebase(List<Task> firebaseTasks) {
        taskMap.clear();
        for (Task task : firebaseTasks) {
            taskMap.put(task.getId(), task);
        }        
        isInitialized = true;
        isLoading = false;
        notifyTasksUpdated();
    }

    public void syncFromFirebase(List<Task> firebaseTasks) {
        Map<String, Task> firebaseTaskMap = new HashMap<>();
        for (Task task : firebaseTasks) {
            firebaseTaskMap.put(task.getId(), task);
        }
        for (Task firebaseTask : firebaseTasks) {
            Task localTask = taskMap.get(firebaseTask.getId());
            if (localTask == null) {
                taskMap.put(firebaseTask.getId(), firebaseTask);
            } else {

                taskMap.put(firebaseTask.getId(), firebaseTask);
            }
        }

        Set<String> toRemove = new HashSet<>();
        for (String localTaskId : taskMap.keySet()) {
            if (!firebaseTaskMap.containsKey(localTaskId)) {
                toRemove.add(localTaskId);
            }
        }
        for (String taskId : toRemove) {
            taskMap.remove(taskId);
        }

        notifyTasksUpdated();
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(taskMap.values());
    }

    public List<Task> getTasksForDate(String date) {
        List<Task> tasksForDate = new ArrayList<>();
        for (Task task : taskMap.values()) {
            if (CalendarUtils.isTaskOnDate(task, date)) {
                tasksForDate.add(task);
            }
        }
        return tasksForDate;
    }

    public Task getTask(String taskId) {
        return taskMap.get(taskId);
    }

    public boolean isInitialized() {
        return isInitialized;
    }
    
    public boolean isLoading() {
        return isLoading;
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
    }

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

    public void clear() {
        taskMap.clear();
        listeners.clear();
        isInitialized = false;
        isLoading = false;
    }
}
