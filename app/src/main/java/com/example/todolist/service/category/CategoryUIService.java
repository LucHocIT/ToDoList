package com.example.todolist.service.category;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import com.example.todolist.R;
import com.example.todolist.model.Category;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * CategoryUIService - Xử lý hiển thị UI cho Category
 */
public class CategoryUIService {
    
    private Context context;
    private LinearLayout categoriesContainer;

    public CategoryUIService(Context context, LinearLayout categoriesContainer) {
        this.context = context;
        this.categoriesContainer = categoriesContainer;
    }

    public void displayCategories(List<Category> categories, CategoryClickListener clickListener) {
        if (categoriesContainer == null) return;
        
        categoriesContainer.removeAllViews();
        
        // Add "All Tasks" button first
        addAllTasksButton(clickListener);
        
        // Add category buttons
        for (Category category : categories) {
            addCategoryButton(category, clickListener);
        }
    }

    private void addAllTasksButton(CategoryClickListener clickListener) {
        MaterialButton allTasksBtn = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        allTasksBtn.setText("Tất cả");
        allTasksBtn.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_category));
        allTasksBtn.setIconGravity(MaterialButton.ICON_GRAVITY_START);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        allTasksBtn.setLayoutParams(params);
        
        allTasksBtn.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onAllTasksClick();
            }
        });
        
        categoriesContainer.addView(allTasksBtn);
    }

    private void addCategoryButton(Category category, CategoryClickListener clickListener) {
        MaterialButton categoryBtn = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        categoryBtn.setText(category.getName());
        

        if (category.getColor() != null && !category.getColor().isEmpty()) {
            try {
                int color = Color.parseColor(category.getColor());
                categoryBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
                categoryBtn.setTextColor(color);
                categoryBtn.setIconTint(android.content.res.ColorStateList.valueOf(color));
            } catch (Exception e) {
                // Use default color if parsing fails
            }
        }
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        categoryBtn.setLayoutParams(params);
        
        categoryBtn.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCategoryClick(category);
            }
        });
        
        categoryBtn.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCategoryLongClick(category);
            }
            return true;
        });
        
        categoriesContainer.addView(categoryBtn);
    }

    private int getIconResource(String iconName) {
        if (iconName == null || iconName.isEmpty()) return 0;
        
        try {
            return context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }

    public void hideContainer() {
        if (categoriesContainer != null) {
            categoriesContainer.setVisibility(View.GONE);
        }
    }

    public void showContainer() {
        if (categoriesContainer != null) {
            categoriesContainer.setVisibility(View.VISIBLE);
        }
    }

    public void clearContainer() {
        if (categoriesContainer != null) {
            categoriesContainer.removeAllViews();
        }
    }

    public interface CategoryClickListener {
        void onAllTasksClick();
        void onCategoryClick(Category category);
        void onCategoryLongClick(Category category);
    }
}
