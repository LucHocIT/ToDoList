package com.example.todolist.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.example.todolist.R;
import com.example.todolist.model.Category;
import java.util.ArrayList;
import java.util.List;

public class CategorySpinnerAdapter extends BaseAdapter {
    private Context context;
    private List<Category> categories;
    private LayoutInflater inflater;

    public CategorySpinnerAdapter(Context context, List<Category> categories) {
        this.context = context;
        this.categories = new ArrayList<>();
        
        // Add debug logging
        Log.d("CategorySpinnerAdapter", "Constructor called with " + (categories != null ? categories.size() : 0) + " categories");
        
        // Always add default "no category" option first
        Category defaultCategory = new Category("không có thể loại", "#999999", 0, false);
        defaultCategory.setId(0);
        this.categories.add(defaultCategory);
        
        // Add only non-duplicate categories from database
        if (categories != null) {
            for (Category category : categories) {
                // Skip the default "no category" entry if it exists in database
                if (category.getName().equalsIgnoreCase("không có thể loại")) {
                    Log.d("CategorySpinnerAdapter", "Skipping 'không có thể loại' from database");
                    continue;
                }
                
                // Skip if category name already exists (case insensitive)
                boolean exists = false;
                for (Category existing : this.categories) {
                    if (existing.getName().equalsIgnoreCase(category.getName())) {
                        exists = true;
                        Log.d("CategorySpinnerAdapter", "Skipping duplicate category: " + category.getName());
                        break;
                    }
                }
                if (!exists) {
                    this.categories.add(category);
                    Log.d("CategorySpinnerAdapter", "Added category: " + category.getName());
                }
            }
        }
        
        Log.d("CategorySpinnerAdapter", "Final adapter has " + this.categories.size() + " categories");
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.spinner_category_item, parent, false);
        }

        Category category = categories.get(position);
        
        TextView textCategoryName = convertView.findViewById(R.id.text_category_name);
        View colorIndicator = convertView.findViewById(R.id.category_color_indicator);

        textCategoryName.setText(category.getName());
        
        // Set color for indicator
        if (colorIndicator != null) {
            try {
                int color = android.graphics.Color.parseColor(category.getColor());
                colorIndicator.getBackground().setTint(color);
            } catch (Exception e) {
                // Default color if parsing fails
                colorIndicator.getBackground().setTint(android.graphics.Color.parseColor("#999999"));
            }
        }

        return convertView;
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories.clear();
        
        // Always add default "no category" option first  
        Category defaultCategory = new Category("không có thể loại", "#999999", 0, false);
        defaultCategory.setId(0);
        this.categories.add(defaultCategory);
        
        // Add only non-duplicate categories from new list
        if (newCategories != null) {
            for (Category category : newCategories) {
                // Skip the default "no category" entry if it exists in database
                if (category.getName().equalsIgnoreCase("không có thể loại")) {
                    continue;
                }
                
                // Skip if category name already exists (case insensitive)
                boolean exists = false;
                for (Category existing : this.categories) {
                    if (existing.getName().equalsIgnoreCase(category.getName())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    this.categories.add(category);
                }
            }
        }
        
        notifyDataSetChanged();
    }

    public Category getCategory(int position) {
        if (position >= 0 && position < categories.size()) {
            return categories.get(position);
        }
        return null;
    }

    public int getPositionForCategoryId(int categoryId) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == categoryId) {
                return i;
            }
        }
        return 0; // Default to "không có thể loại"
    }
}
