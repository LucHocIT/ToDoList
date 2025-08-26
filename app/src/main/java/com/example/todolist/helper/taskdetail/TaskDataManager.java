package com.example.todolist.helper.taskdetail;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.service.TaskService;
import com.example.todolist.util.DateTimePickerDialog;

public class TaskDataManager implements TaskService.TaskUpdateListener {
    private AppCompatActivity activity;
    private TaskService taskService;
    private Task currentTask;
    
    // UI Components
    private EditText editDetailTitle;
    private EditText editDescription;
    private TextView textDueDate;
    private TextView textTime;
    private TextView textReminderValue;
    private TextView textPriorityValue;
    private TextView textPriorityLabel;
    private TextView textRepeatValue;

    public interface TaskUpdateCallback {
        void setResult(int resultCode);
        void onTaskLoaded(Task task);
        void showToast(String message);
        void finish();
        void onTaskCompletionChanged(boolean isCompleted);
    }

    private TaskUpdateCallback callback;

    public TaskDataManager(AppCompatActivity activity, TaskUpdateCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.taskService = new TaskService(activity, this);
    }

    public void initViews(EditText editDetailTitle, EditText editDescription, 
                         TextView textDueDate, TextView textTime, TextView textReminderValue,
                         TextView textPriorityValue, TextView textPriorityLabel, TextView textRepeatValue) {
        this.editDetailTitle = editDetailTitle;
        this.editDescription = editDescription;
        this.textDueDate = textDueDate;
        this.textTime = textTime;
        this.textReminderValue = textReminderValue;
        this.textPriorityValue = textPriorityValue;
        this.textPriorityLabel = textPriorityLabel;
        this.textRepeatValue = textRepeatValue;
        
        setupTextWatchers();
    }

    public void loadTaskData(String taskId) {
        if (taskId != null && !taskId.isEmpty()) {
            currentTask = taskService.getTaskByIdFromCache(taskId);
            if (currentTask != null) {
                displayTaskData();
                callback.onTaskLoaded(currentTask);
            } else {
                taskService.getTaskById(taskId, new BaseRepository.RepositoryCallback<Task>() {
                    @Override
                    public void onSuccess(Task task) {
                        currentTask = task;
                        activity.runOnUiThread(() -> {
                            displayTaskData();
                            callback.onTaskLoaded(currentTask);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        activity.runOnUiThread(() -> {
                            callback.showToast("Không tìm thấy task");
                            callback.finish();
                        });
                    }
                });
            }
        }
    }

    private void displayTaskData() {
        if (currentTask != null) {
            editDetailTitle.setText(currentTask.getTitle());
            editDescription.setText(currentTask.getDescription() != null ? currentTask.getDescription() : "");
            String formattedDate = formatDateDisplay(currentTask.getDueDate());
            textDueDate.setText(formattedDate != null ? formattedDate : "Không");
            textTime.setText(currentTask.getDueTime() != null ? currentTask.getDueTime() : "Không");
            textReminderValue.setText(currentTask.getReminder() != null ? currentTask.getReminder() : "Không");
            setPriorityDisplay(currentTask.getPriority());
            textRepeatValue.setText(currentTask.getRepeat() != null ? currentTask.getRepeat() : "Không");        
            updateCompletionStatus();
        }
    }

    private void setupTextWatchers() {
        editDetailTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (currentTask != null && !currentTask.isCompleted()) {
                    currentTask.setTitle(s.toString());
                    taskService.updateTask(currentTask);
                }
            }
        });

        editDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (currentTask != null && !currentTask.isCompleted()) {
                    currentTask.setDescription(s.toString());
                    taskService.updateTask(currentTask);
                }
            }
        });
    }

    public void showDateTimePicker() {
        if (currentTask != null && !currentTask.isCompleted()) {
            DateTimePickerDialog dialog = new DateTimePickerDialog(activity, new DateTimePickerDialog.OnDateTimeSelectedListener() {
                @Override
                public void onDateTimeSelected(String date, String time, String reminder, String repeat) {
                    currentTask.setDueDate(date);
                    currentTask.setDueTime(time);
                    
                    // Cập nhật reminder và repeat
                    if (reminder != null && !reminder.equals("Không")) {
                        currentTask.setReminder(reminder);
                        currentTask.setHasReminder(true);
                    } else {
                        currentTask.setReminder("Không");
                        currentTask.setHasReminder(false);
                    }
                    
                    if (repeat != null && !repeat.equals("Không")) {
                        currentTask.setRepeat(repeat);
                        currentTask.setIsRepeating(true);
                    } else {
                        currentTask.setRepeat("Không");
                        currentTask.setIsRepeating(false);
                    }
                    
                    // Cập nhật UI
                    textDueDate.setText(formatDateDisplay(date));
                    textTime.setText(time != null ? time : "Không");
                    textReminderValue.setText(reminder != null ? reminder : "Không");
                    textRepeatValue.setText(repeat != null ? repeat : "Không");
                    
                    taskService.updateTask(currentTask);
                    activity.runOnUiThread(() -> callback.setResult(AppCompatActivity.RESULT_OK));
                }
            });
            
            // Set initial values if available
            dialog.setInitialValues(
                currentTask.getDueDate(), 
                currentTask.getDueTime(), 
                currentTask.getReminder(), 
                currentTask.getRepeat()
            );
            
            dialog.show();
        }
    }

    public void updateTaskTitle() {
        if (currentTask != null && !currentTask.isCompleted()) {
            String newTitle = editDetailTitle.getText().toString().trim();
            if (!newTitle.isEmpty() && !newTitle.equals(currentTask.getTitle())) {
                currentTask.setTitle(newTitle);
                taskService.updateTask(currentTask);
                callback.setResult(AppCompatActivity.RESULT_OK);
            }
        }
    }

    public void updateTask(Task task) {
        this.currentTask = task;
        taskService.updateTask(task);
    }

    private void setPriorityDisplay(String priority) {
        // Cập nhật label luôn hiển thị "Độ ưu tiên"
        textPriorityLabel.setText("Độ ưu tiên");
        
        if (priority != null) {
            switch (priority.toLowerCase()) {
                case "cao":
                    textPriorityValue.setText("Cao");
                    textPriorityValue.setTextColor(activity.getResources().getColor(android.R.color.holo_red_dark));
                    break;
                case "trung bình":
                    textPriorityValue.setText("Trung bình");
                    textPriorityValue.setTextColor(activity.getResources().getColor(android.R.color.holo_orange_dark));
                    break;
                case "thấp":
                    textPriorityValue.setText("Thấp");
                    textPriorityValue.setTextColor(activity.getResources().getColor(android.R.color.holo_green_dark));
                    break;
                default:
                    textPriorityValue.setText("Không");
                    textPriorityValue.setTextColor(activity.getResources().getColor(android.R.color.darker_gray));
                    break;
            }
        } else {
            textPriorityValue.setText("Không");
            textPriorityValue.setTextColor(activity.getResources().getColor(android.R.color.darker_gray));
        }
    }
    
    private void updateCompletionStatus() {
        if (currentTask != null) {
            if (currentTask.isCompleted()) {
                // Làm mờ giao diện khi task đã hoàn thành
                editDetailTitle.setEnabled(false);
                editDetailTitle.setAlpha(0.6f);
                editDescription.setEnabled(false);
                editDescription.setAlpha(0.6f);
                
                // Thông báo cho Activity để disable thêm UI
                if (callback != null) {
                    callback.onTaskCompletionChanged(true);
                }
                
                // Hiển thị thông tin hoàn thành
                if (textPriorityLabel != null) {
                    textPriorityLabel.setText("Trạng thái");
                    textPriorityValue.setText("Đã hoàn thành");
                    textPriorityValue.setTextColor(activity.getResources().getColor(android.R.color.holo_green_dark));
                }
            } else {
                // Khôi phục giao diện bình thường
                editDetailTitle.setEnabled(true);
                editDetailTitle.setAlpha(1.0f);
                editDescription.setEnabled(true);
                editDescription.setAlpha(1.0f);
                
                // Thông báo cho Activity để enable UI
                if (callback != null) {
                    callback.onTaskCompletionChanged(false);
                }
                
                setPriorityDisplay(currentTask.getPriority());
            }
        }
    }

    private String formatDateDisplay(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return dateStr;
    }

    public Task getCurrentTask() {
        return currentTask;
    }
    
    public void deleteTask(String taskId) {
        if (currentTask != null && currentTask.getId().equals(taskId)) {
            taskService.deleteTask(currentTask);
        }
    }

    @Override
    public void onTasksUpdated() {
        // Handle task updates if needed
    }

    @Override
    public void onError(String error) {
        activity.runOnUiThread(() -> 
            callback.showToast("TaskService error: " + error)
        );
    }

    public void cleanup() {
        if (taskService != null) {
            taskService.cleanup();
        }
    }
}
