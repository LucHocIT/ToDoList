package com.example.todolist.manager;

import android.content.Context;
import android.util.Log;

import com.example.todolist.model.Task;
import com.example.todolist.model.Category;
import com.example.todolist.model.SubTask;
import com.example.todolist.repository.BaseRepository;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirebaseSyncManager {
    private static final String TAG = "FirebaseSyncManager";
    private static final String USERS_NODE = "users";
    private static final String TASKS_NODE = "tasks";
    private static final String CATEGORIES_NODE = "categories";
    // SUBTASKS_NODE removed - SubTasks are now stored inside Tasks
    
    private static FirebaseSyncManager instance;
    private DatabaseReference database;
    private ExecutorService executor;
    private AuthManager authManager;
    
    private FirebaseSyncManager() {
        this.database = FirebaseDatabase.getInstance().getReference();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static FirebaseSyncManager getInstance() {
        if (instance == null) {
            instance = new FirebaseSyncManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        this.authManager = AuthManager.getInstance();
    }
    
    public void addTaskToFirebase(Task task, BaseRepository.DatabaseCallback<String> callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess(task.getId());
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        executor.execute(() -> {
            try {
                Map<String, Object> taskData = convertTaskToMap(task);
                String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + TASKS_NODE;
                
                if (task.getId() == null || task.getId().isEmpty()) {
                    // Create new task with auto-generated ID
                    DatabaseReference newTaskRef = database.child(userPath).push();
                    String firebaseId = newTaskRef.getKey();
                    
                    newTaskRef.setValue(taskData)
                        .addOnSuccessListener(aVoid -> {
                            if (callback != null) callback.onSuccess(firebaseId);
                            Log.d(TAG, "Task added to Firebase with ID: " + firebaseId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error adding task to Firebase", e);
                            if (callback != null) callback.onError(e.getMessage());
                        });
                } else {
                    // Update existing task
                    database.child(userPath).child(task.getId()).setValue(taskData)
                        .addOnSuccessListener(aVoid -> {
                            if (callback != null) callback.onSuccess(task.getId());
                            Log.d(TAG, "Task updated in Firebase: " + task.getId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating task in Firebase", e);
                            if (callback != null) callback.onError(e.getMessage());
                        });
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in addTaskToFirebase", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    public void updateTaskInFirebase(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess(true);
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        executor.execute(() -> {
            try {
                Map<String, Object> taskData = convertTaskToMap(task);
                String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + TASKS_NODE;
                
                database.child(userPath).child(task.getId()).setValue(taskData)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess(true);
                        Log.d(TAG, "Task updated in Firebase: " + task.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating task in Firebase", e);
                        if (callback != null) callback.onError(e.getMessage());
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in updateTaskInFirebase", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    public void deleteTaskFromFirebase(String taskId, BaseRepository.DatabaseCallback<Boolean> callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess(true);
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        executor.execute(() -> {
            try {
                String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + TASKS_NODE;
                
                database.child(userPath).child(taskId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess(true);
                        Log.d(TAG, "Task deleted from Firebase: " + taskId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting task from Firebase", e);
                        if (callback != null) callback.onError(e.getMessage());
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in deleteTaskFromFirebase", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    public void syncAllTasksToFirebase(List<Task> tasks, SyncCallback callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess("Sync disabled");
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        executor.execute(() -> {
            try {
                String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + TASKS_NODE;
                DatabaseReference tasksRef = database.child(userPath);
                
                // First load existing tasks from Firebase to merge with local tasks
                tasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Create a map of existing Firebase tasks
                        Map<String, Task> firebaseTasks = new HashMap<>();
                        for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                            try {
                                Map<String, Object> taskData = (Map<String, Object>) taskSnapshot.getValue();
                                if (taskData != null) {
                                    Task task = convertMapToTask(taskData);
                                    task.setId(taskSnapshot.getKey());
                                    firebaseTasks.put(task.getId(), task);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing existing task from Firebase", e);
                            }
                        }
                        
                        // Merge local tasks with Firebase tasks
                        Map<String, Task> allTasks = new HashMap<>(firebaseTasks);
                        for (Task localTask : tasks) {
                            // Local tasks override Firebase tasks if they have the same ID
                            allTasks.put(localTask.getId(), localTask);
                        }
                        
                        if (allTasks.isEmpty()) {
                            if (callback != null) callback.onSuccess("All tasks synced to Firebase");
                            return;
                        }
                        
                        int totalTasks = allTasks.size();
                        final int[] completedTasks = {0};
                        final boolean[] hasError = {false};
                        
                        // Upload all merged tasks
                        for (Task task : allTasks.values()) {
                            Map<String, Object> taskData = convertTaskToMap(task);
                            
                            tasksRef.child(task.getId()).setValue(taskData)
                                .addOnSuccessListener(aVoid -> {
                                    completedTasks[0]++;
                                    Log.d(TAG, "Task synced to Firebase: " + task.getTitle());
                                    if (completedTasks[0] == totalTasks && !hasError[0]) {
                                        if (callback != null) callback.onSuccess("All tasks synced to Firebase (" + totalTasks + " tasks)");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    hasError[0] = true;
                                    Log.e(TAG, "Error syncing task: " + task.getTitle(), e);
                                    if (callback != null) callback.onError("Error syncing task: " + e.getMessage());
                                });
                        }
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error loading existing tasks from Firebase", databaseError.toException());
                        // If we can't load existing tasks, just upload local tasks
                        if (tasks.isEmpty()) {
                            if (callback != null) callback.onSuccess("All tasks synced to Firebase");
                            return;
                        }
                        
                        int totalTasks = tasks.size();
                        final int[] completedTasks = {0};
                        final boolean[] hasError = {false};
                        
                        for (Task task : tasks) {
                            Map<String, Object> taskData = convertTaskToMap(task);
                            
                            tasksRef.child(task.getId()).setValue(taskData)
                                .addOnSuccessListener(aVoid -> {
                                    completedTasks[0]++;
                                    if (completedTasks[0] == totalTasks && !hasError[0]) {
                                        if (callback != null) callback.onSuccess("All tasks synced to Firebase");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    hasError[0] = true;
                                    if (callback != null) callback.onError("Error syncing task: " + e.getMessage());
                                });
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Exception in syncAllTasksToFirebase", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    public void loadTasksFromFirebase(FirebaseSyncCallback callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess(new ArrayList<>());
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + TASKS_NODE;
        
        database.child(userPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Task> tasks = new ArrayList<>();
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> taskData = (Map<String, Object>) taskSnapshot.getValue();
                        if (taskData != null) {
                            Task task = convertMapToTask(taskData);
                            task.setId(taskSnapshot.getKey());
                            tasks.add(task);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing task from Firebase", e);
                    }
                }
                if (callback != null) callback.onSuccess(tasks);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading tasks from Firebase", databaseError.toException());
                if (callback != null) callback.onError(databaseError.getMessage());
            }
        });
    }
    
    // Category sync methods
    public void addCategoryToFirebase(Category category, BaseRepository.DatabaseCallback<String> callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess(category.getId());
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        executor.execute(() -> {
            try {
                Map<String, Object> categoryData = category.toMap();
                String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + CATEGORIES_NODE;
                
                if (category.getId() == null || category.getId().isEmpty()) {
                    // Create new category with auto-generated ID
                    DatabaseReference newCategoryRef = database.child(userPath).push();
                    String firebaseId = newCategoryRef.getKey();
                    
                    newCategoryRef.setValue(categoryData)
                        .addOnSuccessListener(aVoid -> {
                            if (callback != null) callback.onSuccess(firebaseId);
                            Log.d(TAG, "Category added to Firebase with ID: " + firebaseId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error adding category to Firebase", e);
                            if (callback != null) callback.onError(e.getMessage());
                        });
                } else {
                    // Update existing category
                    database.child(userPath).child(category.getId()).setValue(categoryData)
                        .addOnSuccessListener(aVoid -> {
                            if (callback != null) callback.onSuccess(category.getId());
                            Log.d(TAG, "Category updated in Firebase: " + category.getId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating category in Firebase", e);
                            if (callback != null) callback.onError(e.getMessage());
                        });
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in addCategoryToFirebase", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    public void updateCategoryInFirebase(Category category, BaseRepository.DatabaseCallback<Boolean> callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess(true);
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        executor.execute(() -> {
            try {
                Map<String, Object> categoryData = category.toMap();
                String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + CATEGORIES_NODE;
                
                database.child(userPath).child(category.getId()).setValue(categoryData)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess(true);
                        Log.d(TAG, "Category updated in Firebase: " + category.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating category in Firebase", e);
                        if (callback != null) callback.onError(e.getMessage());
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in updateCategoryInFirebase", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    public void deleteCategoryFromFirebase(String categoryId, BaseRepository.DatabaseCallback<Boolean> callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess(true);
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        executor.execute(() -> {
            try {
                String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + CATEGORIES_NODE;
                
                database.child(userPath).child(categoryId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess(true);
                        Log.d(TAG, "Category deleted from Firebase: " + categoryId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting category from Firebase", e);
                        if (callback != null) callback.onError(e.getMessage());
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in deleteCategoryFromFirebase", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    public void loadCategoriesFromFirebase(FirebaseCategorySyncCallback callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess(new ArrayList<>());
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + CATEGORIES_NODE;
        
        database.child(userPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Category> categories = new ArrayList<>();
                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> categoryData = (Map<String, Object>) categorySnapshot.getValue();
                        if (categoryData != null) {
                            Category category = Category.fromMap(categoryData);
                            category.setId(categorySnapshot.getKey());
                            categories.add(category);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing category from Firebase", e);
                    }
                }
                if (callback != null) callback.onSuccess(categories);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading categories from Firebase", databaseError.toException());
                if (callback != null) callback.onError(databaseError.getMessage());
            }
        });
    }
    
    public void syncAllCategoriesToFirebase(List<Category> categories, SyncCallback callback) {
        if (!shouldSync()) {
            if (callback != null) callback.onSuccess("Sync disabled");
            return;
        }
        
        String userEmail = authManager.getCurrentUserEmail();
        if (userEmail == null) {
            if (callback != null) callback.onError("User not authenticated");
            return;
        }
        
        executor.execute(() -> {
            try {
                String userPath = USERS_NODE + "/" + sanitizeEmail(userEmail) + "/" + CATEGORIES_NODE;
                DatabaseReference categoriesRef = database.child(userPath);
                
                // First load existing categories from Firebase to merge with local categories
                categoriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Create a map of existing Firebase categories
                        Map<String, Category> firebaseCategories = new HashMap<>();
                        for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                            try {
                                Map<String, Object> categoryData = (Map<String, Object>) categorySnapshot.getValue();
                                if (categoryData != null) {
                                    Category category = Category.fromMap(categoryData);
                                    category.setId(categorySnapshot.getKey());
                                    firebaseCategories.put(category.getId(), category);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing existing category from Firebase", e);
                            }
                        }
                        
                        // Merge local categories with Firebase categories
                        Map<String, Category> allCategories = new HashMap<>(firebaseCategories);
                        for (Category localCategory : categories) {
                            // Local categories override Firebase categories if they have the same ID
                            allCategories.put(localCategory.getId(), localCategory);
                        }
                        
                        if (allCategories.isEmpty()) {
                            if (callback != null) callback.onSuccess("All categories synced to Firebase");
                            return;
                        }
                        
                        int totalCategories = allCategories.size();
                        final int[] completedCategories = {0};
                        final boolean[] hasError = {false};
                        
                        // Upload all merged categories
                        for (Category category : allCategories.values()) {
                            Map<String, Object> categoryData = category.toMap();
                            
                            categoriesRef.child(category.getId()).setValue(categoryData)
                                .addOnSuccessListener(aVoid -> {
                                    completedCategories[0]++;
                                    Log.d(TAG, "Category synced to Firebase: " + category.getName());
                                    if (completedCategories[0] == totalCategories && !hasError[0]) {
                                        if (callback != null) callback.onSuccess("All categories synced to Firebase (" + totalCategories + " categories)");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    hasError[0] = true;
                                    Log.e(TAG, "Error syncing category: " + category.getName(), e);
                                    if (callback != null) callback.onError("Error syncing category: " + e.getMessage());
                                });
                        }
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error loading existing categories from Firebase", databaseError.toException());
                        // If we can't load existing categories, just upload local categories
                        if (categories.isEmpty()) {
                            if (callback != null) callback.onSuccess("All categories synced to Firebase");
                            return;
                        }
                        
                        int totalCategories = categories.size();
                        final int[] completedCategories = {0};
                        final boolean[] hasError = {false};
                        
                        for (Category category : categories) {
                            Map<String, Object> categoryData = category.toMap();
                            
                            categoriesRef.child(category.getId()).setValue(categoryData)
                                .addOnSuccessListener(aVoid -> {
                                    completedCategories[0]++;
                                    if (completedCategories[0] == totalCategories && !hasError[0]) {
                                        if (callback != null) callback.onSuccess("All categories synced to Firebase");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    hasError[0] = true;
                                    if (callback != null) callback.onError("Error syncing category: " + e.getMessage());
                                });
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Exception in syncAllCategoriesToFirebase", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
    
    private boolean shouldSync() {
        return authManager != null && authManager.shouldSyncToFirebase();
    }
    
    private String sanitizeEmail(String email) {
        return email.replace(".", "_").replace("@", "_at_");
    }
    
    private Map<String, Object> convertTaskToMap(Task task) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("title", task.getTitle());
        taskData.put("description", task.getDescription());
        taskData.put("dueDate", task.getDueDate());
        taskData.put("dueTime", task.getDueTime());
        taskData.put("isCompleted", task.isCompleted());
        taskData.put("isImportant", task.isImportant());
        taskData.put("categoryId", task.getCategoryId());
        taskData.put("priority", task.getPriority());
        taskData.put("category", task.getCategory());
        taskData.put("reminderType", task.getReminderType());
        taskData.put("hasReminder", task.isHasReminder());
        taskData.put("repeatType", task.getRepeatType());
        taskData.put("isRepeating", task.isRepeating());
        taskData.put("completionDate", task.getCompletionDate());
        taskData.put("createdDate", task.getCreatedAt());
        taskData.put("lastModified", task.getLastModified() != null ? task.getLastModified() : System.currentTimeMillis());
        
        if (task.getAttachments() != null) {
            taskData.put("attachments", task.getAttachments());
        }

        if (task.getSubTasks() != null && !task.getSubTasks().isEmpty()) {
            List<Map<String, Object>> subTasksData = new ArrayList<>();
            for (SubTask subTask : task.getSubTasks()) {
                if (subTask != null) {
                    subTasksData.add(subTask.toMap());
                }
            }
            taskData.put("subTasks", subTasksData);
        } else {
            taskData.put("subTasks", new ArrayList<>());
        }
        
        return taskData;
    }
    
    private Task convertMapToTask(Map<String, Object> data) {
        Task task = new Task();
        task.setTitle((String) data.get("title"));
        task.setDescription((String) data.get("description"));
        task.setDueDate((String) data.get("dueDate"));
        task.setDueTime((String) data.get("dueTime"));
        task.setCompleted(Boolean.TRUE.equals(data.get("isCompleted")));
        task.setImportant(Boolean.TRUE.equals(data.get("isImportant")));
        task.setCategoryId((String) data.get("categoryId"));
        task.setPriority((String) data.get("priority"));
        task.setCategory((String) data.get("category"));
        task.setReminderType((String) data.get("reminderType"));
        task.setHasReminder(Boolean.TRUE.equals(data.get("hasReminder")));
        task.setRepeatType((String) data.get("repeatType"));
        task.setRepeating(Boolean.TRUE.equals(data.get("isRepeating")));
        task.setCompletionDate((String) data.get("completionDate"));
        task.setCreatedAt((String) data.get("createdDate"));
        
        // Handle lastModified field
        if (data.get("lastModified") instanceof Long) {
            task.setLastModified((Long) data.get("lastModified"));
        } else if (data.get("lastModified") instanceof Number) {
            task.setLastModified(((Number) data.get("lastModified")).longValue());
        } else {
            task.setLastModified(System.currentTimeMillis());
        }
        
        if (data.get("attachments") != null) {
            task.setAttachments((String) data.get("attachments"));
        }
        
        // Convert SubTasks from Firebase Map list back to SubTask objects
        if (data.get("subTasks") != null) {
            List<Map<String, Object>> subTasksData = (List<Map<String, Object>>) data.get("subTasks");
            List<SubTask> subTasks = new ArrayList<>();
            for (Map<String, Object> subTaskData : subTasksData) {
                if (subTaskData != null) {
                    SubTask subTask = SubTask.fromMap(subTaskData);
                    subTasks.add(subTask);
                }
            }
            task.setSubTasks(subTasks);
        } else {
            task.setSubTasks(new ArrayList<>());
        }
        
        return task;
    }
    
    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface FirebaseSyncCallback {
        void onSuccess(List<Task> tasks);
        void onError(String error);
    }
    
    public interface FirebaseCategorySyncCallback {
        void onSuccess(List<Category> categories);
        void onError(String error);
    }
       
    public interface SubTaskSyncCallback {
        void onSuccess(List<SubTask> subTasks);
        void onError(String error);
    }
}
