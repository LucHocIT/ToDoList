package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.todolist.manager.AuthManager;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.util.ProfileHelper;
import com.example.todolist.view.SimplePieChartView;
import com.example.todolist.manager.NavigationDrawerManager;
import com.example.todolist.view.BottomNavigationManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    
    private TextView tvUserName, tvCompletedTasks, tvPendingTasks;
    private TextView tvWeekRange, tvFilterText;
    private ImageView imgUserAvatar, btnPrevWeek, btnNextWeek;
    private LinearLayout filterDropdown, legendContainer;
    
    // Chart Views
    private View chartBarSunday, chartBarMonday, chartBarTuesday, chartBarWednesday;
    private View chartBarThursday, chartBarFriday, chartBarSaturday;
    
    private NavigationDrawerManager drawerManager;
    private TaskRepository taskRepository;
    private AuthManager authManager;
    private Calendar currentWeekStart;
    private SimplePieChartView pieChartView;
    private ProfileHelper profileHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Initialize managers and helper
        authManager = AuthManager.getInstance();
        authManager.initialize(this);
        taskRepository = new TaskRepository(this);
        profileHelper = new ProfileHelper(this);
        
        initViews();
        setupBottomNavigation();
        setupDrawer();
        setupClickListeners();
        loadUserData();
        loadStatistics();
        initWeekNavigation();
    }
    
    private void initViews() {
        // Bottom navigation
        LinearLayout btnNavMenu = findViewById(R.id.btn_nav_menu);
        LinearLayout btnNavTasks = findViewById(R.id.btn_nav_tasks);
        LinearLayout btnNavCalendar = findViewById(R.id.btn_nav_calendar);
        LinearLayout btnNavProfile = findViewById(R.id.btn_nav_profile);
        
        // Profile elements
        imgUserAvatar = findViewById(R.id.img_user_avatar);
        tvUserName = findViewById(R.id.tv_user_name);
        tvCompletedTasks = findViewById(R.id.tv_completed_tasks);
        tvPendingTasks = findViewById(R.id.tv_pending_tasks);
        tvWeekRange = findViewById(R.id.tv_week_range);
        tvFilterText = findViewById(R.id.tv_filter_text);
        
        // Chart bars
        chartBarSunday = findViewById(R.id.chart_bar_sunday);
        chartBarMonday = findViewById(R.id.chart_bar_monday);
        chartBarTuesday = findViewById(R.id.chart_bar_tuesday);
        chartBarWednesday = findViewById(R.id.chart_bar_wednesday);
        chartBarThursday = findViewById(R.id.chart_bar_thursday);
        chartBarFriday = findViewById(R.id.chart_bar_friday);
        chartBarSaturday = findViewById(R.id.chart_bar_saturday);
        
        // Week navigation
        btnPrevWeek = findViewById(R.id.btn_prev_week);
        btnNextWeek = findViewById(R.id.btn_next_week);
        
        // Filter and pie chart
        filterDropdown = findViewById(R.id.filter_dropdown);
        pieChartView = findViewById(R.id.pie_chart_view);
        legendContainer = findViewById(R.id.legend_container);
    }
    
    private void setupBottomNavigation() {
        BottomNavigationManager.setupForActivity(this, BottomNavigationManager.SCREEN_PROFILE);
    }
    
    private void setupDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            NavigationDrawerManager.NavigationListener navigationListener = new NavigationDrawerManager.NavigationListener() {
                @Override
                public void onThemeSelected() { handleDrawerNavigation("theme"); }
                
                @Override
                public void onUtilitiesSelected() { handleDrawerNavigation("utilities"); }
                
                @Override
                public void onContactSelected() { handleDrawerNavigation("contact"); }
                
                @Override
                public void onSettingsSelected() { handleDrawerNavigation("settings"); }
            };
            
            drawerManager = new NavigationDrawerManager(this, drawerLayout, navigationListener);
        }
        
        if (getIntent().getBooleanExtra("open_drawer", false)) {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            }
        }
    }
    
    private void setupClickListeners() {
        imgUserAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(this, SyncAccountActivity.class);
            startActivity(intent);
        });
        
        btnPrevWeek.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1);
            updateWeekDisplay();
        });
        
        btnNextWeek.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1);
            updateWeekDisplay();
        });
        
        filterDropdown.setOnClickListener(v -> {
            profileHelper.showFilterDialog(filterDropdown, tvFilterText);
        });
    }
    
    private void initWeekNavigation() {
        currentWeekStart = Calendar.getInstance();
        int dayOfWeek = currentWeekStart.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        currentWeekStart.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
        
        updateWeekDisplay();
    }
    
    private void updateWeekDisplay() {
        profileHelper.updateWeekDisplay(currentWeekStart, new ProfileHelper.WeekNavigationListener() {
            @Override
            public void onWeekChanged(String weekRange) {
                tvWeekRange.setText(weekRange);
                updateWeeklyChart();
            }
            
            @Override
            public void onNextWeekVisibilityChanged(boolean visible) {
                btnNextWeek.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        });
    }
    
    private void loadUserData() {
        if (authManager.isSignedIn()) {
            String userName = authManager.getCurrentUserName();
            String userEmail = authManager.getCurrentUserEmail();
            tvUserName.setTextColor(getResources().getColor(android.R.color.black));
            tvUserName.setTextSize(20);
            
            if (userName != null && !userName.isEmpty()) {
                tvUserName.setText(userName);
            } else if (userEmail != null && !userEmail.isEmpty()) {
                tvUserName.setText(userEmail.split("@")[0]);
            } else {
                tvUserName.setText("Google User");
            }
            
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                setUserAvatar(currentUser);
            } else {
                imgUserAvatar.setImageResource(R.drawable.ic_person);
            }
        } else {
            tvUserName.setText("Bấm để đăng nhập");
            tvUserName.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvUserName.setTextSize(14);
            imgUserAvatar.setImageResource(R.drawable.user);
        }
    }

    private void setUserAvatar(FirebaseUser user) {
        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(imgUserAvatar);
        } else {
            imgUserAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    private void loadStatistics() {
        profileHelper.loadStatistics(new ProfileHelper.StatisticsListener() {
            @Override
            public void onStatisticsLoaded(int completedTasks, int pendingTasks) {
                runOnUiThread(() -> {
                    tvCompletedTasks.setText(String.valueOf(completedTasks));
                    tvPendingTasks.setText(String.valueOf(pendingTasks));
                    updateWeeklyChart();
                    updatePieChart();
                });
            }

            @Override
            public void onStatisticsError() {
                runOnUiThread(() -> {
                    tvCompletedTasks.setText("34");
                    tvPendingTasks.setText("3");
                });
            }
        });
    }
    
    private void updateWeeklyChart() {
        profileHelper.updateWeeklyChart(currentWeekStart, new ProfileHelper.ChartUpdateListener() {
            @Override
            public void onChartUpdated(int[] taskCounts, boolean hasData) {
                runOnUiThread(() -> updateChartBars(taskCounts, hasData));
            }
        });
    }
    
    private void updateChartBars(int[] taskCounts, boolean hasData) {
        View[] chartBars = {
            chartBarSunday, chartBarMonday, chartBarTuesday, chartBarWednesday,
            chartBarThursday, chartBarFriday, chartBarSaturday
        };
        
        TextView noDataMessage = findViewById(R.id.tv_no_chart_data);
        if (noDataMessage != null) {
            noDataMessage.setVisibility(hasData ? View.GONE : View.VISIBLE);
        }
        
        int maxHeight = 100;
        for (int i = 0; i < 7; i++) {
            if (chartBars[i] != null) {
                int height = Math.min(taskCounts[i] * 12, maxHeight);
                
                android.view.ViewGroup.LayoutParams params = chartBars[i].getLayoutParams();
                params.height = (int) (height * getResources().getDisplayMetrics().density);
                chartBars[i].setLayoutParams(params);
                
                if (taskCounts[i] > 0) {
                    chartBars[i].setBackgroundColor(0xFF5C9CFF);
                } else {
                    chartBars[i].setBackgroundColor(0xFFE0E0E0);
                }
            }
        }
    }
    
    private void updatePieChart() {
        profileHelper.updatePieChart(new ProfileHelper.PieChartListener() {
            @Override
            public void onPieChartUpdated(Map<String, Integer> categoryCount) {
                runOnUiThread(() -> {
                    if (pieChartView != null) {
                        pieChartView.setData(categoryCount);
                    }
                    profileHelper.updateCategoryLegend(legendContainer, categoryCount);
                });
            }
        });
    }
    
    private void handleDrawerNavigation(String destination) {
        Intent intent = null;
        
        switch (destination) {
            case "theme":
                intent = new Intent(this, com.example.todolist.ThemeSelectionActivity.class);
                break;
            case "settings":
                intent = new Intent(this, SettingsActivity.class);
                break;
            case "tasks":
                intent = new Intent(this, MainActivity.class);
                break;
            case "calendar":
                intent = new Intent(this, CalendarActivity.class);
                break;
            case "completed":
                intent = new Intent(this, CompletedTasksActivity.class);
                break;
        }
        
        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        loadStatistics();
    }
}
