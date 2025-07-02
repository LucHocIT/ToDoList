package com.example.todolist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    
    private SharedPreferences sharedPreferences;
    private ThemeManager themeManager;
    
    // UI Components
    private ImageView btnBack;
    private Switch switchNotifications;
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
                // Disable vibration when notifications are disabled
                switchVibration.setChecked(false);
                switchVibration.setEnabled(false);
                // Lưu vào SettingsManager để đảm bảo tắt thật sự
                SettingsManager.setVibrationEnabled(this, false);
                // Automatically disable sound when notifications are disabled
                SettingsManager.setSoundEnabled(this, false);
            } else {
                switchVibration.setEnabled(true);
            }
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
        // Ensure sound is disabled since we removed the sound UI
        SettingsManager.ensureSoundDisabledWhenNotificationsOff(this);
        
        // Load notification settings using SettingsManager
        boolean notificationsEnabled = SettingsManager.isNotificationsEnabled(this);
        boolean vibrationEnabled = SettingsManager.isVibrationEnabled(this);
        
        switchNotifications.setChecked(notificationsEnabled);
        switchVibration.setChecked(vibrationEnabled);
        
        // Enable/disable vibration based on notifications setting
        switchVibration.setEnabled(notificationsEnabled);
        
        // Load language
        String language = SettingsManager.getLanguage(this);
        tvLanguageValue.setText(language);
        
        // Load app version
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion.setText(getString(R.string.version_format, versionName));
        } catch (Exception e) {
            tvAppVersion.setText(getString(R.string.version_default));
        }
    }
    

    
    private void showLanguageDialog() {
        String[] languages = {"Tiếng Việt", "English"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose_language));
        builder.setItems(languages, (dialog, which) -> {
            String selectedLanguage = languages[which];
            tvLanguageValue.setText(selectedLanguage);
            SettingsManager.setLanguage(this, selectedLanguage);
            
            // Apply language change immediately
            applyLanguageChange(selectedLanguage);
            
            Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }
    
    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.about_app_title));
        builder.setMessage(getString(R.string.about_app_message));
        builder.setPositiveButton(getString(R.string.close), null);
        builder.show();
    }
    
    private void showPrivacyPolicy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.privacy_policy_title));
        builder.setMessage(getString(R.string.privacy_policy_message));
        builder.setPositiveButton(getString(R.string.understood), null);
        builder.show();
    }
    
    private void showTermsOfService() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.terms_title));
        builder.setMessage(getString(R.string.terms_message));
        builder.setPositiveButton(getString(R.string.agree), null);
        builder.show();
    }
    
    private void showHelpSupport() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.help_support_title));
        builder.setMessage(getString(R.string.help_support_message));
        builder.setPositiveButton(getString(R.string.send_email), (dialog, which) -> {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"phamluc2304@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
            emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body));
            
            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.choose_email_app)));
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.no_email_app), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.close), null);
        builder.show();
    }
    

    
    private void showResetDataDialog() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.reset_data_title))
            .setMessage(getString(R.string.reset_data_message))
            .setPositiveButton(getString(R.string.agree), (dialog, which) -> {
                performDataReset();
            })
            .setNegativeButton(getString(R.string.cancel), null)
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
            
            Toast.makeText(this, getString(R.string.reset_success), Toast.LENGTH_LONG).show();
            
            // Restart app to reflect changes
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.reset_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    
    private void applyLanguageChange(String languageName) {
        String languageCode;
        if (languageName.equals("English")) {
            languageCode = "en";
        } else {
            languageCode = "vi";
        }
        
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        
        // Recreate activity to apply changes immediately
        recreate();
    }


}
