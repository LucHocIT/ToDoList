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
import com.example.todolist.manager.NavigationDrawerManager;
import com.example.todolist.view.BottomNavigationManager;
import com.example.todolist.manager.AuthManager;
import com.example.todolist.manager.SyncManager;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Initialize managers
        // authManager = new AuthManager(this, new AuthManager.AuthListener() {
        //     @Override
        //     public void onAuthSuccess(FirebaseUser user) {
        //         loadUserData();
        //         loadStatistics();
        //     }

        //     @Override
        //     public void onAuthError(String error) {
        //         // Handle auth error
        //     }

        //     @Override
        //     public void onSignOut() {
        //         loadUserData();
        //         loadStatistics();
        //     }
        // });
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
        updateWeeklyChart();
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
        // FirebaseUser currentUser = authManager.getCurrentUser();
        // if (currentUser != null) {
        //     // User is signed in
        //     tvUserName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Firebase User");
        //     tvUserEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        //     // updateAuthButtonText("Đã đăng nhập");
        //     setUserAvatar(currentUser);
        // } else {
            // User not signed in
            tvUserName.setText("Local User");
            // tvUserEmail.setText("local@example.com");
            // updateAuthButtonText("Đăng nhập Google");
            imgUserAvatar.setImageResource(R.drawable.ic_person);
        // }
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
        // Get the start and end of the current week
        Calendar weekStart = (Calendar) currentWeekStart.clone();
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        
        // Array of chart bars (Sunday to Saturday)
        View[] chartBars = {
            chartBarSunday, chartBarMonday, chartBarTuesday, chartBarWednesday,
            chartBarThursday, chartBarFriday, chartBarSaturday
        };
        
        // Get completed tasks for each day of the week
        for (int i = 0; i < 7; i++) {
            Calendar dayCalendar = (Calendar) weekStart.clone();
            dayCalendar.add(Calendar.DAY_OF_YEAR, i);
            
            // Get completed tasks count for this day
            int completedCount = getCompletedTasksForDate(dayCalendar);
            
            // Update chart bar height (max height 80dp, scale based on max tasks in week)
            int maxHeight = 80; // dp
            int height = Math.min(completedCount * 20, maxHeight); // 20dp per task, max 80dp
            
            if (chartBars[i] != null) {
                android.view.ViewGroup.LayoutParams params = chartBars[i].getLayoutParams();
                params.height = (int) (height * getResources().getDisplayMetrics().density);
                chartBars[i].setLayoutParams(params);
                
                // Set different color if there are tasks
                if (completedCount > 0) {
                    chartBars[i].setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                } else {
                    chartBars[i].setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                }
            }
        }
    }
    
    private int getCompletedTasksForDate(Calendar date) {
        // Get completed tasks for specific date
        // This is a simplified version - you should implement based on your Task model
        // For now, return a sample value since we need to make async call
        return (int) (Math.random() * 5); // Random 0-4 tasks for demo
    }
    
    private boolean isSameDate(String dateStr, Calendar calendar) {
        if (dateStr == null || calendar == null) return false;
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String calendarDateStr = dateFormat.format(calendar.getTime());
        
        return dateStr.equals(calendarDateStr);
    }
}
