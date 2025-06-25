package com.example.todolist.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.model.Category;

import java.util.List;

public class CategorySelectionAdapter extends RecyclerView.Adapter<CategorySelectionAdapter.CategoryViewHolder> {
    
    public interface OnCategorySelectedListener {
        void onCategorySelected(Category category);
    }
    
    private List<Category> categories;
    private Context context;
    private OnCategorySelectedListener listener;
    private int selectedPosition = -1;
    
    public CategorySelectionAdapter(List<Category> categories, Context context, OnCategorySelectedListener listener) {
        this.categories = categories;
        this.context = context;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_selection, parent, false);
        return new CategoryViewHolder(view);
    }
      @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        if (categories == null || position >= categories.size()) return;
        
        Category category = categories.get(position);
        
        holder.tvCategoryName.setText(category.getName());
        holder.radioCategory.setChecked(position == selectedPosition);
        
        // Set category color indicator
        GradientDrawable colorDrawable = new GradientDrawable();
        colorDrawable.setShape(GradientDrawable.OVAL);
        try {
            colorDrawable.setColor(Color.parseColor(category.getColor()));
        } catch (Exception e) {
            colorDrawable.setColor(Color.parseColor("#4285F4")); // Default color
        }
        holder.categoryColorIndicator.setBackground(colorDrawable);
        
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            
            // Update radio buttons
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            
            if (listener != null) {
                listener.onCategorySelected(category);
            }
        });
    }
      @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }
      public Category getSelectedCategory() {
        if (selectedPosition >= 0 && categories != null && selectedPosition < categories.size()) {
            return categories.get(selectedPosition);
        }
        return null;
    }
    
    public void setSelectedCategory(Category category) {
        if (categories == null) return;
        
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == category.getId()) {
                int previousSelected = selectedPosition;
                selectedPosition = i;
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }
    
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioCategory;
        View categoryColorIndicator;
        TextView tvCategoryName;
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            radioCategory = itemView.findViewById(R.id.radio_category);
            categoryColorIndicator = itemView.findViewById(R.id.category_color_indicator);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
        }
    }
}
