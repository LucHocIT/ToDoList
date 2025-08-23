package com.example.todolist;
import android.content.Context;
import android.content.res.Configuration;
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
import com.example.todolist.model.Category;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import com.example.todolist.service.CategoryService;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.util.DateTimePickerDialog;
import com.example.todolist.util.SettingsManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class TaskDetailActivity extends AppCompatActivity implements TaskService.TaskUpdateListener, CategoryService.CategoryUpdateListener {
    public static final String EXTRA_TASK_ID = "task_id";
    private EditText editDetailTitle;
    private TextView textDueDate;
    private TextView textTime;
    private TextView textReminderValue;
    private TextView textPriorityValue;
    private TextView textPriorityLabel;
    private TextView textRepeatValue;
    private Spinner spinnerCategory;
    private LinearLayout layoutDatePicker;
    private ImageView btnBack;
    private Task currentTask;
    private TaskService taskService;
    private CategoryService categoryService;
    private Category selectedCategory;
    private CategorySpinnerAdapter categoryAdapter;
    private List<Category> allCategories;
    private boolean isInitialCategorySetup = true; // Flag to prevent automatic updates during initial setup
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        // Initialize Firebase services
        taskService = new TaskService(this, this);
        categoryService = new CategoryService(this, null, this);
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
        textPriorityLabel = findViewById(R.id.text_priority_label);
        textRepeatValue = findViewById(R.id.text_repeat_value);
        spinnerCategory = findViewById(R.id.spinner_category);
        layoutDatePicker = findViewById(R.id.layout_date_picker);
        btnBack = findViewById(R.id.btn_back_detail);
        setupCategorySpinner();
    }
    private void loadTaskData() {
        String taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId != null && !taskId.isEmpty()) {
            // Load task from Firebase
            taskService.getTaskById(taskId, new BaseRepository.RepositoryCallback<Task>() {
                @Override
                public void onSuccess(Task task) {
                    currentTask = task;
                    runOnUiThread(() -> displayTaskData());
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(TaskDetailActivity.this, "Lỗi tải task: " + error, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });
        }
    }
    private void displayTaskData() {
        if (currentTask != null) {
            editDetailTitle.setText(currentTask.getTitle());
            // Format due date as dd/MM/yyyy
            String formattedDate = formatDateDisplay(currentTask.getDueDate());
            textDueDate.setText(formattedDate != null ? formattedDate : "Không");
            textTime.setText(currentTask.getDueTime() != null ? currentTask.getDueTime() : "Không");
            textReminderValue.setText(currentTask.getReminder() != null ? currentTask.getReminder() : "Không");
            // Set priority
            setPriorityDisplay(currentTask.getPriority());
            textRepeatValue.setText(currentTask.getRepeat() != null ? currentTask.getRepeat() : "Không");
            // Set category selection if available
            if (categoryAdapter != null && currentTask.getCategoryId() != null) {
                // Reset flag before setting category to avoid triggering updates
                isInitialCategorySetup = true;
                setSelectedCategoryInSpinner(currentTask.getCategoryId());
                isInitialCategorySetup = false;
            }
        }
    }
    private void setupCategorySpinner() {
        // Load categories from Firebase
        categoryService.getAllCategories(new BaseRepository.RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> categories) {
                allCategories = categories;
                runOnUiThread(() -> {
                    categoryAdapter = new CategorySpinnerAdapter(TaskDetailActivity.this, allCategories);
                    spinnerCategory.setAdapter(categoryAdapter);
                    // Set up selection listener
                    spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            // Skip automatic updates during initial setup
                            if (isInitialCategorySetup) {
                                return;
                            }
                            Category selectedCat = categoryAdapter.getCategory(position);
                            if (selectedCat != null && currentTask != null && !currentTask.isCompleted()) {
                                selectedCategory = selectedCat;
                                // Only update if category actually changed
                                String newCategoryId = "0".equals(selectedCat.getId()) ? null : selectedCat.getId();
                                String currentCategoryId = currentTask.getCategoryId();
                                // Compare category IDs properly (handle null cases)
                                boolean categoryChanged = (newCategoryId == null && currentCategoryId != null) ||
                                                        (newCategoryId != null && !newCategoryId.equals(currentCategoryId));
                                if (categoryChanged) {
                                    // Update task category
                                    currentTask.setCategoryId(newCategoryId);
                                    // Save to Firebase
                                    taskService.updateTask(currentTask, new com.example.todolist.repository.BaseRepository.DatabaseCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean result) {
                                            runOnUiThread(() -> {
                                                setResult(RESULT_OK);
                                                // Removed unnecessary success toast
                                            });
                                        }
                                        @Override
                                        public void onError(String error) {
                                            runOnUiThread(() ->
                                                Toast.makeText(TaskDetailActivity.this, "Lỗi cập nhật category: " + error, Toast.LENGTH_SHORT).show()
                                            );
                                        }
                                    });
                                }
                            }
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                    // Set selected category if task is loaded
                    if (currentTask != null && currentTask.getCategoryId() != null) {
                        setSelectedCategoryInSpinner(currentTask.getCategoryId());
                    }
                    // Enable category updates after initial setup is complete
                    isInitialCategorySetup = false;
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                    Toast.makeText(TaskDetailActivity.this, "Lỗi tải categories: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    private void setSelectedCategoryInSpinner(String categoryId) {
        if (categoryAdapter != null && allCategories != null) {
            for (int i = 0; i < allCategories.size(); i++) {
                if (allCategories.get(i).getId().equals(categoryId)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }
    }
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            updateTaskTitle();
            finish();
        });
        layoutDatePicker.setOnClickListener(v -> showDateTimePicker());
    }
    private void showDateTimePicker() {
        if (currentTask != null && !currentTask.isCompleted()) {
            DateTimePickerDialog dialog = new DateTimePickerDialog(this, new DateTimePickerDialog.OnDateTimeSelectedListener() {
                @Override
                public void onDateTimeSelected(String date, String time, String reminder, String repeat) {
                    currentTask.setDueDate(date);
                    currentTask.setDueTime(time);
                    // Update UI
                    textDueDate.setText(formatDateDisplay(date));
                    textTime.setText(time != null ? time : "KhĂ´ng");
                    // Save to Firebase
                    taskService.updateTask(currentTask, new com.example.todolist.repository.BaseRepository.DatabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            runOnUiThread(() -> setResult(RESULT_OK));
                        }
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                Toast.makeText(TaskDetailActivity.this, "Lỗi cập nhật ngày giờ: " + error, Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            });
            dialog.show();
        }
    }
    private void updateTaskTitle() {
        if (currentTask != null && !currentTask.isCompleted()) {
            String newTitle = editDetailTitle.getText().toString().trim();
            if (!newTitle.isEmpty() && !newTitle.equals(currentTask.getTitle())) {
                currentTask.setTitle(newTitle);
                // Save to Firebase
                taskService.updateTask(currentTask, new com.example.todolist.repository.BaseRepository.DatabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        setResult(RESULT_OK);
                    }
                    @Override
                    public void onError(String error) {
                        Toast.makeText(TaskDetailActivity.this, "Lỗi cập nhật title: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
    private void setPriorityDisplay(String priority) {
        if (priority != null) {
            switch (priority.toLowerCase()) {
                case "cao":
                    textPriorityValue.setText("Cao");
                    textPriorityValue.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    break;
                case "trung bĂ¬nh":
                    textPriorityValue.setText("Trung bĂ¬nh");
                    textPriorityValue.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    break;
                case "tháº¥p":
                    textPriorityValue.setText("Tháº¥p");
                    textPriorityValue.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    break;
                default:
                    textPriorityValue.setText("KhĂ´ng");
                    textPriorityValue.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    break;
            }
        } else {
            textPriorityValue.setText("Không");
            textPriorityValue.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }
    private String formatDateDisplay(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr; // Return original if parsing fails
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskService != null) {
            taskService.cleanup();
        }
        if (categoryService != null) {
            categoryService.cleanup();
        }
    }
    // TaskService.TaskUpdateListener implementation
    @Override
    public void onTasksUpdated() {
        // Handle task updates if needed
    }
    @Override
    public void onError(String error) {
        runOnUiThread(() -> 
            Toast.makeText(this, "TaskService error: " + error, Toast.LENGTH_SHORT).show()
        );
    }
    // CategoryService.CategoryUpdateListener implementation
    @Override
    public void onCategoriesUpdated() {
        // Handle category updates if needed
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }
    private Context updateBaseContextLocale(Context context) {
        String languageName = SettingsManager.getLanguage(context);
        String languageCode;
        if (languageName.equals("English")) {
            languageCode = "en";
        } else {
            languageCode = "vi";
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }
}
