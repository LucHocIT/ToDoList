package com.example.todolist;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Reminder;
import com.example.todolist.model.TodoTask;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReminderActivity extends AppCompatActivity {
    
    private TextView textTaskTitle;
    private TextView textReminderPhrase;
    private TextView textReminderTime;
    private Button btnSave;
    private ImageView btnBack;
    
    // Day selection TextViews
    private TextView dayCN, dayT2, dayT3, dayT4, dayT5, dayT6, dayT7;
    private List<TextView> dayViews;
    private List<Boolean> selectedDays;
    
    private TodoDatabase database;
    private TodoTask currentTask;
    private String selectedTime = "22:00";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);
        
        initViews();
        setupDatabase();
        loadTaskData();
        setupClickListeners();
        initializeDaySelection();
    }
    
    private void initViews() {
        textTaskTitle = findViewById(R.id.text_task_title);
        textReminderPhrase = findViewById(R.id.text_reminder_phrase);
        textReminderTime = findViewById(R.id.text_reminder_time);
        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.btn_back);
        
        // Day selection views
        dayCN = findViewById(R.id.day_cn);
        dayT2 = findViewById(R.id.day_t2);
        dayT3 = findViewById(R.id.day_t3);
        dayT4 = findViewById(R.id.day_t4);
        dayT5 = findViewById(R.id.day_t5);
        dayT6 = findViewById(R.id.day_t6);
        dayT7 = findViewById(R.id.day_t7);
        
        dayViews = new ArrayList<>();
        dayViews.add(dayCN);   // 0 = Sunday
        dayViews.add(dayT2);   // 1 = Monday
        dayViews.add(dayT3);   // 2 = Tuesday
        dayViews.add(dayT4);   // 3 = Wednesday
        dayViews.add(dayT5);   // 4 = Thursday
        dayViews.add(dayT6);   // 5 = Friday
        dayViews.add(dayT7);   // 6 = Saturday
    }
    
    private void setupDatabase() {
        database = TodoDatabase.getInstance(this);
    }
    
    private void loadTaskData() {
        Intent intent = getIntent();
        int taskId = intent.getIntExtra("task_id", -1);
        
        if (taskId != -1) {
            // Load task from database
            new Thread(() -> {
                currentTask = database.todoDao().getTaskById(taskId);
                runOnUiThread(() -> {
                    if (currentTask != null) {
                        textTaskTitle.setText(currentTask.getTitle());
                        textReminderPhrase.setText("Chúc ngủ ngon, đã đến giờ " + currentTask.getTitle().toLowerCase());
                        
                        // Load existing reminder if exists
                        loadExistingReminder();
                    }
                });
            }).start();
        }
    }
    
    private void loadExistingReminder() {
        new Thread(() -> {
            List<Reminder> existingReminders = database.reminderDao().getRemindersByTaskId(currentTask.getId());
            runOnUiThread(() -> {
                if (!existingReminders.isEmpty()) {
                    Reminder reminder = existingReminders.get(0);
                    selectedTime = reminder.getReminderTime();
                    textReminderTime.setText(selectedTime);
                    
                    // Load selected days
                    String selectedDaysStr = reminder.getSelectedDays();
                    if (selectedDaysStr != null && !selectedDaysStr.isEmpty()) {
                        String[] days = selectedDaysStr.split(",");
                        for (String day : days) {
                            try {
                                int dayIndex = Integer.parseInt(day.trim());
                                if (dayIndex >= 0 && dayIndex < selectedDays.size()) {
                                    selectedDays.set(dayIndex, true);
                                    updateDayViewAppearance(dayViews.get(dayIndex), true);
                                }
                            } catch (NumberFormatException e) {
                                // Ignore invalid day format
                            }
                        }
                    }
                }
            });
        }).start();
    }
    
    private void initializeDaySelection() {
        selectedDays = new ArrayList<>();
        // Initialize with weekdays selected (Monday to Friday)
        selectedDays.add(false); // Sunday
        selectedDays.add(true);  // Monday
        selectedDays.add(true);  // Tuesday
        selectedDays.add(true);  // Wednesday
        selectedDays.add(true);  // Thursday
        selectedDays.add(true);  // Friday
        selectedDays.add(false); // Saturday
        
        // Update appearance for default selection
        for (int i = 0; i < dayViews.size(); i++) {
            updateDayViewAppearance(dayViews.get(i), selectedDays.get(i));
        }
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnSave.setOnClickListener(v -> saveReminder());
        
        textReminderTime.setOnClickListener(v -> showTimePicker());
        
        // Day selection click listeners
        for (int i = 0; i < dayViews.size(); i++) {
            final int dayIndex = i;
            dayViews.get(i).setOnClickListener(v -> toggleDay(dayIndex));
        }
    }
    
    private void toggleDay(int dayIndex) {
        selectedDays.set(dayIndex, !selectedDays.get(dayIndex));
        updateDayViewAppearance(dayViews.get(dayIndex), selectedDays.get(dayIndex));
    }
    
    private void updateDayViewAppearance(TextView dayView, boolean isSelected) {
        if (isSelected) {
            dayView.setBackground(getDrawable(R.drawable.day_selected));
            dayView.setTextColor(getColor(android.R.color.white));
        } else {
            dayView.setBackground(getDrawable(R.drawable.day_selector));
            dayView.setTextColor(getColor(R.color.gray_text));
        }
    }
    
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        // Parse current time if available
        if (selectedTime != null && selectedTime.contains(":")) {
            String[] timeParts = selectedTime.split(":");
            try {
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
            } catch (NumberFormatException e) {
                // Use current time as fallback
            }
        }
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute);
                    textReminderTime.setText(selectedTime);
                }, hour, minute, true);
        
        timePickerDialog.show();
    }
    
    private void saveReminder() {
        if (currentTask == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy nhiệm vụ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get selected days as comma-separated string
        List<String> selectedDaysList = new ArrayList<>();
        for (int i = 0; i < selectedDays.size(); i++) {
            if (selectedDays.get(i)) {
                selectedDaysList.add(String.valueOf(i));
            }
        }
        
        if (selectedDaysList.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một ngày", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String selectedDaysStr = String.join(",", selectedDaysList);
        
        new Thread(() -> {
            // Delete existing reminders for this task
            database.reminderDao().deleteRemindersByTaskId(currentTask.getId());
            
            // Create new reminder
            Reminder reminder = new Reminder(
                currentTask.getId(),
                currentTask.getTitle(),
                "daily",
                selectedTime,
                selectedDaysStr
            );
            
            long reminderId = database.reminderDao().insertReminder(reminder);
            
            runOnUiThread(() -> {
                if (reminderId > 0) {
                    Toast.makeText(this, "Đã lưu lời nhắc thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Lỗi khi lưu lời nhắc", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
