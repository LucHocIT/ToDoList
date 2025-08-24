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
        Category defaultCategory = new Category("Không có thể loại", "#999999", 0, false);
        defaultCategory.setId("0");
        this.categories.add(defaultCategory);
        if (categories != null) {
            for (Category category : categories) {
                if (category.getName().equalsIgnoreCase("không có thể loại") || 
                    category.getName().equalsIgnoreCase("Khổng có thể loại")) {
                    continue;
                }
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
        if (colorIndicator != null) {
            try {
                int color = android.graphics.Color.parseColor(category.getColor());
                colorIndicator.getBackground().setTint(color);
            } catch (Exception e) {
                colorIndicator.getBackground().setTint(android.graphics.Color.parseColor("#999999"));
            }
        }
        return convertView;
    }
    public void updateCategories(List<Category> newCategories) {
        this.categories.clear();
        Category defaultCategory = new Category("Không có thể loại", "#999999", 0, false);
        defaultCategory.setId("0");
        this.categories.add(defaultCategory);
        if (newCategories != null) {
            List<String> addedNames = new ArrayList<>();
            addedNames.add("không có thể loại"); 
            for (Category category : newCategories) {
                String categoryNameLower = category.getName().toLowerCase().trim();
                boolean alreadyExists = false;
                for (String addedName : addedNames) {
                    if (addedName.equalsIgnoreCase(categoryNameLower)) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists) {
                    this.categories.add(category);
                    addedNames.add(categoryNameLower);
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
            if (categories.get(i).getId().equals(String.valueOf(categoryId))) {
                return i;
            }
        }
        return 0; 
    }
}
