package com.example.todolist.widget;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.example.todolist.R;
import com.example.todolist.cache.TaskCache;
import com.example.todolist.helper.calendar.CalendarUtils;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import java.util.ArrayList;
import java.util.List;

public class WeekCalendarWidgetService extends RemoteViewsService {
    
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WeekCalendarRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class WeekCalendarRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    
    private Context context;
    private List<Task> tasks;
    private TaskService taskService;
    private TaskCache taskCache;
    private String selectedDate;
    
    public WeekCalendarRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.selectedDate = intent.getStringExtra("selected_date");
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
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_week_task_item);
        
        String title = task.getTitle();
        if (title != null) {
            title = title.replace("👑", "").trim();
        }
        
        // Set task title
        views.setTextViewText(R.id.widget_week_task_title, title);

        if (task.isImportant()) {
            views.setViewVisibility(R.id.widget_week_task_star, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_week_task_star, View.GONE);
        }

        // Set task time if available
        if (task.getDueTime() != null && !task.getDueTime().isEmpty() && 
            !task.getDueTime().equals("Không")) {
            views.setTextViewText(R.id.widget_week_task_time, task.getDueTime());
            views.setViewVisibility(R.id.widget_week_task_time, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_week_task_time, View.GONE);
        }

        // Set completion circle
        if (task.isCompleted()) {
            views.setTextViewText(R.id.widget_week_task_checkbox, "✓");
            views.setTextColor(R.id.widget_week_task_checkbox, 0xFF4CAF50); // Green checkmark
        } else {
            views.setTextViewText(R.id.widget_week_task_checkbox, "○");
            views.setTextColor(R.id.widget_week_task_checkbox, 0xFF999999); // Gray circle
        }
        
        // Strike through text if completed
        if (task.isCompleted()) {
            views.setTextColor(R.id.widget_week_task_title, 0xFF999999); // Gray text for completed
        } else {
            views.setTextColor(R.id.widget_week_task_title, 0xFF333333); // Black text for active
        }
        
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
        if (selectedDate == null || selectedDate.isEmpty()) {
            return;
        }
        
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
        
        if (allTasks == null) {
            allTasks = taskCache.getAllTasks();
        }
        
        if (allTasks == null || allTasks.isEmpty()) {
            return;
        }
        
        tasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (CalendarUtils.isTaskOnDate(task, selectedDate)) {
                tasks.add(task);
            }
        }
    }
}
