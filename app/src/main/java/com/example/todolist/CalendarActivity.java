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
import com.example.todolist.cache.TaskCache;
import com.example.todolist.manager.NavigationDrawerManager;
import com.example.todolist.manager.ThemeManager;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
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
import java.util.Locale; 
public class CalendarActivity extends AppCompatActivity
        implements CalendarViewHelper.OnDayClickListener, CalendarTaskHelper.TaskLoadListener,
        NavigationDrawerManager.NavigationListener, ThemeManager.ThemeChangeListener, 
        TaskCache.TaskCacheListener {
    private TextView tvMonth, tvYear;
    private GridLayout calendarGrid;
    private LinearLayout taskInfoContainer, weekTaskInfoContainer;
    private ImageView btnPrevMonth, btnNextMonth, btnToggleCalendar;
    private FloatingActionButton fabAdd;
    private View calendarScrollView, weekViewContainer;
    private LinearLayout weekGrid;
    private DrawerLayout drawerLayout;
    private NavigationDrawerManager navigationDrawerManager;
    private ThemeManager themeManager;
    private Calendar currentCalendar;
    private Calendar selectedDate;
    private SimpleDateFormat monthFormat;
    private SimpleDateFormat yearFormat;
    private TaskService taskService;
    private TaskCache taskCache;
    private AddTaskHandler addTaskHandler;
    private int selectedDay = -1;
    private boolean isWeekView = false;
    private List<Task> tasksForSelectedDate = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        initViews();
        initManagers();
        initCalendar();
        setupBottomNavigation();
        loadCalendar();
        loadTasksFromCache(); 
        handleDrawerIntent();
    }
    private void initViews() {
        // Navigation components
        drawerLayout = findViewById(R.id.drawer_layout);
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
        taskService = new TaskService(this, null);
        taskCache = TaskCache.getInstance();
        taskCache.addListener(this);
        taskService.loadTasks(); 
        
        setupClickListeners();
    }
    private void initManagers() {
        navigationDrawerManager = new NavigationDrawerManager(this, drawerLayout, this);
        themeManager = new ThemeManager(this, this);
        if (themeManager != null) {
            themeManager.applyCurrentTheme();
        }
    }
    private void setupClickListeners() {
        addTaskHandler = new AddTaskHandler(this, task -> {
            loadCalendar();
            loadTasksFromCache();
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
        monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        selectedDate.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
        selectedDate.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH)); 
        selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay);
    }
    private void setupBottomNavigation() {
        LinearLayout btnNavMenu = findViewById(R.id.btn_nav_menu);
        LinearLayout btnNavTasks = findViewById(R.id.btn_nav_tasks);
        LinearLayout btnNavCalendar = findViewById(R.id.btn_nav_calendar);
        UnifiedNavigationHelper.setupBottomNavigation(this, btnNavMenu, btnNavTasks,
                btnNavCalendar, null, "calendar");
        UnifiedNavigationHelper.initializeDrawerForActivity(this, drawerLayout, this);
    }

    private void handleDrawerIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("open_drawer", false)) {
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
    }
    private void updateMonthYearDisplay() {
        Calendar displayCalendar = isWeekView ? selectedDate : currentCalendar;
  
        tvMonth.setText(monthFormat.format(displayCalendar.getTime()).toUpperCase());
        tvYear.setText(yearFormat.format(displayCalendar.getTime()));
    }
    private void loadTasksForSelectedDate() {
        String dateString = CalendarTaskHelper.formatSelectedDate(selectedDate, selectedDay);
        CalendarTaskHelper.loadTasksForDate(this, dateString, this);
    }

    @Override
    public void onDayClick(int day) {
        selectedDay = day;
        selectedDate.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
        selectedDate.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH));
        selectedDate.set(Calendar.DAY_OF_MONTH, day);
        loadTasksFromCache();
        loadCalendar();
    }
    
    @Override
    public void onWeekDayClick(int day, Calendar dayCalendar) {
        selectedDay = day;
        selectedDate.set(Calendar.YEAR, dayCalendar.get(Calendar.YEAR));
        selectedDate.set(Calendar.MONTH, dayCalendar.get(Calendar.MONTH));
        selectedDate.set(Calendar.DAY_OF_MONTH, day);
        loadTasksFromCache();
        loadCalendar();
    }
    
    private void loadTasksFromCache() {
        String dateString = CalendarTaskHelper.formatSelectedDate(selectedDate, selectedDay);
        tasksForSelectedDate = taskCache.getTasksForDate(dateString);
        updateTaskDisplay();
    }

    @Override
    public void onTasksLoaded(List<Task> tasks) {
        tasksForSelectedDate = tasks;
        updateTaskDisplay();
    }

    @Override
    public void onTasksUpdated(List<Task> allTasks) {
        if (selectedDay != -1) {
            loadTasksFromCache();
        }
    }

    @Override
    public void onTaskAdded(Task task) {
        if (selectedDay != -1) {
            loadTasksFromCache();
        }
    }

    @Override
    public void onTaskUpdated(Task task) {
        if (selectedDay != -1) {
            loadTasksFromCache();
        }
    }

    @Override
    public void onTaskDeleted(String taskId) {
        if (selectedDay != -1) {
            loadTasksFromCache();
        }
    }
    
    private void updateTaskDisplay() {
        CalendarTaskHelper.updateTaskDisplay(this, taskInfoContainer, tasksForSelectedDate);
        if (isWeekView) {
            CalendarTaskHelper.updateTaskDisplay(this, weekTaskInfoContainer, tasksForSelectedDate);
        }
    }
    private void navigateMonth(int direction) {
        // Add smooth navigation with UIOptimizer
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
  
    @Override
    public void onThemeSelected() {
     
    }
    @Override
    public void onUtilitiesSelected() {
     
    }
    @Override
    public void onContactSelected() {

    }
    @Override
    public void onSettingsSelected() {

    }
    @Override
    public void onThemeChanged(com.example.todolist.manager.ThemeManager.ThemeColor themeColor) {
        recreate();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }
    private Context updateBaseContextLocale(Context context) {
        String languageName = SettingsManager.getLanguage(context); 
        String languageCode;
        if (languageName.equals("English")) {
            languageCode = "en";
        } else {
            languageCode = "vi"; 
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);

        return context.createConfigurationContext(configuration);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskCache != null) {
            taskCache.removeListener(this);
        }
    }
}
