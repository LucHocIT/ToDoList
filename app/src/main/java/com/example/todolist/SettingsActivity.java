package com.example.todolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.todolist.manager.CategoryManager;
import com.example.todolist.manager.ThemeManager;
import com.example.todolist.util.SettingsManager;

public class SettingsActivity extends AppCompatActivity {
    
    private SharedPreferences sharedPreferences;
    private ThemeManager themeManager;
    
    // UI Components
    private ImageView btnBack;
    private Switch switchNotifications;
    private Switch switchSound;
    private Switch switchVibration;
    private LinearLayout layoutLanguage;
    private LinearLayout layoutAboutApp;
    private LinearLayout layoutPrivacyPolicy;
    private LinearLayout layoutTerms;
    private LinearLayout layoutHelpSupport;
    private LinearLayout layoutResetData;
    
    private TextView tvLanguageValue;
    private TextView tvAppVersion;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        initSharedPreferences();
        initThemeManager();
        initViews();
        setupClickListeners();
        loadCurrentSettings();
        
        // Apply current theme
        if (themeManager != null) {
            themeManager.applyCurrentTheme();
        }
    }
    
    private void initSharedPreferences() {
        sharedPreferences = getSharedPreferences("TodoListSettings", MODE_PRIVATE);
    }
    
    private void initThemeManager() {
        themeManager = new ThemeManager(this, null);
    }
    
    private void initViews() {
        // Header
        btnBack = findViewById(R.id.btn_back_settings);
        
        // Notification Settings
        switchNotifications = findViewById(R.id.switch_notifications);
        switchSound = findViewById(R.id.switch_sound);
        switchVibration = findViewById(R.id.switch_vibration);
        
        // General Settings
        layoutLanguage = findViewById(R.id.layout_language);
        tvLanguageValue = findViewById(R.id.tv_language_value);
        
        // About & Support
        layoutAboutApp = findViewById(R.id.layout_about_app);
        layoutPrivacyPolicy = findViewById(R.id.layout_privacy_policy);
        layoutTerms = findViewById(R.id.layout_terms);
        layoutHelpSupport = findViewById(R.id.layout_help_support);
        tvAppVersion = findViewById(R.id.tv_app_version);
        
        // Advanced Settings
        layoutResetData = findViewById(R.id.layout_reset_data);
    }
    
    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());
        
        // Notification switches
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.setNotificationsEnabled(this, isChecked);
            if (!isChecked) {
                // Disable sound and vibration when notifications are disabled
                switchSound.setChecked(false);
                switchVibration.setChecked(false);
                switchSound.setEnabled(false);
                switchVibration.setEnabled(false);
                // Lưu vào SettingsManager để đảm bảo tắt thật sự
                SettingsManager.setSoundEnabled(this, false);
                SettingsManager.setVibrationEnabled(this, false);
            } else {
                switchSound.setEnabled(true);
                switchVibration.setEnabled(true);
            }
        });
        
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.setSoundEnabled(this, isChecked);
        });
        
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.setVibrationEnabled(this, isChecked);
        });

        // Language setting
        layoutLanguage.setOnClickListener(v -> showLanguageDialog());
        
        // About & Support
        layoutAboutApp.setOnClickListener(v -> showAboutDialog());
        layoutPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicy());
        layoutTerms.setOnClickListener(v -> showTermsOfService());
        layoutHelpSupport.setOnClickListener(v -> showHelpSupport());
        
        // Advanced Settings
        layoutResetData.setOnClickListener(v -> showResetDataDialog());
    }
    
    private void loadCurrentSettings() {
        // Sửa logic cài đặt nếu có vấn đề
        SettingsManager.fixNotificationSettings(this);
        
        // Load notification settings using SettingsManager
        boolean notificationsEnabled = SettingsManager.isNotificationsEnabled(this);
        boolean soundEnabled = SettingsManager.isSoundEnabled(this);
        boolean vibrationEnabled = SettingsManager.isVibrationEnabled(this);
        
        switchNotifications.setChecked(notificationsEnabled);
        switchSound.setChecked(soundEnabled);
        switchVibration.setChecked(vibrationEnabled);
        
        // Enable/disable sound and vibration based on notifications setting
        switchSound.setEnabled(notificationsEnabled);
        switchVibration.setEnabled(notificationsEnabled);
        
        // Load language
        String language = SettingsManager.getLanguage(this);
        tvLanguageValue.setText(language);
        
        // Load app version
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion.setText("Phiên bản " + versionName);
        } catch (Exception e) {
            tvAppVersion.setText("Phiên bản 1.0");
        }
    }
    

    
    private void showLanguageDialog() {
        String[] languages = {"Tiếng Việt", "English", "中文", "한국어"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn ngôn ngữ");
        builder.setItems(languages, (dialog, which) -> {
            String selectedLanguage = languages[which];
            tvLanguageValue.setText(selectedLanguage);
            SettingsManager.setLanguage(this, selectedLanguage);
            Toast.makeText(this, "Ngôn ngữ sẽ được áp dụng khi khởi động lại ứng dụng", Toast.LENGTH_LONG).show();
        });
        builder.show();
    }
    
    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Về ứng dụng To-Do List");
        builder.setMessage("To-Do List - Ứng dụng quản lý công việc thông minh\n\n" +
                "Phiên bản: 1.0\n" +
                "Nhà phát triển: Team Development\n" +
                "Email: phamluc2304@gmail.com\n\n" +
                "Ứng dụng giúp bạn quản lý công việc hàng ngày một cách hiệu quả với giao diện đẹp mắt và nhiều tính năng hữu ích.");
        builder.setPositiveButton("Đóng", null);
        builder.show();
    }
    
    private void showPrivacyPolicy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chính sách bảo mật");
        builder.setMessage("Chúng tôi cam kết bảo vệ quyền riêng tư của bạn:\n\n" +
                "• Dữ liệu được lưu trữ cục bộ trên thiết bị\n" +
                "• Không thu thập thông tin cá nhân\n" +
                "• Không chia sẻ dữ liệu với bên thứ ba\n" +
                "• Chỉ yêu cầu quyền cần thiết cho hoạt động ứng dụng");
        builder.setPositiveButton("Đã hiểu", null);
        builder.show();
    }
    
    private void showTermsOfService() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Điều khoản sử dụng");
        builder.setMessage("Bằng việc sử dụng ứng dụng này, bạn đồng ý:\n\n" +
                "• Sử dụng ứng dụng cho mục đích cá nhân\n" +
                "• Không sử dụng ứng dụng cho mục đích bất hợp pháp\n" +
                "• Nhà phát triển không chịu trách nhiệm cho dữ liệu bị mất\n" +
                "• Bạn có trách nhiệm sao lưu dữ liệu của mình");
        builder.setPositiveButton("Đồng ý", null);
        builder.show();
    }
    
    private void showHelpSupport() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trợ giúp & Hỗ trợ");
        builder.setMessage("Cần trợ giúp? Liên hệ với chúng tôi:\n\n" +
                "📧 Email: phamluc2304@gmail.com\n" +
                "📱 Điện thoại: 0354337494\n\n" +
                "Hoặc bạn có thể:\n" +
                "• Xem hướng dẫn sử dụng trong ứng dụng\n" +
                "• Gửi phản hồi qua email\n" +
                "• Báo cáo lỗi để cải thiện ứng dụng");
        builder.setPositiveButton("Gửi email", (dialog, which) -> {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"phamluc2304@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ ứng dụng To-Do List");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Xin chào,\n\nTôi cần hỗ trợ về ứng dụng To-Do List:\n\n");
            
            try {
                startActivity(Intent.createChooser(emailIntent, "Gửi email"));
            } catch (Exception e) {
                Toast.makeText(this, "Không tìm thấy ứng dụng email", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Đóng", null);
        builder.show();
    }
    

    
    private void showResetDataDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Reset dữ liệu")
            .setMessage("Bạn có chắc chắn muốn xóa tất cả dữ liệu và khôi phục về mặc định?\n\nThao tác này không thể hoàn tác!")
            .setPositiveButton("Đồng ý", (dialog, which) -> {
                performDataReset();
            })
            .setNegativeButton("Hủy", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    private void performDataReset() {
        try {
            // Reset database using CategoryManager
            CategoryManager categoryManager = new CategoryManager(this, null, null);
            categoryManager.clearAllDataAndReset();
            
            // Reset app settings
            SettingsManager.resetAllSettings(this);
            
            Toast.makeText(this, "Đã reset tất cả dữ liệu thành công", Toast.LENGTH_LONG).show();
            
            // Restart app to reflect changes
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Có lỗi xảy ra khi reset dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


}
