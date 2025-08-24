package com.example.todolist.util;
import android.util.Log;
import com.example.todolist.model.Task;
import java.util.Calendar;
public class CalendarUtils {
    // Support cho Task model (Firebase)
    public static boolean isTaskOnDate(Task task, String targetDate) {
        try {
            Log.d("CalendarUtils", "Checking task: " + task.getTitle() + " against date: " + targetDate);
            
            if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
                Log.d("CalendarUtils", "Task has no due date");
                return false;
            }
            
            Log.d("CalendarUtils", "Task due date: " + task.getDueDate());
            
            String[] taskDateParts = task.getDueDate().split("/");
            String[] targetDateParts = targetDate.split("/");
            
            if (taskDateParts.length != 3 || targetDateParts.length != 3) {
                Log.d("CalendarUtils", "Invalid date format");
                return false;
            }

            Calendar taskDate = Calendar.getInstance();
            // Assume dd/MM/yyyy format (new format)
            taskDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(taskDateParts[0]));
            taskDate.set(Calendar.MONTH, Integer.parseInt(taskDateParts[1]) - 1);
            taskDate.set(Calendar.YEAR, Integer.parseInt(taskDateParts[2]));

            Calendar targetCalendar = Calendar.getInstance();
            targetCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(targetDateParts[0]));
            targetCalendar.set(Calendar.MONTH, Integer.parseInt(targetDateParts[1]) - 1);
            targetCalendar.set(Calendar.YEAR, Integer.parseInt(targetDateParts[2]));

            if (targetCalendar.before(taskDate)) {
                Log.d("CalendarUtils", "Target date is before task date");
                return false;
            }

            if (!task.isRepeating() || task.getRepeatType() == null || task.getRepeatType().equals("Không có")) {
                boolean result = task.getDueDate().equals(targetDate);
                Log.d("CalendarUtils", "Non-repeating task match: " + result);
                return result;
            }

            Log.d("CalendarUtils", "Checking repeating task with type: " + task.getRepeatType());

            switch (task.getRepeatType()) {
                case "Hằng ngày":
                    boolean dailyResult = !targetCalendar.before(taskDate);
                    Log.d("CalendarUtils", "Daily task result: " + dailyResult);
                    return dailyResult;
                case "Hằng tuần":
                    if (targetCalendar.get(Calendar.DAY_OF_WEEK) == taskDate.get(Calendar.DAY_OF_WEEK)) {
                        long diffInMillis = targetCalendar.getTimeInMillis() - taskDate.getTimeInMillis();
                        long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                        boolean weeklyResult = diffInDays >= 0 && diffInDays % 7 == 0;
                        Log.d("CalendarUtils", "Weekly task result: " + weeklyResult);
                        return weeklyResult;
                    }
                    return false;
                case "Hằng tháng":
                    if (targetCalendar.get(Calendar.DAY_OF_MONTH) == taskDate.get(Calendar.DAY_OF_MONTH)) {
                        int taskYear = taskDate.get(Calendar.YEAR);
                        int taskMonth = taskDate.get(Calendar.MONTH);
                        int targetYear = targetCalendar.get(Calendar.YEAR);
                        int targetMonth = targetCalendar.get(Calendar.MONTH);
                        int monthDiff = (targetYear - taskYear) * 12 + (targetMonth - taskMonth);
                        boolean monthlyResult = monthDiff >= 0;
                        Log.d("CalendarUtils", "Monthly task result: " + monthlyResult);
                        return monthlyResult;
                    }
                    return false;
                default:
                    boolean defaultResult = task.getDueDate().equals(targetDate);
                    Log.d("CalendarUtils", "Default case result: " + defaultResult);
                    return defaultResult;
            }
        } catch (Exception e) {
            Log.e("CalendarUtils", "Error checking task on date", e);
            return task.getDueDate().equals(targetDate);
        }
    }
    public static boolean isTimeOverdue(String taskTime) {
        try {
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
            String[] timeParts = taskTime.split(":");
            if (timeParts.length == 2) {
                int taskHour = Integer.parseInt(timeParts[0]);
                int taskMinute = Integer.parseInt(timeParts[1]);
                int currentTotalMinutes = currentHour * 60 + currentMinute;
                int taskTotalMinutes = taskHour * 60 + taskMinute;
                return taskTotalMinutes < currentTotalMinutes;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    public static int dpToPx(android.content.Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
