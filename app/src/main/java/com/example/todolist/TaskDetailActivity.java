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
    private boolean isInitialCategorySetup = true; 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        isInitialCategorySetup = true;    
        taskService = new TaskService(this, this);
        categoryService = new CategoryService(this, this);
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
            String formattedDate = formatDateDisplay(currentTask.getDueDate());
            textDueDate.setText(formattedDate != null ? formattedDate : "Không");
            textTime.setText(currentTask.getDueTime() != null ? currentTask.getDueTime() : "Không");
            textReminderValue.setText(currentTask.getReminder() != null ? currentTask.getReminder() : "Không");
            setPriorityDisplay(currentTask.getPriority());
            textRepeatValue.setText(currentTask.getRepeat() != null ? currentTask.getRepeat() : "Không");        
            android.util.Log.d("TaskDetail", "Displaying task: " + currentTask.getTitle() + ", categoryId: " + currentTask.getCategoryId());
            if (categoryAdapter != null) {
                setSelectedCategoryInSpinner(currentTask.getCategoryId());
            }
        }
    }
    private void setupCategorySpinner() {
        categoryService.getAllCategories(new BaseRepository.RepositoryCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> categories) {
                allCategories = categories;
                runOnUiThread(() -> {
                    categoryAdapter = new CategorySpinnerAdapter(TaskDetailActivity.this, allCategories);
                    spinnerCategory.setAdapter(categoryAdapter);
                    spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            android.util.Log.d("TaskDetail", "onItemSelected called - position: " + position + ", isInitialSetup: " + isInitialCategorySetup);
                            
                            if (isInitialCategorySetup) {
                                android.util.Log.d("TaskDetail", "Skipping onItemSelected because isInitialCategorySetup = true");
                                return;
                            }
                            
                            Category selectedCat = categoryAdapter.getCategory(position);
                            android.util.Log.d("TaskDetail", "Selected category: " + (selectedCat != null ? selectedCat.getName() + " (id: " + selectedCat.getId() + ")" : "null"));
                            
                            if (selectedCat != null && currentTask != null && !currentTask.isCompleted()) {
                                selectedCategory = selectedCat;
                                String newCategoryId = "0".equals(selectedCat.getId()) ? null : selectedCat.getId();
                                String currentCategoryId = currentTask.getCategoryId();
                                
                                android.util.Log.d("TaskDetail", "Category comparison - current: " + currentCategoryId + ", new: " + newCategoryId);
                                
                                boolean categoryChanged = (newCategoryId == null && currentCategoryId != null) ||
                                                        (newCategoryId != null && !newCategoryId.equals(currentCategoryId));
                                
                                android.util.Log.d("TaskDetail", "Category changed: " + categoryChanged);
                                
                                if (categoryChanged) {
                                    android.util.Log.d("TaskDetail", "Updating task category from " + currentCategoryId + " to " + newCategoryId);
                                    currentTask.setCategoryId(newCategoryId);
                                    taskService.updateTask(currentTask, new com.example.todolist.repository.BaseRepository.DatabaseCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean result) {
                                            runOnUiThread(() -> {
                                                setResult(RESULT_OK);
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

                    if (currentTask != null) {
                        setSelectedCategoryInSpinner(currentTask.getCategoryId());
                    }
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
        if (categoryAdapter != null) {
            android.util.Log.d("TaskDetail", "setSelectedCategoryInSpinner called with categoryId: " + categoryId + ", isInitialSetup: " + isInitialCategorySetup);            
            isInitialCategorySetup = true;
            
            int positionToSelect = 0; 
            for (int i = 0; i < categoryAdapter.getCount(); i++) {
                Category category = categoryAdapter.getCategory(i);
                if (category != null) {
                    if (categoryId == null && "0".equals(category.getId())) {
                        positionToSelect = i;
                        android.util.Log.d("TaskDetail", "Task has no category, selecting default at position: " + i);
                        break;
                    }
                    else if (categoryId != null && category.getId().equals(categoryId)) {
                        positionToSelect = i;
                        android.util.Log.d("TaskDetail", "Found category at position: " + i + ", category: " + category.getName());
                        break;
                    }
                }
            }                   
            
            android.util.Log.d("TaskDetail", "Setting spinner selection to position: " + positionToSelect);
            spinnerCategory.setSelection(positionToSelect);
            spinnerCategory.post(() -> {
                isInitialCategorySetup = false;
                android.util.Log.d("TaskDetail", "isInitialCategorySetup reset to false");
            });
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
                    textDueDate.setText(formatDateDisplay(date));
                    textTime.setText(time != null ? time : "Không");
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
                case "trung bình":
                    textPriorityValue.setText("Trung bình");
                    textPriorityValue.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    break;
                case "thấp":
                    textPriorityValue.setText("Thấp");
                    textPriorityValue.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    break;
                default:
                    textPriorityValue.setText("Không");
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
        return dateStr;
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

    @Override
    public void onTasksUpdated() {

    }
    @Override
    public void onError(String error) {
        runOnUiThread(() -> 
            Toast.makeText(this, "TaskService error: " + error, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onCategoriesUpdated() {

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
