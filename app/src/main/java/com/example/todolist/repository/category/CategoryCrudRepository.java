package com.example.todolist.repository.category;

import com.example.todolist.model.Category;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.util.FirebaseHelper;
import com.google.firebase.database.DatabaseReference;

/**
 * CategoryCrudRepository - Xử lý các thao tác CRUD cơ bản cho Category
 */
public class CategoryCrudRepository extends BaseRepository {
    
    private final FirebaseHelper firebaseHelper;
    private final DatabaseReference categoriesRef;

    public CategoryCrudRepository() {
        this.firebaseHelper = FirebaseHelper.getInstance();
        this.categoriesRef = firebaseHelper.getCategoriesReference();
    }

    public void addCategory(Category category, DatabaseCallback<String> callback) {
        String categoryId = firebaseHelper.generateCategoryId();
        if (categoryId != null) {
            category.setId(categoryId);
            DatabaseReference categoryRef = firebaseHelper.getCategoryReference(categoryId);
            saveData(categoryRef, category.toMap(), new DatabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (callback != null) {
                        callback.onSuccess(categoryId);
                    }
                }

                @Override
                public void onError(String error) {
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onError("Failed to generate category ID");
            }
        }
    }

    public void updateCategory(Category category, DatabaseCallback<Boolean> callback) {
        if (category.getId() == null) {
            if (callback != null) {
                callback.onError("Category ID is required");
            }
            return;
        }

        DatabaseReference categoryRef = firebaseHelper.getCategoryReference(category.getId());
        saveData(categoryRef, category.toMap(), callback);
    }

    public void deleteCategory(Category category, DatabaseCallback<Boolean> callback) {
        if (category.getId() == null) {
            if (callback != null) {
                callback.onError("Category ID is required");
            }
            return;
        }

        DatabaseReference categoryRef = firebaseHelper.getCategoryReference(category.getId());
        categoryRef.removeValue()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Failed to delete category: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }
            });
    }

    public void getCategoryById(String categoryId, RepositoryCallback<Category> callback) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("Category ID is required");
            }
            return;
        }

        DatabaseReference categoryRef = firebaseHelper.getCategoryReference(categoryId);
        categoryRef.get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Category category = task.getResult().getValue(Category.class);
                    if (category != null) {
                        category.setId(categoryId);
                        if (callback != null) {
                            callback.onSuccess(category);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("Category not found");
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Failed to get category: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                }
            });
    }

    public DatabaseReference getCategoriesRef() {
        return categoriesRef;
    }

    public FirebaseHelper getFirebaseHelper() {
        return firebaseHelper;
    }
}
