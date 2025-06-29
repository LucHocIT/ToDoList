package com.example.todolist.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.todolist.MainActivity;
import com.example.todolist.R;

public class CalendarWidgetProvider extends AppWidgetProvider {
    
    public static final String ACTION_PREV_MONTH = "com.example.todolist.widget.PREV_MONTH";
    public static final String ACTION_NEXT_MONTH = "com.example.todolist.widget.NEXT_MONTH";
    public static final String ACTION_DAY_CLICK = "com.example.todolist.widget.DAY_CLICK";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Reset to current month on first update
        CalendarWidgetHelper.resetToCurrentMonth(context);
        
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }
    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        // Reset to current month when widget is first enabled
        CalendarWidgetHelper.resetToCurrentMonth(context);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        String action = intent.getAction();
        if (ACTION_PREV_MONTH.equals(action) || ACTION_NEXT_MONTH.equals(action) || ACTION_DAY_CLICK.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, CalendarWidgetProvider.class));
            
            if (ACTION_PREV_MONTH.equals(action)) {
                CalendarWidgetHelper.navigateMonth(context, -1);
            } else if (ACTION_NEXT_MONTH.equals(action)) {
                CalendarWidgetHelper.navigateMonth(context, 1);
            } else if (ACTION_DAY_CLICK.equals(action)) {
                // Open main app when clicking on a day
                Intent mainIntent = new Intent(context, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(mainIntent);
            }
            
            // Update all widgets
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }
    
    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_calendar);
        
        // Set month/year text
        String monthYear = CalendarWidgetHelper.getCurrentMonthYear(context);
        views.setTextViewText(R.id.widget_month_year, monthYear);
        
        // Set click listeners for navigation
        Intent prevIntent = new Intent(context, CalendarWidgetProvider.class);
        prevIntent.setAction(ACTION_PREV_MONTH);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(context, 0, prevIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_prev_month, prevPendingIntent);
        
        Intent nextIntent = new Intent(context, CalendarWidgetProvider.class);
        nextIntent.setAction(ACTION_NEXT_MONTH);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 0, nextIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_next_month, nextPendingIntent);
        
        // Set click listener for calendar grid (open app)
        Intent dayClickIntent = new Intent(context, CalendarWidgetProvider.class);
        dayClickIntent.setAction(ACTION_DAY_CLICK);
        PendingIntent dayClickPendingIntent = PendingIntent.getBroadcast(context, 0, dayClickIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_calendar_grid, dayClickPendingIntent);
        
        // Generate calendar
        CalendarWidgetHelper.generateCalendar(context, views);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
