package com.example.todolist;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.model.TodoTask;

public class TaskDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_TASK_ID = "task_id";
    
    private EditText editDetailTitle;
    private TextView textDueDate;
    private TextView textTime;
    private TextView textReminderValue;
    private ImageView btnBack;
    
    private TodoTask currentTask;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        
        initViews();
        loadTaskData();
        setupClickListeners();
    }
    
    private void initViews() {
        editDetailTitle = findViewById(R.id.edit_detail_title);
        textDueDate = findViewById(R.id.text_due_date);
        textTime = findViewById(R.id.text_time);
        textReminderValue = findViewById(R.id.text_reminder_value);
        btnBack = findViewById(R.id.btn_back_detail);
    }
    
    private void loadTaskData() {
        int taskId = getIntent().getIntExtra(EXTRA_TASK_ID, -1);
        if (taskId != -1) {
            // Load task from database (simplified for now)
            currentTask = createSampleTask();
            displayTaskData();
        }
    }
    
    private TodoTask createSampleTask() {
        return new TodoTask(
            "Chúc ngủ ngon, đã đến giờ đi ngủ",
            "",
            "2025/05/25",
            "22:00"
        );
    }
    
    private void displayTaskData() {
        if (currentTask != null) {
            editDetailTitle.setText(currentTask.getTitle());
            textDueDate.setText(currentTask.getDueDate());
            textTime.setText(currentTask.getDueTime());
            textReminderValue.setText(currentTask.getReminderType());
        }
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
}
