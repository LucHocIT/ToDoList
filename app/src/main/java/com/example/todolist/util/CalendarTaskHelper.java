package com.example.todolist.util;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarTaskHelper {
    
    public static void loadTasksForDate(Context context, String dateString, 
                                      TaskLoadListener listener) {
        new Thread(() -> {
            TodoDatabase database = TodoDatabase.getInstance(context);
            List<TodoTask> allTasks = database.todoDao().getAllTasks();
            List<TodoTask> tasksForDate = new ArrayList<>();
            
            for (TodoTask task : allTasks) {
                if (CalendarUtils.isTaskOnDate(task, dateString)) {
                    tasksForDate.add(task);
                }
            }
            
            if (listener != null) {
                listener.onTasksLoaded(tasksForDate);
            }
        }).start();
    }
    
    public static void updateTaskDisplay(Context context, LinearLayout container, 
                                       List<TodoTask> tasks) {
        container.removeAllViews();
        
        if (tasks.isEmpty()) {
            addEmptyTaskMessage(context, container);
        } else {
            for (TodoTask task : tasks) {
                View taskItemView = TaskItemViewHelper.createTaskItemView(context, task);
                container.addView(taskItemView);
            }
        }
    }
    
    private static void addEmptyTaskMessage(Context context, LinearLayout container) {
        TextView noTasksView = new TextView(context);
        noTasksView.setText("Không có nhiệm vụ nào trong ngày.");
        noTasksView.setTextSize(16);
        noTasksView.setTextColor(Color.parseColor("#666666"));
        noTasksView.setGravity(android.view.Gravity.CENTER);
        noTasksView.setPadding(16, 32, 16, 8);
        container.addView(noTasksView);
        
        TextView addTaskPrompt = new TextView(context);
        addTaskPrompt.setText("Nhấn + để tạo công việc của bạn.");
        addTaskPrompt.setTextSize(14);
        addTaskPrompt.setTextColor(Color.parseColor("#999999"));
        addTaskPrompt.setGravity(android.view.Gravity.CENTER);
        addTaskPrompt.setPadding(16, 0, 16, 32);
        container.addView(addTaskPrompt);
    }
    
    public static String formatSelectedDate(Calendar selectedDate, int selectedDay) {
        return String.format("%04d/%02d/%02d", 
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH) + 1,
            selectedDay);
    }
    
    public interface TaskLoadListener {
        void onTasksLoaded(List<TodoTask> tasks);
    }
}
