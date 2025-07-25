package com.example.todolist.manager;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.todolist.R;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Category;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CategoryManager {
    
    public interface CategoryUpdateListener {
        void onCategoriesUpdated();
    }
    
    private Context context;
    private TodoDatabase database;
    private CategoryUpdateListener listener;
    private List<Category> categories;
    private LinearLayout categoriesContainer;
    
    public CategoryManager(Context context, LinearLayout categoriesContainer, CategoryUpdateListener listener) {
        this.context = context;
        this.categoriesContainer = categoriesContainer;
        this.listener = listener;
        this.database = TodoDatabase.getInstance(context);
        this.categories = new ArrayList<>();
    }
    
    public void loadCategories() {
        new Thread(() -> {
            // First, clean up any duplicate categories
            database.categoryDao().removeDuplicateCategories();
            
            categories = database.categoryDao().getAllCategories();
            Log.d("CategoryManager", "loadCategories: Loaded " + categories.size() + " categories after cleanup");
            
            if (categories.isEmpty()) {
                createDefaultCategories();
                categories = database.categoryDao().getAllCategories();
                Log.d("CategoryManager", "loadCategories: Created default categories, now have " + categories.size() + " categories");
            }
            
            // Update UI on main thread using Handler
            new Handler(Looper.getMainLooper()).post(() -> {
                // Update dynamic category buttons after loading categories
                updateDynamicCategoryButtonsInternal();
                
                // Only notify listener once after categories are loaded and buttons updated
                if (listener != null) {
                    listener.onCategoriesUpdated();
                }
            });
        }).start();
    }
    
    public void updateDynamicCategoryButtons() {
        // Load categories from database to ensure correct order
        new Thread(() -> {
            List<Category> orderedCategories = database.categoryDao().getAllCategories();
            
            // Update UI on main thread using Handler
            new Handler(Looper.getMainLooper()).post(() -> {
                categories = orderedCategories; // Update local categories list
                updateDynamicCategoryButtonsInternal();
            });
        }).start();
    }

    private void updateDynamicCategoryButtonsInternal() {
        // Always clear dynamic categories first to prevent duplication
        clearDynamicCategories();
        
        // Add dynamic category buttons in order (skip default categories)
        for (Category category : categories) {
            if (!isDefaultCategory(category.getName())) {
                addDynamicCategoryButton(category);
            }
        }
        
        // After updating buttons, setup click listeners
        setupDynamicCategoryClicks();
    }
    
    private boolean categoryButtonExists(String categoryName) {
        // Check if a button with this category name already exists
        for (int i = 4; i < categoriesContainer.getChildCount(); i++) {
            View child = categoriesContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                if (categoryName.equals(button.getTag())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void createDefaultCategories() {
        // Use try-catch to prevent errors if categories already exist
        try {
            // Check if any default categories exist first
            List<Category> existingCategories = database.categoryDao().getAllCategories();
            boolean hasDefaultCategories = false;
            
            for (Category category : existingCategories) {
                if (isDefaultCategory(category.getName())) {
                    hasDefaultCategories = true;
                    break;
                }
            }
            
            // Only create default categories if none exist
            if (!hasDefaultCategories) {
                Log.d("CategoryManager", "Creating default categories");
                
                Category workCategory = new Category("Công việc", "#FF9800", 1, true);
                database.categoryDao().insertCategory(workCategory);
                
                Category personalCategory = new Category("Cá nhân", "#9C27B0", 2, true);
                database.categoryDao().insertCategory(personalCategory);
                
                Category favoriteCategory = new Category("Yêu thích", "#E91E63", 3, true);
                database.categoryDao().insertCategory(favoriteCategory);
                
                Log.d("CategoryManager", "Default categories created successfully");
            } else {
                Log.d("CategoryManager", "Default categories already exist, skipping creation");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("CategoryManager", "Error creating default categories: " + e.getMessage());
        }
    }
    
    private boolean isDefaultCategory(String categoryName) {
        return categoryName.equals("Công việc") || 
               categoryName.equals("Cá nhân") ||
               categoryName.equals("Yêu thích");
    }
    
    private void clearDynamicCategories() {
        int childCount = categoriesContainer.getChildCount();
        // Start from index 4 (after the 4 default buttons: All, Work, Personal, Favorite)
        // Remove all dynamic category buttons in reverse order to avoid index issues
        for (int i = childCount - 1; i >= 4; i--) {
            categoriesContainer.removeViewAt(i);
        }
    }
    
    private void addDynamicCategoryButton(Category category) {
        MaterialButton categoryButton = new MaterialButton(context);
        categoryButton.setText(category.getName());
        categoryButton.setTextSize(14);
        categoryButton.setMinWidth(getDp(100));
        categoryButton.setPadding(getDp(16), 0, getDp(16), 0);
        
        // Set default style
        categoryButton.setTextColor(ContextCompat.getColor(context, R.color.text_gray));
        categoryButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
        
        // Set layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                getDp(40)
        );
        params.setMarginEnd(getDp(12));
        categoryButton.setLayoutParams(params);
        
        // Set tag for identification
        categoryButton.setTag(category.getName());
        
        categoriesContainer.addView(categoryButton);
    }
    
    private void setupDynamicCategoryClicks() {
        // This method will be called by MainActivity to setup click listeners
        // The FilterManager will handle the actual click events
    }
    
    private int getDp(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    public void clearAllDataAndReset() {
        new Thread(() -> {
            database.clearAllTables();
            createDefaultCategories();
            loadCategories();
        }).start();
    }
    
    // Method to clean up any "không có thể loại" entries in database
    public void cleanupDatabase() {
        new Thread(() -> {
            try {
                // Remove any "không có thể loại" entries from database
                Category noCategory = database.categoryDao().getCategoryByName("không có thể loại");
                if (noCategory != null) {
                    database.categoryDao().deleteCategory(noCategory);
                    Log.d("CategoryManager", "Removed 'không có thể loại' from database");
                }
                
                // Remove duplicates
                database.categoryDao().removeDuplicateCategories();
                Log.d("CategoryManager", "Cleaned up database duplicates");
            } catch (Exception e) {
                Log.e("CategoryManager", "Error cleaning database: " + e.getMessage());
            }
        }).start();
    }
    
    // Getters
    public List<Category> getCategories() { return categories; }
}
