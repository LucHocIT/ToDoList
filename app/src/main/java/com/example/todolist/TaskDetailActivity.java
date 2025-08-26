package com.example.todolist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.adapter.SubTaskAdapter;
import com.example.todolist.helper.taskdetail.AttachmentHandler;
import com.example.todolist.helper.taskdetail.CategoryHandler;
import com.example.todolist.helper.taskdetail.TaskDataManager;
import com.example.todolist.helper.taskdetail.UIHelper;
import com.example.todolist.helper.subtask.SubTaskManager;
import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;
import com.example.todolist.service.task.SubTaskService;
import com.example.todolist.service.TaskService;
import com.example.todolist.util.TimeSelectionDialog;
public class TaskDetailActivity extends AppCompatActivity implements 
    AttachmentHandler.TaskUpdateCallback, 
    CategoryHandler.TaskUpdateCallback, 
    TaskDataManager.TaskUpdateCallback,
    SubTaskManager.SubTaskManagerCallback,
    TaskService.TaskUpdateListener {
    
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
    private LinearLayout btnAddSubtask;
    private LinearLayout layoutNotes;
    private LinearLayout layoutAttachments;
    private ImageView btnBack;
    private ImageView btnMenuOptions;
    private RecyclerView recyclerAttachments;
    private RecyclerView recyclerSubTasks; 
    private TaskDataManager taskDataManager;
    private CategoryHandler categoryHandler;
    private AttachmentHandler attachmentHandler;
    private UIHelper uiHelper;
    private SubTaskManager subTaskManager;
    private TaskService taskService;
    
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
        btnMenuOptions = findViewById(R.id.btn_menu_options);
        btnAddSubtask = findViewById(R.id.btn_add_subtask);
        layoutNotes = findViewById(R.id.layout_notes);
        layoutAttachments = findViewById(R.id.layout_attachments);
        recyclerAttachments = findViewById(R.id.recycler_attachments);
        recyclerSubTasks = findViewById(R.id.recycler_subtasks);
    }
    
    private void initHelpers() {
        // Initialize helper classes
        taskDataManager = new TaskDataManager(this, this);
        taskDataManager.initViews(editDetailTitle, editDescription, textDueDate, textTime, 
                                 textReminderValue, textPriorityValue, textPriorityLabel, textRepeatValue);
        
        categoryHandler = new CategoryHandler(this, spinnerCategory, this);
        
        attachmentHandler = new AttachmentHandler(this, recyclerAttachments, null, this);
        
        taskService = new TaskService(this, this);

        subTaskManager = new SubTaskManager(this, recyclerSubTasks, taskDataManager, this);
        
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
        
        btnMenuOptions.setOnClickListener(v -> showOptionsMenu(v));
        
        layoutDatePicker.setOnClickListener(v -> taskDataManager.showDateTimePicker());
        
        layoutAttachments.setOnClickListener(v -> attachmentHandler.showFileTypeDialog());
        
        layoutNotes.setOnClickListener(v -> {
            // TODO: Implement notes functionality
            showToast("Chức năng ghi chú sẽ được phát triển");
        });
        
        btnAddSubtask.setOnClickListener(v -> subTaskManager.onAddNewSubTask());
    }
    
    @Override
    public void updateTask(Task task) {
        this.currentTask = task;
        taskDataManager.updateTask(task);
    }
    
    @Override
    public void onTaskUpdated(Task task) {
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

    @Override
    public void onTaskLoaded(Task task) {
        this.currentTask = task;
        categoryHandler.updateCategorySelection();
        attachmentHandler.updateAttachmentView();
        uiHelper.updateCompletionStatus(task);
        
        // Set task for subtask manager and load subtasks
        subTaskManager.setCurrentTask(task);
        subTaskManager.loadSubTasksFromDatabase();
    }
    
    @Override
    public void onTaskCompletionChanged(boolean isCompleted) {
        spinnerCategory.setEnabled(!isCompleted);
        spinnerCategory.setAlpha(isCompleted ? 0.6f : 1.0f);
        
        layoutDatePicker.setEnabled(!isCompleted);
        layoutDatePicker.setAlpha(isCompleted ? 0.6f : 1.0f);

        layoutAttachments.setEnabled(!isCompleted);
        layoutAttachments.setAlpha(isCompleted ? 0.6f : 1.0f);

        subTaskManager.onTaskCompletionChanged(isCompleted);
    }
    
    @Override
    public void finish() {
        subTaskManager.cleanupEmptySubTasks();
        super.finish();
    }
    
    private void showOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.task_detail_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_mark_as_done) {
                markTaskAsCompleted();
                return true;
            } else if (itemId == R.id.menu_start_focus) {
                startFocusSession();
                return true;
            } else if (itemId == R.id.menu_share) {
                shareTask();
                return true;
            } else if (itemId == R.id.menu_delete) {
                showDeleteConfirmation();
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void markTaskAsCompleted() {
        if (currentTask != null) {
            android.util.Log.d("TaskDetail", "Marking task as completed. Current subtasks count: " + 
                (currentTask.getSubTasks() != null ? currentTask.getSubTasks().size() : 0));
            
            currentTask.setCompleted(true);
            subTaskManager.markAllSubTasksAsCompleted();

            taskService.completeTask(currentTask, true);
            showToast(getString(R.string.task_marked_completed));
            finish();
        }
    }
    
    private void startFocusSession() {
        if (currentTask != null) {
            TimeSelectionDialog dialog = new TimeSelectionDialog(this, new TimeSelectionDialog.OnTimeSelectedListener() {
                @Override
                public void onTimeSelected(int minutes) {
                    Intent intent = new Intent(TaskDetailActivity.this, FocusActivity.class);
                    intent.putExtra(FocusActivity.EXTRA_TASK_TITLE, currentTask.getTitle());
                    intent.putExtra(FocusActivity.EXTRA_FOCUS_DURATION, minutes * 60 * 1000L);
                    startActivity(intent);
                }

                @Override
                public void onCancel() {
                    // User cancelled, do nothing
                }
            });
            dialog.show();
        }
    }
    
    private void shareTask() {
        if (currentTask != null) {
            String shareText = "Nhiệm vụ: " + currentTask.getTitle();
            if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
                shareText += "\nMô tả: " + currentTask.getDescription();
            }
            if (currentTask.getDueDate() != null) {
                shareText += "\nHạn: " + currentTask.getDueDate();
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_task_title)));
        }
    }
    
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.menu_delete))
                .setMessage(getString(R.string.confirm_delete_task))
                .setPositiveButton(getString(R.string.delete_button_text), (dialog, which) -> deleteTask())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
    
    private void deleteTask() {
        if (currentTask != null) {
            taskDataManager.deleteTask(currentTask.getId());
            showToast(getString(R.string.task_deleted));
            finish();
        }
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
    
    @Override
    public void onTasksUpdated() {
    }

    @Override
    public void onError(String error) {
        showToast("Lỗi: " + error);
    }
}
