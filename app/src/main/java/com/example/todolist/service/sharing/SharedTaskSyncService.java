package com.example.todolist.service.sharing;

import android.content.Context;

import com.example.todolist.manager.AuthManager;
import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;
import com.example.todolist.model.TaskShare;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service để đồng bộ real-time các thay đổi trên shared tasks
 */
public class SharedTaskSyncService {
    private static final String SHARED_TASKS_NODE = "shared_tasks";
    private static final String USERS_NODE = "users";
    private static final String TASKS_NODE = "tasks";

    private static SharedTaskSyncService instance;
    private DatabaseReference database;
    private ExecutorService executor;
    private AuthManager authManager;
    private Context context;

    // Listeners for real-time updates
    private List<SharedTaskUpdateListener> updateListeners;
    private List<ValueEventListener> firebaseListeners;
    private List<String> listeningTasks;

    public interface SharedTaskUpdateListener {
        void onSharedTaskUpdated(Task task);
        void onSubTaskUpdated(String taskId, SubTask subTask);
        void onTaskSharingChanged(TaskShare taskShare);
        void onError(String error);
    }

    public interface SharedTaskCallback {
        void onTaskLoaded(Task task);
        void onError(String error);
    }

    private SharedTaskSyncService() {
        this.database = FirebaseDatabase.getInstance().getReference();
        this.executor = Executors.newSingleThreadExecutor();
        this.updateListeners = new ArrayList<>();
        this.firebaseListeners = new ArrayList<>();
        this.listeningTasks = new ArrayList<>();
    }

