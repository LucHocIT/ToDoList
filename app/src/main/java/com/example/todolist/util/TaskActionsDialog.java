package com.example.todolist.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.CalendarView;
import android.widget.Button;
import android.widget.Switch;
import android.widget.LinearLayout;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.example.todolist.R;
import com.example.todolist.model.TodoTask;

public class TaskActionsDialog {
    
    public interface OnActionSelectedListener {
        void onStarAction(TodoTask task);
        void onCalendarAction(TodoTask task);
        void onDeleteAction(TodoTask task);
    }
    
    private Context context;
    private TodoTask task;
    private OnActionSelectedListener listener;
    private AlertDialog dialog;
    
    public TaskActionsDialog(Context context, TodoTask task, OnActionSelectedListener listener) {
        this.context = context;
        this.task = task;
        this.listener = listener;
        createDialog();
    }
    
    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_task_actions, null);
        builder.setView(dialogView);
        
        dialog = builder.create();
        
        // Make dialog background transparent so CardView corners show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        setupClickListeners(dialogView);
        updateStarText(dialogView);
    }
    
    private void setupClickListeners(View dialogView) {
        // Star action
        dialogView.findViewById(R.id.layout_action_star).setOnClickListener(v -> {
            if (listener != null) {
                listener.onStarAction(task);
            }
            dialog.dismiss();
        });
        
        // Calendar action
        dialogView.findViewById(R.id.layout_action_calendar).setOnClickListener(v -> {
            dialog.dismiss();
            showDatePickerDialog();
        });
        
        // Delete action
        dialogView.findViewById(R.id.layout_action_delete).setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAction(task);
            }
            dialog.dismiss();
        });
    }
    
    private void updateStarText(View dialogView) {
        TextView starText = dialogView.findViewById(R.id.tv_star_text);
        if (task.isImportant()) {
            starText.setText("Bỏ đánh dấu quan trọng");
        } else {
            starText.setText("Đánh dấu quan trọng");
        }
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
    
    private void showDatePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_date_picker, null);
        builder.setView(dialogView);
        
        AlertDialog dateDialog = builder.create();
        if (dateDialog.getWindow() != null) {
            dateDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        CalendarView calendarView = dialogView.findViewById(R.id.calendar_view);
        TextView timeStatus = dialogView.findViewById(R.id.tv_time_status);
        TextView reminderStatus = dialogView.findViewById(R.id.tv_reminder_status);
        TextView repeatStatus = dialogView.findViewById(R.id.tv_repeat_status);
        
        // Quick date buttons
        dialogView.findViewById(R.id.btn_today).setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            calendarView.setDate(today.getTimeInMillis());
            updateTaskDate(today);
        });
        
        dialogView.findViewById(R.id.btn_tomorrow).setOnClickListener(v -> {
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            calendarView.setDate(tomorrow.getTimeInMillis());
            updateTaskDate(tomorrow);
        });
        
        dialogView.findViewById(R.id.btn_next_week).setOnClickListener(v -> {
            Calendar nextWeek = Calendar.getInstance();
            nextWeek.add(Calendar.DAY_OF_MONTH, 3);
            calendarView.setDate(nextWeek.getTimeInMillis());
            updateTaskDate(nextWeek);
        });
        
        dialogView.findViewById(R.id.btn_weekend).setOnClickListener(v -> {
            Calendar weekend = Calendar.getInstance();
            int dayOfWeek = weekend.get(Calendar.DAY_OF_WEEK);
            int daysUntilSunday = (Calendar.SUNDAY - dayOfWeek + 7) % 7;
            if (daysUntilSunday == 0) daysUntilSunday = 7;
            weekend.add(Calendar.DAY_OF_MONTH, daysUntilSunday);
            calendarView.setDate(weekend.getTimeInMillis());
            updateTaskDate(weekend);
        });
        
        // Time option
        dialogView.findViewById(R.id.layout_time_option).setOnClickListener(v -> {
            dateDialog.dismiss();
            showTimePickerDialog();
        });
        
        // Reminder option
        dialogView.findViewById(R.id.layout_reminder_option).setOnClickListener(v -> {
            // Toggle reminder
            task.setHasReminder(!task.isHasReminder());
            reminderStatus.setText(task.isHasReminder() ? "Có" : "Không");
        });
        
        // Repeat option
        dialogView.findViewById(R.id.layout_repeat_option).setOnClickListener(v -> {
            dateDialog.dismiss();
            showRepeatPickerDialog();
        });
        
        // Calendar date selection
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            updateTaskDate(selectedDate);
        });
        
        // Cancel and confirm buttons
        dialogView.findViewById(R.id.btn_cancel_date).setOnClickListener(v -> dateDialog.dismiss());
        dialogView.findViewById(R.id.btn_confirm_date).setOnClickListener(v -> {
            if (listener != null) {
                listener.onCalendarAction(task);
            }
            dateDialog.dismiss();
        });
        
        dateDialog.show();
    }
    
    private void showTimePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null);
        builder.setView(dialogView);
        
        AlertDialog timeDialog = builder.create();
        if (timeDialog.getWindow() != null) {
            timeDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView hourText = dialogView.findViewById(R.id.tv_hour);
        TextView minuteText = dialogView.findViewById(R.id.tv_minute);
        
        // Quick time buttons
        dialogView.findViewById(R.id.btn_no_time).setOnClickListener(v -> {
            task.setDueTime("");
            timeDialog.dismiss();
            showDatePickerDialog();
        });
        
        dialogView.findViewById(R.id.btn_07_00).setOnClickListener(v -> {
            task.setDueTime("07:00");
            updateTimeDisplay(hourText, minuteText, 7, 0);
        });
        
        dialogView.findViewById(R.id.btn_09_00).setOnClickListener(v -> {
            task.setDueTime("09:00");
            updateTimeDisplay(hourText, minuteText, 9, 0);
        });
        
        dialogView.findViewById(R.id.btn_10_00).setOnClickListener(v -> {
            task.setDueTime("10:00");
            updateTimeDisplay(hourText, minuteText, 10, 0);
        });
        
        dialogView.findViewById(R.id.btn_12_00).setOnClickListener(v -> {
            task.setDueTime("12:00");
            updateTimeDisplay(hourText, minuteText, 12, 0);
        });
        
        dialogView.findViewById(R.id.btn_14_00).setOnClickListener(v -> {
            task.setDueTime("14:00");
            updateTimeDisplay(hourText, minuteText, 14, 0);
        });
        
        dialogView.findViewById(R.id.btn_16_00).setOnClickListener(v -> {
            task.setDueTime("16:00");
            updateTimeDisplay(hourText, minuteText, 16, 0);
        });
        
        dialogView.findViewById(R.id.btn_18_00).setOnClickListener(v -> {
            task.setDueTime("18:00");
            updateTimeDisplay(hourText, minuteText, 18, 0);
        });
        
        // Cancel and confirm buttons
        dialogView.findViewById(R.id.btn_cancel_time).setOnClickListener(v -> {
            timeDialog.dismiss();
            showDatePickerDialog();
        });
        
        dialogView.findViewById(R.id.btn_confirm_time).setOnClickListener(v -> {
            timeDialog.dismiss();
            showDatePickerDialog();
        });
        
        timeDialog.show();
    }
    
    private void showRepeatPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_repeat_picker, null);
        builder.setView(dialogView);
        
        AlertDialog repeatDialog = builder.create();
        if (repeatDialog.getWindow() != null) {
            repeatDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        Switch repeatSwitch = dialogView.findViewById(R.id.switch_repeat);
        LinearLayout repeatOptions = dialogView.findViewById(R.id.layout_repeat_options);
        
        repeatSwitch.setChecked(task.isRepeating());
        repeatOptions.setVisibility(task.isRepeating() ? View.VISIBLE : View.GONE);
        
        repeatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setRepeating(isChecked);
            repeatOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                task.setRepeatType("Không có");
            }
        });
        
        // Repeat option buttons
        dialogView.findViewById(R.id.btn_hourly).setOnClickListener(v -> {
            task.setRepeatType("Giờ");
            updateRepeatButtons(dialogView, R.id.btn_hourly);
        });
        
        dialogView.findViewById(R.id.btn_daily).setOnClickListener(v -> {
            task.setRepeatType("Hàng ngày");
            updateRepeatButtons(dialogView, R.id.btn_daily);
        });
        
        dialogView.findViewById(R.id.btn_weekly).setOnClickListener(v -> {
            task.setRepeatType("Hàng tuần");
            updateRepeatButtons(dialogView, R.id.btn_weekly);
        });
        
        dialogView.findViewById(R.id.btn_monthly).setOnClickListener(v -> {
            task.setRepeatType("Hàng tháng");
            updateRepeatButtons(dialogView, R.id.btn_monthly);
        });
        
        // Cancel and confirm buttons
        dialogView.findViewById(R.id.btn_cancel_repeat).setOnClickListener(v -> {
            repeatDialog.dismiss();
            showDatePickerDialog();
        });
        
        dialogView.findViewById(R.id.btn_confirm_repeat).setOnClickListener(v -> {
            repeatDialog.dismiss();
            showDatePickerDialog();
        });
        
        repeatDialog.show();
    }
    
    private void updateTaskDate(Calendar calendar) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        task.setDueDate(dateFormat.format(calendar.getTime()));
    }
    
    private void updateTimeDisplay(TextView hourText, TextView minuteText, int hour, int minute) {
        hourText.setText(String.valueOf(hour));
        minuteText.setText(String.format(Locale.getDefault(), "%02d", minute));
    }
    
    private void updateRepeatButtons(View dialogView, int selectedButtonId) {
        int[] buttonIds = {R.id.btn_hourly, R.id.btn_daily, R.id.btn_weekly, R.id.btn_monthly};
        for (int buttonId : buttonIds) {
            Button button = dialogView.findViewById(buttonId);
            if (buttonId == selectedButtonId) {
                button.setBackgroundResource(R.drawable.button_primary);
                button.setTextColor(context.getResources().getColor(android.R.color.white));
            } else {
                button.setBackgroundResource(R.drawable.button_outline);
                button.setTextColor(context.getResources().getColor(R.color.text_gray));
            }
        }
    }
}
