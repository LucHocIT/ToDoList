package com.example.todolist.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.adapter.CategorySelectionAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryPickerDialog {
    
    public interface OnCategorySelectedListener {
        void onCategorySelected(Category category);
    }
    
    private AlertDialog dialog;
    private Context context;
    private OnCategorySelectedListener listener;
    private CategorySelectionAdapter adapter;
    private Category selectedCategory;
    private List<Category> categories;
    
    public CategoryPickerDialog(Context context, OnCategorySelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.categories = new ArrayList<>();
        initDialog();
    }
    
    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_category_picker, null);
        builder.setView(dialogView);
        
        dialog = builder.create();
        
        // Make dialog background transparent so CardView corners show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        setupRecyclerView(dialogView);
        setupClickListeners(dialogView);
        loadCategories();
    }
    
    private void setupRecyclerView(View dialogView) {
        RecyclerView recyclerCategories = dialogView.findViewById(R.id.recycler_categories);
        recyclerCategories.setLayoutManager(new LinearLayoutManager(context));
        
        adapter = new CategorySelectionAdapter(categories, context, category -> {
            selectedCategory = category;
        });
        recyclerCategories.setAdapter(adapter);
    }
    
    private void setupClickListeners(View dialogView) {
        dialogView.findViewById(R.id.btn_cancel_category).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.btn_done_category).setOnClickListener(v -> {
            if (listener != null && selectedCategory != null) {
                listener.onCategorySelected(selectedCategory);
            }
            dialog.dismiss();
        });
    }
    
    private void loadCategories() {
        new Thread(() -> {
            try {
                TodoDatabase database = TodoDatabase.getInstance(context);
                List<Category> dbCategories = database.categoryDao().getAllCategories();
                
                // Update on main thread
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        categories.clear();
                        categories.addAll(dbCategories);
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Add default categories if error
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        addDefaultCategories();
                        adapter.notifyDataSetChanged();
                    });
                }
            }
        }).start();
    }
    
    private void addDefaultCategories() {
        categories.clear();
        categories.add(new Category("Tất cả", "#4285F4", 0, true));
        categories.add(new Category("Công việc", "#FF9800", 1, true));
        categories.add(new Category("Cá nhân", "#9C27B0", 2, true));
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
    
    public void setSelectedCategory(Category category) {
        this.selectedCategory = category;
        if (adapter != null) {
            adapter.setSelectedCategory(category);
        }
    }
}
