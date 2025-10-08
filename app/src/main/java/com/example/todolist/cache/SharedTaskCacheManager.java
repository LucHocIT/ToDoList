package com.example.todolist.cache;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.todolist.manager.AuthManager;
import com.example.todolist.model.Task;
import com.example.todolist.model.TaskShare;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.service.sharing.TaskSharingService;
import com.example.todolist.service.sharing.SharedTaskSyncService;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Quản lý cache và đồng bộ cho shared tasks
 * Áp dụng pattern: Local SQLite → Cache → Firebase Sync
 */
public class SharedTaskCacheManager {
    private static final String TAG = "SharedTaskCacheManager";
    
    private static SharedTaskCacheManager instance;
    private final Map<String, Task> sharedTaskCache = new ConcurrentHashMap<>();
    private final Map<String, Task> pendingSyncTasks = new ConcurrentHashMap<>();
    private final Set<String> syncingTasks = ConcurrentHashMap.newKeySet();
    
    private TaskRepository taskRepository;
    private TaskCache taskCache;
    private AuthManager authManager;
    private SharedTaskSyncService sharedTaskSyncService;
    private TaskSharingService taskSharingService;
    private ExecutorService executor;
    private Handler mainHandler;
    
    public interface SharedTaskCacheListener {
        void onSharedTaskUpdated(Task task);
        void onSharedTaskAdded(Task task);
        void onSharedTaskDeleted(String taskId);
        void onError(String error);
    }
    
    private final Set<SharedTaskCacheListener> listeners = ConcurrentHashMap.newKeySet();
    
