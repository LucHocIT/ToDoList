package com.example.todolist.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class WidgetUpdateHelper {
    
    public static void updateAllWidgets(Context context) {
        updateCalendarWidget(context);
        updateMiniWidget(context);
    }
    
    public static void updateCalendarWidget(Context context) {
        Intent intent = new Intent(context, CalendarWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
            new ComponentName(context, CalendarWidgetProvider.class));
        
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }
    
    public static void updateMiniWidget(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
            new ComponentName(context, MiniWidgetProvider.class));
        
        for (int appWidgetId : appWidgetIds) {
            MiniWidgetProvider.updateWidget(context, appWidgetManager, appWidgetId);
        }
    }
}
