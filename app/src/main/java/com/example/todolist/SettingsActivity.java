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
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.List;
import java.util.Locale;
public class SettingsActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_RINGTONE_PICKER = 1001;
    
    private SharedPreferences sharedPreferences;
    private ThemeManager themeManager;
    // UI Components
    private ImageView btnBack;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchVibration;
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
        // Back button with ripple animation
        btnBack.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100);
                finish();
            });
        });
        // Notification switches with improved feedback
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.setNotificationsEnabled(this, isChecked);
            // Automatically disable sound when notifications are disabled
            if (!isChecked) {
                SettingsManager.setSoundEnabled(this, false);
            }
            updateNotificationSubSettings(isChecked);
            showModernSnackbar(isChecked ? "✓ Đã bật thông báo" : "✗ Đã tắt thông báo");
        });
        
        // Ringtone setting with animation
        layoutRingtone.setOnClickListener(v -> {
            if (switchNotifications.isChecked()) {
                animateClickEffect(v);
                openRingtonePicker();
            } else {
                showModernSnackbar("⚠ Vui lòng bật thông báo trước");
            }
        });
        
        // Vibration switch with feedback
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (switchNotifications.isChecked()) {
                SettingsManager.setVibrationEnabled(this, isChecked);
                showModernSnackbar(isChecked ? "✓ Đã bật rung" : "✗ Đã tắt rung");
            }
        });
        
        layoutLanguage.setOnClickListener(v -> {
            animateClickEffect(v);
            showLanguageDialog();
        });
        layoutAboutApp.setOnClickListener(v -> {
            animateClickEffect(v);
            showAboutDialog();
        });
        layoutPrivacyPolicy.setOnClickListener(v -> {
            animateClickEffect(v);
            showPrivacyPolicy();
        });
        layoutTerms.setOnClickListener(v -> {
            animateClickEffect(v);
            showTermsOfService();
        });
        layoutHelpSupport.setOnClickListener(v -> {
            animateClickEffect(v);
            showHelpSupport();
        });
        layoutResetData.setOnClickListener(v -> {
            animateClickEffect(v);
            showResetDataDialog();
        });
    }
    
    // Modern Material Design 3 feedback
    private void showModernSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(getResources().getColor(R.color.colorPrimary));
        snackbar.setTextColor(Color.WHITE);
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.show();
    }
    
    // Subtle click animation for better UX
    private void animateClickEffect(View view) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(50)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(50);
            });
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_language_selection_enhanced, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Get current language
        String currentLang = SettingsManager.getLanguage(this);
        final String[] selectedLanguage = {currentLang};
        
        // Setup UI elements
        ImageView btnClose = dialogView.findViewById(R.id.btn_close_language);
        LinearLayout optionVi = dialogView.findViewById(R.id.language_option_vi);
        LinearLayout optionEn = dialogView.findViewById(R.id.language_option_en);
        ImageView radioVi = dialogView.findViewById(R.id.radio_vi);
        ImageView radioEn = dialogView.findViewById(R.id.radio_en);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel_language);
        TextView btnApply = dialogView.findViewById(R.id.btn_apply_language);
        
        // Set initial selection
        if (currentLang.equals("English")) {
            optionEn.setBackgroundResource(R.drawable.bg_language_item_selected);
            radioEn.setImageResource(R.drawable.ic_radio_button_checked);
        } else {
            optionVi.setBackgroundResource(R.drawable.bg_language_item_selected);
            radioVi.setImageResource(R.drawable.ic_radio_button_checked);
        }
        
        // Vietnamese option click
        optionVi.setOnClickListener(v -> {
            selectedLanguage[0] = "Tiếng Việt";
            optionVi.setBackgroundResource(R.drawable.bg_language_item_selected);
            optionEn.setBackgroundResource(R.drawable.bg_language_item);
            radioVi.setImageResource(R.drawable.ic_radio_button_checked);
            radioEn.setImageResource(R.drawable.ic_radio_button_unchecked);
        });
        
        // English option click
        optionEn.setOnClickListener(v -> {
            selectedLanguage[0] = "English";
            optionEn.setBackgroundResource(R.drawable.bg_language_item_selected);
            optionVi.setBackgroundResource(R.drawable.bg_language_item);
            radioEn.setImageResource(R.drawable.ic_radio_button_checked);
            radioVi.setImageResource(R.drawable.ic_radio_button_unchecked);
        });
        
        // Close button
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        // Cancel button
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        // Apply button
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                if (!selectedLanguage[0].equals(currentLang)) {
                    tvLanguageValue.setText(selectedLanguage[0]);
                    SettingsManager.setLanguage(this, selectedLanguage[0]);
                    applyLanguageChange(selectedLanguage[0]);
                    dialog.dismiss();
                    // Show restart confirmation dialog
                    showRestartDialog();
                } else {
                    dialog.dismiss();
                }
            });
        }
        
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_privacy_policy_enhanced, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Setup close button
        ImageView btnClose = dialogView.findViewById(R.id.btn_close_privacy);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        // Setup understand button
        TextView btnUnderstand = dialogView.findViewById(R.id.btn_understand_privacy);
        if (btnUnderstand != null) {
            btnUnderstand.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }
    private void showTermsOfService() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_terms_enhanced, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Setup close button
        ImageView btnClose = dialogView.findViewById(R.id.btn_close_terms);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        // Setup agree button
        TextView btnAgree = dialogView.findViewById(R.id.btn_agree_terms);
        if (btnAgree != null) {
            btnAgree.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }
    private void showHelpSupport() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_help_support_enhanced, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Setup close button
        ImageView btnClose = dialogView.findViewById(R.id.btn_close_help);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        // Setup email card click
        LinearLayout cardEmail = dialogView.findViewById(R.id.card_email);
        if (cardEmail != null) {
            cardEmail.setOnClickListener(v -> {
                dialog.dismiss();
                sendEmail();
            });
        }
        
        // Setup phone card click
        LinearLayout cardPhone = dialogView.findViewById(R.id.card_phone);
        if (cardPhone != null) {
            cardPhone.setOnClickListener(v -> {
                dialog.dismiss();
                callPhone();
            });
        }
        
        // Setup send email button
        TextView btnSendEmail = dialogView.findViewById(R.id.btn_send_email);
        if (btnSendEmail != null) {
            btnSendEmail.setOnClickListener(v -> {
                dialog.dismiss();
                sendEmail();
            });
        }
        
        dialog.show();
    }
    
    private void sendEmail() {
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
    }
    
    private void callPhone() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:0354337494"));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở ứng dụng điện thoại", Toast.LENGTH_SHORT).show();
        }
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
