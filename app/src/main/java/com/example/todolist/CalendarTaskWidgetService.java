package com.example.todolist;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.example.todolist.cache.TaskCache;
import com.example.todolist.helper.calendar.CalendarUtils;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import java.util.ArrayList;
import java.util.List;

public class CalendarTaskWidgetService extends RemoteViewsService {
    
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CalendarTaskRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class CalendarTaskRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    
    private Context context;
    private List<Task> tasks;
    private TaskService taskService;
    private TaskCache taskCache;
    private int selectedDay;
    private int selectedMonth;
    private int selectedYear;
    
    public CalendarTaskRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.selectedDay = intent.getIntExtra("extra_day", 1);
        this.selectedMonth = intent.getIntExtra("extra_month", 0);
        this.selectedYear = intent.getIntExtra("extra_year", 2025);
        this.taskService = new TaskService(context, null);
        this.taskCache = TaskCache.getInstance();
        this.tasks = new ArrayList<>();
    }
    
    @Override
    public void onCreate() {
        loadTasksForDate();
    }
    
    @Override
    public void onDataSetChanged() {
        if (tasks != null) {
            tasks.clear();
        }
        loadTasksForDate();
    }
    
    @Override
    public void onDestroy() {
        if (tasks != null) {
            tasks.clear();
        }
    }
    
    @Override
    public int getCount() {
        return tasks != null ? tasks.size() : 0;
    }
    
    @Override
    public RemoteViews getViewAt(int position) {
        if (tasks == null || position >= tasks.size()) {
            return null;
        }
        
        Task task = tasks.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_item_simple);
        
        // Set task title
        views.setTextViewText(R.id.widget_task_title, task.getTitle());

        // Set task time if available
        if (task.getDueTime() != null && !task.getDueTime().isEmpty()) {
            views.setTextViewText(R.id.widget_task_time, task.getDueTime());
            views.setViewVisibility(R.id.widget_task_time, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_task_time, View.GONE);
        }

        // Set priority timeline bar color (left side)
        int priorityColor = getPriorityColor(task.getPriority());
        views.setInt(R.id.widget_task_priority_indicator, "setBackgroundColor", priorityColor);

        // Set completion circle
        if (task.isCompleted()) {
            views.setTextViewText(R.id.widget_task_checkbox, "✓");
            views.setTextColor(R.id.widget_task_checkbox, 0xFFFFFFFF); // White checkmark
            views.setInt(R.id.widget_task_checkbox, "setBackgroundResource", R.drawable.circle_completed);
        } else {
            views.setTextViewText(R.id.widget_task_checkbox, "○");
            views.setTextColor(R.id.widget_task_checkbox, 0xFF666666); // Gray circle
            views.setInt(R.id.widget_task_checkbox, "setBackgroundResource", android.R.color.transparent);
        }

        // Set click intent to open task details
        Intent taskDetailIntent = new Intent(context, MainActivity.class);
        taskDetailIntent.putExtra("task_id", task.getId());
        taskDetailIntent.putExtra("open_task_detail", true);
        taskDetailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            task.getId().hashCode(), 
            taskDetailIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Make the entire item clickable
        views.setOnClickPendingIntent(R.id.widget_task_title, pendingIntent);
        
        return views;
    }
    
    @Override
    public RemoteViews getLoadingView() {
        return null;
    }
    
    @Override
    public int getViewTypeCount() {
        return 1;
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public boolean hasStableIds() {
        return true;
    }
    
    private void loadTasksForDate() {
        String dateString = String.format("%02d/%02d/%04d", 
            selectedDay, selectedMonth + 1, selectedYear);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        taskService.loadTasks();
        
        int retryCount = 0;
        List<Task> allTasks = null;
        while (retryCount < 5) {
            allTasks = taskService.getAllTasksFromCache();
            if (allTasks != null && allTasks.size() > 0) {
                break;
            }
            try {
                Thread.sleep(200);
                retryCount++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        tasks = new ArrayList<>();
        
        if (allTasks != null && allTasks.size() > 0) {
            for (Task task : allTasks) {
                boolean isMatch = CalendarUtils.isTaskOnDate(task, dateString);
                if (isMatch) {
                    tasks.add(task);
                }
            }
        } else {
            List<Task> directCacheTasks = taskCache.getAllTasks();
            if (directCacheTasks != null && directCacheTasks.size() > 0) {
                for (Task task : directCacheTasks) {
                    boolean isMatch = CalendarUtils.isTaskOnDate(task, dateString);
                    if (isMatch) {
                        tasks.add(task);
                    }
                }
            }
        }

        // Sort tasks: uncompleted first, then completed
        List<Task> uncompletedTasks = new ArrayList<>();
        List<Task> completedTasks = new ArrayList<>();
        
        for (Task task : tasks) {
            if (task.isCompleted()) {
                completedTasks.add(task);
            } else {
                uncompletedTasks.add(task);
            }
        }

        tasks.clear();
        tasks.addAll(uncompletedTasks);
        tasks.addAll(completedTasks);
    }
    
    private int getPriorityColor(String priority) {
        if (priority == null) {
            return 0xFF4CAF50; // Green for no priority
        }
        
        switch (priority.toLowerCase()) {
            case "cao":
            case "high":
                return 0xFFFF5252; // Red
            case "trung bình":
            case "medium":
                return 0xFFFF9800; // Orange
            case "thấp":
            case "low":
                return 0xFF4CAF50; // Green
            default:
                return 0xFF4CAF50; // Green
        }
    }
}
