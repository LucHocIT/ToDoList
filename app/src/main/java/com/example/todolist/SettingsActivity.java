package com.example.todolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
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
    private Switch switchDarkMode;
    private LinearLayout layoutNotificationTime;
    private LinearLayout layoutRingtone;
    private LinearLayout layoutLanguage;
    private LinearLayout layoutDataBackup;
    private LinearLayout layoutDataRestore;
    private LinearLayout layoutAboutApp;
    private LinearLayout layoutPrivacyPolicy;
    private LinearLayout layoutTerms;
    private LinearLayout layoutHelpSupport;
    
    private TextView tvNotificationTimeValue;
    private TextView tvRingtoneValue;
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
        layoutNotificationTime = findViewById(R.id.layout_notification_time);
        tvNotificationTimeValue = findViewById(R.id.tv_notification_time_value);
        layoutRingtone = findViewById(R.id.layout_ringtone);
        tvRingtoneValue = findViewById(R.id.tv_ringtone_value);
        
        // Appearance Settings
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        
        // General Settings
        layoutLanguage = findViewById(R.id.layout_language);
        tvLanguageValue = findViewById(R.id.tv_language_value);
        
        // Data Management
        layoutDataBackup = findViewById(R.id.layout_data_backup);
        layoutDataRestore = findViewById(R.id.layout_data_restore);
        
        // About & Support
        layoutAboutApp = findViewById(R.id.layout_about_app);
        layoutPrivacyPolicy = findViewById(R.id.layout_privacy_policy);
        layoutTerms = findViewById(R.id.layout_terms);
        layoutHelpSupport = findViewById(R.id.layout_help_support);
        tvAppVersion = findViewById(R.id.tv_app_version);
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
        
        // Dark mode
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.setDarkModeEnabled(this, isChecked);
            applyDarkMode(isChecked);
        });
        
        // Notification time setting
        layoutNotificationTime.setOnClickListener(v -> showNotificationTimeDialog());
        
        // Ringtone setting
        layoutRingtone.setOnClickListener(v -> showRingtoneSelector());
        
        // Language setting
        layoutLanguage.setOnClickListener(v -> showLanguageDialog());
        
        // Data management
        layoutDataBackup.setOnClickListener(v -> showDataBackupDialog());
        layoutDataRestore.setOnClickListener(v -> showDataRestoreDialog());
        
        // About & Support
        layoutAboutApp.setOnClickListener(v -> showAboutDialog());
        layoutPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicy());
        layoutTerms.setOnClickListener(v -> showTermsOfService());
        layoutHelpSupport.setOnClickListener(v -> showHelpSupport());
    }
    
    private void loadCurrentSettings() {
        // Load notification settings using SettingsManager
        boolean notificationsEnabled = SettingsManager.isNotificationsEnabled(this);
        boolean soundEnabled = SettingsManager.isSoundEnabled(this);
        boolean vibrationEnabled = SettingsManager.isVibrationEnabled(this);
        boolean darkModeEnabled = SettingsManager.isDarkModeEnabled(this);
        
        switchNotifications.setChecked(notificationsEnabled);
        switchSound.setChecked(soundEnabled);
        switchVibration.setChecked(vibrationEnabled);
        switchDarkMode.setChecked(darkModeEnabled);
        
        // Enable/disable sound and vibration based on notifications setting
        switchSound.setEnabled(notificationsEnabled);
        switchVibration.setEnabled(notificationsEnabled);
        
        // Load notification time
        String notificationTime = SettingsManager.getNotificationTime(this);
        tvNotificationTimeValue.setText(notificationTime);
        
        // Load ringtone
        String ringtoneName = SettingsManager.getRingtoneName(this);
        tvRingtoneValue.setText(ringtoneName);
        
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
    
    private void applyDarkMode(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    private void showNotificationTimeDialog() {
        String[] timeOptions = {
            "5 phút trước",
            "10 phút trước", 
            "15 phút trước",
            "30 phút trước",
            "1 giờ trước",
            "2 giờ trước",
            "1 ngày trước"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thời gian nhắc nhở");
        builder.setItems(timeOptions, (dialog, which) -> {
            String selectedTime = timeOptions[which];
            tvNotificationTimeValue.setText(selectedTime);
            SettingsManager.setNotificationTime(this, selectedTime);
            Toast.makeText(this, "Đã cập nhật thời gian nhắc nhở", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }
    
    private void showRingtoneSelector() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn âm thanh thông báo");
        
        String currentRingtone = SettingsManager.getRingtoneUri(this);
        if (currentRingtone != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentRingtone));
        }
        
        startActivityForResult(intent, 1001);
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
    
    private void showDataBackupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sao lưu dữ liệu");
        builder.setMessage("Tính năng này sẽ sao lưu tất cả nhiệm vụ và danh mục của bạn ra file. Bạn có muốn tiếp tục?");
        builder.setPositiveButton("Sao lưu", (dialog, which) -> {
            // TODO: Implement backup functionality
            Toast.makeText(this, "Tính năng sao lưu đang được phát triển", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    
    private void showDataRestoreDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Khôi phục dữ liệu");
        builder.setMessage("Tính năng này sẽ khôi phục dữ liệu từ file sao lưu. Dữ liệu hiện tại sẽ bị ghi đè. Bạn có chắc chắn?");
        builder.setPositiveButton("Khôi phục", (dialog, which) -> {
            // TODO: Implement restore functionality
            Toast.makeText(this, "Tính năng khôi phục đang được phát triển", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", null);
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (ringtoneUri != null) {
                String ringtoneName = RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this);
                tvRingtoneValue.setText(ringtoneName);
                SettingsManager.setRingtoneUri(this, ringtoneUri.toString());
                SettingsManager.setRingtoneName(this, ringtoneName);
                Toast.makeText(this, "Đã cập nhật âm thanh thông báo", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
