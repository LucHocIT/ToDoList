package com.example.todolist.helper.taskdetail;

import android.content.Context;
import android.content.res.Configuration;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.todolist.model.Task;
import com.example.todolist.util.SettingsManager;

import java.util.Locale;

public class UIHelper {
    private AppCompatActivity activity;
    private LinearLayout layoutDatePicker;
    private Spinner spinnerCategory;

    public UIHelper(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void initViews(LinearLayout layoutDatePicker, Spinner spinnerCategory) {
        this.layoutDatePicker = layoutDatePicker;
        this.spinnerCategory = spinnerCategory;
    }

    public void updateCompletionStatus(Task currentTask) {
        if (currentTask != null) {
            if (currentTask.isCompleted()) {
                // Làm mờ giao diện khi task đã hoàn thành
                layoutDatePicker.setEnabled(false);
                layoutDatePicker.setAlpha(0.6f);
                spinnerCategory.setEnabled(false);
                spinnerCategory.setAlpha(0.6f);
            } else {
                // Khôi phục giao diện bình thường
                layoutDatePicker.setEnabled(true);
                layoutDatePicker.setAlpha(1.0f);
                spinnerCategory.setEnabled(true);
                spinnerCategory.setAlpha(1.0f);
            }
        }
    }

    public Context updateBaseContextLocale(Context context) {
        String languageName = SettingsManager.getLanguage(context);
        String languageCode;
        if (languageName.equals("English")) {
            languageCode = "en";
        } else {
            languageCode = "vi";
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }
}
