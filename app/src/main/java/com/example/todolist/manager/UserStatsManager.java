package com.example.todolist.manager;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class UserStatsManager {
    
    public static class UserStats {
        public int totalTasks;
        public int completedTasks;
        public int pendingTasks;
        public int productivityScore;
        public int currentStreak;
        public int longestStreak;
        public int tasksCompletedToday;
        public int tasksCompletedThisWeek;
        public int tasksCompletedThisMonth;
    }
    
    private Context context;
    private TodoDatabase database;
    private SharedPreferences preferences;
    
    public UserStatsManager(Context context) {
        this.context = context;
        this.database = TodoDatabase.getInstance(context);
        this.preferences = context.getSharedPreferences("TodoApp", Context.MODE_PRIVATE);
    }
    
    public UserStats calculateUserStats() {
        UserStats stats = new UserStats();
        
        try {
            // Get all tasks
            List<TodoTask> allTasks = database.todoDao().getAllTasks();
            List<TodoTask> completedTasks = database.todoDao().getCompletedTasks();
            
            stats.totalTasks = allTasks.size();
            stats.completedTasks = completedTasks.size();
            stats.pendingTasks = stats.totalTasks - stats.completedTasks;
            
            // Calculate productivity score
            stats.productivityScore = stats.totalTasks > 0 ? 
                (stats.completedTasks * 100) / stats.totalTasks : 0;
            
            // Calculate streak data
            stats.currentStreak = calculateCurrentStreak(completedTasks);
            stats.longestStreak = calculateLongestStreak(completedTasks);
            
            // Calculate time-based statistics
            stats.tasksCompletedToday = calculateTasksCompletedInPeriod(completedTasks, 0);
            stats.tasksCompletedThisWeek = calculateTasksCompletedInPeriod(completedTasks, 7);
            stats.tasksCompletedThisMonth = calculateTasksCompletedInPeriod(completedTasks, 30);
            
            // Update shared preferences with calculated values
            updatePreferences(stats);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Return default stats on error
            stats = getDefaultStats();
        }
        
        return stats;
    }
    
    private int calculateCurrentStreak(List<TodoTask> completedTasks) {
        // Simplified streak calculation - in a real app, you'd track daily completion dates
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        
        boolean completedToday = false;
        boolean completedYesterday = false;
        
        for (TodoTask task : completedTasks) {
            if (task.getCompletedAt() != null) {
                Calendar taskDate = Calendar.getInstance();
                taskDate.setTime(task.getCompletedAt());
                
                if (isSameDay(taskDate, today)) {
                    completedToday = true;
                } else if (isSameDay(taskDate, yesterday)) {
                    completedYesterday = true;
                }
            }
        }
        
        int currentStreak = preferences.getInt("current_streak", 0);
        
        // Update streak logic
        if (completedToday) {
            if (currentStreak == 0 || !completedYesterday) {
                currentStreak = 1;
            } else {
                currentStreak++;
            }
        } else if (!completedYesterday) {
            currentStreak = 0;
        }
        
        return currentStreak;
    }
    
    private int calculateLongestStreak(List<TodoTask> completedTasks) {
        int currentStreak = calculateCurrentStreak(completedTasks);
        int longestStreak = preferences.getInt("longest_streak", 0);
        
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
        
        return longestStreak;
    }
    
    private int calculateTasksCompletedInPeriod(List<TodoTask> completedTasks, int daysBack) {
        Calendar cutoffDate = Calendar.getInstance();
        if (daysBack > 0) {
            cutoffDate.add(Calendar.DAY_OF_YEAR, -daysBack);
        } else {
            // For today only
            cutoffDate.set(Calendar.HOUR_OF_DAY, 0);
            cutoffDate.set(Calendar.MINUTE, 0);
            cutoffDate.set(Calendar.SECOND, 0);
            cutoffDate.set(Calendar.MILLISECOND, 0);
        }
        
        int count = 0;
        for (TodoTask task : completedTasks) {
            if (task.getCompletedAt() != null) {
                if (daysBack == 0) {
                    // Count tasks completed today
                    Calendar taskDate = Calendar.getInstance();
                    taskDate.setTime(task.getCompletedAt());
                    Calendar today = Calendar.getInstance();
                    if (isSameDay(taskDate, today)) {
                        count++;
                    }
                } else {
                    // Count tasks completed in the last N days
                    if (task.getCompletedAt().after(cutoffDate.getTime())) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    private void updatePreferences(UserStats stats) {
        preferences.edit()
            .putInt("current_streak", stats.currentStreak)
            .putInt("longest_streak", stats.longestStreak)
            .putInt("tasks_completed_today", stats.tasksCompletedToday)
            .putInt("tasks_completed_week", stats.tasksCompletedThisWeek)
            .putInt("tasks_completed_month", stats.tasksCompletedThisMonth)
            .apply();
    }
    
    private UserStats getDefaultStats() {
        UserStats stats = new UserStats();
        stats.totalTasks = 0;
        stats.completedTasks = 0;
        stats.pendingTasks = 0;
        stats.productivityScore = 0;
        stats.currentStreak = 0;
        stats.longestStreak = 0;
        stats.tasksCompletedToday = 0;
        stats.tasksCompletedThisWeek = 0;
        stats.tasksCompletedThisMonth = 0;
        return stats;
    }
    
    public void updateUserName(String name) {
        preferences.edit()
            .putString("user_name", name)
            .apply();
    }
    
    public String getUserName() {
        return preferences.getString("user_name", "Người dùng");
    }
    
    public long getInstallTime() {
        return preferences.getLong("install_time", System.currentTimeMillis());
    }
    
    /**
     * Update streak when a task is completed
     */
    public void onTaskCompleted() {
        // Recalculate stats when a task is completed
        calculateUserStats();
    }
    
    /**
     * Reset user statistics (useful for testing or data reset)
     */
    public void resetStats() {
        preferences.edit()
            .putInt("current_streak", 0)
            .putInt("longest_streak", 0)
            .putInt("tasks_completed_today", 0)
            .putInt("tasks_completed_week", 0)
            .putInt("tasks_completed_month", 0)
            .apply();
    }
}
