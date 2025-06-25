package com.example.todolist;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.adapter.CategorySpinnerAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Category;
import com.example.todolist.model.TodoTask;
import java.util.List;

public class TaskDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_TASK_ID = "task_id";
    
    private EditText editDetailTitle;
    private TextView textDueDate;
    private TextView textTime;
    private TextView textReminderValue;
    private Spinner spinnerCategory;
    private ImageView btnBack;
    
    private TodoTask currentTask;
    private TodoDatabase database;
    private Category selectedCategory;
    private CategorySpinnerAdapter categoryAdapter;
    private List<Category> allCategories;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        
        database = TodoDatabase.getInstance(this);
        
        initViews();
        loadTaskData();
        setupClickListeners();
    }
    
    private void initViews() {
        editDetailTitle = findViewById(R.id.edit_detail_title);
        textDueDate = findViewById(R.id.text_due_date);
        textTime = findViewById(R.id.text_time);
        textReminderValue = findViewById(R.id.text_reminder_value);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnBack = findViewById(R.id.btn_back_detail);
        
        setupCategorySpinner();
    }
    
    private void loadTaskData() {
        int taskId = getIntent().getIntExtra(EXTRA_TASK_ID, -1);
        if (taskId != -1) {
            // Load task from database
            new Thread(() -> {
                currentTask = database.todoDao().getTaskById(taskId);
                if (currentTask == null) {
                    currentTask = createSampleTask();
                }
                runOnUiThread(this::displayTaskData);
            }).start();
        } else {
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
            
            // Set category in spinner
            if (categoryAdapter != null && currentTask.getCategory() != null) {
                // Find category by name and set selection
                for (int i = 0; i < allCategories.size(); i++) {
                    if (allCategories.get(i).getName().equals(currentTask.getCategory())) {
                        spinnerCategory.setSelection(categoryAdapter.getPositionForCategoryId(allCategories.get(i).getId()));
                        break;
                    }
                }
            }
        }
    }
    
    private void setupCategorySpinner() {
        // Load categories from database
        new Thread(() -> {
            allCategories = database.categoryDao().getAllCategories();
            runOnUiThread(() -> {
                categoryAdapter = new CategorySpinnerAdapter(this, allCategories);
                spinnerCategory.setAdapter(categoryAdapter);
                
                // Set up selection listener
                spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Category selectedCat = categoryAdapter.getCategory(position);
                        if (selectedCat != null && currentTask != null) {
                            selectedCategory = selectedCat;
                            // Update task category
                            if (selectedCat.getId() == 0) {
                                currentTask.setCategory(null); // "không có thể loại"
                            } else {
                                currentTask.setCategory(selectedCat.getName());
                            }
                            
                            // Save to database
                            new Thread(() -> {
                                database.todoDao().updateTask(currentTask);
                            }).start();
                        }
                    }
                    
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do nothing
                    }
                });
                
                // Update display if task is already loaded
                if (currentTask != null) {
                    displayTaskData();
                }
            });
        }).start();
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
}
