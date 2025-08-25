package com.example.todolist;

import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.helper.taskdetail.AttachmentHandler;
import com.example.todolist.helper.taskdetail.CategoryHandler;
import com.example.todolist.helper.taskdetail.TaskDataManager;
import com.example.todolist.helper.taskdetail.UIHelper;
import com.example.todolist.model.Task;
public class TaskDetailActivity extends AppCompatActivity implements 
    AttachmentHandler.TaskUpdateCallback, 
    CategoryHandler.TaskUpdateCallback, 
    TaskDataManager.TaskUpdateCallback {
    
    public static final String EXTRA_TASK_ID = "task_id";
    
    private EditText editDetailTitle;
    private EditText editDescription;
    private TextView textDueDate;
    private TextView textTime;
    private TextView textReminderValue;
    private TextView textPriorityValue;
    private TextView textPriorityLabel;
    private TextView textRepeatValue;
    private Spinner spinnerCategory;
    private LinearLayout layoutDatePicker;
    private LinearLayout btnAddAttachment;
    private ImageView btnBack;
    private RecyclerView recyclerAttachments;
    private TextView textNoAttachments; 
    
    // Helper classes
    private TaskDataManager taskDataManager;
    private CategoryHandler categoryHandler;
    private AttachmentHandler attachmentHandler;
    private UIHelper uiHelper;
    
    private Task currentTask; 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        
        initViews();
        initHelpers();
        loadTaskData();
        setupClickListeners();
    }
    
    private void initViews() {
        editDetailTitle = findViewById(R.id.edit_detail_title);
        editDescription = findViewById(R.id.edit_description);
        textDueDate = findViewById(R.id.text_due_date);
        textTime = findViewById(R.id.text_time);
        textReminderValue = findViewById(R.id.text_reminder_value);
        textPriorityValue = findViewById(R.id.text_priority_value);
        textPriorityLabel = findViewById(R.id.text_priority_label);
        textRepeatValue = findViewById(R.id.text_repeat_value);
        spinnerCategory = findViewById(R.id.spinner_category);
        layoutDatePicker = findViewById(R.id.layout_date_picker);
        btnBack = findViewById(R.id.btn_back_detail);
        btnAddAttachment = findViewById(R.id.btn_add_attachment);
        recyclerAttachments = findViewById(R.id.recycler_attachments);
        textNoAttachments = findViewById(R.id.text_no_attachments);
    }
    
    private void initHelpers() {
        // Initialize helper classes
        taskDataManager = new TaskDataManager(this, this);
        taskDataManager.initViews(editDetailTitle, editDescription, textDueDate, textTime, 
                                 textReminderValue, textPriorityValue, textPriorityLabel, textRepeatValue);
        
        categoryHandler = new CategoryHandler(this, spinnerCategory, this);
        
        attachmentHandler = new AttachmentHandler(this, recyclerAttachments, textNoAttachments, this);
        
        uiHelper = new UIHelper(this);
        uiHelper.initViews(layoutDatePicker, spinnerCategory);
    }
    
    private void loadTaskData() {
        String taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        taskDataManager.loadTaskData(taskId);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            taskDataManager.updateTaskTitle();
            finish();
        });
        
        layoutDatePicker.setOnClickListener(v -> taskDataManager.showDateTimePicker());
        
        btnAddAttachment.setOnClickListener(v -> attachmentHandler.showFileTypeDialog());
    }
    
    @Override
    public void updateTask(Task task) {
        this.currentTask = task;
        taskDataManager.updateTask(task);
    }
    
    @Override
    public Task getCurrentTask() {
        return currentTask;
    }
    
    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    // Implementation of TaskDataManager.TaskUpdateCallback
    @Override
    public void onTaskLoaded(Task task) {
        this.currentTask = task;
        categoryHandler.updateCategorySelection();
        attachmentHandler.updateAttachmentView();
        uiHelper.updateCompletionStatus(task);
    }
    
    @Override
    public void finish() {
        super.finish();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        attachmentHandler.handlePermissionResult(requestCode, permissions, grantResults);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskDataManager != null) {
            taskDataManager.cleanup();
        }
        if (categoryHandler != null) {
            categoryHandler.cleanup();
        }
        if (attachmentHandler != null) {
            attachmentHandler.cleanup();
        }
    }
    
    @Override
    protected void attachBaseContext(Context newBase) {
        if (uiHelper == null) {
            uiHelper = new UIHelper(this);
        }
        super.attachBaseContext(uiHelper.updateBaseContextLocale(newBase));
    }
}
