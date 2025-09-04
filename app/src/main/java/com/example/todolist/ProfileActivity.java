package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.PopupMenu;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.todolist.CalendarActivity;
import com.example.todolist.CompletedTasksActivity;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.SettingsActivity;
import com.example.todolist.SyncAccountActivity;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.model.Task;
import com.example.todolist.model.Category;
import com.example.todolist.service.CategoryService;
import com.example.todolist.view.SimplePieChartView;
import com.example.todolist.manager.NavigationDrawerManager;
import com.example.todolist.view.BottomNavigationManager;
import com.example.todolist.manager.AuthManager;
import com.example.todolist.manager.SyncManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {
    
    private LinearLayout btnNavMenu, btnNavTasks, btnNavCalendar, btnNavProfile;
    private LinearLayout btnAuthGoogle, btnSyncSettings, btnAboutApp;
    private TextView tvUserName, tvUserEmail, tvCompletedTasks, tvPendingTasks;
    private TextView tvWeekRange, tvFilterText;
    private ImageView imgUserAvatar, btnPrevWeek, btnNextWeek;
    private LinearLayout filterDropdown;
    
    // Chart Views
    private View chartBarSunday, chartBarMonday, chartBarTuesday, chartBarWednesday;
    private View chartBarThursday, chartBarFriday, chartBarSaturday;
    
    private NavigationDrawerManager drawerManager;
    private TaskRepository taskRepository;
    private AuthManager authManager;
    private SyncManager syncManager;
    private Calendar currentWeekStart;
    private SimplePieChartView pieChartView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Initialize managers
        authManager = AuthManager.getInstance();
        authManager.initialize(this);
        // syncManager = new SyncManager(this);
        
        initViews();
        setupBottomNavigation();
        setupDrawer();
        setupClickListeners();
        loadUserData();
        loadStatistics();
        initWeekNavigation();
    }
    
    private void initViews() {
        // Bottom navigation buttons (từ include bottom_navigation.xml)
        btnNavMenu = findViewById(R.id.btn_nav_menu);
        btnNavTasks = findViewById(R.id.btn_nav_tasks);
        btnNavCalendar = findViewById(R.id.btn_nav_calendar);
        btnNavProfile = findViewById(R.id.btn_nav_profile);
        
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
        
        // Filter dropdown
        filterDropdown = findViewById(R.id.filter_dropdown);
        
        // Pie chart view
        pieChartView = findViewById(R.id.pie_chart_view);
        
        // Initialize repository
        taskRepository = new TaskRepository(this);
    }
    
    private void setupBottomNavigation() {
        // Setup unified bottom navigation
        BottomNavigationManager.setupForActivity(this, BottomNavigationManager.SCREEN_PROFILE);
    }
    
    private void setupDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            NavigationDrawerManager.NavigationListener navigationListener = new NavigationDrawerManager.NavigationListener() {
                @Override
                public void onThemeSelected() {
                    handleDrawerNavigation("theme");
                }
                
                @Override
                public void onUtilitiesSelected() {
                    handleDrawerNavigation("utilities");
                }
                
                @Override
                public void onContactSelected() {
                    handleDrawerNavigation("contact");
                }
                
                @Override
                public void onSettingsSelected() {
                    handleDrawerNavigation("settings");
                }
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
        // Avatar click listener - navigate to sync screen
        imgUserAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(this, SyncAccountActivity.class);
            startActivity(intent);
        });
        
        // Week navigation listeners
        btnPrevWeek.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1);
            updateWeekDisplay();
        });
        
        btnNextWeek.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1);
            updateWeekDisplay();
        });
        
        // Filter dropdown listener
        filterDropdown.setOnClickListener(v -> {
            showFilterDialog();
        });
    }
    
    // Removed initCountdown() and updateCountdown() since countdown UI was removed
    
    private void initWeekNavigation() {
        currentWeekStart = Calendar.getInstance();
        // Set to start of current week (Monday)
        int dayOfWeek = currentWeekStart.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        currentWeekStart.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
        
        updateWeekDisplay();
    }
    
    private void updateWeekDisplay() {
        SimpleDateFormat format = new SimpleDateFormat("M/d", Locale.getDefault());
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        
        String weekRange = format.format(currentWeekStart.getTime()) + "-" + format.format(weekEnd.getTime());
        tvWeekRange.setText(weekRange);
        
        // Check if current week is the actual current week
        Calendar currentActualWeek = Calendar.getInstance();
        int dayOfWeek = currentActualWeek.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        currentActualWeek.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
        
        // Hide next week button if we're viewing current week
        if (isSameWeek(currentWeekStart, currentActualWeek)) {
            btnNextWeek.setVisibility(View.GONE);
        } else {
            btnNextWeek.setVisibility(View.VISIBLE);
        }
        
        updateWeeklyChart();
    }
    
    private boolean isSameWeek(Calendar week1, Calendar week2) {
        return week1.get(Calendar.YEAR) == week2.get(Calendar.YEAR) &&
               week1.get(Calendar.WEEK_OF_YEAR) == week2.get(Calendar.WEEK_OF_YEAR);
    }
    
    private void showFilterDialog() {
        PopupMenu popup = new PopupMenu(this, filterDropdown);
        
        // Add menu items manually
        popup.getMenu().add(0, 0, 0, "Tất cả");
        popup.getMenu().add(0, 1, 1, "Trong 7 ngày nữa");
        popup.getMenu().add(0, 2, 2, "Trong 30 ngày nữa");
        
        popup.setOnMenuItemClickListener(item -> {
            tvFilterText.setText(item.getTitle());
            // TODO: Apply filter logic based on item.getItemId()
            return true;
        });
        
        popup.show();
    }
    
    private void loadUserData() {
        if (authManager.isSignedIn()) {
            // User is signed in with Google
            String userName = authManager.getCurrentUserName();
            String userEmail = authManager.getCurrentUserEmail();
            
            // Set user name (fallback to email if no display name)
            if (userName != null && !userName.isEmpty()) {
                tvUserName.setText(userName);
            } else if (userEmail != null && !userEmail.isEmpty()) {
                tvUserName.setText(userEmail.split("@")[0]); // Use email prefix as name
            } else {
                tvUserName.setText("Google User");
            }
            
            // Load Google avatar
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                setUserAvatar(currentUser);
            } else {
                imgUserAvatar.setImageResource(R.drawable.ic_person);
            }
        } else {
            // User not signed in - show default
            tvUserName.setText("Local User");
            imgUserAvatar.setImageResource(R.drawable.ic_person);
        }
    }
    
    /**
     * Set user avatar from Google account
     */
    private void setUserAvatar(FirebaseUser user) {
        if (user.getPhotoUrl() != null) {
            // Load avatar from Google account using Glide
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop() // Make it circular
                    .placeholder(R.drawable.ic_person) // Default placeholder
                    .error(R.drawable.ic_person) // Error fallback
                    .into(imgUserAvatar);
        } else {
            // No avatar available, use default
            imgUserAvatar.setImageResource(R.drawable.ic_person);
        }
    }
    
    /*
    private void updateAuthButtonText(String text) {
        // Find TextView inside btnAuthGoogle LinearLayout
        if (btnAuthGoogle != null && btnAuthGoogle.getChildCount() > 1) {
            android.view.View secondChild = btnAuthGoogle.getChildAt(1);
            if (secondChild instanceof TextView) {
                ((TextView) secondChild).setText(text);
            }
        }
    }
    */
    
    private void loadStatistics() {
        // Load all tasks and calculate statistics
        taskRepository.getAllTasks(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                int completedTasks = 0;
                int pendingTasks = 0;
                
                for (Task task : tasks) {
                    if (task.isCompleted()) {
                        completedTasks++;
                    } else {
                        pendingTasks++;
                    }
                }
                
                final int finalCompletedTasks = completedTasks;
                final int finalPendingTasks = pendingTasks;
                runOnUiThread(() -> {
                    tvCompletedTasks.setText(String.valueOf(finalCompletedTasks));
                    tvPendingTasks.setText(String.valueOf(finalPendingTasks));
                    updateWeeklyChart();
                    updatePieChart();
                });
            }

            @Override
            public void onError(String error) {
                // If there's an error, show default values
                runOnUiThread(() -> {
                    tvCompletedTasks.setText("34");
                    tvPendingTasks.setText("3");
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Removed countdown handler cleanup since countdown was removed
    }
    
    private void handleDrawerNavigation(String destination) {
        Intent intent = null;
        
        switch (destination) {
            case "theme":
                intent = new Intent(this, com.example.todolist.ThemeSelectionActivity.class);
                break;
            case "utilities":
                // TODO: Handle utilities navigation
                break;
            case "contact":
                // TODO: Handle contact navigation
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // if (requestCode == AuthManager.getSignInRequestCode()) {
        //     authManager.handleSignInResult(data);
        // }
    }
    
    // AuthManager.AuthListener implementations đã được handle trong constructor
    
    // Helper methods
    private void showSignOutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    // authManager.signOut();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showSyncDialog() {
        String[] options = {
            "Đồng bộ lên Cloud (Upload)",
            "Đồng bộ từ Cloud (Download)", 
            "Đồng bộ hai chiều (Merge)"
        };
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn kiểu đồng bộ")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // syncManager.syncLocalToFirebase();
                            break;
                        case 1:
                            // syncManager.syncFirebaseToLocal();
                            break;
                        case 2:
                            // syncManager.syncBidirectional();
                            break;
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Về ứng dụng")
                .setMessage("ToDoList App v1.0\n\nỨng dụng quản lý nhiệm vụ với đồng bộ Firebase\n\nPhát triển bởi: Your Name")
                .setPositiveButton("OK", null)
                .show();
    }
    
    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void updateWeeklyChart() {
        // Get completed tasks and update chart based on real data
        taskRepository.getCompletedTasks(new TaskRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> completedTasks) {
                runOnUiThread(() -> updateChartWithData(completedTasks));
            }

            @Override
            public void onError(String error) {
                // If error, show empty chart
                runOnUiThread(() -> updateChartWithData(new ArrayList<>()));
            }
        });
    }
    
    private void updateChartWithData(List<Task> completedTasks) {
        // Get the start and end of the current week
        Calendar weekStart = (Calendar) currentWeekStart.clone();
        
        // Array of chart bars (Sunday to Saturday)
        View[] chartBars = {
            chartBarSunday, chartBarMonday, chartBarTuesday, chartBarWednesday,
            chartBarThursday, chartBarFriday, chartBarSaturday
        };
        
        // Count completed tasks for each day of the week
        int[] taskCounts = new int[7]; // Sunday=0, Monday=1, ..., Saturday=6
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        for (Task task : completedTasks) {
            if (task.isCompleted() && task.getCompletionDate() != null) {
                try {
                    // Parse completion date
                    Calendar taskDate = Calendar.getInstance();
                    taskDate.setTime(dateFormat.parse(task.getCompletionDate()));
                    
                    // Check if task completion date is in current week
                    Calendar weekEnd = (Calendar) weekStart.clone();
                    weekEnd.add(Calendar.DAY_OF_YEAR, 6);
                    
                    if (!taskDate.before(weekStart) && !taskDate.after(weekEnd)) {
                        int dayOfWeek = taskDate.get(Calendar.DAY_OF_WEEK) - 1; // Convert to 0-6
                        taskCounts[dayOfWeek]++;
                    }
                } catch (Exception e) {
                    // Skip invalid dates
                }
            }
        }
        
        // Check if all days have zero tasks
        boolean hasAnyData = false;
        for (int count : taskCounts) {
            if (count > 0) {
                hasAnyData = true;
                break;
            }
        }
        
        // Find and update the "no data" message visibility
        TextView noDataMessage = findViewById(R.id.tv_no_chart_data);
        if (noDataMessage != null) {
            noDataMessage.setVisibility(hasAnyData ? View.GONE : View.VISIBLE);
        }
        
        // Update chart bar heights (max height 100dp, scale: 2 tasks per level)
        int maxHeight = 100; // dp
        for (int i = 0; i < 7; i++) {
            if (chartBars[i] != null) {
                // Calculate height: each task = 12.5dp, max 8 tasks = 100dp
                int height = Math.min(taskCounts[i] * 12, maxHeight);
                
                android.view.ViewGroup.LayoutParams params = chartBars[i].getLayoutParams();
                params.height = (int) (height * getResources().getDisplayMetrics().density);
                chartBars[i].setLayoutParams(params);
                
                // Set color based on task count
                if (taskCounts[i] > 0) {
                    chartBars[i].setBackgroundColor(0xFF5C9CFF); // Blue
                } else {
                    chartBars[i].setBackgroundColor(0xFFE0E0E0); // Gray
                }
            }
        }
    }
    
    private boolean isSameDate(String dateStr, Calendar calendar) {
        if (dateStr == null || calendar == null) return false;
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String calendarDateStr = dateFormat.format(calendar.getTime());
        
        return dateStr.equals(calendarDateStr);
    }
    
    private void updatePieChart() {
        // Get incomplete tasks to analyze categories
        taskRepository.getIncompleteTasks(new TaskRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> incompleteTasks) {
                // Also get categories to have complete information
                CategoryService categoryService = new CategoryService(ProfileActivity.this, null);
                categoryService.getAllCategories(new BaseRepository.ListCallback<Category>() {
                    @Override
                    public void onSuccess(List<Category> categories) {
                        runOnUiThread(() -> updatePieChartWithData(incompleteTasks, categories));
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> updatePieChartWithData(incompleteTasks, new ArrayList<>()));
                    }
                });
            }

            @Override
            public void onError(String error) {
                // If error, show empty chart
                runOnUiThread(() -> updatePieChartWithData(new ArrayList<>(), new ArrayList<>()));
            }
        });
    }
    
    private void updatePieChartWithData(List<Task> incompleteTasks, List<Category> categories) {
        // Count tasks by category
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, String> categoryNames = new HashMap<>();
        
        // Create a map of category ID to category name
        for (Category category : categories) {
            categoryNames.put(category.getId(), category.getName());
        }
        
        for (Task task : incompleteTasks) {
            String categoryId = task.getCategoryId();
            String categoryName;
            
            if (categoryId == null || categoryId.isEmpty()) {
                categoryName = "Không có thể loại";
            } else {
                categoryName = categoryNames.getOrDefault(categoryId, "Không có thể loại");
            }
            
            categoryCount.put(categoryName, categoryCount.getOrDefault(categoryName, 0) + 1);
        }
        
        // Update the pie chart with real data
        if (pieChartView != null) {
            pieChartView.setData(categoryCount);
        }
        
        // Update the legend with real data
        updateCategoryLegend(categoryCount);
    }
    
    private void updateCategoryLegend(Map<String, Integer> categoryCount) {
        // Find legend container
        LinearLayout legendContainer = findViewById(R.id.legend_container);
        if (legendContainer != null) {
            legendContainer.removeAllViews();
            
            // Colors for different categories - matching the sample image
            int[] colors = {
                0xFF5C9CFF,  // Blue - Danh sách yêu thích
                0xFF9CC3FF,  // Light Blue - không có thể loại 
                0xFFC8DFFF,  // Lighter Blue - Công việc
                0xFF4A90E2,  // Medium Blue
                0xFF2E7BD6,  // Dark Blue
                0xFF8BB8FF,  // Sky Blue
                0xFF6FA8FF,  // Ocean Blue
                0xFF5A9BFF,  // Royal Blue
                0xFFB8D4FF,  // Pale Blue
                0xFF7AB3FF   // Bright Blue
            };
            int colorIndex = 0;
            
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                String categoryName = entry.getKey();
                Integer count = entry.getValue();
                
                // Create legend item layout
                LinearLayout legendItem = new LinearLayout(this);
                legendItem.setOrientation(LinearLayout.HORIZONTAL);
                legendItem.setGravity(android.view.Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                itemParams.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
                legendItem.setLayoutParams(itemParams);
                
                // Color indicator
                View colorView = new View(this);
                LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(
                    (int) (12 * getResources().getDisplayMetrics().density),
                    (int) (12 * getResources().getDisplayMetrics().density)
                );
                colorParams.rightMargin = (int) (8 * getResources().getDisplayMetrics().density);
                colorView.setLayoutParams(colorParams);
                colorView.setBackgroundColor(colors[colorIndex % colors.length]);
                legendItem.addView(colorView);
                
                // Category name
                TextView nameTextView = new TextView(this);
                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                );
                nameTextView.setLayoutParams(nameParams);
                nameTextView.setText(categoryName);
                nameTextView.setTextSize(14);
                nameTextView.setTextColor(getColor(android.R.color.black));
                legendItem.addView(nameTextView);
                
                // Count
                TextView countTextView = new TextView(this);
                countTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                countTextView.setText(String.valueOf(count));
                countTextView.setTextSize(14);
                countTextView.setTextColor(0xFF666666);
                legendItem.addView(countTextView);
                
                legendContainer.addView(legendItem);
                colorIndex++;
            }
            
            // Show message if no incomplete tasks
            if (categoryCount.isEmpty()) {
                TextView noDataText = new TextView(this);
                noDataText.setText("Không có nhiệm vụ chưa hoàn thành");
                noDataText.setTextSize(14);
                noDataText.setTextColor(0xFF999999);
                noDataText.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams noDataParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                noDataParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
                noDataText.setLayoutParams(noDataParams);
                legendContainer.addView(noDataText);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning from sync screen
        loadUserData();
        loadStatistics();
    }
}
