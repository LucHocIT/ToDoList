package com.example.todolist.service;

import android.content.Context;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;

import com.example.todolist.cache.TaskCache;
import com.example.todolist.manager.AuthManager;
import com.example.todolist.manager.FirebaseSyncManager;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.service.task.*;
import com.example.todolist.widget.WidgetUpdateHelper;

import java.util.ArrayList;
import java.util.List;

public class TaskService implements TaskCache.TaskCacheListener, TaskRepeatService.TaskCreator {
    
    public interface TaskUpdateListener {
        void onTasksUpdated();
        void onError(String error);
    }

    public interface TaskOperationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    // Core dependencies
    private Context context;
    private TaskRepository taskRepository;
    private TaskUpdateListener listener;
    private TaskCache taskCache;
    
    // Service delegates
    private TaskManager taskManager;
    private TaskCompletionService completionService;
    private TaskListService listService;
    private SubTaskService subTaskService;
    private TaskSyncService syncService;
    private TaskFirebaseUpdateService firebaseUpdateService;
    
    // Firebase sync management
    private AuthManager authManager;
    private FirebaseSyncManager firebaseSyncManager; 

    public TaskService(Context context, TaskUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.taskRepository = new TaskRepository(context);
        this.taskCache = TaskCache.getInstance();
        taskCache.addListener(this);

        this.taskManager = new TaskManager(context);
        this.completionService = new TaskCompletionService(context);
        this.listService = new TaskListService(context);
        this.subTaskService = new SubTaskService(context);
        this.syncService = new TaskSyncService(context);
        this.firebaseUpdateService = new TaskFirebaseUpdateService();
        this.subTaskService.setTaskService(this);

        this.authManager = AuthManager.getInstance();
        this.authManager.initialize(context);
        this.firebaseSyncManager = FirebaseSyncManager.getInstance();
        this.firebaseSyncManager.initialize(context);
    }

