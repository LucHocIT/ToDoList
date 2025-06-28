package com.example.todolist;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.todolist.adapter.CategorySpinnerAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Category;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.DateTimePickerDialog;
import com.example.todolist.util.AddTaskHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private TextView tvMonth, tvYear;
    private GridLayout calendarGrid;
    private LinearLayout taskInfoContainer, weekTaskInfoContainer;
    private ImageView btnPrevMonth, btnNextMonth, btnToggleCalendar;
    private FloatingActionButton fabAdd;
    private View calendarScrollView, weekViewContainer;
    private LinearLayout weekGrid;
    
    private Calendar currentCalendar;
    private Calendar selectedDate;
    private SimpleDateFormat monthFormat;
    private SimpleDateFormat yearFormat;
    private SimpleDateFormat dayFormat;
    
    private TodoDatabase database;
    private AddTaskHandler addTaskHandler;
    private int selectedDay = -1;
    private boolean isWeekView = false;
    private List<TodoTask> tasksForSelectedDate = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        
        initViews();
        initCalendar();
        setupBottomNavigation();
        loadCalendar();
    }
    
    private void initViews() {
        tvMonth = findViewById(R.id.tv_month);
        tvYear = findViewById(R.id.tv_year);
        calendarGrid = findViewById(R.id.calendar_grid);
        taskInfoContainer = findViewById(R.id.task_info_container);
        weekTaskInfoContainer = findViewById(R.id.week_task_info_container);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        btnToggleCalendar = findViewById(R.id.btn_toggle_calendar);
        fabAdd = findViewById(R.id.fab_add);
        calendarScrollView = findViewById(R.id.calendar_scroll_view);
        weekViewContainer = findViewById(R.id.week_view_container);
        weekGrid = findViewById(R.id.week_grid);
        
        database = TodoDatabase.getInstance(this);
        
        // Khởi tạo AddTaskHandler
        addTaskHandler = new AddTaskHandler(this, task -> {
            // Callback khi task được thêm thành công - reload lịch và tasks
            loadCalendar();
            loadTasksForSelectedDate();
        });
        
        // Setup click listeners
        btnPrevMonth.setOnClickListener(v -> navigateMonth(-1));
        btnNextMonth.setOnClickListener(v -> navigateMonth(1));
        btnToggleCalendar.setOnClickListener(v -> toggleCalendarView());
        
        // Setup FAB click listener - sử dụng AddTaskHandler với ngày được chọn
        fabAdd.setOnClickListener(v -> {
            // Tạo ngày được chọn theo định dạng yyyy/MM/dd
            String selectedDateString = String.format("%04d/%02d/%02d", 
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDay);
            addTaskHandler.showAddTaskDialog(selectedDateString);
        });
    }
    
    private void initCalendar() {
        currentCalendar = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        monthFormat = new SimpleDateFormat("MMMM", new Locale("vi", "VN"));
        yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        dayFormat = new SimpleDateFormat("d", Locale.getDefault());
        
        // Set selected day to today initially
        selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
    }
    
    private void setupBottomNavigation() {
        LinearLayout btnNavMenu = findViewById(R.id.btn_nav_menu);
        LinearLayout btnNavTasks = findViewById(R.id.btn_nav_tasks);
        LinearLayout btnNavCalendar = findViewById(R.id.btn_nav_calendar);
        LinearLayout btnNavProfile = findViewById(R.id.btn_nav_profile);
        
        // Set calendar as selected
        if (btnNavCalendar != null) {
            View calendarIcon = btnNavCalendar.getChildAt(0); // ImageView
            View calendarText = btnNavCalendar.getChildAt(1); // TextView
            if (calendarIcon instanceof ImageView) {
                ((ImageView) calendarIcon).setColorFilter(Color.parseColor("#4285F4"));
            }
            if (calendarText instanceof TextView) {
                ((TextView) calendarText).setTextColor(Color.parseColor("#4285F4"));
            }
        }
        
        if (btnNavTasks != null) {
            btnNavTasks.setOnClickListener(v -> {
                finish(); // Go back to MainActivity (tasks)
            });
        }
        
        if (btnNavMenu != null) {
            btnNavMenu.setOnClickListener(v -> {
                // Open navigation drawer (go to main activity)
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("open_drawer", true);
                startActivity(intent);
                finish();
            });
        }
        
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> {
                // Handle profile navigation
                Toast.makeText(this, "Của tôi", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, -1);
                loadCalendar();
            });
        }
        
        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, 1);
                loadCalendar();
            });
        }
    }
    
    private void loadCalendar() {
        // Update month and year display
        String monthName = monthFormat.format(currentCalendar.getTime()).toUpperCase();
        String yearText = yearFormat.format(currentCalendar.getTime());
        
        tvMonth.setText("THÁNG " + (currentCalendar.get(Calendar.MONTH) + 1));
        tvYear.setText(yearText);
        
        // Clear previous calendar
        calendarGrid.removeAllViews();
        
        // Get first day of month and number of days
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
            TextView dayView = createDayView(prevDay, false, false);
            calendarGrid.addView(dayView);
        }
        
        // Add days of current month
        Calendar today = Calendar.getInstance();
        boolean isCurrentMonth = (currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH));
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        
        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday = isCurrentMonth && day == todayDay;
            boolean isSelected = (currentCalendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                currentCalendar.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                                day == selectedDay);
            TextView dayView = createDayView(day, true, isToday);
            
            // Chỉ highlight ngày được chọn, không highlight today
            if (isSelected) {
                dayView.setBackground(getDrawable(R.drawable.calendar_day_selected));
                dayView.setTextColor(Color.WHITE);
            } else {
                // Không highlight today nữa, chỉ sử dụng style bình thường
                dayView.setTextColor(Color.parseColor("#333333"));
                dayView.setBackground(getDrawable(R.drawable.calendar_day_normal));
            }
            
            // Check if day has tasks
            boolean hasTasksForDay = hasTasks(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), day);
            if (hasTasksForDay) {
                // Add blue dot indicator below the day number
                dayView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.task_indicator_dot);
                dayView.setCompoundDrawablePadding(4);
            }
            
            final int finalDay = day;
            dayView.setOnClickListener(v -> {
                selectedDay = finalDay;
                selectedDate.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
                selectedDate.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH));
                selectedDate.set(Calendar.DAY_OF_MONTH, finalDay);
                loadCalendar();
                loadTasksForSelectedDate();
            });
            
            calendarGrid.addView(dayView);
        }
        
        // Add remaining cells for next month
        int totalCells = calendarGrid.getChildCount();
        int remainingCells = 42 - totalCells; // 42 day cells (6 weeks x 7 days)
        for (int i = 1; i <= remainingCells; i++) {
            TextView dayView = createDayView(i, false, false);
            calendarGrid.addView(dayView);
        }
        
        loadTasksForSelectedDate();
    }
    
    private TextView createDayView(int day, boolean isCurrentMonth, boolean isToday) {
        TextView textView = new TextView(this);
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
            textView.setTextColor(Color.parseColor("#333333"));
            if (isToday) {
                textView.setBackground(getDrawable(R.drawable.calendar_day_today));
                textView.setTextColor(Color.WHITE);
            } else {
                textView.setBackgroundColor(Color.WHITE);
                // Add subtle border
                textView.setBackground(getDrawable(R.drawable.calendar_day_normal));
            }
        } else {
            textView.setTextColor(Color.parseColor("#CCCCCC"));
            textView.setBackgroundColor(Color.WHITE);
        }
        
        textView.setClickable(isCurrentMonth);
        
        return textView;
    }
    
    private boolean hasTasks(int year, int month, int day) {
        // Convert to date string format that matches TodoTask dueDate (yyyy/MM/dd)
        String dateString = String.format("%04d/%02d/%02d", year, month + 1, day);
        
        // Lấy tất cả tasks và kiểm tra xem có task nào hiển thị trong ngày này không
        List<TodoTask> allTasks = database.todoDao().getAllTasks();
        for (TodoTask task : allTasks) {
            if (isTaskOnDate(task, dateString)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void loadTasksForSelectedDate() {
        // Convert selected date to string format that matches TodoTask dueDate
        String dateString = String.format("%04d/%02d/%02d", 
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH) + 1, 
            selectedDay);
        
        new Thread(() -> {
            // Lấy tất cả tasks từ database
            List<TodoTask> allTasks = database.todoDao().getAllTasks();
            List<TodoTask> tasksForDate = new ArrayList<>();
            
            // Lọc tasks cho ngày được chọn (bao gồm cả task lặp lại)
            for (TodoTask task : allTasks) {
                if (isTaskOnDate(task, dateString)) {
                    tasksForDate.add(task);
                }
            }
            
            tasksForSelectedDate = tasksForDate;
            
            runOnUiThread(() -> {
                updateTaskDisplay();
            });
        }).start();
    }
    
    private void updateTaskDisplay() {
        // Clear previous task items
        taskInfoContainer.removeAllViews();
        
        // Also update week view if it's visible
        if (isWeekView) {
            loadWeekTasksForSelectedDate();
        }
        
        if (tasksForSelectedDate.isEmpty()) {
            // Show no tasks message
            TextView noTasksView = new TextView(this);
            noTasksView.setText("Không có nhiệm vụ nào trong ngày.");
            noTasksView.setTextSize(16);
            noTasksView.setTextColor(Color.parseColor("#666666"));
            noTasksView.setGravity(android.view.Gravity.CENTER);
            noTasksView.setPadding(16, 32, 16, 8);
            taskInfoContainer.addView(noTasksView);
            
            TextView addTaskPrompt = new TextView(this);
            addTaskPrompt.setText("Nhấn + để tạo công việc của bạn.");
            addTaskPrompt.setTextSize(14);
            addTaskPrompt.setTextColor(Color.parseColor("#999999"));
            addTaskPrompt.setGravity(android.view.Gravity.CENTER);
            addTaskPrompt.setPadding(16, 0, 16, 32);
            taskInfoContainer.addView(addTaskPrompt);
        } else {
            // Show tasks for the selected date
            for (TodoTask task : tasksForSelectedDate) {
                View taskItemView = createTaskItemView(task);
                taskInfoContainer.addView(taskItemView);
            }
        }
    }
    
    private View createTaskItemView(TodoTask task) {
        LinearLayout taskItem = new LinearLayout(this);
        taskItem.setOrientation(LinearLayout.HORIZONTAL);
        taskItem.setPadding(0, dpToPx(12), dpToPx(16), dpToPx(12));
        taskItem.setGravity(android.view.Gravity.CENTER_VERTICAL);
        taskItem.setBackgroundResource(R.drawable.task_item_background);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(8), 0, dpToPx(8), dpToPx(8)); // Giảm margin trái phải xuống 8dp
        taskItem.setLayoutParams(params);
        
        // Set elevation for shadow effect
        taskItem.setElevation(4f);
        
        // Blue left border - sử dụng drawable có bo tròn, khác màu nếu completed
        View leftBorder = new View(this);
        if (task.isCompleted()) {
            leftBorder.setBackgroundResource(R.drawable.task_left_border_completed);
        } else {
            leftBorder.setBackgroundResource(R.drawable.task_left_border);
        }
        LinearLayout.LayoutParams borderParams = new LinearLayout.LayoutParams(dpToPx(6), LinearLayout.LayoutParams.MATCH_PARENT);
        leftBorder.setLayoutParams(borderParams);
        taskItem.addView(leftBorder);
        
        // Content container
        LinearLayout contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(dpToPx(16), dpToPx(8), 0, dpToPx(8));
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contentParams.weight = 1;
        contentContainer.setLayoutParams(contentParams);
        
        // Task title
        TextView taskTitle = new TextView(this);
        taskTitle.setText(task.getTitle());
        taskTitle.setTextSize(16);
        taskTitle.setTextColor(Color.parseColor("#000000"));
        taskTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Gạch ngang nếu task đã hoàn thành
        if (task.isCompleted()) {
            taskTitle.setPaintFlags(taskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            taskTitle.setTextColor(Color.parseColor("#888888")); // Màu nhạt hơn
        }
        
        contentContainer.addView(taskTitle);
        
        // Time and icons container
        LinearLayout timeIconContainer = new LinearLayout(this);
        timeIconContainer.setOrientation(LinearLayout.HORIZONTAL);
        timeIconContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
        timeIconContainer.setPadding(0, dpToPx(4), 0, 0);
        
        // Task time - logic màu đỏ nếu thời gian đã qua
        if (task.getDueTime() != null && !task.getDueTime().equals("Không")) {
            TextView timeText = new TextView(this);
            timeText.setText(task.getDueTime());
            timeText.setTextSize(14);
            
            // Logic kiểm tra thời gian: đỏ nếu nhỏ hơn thời gian hiện tại
            boolean isOverdue = isTimeOverdue(task.getDueTime());
            if (isOverdue && !task.isCompleted()) {
                timeText.setTextColor(Color.parseColor("#FF0000")); // Red color
            } else {
                timeText.setTextColor(Color.parseColor("#000000")); // Black color
            }
            
            // KHÔNG gạch ngang thời gian, chỉ gạch tiêu đề
            // Thời gian luôn giữ nguyên màu sắc và style bình thường
            
            timeText.setPadding(0, 0, dpToPx(8), 0);
            timeIconContainer.addView(timeText);
        }
        
        // Notification icon if has reminder
        if (task.isHasReminder()) {
            ImageView notificationIcon = new ImageView(this);
            notificationIcon.setImageResource(R.drawable.ic_notifications);
            notificationIcon.setColorFilter(Color.parseColor("#666666"));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(18), dpToPx(18));
            iconParams.setMarginEnd(dpToPx(6));
            notificationIcon.setLayoutParams(iconParams);
            timeIconContainer.addView(notificationIcon);
        }
        
        // Repeat icon if repeating
        if (task.isRepeating()) {
            ImageView repeatIcon = new ImageView(this);
            repeatIcon.setImageResource(R.drawable.ic_repeat);
            repeatIcon.setColorFilter(Color.parseColor("#666666"));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(18), dpToPx(18));
            repeatIcon.setLayoutParams(iconParams);
            timeIconContainer.addView(repeatIcon);
        }
        
        contentContainer.addView(timeIconContainer);
        taskItem.addView(contentContainer);
        
        // Right side - Star icon cho important (giống màn hình chính)
        if (task.isImportant()) {
            ImageView starIcon = new ImageView(this);
            starIcon.setImageResource(R.drawable.ic_star_filled); // Sử dụng sao vàng
            LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
            starParams.setMarginEnd(dpToPx(8));
            starIcon.setLayoutParams(starParams);
            taskItem.addView(starIcon);
        }
        
        // Add click listener to open task detail
        taskItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskDetailActivity.class);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
            startActivity(intent);
        });
        
        return taskItem;
    }
    
    private void showDateTimePickerDialog(DateTimePickerDialog.OnDateTimeSelectedListener listener) {
        DateTimePickerDialog dateTimeDialog = new DateTimePickerDialog(this, listener);
        dateTimeDialog.show();
    }
    
    private void navigateMonth(int direction) {
        if (isWeekView) {
            // Nếu đang ở chế độ tuần, di chuyển theo tuần
            selectedDate.add(Calendar.WEEK_OF_YEAR, direction);
            selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH);
            
            // Cập nhật currentCalendar để sync với tháng của tuần mới
            currentCalendar.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
            currentCalendar.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
            
            loadWeekView();
            loadTasksForSelectedDate();
        } else {
            // Nếu đang ở chế độ tháng, di chuyển theo tháng
            currentCalendar.add(Calendar.MONTH, direction);
            
            // Cập nhật selectedDate để theo tháng mới nếu cần
            if (currentCalendar.get(Calendar.YEAR) != selectedDate.get(Calendar.YEAR) ||
                currentCalendar.get(Calendar.MONTH) != selectedDate.get(Calendar.MONTH)) {
                // Reset selected day để tránh conflict
                selectedDay = -1;
            }
            
            loadCalendar();
        }
    }
    
    private void toggleCalendarView() {
        isWeekView = !isWeekView;
        
        if (isWeekView) {
            // Switch to week view
            calendarScrollView.setVisibility(View.GONE);
            weekViewContainer.setVisibility(View.VISIBLE);
            btnToggleCalendar.setImageResource(R.drawable.ic_expand_more);
            loadWeekView();
        } else {
            // Switch to month view
            calendarScrollView.setVisibility(View.VISIBLE);
            weekViewContainer.setVisibility(View.GONE);
            btnToggleCalendar.setImageResource(R.drawable.ic_expand_less);
        }
    }
    
    private void loadWeekView() {
        weekGrid.removeAllViews();
        
        // Get current week based on selected date
        Calendar weekStart = (Calendar) selectedDate.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        
        // Update month and year display for week view
        String monthName = monthFormat.format(selectedDate.getTime()).toUpperCase();
        String yearText = yearFormat.format(selectedDate.getTime());
        
        tvMonth.setText("THÁNG " + (selectedDate.get(Calendar.MONTH) + 1));
        tvYear.setText(yearText);
        
        for (int i = 0; i < 7; i++) {
            Calendar dayCalendar = (Calendar) weekStart.clone();
            dayCalendar.add(Calendar.DAY_OF_MONTH, i);
            
            TextView dayView = createWeekDayView(dayCalendar);
            weekGrid.addView(dayView);
        }
        
        // Load tasks for selected date in week view
        loadWeekTasksForSelectedDate();
    }
    
    private void loadWeekTasksForSelectedDate() {
        weekTaskInfoContainer.removeAllViews();
        
        if (tasksForSelectedDate.isEmpty()) {
            // Show empty state message
            TextView addTaskPrompt = new TextView(this);
            addTaskPrompt.setText("Nhấn + để tạo công việc của bạn.");
            addTaskPrompt.setTextSize(14);
            addTaskPrompt.setTextColor(Color.parseColor("#999999"));
            addTaskPrompt.setGravity(android.view.Gravity.CENTER);
            addTaskPrompt.setPadding(16, 0, 16, 32);
            weekTaskInfoContainer.addView(addTaskPrompt);
        } else {
            // Show tasks for the selected date
            for (TodoTask task : tasksForSelectedDate) {
                View taskItemView = createTaskItemView(task);
                weekTaskInfoContainer.addView(taskItemView);
            }
        }
    }
    
    private TextView createWeekDayView(Calendar dayCalendar) {
        TextView dayView = new TextView(this);
        int day = dayCalendar.get(Calendar.DAY_OF_MONTH);
        dayView.setText(String.valueOf(day));
        dayView.setTextSize(16);
        dayView.setGravity(android.view.Gravity.CENTER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 120);
        params.weight = 1;
        params.setMargins(4, 4, 4, 4);
        dayView.setLayoutParams(params);
        dayView.setPadding(8, 16, 8, 16);
        
        // Check if it's selected day
        boolean isSelected = isSameDay(dayCalendar, selectedDate);
        boolean isToday = isSameDay(dayCalendar, Calendar.getInstance());
        
        // Check if day has tasks
        boolean hasTasksForDay = hasTasks(dayCalendar.get(Calendar.YEAR), dayCalendar.get(Calendar.MONTH), day);
        if (hasTasksForDay) {
            // Add blue dot indicator below the day number
            dayView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.task_indicator_dot);
            dayView.setCompoundDrawablePadding(4);
        }
        
        // Chỉ highlight ngày được chọn, không highlight today
        if (isSelected) {
            dayView.setBackgroundResource(R.drawable.calendar_day_selected);
            dayView.setTextColor(Color.WHITE);
        } else {
            // Không highlight today nữa, chỉ sử dụng style bình thường
            dayView.setTextColor(Color.parseColor("#333333"));
            dayView.setBackgroundResource(R.drawable.calendar_day_normal);
        }
        
        // Set click listener
        dayView.setOnClickListener(v -> {
            selectedDay = day;
            selectedDate.set(Calendar.YEAR, dayCalendar.get(Calendar.YEAR));
            selectedDate.set(Calendar.MONTH, dayCalendar.get(Calendar.MONTH));
            selectedDate.set(Calendar.DAY_OF_MONTH, day);
            loadWeekView();
            loadTasksForSelectedDate();
        });
        
        return dayView;
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private boolean isTimeOverdue(String taskTime) {
        try {
            // Get current time
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
            
            // Parse task time (format: "HH:mm" hoặc "H:mm")
            String[] timeParts = taskTime.split(":");
            if (timeParts.length == 2) {
                int taskHour = Integer.parseInt(timeParts[0]);
                int taskMinute = Integer.parseInt(timeParts[1]);
                
                // Convert to minutes for easy comparison
                int currentTotalMinutes = currentHour * 60 + currentMinute;
                int taskTotalMinutes = taskHour * 60 + taskMinute;
                
                return taskTotalMinutes < currentTotalMinutes;
            }
        } catch (Exception e) {
            // If parsing fails, don't mark as overdue
            return false;
        }
        return false;
    }

    private boolean isTaskOnDate(TodoTask task, String targetDate) {
        try {
            // Nếu task không có ngày đáo hạn, bỏ qua
            if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
                return false;
            }
            
            // Parse ngày gốc của task
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
            
            // Nếu ngày target trước ngày gốc, không hiển thị
            if (targetCalendar.before(taskDate)) {
                return false;
            }
            
            // Nếu task không lặp lại, chỉ hiển thị trong ngày gốc
            if (!task.isRepeating() || task.getRepeatType() == null || task.getRepeatType().equals("Không có")) {
                return task.getDueDate().equals(targetDate);
            }
            
            // Kiểm tra các loại lặp lại
            switch (task.getRepeatType()) {
                case "Hàng ngày":
                    // Hiển thị mọi ngày từ ngày gốc trở đi
                    return !targetCalendar.before(taskDate);
                    
                case "Hàng tuần":
                    // Hiển thị vào cùng thứ trong tuần
                    if (targetCalendar.get(Calendar.DAY_OF_WEEK) == taskDate.get(Calendar.DAY_OF_WEEK)) {
                        // Kiểm tra xem có phải là tuần hợp lệ không (mỗi 7 ngày)
                        long diffInMillis = targetCalendar.getTimeInMillis() - taskDate.getTimeInMillis();
                        long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                        return diffInDays >= 0 && diffInDays % 7 == 0;
                    }
                    return false;
                    
                case "Hàng tháng":
                    // Hiển thị vào cùng ngày trong tháng
                    if (targetCalendar.get(Calendar.DAY_OF_MONTH) == taskDate.get(Calendar.DAY_OF_MONTH)) {
                        // Kiểm tra xem có phải tháng hợp lệ không
                        int taskYear = taskDate.get(Calendar.YEAR);
                        int taskMonth = taskDate.get(Calendar.MONTH);
                        int targetYear = targetCalendar.get(Calendar.YEAR);
                        int targetMonth = targetCalendar.get(Calendar.MONTH);
                        
                        // Tính số tháng chênh lệch
                        int monthDiff = (targetYear - taskYear) * 12 + (targetMonth - taskMonth);
                        return monthDiff >= 0;
                    }
                    return false;
                    
                default:
                    return task.getDueDate().equals(targetDate);
            }
            
        } catch (Exception e) {
            // Nếu có lỗi parsing, chỉ so sánh ngày gốc
            return task.getDueDate().equals(targetDate);
        }
    }
}
