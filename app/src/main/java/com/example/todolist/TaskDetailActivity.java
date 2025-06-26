package com.example.todolist;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.adapter.CategorySpinnerAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Category;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.DateTimePickerDialog;
import java.util.List;

public class TaskDetailActivity extends AppCompatActivity {
    
    public static final String EXTRA_TASK_ID = "task_id";
    
    private EditText editDetailTitle;
    private TextView textDueDate;
    private TextView textTime;
    private TextView textReminderValue;
    private TextView textPriorityValue;
    private Spinner spinnerCategory;
    private LinearLayout layoutDatePicker;
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
        textPriorityValue = findViewById(R.id.text_priority_value);
        spinnerCategory = findViewById(R.id.spinner_category);
        layoutDatePicker = findViewById(R.id.layout_date_picker);
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
            null,  // No default date
            null   // No default time
        );
    }
    
    private void displayTaskData() {
        if (currentTask != null) {
            editDetailTitle.setText(currentTask.getTitle());
            textDueDate.setText(currentTask.getDueDate() != null ? currentTask.getDueDate() : "Không");
            textTime.setText(currentTask.getDueTime() != null ? currentTask.getDueTime() : "Không");
            textReminderValue.setText(currentTask.getReminderType() != null ? currentTask.getReminderType() : "Không");
            
            // Set priority based on important flag
            textPriorityValue.setText(currentTask.isImportant() ? "Cao" : "Thấp");
            
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
                                runOnUiThread(() -> {
                                    // Set result to indicate data changed
                                    setResult(RESULT_OK);
                                });
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
        
        // Date picker click listener
        layoutDatePicker.setOnClickListener(v -> showDateTimePicker());
    }
    
    private void showDateTimePicker() {
        DateTimePickerDialog dateTimeDialog = new DateTimePickerDialog(this, 
            (date, time, reminder, repeat) -> {
                // Update task data
                if (currentTask != null) {
                    currentTask.setDueDate(date);
                    if (!"Không".equals(time)) {
                        currentTask.setDueTime(time);
                    }
                    if (!"Không".equals(reminder)) {
                        currentTask.setReminderType(reminder);
                        currentTask.setHasReminder(!"Không".equals(reminder));
                    }
                    
                    // Set repeat information
                    if (!"Không".equals(repeat)) {
                        currentTask.setRepeatType(repeat);
                        currentTask.setRepeating(true);
                    } else {
                        currentTask.setRepeatType("Không");
                        currentTask.setRepeating(false);
                    }
                    
                    // Update UI
                    textDueDate.setText(date);
                    if (!"Không".equals(time)) {
                        textTime.setText(time);
                    } else {
                        textTime.setText("Không");
                    }
                    if (!"Không".equals(reminder)) {
                        textReminderValue.setText(reminder);
                    } else {
                        textReminderValue.setText("Không");
                    }
                    
                    // Save to database
                    new Thread(() -> {
                        database.todoDao().updateTask(currentTask);
                        runOnUiThread(() -> {
                            setResult(RESULT_OK);
                            Toast.makeText(this, "Đã cập nhật thời gian", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                }
            });
        
        // Set current values if exists
        if (currentTask != null) {
            dateTimeDialog.setInitialValues(
                currentTask.getDueDate(), 
                currentTask.getDueTime(), 
                currentTask.getReminderType(), 
                currentTask.getRepeatType() != null ? currentTask.getRepeatType() : "Không"
            );
        }
        
        dateTimeDialog.show();
    }
}
