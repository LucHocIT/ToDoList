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
import com.example.todolist.service.CategoryService;
import com.example.todolist.manager.ThemeManager;
import com.example.todolist.util.SettingsManager;
import java.util.Locale;
public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private ThemeManager themeManager;
    // UI Components
    private ImageView btnBack;
    private Switch switchNotifications;
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
        });
        layoutLanguage.setOnClickListener(v -> showLanguageDialog());
        layoutAboutApp.setOnClickListener(v -> showAboutDialog());
        layoutPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicy());
        layoutTerms.setOnClickListener(v -> showTermsOfService());
        layoutHelpSupport.setOnClickListener(v -> showHelpSupport());
        layoutResetData.setOnClickListener(v -> showResetDataDialog());
    }
    private void loadCurrentSettings() {
        SettingsManager.fixNotificationSettings(this);
        SettingsManager.ensureSoundDisabledWhenNotificationsOff(this);
        boolean notificationsEnabled = SettingsManager.isNotificationsEnabled(this);
        switchNotifications.setChecked(notificationsEnabled);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose_language));
        builder.setItems(languages, (dialog, which) -> {
            String selectedLanguage = languages[which];
            tvLanguageValue.setText(selectedLanguage);
            SettingsManager.setLanguage(this, selectedLanguage);
            applyLanguageChange(selectedLanguage);
            Toast.makeText(this, getString(R.string.language_changed_restart_prompt), Toast.LENGTH_LONG).show();
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
            CategoryService categoryService = new CategoryService(this, new CategoryService.CategoryUpdateListener() {
                @Override
                public void onCategoriesUpdated() {
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this, "Lỗi reset: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
            categoryService.clearAllDataAndReset();
            SettingsManager.resetAllSettings(this);
            Toast.makeText(this, getString(R.string.reset_success), Toast.LENGTH_LONG).show();
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
    }
}
