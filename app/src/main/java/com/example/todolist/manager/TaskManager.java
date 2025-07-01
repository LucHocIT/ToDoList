package com.example.todolist.manager;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;
import com.example.todolist.notification.ReminderScheduler;
import com.example.todolist.widget.WidgetUpdateHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskManager {
    
    public interface TaskUpdateListener {
        void onTasksUpdated();
    }
    
    private Context context;
    private TodoDatabase database;
    private TaskUpdateListener listener;
    
    private List<TodoTask> allTasks;
    private List<TodoTask> overdueTasks;
    private List<TodoTask> todayTasks;
    private List<TodoTask> futureTasks;
    private List<TodoTask> completedTodayTasks;
    
    public TaskManager(Context context, TaskUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.database = TodoDatabase.getInstance(context);
        initializeLists();
    }
    
    private void initializeLists() {
        allTasks = new ArrayList<>();
        overdueTasks = new ArrayList<>();
        todayTasks = new ArrayList<>();
        futureTasks = new ArrayList<>();
        completedTodayTasks = new ArrayList<>();
    }
    
    public void loadTasks() {
        new Thread(() -> {
            allTasks = database.todoDao().getAllTasks();
            updateTaskLists();
            if (listener != null) {
                listener.onTasksUpdated();
            }
            // Update widget whenever tasks are loaded
            WidgetUpdateHelper.updateAllWidgets(context);
        }).start();
    }
    
    private void updateTaskLists() {
        overdueTasks.clear();
        todayTasks.clear();
        futureTasks.clear();
        completedTodayTasks.clear();
        
        Calendar now = Calendar.getInstance();
        String todayDateStr = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(now.getTime());
        
        for (TodoTask task : allTasks) {
            if (task.isCompleted()) {
                if (isTaskCompletedToday(task, todayDateStr)) {
                    completedTodayTasks.add(task);
                }
            } else {
                int timeCategory = getTaskTimeCategory(task, now, todayDateStr);
                switch (timeCategory) {
                    case 0: // Overdue
                        overdueTasks.add(task);
                        break;
                    case 1: // Today
                        todayTasks.add(task);
                        break;
                    case 2: // Future
                        futureTasks.add(task);
                        break;
                }
            }
        }
    }
    
    private boolean isTaskCompletedToday(TodoTask task, String todayDateStr) {
        // Check if task was completed today by comparing completion date
        String completionDate = task.getCompletionDate();
        if (completionDate == null || completionDate.isEmpty()) {
            return false; // No completion date means not completed or very old completion
        }
        
        // Compare completion date with today's date
        return completionDate.equals(todayDateStr);
    }
    
    private int getTaskTimeCategory(TodoTask task, Calendar now, String todayDateStr) {
        try {
            String taskDateStr = task.getDueDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date taskDate = dateFormat.parse(taskDateStr);
            Date todayDate = dateFormat.parse(todayDateStr);
            
            if (taskDate.before(todayDate)) {
                return 0; // Overdue
            } else if (taskDate.equals(todayDate)) {
                return 1; // Today
            } else {
                return 2; // Future
            }
        } catch (Exception e) {
            return 1; // Default to today
        }
    }
    
    public void completeTask(TodoTask task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        
        // Set completion date when marking as completed, clear when marking as incomplete
        if (isCompleted) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            task.setCompletionDate(dateFormat.format(new Date()));
        } else {
            task.setCompletionDate(null);
        }
        
        new Thread(() -> {
            database.todoDao().updateTask(task);
            
            // Handle reminder scheduling based on completion status
            ReminderScheduler scheduler = new ReminderScheduler(context);
            if (isCompleted) {
                // Cancel reminders for completed tasks
                scheduler.cancelTaskReminders(task.getId());
            } else {
                // Reschedule reminders for uncompleted tasks
                if (task.isHasReminder()) {
                    scheduler.scheduleTaskReminder(task);
                }
            }
            
            loadTasks();
        }).start();
    }
    
    public void toggleTaskImportant(TodoTask task) {
        task.setImportant(!task.isImportant());
        new Thread(() -> {
            database.todoDao().updateTask(task);
            loadTasks();
        }).start();
        
        String message = task.isImportant() ? "Đã đánh dấu quan trọng" : "Đã bỏ đánh dấu quan trọng";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    public void deleteTask(TodoTask task) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa nhiệm vụ")
                .setMessage("Bạn có chắc chắn muốn xóa nhiệm vụ này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    new Thread(() -> {
                        // Cancel reminders before deleting task
                        ReminderScheduler scheduler = new ReminderScheduler(context);
                        scheduler.cancelTaskReminders(task.getId());
                        
                        database.todoDao().deleteTask(task);
                        loadTasks();
                    }).start();
                    Toast.makeText(context, "Đã xóa nhiệm vụ", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    // Getters
    public List<TodoTask> getAllTasks() { return allTasks; }
    public List<TodoTask> getOverdueTasks() { return overdueTasks; }
    public List<TodoTask> getTodayTasks() { return todayTasks; }
    public List<TodoTask> getFutureTasks() { return futureTasks; }
    public List<TodoTask> getCompletedTodayTasks() { return completedTodayTasks; }
    
    /**
     * Lên lịch lại tất cả thông báo cho các task chưa hoàn thành
     */
    public void rescheduleAllReminders() {
        new Thread(() -> {
            List<TodoTask> allTasks = database.todoDao().getAllTasks();
            ReminderScheduler scheduler = new ReminderScheduler(context);
            
            for (TodoTask task : allTasks) {
                if (!task.isCompleted() && task.isHasReminder()) {
                    scheduler.scheduleTaskReminder(task);
                }
            }
        }).start();
    }
}
