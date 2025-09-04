package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
                showSyncProgressDialog();
            } else if (isChecked && !authManager.isSignedIn()) {
                Toast.makeText(this, "Bạn cần đăng nhập trước khi bật đồng bộ hóa", Toast.LENGTH_SHORT).show();
                autoSyncSwitch.setChecked(false);
                authManager.setSyncEnabled(false);
            }
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
        if (authManager.isSignedIn()) {
            // Hiển thị trạng thái đã đăng nhập
            String email = authManager.getCurrentUserEmail();
            loginStatusText.setText(email != null ? email : "Đã đăng nhập");
            
            // Hiển thị dấu 3 chấm khi đã đăng nhập
            menuButton.setVisibility(View.VISIBLE);
            
        } else {
            // Hiển thị trạng thái chưa đăng nhập
            loginStatusText.setText("Nhấn để đăng nhập");
            
            // Ẩn dấu 3 chấm khi chưa đăng nhập
            menuButton.setVisibility(View.GONE);
        }
        
        // Cập nhật trạng thái sync switch từ AuthManager
        if (autoSyncSwitch != null) {
            autoSyncSwitch.setChecked(authManager.isSyncEnabled());
        }
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
    
    private void showSyncProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sync_progress, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        AlertDialog progressDialog = builder.create();
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView titleText = dialogView.findViewById(R.id.sync_title);
        TextView percentText = dialogView.findViewById(R.id.sync_percent);
        ProgressBar progressSpinner = dialogView.findViewById(R.id.sync_progress_spinner);
        TextView statusText = dialogView.findViewById(R.id.sync_status);
        
        titleText.setText("Đồng bộ hóa bản ghi");
        percentText.setText("3%");
        statusText.setText("Đang kết nối với các dịch vụ của Google...");
        
        progressDialog.show();
        taskService.syncAllTasksToFirebase(new FirebaseSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    // Update to completion and hide spinner
                    percentText.setText("100%");
                    progressSpinner.setVisibility(View.GONE);
                    statusText.setText("Đồng bộ thành công!");
                    
                    // Delay to show completion, then dismiss and refresh main screen
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        progressDialog.dismiss();
                        updateUI();
                        
                        // Send broadcast to refresh main activity tasks
                        Intent intent = new Intent("com.example.todolist.REFRESH_TASKS");
                        sendBroadcast(intent);
                        
                        // Also finish this activity to return to main screen
                        finish();
                    }, 1000);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    statusText.setText("Lỗi đồng bộ: " + error);
                    percentText.setText("Error");
                    progressSpinner.setVisibility(View.GONE);
                    
                    // Auto dismiss after showing error
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {                    
                        progressDialog.dismiss();
                        // Reset sync switch if failed
                        autoSyncSwitch.setChecked(false);
                        authManager.setSyncEnabled(false);
                    }, 2000);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}
