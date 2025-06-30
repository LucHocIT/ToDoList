package com.example.todolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.todolist.database.TodoDatabase;
import com.example.todolist.manager.UserStatsManager;
import com.example.todolist.model.TodoTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    
    private TextView tvUserName, tvMemberSince, tvTotalTasks, tvCompletedTasks, tvPendingTasks;
    private TextView tvProductivityScore, tvLongestStreak, tvCurrentStreak;
    private CardView cardSettings, cardTheme, cardBackup, cardAbout, cardCompletedTasks;
    private ImageView btnBack;
    
    private TodoDatabase database;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        initViews();
        initDatabase();
        setupClickListeners();
        loadUserData();
        loadStatistics();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvUserName = findViewById(R.id.tv_user_name);
        tvMemberSince = findViewById(R.id.tv_member_since);
        tvTotalTasks = findViewById(R.id.tv_total_tasks);
        tvCompletedTasks = findViewById(R.id.tv_completed_tasks);
        tvPendingTasks = findViewById(R.id.tv_pending_tasks);
        tvProductivityScore = findViewById(R.id.tv_productivity_score);
        tvLongestStreak = findViewById(R.id.tv_longest_streak);
        tvCurrentStreak = findViewById(R.id.tv_current_streak);
        
        cardSettings = findViewById(R.id.card_settings);
        cardTheme = findViewById(R.id.card_theme);
        cardBackup = findViewById(R.id.card_backup);
        cardAbout = findViewById(R.id.card_about);
        cardCompletedTasks = findViewById(R.id.card_completed_tasks);
    }
    
    private void initDatabase() {
        database = TodoDatabase.getInstance(this);
        preferences = getSharedPreferences("TodoApp", MODE_PRIVATE);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        
        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        cardTheme.setOnClickListener(v -> {
            Intent intent = new Intent(this, ThemeSelectionActivity.class);
            startActivity(intent);
        });
        
        cardCompletedTasks.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompletedTasksActivity.class);
            startActivity(intent);
        });
        
        cardBackup.setOnClickListener(v -> {
            // Implement backup functionality
            Toast.makeText(this, "Tính năng sao lưu đang được phát triển", Toast.LENGTH_SHORT).show();
        });
        
        cardAbout.setOnClickListener(v -> {
            showAboutDialog();
        });
    }
    
    private void loadUserData() {
        // Load user name (default if not set)
        String userName = preferences.getString("user_name", "Người dùng");
        tvUserName.setText(userName);
        
        // Load member since date
        long installTime = preferences.getLong("install_time", System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
        String memberSince = "Thành viên từ " + dateFormat.format(new Date(installTime));
        tvMemberSince.setText(memberSince);
    }
    
    private void loadStatistics() {
        new Thread(() -> {
            try {
                // Get all tasks
                List<TodoTask> allTasks = database.todoDao().getAllTasks();
                List<TodoTask> completedTasks = database.todoDao().getCompletedTasks();
                
                int totalTasks = allTasks.size();
                int completed = completedTasks.size();
                int pending = totalTasks - completed;
                
                // Calculate productivity score (percentage of completed tasks)
                int productivityScore = totalTasks > 0 ? (completed * 100) / totalTasks : 0;
                
                // Calculate streaks (simplified version)
                int currentStreak = calculateCurrentStreak(completedTasks);
                int longestStreak = calculateLongestStreak(completedTasks);
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    tvTotalTasks.setText(String.valueOf(totalTasks));
                    tvCompletedTasks.setText(String.valueOf(completed));
                    tvPendingTasks.setText(String.valueOf(pending));
                    tvProductivityScore.setText(productivityScore + "%");
                    tvCurrentStreak.setText(currentStreak + " ngày");
                    tvLongestStreak.setText(longestStreak + " ngày");
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi khi tải thống kê", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private int calculateCurrentStreak(List<TodoTask> completedTasks) {
        // Simplified streak calculation
        // In a real app, you'd track daily completion dates
        return preferences.getInt("current_streak", 0);
    }
    
    private int calculateLongestStreak(List<TodoTask> completedTasks) {
        // Simplified streak calculation
        return preferences.getInt("longest_streak", 0);
    }
    
    private void showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Về ứng dụng")
                .setMessage("To-Do List App\nPhiên bản 1.0\n\nỨng dụng quản lý công việc đơn giản và hiệu quả.\n\n© 2025 To-Do List Team")
                .setPositiveButton("Đóng", null)
                .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics(); // Refresh statistics when returning to the activity
    }
}
