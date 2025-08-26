package com.example.todolist;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.example.todolist.cache.TaskCache;
import com.example.todolist.helper.calendar.CalendarUtils;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarTaskWidgetService extends RemoteViewsService {
    
    private static final String TAG = "CalendarTaskWidget";
    
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.d(TAG, "=== CalendarTaskWidgetService.onGetViewFactory() called ===");
        Log.d(TAG, "Intent: " + intent.toString());
        if (intent.getExtras() != null) {
            Log.d(TAG, "Intent extras: " + intent.getExtras().toString());
        } else {
            Log.d(TAG, "Intent extras: null");
        }
        return new CalendarTaskRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class CalendarTaskRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    
    private static final String TAG = "CalendarTaskFactory";
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
        
        Log.d(TAG, "=== CalendarTaskRemoteViewsFactory Constructor ===");
        Log.d(TAG, "Selected date: " + selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
        Log.d(TAG, "Intent extras: day=" + intent.getIntExtra("extra_day", -1) + 
              ", month=" + intent.getIntExtra("extra_month", -1) + 
              ", year=" + intent.getIntExtra("extra_year", -1));
    }
    
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        loadTasksForDate();
    }
    
    @Override
    public void onDataSetChanged() {
        // Get fresh date values from the current intent
        // Note: We need to get these from somewhere, but RemoteViewsFactory doesn't 
        // provide access to new intent data in onDataSetChanged
        // This is a limitation of Android's widget architecture
        
        Log.d(TAG, "onDataSetChanged() called for date: " + selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
        
        // Clear existing tasks
        if (tasks != null) {
            tasks.clear();
        }
        
        // Reload tasks when data changes
        loadTasksForDate();
        
        Log.d(TAG, "onDataSetChanged() completed with " + (tasks != null ? tasks.size() : 0) + " tasks");
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        if (tasks != null) {
            tasks.clear();
        }
    }
    
    @Override
    public int getCount() {
        int count = tasks != null ? tasks.size() : 0;
        Log.d(TAG, "getCount() returning: " + count);
        return count;
    }
    
    @Override
    public RemoteViews getViewAt(int position) {
        Log.d(TAG, "getViewAt() called for position: " + position);
        
        if (tasks == null || position >= tasks.size()) {
            Log.d(TAG, "getViewAt() returning null - tasks null or position out of bounds");
            return null;
        }
        
        Task task = tasks.get(position);
        Log.d(TAG, "getViewAt() creating view for task: " + task.getTitle());
        
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

        // Set priority color
        int priorityColor = getPriorityColor(task.getPriority());
        views.setInt(R.id.widget_task_priority_indicator, "setBackgroundColor", priorityColor);

        // Set checkbox state using TextView
        String checkboxText = task.isCompleted() ? "☑" : "☐";
        views.setTextViewText(R.id.widget_task_checkbox, checkboxText);
        
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
        Log.d(TAG, "loadTasksForDate() called");
        
        // Create date string for comparison
        String dateString = String.format("%02d/%02d/%04d", 
            selectedDay, selectedMonth + 1, selectedYear);
        Log.d(TAG, "Looking for tasks on date: " + dateString);
        
        // Wait a bit for cache to be ready and force reload
        try {
            Thread.sleep(100); // Small delay to ensure cache is ready
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Force reload cache first
        taskService.loadTasks();
        
        // Wait for cache to be loaded
        int retryCount = 0;
        List<Task> allTasks = null;
        while (retryCount < 5) {
            allTasks = taskService.getAllTasksFromCache();
            if (allTasks != null && allTasks.size() > 0) {
                break;
            }
            try {
                Thread.sleep(200); // Wait 200ms before retry
                retryCount++;
                Log.d(TAG, "Retry " + retryCount + " - waiting for cache to load...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        Log.d(TAG, "Total tasks from cache: " + (allTasks != null ? allTasks.size() : 0));
        
        tasks = new ArrayList<>();
        
        if (allTasks != null && allTasks.size() > 0) {
            Log.d(TAG, "=== Checking all tasks for date match ===");
            for (Task task : allTasks) {
                Log.d(TAG, "Task ID: " + task.getId());
                Log.d(TAG, "Task Title: " + task.getTitle());
                Log.d(TAG, "Task Due Date: " + task.getDueDate());
                Log.d(TAG, "Task Is Repeating: " + task.isRepeating());
                Log.d(TAG, "Task Repeat Type: " + task.getRepeatType());
                Log.d(TAG, "Task Is Completed: " + task.isCompleted());
                Log.d(TAG, "Target date for comparison: " + dateString);
                
                boolean isMatch = CalendarUtils.isTaskOnDate(task, dateString);
                Log.d(TAG, "Does task match date? " + isMatch);
                
                if (isMatch) {
                    tasks.add(task);
                    Log.d(TAG, "✓ Task added to widget list: " + task.getTitle());
                } else {
                    Log.d(TAG, "✗ Task does not match date: " + task.getTitle());
                    // Debug why it doesn't match
                    if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                        Log.d(TAG, "  Reason: Due date '" + task.getDueDate() + "' != target '" + dateString + "'");
                        if (task.isRepeating() && task.getRepeatType() != null && 
                            !task.getRepeatType().equals("Không") && !task.getRepeatType().equals("Không có")) {
                            Log.d(TAG, "  This is a repeating task with type: " + task.getRepeatType());
                        }
                    } else {
                        Log.d(TAG, "  Reason: Task has no due date");
                    }
                }
                Log.d(TAG, "---");
            }
        } else {
            Log.w(TAG, "No tasks available in cache!");
            taskCache.logCacheState("CalendarWidget");
            
            // Try direct access to cache
            List<Task> directCacheTasks = taskCache.getAllTasks();
            Log.d(TAG, "Direct cache access returned: " + (directCacheTasks != null ? directCacheTasks.size() : 0) + " tasks");
            
            // If direct cache has tasks, use them
            if (directCacheTasks != null && directCacheTasks.size() > 0) {
                allTasks = directCacheTasks;
                Log.d(TAG, "Using direct cache tasks instead");
                
                // Repeat the task checking logic with direct cache tasks
                for (Task task : allTasks) {
                    Log.d(TAG, "Checking direct cache task: " + task.getTitle() + " (due: " + task.getDueDate() + ")");
                    boolean isMatch = CalendarUtils.isTaskOnDate(task, dateString);
                    if (isMatch) {
                        tasks.add(task);
                        Log.d(TAG, "✓ Task from direct cache added: " + task.getTitle());
                    }
                }
            }
        }
        
        Log.d(TAG, "Tasks found for date: " + tasks.size());

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

        // Clear and rebuild list with proper order
        tasks.clear();
        tasks.addAll(uncompletedTasks);
        tasks.addAll(completedTasks);
        
        Log.d(TAG, "Final task list size after sorting: " + tasks.size());
    }
    
    private int getPriorityColor(String priority) {
        if (priority == null) {
            return 0xFF9E9E9E; // Gray for no priority
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
                return 0xFF9E9E9E; // Gray
        }
    }
}