    private SharedTaskCacheManager() {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static SharedTaskCacheManager getInstance() {
        if (instance == null) {
            synchronized (SharedTaskCacheManager.class) {
                if (instance == null) {
                    instance = new SharedTaskCacheManager();
                }
            }
        }
        return instance;
    }
    
    public void initialize(Context context) {
        this.taskRepository = new TaskRepository(context);
        this.taskCache = TaskCache.getInstance();
        this.authManager = AuthManager.getInstance();
        this.sharedTaskSyncService = SharedTaskSyncService.getInstance();
        this.taskSharingService = TaskSharingService.getInstance();
        
        // Khởi tạo các service
        this.authManager.initialize(context);
        this.sharedTaskSyncService.initialize(context);
        this.taskSharingService.initialize(context);
        
        // Lắng nghe thay đổi từ Firebase
        setupFirebaseListeners();
    }
    
    public void addListener(SharedTaskCacheListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(SharedTaskCacheListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Cập nhật shared task với cơ chế: Local → Cache → Firebase
     */
    public void updateSharedTask(Task task, BaseRepository.DatabaseCallback<String> callback) {
        if (task == null || task.getId() == null) {
            if (callback != null) callback.onError("Invalid task data");
            return;
        }
        
        Log.d(TAG, "Updating shared task: " + task.getId());
        
        // 1. Optimistic update cache
        sharedTaskCache.put(task.getId(), task);
        notifyTaskUpdated(task);
        
        // 2. Lưu vào local SQLite
        saveToLocalDatabase(task, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // 3. Đồng bộ lên Firebase nếu có kết nối
                syncToFirebase(task, callback);
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to save to local DB, adding to pending sync: " + error);
                // Lưu vào pending sync để đồng bộ sau
                pendingSyncTasks.put(task.getId(), task);
                if (callback != null) callback.onSuccess("Task cached locally, will sync when online");
            }
        });
    }
    
    /**
     * Lưu shared task vào local SQLite
     */
    private void saveToLocalDatabase(Task task, BaseRepository.DatabaseCallback<String> callback) {
        executor.execute(() -> {
            try {
                // Đánh dấu task là shared để phân biệt với task thường
                task.setShared(true);
                
                taskRepository.updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Log.d(TAG, "Task saved to local DB: " + task.getId());
                        if (callback != null) callback.onSuccess("Saved locally");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to save to local DB: " + error);
                        if (callback != null) callback.onError(error);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Exception saving to local DB", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Đồng bộ lên Firebase
     */
    private void syncToFirebase(Task task, BaseRepository.DatabaseCallback<String> callback) {
        if (!authManager.shouldSyncToFirebase()) {
            Log.d(TAG, "Sync disabled, task kept in local cache");
            pendingSyncTasks.put(task.getId(), task);
            if (callback != null) callback.onSuccess("Task cached locally");
            return;
        }
        
        if (syncingTasks.contains(task.getId())) {
            Log.d(TAG, "Task already syncing: " + task.getId());
            return;
        }
        
        syncingTasks.add(task.getId());
        
        sharedTaskSyncService.updateSharedTask(task, new TaskSharingService.SharingCallback() {
            @Override
            public void onSuccess(String message) {
                syncingTasks.remove(task.getId());
                pendingSyncTasks.remove(task.getId());
                Log.d(TAG, "Task synced to Firebase: " + task.getId());
                
                // Cập nhật TaskCache chính để UI được refresh
                taskCache.updateTaskOptimistic(task);
                
                if (callback != null) callback.onSuccess("Task synced successfully");
            }
            
            @Override
            public void onError(String error) {
                syncingTasks.remove(task.getId());
                Log.w(TAG, "Failed to sync to Firebase: " + error);
                
                // Giữ trong pending sync để thử lại sau
                pendingSyncTasks.put(task.getId(), task);
                
                if (callback != null) callback.onSuccess("Task cached locally, will retry sync");
            }
        });
    }
    
    /**
     * Load shared task từ cache hoặc Firebase
     */
    public void loadSharedTask(String taskId, BaseRepository.DatabaseCallback<Task> callback) {
        // Kiểm tra cache trước
        Task cachedTask = sharedTaskCache.get(taskId);
        if (cachedTask != null) {
            if (callback != null) callback.onSuccess(cachedTask);
            return;
        }

        // Load từ Firebase
        sharedTaskSyncService.loadSharedTask(taskId, new SharedTaskSyncService.SharedTaskCallback() {
            @Override
            public void onTaskLoaded(Task task) {
                // Lưu vào cache
                sharedTaskCache.put(taskId, task);
                // Cập nhật TaskCache chính
                taskCache.updateTaskOptimistic(task);
                notifyTaskUpdated(task);

                // Khởi tạo listener real-time cho shared task
                sharedTaskSyncService.startListeningForTaskUpdates(taskId);

                if (callback != null) callback.onSuccess(task);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load shared task: " + error);
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    /**
     * Đồng bộ tất cả pending tasks khi có mạng
     */
    public void syncPendingTasks() {
        if (!authManager.shouldSyncToFirebase() || pendingSyncTasks.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Syncing " + pendingSyncTasks.size() + " pending shared tasks");
        
        List<Task> tasksToSync = new ArrayList<>(pendingSyncTasks.values());
        for (Task task : tasksToSync) {
            syncToFirebase(task, new BaseRepository.DatabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.d(TAG, "Pending task synced: " + task.getId());
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "Failed to sync pending task: " + task.getId());
                }
            });
        }
    }
    
    /**
     * Thiết lập listeners cho Firebase để nhận updates real-time
     */
    private void setupFirebaseListeners() {
        sharedTaskSyncService.addUpdateListener(new SharedTaskSyncService.SharedTaskUpdateListener() {
            @Override
            public void onSharedTaskUpdated(Task task) {
                Log.d(TAG, "Received shared task update from Firebase: " + task.getId());
                
                // Cập nhật cache
                sharedTaskCache.put(task.getId(), task);
                
                // Cập nhật TaskCache chính để UI được refresh
                taskCache.updateTaskOptimistic(task);
                
                // Lưu vào local DB
                saveToLocalDatabase(task, null);
                
                // Thông báo listeners
                notifyTaskUpdated(task);
            }
            
            @Override
            public void onSubTaskUpdated(String taskId, com.example.todolist.model.SubTask subTask) {
                // Load lại task để có subtask mới nhất
                loadSharedTask(taskId, null);
            }
            
            @Override
            public void onTaskSharingChanged(TaskShare taskShare) {
                Log.d(TAG, "Task sharing changed: " + taskShare.getTaskId());
                // Load lại task để cập nhật trạng thái sharing
                loadSharedTask(taskShare.getTaskId(), null);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Firebase listener error: " + error);
                notifyError(error);
            }
        });
    }
    
    /**
     * Kiểm tra task có phải shared task không
     */
    public boolean isSharedTask(String taskId) {
        return sharedTaskCache.containsKey(taskId);
    }
    
    /**
     * Lấy tất cả shared tasks trong cache
     */
    public List<Task> getAllSharedTasks() {
        return new ArrayList<>(sharedTaskCache.values());
    }
    
    /**
     * Xóa shared task khỏi cache
     */
    public void removeSharedTask(String taskId) {
        sharedTaskCache.remove(taskId);
        pendingSyncTasks.remove(taskId);
        taskCache.deleteTaskOptimistic(taskId);
        notifyTaskDeleted(taskId);
    }
    
    // Notification methods
    private void notifyTaskUpdated(Task task) {
        mainHandler.post(() -> {
            for (SharedTaskCacheListener listener : listeners) {
                try {
                    listener.onSharedTaskUpdated(task);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener", e);
                }
            }
        });
    }
    
    private void notifyTaskAdded(Task task) {
        mainHandler.post(() -> {
            for (SharedTaskCacheListener listener : listeners) {
                try {
                    listener.onSharedTaskAdded(task);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener", e);
                }
            }
        });
    }
    
    private void notifyTaskDeleted(String taskId) {
        mainHandler.post(() -> {
            for (SharedTaskCacheListener listener : listeners) {
                try {
                    listener.onSharedTaskDeleted(taskId);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener", e);
                }
            }
        });
    }
    
    private void notifyError(String error) {
        mainHandler.post(() -> {
            for (SharedTaskCacheListener listener : listeners) {
                try {
                    listener.onError(error);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener", e);
                }
            }
        });
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executor != null) {
            executor.shutdown();
        }
        listeners.clear();
        sharedTaskCache.clear();
        pendingSyncTasks.clear();
        syncingTasks.clear();
    }
}