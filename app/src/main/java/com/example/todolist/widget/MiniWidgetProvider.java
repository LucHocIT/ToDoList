package com.example.todolist.widget;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import java.util.List;
/**
 * Enhanced Mini Widget Provider for 1x1 quick add task widget
 * Shows task count and provides a beautiful gradient design
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
        // Set click listener for add button first (for immediate response)
        Intent addIntent = new Intent(context, MiniWidgetProvider.class);
        addIntent.setAction(ACTION_QUICK_ADD);
        PendingIntent addPendingIntent = PendingIntent.getBroadcast(context, 0, addIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_mini_add_button, addPendingIntent);
        // Update widget immediately with basic setup
        appWidgetManager.updateAppWidget(appWidgetId, views);
        // Get task count in background and update again
        new Thread(() -> {
            try {
                // Create TaskService instance for data access
                TaskService taskService = new TaskService(context, new TaskService.TaskUpdateListener() {
                    @Override
                    public void onTasksUpdated() {
                        // Widget update handled elsewhere
                    }
                    @Override
                    public void onError(String error) {
                        // Handle error silently for widget
                    }
                });
                List<Task> incompleteTasks = taskService.getIncompleteTasks();
                int taskCount = incompleteTasks != null ? incompleteTasks.size() : 0;
                // Update UI on main thread
                RemoteViews updatedViews = new RemoteViews(context.getPackageName(), R.layout.widget_mini);
                // Set task count with smart display logic
                if (taskCount > 0) {
                    String countText = taskCount > 99 ? "99+" : String.valueOf(taskCount);
                    updatedViews.setTextViewText(R.id.widget_mini_task_count, countText);
                    updatedViews.setViewVisibility(R.id.widget_mini_task_count, android.view.View.VISIBLE);
                } else {
                    updatedViews.setViewVisibility(R.id.widget_mini_task_count, android.view.View.GONE);
                }
                // Re-set click listener
                updatedViews.setOnClickPendingIntent(R.id.widget_mini_add_button, addPendingIntent);
                // Update widget with task count
                appWidgetManager.updateAppWidget(appWidgetId, updatedViews);
            } catch (Exception e) {
                e.printStackTrace();
                // Error case handled by initial update above
            }
        }).start();
    }
}
