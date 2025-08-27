package com.example.todolist;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;

import com.example.todolist.R;
import com.example.todolist.MainActivity;
import com.example.todolist.CalendarActivity;
import com.example.todolist.activities.ProfileActivity;

/**
 * Lớp quản lý Bottom Navigation chung cho toàn bộ ứng dụng
 * Thay thế cho các lớp navigation cũ và thống nhất UI/UX
 */
public class BottomNavigationManager {
    
    // Các màn hình chính - theo thứ tự: Menu, Nhiệm vụ, Lịch, Của tôi
    public static final String SCREEN_MENU = "menu";
    public static final String SCREEN_TASKS = "tasks";
    public static final String SCREEN_CALENDAR = "calendar";
    public static final String SCREEN_PROFILE = "profile";
    
    private AppCompatActivity activity;
    private String currentScreen;
    
    // Navigation buttons
    private LinearLayout btnNavTasks;
    private LinearLayout btnNavCalendar;
    private LinearLayout btnNavProfile;
    private LinearLayout btnNavMenu;
    
    // Navigation icons
    private ImageView imgNavTasks;
    private ImageView imgNavCalendar;
    private ImageView imgNavProfile;
    private ImageView imgNavMenu;
    
    // Navigation texts
    private TextView textNavTasks;
    private TextView textNavCalendar;
    private TextView textNavProfile;
    private TextView textNavMenu;
    
    // Colors
    private int selectedColor;
    private int unselectedColor;
    
    public BottomNavigationManager(AppCompatActivity activity, String currentScreen) {
        this.activity = activity;
        this.currentScreen = currentScreen;
        
        // Initialize colors
        selectedColor = ContextCompat.getColor(activity, R.color.primary);
        unselectedColor = ContextCompat.getColor(activity, R.color.text_gray);
        
        initViews();
        setupClickListeners();
        updateSelectedState();
    }
    
    private void initViews() {
        // Find navigation buttons - theo thứ tự: Menu, Nhiệm vụ, Lịch, Của tôi
        btnNavMenu = activity.findViewById(R.id.btn_nav_menu);
        btnNavTasks = activity.findViewById(R.id.btn_nav_tasks);
        btnNavCalendar = activity.findViewById(R.id.btn_nav_calendar);
        btnNavProfile = activity.findViewById(R.id.btn_nav_profile);
        
        // Find navigation icons
        imgNavMenu = activity.findViewById(R.id.img_nav_menu);
        imgNavTasks = activity.findViewById(R.id.img_nav_tasks);
        imgNavCalendar = activity.findViewById(R.id.img_nav_calendar);
        imgNavProfile = activity.findViewById(R.id.img_nav_profile);
        
        // Find navigation texts
        textNavMenu = activity.findViewById(R.id.text_nav_menu);
        textNavTasks = activity.findViewById(R.id.text_nav_tasks);
        textNavCalendar = activity.findViewById(R.id.text_nav_calendar);
        textNavProfile = activity.findViewById(R.id.text_nav_profile);
    }
    
