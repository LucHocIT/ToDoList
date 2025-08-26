package com.example.todolist;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;
import com.example.todolist.cache.TaskCache;
import com.example.todolist.helper.calendar.CalendarUtils;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import java.util.Calendar;
import java.util.List;

public class CalendarTaskWidget extends AppWidgetProvider {
    
    private static final String DAY_CLICK_ACTION = "com.example.todolist.DAY_CLICK";
    private static final String PREV_MONTH_ACTION = "com.example.todolist.PREV_MONTH";
    private static final String NEXT_MONTH_ACTION = "com.example.todolist.NEXT_MONTH";
    
    private static final String WIDGET_PREFS = "calendar_widget_prefs";
    private static final String PREF_CURRENT_MONTH = "current_month";
    private static final String PREF_CURRENT_YEAR = "current_year";
    private static final String PREF_SELECTED_DAY = "selected_day";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Load saved date or use current date
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        int currentYear = prefs.getInt(PREF_CURRENT_YEAR, calendar.get(Calendar.YEAR));
        int currentMonth = prefs.getInt(PREF_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        int selectedDay = prefs.getInt(PREF_SELECTED_DAY, calendar.get(Calendar.DAY_OF_MONTH));
        
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, currentYear, currentMonth, selectedDay);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        int currentYear = prefs.getInt(PREF_CURRENT_YEAR, calendar.get(Calendar.YEAR));
        int currentMonth = prefs.getInt(PREF_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        int selectedDay = prefs.getInt(PREF_SELECTED_DAY, calendar.get(Calendar.DAY_OF_MONTH));
        
        if (DAY_CLICK_ACTION.equals(intent.getAction())) {
            selectedDay = intent.getIntExtra("day", 1);
            currentMonth = intent.getIntExtra("month", 0);
            currentYear = intent.getIntExtra("year", 2025);
            
            // Save updated values
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_CURRENT_YEAR, currentYear);
            editor.putInt(PREF_CURRENT_MONTH, currentMonth);
            editor.putInt(PREF_SELECTED_DAY, selectedDay);
            editor.apply();
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, CalendarTaskWidget.class));
            
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, currentYear, currentMonth, selectedDay);
            }
        } else if (PREV_MONTH_ACTION.equals(intent.getAction())) {
            // Navigate to previous month (direction = -1)
            calendar.set(currentYear, currentMonth, 1);
            calendar.add(Calendar.MONTH, -1);  // ← Trừ 1 tháng
            currentYear = calendar.get(Calendar.YEAR);
            currentMonth = calendar.get(Calendar.MONTH);
            selectedDay = 1; // Reset to first day of month
            
            // Save updated values
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_CURRENT_YEAR, currentYear);
            editor.putInt(PREF_CURRENT_MONTH, currentMonth);
            editor.putInt(PREF_SELECTED_DAY, selectedDay);
            editor.apply();
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, CalendarTaskWidget.class));
            
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, currentYear, currentMonth, selectedDay);
            }
        } else if (NEXT_MONTH_ACTION.equals(intent.getAction())) {
            // Navigate to next month (direction = +1)
            calendar.set(currentYear, currentMonth, 1);
            calendar.add(Calendar.MONTH, 1);   // ← Cộng 1 tháng
            currentYear = calendar.get(Calendar.YEAR);
            currentMonth = calendar.get(Calendar.MONTH);
            selectedDay = 1; // Reset to first day of month
            
            // Save updated values
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_CURRENT_YEAR, currentYear);
            editor.putInt(PREF_CURRENT_MONTH, currentMonth);
            editor.putInt(PREF_SELECTED_DAY, selectedDay);
            editor.apply();
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, CalendarTaskWidget.class));
            
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, currentYear, currentMonth, selectedDay);
            }
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, 
                             int currentYear, int currentMonth, int selectedDay) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_calendar_task_4x2);
        
        updateCalendarView(context, views, currentYear, currentMonth, selectedDay);
        updateTaskList(context, views, appWidgetId, currentYear, currentMonth, selectedDay);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void updateCalendarView(Context context, RemoteViews views, int currentYear, int currentMonth, int selectedDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonth, 1);
        
        // Update month/year display
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                              "July", "August", "September", "October", "November", "December"};
        String monthYearText = monthNames[currentMonth] + " " + currentYear;
        views.setTextViewText(R.id.month_year_text, monthYearText);
        
        // Setup navigation buttons
        setupNavigationButtons(context, views);
        
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        int[] dayIds = {
            R.id.day_1, R.id.day_2, R.id.day_3, R.id.day_4, R.id.day_5, R.id.day_6, R.id.day_7,
            R.id.day_8, R.id.day_9, R.id.day_10, R.id.day_11, R.id.day_12, R.id.day_13, R.id.day_14,
            R.id.day_15, R.id.day_16, R.id.day_17, R.id.day_18, R.id.day_19, R.id.day_20, R.id.day_21,
            R.id.day_22, R.id.day_23, R.id.day_24, R.id.day_25, R.id.day_26, R.id.day_27, R.id.day_28,
            R.id.day_29, R.id.day_30, R.id.day_31, R.id.day_32, R.id.day_33, R.id.day_34, R.id.day_35,
            R.id.day_36, R.id.day_37, R.id.day_38, R.id.day_39, R.id.day_40, R.id.day_41, R.id.day_42
        };
        
        for (int dayId : dayIds) {
            views.setTextViewText(dayId, "");
            views.setInt(dayId, "setBackgroundResource", android.R.color.transparent);
        }
        
        int dayCounter = 1;
        
        for (int i = firstDayOfWeek; i < firstDayOfWeek + daysInMonth && i < dayIds.length; i++) {
            views.setTextViewText(dayIds[i], String.valueOf(dayCounter));
            
            if (dayCounter == selectedDay) {
                views.setInt(dayIds[i], "setBackgroundResource", R.drawable.selected_day_bg);
            } else {
                views.setInt(dayIds[i], "setBackgroundResource", android.R.color.transparent);
            }
            
            if (hasTasksForDate(context, currentYear, currentMonth, dayCounter)) {
                String dayWithDot = dayCounter + "\n•"; 
                views.setTextViewText(dayIds[i], dayWithDot);
                views.setTextColor(dayIds[i], 0xFF1565C0); 
            } else {
                views.setTextViewText(dayIds[i], String.valueOf(dayCounter));
                views.setTextColor(dayIds[i], 0xFF333333); 
            }
            
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
    }

    private void updateTaskList(Context context, RemoteViews views, int appWidgetId, 
                               int currentYear, int currentMonth, int selectedDay) {
        Intent serviceIntent = new Intent(context, CalendarTaskWidgetService.class);
        serviceIntent.putExtra("extra_day", selectedDay);
        serviceIntent.putExtra("extra_month", currentMonth);
        serviceIntent.putExtra("extra_year", currentYear);
        serviceIntent.putExtra("widget_id", appWidgetId);
        
        String uniqueUri = "content://widget/calendar/" + selectedDay + "/" + currentMonth + "/" + currentYear + "/" + appWidgetId;
        serviceIntent.setData(Uri.parse(uniqueUri));
        
        try {
            views.setRemoteAdapter(R.id.task_list, serviceIntent);
            views.setEmptyView(R.id.task_list, R.id.empty_text);
        } catch (Exception e) {
        }
        
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.task_list);
        
        new Thread(() -> {
            try {
                Thread.sleep(500);
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.task_list);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupNavigationButtons(Context context, RemoteViews views) {
        // Setup Previous Month button
        Intent prevMonthIntent = new Intent(context, CalendarTaskWidget.class);
        prevMonthIntent.setAction(PREV_MONTH_ACTION);
        PendingIntent prevMonthPendingIntent = PendingIntent.getBroadcast(context, 
            10001, prevMonthIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_prev_month, prevMonthPendingIntent);
        
        // Setup Next Month button
        Intent nextMonthIntent = new Intent(context, CalendarTaskWidget.class);
        nextMonthIntent.setAction(NEXT_MONTH_ACTION);
        PendingIntent nextMonthPendingIntent = PendingIntent.getBroadcast(context, 
            10002, nextMonthIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_next_month, nextMonthPendingIntent);
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
