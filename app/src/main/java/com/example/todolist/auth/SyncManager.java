package com.example.todolist.auth;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final String USERS_PATH = "users";
    private static final String TASKS_PATH = "tasks";
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TaskRepository taskRepository;
    private SyncListener syncListener;
    
    public interface SyncListener {
        void onSyncStart();
        void onSyncProgress(int progress, String status);
        void onSyncComplete(boolean success, String message);
        void onSyncError(String error);
    }
    
    public SyncManager(SyncListener listener) {
        this.syncListener = listener;
        this.mAuth = FirebaseAuth.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.taskRepository = new TaskRepository();
    }
    
    /**
     * Đồng bộ tasks từ local lên Firebase
     */
    public void syncLocalToFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (syncListener != null) {
                syncListener.onSyncError("User not signed in");
            }
            return;
        }
        
        if (syncListener != null) {
            syncListener.onSyncStart();
            syncListener.onSyncProgress(10, "Đang tải dữ liệu local...");
        }
        
        // Lấy tất cả tasks từ local database
        taskRepository.getAllTasks(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> localTasks) {
                Log.d(TAG, "Found " + localTasks.size() + " local tasks");
                
                if (syncListener != null) {
                    syncListener.onSyncProgress(30, "Đang đồng bộ lên Firebase...");
                }
                
                uploadTasksToFirebase(user.getUid(), localTasks);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading local tasks: " + error);
                if (syncListener != null) {
                    syncListener.onSyncError("Lỗi tải dữ liệu local: " + error);
                }
            }
        });
    }
    
    /**
     * Đồng bộ tasks từ Firebase về local
     */
    public void syncFirebaseToLocal() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (syncListener != null) {
                syncListener.onSyncError("User not signed in");
            }
            return;
        }
        
        if (syncListener != null) {
            syncListener.onSyncStart();
            syncListener.onSyncProgress(10, "Đang tải dữ liệu từ Firebase...");
        }
        
        DatabaseReference userTasksRef = mDatabase.child(USERS_PATH).child(user.getUid()).child(TASKS_PATH);
        
        userTasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Task> firebaseTasks = new ArrayList<>();
                
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setId(taskSnapshot.getKey());
                        firebaseTasks.add(task);
                    }
                }
                
                Log.d(TAG, "Found " + firebaseTasks.size() + " Firebase tasks");
                
                if (syncListener != null) {
                    syncListener.onSyncProgress(60, "Đang cập nhật dữ liệu local...");
                }
                
                mergeTasksToLocal(firebaseTasks);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading Firebase tasks: " + error.getMessage());
                if (syncListener != null) {
                    syncListener.onSyncError("Lỗi tải dữ liệu Firebase: " + error.getMessage());
                }
            }
        });
    }
    
    /**
     * Đồng bộ hai chiều (merge dữ liệu)
     */
    public void syncBidirectional() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (syncListener != null) {
                syncListener.onSyncError("User not signed in");
            }
            return;
        }
        
        if (syncListener != null) {
            syncListener.onSyncStart();
            syncListener.onSyncProgress(5, "Bắt đầu đồng bộ hai chiều...");
        }
        
        // Thực hiện sync từ local lên trước
        syncLocalToFirebase();
        
        // Sau đó sync từ Firebase về để merge
        new android.os.Handler().postDelayed(() -> {
            syncFirebaseToLocal();
        }, 2000); // Delay 2 giây để upload hoàn thành
    }
    
    private void uploadTasksToFirebase(String userId, List<Task> tasks) {
        DatabaseReference userTasksRef = mDatabase.child(USERS_PATH).child(userId).child(TASKS_PATH);
        
        int totalTasks = tasks.size();
        if (totalTasks == 0) {
            if (syncListener != null) {
                syncListener.onSyncComplete(true, "Không có dữ liệu để đồng bộ");
            }
            return;
        }
        
        final int[] completedUploads = {0};
        
        for (Task task : tasks) {
            String taskId = task.getId();
            if (taskId == null || taskId.isEmpty()) {
                taskId = userTasksRef.push().getKey();
                task.setId(taskId);
            }
            
            userTasksRef.child(taskId).setValue(task)
                    .addOnCompleteListener(uploadTask -> {
                        completedUploads[0]++;
                        
                        int progress = 30 + (completedUploads[0] * 40 / totalTasks);
                        if (syncListener != null) {
                            syncListener.onSyncProgress(progress, 
                                "Đã upload " + completedUploads[0] + "/" + totalTasks + " tasks");
                        }
                        
                        if (completedUploads[0] == totalTasks) {
                            Log.d(TAG, "All tasks uploaded successfully");
                            if (syncListener != null) {
                                syncListener.onSyncComplete(true, 
                                    "Đã đồng bộ " + totalTasks + " nhiệm vụ lên Firebase");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error uploading task: " + e.getMessage());
                        if (syncListener != null) {
                            syncListener.onSyncError("Lỗi upload task: " + e.getMessage());
                        }
                    });
        }
    }
    
    private void mergeTasksToLocal(List<Task> firebaseTasks) {
        // Implement merge logic: cập nhật local database với tasks từ Firebase
        // Ở đây ta có thể implement logic phức tạp để merge data
        // Hiện tại sẽ đơn giản là add tất cả tasks từ Firebase
        
        int totalTasks = firebaseTasks.size();
        if (totalTasks == 0) {
            if (syncListener != null) {
                syncListener.onSyncComplete(true, "Firebase không có dữ liệu để đồng bộ");
            }
            return;
        }
        
        final int[] completedSaves = {0};
        
        for (Task task : firebaseTasks) {
            taskRepository.addTask(task, new BaseRepository.DatabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    completedSaves[0]++;
                    
                    int progress = 60 + (completedSaves[0] * 35 / totalTasks);
                    if (syncListener != null) {
                        syncListener.onSyncProgress(progress, 
                            "Đã lưu " + completedSaves[0] + "/" + totalTasks + " tasks");
                    }
                    
                    if (completedSaves[0] == totalTasks) {
                        Log.d(TAG, "All Firebase tasks saved to local");
                        if (syncListener != null) {
                            syncListener.onSyncComplete(true, 
                                "Đã đồng bộ " + totalTasks + " nhiệm vụ từ Firebase");
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error saving task to local: " + error);
                    if (syncListener != null) {
                        syncListener.onSyncError("Lỗi lưu task local: " + error);
                    }
                }
            });
        }
    }
    
    /**
     * Xóa tất cả dữ liệu của user trên Firebase
     */
    public void clearUserDataOnFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        DatabaseReference userRef = mDatabase.child(USERS_PATH).child(user.getUid());
        userRef.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User data cleared from Firebase");
                    } else {
                        Log.e(TAG, "Error clearing user data from Firebase");
                    }
                });
    }
}