    private void setupClickListeners() {
        if (btnNavMenu != null) {
            btnNavMenu.setOnClickListener(v -> navigateToScreen(SCREEN_MENU));
        }
        
        if (btnNavTasks != null) {
            btnNavTasks.setOnClickListener(v -> navigateToScreen(SCREEN_TASKS));
        }
        
        if (btnNavCalendar != null) {
            btnNavCalendar.setOnClickListener(v -> navigateToScreen(SCREEN_CALENDAR));
        }
        
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> navigateToScreen(SCREEN_PROFILE));
        }
    }
    
    private void navigateToScreen(String targetScreen) {
        android.util.Log.d("BottomNav", "navigateToScreen called: current=" + currentScreen + ", target=" + targetScreen);
        
        // Không navigate nếu đã ở màn hình hiện tại
        if (currentScreen.equals(targetScreen)) {
            android.util.Log.d("BottomNav", "Already on target screen, ignoring");
            return;
        }
        
        Intent intent = null;
        
        switch (targetScreen) {
            case SCREEN_TASKS:
                intent = new Intent(activity, MainActivity.class);
                break;
                
            case SCREEN_CALENDAR:
                intent = new Intent(activity, CalendarActivity.class);
                break;
                
            case SCREEN_PROFILE:
                intent = new Intent(activity, ProfileActivity.class);
                break;
                
            case SCREEN_MENU:
                // Mở navigation drawer nếu có - tất cả activity đều có drawer
                DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
                if (drawerLayout != null) {
                    android.util.Log.d("BottomNav", "Opening drawer");
                    // Cập nhật UI state trước khi mở drawer
                    updateCurrentScreen(SCREEN_MENU);
                    drawerLayout.openDrawer(GravityCompat.START);
                    return; // Không navigate, chỉ mở drawer
                } else {
                    android.util.Log.d("BottomNav", "No drawer found, fallback to MainActivity");
                    // Nếu không có drawer, fallback về MainActivity
                    intent = new Intent(activity, MainActivity.class);
                    intent.putExtra("open_drawer", true);
                }
                break;
        }
        
        if (intent != null) {
            android.util.Log.d("BottomNav", "Starting new activity for " + targetScreen);
            // Tắt hiệu ứng transition
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0); // Tắt hiệu ứng chuyển màn hình
            activity.finish(); // Đóng activity hiện tại để tránh stack overflow
        }
    }
    
    private void updateSelectedState() {
        android.util.Log.d("BottomNav", "updateSelectedState called for screen: " + currentScreen);
        
        // Reset tất cả về trạng thái unselected
        resetAllToUnselected();

        switch (currentScreen) {
            case SCREEN_MENU:
                android.util.Log.d("BottomNav", "Setting MENU as selected");
                setSelected(imgNavMenu, textNavMenu);
                break;
                
            case SCREEN_TASKS:
                android.util.Log.d("BottomNav", "Setting TASKS as selected");
                setSelected(imgNavTasks, textNavTasks);
                break;
                
            case SCREEN_CALENDAR:
                android.util.Log.d("BottomNav", "Setting CALENDAR as selected");
                setSelected(imgNavCalendar, textNavCalendar);
                break;
                
            case SCREEN_PROFILE:
                android.util.Log.d("BottomNav", "Setting PROFILE as selected");
                setSelected(imgNavProfile, textNavProfile);
                break;
        }
    }
    
    private void resetAllToUnselected() {
        setUnselected(imgNavMenu, textNavMenu);
        setUnselected(imgNavTasks, textNavTasks);
        setUnselected(imgNavCalendar, textNavCalendar);
        setUnselected(imgNavProfile, textNavProfile);
    }
    
    private void setSelected(ImageView icon, TextView text) {
        if (icon != null) {
            android.util.Log.d("BottomNav", "Setting icon " + getResourceName(icon) + " to selected color");
            icon.setColorFilter(selectedColor);
        }
        if (text != null) {
            android.util.Log.d("BottomNav", "Setting text " + getResourceName(text) + " to selected color");
            text.setTextColor(selectedColor);
        }
    }
    
    private void setUnselected(ImageView icon, TextView text) {
        if (icon != null) {
            android.util.Log.d("BottomNav", "Setting icon " + getResourceName(icon) + " to unselected color");
            icon.setColorFilter(unselectedColor);
        }
        if (text != null) {
            android.util.Log.d("BottomNav", "Setting text " + getResourceName(text) + " to unselected color");
            text.setTextColor(unselectedColor);
        }
    }
    
    private String getResourceName(android.view.View view) {
        try {
            return activity.getResources().getResourceEntryName(view.getId());
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Cập nhật màn hình hiện tại và refresh UI
     */
    public void updateCurrentScreen(String newScreen) {
        this.currentScreen = newScreen;
        updateSelectedState();
    }
    
    /**
     * Setup bottom navigation cho activity
     * Method này sẽ thay thế cho UnifiedNavigationHelper
     */
    public static void setupForActivity(AppCompatActivity activity, String currentScreen) {
        new BottomNavigationManager(activity, currentScreen);
    }
}
