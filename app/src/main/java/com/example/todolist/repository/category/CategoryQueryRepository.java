package com.example.todolist.repository.category;

import androidx.annotation.NonNull;
import com.example.todolist.model.Category;
import com.example.todolist.repository.BaseRepository;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * CategoryQueryRepository - Xử lý các truy vấn và lọc dữ liệu Category
 */
public class CategoryQueryRepository extends BaseRepository {
    
    private CategoryCrudRepository crudRepository;

    public CategoryQueryRepository() {
        this.crudRepository = new CategoryCrudRepository();
    }

    public void getAllCategories(ListCallback<Category> callback) {
        DatabaseReference categoriesRef = crudRepository.getCategoriesRef();
        
        categoriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                
                // Sort categories by creation time
                Collections.sort(categories, new Comparator<Category>() {
                    @Override
                    public int compare(Category c1, Category c2) {
                        try {
                            long time1 = c1.getCreatedAt();
                            long time2 = c2.getCreatedAt();
                            return Long.compare(time1, time2);
                        } catch (Exception e) {
                            return c1.getName().compareTo(c2.getName());
                        }
                    }
                });
                
                if (callback != null) {
                    callback.onSuccess(categories);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (callback != null) {
                    callback.onError("Failed to get categories: " + databaseError.getMessage());
                }
            }
        });
    }

    public void searchCategories(String query, RepositoryCallback<List<Category>> callback) {
        getAllCategories(new ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categories) {
                List<Category> filteredCategories = new ArrayList<>();
                
                for (Category category : categories) {
                    if (query == null || query.trim().isEmpty() ||
                        category.getName().toLowerCase().contains(query.toLowerCase())) {
                        filteredCategories.add(category);
                    }
                }
                
                if (callback != null) {
                    callback.onSuccess(filteredCategories);
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    public void getCategoriesByColor(String color, ListCallback<Category> callback) {
        DatabaseReference categoriesRef = crudRepository.getCategoriesRef();
        Query query = categoriesRef.orderByChild("color").equalTo(color);
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
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
                
                if (callback != null) {
                    callback.onSuccess(categories);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (callback != null) {
                    callback.onError("Failed to get categories by color: " + databaseError.getMessage());
                }
            }
        });
    }

    public void checkDuplicateCategories(RepositoryCallback<List<Category>> callback) {
        getAllCategories(new ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categories) {
                List<Category> duplicates = new ArrayList<>();
                List<String> seenNames = new ArrayList<>();
                
                for (Category category : categories) {
                    String name = category.getName().toLowerCase().trim();
                    if (seenNames.contains(name)) {
                        duplicates.add(category);
                    } else {
                        seenNames.add(name);
                    }
                }
                
                if (callback != null) {
                    callback.onSuccess(duplicates);
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    public void getDefaultCategories(ListCallback<Category> callback) {
        getAllCategories(new ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categories) {
                List<Category> defaultCategories = new ArrayList<>();
                
                for (Category category : categories) {
                    if (isDefaultCategory(category)) {
                        defaultCategories.add(category);
                    }
                }
                
                if (callback != null) {
                    callback.onSuccess(defaultCategories);
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    private boolean isDefaultCategory(Category category) {
        if (category == null || category.getName() == null) return false;
        
        String name = category.getName().toLowerCase();
        return name.equals("công việc") || 
               name.equals("cá nhân") || 
               name.equals("học tập") ||
               name.equals("work") ||
               name.equals("personal") ||
               name.equals("study");
    }
}
