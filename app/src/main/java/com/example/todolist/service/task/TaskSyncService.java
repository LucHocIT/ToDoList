package com.example.todolist.service.task;

import android.content.Context;
import com.example.todolist.cache.TaskCache;
import com.example.todolist.manager.AuthManager;
import com.example.todolist.manager.FirebaseSyncManager;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;

import java.util.List;

public class TaskSyncService {
    
    private TaskRepository taskRepository;
    private TaskCache taskCache;
    private AuthManager authManager;
    private FirebaseSyncManager firebaseSyncManager;
    
    public TaskSyncService(Context context) {
        this.taskRepository = new TaskRepository(context);
        this.taskCache = TaskCache.getInstance();
        this.authManager = AuthManager.getInstance();
        this.firebaseSyncManager = FirebaseSyncManager.getInstance();
    }

    public void performTaskOperation(Task task, TaskOperation operation, BaseRepository.DatabaseCallback<String> callback) {
        // Optimistic UI update
        applyOptimisticUpdate(task, operation);
        
        // Lưu vào SQLite
        performLocalOperation(task, operation, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Sync với Firebase nếu cần
                if (authManager.shouldSyncToFirebase()) {
                    performFirebaseOperation(task, operation, new BaseRepository.DatabaseCallback<String>() {
                        @Override
                        public void onSuccess(String firebaseResult) {
                            handleFirebaseSuccess(task, firebaseResult, callback);
                        }

                        @Override
                        public void onError(String error) {
                            android.util.Log.w("TaskSyncService", "Firebase sync failed: " + error);
                            if (callback != null) callback.onSuccess(result);
                        }
                    });
                } else {
                    if (callback != null) callback.onSuccess(result);
                }
            }

            @Override
            public void onError(String error) {
                rollbackOptimisticUpdate(task, operation);
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void loadAndMergeFromFirebase(Runnable onComplete) {
        firebaseSyncManager.loadTasksFromFirebase(new FirebaseSyncManager.FirebaseSyncCallback() {
            @Override
            public void onSuccess(List<Task> firebaseTasks) {
                mergeFirebaseTasksWithLocal(firebaseTasks);
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onError(String error) {
                android.util.Log.w("TaskSyncService", "Failed to load from Firebase: " + error);
                if (onComplete != null) onComplete.run();
            }
        });
    }

    public void syncAllTasksToFirebase(FirebaseSyncManager.SyncCallback callback) {
        if (!authManager.shouldSyncToFirebase()) {
            if (callback != null) callback.onError("Sync is disabled");
            return;
        }
        
        List<Task> allLocalTasks = taskCache.getAllTasks();
        firebaseSyncManager.syncAllTasksToFirebase(allLocalTasks, callback);
    }
    
    private void applyOptimisticUpdate(Task task, TaskOperation operation) {
        switch (operation) {
            case ADD:
                taskCache.addTaskOptimistic(task);
                break;
            case UPDATE:
                taskCache.updateTaskOptimistic(task);
                break;
            case DELETE:
                taskCache.deleteTaskOptimistic(task.getId());
                break;
        }
    }
    
    private void rollbackOptimisticUpdate(Task task, TaskOperation operation) {
        switch (operation) {
            case ADD:
                taskCache.deleteTaskOptimistic(task.getId());
                break;
            case UPDATE:
                // TODO: Load original task and restore
                break;
            case DELETE:
                taskCache.addTaskOptimistic(task);
                break;
        }
    }
    
    private void performLocalOperation(Task task, TaskOperation operation, BaseRepository.DatabaseCallback<String> callback) {
        switch (operation) {
            case ADD:
                taskRepository.addTask(task, callback);
                break;
            case UPDATE:
                taskRepository.updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (callback != null) callback.onSuccess(task.getId());
                    }

                    @Override
                    public void onError(String error) {
                        if (callback != null) callback.onError(error);
                    }
                });
                break;
            case DELETE:
                taskRepository.deleteTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (callback != null) callback.onSuccess(task.getId());
                    }

                    @Override
                    public void onError(String error) {
                        if (callback != null) callback.onError(error);
                    }
                });
                break;
        }
    }
    
    private void performFirebaseOperation(Task task, TaskOperation operation, BaseRepository.DatabaseCallback<String> callback) {
        switch (operation) {
            case ADD:
                firebaseSyncManager.addTaskToFirebase(task, callback);
                break;
            case UPDATE:
                firebaseSyncManager.updateTaskInFirebase(task, new BaseRepository.DatabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (callback != null) callback.onSuccess(task.getId());
                    }

                    @Override
                    public void onError(String error) {
                        if (callback != null) callback.onError(error);
                    }
                });
                break;
            case DELETE:
                firebaseSyncManager.deleteTaskFromFirebase(task.getId(), new BaseRepository.DatabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (callback != null) callback.onSuccess(task.getId());
                    }

                    @Override
                    public void onError(String error) {
                        if (callback != null) callback.onError(error);
                    }
                });
                break;
        }
    }
    
    private void handleFirebaseSuccess(Task task, String firebaseResult, BaseRepository.DatabaseCallback<String> callback) {
        if (!task.getId().equals(firebaseResult)) {
            taskCache.deleteTaskOptimistic(task.getId());
            task.setId(firebaseResult);
            taskCache.addTaskOptimistic(task);
            taskRepository.updateTask(task, null);
        }
        if (callback != null) callback.onSuccess(firebaseResult);
    }
    
    private void mergeFirebaseTasksWithLocal(List<Task> firebaseTasks) {
        List<Task> localTasks = taskCache.getAllTasks();
        boolean hasNewTasks = false;
        
        for (Task firebaseTask : firebaseTasks) {
            boolean existsLocally = localTasks.stream()
                    .anyMatch(localTask -> localTask.getId().equals(firebaseTask.getId()));
            
            if (!existsLocally) {
                taskRepository.addTask(firebaseTask, null);
                taskCache.addTaskOptimistic(firebaseTask);
                hasNewTasks = true;
            }
        }
    }
    
    public enum TaskOperation {
        ADD, UPDATE, DELETE
    }
}
