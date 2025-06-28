package com.example.todolist.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.todolist.MainActivity;
import com.example.todolist.R;

public class CalendarNavigationHelper {
    
    public static void setupBottomNavigation(Context context, 
                                           LinearLayout btnNavMenu,
                                           LinearLayout btnNavTasks, 
                                           LinearLayout btnNavCalendar,
                                           LinearLayout btnNavProfile) {
        
        // Set calendar as selected
        if (btnNavCalendar != null) {
            setNavigationSelected(btnNavCalendar);
        }
        
        if (btnNavTasks != null) {
            btnNavTasks.setOnClickListener(v -> {
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).finish();
                }
            });
        }
        
        if (btnNavMenu != null) {
            btnNavMenu.setOnClickListener(v -> {
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("open_drawer", true);
                context.startActivity(intent);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).finish();
                }
            });
        }
        
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> {
                Toast.makeText(context, "Của tôi", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private static void setNavigationSelected(LinearLayout navItem) {
        View icon = navItem.getChildAt(0);
        View text = navItem.getChildAt(1);
        
        if (icon instanceof ImageView) {
            ((ImageView) icon).setColorFilter(Color.parseColor("#4285F4"));
        }
        if (text instanceof TextView) {
            ((TextView) text).setTextColor(Color.parseColor("#4285F4"));
        }
    }
}
