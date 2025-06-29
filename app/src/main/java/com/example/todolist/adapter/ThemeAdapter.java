package com.example.todolist.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.manager.ThemeManager;

import java.util.Arrays;
import java.util.List;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {
    
    public interface OnThemeSelectedListener {
        void onThemeSelected(ThemeManager.ThemeColor theme);
    }
    
    private Context context;
    private List<ThemeManager.ThemeColor> themes;
    private ThemeManager.ThemeColor selectedTheme;
    private OnThemeSelectedListener listener;
    
    public ThemeAdapter(Context context, ThemeManager.ThemeColor selectedTheme, OnThemeSelectedListener listener) {
        this.context = context;
        this.selectedTheme = selectedTheme;
        this.listener = listener;
        this.themes = Arrays.asList(ThemeManager.ThemeColor.values());
    }
    
    @NonNull
    @Override
    public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_theme, parent, false);
        return new ThemeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
        ThemeManager.ThemeColor theme = themes.get(position);
        holder.bind(theme);
    }
    
    @Override
    public int getItemCount() {
        return themes.size();
    }
    
    public void setSelectedTheme(ThemeManager.ThemeColor theme) {
        this.selectedTheme = theme;
        notifyDataSetChanged();
    }
    
    class ThemeViewHolder extends RecyclerView.ViewHolder {
        
        private CardView cardTheme;
        private View colorPreview;
        private TextView textThemeName;
        private ImageView iconSelected;
        private View gradientPreview;
        
        public ThemeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTheme = itemView.findViewById(R.id.card_theme);
            colorPreview = itemView.findViewById(R.id.color_preview);
            textThemeName = itemView.findViewById(R.id.text_theme_name);
            iconSelected = itemView.findViewById(R.id.icon_selected);
            gradientPreview = itemView.findViewById(R.id.gradient_preview);
        }
        
        public void bind(ThemeManager.ThemeColor theme) {
            // Set theme name
            textThemeName.setText(theme.getName());
            
            // Set color preview
            int primaryColor = context.getResources().getColor(theme.getPrimaryColorRes(), null);
            int secondaryColor = context.getResources().getColor(theme.getSecondaryColorRes(), null);
            int lightColor = context.getResources().getColor(theme.getLightColorRes(), null);
            
            colorPreview.setBackgroundColor(primaryColor);
            gradientPreview.setBackgroundColor(lightColor);
            
            // Show selection indicator
            boolean isSelected = theme == selectedTheme;
            iconSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            
            if (isSelected) {
                // Use elevation and background color for selection indicator
                cardTheme.setCardElevation(12f);
                cardTheme.setCardBackgroundColor(context.getResources().getColor(android.R.color.white, null));
                iconSelected.setColorFilter(primaryColor);
            } else {
                cardTheme.setCardElevation(4f);
                cardTheme.setCardBackgroundColor(context.getResources().getColor(android.R.color.white, null));
            }
            
            // Set click listener
            cardTheme.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onThemeSelected(theme);
                }
            });
            
            // Add ripple effect with theme color
            cardTheme.setForeground(context.getDrawable(android.R.drawable.list_selector_background));
        }
    }
}
