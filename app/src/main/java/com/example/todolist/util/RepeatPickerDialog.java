package com.example.todolist.util;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.example.todolist.R;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;

public class RepeatPickerDialog {
    
    public interface OnRepeatSelectedListener {
        void onRepeatSelected(boolean isRepeating, String repeatType);
    }
    
    private Context context;
    private OnRepeatSelectedListener listener;
    private Dialog dialog;
    private Switch switchRepeat;
    private LinearLayout layoutRepeatOptions;
    private TextView tvMonthYear;
    private GridLayout calendarDaysContainer;
    private Calendar currentCalendar;
    
    private boolean isRepeating = false;
    private String repeatType = "daily"; // daily, weekly, monthly, hourly
    
    public RepeatPickerDialog(Context context, OnRepeatSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.currentCalendar = Calendar.getInstance();
        createDialog();
    }
    
    private void createDialog() {
        dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_repeat_picker, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        initViews(view);
        setupClickListeners(view);
        updateCalendar();
    }
    
    private void initViews(View view) {
        switchRepeat = view.findViewById(R.id.switch_repeat);
        layoutRepeatOptions = view.findViewById(R.id.layout_repeat_options);
        tvMonthYear = view.findViewById(R.id.tv_month_year_repeat);
        calendarDaysContainer = view.findViewById(R.id.calendar_days_container_repeat);
    }
    
    private void setupClickListeners(View view) {
        // Month navigation
        view.findViewById(R.id.btn_prev_month_repeat).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });
        
        view.findViewById(R.id.btn_next_month_repeat).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });
        
        // Repeat switch
        switchRepeat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isRepeating = isChecked;
            layoutRepeatOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        
        // Repeat frequency buttons
        MaterialButton btnHourly = view.findViewById(R.id.btn_repeat_hourly);
        MaterialButton btnDaily = view.findViewById(R.id.btn_repeat_daily);
        MaterialButton btnWeekly = view.findViewById(R.id.btn_repeat_weekly);
        MaterialButton btnMonthly = view.findViewById(R.id.btn_repeat_monthly);
        
        btnHourly.setOnClickListener(v -> selectRepeatType("hourly", btnHourly));
        btnDaily.setOnClickListener(v -> selectRepeatType("daily", btnDaily));
        btnWeekly.setOnClickListener(v -> selectRepeatType("weekly", btnWeekly));
        btnMonthly.setOnClickListener(v -> selectRepeatType("monthly", btnMonthly));
        
        // Action buttons
        TextView btnCancel = view.findViewById(R.id.btn_cancel_repeat);
        TextView btnDone = view.findViewById(R.id.btn_done_repeat);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDone.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRepeatSelected(isRepeating, repeatType);
            }
            dialog.dismiss();
        });
    }
    
    private void selectRepeatType(String type, MaterialButton selectedButton) {
        repeatType = type;
        
        // Reset all buttons to default style
        View view = dialog.findViewById(android.R.id.content);
        MaterialButton btnHourly = view.findViewById(R.id.btn_repeat_hourly);
        MaterialButton btnDaily = view.findViewById(R.id.btn_repeat_daily);
        MaterialButton btnWeekly = view.findViewById(R.id.btn_repeat_weekly);
        MaterialButton btnMonthly = view.findViewById(R.id.btn_repeat_monthly);
        
        resetButtonStyle(btnHourly);
        resetButtonStyle(btnDaily);
        resetButtonStyle(btnWeekly);
        resetButtonStyle(btnMonthly);
        
        // Highlight selected button
        selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4285F4));
        selectedButton.setTextColor(0xFFFFFFFF);
    }
    
    private void resetButtonStyle(MaterialButton button) {
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF5F5F5));
        button.setTextColor(0xFF666666);
    }
    
    private void updateCalendar() {
        // Update month/year display
        String[] months = {"THÁNG 1", "THÁNG 2", "THÁNG 3", "THÁNG 4", "THÁNG 5", "THÁNG 6",
                          "THÁNG 7", "THÁNG 8", "THÁNG 9", "THÁNG 10", "THÁNG 11", "THÁNG 12"};
        int month = currentCalendar.get(Calendar.MONTH);
        int year = currentCalendar.get(Calendar.YEAR);
        tvMonthYear.setText(months[month] + "  " + year);
        
        // Generate calendar days
        generateCalendarDays();
    }
    
    private void generateCalendarDays() {
        calendarDaysContainer.removeAllViews();
        
        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Add empty cells for days before the first day of the month
        for (int i = 1; i < firstDayOfWeek; i++) {
            View emptyView = new View(context);
            emptyView.setLayoutParams(new GridLayout.LayoutParams());
            calendarDaysContainer.addView(emptyView);
        }
        
        // Add days of the month
        for (int day = 1; day <= daysInMonth; day++) {
            TextView dayView = new TextView(context);
            dayView.setText(String.valueOf(day));
            dayView.setTextSize(14);
            dayView.setTextColor(0xFF333333);
            dayView.setGravity(android.view.Gravity.CENTER);
            dayView.setPadding(8, 8, 8, 8);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.height = 48;
            dayView.setLayoutParams(params);
            
            // Highlight current day
            Calendar today = Calendar.getInstance();
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)) {
                dayView.setBackgroundResource(R.drawable.circle_selected_hour);
                dayView.setTextColor(0xFFFFFFFF);
            }
            
            calendarDaysContainer.addView(dayView);
        }
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
