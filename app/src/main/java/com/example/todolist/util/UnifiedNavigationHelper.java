package com.example.todolist.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.todolist.CalendarActivity;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.SettingsActivity;
import com.example.todolist.ThemeSelectionActivity;
import com.example.todolist.dialog.WidgetsDialog;
import com.example.todolist.manager.NavigationDrawerManager;

public class UnifiedNavigationHelper {
    
    private static NavigationDrawerManager sharedDrawerManager;
    
    /**
     * Setup bottom navigation for any activity
     * @param context Current activity context
     * @param btnNavMenu Menu button
     * @param btnNavTasks Tasks button  
     * @param btnNavCalendar Calendar button
     * @param btnNavProfile Profile button (optional)
     * @param currentScreen Current screen identifier ("tasks" or "calendar")
     */
    public static void setupBottomNavigation(Context context, 
                                           LinearLayout btnNavMenu,
                                           LinearLayout btnNavTasks, 
                                           LinearLayout btnNavCalendar,
                                           LinearLayout btnNavProfile,
                                           String currentScreen) {
        
        // Set current screen as selected
        setCurrentScreenSelected(currentScreen, btnNavTasks, btnNavCalendar);
        
        // Setup navigation button listeners
        setupNavigationListeners(context, btnNavMenu, btnNavTasks, btnNavCalendar, currentScreen);
    }
    
    private static void setCurrentScreenSelected(String currentScreen, 
                                               LinearLayout btnNavTasks, 
                                               LinearLayout btnNavCalendar) {
        switch (currentScreen) {
            case "calendar":
                if (btnNavCalendar != null) {
                    setNavigationSelected(btnNavCalendar);
                }
                break;
            case "tasks":
                if (btnNavTasks != null) {
                    setNavigationSelected(btnNavTasks);
                }
                break;
        }
    }
    
    private static void setupNavigationListeners(Context context,
                                                LinearLayout btnNavMenu,
                                                LinearLayout btnNavTasks, 
                                                LinearLayout btnNavCalendar,
                                                String currentScreen) {
        
        // Tasks navigation
        if (btnNavTasks != null) {
            btnNavTasks.setOnClickListener(v -> {
                if (!currentScreen.equals("tasks")) {
                    navigateToTasks(context);
                }
            });
        }
        
        // Calendar navigation
        if (btnNavCalendar != null) {
            btnNavCalendar.setOnClickListener(v -> {
                if (!currentScreen.equals("calendar")) {
                    navigateToCalendar(context);
                }
            });
        }
        
        // Menu navigation - Always use unified menu system
        if (btnNavMenu != null) {
            btnNavMenu.setOnClickListener(v -> {
                handleMenuClick(context);
            });
        }
    }
    
    private static void navigateToTasks(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).finish();
        }
    }
    
    private static void navigateToCalendar(Context context) {
        Intent intent = new Intent(context, CalendarActivity.class);
        context.startActivity(intent);
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).finish();
        }
    }
    
    /**
     * Unified menu handling - all activities use the same menu system
     */
    private static void handleMenuClick(Context context) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            
            // Try to find drawer layout in current activity
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            
            if (drawerLayout != null) {
                // If activity has drawer, open it directly
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                // If no drawer, navigate to MainActivity and open drawer
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("open_drawer", true);
                context.startActivity(intent);
                // Don't finish current activity so user can go back
            }
        }
    }
    
    /**
     * Initialize shared drawer manager for activities that have drawer
     */
    public static void initializeDrawerForActivity(AppCompatActivity activity, 
                                                  DrawerLayout drawerLayout,
                                                  NavigationDrawerManager.NavigationListener listener) {
        if (drawerLayout != null) {
            sharedDrawerManager = new NavigationDrawerManager(activity, drawerLayout, listener);
        }
    }
    
    /**
     * Check if current activity has drawer capability
     */
    public static boolean hasDrawerCapability(Context context) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            return drawerLayout != null;
        }
        return false;
    }
    
    private static void setNavigationSelected(LinearLayout navItem) {
        if (navItem == null) return;
        
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
