package com.example.todolist.service.sharing;

import android.content.Context;
import android.util.Log;

import com.example.todolist.manager.AuthManager;
import com.example.todolist.model.SharedUser;
import com.example.todolist.model.Task;
import com.example.todolist.model.TaskShare;
import com.example.todolist.repository.BaseRepository;
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

public class TaskSharingService {
    private static final String TAG = "TaskSharingService";
    private static final String SHARED_TASKS_NODE = "shared_tasks";
    private static final String USER_SHARED_TASKS_NODE = "user_shared_tasks";

    private static TaskSharingService instance;
    private DatabaseReference database;
    private ExecutorService executor;
    private AuthManager authManager;
    private Context context;

    public interface SharingCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface SharedTasksCallback {
        void onSharedTasksLoaded(List<TaskShare> sharedTasks);
        void onError(String error);
    }

    public interface TaskShareCallback {
        void onTaskShareLoaded(TaskShare taskShare);
        void onError(String error);
    }

    private TaskSharingService() {
        this.database = FirebaseDatabase.getInstance().getReference();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static TaskSharingService getInstance() {
        if (instance == null) {
            instance = new TaskSharingService();
        }
        return instance;
    }

    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.authManager = AuthManager.getInstance();
        this.authManager.initialize(context);
    }

    /**
     * Chia sẻ task với một người dùng
     */
    public void shareTask(String taskId, String userEmail, String userName, boolean canEdit, SharingCallback callback) {
        // Kiểm tra và khởi tạo authManager nếu cần
        if (authManager == null) {
            authManager = AuthManager.getInstance();
            if (context != null) {
                authManager.initialize(context);
            }
        }
        
        String currentUserEmail = authManager != null ? authManager.getCurrentUserEmail() : null;

        executor.execute(() -> {
            try {
                // Kiểm tra xem task đã được chia sẻ chưa
                getTaskShare(taskId, new TaskShareCallback() {
                    @Override
                    public void onTaskShareLoaded(TaskShare taskShare) {
                        if (taskShare != null) {
                            // Task đã được chia sẻ, thêm người dùng mới
                            addUserToSharedTask(taskShare, userEmail, userName, canEdit, callback);
                        } else {
                            // Tạo mới task share
                            createNewTaskShare(taskId, userEmail, userName, canEdit, callback);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Không tìm thấy task share, tạo mới
                        createNewTaskShare(taskId, userEmail, userName, canEdit, callback);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error sharing task", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    private void createNewTaskShare(String taskId, String userEmail, String userName, boolean canEdit, SharingCallback callback) {
        // Kiểm tra và khởi tạo authManager nếu cần
        if (authManager == null) {
            authManager = AuthManager.getInstance();
            if (context != null) {
                authManager.initialize(context);
            }
        }
        
        String currentUserEmail = authManager != null ? authManager.getCurrentUserEmail() : null;
        String currentUserName = authManager != null ? authManager.getCurrentUserName() : null;

        TaskShare taskShare = new TaskShare(taskId, sanitizeEmail(currentUserEmail), currentUserEmail, currentUserName);
        SharedUser sharedUser = new SharedUser(userEmail, userName, canEdit);
        taskShare.addSharedUser(sharedUser);

        // Tạo ID cho task share
        DatabaseReference newShareRef = database.child(SHARED_TASKS_NODE).push();
        String shareId = newShareRef.getKey();
        taskShare.setId(shareId);

        // Lưu vào Firebase
        newShareRef.setValue(taskShare.toMap())
                .addOnSuccessListener(aVoid -> {
                    // Thêm reference vào user_shared_tasks cho cả owner và shared user
                    addTaskShareReference(currentUserEmail, shareId, taskId, true);
                    addTaskShareReference(userEmail, shareId, taskId, false);
                    
                    if (callback != null) callback.onSuccess("Task shared successfully");
                    Log.d(TAG, "Task shared successfully: " + shareId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating task share", e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    private void addUserToSharedTask(TaskShare taskShare, String userEmail, String userName, boolean canEdit, SharingCallback callback) {
        // Kiểm tra xem người dùng đã được chia sẻ chưa
        if (taskShare.isUserShared(userEmail)) {
            if (callback != null) callback.onError("User already has access to this task");
            return;
        }

        SharedUser sharedUser = new SharedUser(userEmail, userName, canEdit);
        taskShare.addSharedUser(sharedUser);

        // Cập nhật trong Firebase
        database.child(SHARED_TASKS_NODE).child(taskShare.getId()).setValue(taskShare.toMap())
                .addOnSuccessListener(aVoid -> {
                    // Thêm reference cho user mới
                    addTaskShareReference(userEmail, taskShare.getId(), taskShare.getTaskId(), false);
                    
                    if (callback != null) callback.onSuccess("User added to shared task");
                    Log.d(TAG, "User added to shared task: " + userEmail);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding user to shared task", e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Thêm reference vào user_shared_tasks
     */
    private void addTaskShareReference(String userEmail, String shareId, String taskId, boolean isOwner) {
        String sanitizedEmail = sanitizeEmail(userEmail);
        String userPath = USER_SHARED_TASKS_NODE + "/" + sanitizedEmail + "/" + shareId;

        Map<String, Object> referenceData = new java.util.HashMap<>();
        referenceData.put("shareId", shareId);
        referenceData.put("taskId", taskId);
        referenceData.put("isOwner", isOwner);
        referenceData.put("addedAt", System.currentTimeMillis());

        database.child(userPath).setValue(referenceData);
    }

    /**
     * Lấy thông tin chia sẻ của một task
     */
    public void getTaskShare(String taskId, TaskShareCallback callback) {
        // Bỏ qua kiểm tra sync - cho phép thực hiện luôn

        database.child(SHARED_TASKS_NODE)
                .orderByChild("taskId")
                .equalTo(taskId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                TaskShare taskShare = convertToTaskShare(snapshot);
                                if (taskShare != null && taskShare.isActive()) {
                                    if (callback != null) callback.onTaskShareLoaded(taskShare);
                                    return;
                                }
                            }
                        }
                        if (callback != null) callback.onError("Task share not found");
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error getting task share", databaseError.toException());
                        if (callback != null) callback.onError(databaseError.getMessage());
                    }
                });
    }

    public void getSharedTasksForCurrentUser(SharedTasksCallback callback) {
        // Kiểm tra và khởi tạo authManager nếu cần
        if (authManager == null) {
            authManager = AuthManager.getInstance();
            if (context != null) {
                authManager.initialize(context);
            }
        }
        
        String currentUserEmail = authManager != null ? authManager.getCurrentUserEmail() : null;
        if (currentUserEmail == null) {
            // Nếu chưa đăng nhập, thử lấy từ SharedPreferences hoặc trả về empty
            if (callback != null) callback.onSharedTasksLoaded(new ArrayList<>());
            return;
        }

        getSharedTasksForUser(currentUserEmail, callback);
    }

    public void getSharedTasksForUser(String userEmail, SharedTasksCallback callback) {
        String sanitizedEmail = sanitizeEmail(userEmail);
        String userPath = USER_SHARED_TASKS_NODE + "/" + sanitizedEmail;

        database.child(userPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> shareIds = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String shareId = snapshot.child("shareId").getValue(String.class);
                    if (shareId != null) {
                        shareIds.add(shareId);
                    }
                }

                if (shareIds.isEmpty()) {
                    if (callback != null) callback.onSharedTasksLoaded(new ArrayList<>());
                    return;
                }

                loadTaskShares(shareIds, callback);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error getting shared tasks for user", databaseError.toException());
                if (callback != null) callback.onError(databaseError.getMessage());
            }
        });
    }

    private void loadTaskShares(List<String> shareIds, SharedTasksCallback callback) {
        List<TaskShare> taskShares = new ArrayList<>();
        final int[] loadedCount = {0};

        for (String shareId : shareIds) {
            database.child(SHARED_TASKS_NODE).child(shareId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            TaskShare taskShare = convertToTaskShare(dataSnapshot);
                            if (taskShare != null && taskShare.isActive()) {
                                taskShares.add(taskShare);
                            }

                            loadedCount[0]++;
                            if (loadedCount[0] == shareIds.size()) {
                                if (callback != null) callback.onSharedTasksLoaded(taskShares);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            loadedCount[0]++;
                            if (loadedCount[0] == shareIds.size()) {
                                if (callback != null) callback.onSharedTasksLoaded(taskShares);
                            }
                        }
                    });
        }
    }

    /**
     * Rời khỏi shared task
     */
    public void leaveSharedTask(String taskId, SharingCallback callback) {
        // Kiểm tra và khởi tạo authManager nếu cần
        if (authManager == null) {
            authManager = AuthManager.getInstance();
            if (context != null) {
                authManager.initialize(context);
            }
        }
        
        String currentUserEmail = authManager != null ? authManager.getCurrentUserEmail() : null;

        getTaskShare(taskId, new TaskShareCallback() {
            @Override
            public void onTaskShareLoaded(TaskShare taskShare) {
                if (taskShare.isOwner(currentUserEmail)) {
                    if (callback != null) callback.onError("Owner cannot leave shared task");
                    return;
                }

                taskShare.removeSharedUser(currentUserEmail);

                // Cập nhật trong Firebase
                database.child(SHARED_TASKS_NODE).child(taskShare.getId()).setValue(taskShare.toMap())
                        .addOnSuccessListener(aVoid -> {
                            // Xóa reference từ user_shared_tasks
                            String sanitizedEmail = sanitizeEmail(currentUserEmail);
                            database.child(USER_SHARED_TASKS_NODE).child(sanitizedEmail).child(taskShare.getId()).removeValue();
                            
                            if (callback != null) callback.onSuccess("Left shared task successfully");
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) callback.onError(e.getMessage());
                        });
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }


    public void removeTaskSharing(String taskId, SharingCallback callback) {
        // Kiểm tra và khởi tạo authManager nếu cần
        if (authManager == null) {
            authManager = AuthManager.getInstance();
            if (context != null) {
                authManager.initialize(context);
            }
        }
        
        String currentUserEmail = authManager != null ? authManager.getCurrentUserEmail() : null;
        getTaskShare(taskId, new TaskShareCallback() {
            @Override
            public void onTaskShareLoaded(TaskShare taskShare) {
                if (!taskShare.isOwner(currentUserEmail)) {
                    if (callback != null) callback.onError("Only owner can remove task sharing");
                    return;
                }

                // Đánh dấu không active thay vì xóa hoàn toàn
                taskShare.setActive(false);

                database.child(SHARED_TASKS_NODE).child(taskShare.getId()).setValue(taskShare.toMap())
                        .addOnSuccessListener(aVoid -> {
                            if (callback != null) callback.onSuccess("Task sharing removed successfully");
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) callback.onError(e.getMessage());
                        });
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    private TaskShare convertToTaskShare(DataSnapshot dataSnapshot) {
        try {
            TaskShare taskShare = new TaskShare();
            taskShare.setId(dataSnapshot.getKey());
            taskShare.setTaskId(dataSnapshot.child("taskId").getValue(String.class));
            taskShare.setOwnerId(dataSnapshot.child("ownerId").getValue(String.class));
            taskShare.setOwnerEmail(dataSnapshot.child("ownerEmail").getValue(String.class));
            taskShare.setOwnerName(dataSnapshot.child("ownerName").getValue(String.class));
            taskShare.setCreatedAt(dataSnapshot.child("createdAt").getValue(String.class));
            taskShare.setUpdatedAt(dataSnapshot.child("updatedAt").getValue(String.class));
            
            Boolean isActive = dataSnapshot.child("isActive").getValue(Boolean.class);
            taskShare.setActive(isActive != null ? isActive : true);

            // Load shared users
            List<SharedUser> sharedUsers = new ArrayList<>();
            DataSnapshot usersSnapshot = dataSnapshot.child("sharedUsers");
            for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                SharedUser sharedUser = convertToSharedUser(userSnapshot);
                if (sharedUser != null) {
                    sharedUsers.add(sharedUser);
                }
            }
            taskShare.setSharedUsers(sharedUsers);

            return taskShare;
        } catch (Exception e) {
            Log.e(TAG, "Error converting to TaskShare", e);
            return null;
        }
    }

    private SharedUser convertToSharedUser(DataSnapshot dataSnapshot) {
        try {
            SharedUser sharedUser = new SharedUser();
            sharedUser.setEmail(dataSnapshot.child("email").getValue(String.class));
            sharedUser.setName(dataSnapshot.child("name").getValue(String.class));
            sharedUser.setUserId(dataSnapshot.child("userId").getValue(String.class));
            
            Boolean canEdit = dataSnapshot.child("canEdit").getValue(Boolean.class);
            sharedUser.setCanEdit(canEdit != null ? canEdit : true);
            
            sharedUser.setInvitedAt(dataSnapshot.child("invitedAt").getValue(String.class));
            sharedUser.setAcceptedAt(dataSnapshot.child("acceptedAt").getValue(String.class));
            sharedUser.setStatus(dataSnapshot.child("status").getValue(String.class));

            return sharedUser;
        } catch (Exception e) {
            Log.e(TAG, "Error converting to SharedUser", e);
            return null;
        }
    }

    public void updateTaskShare(TaskShare taskShare, SharingCallback callback) {
        if (taskShare == null || taskShare.getId() == null) {
            if (callback != null) callback.onError("Invalid task share data");
            return;
        }

        database.child(SHARED_TASKS_NODE).child(taskShare.getId()).setValue(taskShare.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess("Task share updated successfully");
                    Log.d(TAG, "Task share updated: " + taskShare.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task share", e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void acceptTaskInvitation(String taskId, String userEmail, SharingCallback callback) {
        Log.d(TAG, "Accepting task invitation for taskId: " + taskId + ", userEmail: " + userEmail);
        
        // Tìm TaskShare dựa trên taskId
        getTaskShare(taskId, new TaskShareCallback() {
            @Override
            public void onTaskShareLoaded(TaskShare taskShare) {
                // Tìm SharedUser trong danh sách
                SharedUser sharedUser = taskShare.getSharedUser(userEmail);
                if (sharedUser == null) {
                    Log.e(TAG, "User not found in task share: " + userEmail);
                    if (callback != null) callback.onError("Bạn không có quyền truy cập task này");
                    return;
                }
                sharedUser.acceptInvitation();
                taskShare.updateTimestamp();

                updateTaskShare(taskShare, new SharingCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Task invitation accepted successfully for user: " + userEmail);
                        if (callback != null) callback.onSuccess("Đã tham gia task thành công!");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error updating task share after accepting invitation: " + error);
                        if (callback != null) callback.onError("Lỗi cập nhật trạng thái: " + error);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting task share for acceptance: " + error);
                if (callback != null) callback.onError("Không tìm thấy thông tin chia sẻ task");
            }
        });
    }

    public void getUserSharedTasks(String userEmail, SharedTasksCallback callback) {
        if (callback != null) {
            callback.onSharedTasksLoaded(new ArrayList<>());
        }
    }

    private boolean shouldSync() {
        return authManager != null && authManager.shouldSyncToFirebase();
    }

    private String sanitizeEmail(String email) {
        if (email == null) return null;
        return email.replace(".", "_").replace("@", "_at_");
    }
}