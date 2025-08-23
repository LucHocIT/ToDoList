package com.example.todolist.util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {
    private static FirebaseHelper instance;
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private FirebaseHelper() {
        // Firebase persistence is already enabled in TodoApplication
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }
    /**
     * Get current user ID for database operations
     * @return Current user ID or "anonymous" if not authenticated
     */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        // For local usage without authentication, use device-specific ID
        return "local_user";
    }
    /**
     * Get reference to user's tasks
     * @return DatabaseReference to tasks
     */
    public DatabaseReference getTasksReference() {
        String userId = getCurrentUserId();
        DatabaseReference tasksRef = database.getReference("users").child(userId).child("tasks");
        tasksRef.keepSynced(true); // Keep synced offline
        return tasksRef;
    }
    /**
     * Get reference to user's categories
     * @return DatabaseReference to categories
     */
    public DatabaseReference getCategoriesReference() {
        String userId = getCurrentUserId();
        DatabaseReference categoriesRef = database.getReference("users").child(userId).child("categories");
        categoriesRef.keepSynced(true); // Keep synced offline
        return categoriesRef;
    }
    /**
     * Get reference to specific task
     * @param taskId Task ID
     * @return DatabaseReference to specific task
     */
    public DatabaseReference getTaskReference(String taskId) {
        return getTasksReference().child(taskId);
    }
    /**
     * Get reference to specific category
     * @param categoryId Category ID
     * @return DatabaseReference to specific category
     */
    public DatabaseReference getCategoryReference(String categoryId) {
        return getCategoriesReference().child(categoryId);
    }
    /**
     * Generate new task ID
     * @return New unique task ID
     */
    public String generateTaskId() {
        return getTasksReference().push().getKey();
    }
    /**
     * Generate new category ID
     * @return New unique category ID
     */
    public String generateCategoryId() {
        return getCategoriesReference().push().getKey();
    }
    /**
     * Check if user is authenticated
     * @return true if authenticated, false otherwise
     */
    public boolean isUserAuthenticated() {
        return auth.getCurrentUser() != null;
    }
}
