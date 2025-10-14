package com.example.todolist;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.todolist.model.Category;
import com.example.todolist.service.CategoryService;
import com.example.todolist.manager.ThemeManager;
import com.example.todolist.util.SettingsManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;
import java.util.Locale;
public class SettingsActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_RINGTONE_PICKER = 1001;
    
    private SharedPreferences sharedPreferences;
    private ThemeManager themeManager;
    // UI Components
    private ImageView btnBack;
    private Switch switchNotifications;
    private Switch switchVibration;
    private LinearLayout layoutRingtone;
    private TextView tvRingtoneValue;
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
        layoutRingtone = findViewById(R.id.layout_ringtone);
        tvRingtoneValue = findViewById(R.id.tv_ringtone_value);
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
            // Automatically disable sound when notifications are disabled
            if (!isChecked) {
                SettingsManager.setSoundEnabled(this, false);
            }
            updateNotificationSubSettings(isChecked);
        });
        
        // Ringtone setting
        layoutRingtone.setOnClickListener(v -> {
            if (switchNotifications.isChecked()) {
                openRingtonePicker();
            } else {
                showFeedback("Vui lòng bật thông báo trước");
            }
        });
        
        // Vibration switch
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (switchNotifications.isChecked()) {
                SettingsManager.setVibrationEnabled(this, isChecked);
                showFeedback(isChecked ? "Đã bật rung" : "Đã tắt rung");
            }
        });
        
        layoutLanguage.setOnClickListener(v -> showLanguageDialog());
        layoutAboutApp.setOnClickListener(v -> showAboutDialog());
        layoutPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicy());
        layoutTerms.setOnClickListener(v -> showTermsOfService());
        layoutHelpSupport.setOnClickListener(v -> showHelpSupport());
        layoutResetData.setOnClickListener(v -> showResetDataDialog());
    }
    
    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void loadCurrentSettings() {
        SettingsManager.fixNotificationSettings(this);
        SettingsManager.ensureSoundDisabledWhenNotificationsOff(this);
        boolean notificationsEnabled = SettingsManager.isNotificationsEnabled(this);
        switchNotifications.setChecked(notificationsEnabled);
        
        // Load ringtone setting
        String ringtoneName = SettingsManager.getRingtoneName(this);
        tvRingtoneValue.setText(ringtoneName);
        
        // Load vibration setting
        boolean vibrationEnabled = SettingsManager.isVibrationEnabled(this);
        switchVibration.setChecked(vibrationEnabled);
        
        // Update sub-settings visibility
        updateNotificationSubSettings(notificationsEnabled);
        
        String language = SettingsManager.getLanguage(this);
        tvLanguageValue.setText(language);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion.setText(getString(R.string.version_format, versionName));
        } catch (Exception e) {
            tvAppVersion.setText(getString(R.string.version_default));
        }
    }
    private void showLanguageDialog() {
        String[] languages = {"Tiếng Việt", "English"};
        String currentLang = SettingsManager.getLanguage(this);
        int checkedItem = currentLang.equals("English") ? 1 : 0;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose_language));
        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            String selectedLanguage = languages[which];
            if (!selectedLanguage.equals(currentLang)) {
                tvLanguageValue.setText(selectedLanguage);
                SettingsManager.setLanguage(this, selectedLanguage);
                applyLanguageChange(selectedLanguage);
                
                // Show restart confirmation dialog
                showRestartDialog();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showRestartDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Khởi động lại ứng dụng")
            .setMessage("Ứng dụng cần khởi động lại để áp dụng ngôn ngữ mới. Bạn có muốn khởi động lại ngay bây giờ?")
            .setPositiveButton("Khởi động lại", (dialog, which) -> {
                restartApp();
            })
            .setNegativeButton("Để sau", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .show();
    }
    
    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        }
    }
    private void showAboutDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_about_enhanced, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Setup close button
        ImageView btnClose = dialogView.findViewById(R.id.btn_close_about);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        // Setup version text
        TextView tvVersion = dialogView.findViewById(R.id.tv_about_version);
        if (tvVersion != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                tvVersion.setText("Version " + versionName);
            } catch (Exception e) {
                tvVersion.setText("Version 2.0");
            }
        }
        
        dialog.show();
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reset_confirm, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Setup buttons
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel_reset);
        TextView btnConfirm = dialogView.findViewById(R.id.btn_confirm_reset);
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                performDataReset();
            });
        }
        
        dialog.show();
    }
    private void performDataReset() {
        try {
            CategoryService categoryService = new CategoryService(this, new CategoryService.CategoryUpdateListener() {
                @Override
                public void onCategoriesUpdated(List<Category> categories) {
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this, "Lỗi reset: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
            categoryService.clearAllCategories(new CategoryService.CategoryOperationCallback() {
                @Override
                public void onSuccess() {
                    categoryService.initializeDefaultCategories(new CategoryService.CategoryOperationCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                SettingsManager.resetAllSettings(SettingsActivity.this);
                                Toast.makeText(SettingsActivity.this, getString(R.string.reset_success), Toast.LENGTH_LONG).show();
                                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                                if (intent != null) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finishAffinity();
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(SettingsActivity.this, "Lỗi khởi tạo lại: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this, "Lỗi xóa dữ liệu: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
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
    }

    private void updateNotificationSubSettings(boolean enabled) {
        // Enable/disable ringtone and vibration settings based on notification state
        layoutRingtone.setEnabled(enabled);
        layoutRingtone.setAlpha(enabled ? 1.0f : 0.5f);
        switchVibration.setEnabled(enabled);
        
        if (!enabled) {
            // Optionally disable vibration when notifications are off
            switchVibration.setChecked(false);
            SettingsManager.setVibrationEnabled(this, false);
        }
    }

    private void openRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_ringtone));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getRingtoneUri());
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE_PICKER);
    }

    private Uri getRingtoneUri() {
        String uriString = SettingsManager.getRingtoneUri(this);
        if (uriString != null && !uriString.isEmpty()) {
            return Uri.parse(uriString);
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RINGTONE_PICKER && resultCode == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                // Save the ringtone URI
                SettingsManager.setRingtoneUri(this, uri.toString());
                // Get and save the ringtone name
                android.media.Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
                String ringtoneName = ringtone.getTitle(this);
                SettingsManager.setRingtoneName(this, ringtoneName);
                tvRingtoneValue.setText(ringtoneName);
            } else {
                // Silent was selected
                SettingsManager.setRingtoneUri(this, "");
                SettingsManager.setRingtoneName(this, getString(R.string.silent));
                tvRingtoneValue.setText(getString(R.string.silent));
            }
        }
    }
}
