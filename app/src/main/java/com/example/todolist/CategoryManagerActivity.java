package com.example.todolist;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CategoryManagerActivity extends AppCompatActivity {

    private LinearLayout layoutCategories;
    private LinearLayout btnCreateNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        initViews();
        setupClickListeners();
        loadCategories();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        layoutCategories = findViewById(R.id.layout_categories);
        btnCreateNew = findViewById(R.id.btn_create_new);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnCreateNew.setOnClickListener(v -> showCreateCategoryDialog());
    }

    private void loadCategories() {
        // Add default categories
        addCategoryItem("Cá nhân", 0);
        addCategoryItem("Công việc", 0);
        addCategoryItem("Yêu thích", 0);
    }

    private void addCategoryItem(String name, int count) {
        View categoryView = LayoutInflater.from(this).inflate(R.layout.item_category, null);
        
        TextView tvCategoryName = categoryView.findViewById(R.id.tv_category_name);
        TextView tvTaskCount = categoryView.findViewById(R.id.tv_task_count);
        ImageView btnMenu = categoryView.findViewById(R.id.btn_category_menu);

        tvCategoryName.setText(name);
        tvTaskCount.setText(String.valueOf(count));

        btnMenu.setOnClickListener(v -> showCategoryMenu(name));

        layoutCategories.addView(categoryView);
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
                addCategoryItem(categoryName, 0);
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
