package com.example.todolist.repository;

import androidx.annotation.NonNull;
import com.example.todolist.model.Category;
import com.example.todolist.repository.category.CategoryCrudRepository;
import com.example.todolist.repository.category.CategoryQueryRepository;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CategoryRepository extends BaseRepository {
    
    // Sub-repositories that handle specific functionality
    private CategoryCrudRepository crudRepository;      // CRUD operations
    private CategoryQueryRepository queryRepository;    // Query and filtering
    
    public CategoryRepository() {
        this.crudRepository = new CategoryCrudRepository();
        this.queryRepository = new CategoryQueryRepository();
    }

    // === CRUD OPERATIONS - Delegate to CategoryCrudRepository ===
    public void addCategory(Category category, DatabaseCallback<String> callback) {
        crudRepository.addCategory(category, callback);
    }

    public void updateCategory(Category category, DatabaseCallback<Boolean> callback) {
        crudRepository.updateCategory(category, callback);
    }

    public void deleteCategory(Category category, DatabaseCallback<Boolean> callback) {
        crudRepository.deleteCategory(category, callback);
    }

    public void getCategoryById(String categoryId, RepositoryCallback<Category> callback) {
        crudRepository.getCategoryById(categoryId, callback);
    }

    // === QUERY OPERATIONS - Delegate to CategoryQueryRepository ===
    public void getAllCategories(ListCallback<Category> callback) {
        queryRepository.getAllCategories(callback);
    }

    public void searchCategories(String query, RepositoryCallback<List<Category>> callback) {
        queryRepository.searchCategories(query, callback);
    }

    public void getCategoriesByColor(String color, ListCallback<Category> callback) {
        queryRepository.getCategoriesByColor(color, callback);
    }

    public void getDefaultCategories(ListCallback<Category> callback) {
        queryRepository.getDefaultCategories(callback);
    }
    
    public ValueEventListener addCategoriesRealtimeListener(ListCallback<Category> callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Category> categories = new ArrayList<>();
                
                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    Category category = categorySnapshot.getValue(Category.class);
                    if (category != null) {
                        category.setId(categorySnapshot.getKey());
                        categories.add(category);
                    }
                }
                Collections.sort(categories, new Comparator<Category>() {
                    @Override
                    public int compare(Category c1, Category c2) {
                        int order1 = c1.getSortOrder();
                        int order2 = c2.getSortOrder();
                        
                        int orderComparison = Integer.compare(order1, order2);
                        if (orderComparison != 0) {
                            return orderComparison;
                        }
                        
                        return c1.getName().compareTo(c2.getName());
                    }
                });
                
                if (callback != null) {
                    callback.onSuccess(categories);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (callback != null) {
                    callback.onError("Realtime listener error: " + databaseError.getMessage());
                }
            }
        };
        
        crudRepository.getCategoriesRef().addValueEventListener(listener);
        return listener;
    }

    public void removeCategoriesListener(ValueEventListener listener) {
        if (listener != null) {
            crudRepository.getCategoriesRef().removeEventListener(listener);
        }
    }

    // === UTILITY OPERATIONS ===
    public void initializeDefaultCategories(DatabaseCallback<Boolean> callback) {
        queryRepository.getDefaultCategories(new ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> defaultCategories) {
                if (defaultCategories.isEmpty()) {
                    createDefaultCategories(callback);
                } else {
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                }
            }

            @Override
            public void onError(String error) {
                createDefaultCategories(callback);
            }
        });
    }

    private void createDefaultCategories(DatabaseCallback<Boolean> callback) {
        List<Category> defaultCategories = new ArrayList<>();
        
        Category personalCategory = new Category();
        personalCategory.setName("Cá nhân");
        personalCategory.setColor("#4CAF50");
        personalCategory.setSortOrder(0);
        personalCategory.setIsDefault(true);
        defaultCategories.add(personalCategory);
        
        Category favoriteCategory = new Category();
        favoriteCategory.setName("Yêu thích");
        favoriteCategory.setColor("#FF9800");
        favoriteCategory.setSortOrder(1);
        favoriteCategory.setIsDefault(true);
        defaultCategories.add(favoriteCategory);
        
        Category workCategory = new Category();
        workCategory.setName("Công việc");
        workCategory.setColor("#2196F3");
        workCategory.setSortOrder(2);
        workCategory.setIsDefault(true);
        defaultCategories.add(workCategory);
        
        addCategoriesSequentially(defaultCategories, 0, callback);
    }

    private void addCategoriesSequentially(List<Category> categories, int index, DatabaseCallback<Boolean> callback) {
        if (index >= categories.size()) {
            if (callback != null) {
                callback.onSuccess(true);
            }
            return;
        }
        
        addCategory(categories.get(index), new DatabaseCallback<String>() {
            @Override
            public void onSuccess(String categoryId) {
                addCategoriesSequentially(categories, index + 1, callback);
            }

            @Override
            public void onError(String error) {
                addCategoriesSequentially(categories, index + 1, callback);
            }
        });
    }

    public void removeDuplicateCategories(DatabaseCallback<Boolean> callback) {
        queryRepository.checkDuplicateCategories(new RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> duplicates) {
                if (duplicates.isEmpty()) {
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                    return;
                }
                
                removeDuplicatesSequentially(duplicates, 0, callback);
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    private void removeDuplicatesSequentially(List<Category> duplicates, int index, DatabaseCallback<Boolean> callback) {
        if (index >= duplicates.size()) {
            if (callback != null) {
                callback.onSuccess(true);
            }
            return;
        }
        
        deleteCategory(duplicates.get(index), new DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                removeDuplicatesSequentially(duplicates, index + 1, callback);
            }

            @Override
            public void onError(String error) {
                removeDuplicatesSequentially(duplicates, index + 1, callback);
            }
        });
    }

    // === LEGACY COMPATIBILITY METHODS ===
    @Deprecated
    public void insertCategory(Category category, DatabaseCallback<String> callback) {
        addCategory(category, callback);
    }

    @Deprecated
    public void getAllCategories(RepositoryCallback<List<Category>> callback) {
        getAllCategories(new ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categories) {
                callback.onSuccess(categories);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
