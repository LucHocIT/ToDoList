package com.example.todolist.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.todolist.R;
import com.example.todolist.manager.AuthManager;
import com.example.todolist.manager.FirebaseSyncManager;
import com.example.todolist.manager.SyncManager;
import com.example.todolist.service.TaskService;

public class SyncAccountActivity extends AppCompatActivity {
    
    private ImageView backButton;
    private ImageView helpButton;
    private ImageView menuButton;
    private RelativeLayout driveLayout;
    private TextView loginStatusText;
    private LinearLayout loggedInContent;
    private TextView lastSyncTime;
    private TextView syncStatus;
    private TextView syncTimeDisplay; // For the main sync card
    private SwitchCompat autoSyncSwitch;
    
    private AuthManager authManager;
    private SyncManager syncManager;
    private FirebaseSyncManager firebaseSyncManager;
    private TaskService taskService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_account);

        // Initialize AuthManager
        authManager = AuthManager.getInstance();
        authManager.initialize(this);
        
        // Initialize SyncManager (old one for backward compatibility)
        syncManager = SyncManager.getInstance();
        syncManager.initialize(this);
        
        // Initialize Firebase Sync Manager
        firebaseSyncManager = FirebaseSyncManager.getInstance();
        firebaseSyncManager.initialize(this);
        
        // Initialize TaskService for sync operations
        taskService = new TaskService(this, null);
        
        initializeViews();
        setupClickListeners();
        updateUI();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        helpButton = findViewById(R.id.helpButton);
        menuButton = findViewById(R.id.menuButton);
        driveLayout = findViewById(R.id.driveLayout);
        loginStatusText = findViewById(R.id.loginStatusText);
        loggedInContent = findViewById(R.id.loggedInContent);
        lastSyncTime = findViewById(R.id.lastSyncTime);
        syncStatus = findViewById(R.id.syncStatus);
        syncTimeDisplay = findViewById(R.id.syncTimeDisplay);
        autoSyncSwitch = findViewById(R.id.autoSyncSwitch);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        helpButton.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng trợ giúp sẽ được thêm sau", Toast.LENGTH_SHORT).show();
        });
        
        menuButton.setOnClickListener(v -> showMenu());
        
        driveLayout.setOnClickListener(v -> {
            if (!authManager.isSignedIn()) {
                // Thực hiện đăng nhập
                authManager.signIn(this, new AuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(String email) {
                        runOnUiThread(() -> {
                            updateUI();
                            Toast.makeText(SyncAccountActivity.this, "Đăng nhập thành công: " + email + ". Bạn có thể bật đồng bộ trong cài đặt.", Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(SyncAccountActivity.this, "Lỗi đăng nhập: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
        
        autoSyncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Lưu trạng thái sync vào AuthManager (thay vì SyncManager cũ)
            authManager.setSyncEnabled(isChecked);
            
            if (isChecked && authManager.isSignedIn()) {
                // Khi bật sync và đã login, đồng bộ tất cả tasks lên Firebase
                Toast.makeText(this, "Đang đồng bộ tasks lên Firebase...", Toast.LENGTH_SHORT).show();
                taskService.syncAllTasksToFirebase(new FirebaseSyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(SyncAccountActivity.this, "Đồng bộ thành công: " + message, Toast.LENGTH_SHORT).show();
                            updateUI();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(SyncAccountActivity.this, "Lỗi đồng bộ: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            
            Toast.makeText(this, "Tự động đồng bộ: " + (isChecked ? "Bật" : "Tắt"), Toast.LENGTH_SHORT).show();
        });
    }

    private void showMenu() {
        PopupMenu popup = new PopupMenu(this, menuButton);
        popup.getMenuInflater().inflate(R.menu.sync_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_sign_out) {
                logout();
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void logout() {
        authManager.signOut(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    updateUI();
                    Toast.makeText(SyncAccountActivity.this, "Đã đăng xuất và tắt đồng bộ", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SyncAccountActivity.this, "Lỗi đăng xuất: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateUI() {
        // Cập nhật thời gian đồng bộ cuối từ SyncManager
        String lastSync = syncManager.getLastSyncTime();
        if (syncTimeDisplay != null) {
            syncTimeDisplay.setText("Thời gian đồng bộ hóa lần cuối: " + lastSync);
        }
        
        // Cập nhật trạng thái sync switch từ AuthManager
        if (autoSyncSwitch != null) {
            autoSyncSwitch.setChecked(authManager.isSyncEnabled());
        }
        
        if (authManager.isSignedIn()) {
            // Hiển thị trạng thái đã đăng nhập
            String email = authManager.getCurrentUserEmail();
            loginStatusText.setText(email != null ? email : "Đã đăng nhập");
            
            // Hiển thị phần bổ sung khi đã đăng nhập
            loggedInContent.setVisibility(View.VISIBLE);
            
            // Cập nhật thông tin đồng bộ
            if (lastSyncTime != null) {
                lastSyncTime.setText(lastSync);
            }
            if (syncStatus != null) {
                if (authManager.isSyncEnabled()) {
                    syncStatus.setText("Đã bật đồng bộ");
                    syncStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    syncStatus.setText("Chưa bật đồng bộ");
                    syncStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                }
            }
        } else {
            // Hiển thị trạng thái chưa đăng nhập
            loginStatusText.setText("Nhấn để đăng nhập");
            
            // Ẩn phần bổ sung khi chưa đăng nhập
            loggedInContent.setVisibility(View.GONE);
            
            // Tắt sync khi chưa đăng nhập
            if (syncStatus != null) {
                syncStatus.setText("Chưa đăng nhập");
                syncStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
        }
        
        // Switch luôn hoạt động bất kể trạng thái đăng nhập
        // Các card chính (Đồng bộ hóa bản ghi và Tự động đồng bộ hóa) luôn hiển thị
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Handle Google Sign-In result
        if (requestCode == 9001) { // RC_SIGN_IN constant from AuthManager
            authManager.handleSignInResult(data, new AuthManager.AuthCallback() {
                @Override
                public void onSuccess(String email) {
                    runOnUiThread(() -> {
                        updateUI();
                        Toast.makeText(SyncAccountActivity.this, "Đăng nhập thành công: " + email + ". Bạn có thể bật đồng bộ.", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(SyncAccountActivity.this, "Lỗi đăng nhập: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}
