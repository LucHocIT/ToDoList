package com.example.todolist.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.todolist.MainActivity;
import com.example.todolist.R;

/**
 * Mini Widget Provider for 1x1 quick add task widget
 * Provides a simple button to quickly add tasks from home screen
 */
public class MiniWidgetProvider extends AppWidgetProvider {
    
    public static final String ACTION_QUICK_ADD = "com.example.todolist.widget.QUICK_ADD";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        String action = intent.getAction();
        if (ACTION_QUICK_ADD.equals(action)) {
            // Open main app with quick add intent
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.setAction("com.example.todolist.QUICK_ADD_TASK");
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(mainIntent);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_mini);
        
        // Set click listener for add button
        Intent addIntent = new Intent(context, MiniWidgetProvider.class);
        addIntent.setAction(ACTION_QUICK_ADD);
        PendingIntent addPendingIntent = PendingIntent.getBroadcast(context, 0, addIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_mini_add_button, addPendingIntent);
        
        // Update widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
