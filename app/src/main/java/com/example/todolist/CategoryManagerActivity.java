package com.example.todolist;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.adapter.CategoryAdapter;
import com.example.todolist.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryManagerActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {

    private RecyclerView recyclerCategories;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories;    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        initViews();
        setupRecyclerView();
        loadCategories();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        recyclerCategories = findViewById(R.id.recycler_categories);
        categories = new ArrayList<>();

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter(categories, this);
        recyclerCategories.setAdapter(categoryAdapter);
        
        // Add ItemTouchHelper for drag & drop
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                @NonNull RecyclerView.ViewHolder viewHolder, 
                                @NonNull RecyclerView.ViewHolder target) {
                
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                // Don't allow moving the "Add new" item
                if (fromPosition == categories.size() - 1 || toPosition == categories.size() - 1) {
                    return false;
                }
                
                Collections.swap(categories, fromPosition, toPosition);
                categoryAdapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used
            }
        });
        
        itemTouchHelper.attachToRecyclerView(recyclerCategories);
    }    private void loadCategories() {
        // Add default categories
        categories.add(new Category("Cá nhân", 0, "#4285F4"));
        categories.add(new Category("Công việc", 0, "#FF9800"));
        categories.add(new Category("Yêu thích", 0, "#F44336"));
        
        // Add "Create new" item at the end
        categories.add(new Category("", -1, "")); // Special item for "Add new"
        
        categoryAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCategoryClick(Category category) {
        if (category.getTaskCount() == -1) {
            // This is the "Add new" item
            showCreateCategoryDialog();
        } else {
            // Regular category click
            Toast.makeText(this, "Clicked: " + category.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCategoryMenuClick(Category category) {
        showCategoryMenu(category.getName());
    }

    private void showCreateCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_category, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        EditText editCategoryName = dialogView.findViewById(R.id.edit_category_name);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnSave = dialogView.findViewById(R.id.btn_save);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_char_count);

        // Update character count
        editCategoryName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/50");
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
          btnSave.setOnClickListener(v -> {
            String categoryName = editCategoryName.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                // Insert before the "Add new" item
                categories.add(categories.size() - 1, new Category(categoryName, 0, "#4285F4"));
                categoryAdapter.notifyItemInserted(categories.size() - 2);
                dialog.dismiss();
                Toast.makeText(this, "Đã tạo danh mục mới", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showCategoryMenu(String categoryName) {
        // Show options for edit/delete category
        Toast.makeText(this, "Menu cho danh mục: " + categoryName, Toast.LENGTH_SHORT).show();
    }
}
