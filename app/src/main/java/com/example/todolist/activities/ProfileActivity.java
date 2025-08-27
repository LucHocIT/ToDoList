package com.example.todolist.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.todolist.CalendarActivity;
import com.example.todolist.CompletedTasksActivity;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.SettingsActivity;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.model.Task;
import com.example.todolist.manager.NavigationDrawerManager;
import com.example.todolist.BottomNavigationManager;
import com.example.todolist.auth.AuthManager;
import com.example.todolist.auth.SyncManager;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ProfileActivity extends AppCompatActivity implements AuthManager.AuthListener, SyncManager.SyncListener {
    
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
        authManager = new AuthManager(this, this);
        syncManager = new SyncManager(this);
        
        initViews();
        setupBottomNavigation();
        setupDrawer();
        setupClickListeners();
        loadUserData();
        loadStatistics();
    }
    
    private void initViews() {
        // Bottom navigation buttons
        btnNavMenu = findViewById(R.id.btn_nav_menu);
        btnNavTasks = findViewById(R.id.btn_nav_tasks);
        btnNavCalendar = findViewById(R.id.btn_nav_calendar);
        btnNavProfile = findViewById(R.id.btn_nav_profile);
        
        // Profile elements
        imgUserAvatar = findViewById(R.id.img_user_avatar);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvTotalTasks = findViewById(R.id.tv_total_tasks);
        tvCompletedTasks = findViewById(R.id.tv_completed_tasks);
        
        // Action buttons
        btnAuthGoogle = findViewById(R.id.btn_auth_google);
        btnSyncSettings = findViewById(R.id.btn_sync_settings);
        btnAboutApp = findViewById(R.id.btn_about_app);
        
        // Initialize repository
        taskRepository = new TaskRepository();
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
        // Google Authentication Button
        btnAuthGoogle.setOnClickListener(v -> {
            if (authManager.isSignedIn()) {
                // User is signed in, show sign out option
                showSignOutDialog();
            } else {
                // User is not signed in, start sign in
                authManager.signInWithGoogle(this);
            }
        });
        
        // Sync Settings Button
        btnSyncSettings.setOnClickListener(v -> {
            if (authManager.isSignedIn()) {
                showSyncDialog();
            } else {
                // Show message to sign in first
                showToast("Vui lòng đăng nhập trước khi đồng bộ");
            }
        });
        
        // About App Button
        btnAboutApp.setOnClickListener(v -> {
            showAboutDialog();
        });
    }
    
    private void loadUserData() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            // User is signed in
            tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            tvUserEmail.setText(user.getEmail());
            
            // Load user avatar if available - for now use default
            // TODO: Implement image loading with Glide later
            imgUserAvatar.setImageResource(R.drawable.ic_person);
            
            // Update button text to show "Sign Out"
            updateAuthButtonText("Đăng xuất");
        } else {
            // User is not signed in
            tvUserName.setText("Chưa đăng nhập");
            tvUserEmail.setText("Đăng nhập để đồng bộ dữ liệu");
            imgUserAvatar.setImageResource(R.drawable.ic_person);
            
            // Update button text to show "Sign In"
            updateAuthButtonText("Đăng nhập với Google");
        }
    }
    
    private void updateAuthButtonText(String text) {
        // Find TextView inside btnAuthGoogle LinearLayout
        if (btnAuthGoogle != null && btnAuthGoogle.getChildCount() > 1) {
            android.view.View secondChild = btnAuthGoogle.getChildAt(1);
            if (secondChild instanceof TextView) {
                ((TextView) secondChild).setText(text);
            }
        }
    }
    
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
        
        if (requestCode == AuthManager.getSignInRequestCode()) {
            authManager.handleSignInResult(data);
        }
    }
    
    // AuthManager.AuthListener implementations
    @Override
    public void onAuthSuccess(FirebaseUser user) {
        runOnUiThread(() -> {
            loadUserData();
            loadStatistics();
            showToast("Đăng nhập thành công!");
        });
    }
    
    @Override
    public void onAuthError(String error) {
        runOnUiThread(() -> {
            showToast("Lỗi đăng nhập: " + error);
        });
    }
    
    @Override
    public void onSignOut() {
        runOnUiThread(() -> {
            loadUserData();
            loadStatistics();
            showToast("Đã đăng xuất");
        });
    }
    
    // SyncManager.SyncListener implementations
    @Override
    public void onSyncStart() {
        runOnUiThread(() -> {
            showToast("Bắt đầu đồng bộ...");
        });
    }
    
    @Override
    public void onSyncProgress(int progress, String status) {
        runOnUiThread(() -> {
            // Update progress in UI if needed
            // For now, just log
        });
    }
    
    @Override
    public void onSyncComplete(boolean success, String message) {
        runOnUiThread(() -> {
            showToast(message);
            if (success) {
                loadStatistics(); // Refresh statistics after sync
            }
        });
    }
    
    @Override
    public void onSyncError(String error) {
        runOnUiThread(() -> {
            showToast("Lỗi đồng bộ: " + error);
        });
    }
    
    // Helper methods
    private void showSignOutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    authManager.signOut();
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
                            syncManager.syncLocalToFirebase();
                            break;
                        case 1:
                            syncManager.syncFirebaseToLocal();
                            break;
                        case 2:
                            syncManager.syncBidirectional();
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
