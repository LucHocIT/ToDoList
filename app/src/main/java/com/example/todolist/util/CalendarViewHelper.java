package com.example.todolist.util;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.todolist.R;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import java.util.Calendar;
import java.util.List;
public class CalendarViewHelper {
    public static void loadMonthCalendar(Context context, GridLayout calendarGrid, 
                                       Calendar currentCalendar, Calendar selectedDate, 
                                       int selectedDay, OnDayClickListener listener) {
        calendarGrid.removeAllViews();
        Calendar tempCal = (Calendar) currentCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        // Add empty cells for days before month starts
        int startOffset = (firstDayOfWeek == Calendar.SUNDAY) ? 0 : firstDayOfWeek - 1;
        Calendar prevMonth = (Calendar) currentCalendar.clone();
        prevMonth.add(Calendar.MONTH, -1);
        int daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = startOffset - 1; i >= 0; i--) {
            int prevDay = daysInPrevMonth - i;
            TextView dayView = createDayView(context, prevDay, false, false, false, false);
            calendarGrid.addView(dayView);
        }
        // Add days of current month
        TaskService database = new TaskService(context, null);
        Calendar today = Calendar.getInstance();
        boolean isCurrentMonth = (currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH));
        
        // Load tasks from database first
        database.getAllTasks(new com.example.todolist.repository.BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> allTasks) {
                // Add debug logging to see what tasks we get
                android.util.Log.d("CalendarViewHelper", "Successfully loaded " + allTasks.size() + " tasks from database");
                for (Task task : allTasks) {
                    android.util.Log.d("CalendarViewHelper", "Task: " + task.getTitle() + ", Due: " + task.getDueDate() + ", Completed: " + task.isCompleted());
                }
                
                // Now create day views with task information
                for (int day = 1; day <= daysInMonth; day++) {
                    boolean isToday = isCurrentMonth && day == today.get(Calendar.DAY_OF_MONTH);
                    boolean isSelected = (currentCalendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                        currentCalendar.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                                        day == selectedDay);
                    boolean hasTasksForDay = hasTasksForDate(allTasks, currentCalendar.get(Calendar.YEAR), 
                                                    currentCalendar.get(Calendar.MONTH), day);
                    
                    TextView dayView = createDayView(context, day, true, isToday, isSelected, hasTasksForDay);
                    final int finalDay = day;
                    dayView.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDayClick(finalDay);
                        }
                    });
                    calendarGrid.addView(dayView);
                }
                
                // Add remaining cells for next month
                int totalCells = calendarGrid.getChildCount();
                int remainingCells = 42 - totalCells;
                for (int i = 1; i <= remainingCells; i++) {
                    TextView dayView = createDayView(context, i, false, false, false, false);
                    calendarGrid.addView(dayView);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("CalendarViewHelper", "Error loading tasks for calendar: " + error);
                // Fallback: create calendar without task indicators
                for (int day = 1; day <= daysInMonth; day++) {
                    boolean isToday = isCurrentMonth && day == today.get(Calendar.DAY_OF_MONTH);
                    boolean isSelected = (currentCalendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                        currentCalendar.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                                        day == selectedDay);
                    
                    TextView dayView = createDayView(context, day, true, isToday, isSelected, false);
                    final int finalDay = day;
                    dayView.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDayClick(finalDay);
                        }
                    });
                    calendarGrid.addView(dayView);
                }
                
                // Add remaining cells for next month
                int totalCells = calendarGrid.getChildCount();
                int remainingCells = 42 - totalCells;
                for (int i = 1; i <= remainingCells; i++) {
                    TextView dayView = createDayView(context, i, false, false, false, false);
                    calendarGrid.addView(dayView);
                }
            }
        });
    }
    public static void loadWeekView(Context context, LinearLayout weekGrid, 
                                  Calendar selectedDate, int selectedDay, 
                                  OnDayClickListener listener) {
        weekGrid.removeAllViews();
        Calendar weekStart = (Calendar) selectedDate.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        
        TaskService database = new TaskService(context, null);
        
        // Load tasks from database first
        database.getAllTasks(new com.example.todolist.repository.BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> allTasks) {
                // Add debug logging to see what tasks we get
                android.util.Log.d("CalendarViewHelper", "Week view: Successfully loaded " + allTasks.size() + " tasks from database");
                for (Task task : allTasks) {
                    android.util.Log.d("CalendarViewHelper", "Week view Task: " + task.getTitle() + ", Due: " + task.getDueDate() + ", Completed: " + task.isCompleted());
                }
                
                // Now create week day views with task information
                for (int i = 0; i < 7; i++) {
                    Calendar dayCalendar = (Calendar) weekStart.clone();
                    dayCalendar.add(Calendar.DAY_OF_MONTH, i);
                    TextView dayView = createWeekDayView(context, dayCalendar, selectedDate, allTasks);
                    dayView.setOnClickListener(v -> {
                        if (listener != null) {
                            int day = dayCalendar.get(Calendar.DAY_OF_MONTH);
                            listener.onWeekDayClick(day, dayCalendar);
                        }
                    });
                    weekGrid.addView(dayView);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("CalendarViewHelper", "Error loading tasks for week view: " + error);
                // Fallback: create week view without task indicators
                for (int i = 0; i < 7; i++) {
                    Calendar dayCalendar = (Calendar) weekStart.clone();
                    dayCalendar.add(Calendar.DAY_OF_MONTH, i);
                    TextView dayView = createWeekDayViewFallback(context, dayCalendar, selectedDate);
                    dayView.setOnClickListener(v -> {
                        if (listener != null) {
                            int day = dayCalendar.get(Calendar.DAY_OF_MONTH);
                            listener.onWeekDayClick(day, dayCalendar);
                        }
                    });
                    weekGrid.addView(dayView);
                }
            }
        });
    }
    private static TextView createDayView(Context context, int day, boolean isCurrentMonth, 
                                        boolean isToday, boolean isSelected, boolean hasTasks) {
        TextView textView = new TextView(context);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 120;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(4, 4, 4, 4);
        textView.setLayoutParams(params);
        textView.setText(String.valueOf(day));
        textView.setTextSize(16);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setPadding(8, 16, 8, 16);
        if (isCurrentMonth) {
            if (isSelected) {
                textView.setBackgroundResource(R.drawable.calendar_day_selected);
                textView.setTextColor(Color.WHITE);
            } else {
                textView.setTextColor(Color.parseColor("#333333"));
                textView.setBackgroundResource(R.drawable.calendar_day_normal);
            }
            
            // Debug: Log khi có task
            if (hasTasks) {
                Log.d("CalendarViewHelper", "Setting task indicator for day " + day);
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.task_indicator_dot);
                textView.setCompoundDrawablePadding(4);
            } else {
                // Đảm bảo clear drawable nếu không có task
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        } else {
            textView.setTextColor(Color.parseColor("#CCCCCC"));
            textView.setBackgroundColor(Color.WHITE);
            // Clear drawable cho những ngày không thuộc tháng hiện tại
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
        textView.setClickable(isCurrentMonth);
        return textView;
    }
    private static TextView createWeekDayView(Context context, Calendar dayCalendar, 
                                            Calendar selectedDate, List<Task> allTasks) {
        TextView dayView = new TextView(context);
        int day = dayCalendar.get(Calendar.DAY_OF_MONTH);
        dayView.setText(String.valueOf(day));
        dayView.setTextSize(16);
        dayView.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 120);
        params.weight = 1;
        params.setMargins(4, 4, 4, 4);
        dayView.setLayoutParams(params);
        dayView.setPadding(8, 16, 8, 16);

        boolean isSelected = CalendarUtils.isSameDay(dayCalendar, selectedDate);
        boolean hasTasksForDay = hasTasksForDate(allTasks, dayCalendar.get(Calendar.YEAR), 
                                        dayCalendar.get(Calendar.MONTH), day);

        // Debug: Log cho week view
        Log.d("CalendarViewHelper", "Week view - Day " + day + " has tasks: " + hasTasksForDay);

        if (hasTasksForDay) {
            Log.d("CalendarViewHelper", "Setting task indicator for week day " + day);
            dayView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.task_indicator_dot);
            dayView.setCompoundDrawablePadding(4);
        } else {
            // Clear drawable nếu không có task
            dayView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
        
        if (isSelected) {
            dayView.setBackgroundResource(R.drawable.calendar_day_selected);
            dayView.setTextColor(Color.WHITE);
        } else {
            dayView.setTextColor(Color.parseColor("#333333"));
            dayView.setBackgroundResource(R.drawable.calendar_day_normal);
        }
        return dayView;
    }
    
    private static TextView createWeekDayViewFallback(Context context, Calendar dayCalendar, 
                                                    Calendar selectedDate) {
        TextView dayView = new TextView(context);
        int day = dayCalendar.get(Calendar.DAY_OF_MONTH);
        dayView.setText(String.valueOf(day));
        dayView.setTextSize(16);
        dayView.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 120);
        params.weight = 1;
        params.setMargins(4, 4, 4, 4);
        dayView.setLayoutParams(params);
        dayView.setPadding(8, 16, 8, 16);

        boolean isSelected = CalendarUtils.isSameDay(dayCalendar, selectedDate);
        
        // No task indicator for fallback
        dayView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        
        if (isSelected) {
            dayView.setBackgroundResource(R.drawable.calendar_day_selected);
            dayView.setTextColor(Color.WHITE);
        } else {
            dayView.setTextColor(Color.parseColor("#333333"));
            dayView.setBackgroundResource(R.drawable.calendar_day_normal);
        }
        return dayView;
    }
    private static boolean hasTasksForDate(List<Task> allTasks, int year, int month, int day) {
        // Sửa format ngày để phù hợp với CalendarUtils.isTaskOnDate() (dd/MM/yyyy)
        String dateString = String.format("%02d/%02d/%04d", day, month + 1, year);
        
        // Debug logging
        android.util.Log.d("CalendarViewHelper", "Checking tasks for date: " + dateString);
        android.util.Log.d("CalendarViewHelper", "Total tasks: " + allTasks.size());
        
        for (Task task : allTasks) {
            android.util.Log.d("CalendarViewHelper", "Task: " + task.getTitle() + ", Due: " + task.getDueDate());
            if (CalendarUtils.isTaskOnDate(task, dateString)) {
                android.util.Log.d("CalendarViewHelper", "Found task on date " + dateString + ": " + task.getTitle());
                return true;
            }
        }
        
        android.util.Log.d("CalendarViewHelper", "No tasks found for date: " + dateString);
        return false;
    }
    public interface OnDayClickListener {
        void onDayClick(int day);
        void onWeekDayClick(int day, Calendar dayCalendar);
    }
}