    public static SharedTaskSyncService getInstance() {
        if (instance == null) {
            instance = new SharedTaskSyncService();
        }
        return instance;
    }

    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.authManager = AuthManager.getInstance();
        this.authManager.initialize(context);
    }

    /**
     * Thêm listener cho real-time updates
     */
    public void addUpdateListener(SharedTaskUpdateListener listener) {
        if (!updateListeners.contains(listener)) {
            updateListeners.add(listener);
        }
    }

    /**
     * Xóa listener
     */
    public void removeUpdateListener(SharedTaskUpdateListener listener) {
        updateListeners.remove(listener);
    }

    /**
     * Bắt đầu lắng nghe real-time updates cho một shared task
     */
    public void startListeningForTaskUpdates(String taskId) {
        if (listeningTasks.contains(taskId)) {
            return; // Đã đang lắng nghe rồi
        }

        String currentUserEmail = authManager.getCurrentUserEmail();
        if (currentUserEmail == null) return;

        // Kiểm tra xem task có được chia sẻ không
        TaskSharingService.getInstance().getTaskShare(taskId, new TaskSharingService.TaskShareCallback() {
            @Override
            public void onTaskShareLoaded(TaskShare taskShare) {
                if (taskShare.isOwner(currentUserEmail) || taskShare.isUserShared(currentUserEmail)) {
                    setupTaskListener(taskId, taskShare.getOwnerId());
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void setupTaskListener(String taskId, String ownerId) {
        String userPath = USERS_NODE + "/" + sanitizeEmail(ownerId) + "/" + TASKS_NODE + "/" + taskId;
        
        ValueEventListener taskListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Task task = convertToTask(dataSnapshot);
                    if (task != null) {
                        notifyTaskUpdated(task);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                notifyError(databaseError.getMessage());
            }
        };

        database.child(userPath).addValueEventListener(taskListener);
        firebaseListeners.add(taskListener);
        listeningTasks.add(taskId);
    }

    /**
     * Dừng lắng nghe updates cho một task
     */
    public void stopListeningForTaskUpdates(String taskId) {
        int index = listeningTasks.indexOf(taskId);
        if (index >= 0 && index < firebaseListeners.size()) {
            // Tạm thời bỏ qua việc remove listener để tránh lỗi
            // database.removeEventListener(firebaseListeners.get(index));
            listeningTasks.remove(index);
            firebaseListeners.remove(index);
        }
    }

    /**
     * Cập nhật shared task
     */
    public void updateSharedTask(Task task, TaskSharingService.SharingCallback callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onError("Sync disabled");
            return;
        }

        String currentUserEmail = authManager.getCurrentUserEmail();
        if (currentUserEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }

        TaskSharingService.getInstance().getTaskShare(task.getId(), new TaskSharingService.TaskShareCallback() {
            @Override
            public void onTaskShareLoaded(TaskShare taskShare) {
                if (!taskShare.canUserEdit(currentUserEmail)) {
                    if (callback != null) callback.onError("You don't have permission to edit this task");
                    return;
                }

                // Cập nhật task trong database của owner
                String ownerPath = USERS_NODE + "/" + sanitizeEmail(taskShare.getOwnerEmail()) + "/" + TASKS_NODE + "/" + task.getId();
                Map<String, Object> taskData = task.toMap();
                
                database.child(ownerPath).setValue(taskData)
                        .addOnSuccessListener(aVoid -> {
                            if (callback != null) callback.onSuccess("Task updated successfully");
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) callback.onError(e.getMessage());
                        });
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError("Task is not shared");
            }
        });
    }

    /**
     * Thêm subtask vào shared task
     */
    public void addSubTaskToSharedTask(String taskId, SubTask subTask, TaskSharingService.SharingCallback callback) {
        loadSharedTask(taskId, new SharedTaskCallback() {
            @Override
            public void onTaskLoaded(Task task) {
                task.addSubTask(subTask);
                updateSharedTask(task, callback);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    /**
     * Cập nhật subtask trong shared task
     */
    public void updateSubTaskInSharedTask(String taskId, SubTask subTask, TaskSharingService.SharingCallback callback) {
        loadSharedTask(taskId, new SharedTaskCallback() {
            @Override
            public void onTaskLoaded(Task task) {
                List<SubTask> subTasks = task.getSubTasks();
                if (subTasks != null) {
                    for (int i = 0; i < subTasks.size(); i++) {
                        if (subTasks.get(i).getId().equals(subTask.getId())) {
                            subTasks.set(i, subTask);
                            break;
                        }
                    }
                    task.setSubTasks(subTasks);
                    updateSharedTask(task, callback);
                } else {
                    if (callback != null) callback.onError("SubTask not found");
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    /**
     * Xóa subtask từ shared task
     */
    public void removeSubTaskFromSharedTask(String taskId, String subTaskId, TaskSharingService.SharingCallback callback) {
        loadSharedTask(taskId, new SharedTaskCallback() {
            @Override
            public void onTaskLoaded(Task task) {
                List<SubTask> subTasks = task.getSubTasks();
                if (subTasks != null) {
                    subTasks.removeIf(subTask -> subTask.getId().equals(subTaskId));
                    task.setSubTasks(subTasks);
                    updateSharedTask(task, callback);
                } else {
                    if (callback != null) callback.onError("SubTask not found");
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    /**
     * Load shared task từ Firebase
     */
    public void loadSharedTask(String taskId, SharedTaskCallback callback) {
        TaskSharingService.getInstance().getTaskShare(taskId, new TaskSharingService.TaskShareCallback() {
            @Override
            public void onTaskShareLoaded(TaskShare taskShare) {
                String ownerPath = USERS_NODE + "/" + sanitizeEmail(taskShare.getOwnerEmail()) + "/" + TASKS_NODE + "/" + taskId;
                
                database.child(ownerPath).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Task task = convertToTask(dataSnapshot);
                            
                            if (task != null && task.getId() != null && !task.getId().trim().isEmpty()) {
                                // Đánh dấu task này là shared
                                task.setShared(true);
                                
                                // Verify task is still valid after setShared
                                if (task.getId() != null && !task.getId().trim().isEmpty()) {
                                    if (callback != null) callback.onTaskLoaded(task);
                                } else {
                                    if (callback != null) callback.onError("Task ID became null after setShared");
                                }
                            } else {
                                if (callback != null) callback.onError("Failed to convert task data");
                            }
                        } else {
                            if (callback != null) callback.onError("Task not found");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        if (callback != null) callback.onError(databaseError.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError("Task is not shared: " + error);
            }
        });
    }

    /**
     * Dừng tất cả listeners
     */
    public void stopAllListeners() {
        // Tạm thời bỏ qua việc remove listeners để tránh lỗi
        firebaseListeners.clear();
        listeningTasks.clear();
        updateListeners.clear();
    }

    /**
     * Dừng lắng nghe updates cho tất cả shared tasks
     */
    public void stopListeningForAllTasks() {
        for (String taskId : new ArrayList<>(listeningTasks)) {
            stopListeningForTaskUpdates(taskId);
        }
    }

    private Task convertToTask(DataSnapshot dataSnapshot) {
        try {
            String taskId = dataSnapshot.getKey();
            if (taskId == null || taskId.trim().isEmpty()) {
                return null;
            }
            
            Task task = new Task();
            task.setId(taskId);
            if (task.getId() == null || task.getId().trim().isEmpty()) {
                return null;
            }
            
            String title = dataSnapshot.child("title").getValue(String.class);
            task.setTitle(title);
            
            task.setDescription(dataSnapshot.child("description").getValue(String.class));
            task.setDueDate(dataSnapshot.child("dueDate").getValue(String.class));
            task.setDueTime(dataSnapshot.child("dueTime").getValue(String.class));
            
            Boolean isCompleted = dataSnapshot.child("isCompleted").getValue(Boolean.class);
            task.setCompleted(isCompleted != null ? isCompleted : false);
            
            Boolean isImportant = dataSnapshot.child("isImportant").getValue(Boolean.class);
            task.setImportant(isImportant != null ? isImportant : false);
            
            task.setCategory(dataSnapshot.child("category").getValue(String.class));
            task.setReminderType(dataSnapshot.child("reminderType").getValue(String.class));
            
            Boolean hasReminder = dataSnapshot.child("hasReminder").getValue(Boolean.class);
            task.setHasReminder(hasReminder != null ? hasReminder : false);
            
            task.setAttachments(dataSnapshot.child("attachments").getValue(String.class));
            task.setRepeatType(dataSnapshot.child("repeatType").getValue(String.class));
            
            Boolean isRepeating = dataSnapshot.child("isRepeating").getValue(Boolean.class);
            task.setRepeating(isRepeating != null ? isRepeating : false);
            
            task.setCompletionDate(dataSnapshot.child("completionDate").getValue(String.class));
            task.setCreatedAt(dataSnapshot.child("createdAt").getValue(String.class));
            task.setUpdatedAt(dataSnapshot.child("updatedAt").getValue(String.class));
            
            Long lastModified = dataSnapshot.child("lastModified").getValue(Long.class);
            task.setLastModified(lastModified);

            // Load SubTasks
            List<SubTask> subTasks = new ArrayList<>();
            DataSnapshot subTasksSnapshot = dataSnapshot.child("subTasks");
            for (DataSnapshot subTaskSnapshot : subTasksSnapshot.getChildren()) {
                SubTask subTask = convertToSubTask(subTaskSnapshot);
                if (subTask != null) {
                    subTasks.add(subTask);
                }
            }
            task.setSubTasks(subTasks);

            if (task.getId() == null || task.getId().trim().isEmpty()) {
                return null;
            }

            return task;
        } catch (Exception e) {
            return null;
        }
    }

    private SubTask convertToSubTask(DataSnapshot dataSnapshot) {
        try {
            SubTask subTask = new SubTask();
            String subTaskId = dataSnapshot.child("id").getValue(String.class);
            if (subTaskId == null) {
                subTaskId = dataSnapshot.getKey();
            }
            subTask.setId(subTaskId);
            
            subTask.setTitle(dataSnapshot.child("title").getValue(String.class));
            subTask.setTaskId(dataSnapshot.child("taskId").getValue(String.class));
            
            Boolean isCompleted = dataSnapshot.child("isCompleted").getValue(Boolean.class);
            subTask.setCompleted(isCompleted != null ? isCompleted : false);
            
            subTask.setCreatedAt(dataSnapshot.child("createdAt").getValue(String.class));

            return subTask;
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyTaskUpdated(Task task) {
        for (SharedTaskUpdateListener listener : updateListeners) {
            try {
                listener.onSharedTaskUpdated(task);
            } catch (Exception e) {
            }
        }
    }

    private void notifyError(String error) {
        for (SharedTaskUpdateListener listener : updateListeners) {
            try {
                listener.onError(error);
            } catch (Exception e) {
            }
        }
    }

    private boolean shouldSync() {
        return authManager != null && authManager.shouldSyncToFirebase();
    }

    private String sanitizeEmail(String email) {
        return email.replace(".", "_").replace("@", "_at_");
    }
}