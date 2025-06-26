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
    private LinearLayout taskInfoContainer;
    private ImageView btnPrevMonth, btnNextMonth;
    private FloatingActionButton fabAdd;
    
    private Calendar currentCalendar;
    private Calendar selectedDate;
    private SimpleDateFormat monthFormat;
    private SimpleDateFormat yearFormat;
    private SimpleDateFormat dayFormat;
    
    private TodoDatabase database;
    private int selectedDay = -1;
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
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        fabAdd = findViewById(R.id.fab_add);
        
        database = TodoDatabase.getInstance(this);
        
        // Setup FAB click listener
        fabAdd.setOnClickListener(v -> showAddTaskDialog());
    }
    
    private void initCalendar() {
        currentCalendar = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        monthFormat = new SimpleDateFormat("MMMM", new Locale("vi", "VN"));
        yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        dayFormat = new SimpleDateFormat("d", Locale.getDefault());
        
        // Set selected date to today
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
                // Handle menu navigation
                Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
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
        
        // Add day headers
        String[] dayHeaders = {"CN", "Th 2", "Th 3", "Th 4", "Th 5", "Th 6", "Th 7"};
        for (String dayHeader : dayHeaders) {
            TextView headerView = createDayHeaderView(dayHeader);
            calendarGrid.addView(headerView);
        }
        
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
            
            if (isSelected) {
                dayView.setBackground(getDrawable(R.drawable.calendar_day_selected));
                dayView.setTextColor(Color.WHITE);
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
        int remainingCells = 49 - totalCells; // 7 headers + 42 day cells
        for (int i = 1; i <= remainingCells; i++) {
            TextView dayView = createDayView(i, false, false);
            calendarGrid.addView(dayView);
        }
        
        loadTasksForSelectedDate();
    }
    
    private TextView createDayHeaderView(String text) {
        TextView textView = new TextView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(2, 8, 2, 8);
        textView.setLayoutParams(params);
        
        textView.setText(text);
        textView.setTextSize(12);
        textView.setTextColor(Color.parseColor("#666666"));
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setPadding(8, 8, 8, 8);
        
        return textView;
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
        
        List<TodoTask> tasks = database.todoDao().getTasksForDate(dateString);
        
        return tasks != null && !tasks.isEmpty();
    }
    
    private void loadTasksForSelectedDate() {
        // Convert selected date to string format that matches TodoTask dueDate
        String dateString = String.format("%04d/%02d/%02d", 
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH) + 1, 
            selectedDay);
        
        new Thread(() -> {
            tasksForSelectedDate = database.todoDao().getTasksForDate(dateString);
            if (tasksForSelectedDate == null) {
                tasksForSelectedDate = new ArrayList<>();
            }
            
            runOnUiThread(() -> {
                updateTaskDisplay();
            });
        }).start();
    }
    
    private void updateTaskDisplay() {
        // Clear previous task items
        taskInfoContainer.removeAllViews();
        
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
        taskItem.setPadding(0, 16, 16, 16);
        taskItem.setGravity(android.view.Gravity.CENTER_VERTICAL);
        taskItem.setBackgroundColor(Color.WHITE);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 1); // Small margin between items
        taskItem.setLayoutParams(params);
        
        // Blue left border
        View leftBorder = new View(this);
        leftBorder.setBackgroundColor(Color.parseColor("#4285F4"));
        LinearLayout.LayoutParams borderParams = new LinearLayout.LayoutParams(8, LinearLayout.LayoutParams.MATCH_PARENT);
        leftBorder.setLayoutParams(borderParams);
        taskItem.addView(leftBorder);
        
        // Content container
        LinearLayout contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(16, 0, 0, 0);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contentParams.weight = 1;
        contentContainer.setLayoutParams(contentParams);
        
        // Task title
        TextView taskTitle = new TextView(this);
        taskTitle.setText(task.getTitle());
        taskTitle.setTextSize(16);
        taskTitle.setTextColor(Color.parseColor("#333333"));
        taskTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        contentContainer.addView(taskTitle);
        
        // Task time if available
        if (task.getDueTime() != null && !task.getDueTime().equals("Không")) {
            LinearLayout timeContainer = new LinearLayout(this);
            timeContainer.setOrientation(LinearLayout.HORIZONTAL);
            timeContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
            timeContainer.setPadding(0, 4, 0, 0);
            
            TextView timeText = new TextView(this);
            timeText.setText(task.getDueTime());
            timeText.setTextSize(14);
            timeText.setTextColor(Color.parseColor("#666666"));
            timeText.setPadding(0, 0, 8, 0);
            timeContainer.addView(timeText);
            
            // Notification icon if has reminder
            if (task.isHasReminder()) {
                ImageView notificationIcon = new ImageView(this);
                notificationIcon.setImageResource(R.drawable.ic_notifications);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(20, 20);
                iconParams.setMarginEnd(8);
                notificationIcon.setLayoutParams(iconParams);
                timeContainer.addView(notificationIcon);
            }
            
            // Repeat icon if repeating
            if (task.isRepeating()) {
                ImageView repeatIcon = new ImageView(this);
                repeatIcon.setImageResource(R.drawable.ic_repeat);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(20, 20);
                repeatIcon.setLayoutParams(iconParams);
                timeContainer.addView(repeatIcon);
            }
            
            contentContainer.addView(timeContainer);
        }
        
        taskItem.addView(contentContainer);
        
        // Right side icons
        LinearLayout rightContainer = new LinearLayout(this);
        rightContainer.setOrientation(LinearLayout.HORIZONTAL);
        rightContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Important icon if task is important
        if (task.isImportant()) {
            ImageView flagIcon = new ImageView(this);
            flagIcon.setImageResource(R.drawable.ic_flag);
            flagIcon.setColorFilter(Color.parseColor("#FFA726"));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(24, 24);
            iconParams.setMarginEnd(8);
            flagIcon.setLayoutParams(iconParams);
            rightContainer.addView(flagIcon);
        }
        
        // Person icon (for assignment, like in the image)
        ImageView personIcon = new ImageView(this);
        personIcon.setImageResource(R.drawable.ic_person_outline);
        personIcon.setColorFilter(Color.parseColor("#999999"));
        LinearLayout.LayoutParams personParams = new LinearLayout.LayoutParams(24, 24);
        personIcon.setLayoutParams(personParams);
        rightContainer.addView(personIcon);
        
        taskItem.addView(rightContainer);
        
        // Add click listener to open task detail
        taskItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskDetailActivity.class);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
            startActivity(intent);
        });
        
        return taskItem;
    }
    
    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        
        // Make dialog background transparent so CardView corners show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        EditText editTaskTitle = dialogView.findViewById(R.id.edit_task_title);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_save);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category_dialog);
        ImageView iconCalendar = dialogView.findViewById(R.id.icon_calendar_dialog);

        // Variables to store selected values (pre-fill with selected date)
        final String[] selectedDate = {String.format("%04d/%02d/%02d", 
            this.selectedDate.get(Calendar.YEAR),
            this.selectedDate.get(Calendar.MONTH) + 1,
            selectedDay)}; 
        final String[] selectedTime = {"Không"}; // Default time
        final String[] selectedReminder = {"Không"}; // Default reminder
        final String[] selectedRepeat = {"Không"}; // Default repeat
        
        // Setup category spinner
        setupCategorySpinner(spinnerCategory);
        
        // Calendar icon click handler
        iconCalendar.setOnClickListener(v -> {
            showDateTimePickerDialog(new DateTimePickerDialog.OnDateTimeSelectedListener() {
                @Override
                public void onDateTimeSelected(String date, String time, String reminder, String repeat) {
                    selectedDate[0] = date;
                    selectedTime[0] = time;
                    selectedReminder[0] = reminder;
                    selectedRepeat[0] = repeat;
                }
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String title = editTaskTitle.getText().toString().trim();
            if (!title.isEmpty()) {
                Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
                String categoryName = selectedCategory != null ? selectedCategory.getName() : "Công việc";
                
                createNewTaskWithDetails(title, categoryName, selectedDate[0], selectedTime[0], selectedReminder[0], selectedRepeat[0]);
                dialog.dismiss();
            } else {
                editTaskTitle.setError("Vui lòng nhập tiêu đề nhiệm vụ");
            }
        });

        dialog.show();
    }
    
    private void createNewTaskWithDetails(String title, String category, String date, String time, String reminder, String repeat) {
        // Only set date if it's not the default today date (meaning user explicitly chose a date)
        String finalDate = null;
        String finalTime = null;
        
        // If user clicked calendar and selected date/time, use those values
        if (date != null && !date.equals("Không")) {
            finalDate = date;
        }
        if (time != null && !time.equals("Không") && !time.equals("12:00")) { // 12:00 is default, don't use it
            finalTime = time;
        }
        
        TodoTask newTask = new TodoTask(title, "", finalDate, finalTime);
        newTask.setCategory(category);
        
        // Set reminder if not "Không" and has time
        if (reminder != null && !reminder.equals("Không") && finalTime != null) {
            newTask.setHasReminder(true);
            newTask.setReminderType(reminder);
        }
        
        // Set repeat if not "Không"
        if (repeat != null && !repeat.equals("Không")) {
            newTask.setRepeating(true);
            newTask.setRepeatType(repeat);
        }
        
        // Add to database
        new Thread(() -> {
            database.todoDao().insertTask(newTask);
            runOnUiThread(() -> {
                // Reload calendar and tasks
                loadCalendar();
                loadTasksForSelectedDate();
                Toast.makeText(this, "Đã thêm nhiệm vụ mới", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    
    private void showDateTimePickerDialog(DateTimePickerDialog.OnDateTimeSelectedListener listener) {
        DateTimePickerDialog dateTimeDialog = new DateTimePickerDialog(this, listener);
        dateTimeDialog.show();
    }
    
    private void setupCategorySpinner(Spinner spinner) {
        new Thread(() -> {
            try {
                List<Category> categories = database.categoryDao().getAllCategories();
                
                // Add default categories if empty
                if (categories.isEmpty()) {
                    Category work = new Category("Công việc", "#FF9800", 0, true);
                    Category personal = new Category("Cá nhân", "#4CAF50", 1, true);
                    Category favorite = new Category("Yêu thích", "#F44336", 2, true);
                    
                    database.categoryDao().insertCategory(work);
                    database.categoryDao().insertCategory(personal);  
                    database.categoryDao().insertCategory(favorite);
                    
                    categories.add(work);
                    categories.add(personal);
                    categories.add(favorite);
                }
                
                final List<Category> finalCategories = categories;
                runOnUiThread(() -> {
                    CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(this, finalCategories);
                    spinner.setAdapter(adapter);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi khi tải danh mục", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
