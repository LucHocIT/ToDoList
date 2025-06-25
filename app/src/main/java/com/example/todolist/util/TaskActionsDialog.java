package com.example.todolist.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.GridLayout;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
        
        TextView monthYearText = dialogView.findViewById(R.id.tv_month_year);
        GridLayout calendarContainer = dialogView.findViewById(R.id.calendar_days_container);
        TextView timeStatus = dialogView.findViewById(R.id.tv_time_status);
        TextView reminderStatus = dialogView.findViewById(R.id.tv_reminder_status);
        TextView repeatStatus = dialogView.findViewById(R.id.tv_repeat_status);
        
        // Current calendar instance
        Calendar currentCalendar = Calendar.getInstance();
        
        // Update status displays
        updateStatusDisplays(dialogView);
        
        // Generate calendar
        generateCalendar(calendarContainer, currentCalendar, monthYearText);
        
        // Initialize calendar with current date selection if task has date
        if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
            // Parse task date and highlight it
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Calendar taskDate = Calendar.getInstance();
                taskDate.setTime(dateFormat.parse(task.getDueDate()));
                highlightSelectedDate(calendarContainer, taskDate);
            } catch (Exception e) {
                // Handle parsing error
                e.printStackTrace();
            }
        }
        
        // Navigation buttons
        dialogView.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            generateCalendar(calendarContainer, currentCalendar, monthYearText);
        });
        
        dialogView.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            generateCalendar(calendarContainer, currentCalendar, monthYearText);
        });
        
        // Quick date buttons
        dialogView.findViewById(R.id.btn_today).setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            updateTaskDate(today);
            highlightSelectedDate(calendarContainer, today);
            updateQuickSelectButtons(dialogView, "today");
        });
        
        dialogView.findViewById(R.id.btn_tomorrow).setOnClickListener(v -> {
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            updateTaskDate(tomorrow);
            highlightSelectedDate(calendarContainer, tomorrow);
            updateQuickSelectButtons(dialogView, "tomorrow");
        });
        
        dialogView.findViewById(R.id.btn_next_week).setOnClickListener(v -> {
            Calendar nextWeek = Calendar.getInstance();
            nextWeek.add(Calendar.DAY_OF_MONTH, 3);
            updateTaskDate(nextWeek);
            highlightSelectedDate(calendarContainer, nextWeek);
            updateQuickSelectButtons(dialogView, "next_week");
        });
        
        dialogView.findViewById(R.id.btn_weekend).setOnClickListener(v -> {
            Calendar weekend = Calendar.getInstance();
            int dayOfWeek = weekend.get(Calendar.DAY_OF_WEEK);
            int daysUntilSunday = (Calendar.SUNDAY - dayOfWeek + 7) % 7;
            if (daysUntilSunday == 0) daysUntilSunday = 7;
            weekend.add(Calendar.DAY_OF_MONTH, daysUntilSunday);
            updateTaskDate(weekend);
            highlightSelectedDate(calendarContainer, weekend);
            updateQuickSelectButtons(dialogView, "weekend");
        });
        
        // Time option
        dialogView.findViewById(R.id.layout_time_option).setOnClickListener(v -> {
            dateDialog.dismiss();
            showTimePickerDialog(dialogView);
        });
        
        // Reminder option
        dialogView.findViewById(R.id.layout_reminder_option).setOnClickListener(v -> {
            // Toggle reminder
            task.setHasReminder(!task.isHasReminder());
            updateStatusDisplays(dialogView);
        });
        
        // Repeat option
        dialogView.findViewById(R.id.layout_repeat_option).setOnClickListener(v -> {
            dateDialog.dismiss();
            showRepeatPickerDialog(dialogView);
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
    
    private void generateCalendar(GridLayout container, Calendar calendar, TextView monthYearText) {
        container.removeAllViews();
        
        // Update month/year display
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
        monthYearText.setText(monthYearFormat.format(calendar.getTime()).toUpperCase());
        
        // Get first day of month
        Calendar firstDay = (Calendar) calendar.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);
        
        // Get last day of month
        Calendar lastDay = (Calendar) calendar.clone();
        lastDay.set(Calendar.DAY_OF_MONTH, lastDay.getActualMaximum(Calendar.DAY_OF_MONTH));
        
        // Calculate starting position (Sunday = 1, Monday = 2, ...)
        int startDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK);
        int startPosition = (startDayOfWeek == Calendar.SUNDAY) ? 0 : startDayOfWeek - 1;
        
        // Add days from previous month
        Calendar prevMonth = (Calendar) firstDay.clone();
        prevMonth.add(Calendar.DAY_OF_MONTH, -startPosition);
        
        // Generate 42 days (6 weeks)
        for (int i = 0; i < 42; i++) {
            TextView dayView = createDayView(prevMonth, firstDay.get(Calendar.MONTH) == prevMonth.get(Calendar.MONTH));
            container.addView(dayView);
            prevMonth.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
    
    private TextView createDayView(Calendar day, boolean isCurrentMonth) {
        TextView dayView = new TextView(context);
        dayView.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
        dayView.setTextSize(16);
        dayView.setGravity(android.view.Gravity.CENTER);
        
        // Set layout params
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(48);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        dayView.setLayoutParams(params);
        
        // Style the day view
        if (isCurrentMonth) {
            dayView.setTextColor(Color.parseColor("#212121"));
            
            // Check if it's today
            Calendar today = Calendar.getInstance();
            if (isSameDay(day, today)) {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.OVAL);
                drawable.setColor(Color.parseColor("#4285F4"));
                dayView.setBackground(drawable);
                dayView.setTextColor(Color.WHITE);
            }
            
            // Check if it's selected date
            if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Calendar taskDate = Calendar.getInstance();
                    taskDate.setTime(sdf.parse(task.getDueDate()));
                    if (isSameDay(day, taskDate)) {
                        GradientDrawable drawable = new GradientDrawable();
                        drawable.setShape(GradientDrawable.OVAL);
                        drawable.setColor(Color.parseColor("#4285F4"));
                        dayView.setBackground(drawable);
                        dayView.setTextColor(Color.WHITE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            dayView.setTextColor(Color.parseColor("#CCCCCC"));
        }
        
        // Set click listener
        final Calendar dayClone = (Calendar) day.clone();
        dayView.setOnClickListener(v -> {
            if (isCurrentMonth) {
                updateTaskDate(dayClone);
                highlightSelectedDate((GridLayout) dayView.getParent(), dayClone);
            }
        });
        
        return dayView;
    }
    
    private void highlightSelectedDate(GridLayout container, Calendar selectedDate) {
        // Reset all day views
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView dayView = (TextView) container.getChildAt(i);
            dayView.setBackground(null);
            dayView.setTextColor(Color.parseColor("#212121"));
        }
        
        // Regenerate calendar to highlight selected date
        Calendar displayCalendar = Calendar.getInstance();
        displayCalendar.setTime(selectedDate.getTime());
        TextView monthYearText = ((View) container.getParent()).findViewById(R.id.tv_month_year);
        generateCalendar(container, displayCalendar, monthYearText);
    }
    
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private void showTimePickerDialog(View parentDialogView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null);
        builder.setView(dialogView);
        
        AlertDialog timeDialog = builder.create();
        if (timeDialog.getWindow() != null) {
            timeDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView hourText = dialogView.findViewById(R.id.tv_hour);
        TextView minuteText = dialogView.findViewById(R.id.tv_minute);
        
        // Initialize with current time or default
        int currentHour = 2;
        int currentMinute = 7;
        if (task.getDueTime() != null && !task.getDueTime().isEmpty()) {
            String[] timeParts = task.getDueTime().split(":");
            if (timeParts.length == 2) {
                currentHour = Integer.parseInt(timeParts[0]);
                currentMinute = Integer.parseInt(timeParts[1]);
            }
        }
        
        updateTimeDisplay(hourText, minuteText, currentHour, currentMinute);
        updateClockHighlight(dialogView, currentHour);
        
        // Clock hour selection
        setupClockHourSelection(dialogView, hourText, minuteText);
        
        // Quick time buttons
        dialogView.findViewById(R.id.btn_no_time).setOnClickListener(v -> {
            task.setDueTime("");
            timeDialog.dismiss();
            showDatePickerDialog();
        });
        
        dialogView.findViewById(R.id.btn_07_00).setOnClickListener(v -> {
            task.setDueTime("07:00");
            updateTimeDisplay(hourText, minuteText, 7, 0);
            updateClockHighlight(dialogView, 7);
        });
        
        dialogView.findViewById(R.id.btn_09_00).setOnClickListener(v -> {
            task.setDueTime("09:00");
            updateTimeDisplay(hourText, minuteText, 9, 0);
            updateClockHighlight(dialogView, 9);
        });
        
        dialogView.findViewById(R.id.btn_10_00).setOnClickListener(v -> {
            task.setDueTime("10:00");
            updateTimeDisplay(hourText, minuteText, 10, 0);
            updateClockHighlight(dialogView, 10);
        });
        
        dialogView.findViewById(R.id.btn_12_00).setOnClickListener(v -> {
            task.setDueTime("12:00");
            updateTimeDisplay(hourText, minuteText, 12, 0);
            updateClockHighlight(dialogView, 12);
        });
        
        dialogView.findViewById(R.id.btn_14_00).setOnClickListener(v -> {
            task.setDueTime("14:00");
            updateTimeDisplay(hourText, minuteText, 14, 0);
            updateClockHighlight(dialogView, 14);
        });
        
        dialogView.findViewById(R.id.btn_16_00).setOnClickListener(v -> {
            task.setDueTime("16:00");
            updateTimeDisplay(hourText, minuteText, 16, 0);
            updateClockHighlight(dialogView, 16);
        });
        
        dialogView.findViewById(R.id.btn_18_00).setOnClickListener(v -> {
            task.setDueTime("18:00");
            updateTimeDisplay(hourText, minuteText, 18, 0);
            updateClockHighlight(dialogView, 18);
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
    
    private void setupClockHourSelection(View dialogView, TextView hourText, TextView minuteText) {
        // Hour selection from clock
        int[] hourIds = {
            R.id.hour_12, R.id.hour_1, R.id.hour_2, R.id.hour_3,
            R.id.hour_4, R.id.hour_5, R.id.hour_6, R.id.hour_7,
            R.id.hour_8, R.id.hour_9, R.id.hour_10, R.id.hour_11
        };
        
        for (int i = 0; i < hourIds.length; i++) {
            final int hour = (i == 0) ? 12 : i;
            TextView hourView = dialogView.findViewById(hourIds[i]);
            if (hourView != null) {
                hourView.setOnClickListener(v -> {
                    int selectedHour = hour;
                    String currentTime = hourText.getText().toString() + ":" + minuteText.getText().toString();
                    // Check if it's PM (if current hour > 12)
                    if (task.getDueTime() != null && !task.getDueTime().isEmpty()) {
                        String[] parts = task.getDueTime().split(":");
                        if (parts.length == 2) {
                            int currentHour = Integer.parseInt(parts[0]);
                            if (currentHour > 12) {
                                selectedHour = (hour == 12) ? 12 : hour + 12;
                            }
                        }
                    }
                    
                    task.setDueTime(String.format("%02d:%02d", selectedHour, Integer.parseInt(minuteText.getText().toString())));
                    updateTimeDisplay(hourText, minuteText, selectedHour, Integer.parseInt(minuteText.getText().toString()));
                    updateClockHighlight(dialogView, selectedHour);
                });
            }
        }
    }
    
    private void updateClockHighlight(View dialogView, int selectedHour) {
        // Reset all hour highlights
        int[] hourIds = {
            R.id.hour_12, R.id.hour_1, R.id.hour_2, R.id.hour_3,
            R.id.hour_4, R.id.hour_5, R.id.hour_6, R.id.hour_7,
            R.id.hour_8, R.id.hour_9, R.id.hour_10, R.id.hour_11
        };
        
        for (int hourId : hourIds) {
            TextView hourView = dialogView.findViewById(hourId);
            if (hourView != null) {
                hourView.setBackgroundResource(0);
                hourView.setTextColor(Color.parseColor("#757575"));
            }
        }
        
        // Highlight selected hour
        int displayHour = selectedHour > 12 ? selectedHour - 12 : selectedHour;
        if (displayHour == 0) displayHour = 12;
        
        int selectedHourId = 0;
        switch (displayHour) {
            case 12: selectedHourId = R.id.hour_12; break;
            case 1: selectedHourId = R.id.hour_1; break;
            case 2: selectedHourId = R.id.hour_2; break;
            case 3: selectedHourId = R.id.hour_3; break;
            case 4: selectedHourId = R.id.hour_4; break;
            case 5: selectedHourId = R.id.hour_5; break;
            case 6: selectedHourId = R.id.hour_6; break;
            case 7: selectedHourId = R.id.hour_7; break;
            case 8: selectedHourId = R.id.hour_8; break;
            case 9: selectedHourId = R.id.hour_9; break;
            case 10: selectedHourId = R.id.hour_10; break;
            case 11: selectedHourId = R.id.hour_11; break;
        }
        
        if (selectedHourId != 0) {
            TextView selectedHourView = dialogView.findViewById(selectedHourId);
            if (selectedHourView != null) {
                selectedHourView.setBackgroundResource(R.drawable.circle_blue);
                selectedHourView.setTextColor(Color.WHITE);
            }
        }
    }
    
    private void showRepeatPickerDialog(View parentDialogView) {
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
    
    private void updateQuickSelectButtons(View dialogView, String buttonType) {
        // Reset all buttons to default style
        com.google.android.material.button.MaterialButton btnNoReminder = dialogView.findViewById(R.id.btn_no_reminder);
        com.google.android.material.button.MaterialButton btnToday = dialogView.findViewById(R.id.btn_today);
        com.google.android.material.button.MaterialButton btnTomorrow = dialogView.findViewById(R.id.btn_tomorrow);
        com.google.android.material.button.MaterialButton btnNextWeek = dialogView.findViewById(R.id.btn_next_week);
        com.google.android.material.button.MaterialButton btnWeekend = dialogView.findViewById(R.id.btn_weekend);
        
        // Reset all to default style
        resetButtonStyle(btnNoReminder);
        resetButtonStyle(btnToday);
        resetButtonStyle(btnTomorrow);
        resetButtonStyle(btnNextWeek);
        resetButtonStyle(btnWeekend);
        
        // Highlight selected button
        switch (buttonType) {
            case "no_reminder":
                setButtonSelected(btnNoReminder);
                break;
            case "today":
                setButtonSelected(btnToday);
                break;
            case "tomorrow":
                setButtonSelected(btnTomorrow);
                break;
            case "next_week":
                setButtonSelected(btnNextWeek);
                break;
            case "weekend":
                setButtonSelected(btnWeekend);
                break;
        }
    }
    
    private void resetButtonStyle(com.google.android.material.button.MaterialButton button) {
        if (button != null) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
            button.setTextColor(Color.parseColor("#757575"));
        }
    }
    
    private void setButtonSelected(com.google.android.material.button.MaterialButton button) {
        if (button != null) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4285F4")));
            button.setTextColor(Color.WHITE);
        }
    }
    
    private void updateStatusDisplays(View dialogView) {
        TextView timeStatus = dialogView.findViewById(R.id.tv_time_status);
        TextView reminderStatus = dialogView.findViewById(R.id.tv_reminder_status);
        TextView repeatStatus = dialogView.findViewById(R.id.tv_repeat_status);
        
        if (timeStatus != null) {
            String timeText = (task.getDueTime() != null && !task.getDueTime().isEmpty()) 
                ? task.getDueTime() : "Không";
            timeStatus.setText(timeText);
        }
        
        if (reminderStatus != null) {
            reminderStatus.setText(task.isHasReminder() ? "05:55" : "Không");
        }
        
        if (repeatStatus != null) {
            String repeatText = task.isRepeating() ? task.getRepeatType() : "Không";
            repeatStatus.setText(repeatText);
        }
    }
}
