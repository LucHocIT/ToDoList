package com.example.todolist.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.widget.RemoteViews;

import com.example.todolist.R;
import com.example.todolist.cache.TaskCache;
import com.example.todolist.helper.calendar.CalendarUtils;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WeekCalendarWidget extends AppWidgetProvider {

    private static final String WEEK_DAY_CLICK_ACTION = "com.example.todolist.WEEK_DAY_CLICK";
    private static final String PREV_WEEK_ACTION = "com.example.todolist.PREV_WEEK";
    private static final String NEXT_WEEK_ACTION = "com.example.todolist.NEXT_WEEK";
    
    private static final String WIDGET_PREFS = "week_calendar_widget_prefs_v2"; // Changed to v2 to clear old data
    private static final String PREF_CURRENT_YEAR = "current_year";
    private static final String PREF_CURRENT_MONTH = "current_month";
    private static final String PREF_CURRENT_WEEK = "current_week";
    private static final String PREF_SELECTED_DAY = "selected_day";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Load saved date or use current date
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        int currentYear = getIntSafely(prefs, PREF_CURRENT_YEAR, calendar.get(Calendar.YEAR));
        int currentMonth = getIntSafely(prefs, PREF_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        int currentWeek = getIntSafely(prefs, PREF_CURRENT_WEEK, calendar.get(Calendar.WEEK_OF_YEAR));
        int selectedDay = getIntSafely(prefs, PREF_SELECTED_DAY, calendar.get(Calendar.DAY_OF_MONTH));
        
        android.util.Log.d("WeekWidget", "onUpdate - currentYear: " + currentYear + 
                          ", currentMonth: " + currentMonth + 
                          ", currentWeek: " + currentWeek + 
                          ", selectedDay: " + selectedDay);
        
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, currentYear, currentMonth, currentWeek, selectedDay);
        }
    }
    
    // Helper method to safely read int from SharedPreferences (handles Long to Int conversion)
    private int getIntSafely(SharedPreferences prefs, String key, int defaultValue) {
        try {
            return prefs.getInt(key, defaultValue);
        } catch (ClassCastException e) {
            // Value was stored as Long, convert it
            try {
                long longValue = prefs.getLong(key, defaultValue);
                return (int) longValue;
            } catch (Exception ex) {
                return defaultValue;
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        int currentYear = getIntSafely(prefs, PREF_CURRENT_YEAR, calendar.get(Calendar.YEAR));
        int currentMonth = getIntSafely(prefs, PREF_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        int currentWeek = getIntSafely(prefs, PREF_CURRENT_WEEK, calendar.get(Calendar.WEEK_OF_YEAR));
        int selectedDay = getIntSafely(prefs, PREF_SELECTED_DAY, calendar.get(Calendar.DAY_OF_MONTH));

        if (WEEK_DAY_CLICK_ACTION.equals(intent.getAction())) {
            // Handle day selection - chọn ngày để xem task
            selectedDay = intent.getIntExtra("day", selectedDay);
            currentMonth = intent.getIntExtra("month", currentMonth);
            currentYear = intent.getIntExtra("year", currentYear);
            
            // Update current week based on selected date
            calendar.set(currentYear, currentMonth, selectedDay);
            currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
            
            // Save updated values
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_CURRENT_YEAR, currentYear);
            editor.putInt(PREF_CURRENT_MONTH, currentMonth);
            editor.putInt(PREF_CURRENT_WEEK, currentWeek);
            editor.putInt(PREF_SELECTED_DAY, selectedDay);
            editor.apply();
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, WeekCalendarWidget.class));
            
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, currentYear, currentMonth, currentWeek, selectedDay);
            }
        } else if (PREV_WEEK_ACTION.equals(intent.getAction())) {
            // Navigate to previous week - chuyển sang tuần trước (7 ngày trước)
            calendar.set(currentYear, currentMonth, selectedDay);
            calendar.add(Calendar.DAY_OF_MONTH, -7);
            
            currentYear = calendar.get(Calendar.YEAR);
            currentMonth = calendar.get(Calendar.MONTH);
            selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
            currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
            
            // Save updated values
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_CURRENT_YEAR, currentYear);
            editor.putInt(PREF_CURRENT_MONTH, currentMonth);
            editor.putInt(PREF_CURRENT_WEEK, currentWeek);
            editor.putInt(PREF_SELECTED_DAY, selectedDay);
            editor.apply();
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, WeekCalendarWidget.class));
            
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, currentYear, currentMonth, currentWeek, selectedDay);
            }
        } else if (NEXT_WEEK_ACTION.equals(intent.getAction())) {
            // Navigate to next week - chuyển sang tuần sau (7 ngày sau)
            calendar.set(currentYear, currentMonth, selectedDay);
            calendar.add(Calendar.DAY_OF_MONTH, 7);
            
            currentYear = calendar.get(Calendar.YEAR);
            currentMonth = calendar.get(Calendar.MONTH);
            selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
            currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
            
            // Save updated values
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_CURRENT_YEAR, currentYear);
            editor.putInt(PREF_CURRENT_MONTH, currentMonth);
            editor.putInt(PREF_CURRENT_WEEK, currentWeek);
            editor.putInt(PREF_SELECTED_DAY, selectedDay);
            editor.apply();
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, WeekCalendarWidget.class));
            
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, currentYear, currentMonth, currentWeek, selectedDay);
            }
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, 
                             int currentYear, int currentMonth, int currentWeek, int selectedDay) {
        
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_week_calendar_4x4);
        
        // Setup week calendar data
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(Calendar.YEAR, currentYear);
        selectedDate.set(Calendar.MONTH, currentMonth);
        selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay);
        
        // Get week start (Sunday)
        Calendar weekStart = (Calendar) selectedDate.clone();
        int dayOfWeek = weekStart.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = (dayOfWeek - Calendar.SUNDAY + 7) % 7;
        weekStart.add(Calendar.DAY_OF_MONTH, -daysToSubtract);
        
        // Setup month/year display - FIXED: Use correct date format
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        String monthYearText = monthYearFormat.format(selectedDate.getTime());
        views.setTextViewText(R.id.widget_week_month_year, monthYearText);
        
        // Setup week days
        Calendar today = Calendar.getInstance();
        int[] dayViewIds = {
            R.id.widget_week_day_0, R.id.widget_week_day_1, R.id.widget_week_day_2,
            R.id.widget_week_day_3, R.id.widget_week_day_4, R.id.widget_week_day_5, R.id.widget_week_day_6
        };
        
        int[] labelViewIds = {
            R.id.widget_week_label_0, R.id.widget_week_label_1, R.id.widget_week_label_2,
            R.id.widget_week_label_3, R.id.widget_week_label_4, R.id.widget_week_label_5, R.id.widget_week_label_6
        };
        
        for (int i = 0; i < 7; i++) {
            Calendar dayCalendar = (Calendar) weekStart.clone();
            dayCalendar.add(Calendar.DAY_OF_MONTH, i);
            
            int day = dayCalendar.get(Calendar.DAY_OF_MONTH);
            int dayMonth = dayCalendar.get(Calendar.MONTH);
            int dayYear = dayCalendar.get(Calendar.YEAR);
            
            boolean isToday = (dayYear == today.get(Calendar.YEAR) &&
                              dayCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR));
            boolean isSelected = (dayYear == selectedDate.get(Calendar.YEAR) &&
                                 dayMonth == selectedDate.get(Calendar.MONTH) &&
                                 day == selectedDate.get(Calendar.DAY_OF_MONTH));
            
            // Check if this day has tasks
            boolean hasTasks = hasTasksForDate(context, dayYear, dayMonth, day);
            
            // Display day with dot if has tasks
            if (hasTasks) {
                views.setTextViewText(dayViewIds[i], day + "\n•");
            } else {
                views.setTextViewText(dayViewIds[i], String.valueOf(day));
            }
            
            // Set color and background based on status
            if (isSelected) {
                // Ngày được chọn: gạch chân màu xanh
                views.setTextColor(dayViewIds[i], Color.parseColor("#4285F4"));
                views.setInt(dayViewIds[i], "setBackgroundResource", R.drawable.widget_day_underline);
                views.setTextColor(labelViewIds[i], Color.parseColor("#4285F4"));
            } else if (isToday) {
                // Ngày hôm nay: chữ màu xanh, không gạch chân
                views.setTextColor(dayViewIds[i], Color.parseColor("#4285F4"));
                views.setInt(dayViewIds[i], "setBackgroundResource", android.R.color.transparent);
                views.setTextColor(labelViewIds[i], Color.parseColor("#4285F4"));
            } else {
                // Ngày bình thường: chữ màu xám
                views.setTextColor(dayViewIds[i], Color.parseColor("#666666"));
                views.setInt(dayViewIds[i], "setBackgroundResource", android.R.color.transparent);
                views.setTextColor(labelViewIds[i], Color.parseColor("#666666"));
            }
            
            // Setup click listener for day
            Intent dayClickIntent = new Intent(context, WeekCalendarWidget.class);
            dayClickIntent.setAction(WEEK_DAY_CLICK_ACTION);
            dayClickIntent.putExtra("day", day);
            dayClickIntent.putExtra("month", dayMonth);
            dayClickIntent.putExtra("year", dayYear);
            
            PendingIntent dayPendingIntent = PendingIntent.getBroadcast(context, 
                100 + i, dayClickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(dayViewIds[i], dayPendingIntent);
        }
        
        // Load tasks for selected day
        android.util.Log.d("WeekWidget", "Before load - Year: " + selectedDate.get(Calendar.YEAR) + 
                          ", Month: " + selectedDate.get(Calendar.MONTH) + 
                          ", Day: " + selectedDate.get(Calendar.DAY_OF_MONTH));
        loadTasksForSelectedDay(context, views, selectedDate.get(Calendar.YEAR), 
                               selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));
        
        // Setup navigation buttons
        Intent prevWeekIntent = new Intent(context, WeekCalendarWidget.class);
        prevWeekIntent.setAction(PREV_WEEK_ACTION);
        PendingIntent prevWeekPendingIntent = PendingIntent.getBroadcast(context, 
            201, prevWeekIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_prev_week, prevWeekPendingIntent);
        
        Intent nextWeekIntent = new Intent(context, WeekCalendarWidget.class);
        nextWeekIntent.setAction(NEXT_WEEK_ACTION);
        PendingIntent nextWeekPendingIntent = PendingIntent.getBroadcast(context, 
            202, nextWeekIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_next_week, nextWeekPendingIntent);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void loadTasksForSelectedDay(Context context, RemoteViews views, int year, int month, int day) {
        // Format date string
        String selectedDateStr = String.format("%02d/%02d/%04d", day, month + 1, year);
        
        // Setup RemoteAdapter for ListView
        Intent serviceIntent = new Intent(context, WeekCalendarWidgetService.class);
        serviceIntent.putExtra("selected_date", selectedDateStr);
        
        String uniqueUri = "content://widget/week/" + day + "/" + month + "/" + year;
        serviceIntent.setData(Uri.parse(uniqueUri));
        
        try {
            views.setRemoteAdapter(R.id.widget_week_task_list, serviceIntent);
            views.setEmptyView(R.id.widget_week_task_list, R.id.widget_week_empty_text);
        } catch (Exception e) {
            android.util.Log.e("WeekWidget", "Error setting RemoteAdapter", e);
        }
        
        // Notify data changed
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
            new ComponentName(context, WeekCalendarWidget.class));
        for (int id : appWidgetIds) {
            appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_week_task_list);
        }
    }
    
    private boolean hasTasksForDate(Context context, int year, int month, int day) {
        String dateString = String.format("%02d/%02d/%04d", day, month + 1, year);
        
        TaskCache taskCache = TaskCache.getInstance();
        List<Task> allTasks = taskCache.getAllTasks();
        
        if (allTasks == null || allTasks.isEmpty()) {
            TaskService taskService = new TaskService(context, null);
            allTasks = taskService.getAllTasksFromCache();
        }
        
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