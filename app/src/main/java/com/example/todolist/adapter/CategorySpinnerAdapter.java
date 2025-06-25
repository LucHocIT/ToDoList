package com.example.todolist.adapter;

import android.content.Context;
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
        
        // Create default category object
        Category defaultCategory = new Category("không có thể loại", "#999999", 0, false);
        defaultCategory.setId(0); // Set ID after creation
        this.categories.add(defaultCategory);
        
        this.categories.addAll(categories);
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
        
        // Create default category object  
        Category defaultCategory = new Category("không có thể loại", "#999999", 0, false);
        defaultCategory.setId(0);
        this.categories.add(defaultCategory);
        
        this.categories.addAll(newCategories);
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
