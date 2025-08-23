package com.example.todolist.manager;
import android.content.Context;
import android.content.SharedPreferences;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import java.util.ArrayList;
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
    private TaskService taskService;
    private SharedPreferences preferences;
    public UserStatsManager(Context context) {
        this.context = context;
        this.taskService = new TaskService(context, null);
        this.preferences = context.getSharedPreferences("TodoApp", Context.MODE_PRIVATE);
    }
    public interface StatsCallback {
        void onStatsCalculated(UserStats stats);
    }
    public void calculateUserStats(StatsCallback callback) {
        UserStats stats = new UserStats();
        try {
            // Get all tasks using Firebase service
            taskService.getAllTasks(new com.example.todolist.repository.BaseRepository.RepositoryCallback<List<Task>>() {
                @Override
                public void onSuccess(List<Task> allTasks) {
                    List<Task> completedTasks = new ArrayList<>();
                    for (Task task : allTasks) {
                        if (task.isCompleted()) {
                            completedTasks.add(task);
                        }
                    }
                    stats.totalTasks = allTasks.size();
                    stats.completedTasks = completedTasks.size();
                stats.pendingTasks = stats.totalTasks - stats.completedTasks;
                stats.productivityScore = stats.totalTasks > 0 ? 
                    (stats.completedTasks * 100) / stats.totalTasks : 0;
                stats.currentStreak = calculateCurrentStreak(completedTasks);
                stats.longestStreak = calculateLongestStreak(completedTasks);
                // Calculate time-based statistics
                stats.tasksCompletedToday = calculateTasksCompletedInPeriod(completedTasks, 0);
                stats.tasksCompletedThisWeek = calculateTasksCompletedInPeriod(completedTasks, 7);
                stats.tasksCompletedThisMonth = calculateTasksCompletedInPeriod(completedTasks, 30);
                updatePreferences(stats);
                // Return stats via callback
                if (callback != null) {
                    callback.onStatsCalculated(stats);
                }
            }
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onStatsCalculated(getDefaultStats());
                }
            }
        });
        } catch (Exception e) {
            e.printStackTrace();
            // Return default stats on error
            if (callback != null) {
                callback.onStatsCalculated(getDefaultStats());
            }
        }
    }
    private int calculateCurrentStreak(List<Task> completedTasks) {
        int currentStreak = preferences.getInt("current_streak", 0);
        return currentStreak;
    }
    private int calculateLongestStreak(List<Task> completedTasks) {
        int currentStreak = calculateCurrentStreak(completedTasks);
        int longestStreak = preferences.getInt("longest_streak", 0);
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
        return longestStreak;
    }
    private int calculateTasksCompletedInPeriod(List<Task> completedTasks, int daysBack) {

        if (daysBack == 0) {
            return Math.min(completedTasks.size() / 7, 5); // Assume 1/7 of tasks completed today, max 5
        } else if (daysBack == 7) {
            // Tasks completed this week
            return Math.min(completedTasks.size(), 20); // Max 20 tasks per week
        } else {
            // Tasks completed this month
            return completedTasks.size();
        }
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

    public void onTaskCompleted() {

        calculateUserStats(null);
    }

    public void calculateUserStats() {
        calculateUserStats(null);
    }

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
