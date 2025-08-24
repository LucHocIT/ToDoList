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
    private String selectedTime;
    private String selectedReminder;
    private String selectedRepeat;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    public DateTimePickerDialog(Context context, OnDateTimeSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.selectedTime = context.getString(R.string.none);
        this.selectedReminder = context.getString(R.string.none);
        this.selectedRepeat = context.getString(R.string.none);
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
        textSelectedTime.setText(selectedTime);
        textSelectedReminder.setText(selectedReminder);
        textSelectedRepeat.setText(selectedRepeat);
    }
    private void setupListeners() {
        // Calendar listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = dateFormat.format(calendar.getTime());
            updateRepeatState(); 
        });
        // Time picker
        layoutTimePicker.setOnClickListener(v -> showTimePicker());
        layoutReminderPicker.setOnClickListener(v -> {
            // Sá»­a chuá»—i cá»©ng "KhĂ´ng" thĂ nh tĂ i nguyĂªn chuá»—i
            if (selectedTime != null && !selectedTime.equals(context.getString(R.string.none))) {
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
        // Initial state updates
        updateReminderState();
        updateRepeatState();
    }
    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            context,
            (view, hourOfDay, minute) -> {
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                textSelectedTime.setText(selectedTime);
                updateReminderState(); 
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true
        );
        timePickerDialog.show();
    }
    private void updateReminderState() {
        boolean hasTime = selectedTime != null && !selectedTime.equals(context.getString(R.string.none));
        layoutReminderPicker.setEnabled(hasTime);
        layoutReminderPicker.setAlpha(hasTime ? 1.0f : 0.5f);
        if (hasTime && selectedReminder.equals(context.getString(R.string.none))) {
            selectedReminder = context.getString(R.string.reminder_5_min);
            textSelectedReminder.setText(selectedReminder);
        }
        if (!hasTime) {
            selectedReminder = context.getString(R.string.none);
            textSelectedReminder.setText(selectedReminder);
        }
    }
    private void updateRepeatState() {
        boolean hasDate = selectedDate != null && !selectedDate.isEmpty();
        layoutRepeatPicker.setEnabled(hasDate);
        layoutRepeatPicker.setAlpha(hasDate ? 1.0f : 0.5f);
        if (hasDate && selectedRepeat.equals(context.getString(R.string.none))) {
            selectedRepeat = context.getString(R.string.repeat_daily);
            textSelectedRepeat.setText(selectedRepeat);
        }
        if (!hasDate) {
            selectedRepeat = context.getString(R.string.none);
            textSelectedRepeat.setText(selectedRepeat);
        }
    }
    private void showReminderPicker() {
        String[] reminderOptions = {
                context.getString(R.string.none),            
                context.getString(R.string.reminder_5_min),  
                context.getString(R.string.reminder_15_min),
                context.getString(R.string.reminder_30_min), 
                context.getString(R.string.reminder_1_hour)  
        };
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        // Thay tháº¿ "Chá»n lá»i nháº¯c" báº±ng tĂ i nguyĂªn chuá»—i
        builder.setTitle(context.getString(R.string.choose_reminder));
        builder.setItems(reminderOptions, (dialog, which) -> {
            selectedReminder = reminderOptions[which];
            textSelectedReminder.setText(selectedReminder);
        });
        builder.show();
    }
    private void showRepeatPicker() {
        String[] repeatOptions = {
                context.getString(R.string.none),        
                context.getString(R.string.repeat_daily),   
                context.getString(R.string.repeat_weekly),
                context.getString(R.string.repeat_monthly), 
                context.getString(R.string.repeat_yearly)   
        };
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        // Thay tháº¿ "Chá»n láº·p láº¡i" báº±ng tĂ i nguyĂªn chuá»—i
        builder.setTitle(context.getString(R.string.choose_repeat_option));
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
        updateReminderState();
        updateRepeatState();
    }
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
