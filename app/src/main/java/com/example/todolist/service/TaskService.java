package com.example.todolist.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;

import com.example.todolist.cache.TaskCache;
import com.example.todolist.manager.AuthManager;
import com.example.todolist.manager.FirebaseSyncManager;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.service.task.TaskManager;
import com.example.todolist.service.task.TaskCompletionService;
import com.example.todolist.service.task.TaskListService;
import com.example.todolist.service.task.SubTaskService;
import com.example.todolist.widget.WidgetUpdateHelper;

import java.util.ArrayList;
import java.util.List;

public class TaskService implements TaskCache.TaskCacheListener, com.example.todolist.service.task.TaskRepeatService.TaskCreator {
    
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
    private TaskManager taskManager;
    private TaskCompletionService completionService;
    private TaskListService listService;
    private SubTaskService subTaskService;
    private TaskCache taskCache;
    private Handler firebaseUpdateHandler;
    private List<Task> allTasks;
    private Runnable pendingFirebaseUpdate;
    private long lastLocalUpdateTime;
    private static final long LOCAL_UPDATE_PRIORITY_WINDOW = 1000;
    private static final long FIREBASE_UPDATE_DELAY = 500;
    
    // Firebase sync management
    private AuthManager authManager;
    private FirebaseSyncManager firebaseSyncManager; 

