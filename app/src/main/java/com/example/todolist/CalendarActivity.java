package com.example.todolist;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.todolist.database.TodoDatabase;
import com.example.todolist.manager.NavigationDrawerManager;
import com.example.todolist.manager.ThemeManager;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.AddTaskHandler;
import com.example.todolist.util.CalendarTaskHelper;
import com.example.todolist.util.CalendarViewHelper;
import com.example.todolist.util.SettingsManager;
import com.example.todolist.util.UnifiedNavigationHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale; // Đảm bảo import này

public class CalendarActivity extends AppCompatActivity
        implements CalendarViewHelper.OnDayClickListener, CalendarTaskHelper.TaskLoadListener,
        NavigationDrawerManager.NavigationListener, ThemeManager.ThemeChangeListener {

    private TextView tvMonth, tvYear;
    private GridLayout calendarGrid;
    private LinearLayout taskInfoContainer, weekTaskInfoContainer;
    private ImageView btnPrevMonth, btnNextMonth, btnToggleCalendar;
    private FloatingActionButton fabAdd;
    private View calendarScrollView, weekViewContainer;
    private LinearLayout weekGrid;

    // Navigation components
    private DrawerLayout drawerLayout;
    private NavigationDrawerManager navigationDrawerManager;
    private ThemeManager themeManager;

    private Calendar currentCalendar;
    private Calendar selectedDate;
    // Sử dụng Locale.getDefault() để hiển thị tháng theo ngôn ngữ đã chọn
    private SimpleDateFormat monthFormat;
    private SimpleDateFormat yearFormat;

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
        initManagers();
        initCalendar();
        setupBottomNavigation();
        loadCalendar();

        // Handle drawer open intent from other activities
        handleDrawerIntent();
    }

    private void initViews() {
        // Navigation components
        drawerLayout = findViewById(R.id.drawer_layout);

        // Calendar components
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
        setupClickListeners();
    }

    private void initManagers() {
        // Initialize NavigationDrawerManager
        navigationDrawerManager = new NavigationDrawerManager(this, drawerLayout, this);

        // Initialize ThemeManager
        themeManager = new ThemeManager(this, this);

        // Apply current theme
        if (themeManager != null) {
            themeManager.applyCurrentTheme();
        }
    }

    private void setupClickListeners() {
        addTaskHandler = new AddTaskHandler(this, task -> {
            loadCalendar();
            loadTasksForSelectedDate();
        });

        btnPrevMonth.setOnClickListener(v -> navigateMonth(-1));
        btnNextMonth.setOnClickListener(v -> navigateMonth(1));
        btnToggleCalendar.setOnClickListener(v -> toggleCalendarView());

        fabAdd.setOnClickListener(v -> {
            String selectedDateString = CalendarTaskHelper.formatSelectedDate(selectedDate, selectedDay);
            addTaskHandler.showAddTaskDialog(selectedDateString);
        });
    }

    private void initCalendar() {
        currentCalendar = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        // Sửa đổi: Sử dụng Locale.getDefault() để định dạng tháng theo ngôn ngữ hiện tại của ứng dụng
        monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
    }

    private void setupBottomNavigation() {
        LinearLayout btnNavMenu = findViewById(R.id.btn_nav_menu);
        LinearLayout btnNavTasks = findViewById(R.id.btn_nav_tasks);
        LinearLayout btnNavCalendar = findViewById(R.id.btn_nav_calendar);

        // Use unified navigation helper
        UnifiedNavigationHelper.setupBottomNavigation(this, btnNavMenu, btnNavTasks,
                btnNavCalendar, null, "calendar");

        // Initialize drawer for CalendarActivity
        UnifiedNavigationHelper.initializeDrawerForActivity(this, drawerLayout, this);
    }

    /**
     * Handle drawer open intent from other activities
     */
    private void handleDrawerIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("open_drawer", false)) {
            // Mở drawer sau khi layout đã sẵn sàng
            findViewById(android.R.id.content).post(() -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    private void loadCalendar() {
        updateMonthYearDisplay();

        if (isWeekView) {
            CalendarViewHelper.loadWeekView(this, weekGrid, selectedDate, selectedDay, this);
        } else {
            CalendarViewHelper.loadMonthCalendar(this, calendarGrid, currentCalendar,
                    selectedDate, selectedDay, this);
        }

        loadTasksForSelectedDate();
    }

    private void updateMonthYearDisplay() {
        Calendar displayCalendar = isWeekView ? selectedDate : currentCalendar;
        // Sửa đổi: Lấy tên tháng từ SimpleDateFormat đã được khởi tạo với Locale.getDefault()
        tvMonth.setText(monthFormat.format(displayCalendar.getTime()).toUpperCase()); // Chuyển sang chữ hoa nếu muốn
        tvYear.setText(yearFormat.format(displayCalendar.getTime()));
    }

    private void loadTasksForSelectedDate() {
        String dateString = CalendarTaskHelper.formatSelectedDate(selectedDate, selectedDay);
        CalendarTaskHelper.loadTasksForDate(this, dateString, this);
    }

    // Implementation of CalendarViewHelper.OnDayClickListener
    @Override
    public void onDayClick(int day) {
        selectedDay = day;
        selectedDate.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
        selectedDate.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH));
        selectedDate.set(Calendar.DAY_OF_MONTH, day);
        loadCalendar();
    }

    @Override
    public void onWeekDayClick(int day, Calendar dayCalendar) {
        selectedDay = day;
        selectedDate.set(Calendar.YEAR, dayCalendar.get(Calendar.YEAR));
        selectedDate.set(Calendar.MONTH, dayCalendar.get(Calendar.MONTH));
        selectedDate.set(Calendar.DAY_OF_MONTH, day);
        loadCalendar();
    }

    // Implementation of CalendarTaskHelper.TaskLoadListener
    @Override
    public void onTasksLoaded(List<TodoTask> tasks) {
        tasksForSelectedDate = tasks;
        runOnUiThread(() -> updateTaskDisplay());
    }

    private void updateTaskDisplay() {
        CalendarTaskHelper.updateTaskDisplay(this, taskInfoContainer, tasksForSelectedDate);

        if (isWeekView) {
            CalendarTaskHelper.updateTaskDisplay(this, weekTaskInfoContainer, tasksForSelectedDate);
        }
    }

    private void navigateMonth(int direction) {
        if (isWeekView) {
            selectedDate.add(Calendar.WEEK_OF_YEAR, direction);
            selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH);
            currentCalendar.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
            currentCalendar.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
        } else {
            currentCalendar.add(Calendar.MONTH, direction);
            if (currentCalendar.get(Calendar.YEAR) != selectedDate.get(Calendar.YEAR) ||
                    currentCalendar.get(Calendar.MONTH) != selectedDate.get(Calendar.MONTH)) {
                selectedDay = -1;
            }
        }
        loadCalendar();
    }

    private void toggleCalendarView() {
        isWeekView = !isWeekView;

        if (isWeekView) {
            calendarScrollView.setVisibility(View.GONE);
            weekViewContainer.setVisibility(View.VISIBLE);
            btnToggleCalendar.setImageResource(R.drawable.ic_expand_more);
        } else {
            calendarScrollView.setVisibility(View.VISIBLE);
            weekViewContainer.setVisibility(View.GONE);
            btnToggleCalendar.setImageResource(R.drawable.ic_expand_less);
        }

        loadCalendar();
    }

    // NavigationDrawerManager.NavigationListener implementations
    @Override
    public void onThemeSelected() {
        // Theme selection handled by NavigationDrawerManager
    }

    @Override
    public void onUtilitiesSelected() {
        // Utilities handled by NavigationDrawerManager
    }

    @Override
    public void onContactSelected() {
        // Contact handled by NavigationDrawerManager
    }

    @Override
    public void onSettingsSelected() {
        // Settings handled by NavigationDrawerManager
    }

    // ThemeManager.ThemeChangeListener implementation
    @Override
    public void onThemeChanged(com.example.todolist.manager.ThemeManager.ThemeColor themeColor) {
        // Recreate activity when theme changes
        recreate();
    }

    // --- PHẦN QUAN TRỌNG NHẤT ĐỂ ÁP DỤNG NGÔN NGỮ ---
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }

    private Context updateBaseContextLocale(Context context) {
        String languageName = SettingsManager.getLanguage(context); // Lấy ngôn ngữ đã lưu
        String languageCode;
        if (languageName.equals("English")) {
            languageCode = "en";
        } else {
            languageCode = "vi"; // Mặc định là tiếng Việt
        }

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);

        // Trả về một Context mới với cấu hình đã cập nhật
        return context.createConfigurationContext(configuration);
    }
    // --- KẾT THÚC PHẦN QUAN TRỌNG NHẤT ---

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}

