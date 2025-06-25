package com.example.todolist.util;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.todolist.R;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;

public class CalendarPickerDialog {
    
    public interface OnDateSelectedListener {
        void onDateSelected(Calendar selectedDate, boolean hasTime, String time, boolean isRepeating, String repeatType);
    }
    
    private Context context;
    private OnDateSelectedListener listener;
    private Dialog dialog;
    private TextView tvMonthYear;
    private GridLayout calendarDaysContainer;
    private TextView tvTimeStatus;
    private TextView tvRepeatStatus;
    private LinearLayout layoutTimeOption;
    private LinearLayout layoutRepeatOption;
    private Calendar currentCalendar;
    private Calendar selectedDate;
    
    // Time and repeat settings
    private boolean hasTime = false;
    private String selectedTime = "";
    private boolean isRepeating = false;
    private String repeatType = "daily";
    
    public CalendarPickerDialog(Context context, OnDateSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.currentCalendar = Calendar.getInstance();
        this.selectedDate = Calendar.getInstance();
        createDialog();
    }
    
    private void createDialog() {
        dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_calendar_picker, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        initViews(view);
        setupClickListeners(view);
        updateCalendar();
    }
      private void initViews(View view) {
        tvMonthYear = view.findViewById(R.id.tv_month_year);
        calendarDaysContainer = view.findViewById(R.id.calendar_days_container);
        layoutTimeOption = view.findViewById(R.id.layout_time_option);
        layoutRepeatOption = view.findViewById(R.id.layout_repeat_option);
        tvTimeStatus = view.findViewById(R.id.tv_time_status);
        tvRepeatStatus = view.findViewById(R.id.tv_repeat_status);
    }
    
    private void setupClickListeners(View view) {
        // Month navigation
        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });
        
        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });
        
        // Quick date selection
        MaterialButton btnNoDueDate = view.findViewById(R.id.btn_no_due_date);
        MaterialButton btnToday = view.findViewById(R.id.btn_today);
        MaterialButton btnTomorrow = view.findViewById(R.id.btn_tomorrow);
        MaterialButton btnThreeDays = view.findViewById(R.id.btn_three_days);
        MaterialButton btnThisSunday = view.findViewById(R.id.btn_this_sunday);
        
        btnNoDueDate.setOnClickListener(v -> selectDate(null));
        btnToday.setOnClickListener(v -> selectDate(Calendar.getInstance()));
        btnTomorrow.setOnClickListener(v -> {
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            selectDate(tomorrow);
        });
        btnThreeDays.setOnClickListener(v -> {
            Calendar threeDays = Calendar.getInstance();
            threeDays.add(Calendar.DAY_OF_MONTH, 3);
            selectDate(threeDays);
        });
        btnThisSunday.setOnClickListener(v -> {
            Calendar sunday = Calendar.getInstance();
            int daysUntilSunday = (Calendar.SUNDAY - sunday.get(Calendar.DAY_OF_WEEK) + 7) % 7;
            if (daysUntilSunday == 0) daysUntilSunday = 7; // Next Sunday if today is Sunday
            sunday.add(Calendar.DAY_OF_MONTH, daysUntilSunday);
            selectDate(sunday);
        });
        
        // Time option click
        if (layoutTimeOption != null) {
            layoutTimeOption.setOnClickListener(v -> showTimePicker());
        }
        
        // Repeat option click
        if (layoutRepeatOption != null) {
            layoutRepeatOption.setOnClickListener(v -> showRepeatPicker());
        }
        
        // Action buttons
        TextView btnCancel = view.findViewById(R.id.btn_cancel_calendar);
        TextView btnDone = view.findViewById(R.id.btn_done_calendar);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDone.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDateSelected(selectedDate, hasTime, selectedTime, isRepeating, repeatType);
            }
            dialog.dismiss();
        });
    }
    
    private void selectDate(Calendar date) {
        selectedDate = date;
        updateCalendar();
    }
    
    private void showTimePicker() {
        TimePickerDialog timeDialog = new TimePickerDialog(context, (time, hasTimeSet) -> {
            hasTime = hasTimeSet;
            selectedTime = time;
            updateTimeStatus();
        });
        timeDialog.show();
    }
    
    private void showRepeatPicker() {
        RepeatPickerDialog repeatDialog = new RepeatPickerDialog(context, (repeating, type) -> {
            isRepeating = repeating;
            repeatType = type;
            updateRepeatStatus();
        });
        repeatDialog.show();
    }
    
    private void updateTimeStatus() {
        if (tvTimeStatus != null) {
            if (hasTime) {
                tvTimeStatus.setText(selectedTime);
            } else {
                tvTimeStatus.setText("Không");
            }
        }
    }
    
    private void updateRepeatStatus() {
        if (tvRepeatStatus != null) {
            if (isRepeating) {
                switch (repeatType) {
                    case "hourly":
                        tvRepeatStatus.setText("Hàng giờ");
                        break;
                    case "daily":
                        tvRepeatStatus.setText("Hàng ngày");
                        break;
                    case "weekly":
                        tvRepeatStatus.setText("Hàng tuần");
                        break;
                    case "monthly":
                        tvRepeatStatus.setText("Hàng tháng");
                        break;
                    default:
                        tvRepeatStatus.setText("Lặp lại");
                }
            } else {
                tvRepeatStatus.setText("Không");
            }
        }
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
            GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams();
            emptyParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            emptyParams.height = 48;
            emptyView.setLayoutParams(emptyParams);
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
            dayView.setClickable(true);
            dayView.setBackground(context.getDrawable(android.R.attr.selectableItemBackground));
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.height = 48;
            dayView.setLayoutParams(params);
            
            // Highlight selected day
            if (selectedDate != null &&
                calendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                day == selectedDate.get(Calendar.DAY_OF_MONTH)) {
                dayView.setBackgroundResource(R.drawable.circle_selected_hour);
                dayView.setTextColor(0xFFFFFFFF);
            }
            
            // Highlight today
            Calendar today = Calendar.getInstance();
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH) &&
                (selectedDate == null || 
                 selectedDate.get(Calendar.YEAR) != today.get(Calendar.YEAR) ||
                 selectedDate.get(Calendar.MONTH) != today.get(Calendar.MONTH) ||
                 selectedDate.get(Calendar.DAY_OF_MONTH) != today.get(Calendar.DAY_OF_MONTH))) {
                dayView.setBackgroundResource(R.drawable.circle_today);
                dayView.setTextColor(0xFF4285F4);
            }
            
            final int finalDay = day;
            final Calendar finalCalendar = (Calendar) calendar.clone();
            dayView.setOnClickListener(v -> {
                finalCalendar.set(Calendar.DAY_OF_MONTH, finalDay);
                selectDate((Calendar) finalCalendar.clone());
            });
            
            calendarDaysContainer.addView(dayView);
        }
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