    public TaskService(Context context, TaskUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.taskRepository = new TaskRepository(context); // Pass context for SQLite
        this.taskCache = TaskCache.getInstance();
        taskCache.addListener(this);
        this.firebaseUpdateHandler = new Handler(Looper.getMainLooper());
        
        this.taskManager = new TaskManager(context);
        this.completionService = new TaskCompletionService(context);
        this.listService = new TaskListService(context);
        this.subTaskService = new SubTaskService(context);
        
        // Initialize Firebase sync management
        this.authManager = AuthManager.getInstance();
        this.authManager.initialize(context);
        this.firebaseSyncManager = FirebaseSyncManager.getInstance();
        this.firebaseSyncManager.initialize(context);
        
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
        
        // Load tasks from SQLite database first
        taskRepository.getAllTasks(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                taskCache.loadFromFirebase(tasks); // Keep using cache for UI consistency
                taskCache.setLoading(false);
                if (listener != null) {
                    listener.onTasksUpdated();
                }
                
                // Nếu đã login và bật sync, tải thêm từ Firebase để merge
                if (authManager.shouldSyncToFirebase()) {
                    loadAndMergeFromFirebase();
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
    
    private void loadAndMergeFromFirebase() {
        firebaseSyncManager.loadTasksFromFirebase(new FirebaseSyncManager.FirebaseSyncCallback() {
            @Override
            public void onSuccess(List<Task> firebaseTasks) {
                // Merge Firebase tasks with local cache
                // Simple merge: add Firebase tasks that don't exist locally
                List<Task> localTasks = taskCache.getAllTasks();
                boolean hasNewTasks = false;
                
                for (Task firebaseTask : firebaseTasks) {
                    boolean existsLocally = false;
                    for (Task localTask : localTasks) {
                        if (localTask.getId().equals(firebaseTask.getId())) {
                            existsLocally = true;
                            break;
                        }
                    }
                    
                    if (!existsLocally) {
                        // Add to local database and cache
                        taskRepository.addTask(firebaseTask, null);
                        taskCache.addTaskOptimistic(firebaseTask);
                        hasNewTasks = true;
                    }
                }
                
                if (hasNewTasks && listener != null) {
                    listener.onTasksUpdated();
                }
            }

            @Override
            public void onError(String error) {
                android.util.Log.w("TaskService", "Failed to load tasks from Firebase: " + error);
            }
        });
    }
    
    // Method để đồng bộ tất cả local tasks lên Firebase khi user bật sync
    public void syncAllTasksToFirebase(FirebaseSyncManager.SyncCallback callback) {
        if (!authManager.shouldSyncToFirebase()) {
            if (callback != null) callback.onError("Sync is disabled or user not logged in");
            return;
        }
        
        List<Task> allLocalTasks = taskCache.getAllTasks();
        firebaseSyncManager.syncAllTasksToFirebase(allLocalTasks, callback);
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

        // Luôn lưu vào cache trước (optimistic UI)
        taskCache.addTaskOptimistic(task);
        
        // Luôn lưu vào SQLite local database
        taskRepository.addTask(task, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String localTaskId) {
                // Nếu đã login và bật sync thì đồng bộ lên Firebase
                if (authManager.shouldSyncToFirebase()) {
                    firebaseSyncManager.addTaskToFirebase(task, new BaseRepository.DatabaseCallback<String>() {
                        @Override
                        public void onSuccess(String firebaseTaskId) {
                            // Cập nhật ID nếu Firebase tạo ID khác
                            if (!task.getId().equals(firebaseTaskId)) {
                                taskCache.deleteTaskOptimistic(task.getId());
                                task.setId(firebaseTaskId);
                                taskCache.addTaskOptimistic(task);
                                
                                // Cập nhật lại trong SQLite với Firebase ID
                                taskRepository.updateTask(task, null);
                            }
                            
                            handleRepeatTaskCreation(task, callback);
                        }

                        @Override
                        public void onError(String error) {
                            // Firebase thất bại nhưng SQLite đã thành công - vẫn OK
                            android.util.Log.w("TaskService", "Firebase sync failed but local save succeeded: " + error);
                            handleRepeatTaskCreation(task, callback);
                        }
                    });
                } else {
                    // Chưa login hoặc chưa bật sync - chỉ lưu local
                    handleRepeatTaskCreation(task, callback);
                }
            }

            @Override
            public void onError(String error) {
                // Rollback optimistic update nếu SQLite fail
                taskCache.deleteTaskOptimistic(task.getId());
                if (callback != null) callback.onError("Local save failed: " + error);
            }
        });
    }
    
    private void handleRepeatTaskCreation(Task task, TaskOperationCallback callback) {
        // Tạo repeat instances nếu cần
        if (com.example.todolist.service.task.TaskRepeatService.needsRepeatInstances(task)) {
            com.example.todolist.service.task.TaskRepeatService.createRepeatInstances(task, TaskService.this, new com.example.todolist.service.task.TaskRepeatService.RepeatTaskCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String error) {
                    // Log error nhưng vẫn báo success cho task chính
                    android.util.Log.e("TaskService", "Error creating repeat instances: " + error);
                    if (callback != null) callback.onSuccess();
                }
            });
        } else {
            if (callback != null) callback.onSuccess();
        }
    }
    
    public void addTaskWithoutRepeat(Task task) {
        addTaskWithoutRepeat(task, null);
    }
    
    @Override
    public void addTaskWithoutRepeat(Task task, BaseRepository.DatabaseCallback<String> callback) {
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(String.valueOf(System.currentTimeMillis()) + "_" + Math.random());
        }
        
        // Luôn lưu vào cache trước (optimistic UI)
        taskCache.addTaskOptimistic(task);
        
        // Luôn lưu vào SQLite local database
        taskRepository.addTask(task, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String localTaskId) {
                // Nếu đã login và bật sync thì đồng bộ lên Firebase
                if (authManager.shouldSyncToFirebase()) {
                    firebaseSyncManager.addTaskToFirebase(task, new BaseRepository.DatabaseCallback<String>() {
                        @Override
                        public void onSuccess(String firebaseTaskId) {
                            // Cập nhật ID nếu Firebase tạo ID khác
                            if (!task.getId().equals(firebaseTaskId)) {
                                taskCache.deleteTaskOptimistic(task.getId());
                                task.setId(firebaseTaskId);
                                taskCache.addTaskOptimistic(task);
                                
                                // Cập nhật lại trong SQLite với Firebase ID
                                taskRepository.updateTask(task, null);
                            }
                            if (callback != null) callback.onSuccess(firebaseTaskId);
                        }

                        @Override
                        public void onError(String error) {
                            // Firebase thất bại nhưng SQLite đã thành công - vẫn OK
                            android.util.Log.w("TaskService", "Firebase sync failed but local save succeeded: " + error);
                            if (callback != null) callback.onSuccess(task.getId());
                        }
                    });
                } else {
                    // Chưa login hoặc chưa bật sync - chỉ lưu local
                    if (callback != null) callback.onSuccess(task.getId());
                }
            }

            @Override
            public void onError(String error) {
                // Rollback optimistic update nếu SQLite fail
                taskCache.deleteTaskOptimistic(task.getId());
                if (callback != null) callback.onError("Local save failed: " + error);
            }
        });
    }

    public void updateTask(Task task) {
        updateTask(task, null);
    }
    
    public void updateTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) { 
        // Luôn cập nhật cache trước (optimistic UI)
        taskCache.updateTaskOptimistic(task);
        
        // Luôn cập nhật SQLite local database
        taskRepository.updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                // Nếu đã login và bật sync thì đồng bộ lên Firebase
                if (authManager.shouldSyncToFirebase()) {
                    firebaseSyncManager.updateTaskInFirebase(task, new BaseRepository.DatabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean firebaseResult) {
                            if (callback != null) callback.onSuccess(result);
                        }

                        @Override
                        public void onError(String error) {
                            // Firebase thất bại nhưng SQLite đã thành công - vẫn OK
                            android.util.Log.w("TaskService", "Firebase update failed but local update succeeded: " + error);
                            if (callback != null) callback.onSuccess(result);
                        }
                    });
                } else {
                    // Chưa login hoặc chưa bật sync - chỉ update local
                    if (callback != null) callback.onSuccess(result);
                }
            }

            @Override
            public void onError(String error) {
                // Rollback optimistic update nếu SQLite fail
                // TODO: Load original task from database and restore cache
                if (callback != null) callback.onError("Local update failed: " + error);
            }
        });
    }

    public void deleteTask(Task task) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa task này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteTask(task, null);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    public void deleteTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        // Luôn xóa khỏi cache trước (optimistic UI)
        taskCache.deleteTaskOptimistic(task.getId());
        
        // Luôn xóa khỏi SQLite local database
        taskRepository.deleteTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                // Nếu đã login và bật sync thì xóa khỏi Firebase
                if (authManager.shouldSyncToFirebase()) {
                    firebaseSyncManager.deleteTaskFromFirebase(task.getId(), new BaseRepository.DatabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean firebaseResult) {
                            if (callback != null) callback.onSuccess(result);
                        }

                        @Override
                        public void onError(String error) {
                            // Firebase thất bại nhưng SQLite đã thành công - vẫn OK
                            android.util.Log.w("TaskService", "Firebase delete failed but local delete succeeded: " + error);
                            if (callback != null) callback.onSuccess(result);
                        }
                    });
                } else {
                    // Chưa login hoặc chưa bật sync - chỉ xóa local
                    if (callback != null) callback.onSuccess(result);
                }
            }

            @Override
            public void onError(String error) {
                // Rollback optimistic update nếu SQLite fail
                taskCache.addTaskOptimistic(task);
                if (callback != null) callback.onError("Local delete failed: " + error);
                if (listener != null) {
                    listener.onError("Lỗi xóa task: " + error);
                }
            }
        });
    }

    public void completeTask(Task task, boolean isCompleted) {
        lastLocalUpdateTime = System.currentTimeMillis();
        cancelPendingFirebaseUpdates();
        
        // Xử lý task lặp lại khi hoàn thành
        if (isCompleted && task.isRepeating() && task.getRepeatType() != null && !task.getRepeatType().equals("Không")) {
            // Cập nhật ngày đến hạn cho chu kỳ tiếp theo thay vì đánh dấu hoàn thành
            com.example.todolist.service.task.TaskRepeatService.updateTaskForNextRepeat(task);
            
            // Cập nhật task với ngày mới
            updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    updateTaskInList(task);
                    listService.categorizeTasks(allTasks);
                    notifyListener();
                }

                @Override
                public void onError(String error) {
                    if (listener != null) {
                        listener.onError("Lỗi cập nhật task lặp lại: " + error);
                    }
                }
            });
            return;
        }
        
        // Xử lý subtasks khi hoàn thành task bình thường
        if (isCompleted && task.getSubTasks() != null && !task.getSubTasks().isEmpty()) {
            for (com.example.todolist.model.SubTask subTask : task.getSubTasks()) {
                if (!subTask.isCompleted()) {
                    subTask.setCompleted(true);
                    subTaskService.updateSubTask(task.getId(), subTask, null);
                }
            }
        }
        
        completionService.completeTask(task, isCompleted, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                updateTaskInList(task);
                listService.categorizeTasks(allTasks);
                notifyListener();
            }

            @Override
            public void onError(String error) {
                task.setIsCompleted(!isCompleted);
                task.setCompletionDate(isCompleted ? null : String.valueOf(System.currentTimeMillis()));
                if (isCompleted && task.getSubTasks() != null) {
                    for (com.example.todolist.model.SubTask subTask : task.getSubTasks()) {
                        subTask.setCompleted(false);
                    }
                }
                
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
        taskRepository.getAllTasks(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                callback.onSuccess(tasks);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
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
        List<Task> tasks = taskCache.getAllTasks();
        return tasks;
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
    
    public void loadSubTasksForTask(String taskId, TaskOperationCallback callback) {
        subTaskService.getSubTasks(taskId, new BaseRepository.ListCallback<com.example.todolist.model.SubTask>() {
            @Override
            public void onSuccess(List<com.example.todolist.model.SubTask> subTasks) {
                // Cập nhật subtasks cho task trong cache
                List<Task> cachedTasks = taskCache.getAllTasks();
                for (Task task : cachedTasks) {
                    if (task.getId().equals(taskId)) {
                        task.setSubTasks(subTasks);
                        break;
                    }
                }
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    public void loadSubTasksForAllTasks() {
        List<Task> allTasks = taskCache.getAllTasks();
        for (Task task : allTasks) {
            loadSubTasksForTask(task.getId(), null);
        }
    }
}
