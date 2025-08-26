package com.example.todolist;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import com.example.todolist.cache.TaskCache;
import com.example.todolist.helper.calendar.CalendarUtils;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import java.util.Calendar;
import java.util.List;

public class CalendarTaskWidget extends AppWidgetProvider {
    
    private static final String TAG = "CalendarTaskWidget";
    private static final String DAY_CLICK_ACTION = "com.example.todolist.DAY_CLICK";
    
    private int currentYear;
    private int currentMonth;
    private int selectedDay = 1;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() called");
        
        // Initialize current date
        Calendar calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH);
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
        
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        Log.d(TAG, "onReceive() called with action: " + intent.getAction());
        
        if (DAY_CLICK_ACTION.equals(intent.getAction())) {
            selectedDay = intent.getIntExtra("day", 1);
            currentMonth = intent.getIntExtra("month", 0);
            currentYear = intent.getIntExtra("year", 2025);
            
            Log.d(TAG, "Day clicked: " + selectedDay + "/" + (currentMonth + 1) + "/" + currentYear);
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, CalendarTaskWidget.class));
            
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "updateWidget() called for widget ID: " + appWidgetId);
        
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_calendar_task_4x2);
        
        // Update calendar view
        updateCalendarView(context, views);
        
        // Update task list
        updateTaskList(context, views, appWidgetId);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
        Log.d(TAG, "Widget updated successfully");
    }

    private void updateCalendarView(Context context, RemoteViews views) {
        Log.d(TAG, "updateCalendarView() called for " + (currentMonth + 1) + "/" + currentYear);
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonth, 1);
        
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Calendar day IDs array
        int[] dayIds = {
            R.id.day_1, R.id.day_2, R.id.day_3, R.id.day_4, R.id.day_5, R.id.day_6, R.id.day_7,
            R.id.day_8, R.id.day_9, R.id.day_10, R.id.day_11, R.id.day_12, R.id.day_13, R.id.day_14,
            R.id.day_15, R.id.day_16, R.id.day_17, R.id.day_18, R.id.day_19, R.id.day_20, R.id.day_21,
            R.id.day_22, R.id.day_23, R.id.day_24, R.id.day_25, R.id.day_26, R.id.day_27, R.id.day_28,
            R.id.day_29, R.id.day_30, R.id.day_31, R.id.day_32, R.id.day_33, R.id.day_34, R.id.day_35,
            R.id.day_36, R.id.day_37, R.id.day_38, R.id.day_39, R.id.day_40, R.id.day_41, R.id.day_42
        };
        
        // Clear all day views first
        for (int dayId : dayIds) {
            views.setTextViewText(dayId, "");
            views.setInt(dayId, "setBackgroundResource", android.R.color.transparent);
        }
        
        int dayCounter = 1;
        
        // Fill in the calendar days
        for (int i = firstDayOfWeek; i < firstDayOfWeek + daysInMonth && i < dayIds.length; i++) {
            views.setTextViewText(dayIds[i], String.valueOf(dayCounter));
            
            // Highlight selected day
            if (dayCounter == selectedDay) {
                views.setInt(dayIds[i], "setBackgroundResource", R.drawable.selected_day_bg);
            } else {
                views.setInt(dayIds[i], "setBackgroundResource", android.R.color.transparent);
            }
            
            // Check if day has tasks and add indicator
            if (hasTasksForDate(context, currentYear, currentMonth, dayCounter)) {
                views.setTextViewText(dayIds[i], dayCounter + "•");
            }
            
            // Set click listener for day
            Intent dayClickIntent = new Intent(context, CalendarTaskWidget.class);
            dayClickIntent.setAction(DAY_CLICK_ACTION);
            dayClickIntent.putExtra("day", dayCounter);
            dayClickIntent.putExtra("month", currentMonth);
            dayClickIntent.putExtra("year", currentYear);
            
            PendingIntent dayClickPendingIntent = PendingIntent.getBroadcast(context, 
                dayCounter, dayClickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(dayIds[i], dayClickPendingIntent);
            
            dayCounter++;
        }
        
        Log.d(TAG, "Calendar view updated with " + daysInMonth + " days");
    }

    private void updateTaskList(Context context, RemoteViews views, int appWidgetId) {
        Log.d(TAG, "updateTaskList() called for date: " + selectedDay + "/" + (currentMonth + 1) + "/" + currentYear);
        
        // Set up the remote adapter for the ListView
        Intent serviceIntent = new Intent(context, CalendarTaskWidgetService.class);
        serviceIntent.putExtra("extra_day", selectedDay);
        serviceIntent.putExtra("extra_month", currentMonth);
        serviceIntent.putExtra("extra_year", currentYear);
        serviceIntent.putExtra("widget_id", appWidgetId); // Add widget ID for better tracking
        
        // Create unique URI based on date to force factory recreation
        String uniqueUri = "content://widget/calendar/" + selectedDay + "/" + currentMonth + "/" + currentYear + "/" + appWidgetId;
        serviceIntent.setData(Uri.parse(uniqueUri));
        
        Log.d(TAG, "Setting remote adapter with intent extras - day: " + selectedDay + ", month: " + currentMonth + ", year: " + currentYear);
        Log.d(TAG, "Intent URI: " + uniqueUri);
        
        try {
            views.setRemoteAdapter(R.id.task_list, serviceIntent);
            views.setEmptyView(R.id.task_list, R.id.empty_text);
            Log.d(TAG, "Remote adapter set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting remote adapter: " + e.getMessage());
        }
        
        // Force refresh the list view multiple times to ensure data is loaded
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.task_list);
        
        // Add a delayed refresh to handle cache loading delays
        new Thread(() -> {
            try {
                Thread.sleep(500); // Wait for cache to be ready
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.task_list);
                Log.d(TAG, "Delayed widget data refresh completed");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        
        Log.d(TAG, "Task list updated with service intent and notified view data changed");
    }

    private boolean hasTasksForDate(Context context, int year, int month, int day) {
        String dateString = String.format("%02d/%02d/%04d", day, month + 1, year);
        
        TaskService taskService = new TaskService(context, null);
        List<Task> allTasks = taskService.getAllTasksFromCache();
        
        if (allTasks != null) {
            for (Task task : allTasks) {
                if (CalendarUtils.isTaskOnDate(task, dateString)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
