package com.example.todolist.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.todolist.CalendarActivity;
import com.example.todolist.CompletedTasksActivity;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.SettingsActivity;
import com.example.todolist.activities.SyncAccountActivity;
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

public class ProfileActivity extends AppCompatActivity {
    
    private LinearLayout btnNavMenu, btnNavTasks, btnNavCalendar, btnNavProfile;
    private LinearLayout btnAuthGoogle, btnSyncSettings, btnAboutApp;
    private TextView tvUserName, tvUserEmail, tvTotalTasks, tvCompletedTasks;
    private ImageView imgUserAvatar;
    private NavigationDrawerManager drawerManager;
    private TaskRepository taskRepository;
    private AuthManager authManager;
    private SyncManager syncManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Initialize managers
        // authManager = new AuthManager(this, new AuthManager.AuthListener() {
        //     @Override
        //     public void onAuthSuccess(FirebaseUser user) {
        //         loadUserData();
        //         // loadStatistics();
        //     }

        //     @Override
        //     public void onAuthError(String error) {
        //         // Handle auth error
        //     }

        //     @Override
        //     public void onSignOut() {
        //         loadUserData();
        //         // loadStatistics();
        //     }
        // });
        // syncManager = new SyncManager(this);
        
        initViews();
        setupBottomNavigation();
        setupDrawer();
        setupClickListeners();
        loadUserData();
        // loadStatistics();
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
        // tvUserEmail = findViewById(R.id.tv_user_email);
        // tvTotalTasks = findViewById(R.id.tv_total_tasks);
        // tvCompletedTasks = findViewById(R.id.tv_completed_tasks);
        
        // Action buttons
        // btnAuthGoogle = findViewById(R.id.btn_auth_google);
        // btnSyncSettings = findViewById(R.id.btn_sync_settings);
        // btnAboutApp = findViewById(R.id.btn_about_app);
        
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
        
        // Google Authentication Button
        /*
        btnAuthGoogle.setOnClickListener(v -> {
            Intent intent = new Intent(this, SyncAccountActivity.class);
            startActivity(intent);
        });
        
        // Sync Settings Button
        btnSyncSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SyncAccountActivity.class);
            startActivity(intent);
        });
        
        // About App Button
        btnAboutApp.setOnClickListener(v -> {
            showAboutDialog();
        });
        */
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
                int totalTasks = tasks.size();
                int completedTasks = 0;
                
                for (Task task : tasks) {
                    if (task.isCompleted()) {
                        completedTasks++;
                    }
                }
                
                final int finalCompletedTasks = completedTasks;
                runOnUiThread(() -> {
                    tvTotalTasks.setText(String.valueOf(totalTasks));
                    tvCompletedTasks.setText(String.valueOf(finalCompletedTasks));
                });
            }

            @Override
            public void onError(String error) {
                // If there's an error, show default values
                runOnUiThread(() -> {
                    tvTotalTasks.setText("0");
                    tvCompletedTasks.setText("0");
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
}
