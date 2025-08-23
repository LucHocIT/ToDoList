package com.example.todolist.widget;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import com.example.todolist.R;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
public class CalendarWidgetHelper {
    private static final String WIDGET_PREFS = "widget_preferences";
    private static final String PREF_CURRENT_MONTH = "current_month";
    private static final String PREF_CURRENT_YEAR = "current_year";
    public static String getCurrentMonthYear(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        int month = prefs.getInt(PREF_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        int year = prefs.getInt(PREF_CURRENT_YEAR, calendar.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
        return monthFormat.format(calendar.getTime());
    }
    public static void navigateMonth(Context context, int direction) {
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        int month = prefs.getInt(PREF_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        int year = prefs.getInt(PREF_CURRENT_YEAR, calendar.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);
        calendar.add(Calendar.MONTH, direction);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        editor.putInt(PREF_CURRENT_YEAR, calendar.get(Calendar.YEAR));
        editor.apply();
    }
    public static void generateCalendar(Context context, RemoteViews views) {
        // Remove all views from the grid first
        views.removeAllViews(R.id.widget_calendar_grid);
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        int month = prefs.getInt(PREF_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        int year = prefs.getInt(PREF_CURRENT_YEAR, calendar.get(Calendar.YEAR));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        // Get first day of week (Sunday = 1, Monday = 2, etc.)
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        // Get number of days in month
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        // Get tasks for this month
        List<Task> monthTasks = getTasksForMonth(context, year, month);
        // Create calendar grid with 6 rows
        int dayCounter = 1;
        boolean monthStarted = false;
        for (int week = 0; week < 6; week++) {
            // Create a row for this week
            RemoteViews weekRow = new RemoteViews(context.getPackageName(), R.layout.widget_week_row);
            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                RemoteViews dayView = new RemoteViews(context.getPackageName(), R.layout.widget_calendar_day);
                if (week == 0 && dayOfWeek < firstDayOfWeek) {
                    // Empty cell before month starts
                    dayView.setTextViewText(R.id.widget_day_number, "");
                    dayView.setViewVisibility(R.id.widget_task_dot, android.view.View.GONE);
                } else if (dayCounter <= daysInMonth) {
                    // Day in current month
                    monthStarted = true;
                    dayView.setTextViewText(R.id.widget_day_number, String.valueOf(dayCounter));
                    // Check if this is today
                    boolean isToday = (today.get(Calendar.YEAR) == year && 
                                     today.get(Calendar.MONTH) == month && 
                                     today.get(Calendar.DAY_OF_MONTH) == dayCounter);
                    if (isToday) {
                        dayView.setInt(R.id.widget_day_number, "setTextColor", 0xFFFFFFFF);
                        dayView.setInt(R.id.widget_day_number, "setBackgroundResource", R.drawable.selected_day_background);
                    } else {
                        dayView.setInt(R.id.widget_day_number, "setTextColor", 0xFF333333);
                    }
                    // Check if this day has tasks
                    boolean hasTasks = hasTasksOnDay(monthTasks, dayCounter, month, year);
                    if (hasTasks) {
                        dayView.setViewVisibility(R.id.widget_task_dot, android.view.View.VISIBLE);
                    } else {
                        dayView.setViewVisibility(R.id.widget_task_dot, android.view.View.GONE);
                    }
                    dayCounter++;
                } else {
                    // Empty cell after month ends
                    dayView.setTextViewText(R.id.widget_day_number, "");
                    dayView.setViewVisibility(R.id.widget_task_dot, android.view.View.GONE);
                }
                weekRow.addView(R.id.widget_week_container, dayView);
            }
            views.addView(R.id.widget_calendar_grid, weekRow);
        }
    }
    private static List<Task> getTasksForMonth(Context context, int year, int month) {
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
        // Return all tasks - widget will filter them by date
        return taskService.getAllTasks();
    }
    private static boolean hasTasksOnDay(List<Task> tasks, int day, int month, int year) {
        // Use the same format as the main app: yyyy/MM/dd
        String targetDate = String.format("%04d/%02d/%02d", year, month + 1, day);
        for (Task task : tasks) {
            if (com.example.todolist.util.CalendarUtils.isTaskOnDate(task, targetDate)) {
                return true;
            }
        }
        return false;
    }
    public static void resetToCurrentMonth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_CURRENT_MONTH, calendar.get(Calendar.MONTH));
        editor.putInt(PREF_CURRENT_YEAR, calendar.get(Calendar.YEAR));
        editor.apply();
    }
}
