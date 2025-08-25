package com.example.todolist.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;

import com.example.todolist.cache.TaskCache;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.service.task.TaskManager;
import com.example.todolist.service.task.TaskCompletionService;
import com.example.todolist.service.task.TaskListService;
import com.example.todolist.widget.WidgetUpdateHelper;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TaskService implements TaskCache.TaskCacheListener {
    
    public interface TaskUpdateListener {
        void onTasksUpdated();
        void onError(String error);
    }

    public interface TaskOperationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    private Context context;
    private TaskRepository taskRepository;
    private TaskUpdateListener listener;
    private ValueEventListener realtimeListener;
    private TaskManager taskManager;              
    private TaskCompletionService completionService;  
    private TaskListService listService;         
    private TaskCache taskCache;
    private Handler firebaseUpdateHandler;
    private List<Task> allTasks;
    private Runnable pendingFirebaseUpdate;
    private long lastLocalUpdateTime;
    private static final long LOCAL_UPDATE_PRIORITY_WINDOW = 1000;
    private static final long FIREBASE_UPDATE_DELAY = 500; 

    public TaskService(Context context, TaskUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.taskRepository = new TaskRepository();
        this.taskCache = TaskCache.getInstance();
        taskCache.addListener(this);
        this.firebaseUpdateHandler = new Handler(Looper.getMainLooper());
        
        this.taskManager = new TaskManager(context);
        this.completionService = new TaskCompletionService();
        this.listService = new TaskListService();
        
        this.allTasks = new ArrayList<>();
    }

    public void loadTasks() {
        if (taskCache.isInitialized()) {
            if (listener != null) {
                listener.onTasksUpdated();
            }
            return;
        }
        taskCache.setLoading(true);
        realtimeListener = taskRepository.addTasksRealtimeListener(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                if (!taskCache.isInitialized()) {
                    // Lần đầu tiên - load vào cache
                    taskCache.loadFromFirebase(tasks);
                } else {
                    // Đã có cache - sync background
                    taskCache.syncFromFirebase(tasks);
                }
            }

            @Override
            public void onError(String error) {
                taskCache.setLoading(false);
                if (listener != null) {
                    listener.onError("Lỗi tải tasks: " + error);
                }
            }
        });
    }
    
    private void handleFirebaseTasksUpdate(List<Task> tasks) {
        if (pendingFirebaseUpdate != null) {
            firebaseUpdateHandler.removeCallbacks(pendingFirebaseUpdate);
        }
        
        pendingFirebaseUpdate = () -> {
            long timeSinceLastLocalUpdate = System.currentTimeMillis() - lastLocalUpdateTime;
            if (timeSinceLastLocalUpdate < LOCAL_UPDATE_PRIORITY_WINDOW) {
                pendingFirebaseUpdate = null;
                return;
            }
            
            allTasks = tasks;
            listService.categorizeTasks(allTasks);
            
            notifyListener();
            WidgetUpdateHelper.updateAllWidgets(context);
            pendingFirebaseUpdate = null;
        };
        
        firebaseUpdateHandler.postDelayed(pendingFirebaseUpdate, FIREBASE_UPDATE_DELAY);
    }
    
    private void notifyListener() {
        if (listener != null) {
            listener.onTasksUpdated();
        }
    }

    public void addTask(Task task) {
        addTask(task, null);
    }
    
    public void addTask(Task task, TaskOperationCallback callback) {
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(String.valueOf(System.currentTimeMillis()) + "_" + Math.random());
        }
        
        taskCache.addTaskOptimistic(task);
        taskManager.addTask(task, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String taskId) {
                if (!task.getId().equals(taskId)) {
                    taskCache.deleteTaskOptimistic(task.getId());
                    task.setId(taskId);
                    taskCache.addTaskOptimistic(task);
                }
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                // Rollback optimistic update nếu Firebase fail
                taskCache.deleteTaskOptimistic(task.getId());
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void updateTask(Task task) {
        updateTask(task, null);
    }
    
    public void updateTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        // 1. Optimistic update - cập nhật cache ngay  
        taskCache.updateTaskOptimistic(task);
        
        // 2. Sync với Firebase ngầm
        taskManager.updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                // Task đã được update thành công trên Firebase
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                // TODO: Có thể rollback optimistic update nếu cần
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void deleteTask(Task task) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa task này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // 1. Optimistic update - xóa khỏi cache ngay
                    taskCache.deleteTaskOptimistic(task.getId());
                    
                    // 2. Sync với Firebase ngầm
                    taskManager.deleteTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            // Success handled by Firebase listener
                        }

                        @Override
                        public void onError(String error) {
                            if (listener != null) {
                                listener.onError("Lỗi xóa task: " + error);
                            }
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    public void deleteTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        taskManager.deleteTask(task, callback);
    }

    public void completeTask(Task task, boolean isCompleted) {
        lastLocalUpdateTime = System.currentTimeMillis();
        cancelPendingFirebaseUpdates();
        completionService.completeTask(task, isCompleted, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                updateTaskInList(task);
                listService.categorizeTasks(allTasks);
                notifyListener();
            }

            @Override
            public void onError(String error) {
                // Revert on error
                task.setIsCompleted(!isCompleted);
                task.setCompletionDate(isCompleted ? null : String.valueOf(System.currentTimeMillis()));
                
                updateTaskInList(task);
                listService.categorizeTasks(allTasks);
                notifyListener();
                
                if (listener != null) {
                    listener.onError("Lỗi cập nhật trạng thái task: " + error);
                }
            }
        });
    }

    public void toggleTaskCompletion(Task task) {
        completeTask(task, !task.isCompleted());
    }
    
    public void toggleTaskImportance(Task task) {
        completionService.toggleTaskImportance(task, null);
    }
    
    public List<Task> getIncompleteTasks() { 
        List<Task> incomplete = new ArrayList<>();
        for (Task task : taskCache.getAllTasks()) {
            if (!task.isCompleted()) {
                incomplete.add(task);
            }
        }
        return incomplete;
    }
    
    public void getAllTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        taskRepository.getAllTasks(callback);
    }
    
    public void searchTasks(String query, BaseRepository.RepositoryCallback<List<Task>> callback) {
        taskManager.searchTasks(query, callback);
    }
    
    public void getTasksByCategory(String categoryId, BaseRepository.RepositoryCallback<List<Task>> callback) {
        listService.getTasksByCategory(categoryId, callback);
    }
    
    public List<Task> getTasksByCategoryFromCache(String categoryId) {
        List<Task> allTasks = taskCache.getAllTasks();
        List<Task> categoryTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (categoryId.equals(task.getCategoryId())) {
                categoryTasks.add(task);
            }
        }
        return categoryTasks;
    }
    
    public void getTaskById(String taskId, BaseRepository.RepositoryCallback<Task> callback) {
        taskManager.getTaskById(taskId, callback);
    }

    public Task getTaskByIdFromCache(String taskId) {
        return taskCache.getTask(taskId);
    }
    
    public List<Task> getAllTasksFromCache() {
        return taskCache.getAllTasks();
    }
    
    public void getTasksByDate(String date, BaseRepository.RepositoryCallback<List<Task>> callback) {
        listService.getTasksByDate(date, callback);
    }
    
    public void getCompletedTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        completionService.getCompletedTasks(callback);
    }

    public List<Task> getCompletedTasksFromCache() {
        List<Task> allTasks = taskCache.getAllTasks();
        List<Task> completedTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.isCompleted()) {
                completedTasks.add(task);
            }
        }
        return completedTasks;
    }
    
    public void getUncompletedTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        completionService.getIncompleteTasks(callback);
    }

    private void updateTaskInList(Task updatedTask) {
        if (allTasks != null && updatedTask.getId() != null) {
            for (int i = 0; i < allTasks.size(); i++) {
                Task task = allTasks.get(i);
                if (updatedTask.getId().equals(task.getId())) {
                    allTasks.set(i, updatedTask);
                    break;
                }
            }
        }
    }
    
    private void cancelPendingFirebaseUpdates() {
        if (pendingFirebaseUpdate != null) {
            firebaseUpdateHandler.removeCallbacks(pendingFirebaseUpdate);
            pendingFirebaseUpdate = null;
        }
    }

    public void rescheduleAllReminders() {
        if (taskManager != null) {
        }
    }
    
    public void toggleTaskImportant(Task task) {
        task.setIsImportant(!task.isImportant());
        updateTask(task);
    }
    
    public void cleanup() {
        if (realtimeListener != null) {
            taskRepository.removeTasksListener(realtimeListener);
        }
        // Unregister from cache
        taskCache.removeListener(this);
    }

    @Override
    public void onTasksUpdated(List<Task> tasks) {
        if (listService == null) {
            listService = new TaskListService();
        }
        listService.updateTasks(tasks);

        if (listener != null) {
            listener.onTasksUpdated();
        }
        // Update widgets
        WidgetUpdateHelper.updateAllWidgets(context);
    }
    
    @Override
    public void onTaskAdded(Task task) {
        // Có thể thêm logic đặc biệt cho task mới được add
    }
    
    @Override
    public void onTaskUpdated(Task task) {
        // Có thể thêm logic đặc biệt cho task được update
    }
    
    @Override
    public void onTaskDeleted(String taskId) {
        // Có thể thêm logic đặc biệt cho task bị xóa
    }

    public List<Task> getAllTasks() {
        return taskCache.getAllTasks();
    }
    
    public List<Task> getTasksForDate(String date) {
        return taskCache.getTasksForDate(date);
    }

    public List<Task> getTodayTasks() {
        List<Task> allTasks = taskCache.getAllTasks();
        if (listService != null) {
            listService.categorizeTasks(allTasks);
            return listService.getTodayTasks();
        }
        return allTasks;
    }
    
    public List<Task> getOverdueTasks() {
        List<Task> allTasks = taskCache.getAllTasks();
        if (listService != null) {
            listService.categorizeTasks(allTasks);
            return listService.getOverdueTasks();
        }
        return allTasks;
    }
    
    public List<Task> getFutureTasks() {
        List<Task> allTasks = taskCache.getAllTasks();
        if (listService != null) {
            listService.categorizeTasks(allTasks);
            return listService.getFutureTasks();
        }
        return allTasks;
    }
    
    public List<Task> getCompletedTodayTasks() {
        List<Task> allTasks = taskCache.getAllTasks();
        if (listService != null) {
            listService.categorizeTasks(allTasks);
            return listService.getCompletedTodayTasks();
        }
        return allTasks;
    }
}
