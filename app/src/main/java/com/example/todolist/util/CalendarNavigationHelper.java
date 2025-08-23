package com.example.todolist.util;
import android.content.Context;
import android.widget.LinearLayout;
public class CalendarNavigationHelper {
    public static void setupBottomNavigation(Context context, 
                                           LinearLayout btnNavMenu,
                                           LinearLayout btnNavTasks, 
                                           LinearLayout btnNavCalendar,
                                           LinearLayout btnNavProfile) {
        // Use unified navigation helper
        UnifiedNavigationHelper.setupBottomNavigation(context, btnNavMenu, btnNavTasks, 
                                                    btnNavCalendar, btnNavProfile, "calendar");
    }
}
