package com.example.todolist.util;
import com.example.todolist.model.Task;
import java.util.Calendar;
public class CalendarUtils {
    // Support cho Task model (Firebase)
    public static boolean isTaskOnDate(Task task, String targetDate) {
        try {
            if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
                return false;
            }
            String[] taskDateParts = task.getDueDate().split("/");
            String[] targetDateParts = targetDate.split("/");
            if (taskDateParts.length != 3 || targetDateParts.length != 3) {
                return false;
            }
            Calendar taskDate = Calendar.getInstance();
            taskDate.set(Calendar.YEAR, Integer.parseInt(taskDateParts[0]));
            taskDate.set(Calendar.MONTH, Integer.parseInt(taskDateParts[1]) - 1);
            taskDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(taskDateParts[2]));
            Calendar targetCalendar = Calendar.getInstance();
            targetCalendar.set(Calendar.YEAR, Integer.parseInt(targetDateParts[0]));
            targetCalendar.set(Calendar.MONTH, Integer.parseInt(targetDateParts[1]) - 1);
            targetCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(targetDateParts[2]));
            if (targetCalendar.before(taskDate)) {
                return false;
            }
            if (!task.isRepeating() || task.getRepeatType() == null || task.getRepeatType().equals("Không có")) {
                return task.getDueDate().equals(targetDate);
            }
            switch (task.getRepeatType()) {
                case "Hằng ngày":
                    return !targetCalendar.before(taskDate);
                case "Hằng tuần":
                    if (targetCalendar.get(Calendar.DAY_OF_WEEK) == taskDate.get(Calendar.DAY_OF_WEEK)) {
                        long diffInMillis = targetCalendar.getTimeInMillis() - taskDate.getTimeInMillis();
                        long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                        return diffInDays >= 0 && diffInDays % 7 == 0;
                    }
                    return false;
                case "Hằng tháng":
                    if (targetCalendar.get(Calendar.DAY_OF_MONTH) == taskDate.get(Calendar.DAY_OF_MONTH)) {
                        int taskYear = taskDate.get(Calendar.YEAR);
                        int taskMonth = taskDate.get(Calendar.MONTH);
                        int targetYear = targetCalendar.get(Calendar.YEAR);
                        int targetMonth = targetCalendar.get(Calendar.MONTH);
                        int monthDiff = (targetYear - taskYear) * 12 + (targetMonth - taskMonth);
                        return monthDiff >= 0;
                    }
                    return false;
                default:
                    return task.getDueDate().equals(targetDate);
            }
        } catch (Exception e) {
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
