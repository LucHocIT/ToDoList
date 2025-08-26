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
import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;
import com.example.todolist.service.task.SubTaskService;
import com.example.todolist.service.TaskService;
import com.example.todolist.util.TimeSelectionDialog;
public class TaskDetailActivity extends AppCompatActivity implements 
    AttachmentHandler.TaskUpdateCallback, 
    CategoryHandler.TaskUpdateCallback, 
    TaskDataManager.TaskUpdateCallback,
    SubTaskAdapter.OnSubTaskListener,
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
    private LinearLayout btnAddSubTaskHeader;
    private ImageView btnBack;
    private ImageView btnMenuOptions;
    private RecyclerView recyclerAttachments;
    private RecyclerView recyclerSubTasks;
    private TextView textNoAttachments; 
    
    // Helper classes
    private TaskDataManager taskDataManager;
    private CategoryHandler categoryHandler;
    private AttachmentHandler attachmentHandler;
    private UIHelper uiHelper;
    private SubTaskAdapter subTaskAdapter;
    private SubTaskService subTaskService;
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
        btnAddAttachment = findViewById(R.id.btn_add_attachment);
        recyclerAttachments = findViewById(R.id.recycler_attachments);
        recyclerSubTasks = findViewById(R.id.recycler_subtasks);
        textNoAttachments = findViewById(R.id.text_no_attachments);
    }
    
    private void initHelpers() {
        // Initialize helper classes
        taskDataManager = new TaskDataManager(this, this);
        taskDataManager.initViews(editDetailTitle, editDescription, textDueDate, textTime, 
                                 textReminderValue, textPriorityValue, textPriorityLabel, textRepeatValue);
        
        categoryHandler = new CategoryHandler(this, spinnerCategory, this);
        
        attachmentHandler = new AttachmentHandler(this, recyclerAttachments, textNoAttachments, this);
        
        subTaskService = new SubTaskService(this);
        taskService = new TaskService(this, this);
        
        // Initialize subtask adapter
        setupSubTaskRecyclerView();
        
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
        
        // Load subtasks from Firebase
        if (task.getId() != null) {
            subTaskService.getSubTasks(task.getId(), new com.example.todolist.repository.BaseRepository.ListCallback<SubTask>() {
                @Override
                public void onSuccess(java.util.List<SubTask> subTasks) {
                    runOnUiThread(() -> {
                        task.setSubTasks(subTasks);
                        refreshSubTasks();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        // If no subtasks found, just refresh with empty list
                        if (task.getSubTasks() == null) {
                            task.setSubTasks(new java.util.ArrayList<>());
                        }
                        refreshSubTasks();
                    });
                }
            });
        } else {
            refreshSubTasks();
        }
    }
    
    @Override
    public void onTaskCompletionChanged(boolean isCompleted) {
        // Vô hiệu hóa/kích hoạt các UI components
        spinnerCategory.setEnabled(!isCompleted);
        spinnerCategory.setAlpha(isCompleted ? 0.6f : 1.0f);
        
        layoutDatePicker.setEnabled(!isCompleted);
        layoutDatePicker.setAlpha(isCompleted ? 0.6f : 1.0f);
        
        btnAddAttachment.setEnabled(!isCompleted);
        btnAddAttachment.setAlpha(isCompleted ? 0.6f : 1.0f);
        
        // Disable subtasks khi task hoàn thành
        if (recyclerSubTasks != null) {
            recyclerSubTasks.setEnabled(!isCompleted);
            recyclerSubTasks.setAlpha(isCompleted ? 0.6f : 1.0f);
        }
        if (isCompleted && currentTask != null && currentTask.getSubTasks() != null) {
            for (SubTask subTask : currentTask.getSubTasks()) {
                if (!subTask.isCompleted()) {
                    subTask.setCompleted(true);
                    // Update từng subtask trong Firebase
                    subTaskService.updateSubTask(currentTask.getId(), subTask, null);
                }
            }
        }
        
        // Refresh subtasks để cập nhật UI
        if (subTaskAdapter != null) {
            subTaskAdapter.setTaskCompleted(isCompleted);
            subTaskAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public void finish() {
        // Cleanup subtasks rỗng trước khi thoát
        if (currentTask != null && currentTask.getSubTasks() != null) {
            java.util.List<SubTask> emptySubTasks = new java.util.ArrayList<>();
            for (SubTask subTask : currentTask.getSubTasks()) {
                if (subTask.getTitle() == null || subTask.getTitle().trim().isEmpty()) {
                    emptySubTasks.add(subTask);
                }
            }
            
            // Xóa các subtasks rỗng
            for (SubTask emptySubTask : emptySubTasks) {
                subTaskService.deleteSubTask(currentTask.getId(), emptySubTask.getId(), null);
                currentTask.removeSubTask(emptySubTask);
            }
            
            if (!emptySubTasks.isEmpty()) {
                taskDataManager.updateTask(currentTask);
            }
        }
        
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
            // Log để debug
            android.util.Log.d("TaskDetail", "Marking task as completed. Current subtasks count: " + 
                (currentTask.getSubTasks() != null ? currentTask.getSubTasks().size() : 0));
            
            currentTask.setCompleted(true);
            if (currentTask.getSubTasks() != null && !currentTask.getSubTasks().isEmpty()) {
                android.util.Log.d("TaskDetail", "Auto-completing " + currentTask.getSubTasks().size() + " subtasks");
                
                for (SubTask subTask : currentTask.getSubTasks()) {
                    if (!subTask.isCompleted()) {
                        android.util.Log.d("TaskDetail", "Completing subtask: " + subTask.getTitle());
                        subTask.setCompleted(true);
                        // Update từng subtask trong Firebase
                        subTaskService.updateSubTask(currentTask.getId(), subTask, null);
                    }
                }
                
                // Refresh UI để hiển thị subtasks đã completed
                runOnUiThread(() -> refreshSubTasks());
            }
            
            // Sử dụng taskService.completeTask thay vì taskDataManager.updateTask
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
    
    // SubTask methods
    private void setupSubTaskRecyclerView() {
        subTaskAdapter = new SubTaskAdapter(
            currentTask != null ? currentTask.getSubTasks() : new java.util.ArrayList<>(), 
            this
        );
        if (currentTask != null) {
            subTaskAdapter.setTaskCompleted(currentTask.isCompleted());
        }
        recyclerSubTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerSubTasks.setAdapter(subTaskAdapter);
    }
    
    @Override
    public void onSubTaskStatusChanged(SubTask subTask, boolean isCompleted) {
        subTask.setCompleted(isCompleted);
        if (currentTask != null) {
            subTaskService.updateSubTask(currentTask.getId(), subTask, new SubTaskService.SubTaskOperationCallback() {
                @Override
                public void onSuccess() {
                    // Then update task
                    taskDataManager.updateTask(currentTask);
                    runOnUiThread(() -> subTaskAdapter.notifyDataSetChanged());
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(TaskDetailActivity.this, "Lỗi cập nhật subtask: " + error, Toast.LENGTH_SHORT).show();
                        // Revert changes
                        subTask.setCompleted(!isCompleted);
                        subTaskAdapter.notifyDataSetChanged();
                    });
                }
            });
        }
    }
    
    @Override
    public void onSubTaskTextChanged(SubTask subTask, String newText) {
        if (!newText.isEmpty()) {
            subTask.setTitle(newText);
            if (currentTask != null) {
                subTaskService.saveSubTask(currentTask.getId(), subTask, new SubTaskService.SubTaskOperationCallback() {
                    @Override
                    public void onSuccess() {
                        // Then update task
                        taskDataManager.updateTask(currentTask);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(TaskDetailActivity.this, "Lỗi cập nhật subtask: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        } else {
            // Nếu text rỗng, xóa subtask
            onSubTaskDeleted(subTask);
        }
    }
    
    @Override
    public void onSubTaskDeleted(SubTask subTask) {
        if (currentTask != null) {
            // Delete from Firebase first
            subTaskService.deleteSubTask(currentTask.getId(), subTask.getId(), new SubTaskService.SubTaskOperationCallback() {
                @Override
                public void onSuccess() {
                    // Then remove from local task
                    currentTask.removeSubTask(subTask);
                    taskDataManager.updateTask(currentTask);
                    runOnUiThread(() -> subTaskAdapter.notifyDataSetChanged());
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(TaskDetailActivity.this, "Lỗi xóa subtask: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }
    
    @Override
    public void onAddNewSubTask() {
        if (currentTask != null) {
            SubTask newSubTask = new SubTask("", currentTask.getId());
            newSubTask.setId(java.util.UUID.randomUUID().toString());
            currentTask.addSubTask(newSubTask);
            runOnUiThread(() -> subTaskAdapter.notifyDataSetChanged());
        }
    }
    
    private void refreshSubTasks() {
        if (subTaskAdapter != null && currentTask != null) {
            subTaskAdapter = new SubTaskAdapter(currentTask.getSubTasks(), this);
            subTaskAdapter.setTaskCompleted(currentTask.isCompleted());
            recyclerSubTasks.setAdapter(subTaskAdapter);
        }
    }

    // TaskService.TaskUpdateListener methods
    @Override
    public void onTasksUpdated() {
        // Không cần làm gì vì đây là detail activity, không quản lý danh sách tasks
    }

    @Override
    public void onError(String error) {
        showToast("Lỗi: " + error);
    }
}
