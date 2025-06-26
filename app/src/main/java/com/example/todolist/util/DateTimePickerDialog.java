package com.example.todolist.util;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.todolist.R;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateTimePickerDialog {
    
    public interface OnDateTimeSelectedListener {
        void onDateTimeSelected(String date, String time, String reminder, String repeat);
    }
    
    private Dialog dialog;
    private Context context;
    private OnDateTimeSelectedListener listener;
    
    private CalendarView calendarView;
    private TextView textSelectedTime;
    private TextView textSelectedReminder;
    private TextView textSelectedRepeat;
    private LinearLayout layoutTimePicker;
    private LinearLayout layoutReminderPicker;
    private LinearLayout layoutRepeatPicker;
    private TextView btnCancel;
    private TextView btnSave;
    
    private String selectedDate = "";
    private String selectedTime = "Không";
    private String selectedReminder = "Không";
    private String selectedRepeat = "Không";
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
    
    public DateTimePickerDialog(Context context, OnDateTimeSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        initDialog();
    }
    
    private void initDialog() {
        dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_date_time_picker, null);
        dialog.setContentView(view);
        
        // Initialize views
        calendarView = view.findViewById(R.id.calendar_view);
        textSelectedTime = view.findViewById(R.id.text_selected_time);
        textSelectedReminder = view.findViewById(R.id.text_selected_reminder);
        textSelectedRepeat = view.findViewById(R.id.text_selected_repeat);
        layoutTimePicker = view.findViewById(R.id.layout_time_picker);
        layoutReminderPicker = view.findViewById(R.id.layout_reminder_picker);
        layoutRepeatPicker = view.findViewById(R.id.layout_repeat_picker);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSave = view.findViewById(R.id.btn_save);
        
        setupListeners();
        setupDefaultValues();
    }
    
    private void setupDefaultValues() {
        Calendar today = Calendar.getInstance();
        selectedDate = dateFormat.format(today.getTime());
        calendarView.setDate(today.getTimeInMillis());
    }
    
    private void setupListeners() {
        // Calendar listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = dateFormat.format(calendar.getTime());
        });
        
        // Time picker
        layoutTimePicker.setOnClickListener(v -> showTimePicker());
        
        // Reminder picker - only enabled when time is set
        layoutReminderPicker.setOnClickListener(v -> {
            if (selectedTime != null && !selectedTime.equals("Không")) {
                showReminderPicker();
            }
        });
        
        // Repeat picker
        layoutRepeatPicker.setOnClickListener(v -> showRepeatPicker());
        
        // Action buttons
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDateTimeSelected(selectedDate, selectedTime, selectedReminder, selectedRepeat);
            }
            dialog.dismiss();
        });
        
        // Initial state update
        updateReminderState();
    }
    
    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            context,
            (view, hourOfDay, minute) -> {
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                textSelectedTime.setText(selectedTime);
                updateReminderState(); // Update reminder state when time changes
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        );
        timePickerDialog.show();
    }
    
    private void updateReminderState() {
        boolean hasTime = selectedTime != null && !selectedTime.equals("Không");
        
        // Enable/disable reminder picker
        layoutReminderPicker.setEnabled(hasTime);
        layoutReminderPicker.setAlpha(hasTime ? 1.0f : 0.5f);
        
        // Set default reminder to "5 phút trước" when time is selected
        if (hasTime && selectedReminder.equals("Không")) {
            selectedReminder = "5 phút trước";
            textSelectedReminder.setText(selectedReminder);
        }
        
        // Reset reminder if no time selected
        if (!hasTime) {
            selectedReminder = "Không";
            textSelectedReminder.setText(selectedReminder);
        }
    }
    
    private void showReminderPicker() {
        String[] reminderOptions = {"Không", "5 phút trước", "15 phút trước", "30 phút trước", "1 giờ trước", "1 ngày trước"};
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Chọn lời nhắc");
        builder.setItems(reminderOptions, (dialog, which) -> {
            selectedReminder = reminderOptions[which];
            textSelectedReminder.setText(selectedReminder);
        });
        builder.show();
    }
    
    private void showRepeatPicker() {
        String[] repeatOptions = {"Không", "Hàng ngày", "Hàng tuần", "Hàng tháng", "Hàng năm"};
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Chọn lặp lại");
        builder.setItems(repeatOptions, (dialog, which) -> {
            selectedRepeat = repeatOptions[which];
            textSelectedRepeat.setText(selectedRepeat);
        });
        builder.show();
    }
    
    public void setInitialValues(String date, String time, String reminder, String repeat) {
        if (date != null && !date.isEmpty()) {
            selectedDate = date;
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateFormat.parse(date));
                calendarView.setDate(calendar.getTimeInMillis());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (time != null && !time.isEmpty()) {
            selectedTime = time;
            textSelectedTime.setText(time);
        }
        
        if (reminder != null && !reminder.isEmpty()) {
            selectedReminder = reminder;
            textSelectedReminder.setText(reminder);
        }
        
        if (repeat != null && !repeat.isEmpty()) {
            selectedRepeat = repeat;
            textSelectedRepeat.setText(repeat);
        }
        
        // Update reminder state based on time
        updateReminderState();
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
