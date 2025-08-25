package com.example.todolist.helper.calendar;

import android.content.Context;
import android.widget.LinearLayout;
import com.example.todolist.util.UnifiedNavigationHelper;

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
