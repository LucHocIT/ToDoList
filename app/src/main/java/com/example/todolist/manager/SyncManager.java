package com.example.todolist.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.todolist.repository.TaskRepository;
import com.example.todolist.model.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final String PREF_NAME = "sync_prefs";
    private static final String KEY_LAST_SYNC = "last_sync_time";
    private static final String KEY_AUTO_SYNC = "auto_sync_enabled";
    
    private static SyncManager instance;
    private DatabaseReference database;
    private Context context;
    private SharedPreferences prefs;
    private TaskRepository taskRepository;
    
    private SyncManager() {
        // Private constructor for singleton
    }
    
    public static SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.database = FirebaseDatabase.getInstance().getReference();
        this.taskRepository = new TaskRepository(context);
    }
    
    public void syncTasks(String userEmail, SyncCallback callback) {
        if (userEmail == null || userEmail.isEmpty()) {
            if (callback != null) {
                callback.onError("User email is required for sync");
            }
            return;
        }
        
        // Get local tasks (simplified for now)
        // List<Task> localTasks = taskRepository.getAllTasks();
        List<Task> localTasks = new ArrayList<>();
        
        // Upload local tasks to Firebase Realtime Database
        uploadTasksToFirebase(userEmail, localTasks, new SyncCallback() {
            @Override
            public void onSuccess(String message) {
                // Download tasks from Firebase Realtime Database
                downloadTasksFromFirebase(userEmail, new SyncCallback() {
                    @Override
                    public void onSuccess(String downloadMessage) {
                        updateLastSyncTime();
                        if (callback != null) {
                            callback.onSuccess("Sync completed successfully");
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        if (callback != null) {
                            callback.onError("Download failed: " + error);
                        }
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError("Upload failed: " + error);
                }
            }
        });
    }
    
    private void uploadTasksToFirebase(String userEmail, List<Task> tasks, SyncCallback callback) {
        if (tasks.isEmpty()) {
            if (callback != null) {
                callback.onSuccess("No tasks to upload");
            }
            return;
        }
        
        String sanitizedEmail = userEmail.replace(".", "_").replace("@", "_at_");
        String userPath = "users/" + sanitizedEmail + "/tasks";
        
        for (Task task : tasks) {
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("title", task.getTitle());
            taskData.put("description", task.getDescription());
            taskData.put("priority", task.getPriority());
            taskData.put("category", task.getCategory());
            taskData.put("dueDate", task.getDueDate());
            taskData.put("isCompleted", task.isCompleted());
            taskData.put("lastModified", System.currentTimeMillis());
            
            String taskId = "task_" + task.getId();
            
            database.child(userPath).child(taskId).setValue(taskData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task uploaded: " + task.getTitle());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading task: " + task.getTitle(), e);
                });
        }
        
        if (callback != null) {
            callback.onSuccess("Tasks uploaded to Firebase");
        }
    }
    
    private void downloadTasksFromFirebase(String userEmail, SyncCallback callback) {
        String sanitizedEmail = userEmail.replace(".", "_").replace("@", "_at_");
        String userPath = "users/" + sanitizedEmail + "/tasks";
        
        database.child(userPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Task> remoteTasks = new ArrayList<>();
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> taskData = (Map<String, Object>) taskSnapshot.getValue();
                        if (taskData != null) {
                            Task remoteTask = new Task();
                            remoteTask.setTitle((String) taskData.get("title"));
                            remoteTask.setDescription((String) taskData.get("description"));
                            remoteTask.setPriority((String) taskData.get("priority"));
                            remoteTask.setCategory((String) taskData.get("category"));
                            remoteTask.setDueDate((String) taskData.get("dueDate"));
                            remoteTask.setCompleted(Boolean.TRUE.equals(taskData.get("isCompleted")));
                            
                            remoteTasks.add(remoteTask);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing task data", e);
                    }
                }
                
                // Merge remote tasks with local database
                mergeTasksWithLocal(remoteTasks);
                
                if (callback != null) {
                    callback.onSuccess("Tasks downloaded from Firebase");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error getting tasks", databaseError.toException());
                if (callback != null) {
                    callback.onError("Failed to download tasks: " + databaseError.getMessage());
                }
            }
        });
    }
    
    private void mergeTasksWithLocal(List<Task> remoteTasks) {
        // Simple merge strategy: add remote tasks that don't exist locally (simplified)
        // List<Task> localTasks = taskRepository.getAllTasks();
        List<Task> localTasks = new ArrayList<>();
        
        for (Task remoteTask : remoteTasks) {
            boolean exists = false;
            for (Task localTask : localTasks) {
                if (localTask.getTitle().equals(remoteTask.getTitle()) && 
                    localTask.getDescription().equals(remoteTask.getDescription())) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                // taskRepository.insertTask(remoteTask);
                Log.d(TAG, "Would add remote task: " + remoteTask.getTitle());
            }
        }
    }
    
    public boolean isAutoSyncEnabled() {
        return prefs.getBoolean(KEY_AUTO_SYNC, true);
    }
    
    public void setAutoSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply();
    }
    
    public String getLastSyncTime() {
        long lastSync = prefs.getLong(KEY_LAST_SYNC, 0);
        if (lastSync == 0) {
            return "Chưa từng";
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(lastSync));
    }
    
    private void updateLastSyncTime() {
        prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply();
    }
    
    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
