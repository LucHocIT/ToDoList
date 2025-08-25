package com.example.todolist.helper.taskdetail;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.todolist.adapter.CategorySpinnerAdapter;
import com.example.todolist.model.Category;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.service.CategoryService;

import java.util.List;

public class CategoryHandler implements CategoryService.CategoryUpdateListener {
    private AppCompatActivity activity;
    private CategoryService categoryService;
    private Spinner spinnerCategory;
    private CategorySpinnerAdapter categoryAdapter;
    private List<Category> allCategories;
    private Category selectedCategory;
    private boolean isInitialCategorySetup = true;

    public interface TaskUpdateCallback {
        void updateTask(Task task);
        Task getCurrentTask();
        void setResult(int resultCode);
    }

    private TaskUpdateCallback taskUpdateCallback;

    public CategoryHandler(AppCompatActivity activity, Spinner spinnerCategory, TaskUpdateCallback callback) {
        this.activity = activity;
        this.spinnerCategory = spinnerCategory;
        this.taskUpdateCallback = callback;
        this.categoryService = new CategoryService(activity, this);
        
        setupCategorySpinner();
    }

    private void setupCategorySpinner() {
        categoryService.getAllCategories(new BaseRepository.RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> categories) {
                allCategories = categories;
                activity.runOnUiThread(() -> {
                    categoryAdapter = new CategorySpinnerAdapter(activity, allCategories);
                    spinnerCategory.setAdapter(categoryAdapter);
                    spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            android.util.Log.d("TaskDetail", "onItemSelected called - position: " + position + ", isInitialSetup: " + isInitialCategorySetup);
                            
                            if (isInitialCategorySetup) {
                                android.util.Log.d("TaskDetail", "Skipping onItemSelected because isInitialCategorySetup = true");
                                return;
                            }
                            
                            Category selectedCat = categoryAdapter.getCategory(position);
                            android.util.Log.d("TaskDetail", "Selected category: " + (selectedCat != null ? selectedCat.getName() + " (id: " + selectedCat.getId() + ")" : "null"));
                            
                            Task currentTask = taskUpdateCallback.getCurrentTask();
                            if (selectedCat != null && currentTask != null && !currentTask.isCompleted()) {
                                selectedCategory = selectedCat;
                                String newCategoryId = "0".equals(selectedCat.getId()) ? null : selectedCat.getId();
                                String currentCategoryId = currentTask.getCategoryId();
                                
                                android.util.Log.d("TaskDetail", "Category comparison - current: " + currentCategoryId + ", new: " + newCategoryId);
                                
                                boolean categoryChanged = (newCategoryId == null && currentCategoryId != null) ||
                                                        (newCategoryId != null && !newCategoryId.equals(currentCategoryId));
                                
                                android.util.Log.d("TaskDetail", "Category changed: " + categoryChanged);
                                
                                if (categoryChanged) {
                                    android.util.Log.d("TaskDetail", "Updating task category from " + currentCategoryId + " to " + newCategoryId);
                                    currentTask.setCategoryId(newCategoryId);

                                    taskUpdateCallback.updateTask(currentTask);
                                    activity.runOnUiThread(() -> {
                                        taskUpdateCallback.setResult(AppCompatActivity.RESULT_OK);
                                    });
                                }
                            }
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    Task currentTask = taskUpdateCallback.getCurrentTask();
                    if (currentTask != null) {
                        setSelectedCategoryInSpinner(currentTask.getCategoryId());
                    }
                });
            }
            @Override
            public void onError(String error) {
                activity.runOnUiThread(() ->
                    Toast.makeText(activity, "Lỗi tải categories: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    public void setSelectedCategoryInSpinner(String categoryId) {
        if (categoryAdapter != null) {
            android.util.Log.d("TaskDetail", "setSelectedCategoryInSpinner called with categoryId: " + categoryId + ", isInitialSetup: " + isInitialCategorySetup);            
            isInitialCategorySetup = true;
            
            int positionToSelect = 0; 
            for (int i = 0; i < categoryAdapter.getCount(); i++) {
                Category category = categoryAdapter.getCategory(i);
                if (category != null) {
                    if (categoryId == null && "0".equals(category.getId())) {
                        positionToSelect = i;
                        android.util.Log.d("TaskDetail", "Task has no category, selecting default at position: " + i);
                        break;
                    }
                    else if (categoryId != null && category.getId().equals(categoryId)) {
                        positionToSelect = i;
                        android.util.Log.d("TaskDetail", "Found category at position: " + i + ", category: " + category.getName());
                        break;
                    }
                }
            }                   
            
            android.util.Log.d("TaskDetail", "Setting spinner selection to position: " + positionToSelect);
            spinnerCategory.setSelection(positionToSelect);
            spinnerCategory.post(() -> {
                isInitialCategorySetup = false;
                android.util.Log.d("TaskDetail", "isInitialCategorySetup reset to false");
            });
        }
    }

    public void updateCategorySelection() {
        Task currentTask = taskUpdateCallback.getCurrentTask();
        if (currentTask != null && categoryAdapter != null) {
            setSelectedCategoryInSpinner(currentTask.getCategoryId());
        }
    }

    @Override
    public void onCategoriesUpdated() {
        // Handle category updates if needed
    }

    @Override
    public void onError(String error) {
        activity.runOnUiThread(() -> 
            Toast.makeText(activity, "CategoryService error: " + error, Toast.LENGTH_SHORT).show()
        );
    }

    public void cleanup() {
        if (categoryService != null) {
            categoryService.cleanup();
        }
    }
}