    public void loadTasks() {
        if (taskCache.isInitialized()) {
            notifyListener();
            return;
        }
        
        taskCache.setLoading(true);
        taskRepository.getAllTasks(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                taskCache.loadFromFirebase(tasks);
                taskCache.setLoading(false);
                notifyListener();

                if (authManager.shouldSyncToFirebase()) {
                    syncService.loadAndMergeFromFirebase(() -> notifyListener());
                }
            }

            @Override
            public void onError(String error) {
                taskCache.setLoading(false);
                notifyError("Lỗi tải tasks: " + error);
            }
        });
    }

    public void syncAllTasksToFirebase(FirebaseSyncManager.SyncCallback callback) {
        syncService.syncAllTasksToFirebase(callback);
    }

    // CRUD Operations - delegated to TaskSyncService
    public void addTask(Task task) {
        addTask(task, null);
    }
    
    public void addTask(Task task, TaskOperationCallback callback) {
        ensureTaskId(task);
        syncService.performTaskOperation(task, TaskSyncService.TaskOperation.ADD, 
            createOperationCallback(callback, task, () -> handleRepeatTaskCreation(task, callback)));
    }
    
    @Override
    public void addTaskWithoutRepeat(Task task, BaseRepository.DatabaseCallback<String> callback) {
        ensureTaskId(task);
        syncService.performTaskOperation(task, TaskSyncService.TaskOperation.ADD, callback);
    }
    
    public void addTaskWithoutRepeat(Task task) {
        addTaskWithoutRepeat(task, null);
    }

    public void updateTask(Task task) {
        updateTask(task, null);
    }
    
    public void updateTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        android.util.Log.d("TaskService", "updateTask: task.id=" + task.getId() + ", has " + (task.getSubTasks() != null ? task.getSubTasks().size() : 0) + " subtasks");
        if (task.getSubTasks() != null) {
            for (int i = 0; i < task.getSubTasks().size(); i++) {
                com.example.todolist.model.SubTask st = task.getSubTasks().get(i);
                android.util.Log.d("TaskService", "updateTask: subtask[" + i + "] = title:'" + st.getTitle() + "', completed:" + st.isCompleted() + ", id:" + st.getId());
            }
        }
        
        Task cachedTask = taskCache.getTask(task.getId());
        if (cachedTask != null && cachedTask.getSubTasks() != null && !cachedTask.getSubTasks().isEmpty()) {
            android.util.Log.d("TaskService", "updateTask: found cached task with " + cachedTask.getSubTasks().size() + " subtasks");
            // If the task being updated doesn't have SubTasks but the cached one does, preserve them
            if (task.getSubTasks() == null || task.getSubTasks().isEmpty()) {
                android.util.Log.d("TaskService", "updateTask: preserving cached subtasks");
                task.setSubTasks(cachedTask.getSubTasks());
            }
        } else if (task.getSubTasks() == null || task.getSubTasks().isEmpty()) {
            android.util.Log.d("TaskService", "updateTask: loading subtasks from database");
            loadSubTasksForTaskSync(task);
        }
        
        android.util.Log.d("TaskService", "updateTask: final task has " + (task.getSubTasks() != null ? task.getSubTasks().size() : 0) + " subtasks before sync");
        syncService.performTaskOperation(task, TaskSyncService.TaskOperation.UPDATE, 
            convertBooleanCallback(callback));
    }
    
    private void loadSubTasksForTaskSync(Task task) {
        try {
            subTaskService.getSubTasks(task.getId(), new BaseRepository.ListCallback<com.example.todolist.model.SubTask>() {
                @Override
                public void onSuccess(List<com.example.todolist.model.SubTask> subTasks) {
                    task.setSubTasks(subTasks);
                }

                @Override
                public void onError(String error) {
                    Log.w("TaskService", "Failed to load subtasks for task " + task.getId() + ": " + error);
                }
            });
        } catch (Exception e) {
            Log.w("TaskService", "Exception loading subtasks: " + e.getMessage());
        }
    }

    public void deleteTask(Task task) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa task này?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteTask(task, null))
                .setNegativeButton("Hủy", null)
                .show();
    }

    public void deleteTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        syncService.performTaskOperation(task, TaskSyncService.TaskOperation.DELETE,
            convertBooleanCallback(callback));
    }

    // Task Completion - delegated to TaskCompletionService
    public void completeTask(Task task, boolean isCompleted) {
        firebaseUpdateService.markLocalUpdate();
        
        // Handle repeating tasks
        if (isCompleted && task.isRepeating() && task.getRepeatType() != null && !task.getRepeatType().equals("Không")) {
            TaskRepeatService.updateTaskForNextRepeat(task);
            updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    notifyListener();
                }

                @Override
                public void onError(String error) {
                    notifyError("Lỗi cập nhật task lặp lại: " + error);
                }
            });
            return;
        }
        
        // Handle subtasks completion
        if (isCompleted && task.getSubTasks() != null && !task.getSubTasks().isEmpty()) {
            subTaskService.completeAllSubTasks(task);
        }
        
        completionService.completeTask(task, isCompleted, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                syncService.performTaskOperation(task, TaskSyncService.TaskOperation.UPDATE,
                    convertBooleanCallback(new BaseRepository.DatabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean syncResult) {
                            notifyListener();
                        }

                        @Override
                        public void onError(String syncError) {
                            Log.w("TaskService", "Firebase sync failed for task completion: " + syncError);
                            notifyListener(); 
                        }
                    }));
            }

            @Override
            public void onError(String error) {
                rollbackTaskCompletion(task, isCompleted);
                notifyError("Lỗi cập nhật trạng thái task: " + error);
            }
        });
    }

    public void toggleTaskCompletion(Task task) {
        completeTask(task, !task.isCompleted());
    }
    
    public void toggleTaskImportance(Task task) {
        completionService.toggleTaskImportance(task, null);
    }
    
    public void toggleTaskImportant(Task task) {
        task.setIsImportant(!task.isImportant());
        updateTask(task);
    }
    
    // Query Operations - mostly delegated to appropriate services
    public List<Task> getIncompleteTasks() { 
        return filterTasks(task -> !task.isCompleted());
    }
    
    public List<Task> getCompletedTasksFromCache() {
        return filterTasks(Task::isCompleted);
    }
    
    public List<Task> getTasksByCategoryFromCache(String categoryId) {
        return filterTasks(task -> categoryId.equals(task.getCategoryId()));
    }
    
    private List<Task> filterTasks(java.util.function.Predicate<Task> predicate) {
        return taskCache.getAllTasks().stream()
                .filter(predicate)
                .collect(java.util.stream.Collectors.toList());
    }
    
    // Async query operations
    public void getAllTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        taskRepository.getAllTasks(createListCallback(callback));
    }
    
    public void searchTasks(String query, BaseRepository.RepositoryCallback<List<Task>> callback) {
        taskManager.searchTasks(query, callback);
    }
    
    public void getTasksByCategory(String categoryId, BaseRepository.RepositoryCallback<List<Task>> callback) {
        listService.getTasksByCategory(categoryId, callback);
    }
    
    public void getTaskById(String taskId, BaseRepository.RepositoryCallback<Task> callback) {
        taskManager.getTaskById(taskId, callback);
    }
    
    public void getTasksByDate(String date, BaseRepository.RepositoryCallback<List<Task>> callback) {
        listService.getTasksByDate(date, callback);
    }
    
    public void getCompletedTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        completionService.getCompletedTasks(callback);
    }
    
    public void getUncompletedTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        completionService.getIncompleteTasks(callback);
    }

    // Cache-based getters
    public Task getTaskByIdFromCache(String taskId) { return taskCache.getTask(taskId); }
    public List<Task> getAllTasksFromCache() { return taskCache.getAllTasks(); }
    public List<Task> getAllTasks() { return taskCache.getAllTasks(); }
    public List<Task> getTasksForDate(String date) { return taskCache.getTasksForDate(date); }

    // Task List Category Operations - delegated to TaskListService
    public List<Task> getTodayTasks() { return getCategorizedTasks(listService::getTodayTasks); }
    public List<Task> getOverdueTasks() { return getCategorizedTasks(listService::getOverdueTasks); }
    public List<Task> getFutureTasks() { return getCategorizedTasks(listService::getFutureTasks); }
    public List<Task> getCompletedTodayTasks() { return getCategorizedTasks(listService::getCompletedTodayTasks); }
    
    private List<Task> getCategorizedTasks(java.util.function.Supplier<List<Task>> supplier) {
        List<Task> allTasks = taskCache.getAllTasks();
        listService.categorizeTasks(allTasks);
        return supplier.get();
    }
    
    // SubTask Operations - delegated to SubTaskService
    public void loadSubTasksForTask(String taskId, TaskOperationCallback callback) {
        subTaskService.getSubTasks(taskId, new BaseRepository.ListCallback<com.example.todolist.model.SubTask>() {
            @Override
            public void onSuccess(List<com.example.todolist.model.SubTask> subTasks) {
                // Update subtasks for task in cache
                List<Task> cachedTasks = taskCache.getAllTasks();
                for (Task task : cachedTasks) {
                    if (task.getId().equals(taskId)) {
                        task.setSubTasks(subTasks);
                        break;
                    }
                }
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    public void loadSubTasksForAllTasks() {
        List<Task> allTasks = taskCache.getAllTasks();
        for (Task task : allTasks) {
            loadSubTasksForTask(task.getId(), null);
        }
    }

    // Utility and lifecycle methods
    public void rescheduleAllReminders() {
        // Delegate to TaskManager if needed
    }
    
    public void cleanup() {
        taskCache.removeListener(this);
        firebaseUpdateService.cancelPendingFirebaseUpdates();
    }

    // TaskCache.TaskCacheListener implementation
    @Override
    public void onTasksUpdated(List<Task> tasks) {
        listService.updateTasks(tasks);
        notifyListener();
        WidgetUpdateHelper.updateAllWidgets(context);
    }
    
    @Override
    public void onTaskAdded(Task task) {
        // Can add special logic for newly added tasks
    }
    
    @Override
    public void onTaskUpdated(Task task) {
        // Can add special logic for updated tasks
    }
    
    @Override
    public void onTaskDeleted(String taskId) {
        // Can add special logic for deleted tasks
    }

    // Helper methods
    private void ensureTaskId(Task task) {
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(String.valueOf(System.currentTimeMillis()) + "_" + Math.random());
        }
    }
    
    private BaseRepository.ListCallback<Task> createListCallback(BaseRepository.RepositoryCallback<List<Task>> callback) {
        return new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) { callback.onSuccess(tasks); }
            
            @Override
            public void onError(String error) { callback.onError(error); }
        };
    }
    
    private BaseRepository.DatabaseCallback<String> convertBooleanCallback(BaseRepository.DatabaseCallback<Boolean> callback) {
        return callback != null ? new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) { callback.onSuccess(true); }

            @Override
            public void onError(String error) { callback.onError(error); }
        } : null;
    }
    
    private BaseRepository.DatabaseCallback<String> createOperationCallback(
            TaskOperationCallback callback, Task task, Runnable additionalAction) {
        return new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (additionalAction != null) {
                    additionalAction.run();
                } else if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        };
    }
    
    private void handleRepeatTaskCreation(Task task, TaskOperationCallback callback) {
        if (TaskRepeatService.needsRepeatInstances(task)) {
            TaskRepeatService.createRepeatInstances(task, this, new TaskRepeatService.RepeatTaskCallback() {
                @Override
                public void onSuccess() { if (callback != null) callback.onSuccess(); }

                @Override
                public void onError(String error) {
                    android.util.Log.e("TaskService", "Error creating repeat instances: " + error);
                    if (callback != null) callback.onSuccess();
                }
            });
        } else {
            if (callback != null) callback.onSuccess();
        }
    }
    
    private void rollbackTaskCompletion(Task task, boolean isCompleted) {
        task.setIsCompleted(!isCompleted);
        task.setCompletionDate(isCompleted ? null : String.valueOf(System.currentTimeMillis()));
        if (isCompleted && task.getSubTasks() != null) {
            task.getSubTasks().forEach(subTask -> subTask.setCompleted(false));
        }
        notifyListener();
    }
    
    private void notifyListener() { if (listener != null) listener.onTasksUpdated(); }
    private void notifyError(String error) { if (listener != null) listener.onError(error); }
}
