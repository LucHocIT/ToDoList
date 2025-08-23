package com.example.todolist.util;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.example.todolist.R;
public class TaskSortDialog {
    public interface OnSortOptionSelectedListener {
        void onSortOptionSelected(SortType sortType);
    }
    private AlertDialog dialog;
    private Context context;
    private OnSortOptionSelectedListener listener;
    private SharedPreferences preferences;
    private RadioButton radioDateTime, radioCreationTime, radioAlphabetical;
    private RadioGroup radioGroupSortOptions;
    private LinearLayout optionDateTime, optionCreationTime, optionAlphabetical;
    private TextView btnChoose;
    private SortType currentSortType = SortType.DATE_TIME;
    public TaskSortDialog(Context context, OnSortOptionSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.preferences = context.getSharedPreferences("task_sort_prefs", Context.MODE_PRIVATE);
        initDialog();
        loadSavedSortType();
    }
    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sort_tasks, null);
        builder.setView(dialogView);
        dialog = builder.create();
        // Make dialog background transparent so custom background shows
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        initViews(dialogView);
        setupClickListeners();
    }
    private void initViews(View dialogView) {
        radioDateTime = dialogView.findViewById(R.id.radio_date_time);
        radioCreationTime = dialogView.findViewById(R.id.radio_creation_time);
        radioAlphabetical = dialogView.findViewById(R.id.radio_alphabetical);
        radioGroupSortOptions = dialogView.findViewById(R.id.radioGroupSortOptions);
        optionDateTime = dialogView.findViewById(R.id.option_date_time);
        optionCreationTime = dialogView.findViewById(R.id.option_creation_time);
        optionAlphabetical = dialogView.findViewById(R.id.option_alphabetical);
        btnChoose = dialogView.findViewById(R.id.btn_choose_sort);
    }
    private void setupClickListeners() {
        optionDateTime.setOnClickListener(v -> {
            selectOption(SortType.DATE_TIME);
        });
        optionCreationTime.setOnClickListener(v -> {
            selectOption(SortType.CREATION_TIME);
        });
        optionAlphabetical.setOnClickListener(v -> {
            selectOption(SortType.ALPHABETICAL);
        });
        // Also handle direct radio button clicks
        radioGroupSortOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_date_time) {
                currentSortType = SortType.DATE_TIME;
            } else if (checkedId == R.id.radio_creation_time) {
                currentSortType = SortType.CREATION_TIME;
            } else if (checkedId == R.id.radio_alphabetical) {
                currentSortType = SortType.ALPHABETICAL;
            }
        });
        btnChoose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSortOptionSelected(currentSortType);
            }
            saveSortType(currentSortType);
            dialog.dismiss();
        });
    }
    private void selectOption(SortType sortType) {
        currentSortType = sortType;
        // Use RadioGroup to ensure only one is selected
        switch (sortType) {
            case DATE_TIME:
                radioGroupSortOptions.check(R.id.radio_date_time);
                break;
            case CREATION_TIME:
                radioGroupSortOptions.check(R.id.radio_creation_time);
                break;
            case ALPHABETICAL:
                radioGroupSortOptions.check(R.id.radio_alphabetical);
                break;
        }
    }
    private void loadSavedSortType() {
        String savedSort = preferences.getString("sort_type", SortType.DATE_TIME.getValue());
        currentSortType = SortType.fromValue(savedSort);
        selectOption(currentSortType);
    }
    private void saveSortType(SortType sortType) {
        preferences.edit()
                .putString("sort_type", sortType.getValue())
                .apply();
    }
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
