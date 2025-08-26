package com.example.todolist.util;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.adapter.CategorySpinnerAdapter;
import com.example.todolist.adapter.SubTaskAdapter;
import com.example.todolist.model.Category;
import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import com.example.todolist.service.CategoryService;
import com.example.todolist.notification.ReminderScheduler;
import com.example.todolist.repository.BaseRepository;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
public class AddTaskHandler {
    private Context context;
    private TaskService taskService;
    private CategoryService categoryService;
    private OnTaskAddedListener listener;
    
    // SubTask management
    private List<SubTask> tempSubTasks;
    private SubTaskAdapter subTaskAdapter;
    private RecyclerView recyclerSubTasks;
    private LinearLayout layoutSubTaskSection;
    
    public interface OnTaskAddedListener {
        void onTaskAdded(Task task);
    }
    public AddTaskHandler(Context context, OnTaskAddedListener listener) {
        this.context = context;
        this.listener = listener;
        
        // Initialize SubTask list
        this.tempSubTasks = new ArrayList<>();
        
        // Initialize services
        this.taskService = new TaskService(context, new TaskService.TaskUpdateListener() {
            @Override
            public void onTasksUpdated() {
                // Handle in listener
            }
            @Override
            public void onError(String error) {
                // Handle error
            }
        });
        this.categoryService = new CategoryService(context, new CategoryService.CategoryUpdateListener() {
            @Override
            public void onCategoriesUpdated() {
                // Handle in listener
            }
            @Override
            public void onError(String error) {
                // Handle error  
            }
        });
    }
    public void showAddTaskDialog(String prefilledDate, String prefilledCategory) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        EditText editTaskTitle = dialogView.findViewById(R.id.edit_task_title);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_save);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category_dialog);
        ImageView iconCalendar = dialogView.findViewById(R.id.icon_calendar_dialog);
        
        // Setup SubTask UI
        ImageView iconSubTask = dialogView.findViewById(R.id.icon_subtask_dialog);
        layoutSubTaskSection = dialogView.findViewById(R.id.layout_subtask_section);
        recyclerSubTasks = dialogView.findViewById(R.id.recycler_subtasks_dialog);
        TextView btnAddSubTask = dialogView.findViewById(R.id.btn_add_subtask_dialog);
        
        // Initialize SubTask adapter
        setupSubTaskRecyclerView();
        
        // SubTask icon click listener
        iconSubTask.setOnClickListener(v -> {
            if (layoutSubTaskSection.getVisibility() == View.GONE) {
                layoutSubTaskSection.setVisibility(View.VISIBLE);
                iconSubTask.setRotation(45f); // Rotate to indicate active state
            } else {
                layoutSubTaskSection.setVisibility(View.GONE);
                iconSubTask.setRotation(0f);
            }
        });
        
        // Add SubTask button click listener
        btnAddSubTask.setOnClickListener(v -> {
            SubTask newSubTask = new SubTask();
            newSubTask.setTitle("");
            newSubTask.setId("temp_" + System.currentTimeMillis());
            tempSubTasks.add(newSubTask);
            subTaskAdapter.notifyItemInserted(tempSubTasks.size() - 1);
        });
        
        final String[] selectedDate = {prefilledDate};
        final String[] selectedTime = {context.getString(R.string.none)};
        final String[] selectedReminder = {context.getString(R.string.none)};
        final String[] selectedRepeat = {context.getString(R.string.none)};
        setupCategorySpinner(spinnerCategory, prefilledCategory);
        iconCalendar.setOnClickListener(v -> {
            showDateTimePickerDialog((date, time, reminder, repeat) -> {
                selectedDate[0] = date;
                selectedTime[0] = time;
                selectedReminder[0] = reminder;
                selectedRepeat[0] = repeat;
            });
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String title = editTaskTitle.getText().toString().trim();
            if (!title.isEmpty()) {
                String categoryName = getCategoryFromSpinner(spinnerCategory);
                createNewTaskWithDetails(title, categoryName, selectedDate[0], selectedTime[0], selectedReminder[0], selectedRepeat[0]);
                dialog.dismiss();
            } else {
                editTaskTitle.setError(context.getString(R.string.title_required));
            }
        });
        dialog.show();
    }
    public void showAddTaskDialog() {
        showAddTaskDialog(null, null);
    }
    public void showAddTaskDialog(String prefilledDate) {
        showAddTaskDialog(prefilledDate, null);
    }
    
    private void setupSubTaskRecyclerView() {
        // Clear existing subtasks for new dialog
        tempSubTasks.clear();
        
        // Initialize adapter with SubTask listener
        SubTaskAdapter.OnSubTaskListener subTaskListener = new SubTaskAdapter.OnSubTaskListener() {
            @Override
            public void onSubTaskStatusChanged(SubTask subTask, boolean isCompleted) {
                subTask.setCompleted(isCompleted);
            }

            @Override
            public void onSubTaskTextChanged(SubTask subTask, String newText) {
                subTask.setTitle(newText);
            }

            @Override
            public void onSubTaskDeleted(SubTask subTask) {
                int position = tempSubTasks.indexOf(subTask);
                if (position != -1) {
                    tempSubTasks.remove(position);
                    subTaskAdapter.notifyItemRemoved(position);
                }
            }

            @Override
            public void onAddNewSubTask() {
                // This won't be used in dialog, we handle it with button
            }
        };
        
        subTaskAdapter = new SubTaskAdapter(tempSubTasks, subTaskListener);
        recyclerSubTasks.setLayoutManager(new LinearLayoutManager(context));
        recyclerSubTasks.setAdapter(subTaskAdapter);
    }
    
    private void setupCategorySpinner(Spinner spinner, String prefilledCategory) {
        // Get categories from CategoryService
        categoryService.getAllCategories(new BaseRepository.ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categories) {
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> {
                        CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(context, categories);
                        spinner.setAdapter(adapter);
                        if (prefilledCategory != null) {
                            for (int i = 0; i < categories.size(); i++) {
                                if (categories.get(i).getName().equals(prefilledCategory)) {
                                    spinner.setSelection(i);
                                    break;
                                }
                            }
                        }
                    });
                }
            }
            @Override
            public void onError(String error) {
                // Fallback to empty list
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> {
                        CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(context, new ArrayList<>());
                        spinner.setAdapter(adapter);
                    });
                }
            }
        });
    }
    private String getCategoryFromSpinner(Spinner spinner) {
        try {
            CategorySpinnerAdapter adapter = (CategorySpinnerAdapter) spinner.getAdapter();
            Category selectedCategory = adapter.getCategory(spinner.getSelectedItemPosition());
            if (selectedCategory != null) {
                if ("0".equals(selectedCategory.getId()) ||
                        selectedCategory.getName().equalsIgnoreCase(context.getString(R.string.no_category))) {
                    return null;
                }
                return selectedCategory.getName();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    private void showDateTimePickerDialog(DateTimePickerDialog.OnDateTimeSelectedListener listener) {
        DateTimePickerDialog dialog = new DateTimePickerDialog(context, listener);
        dialog.show();
    }
    private void createNewTaskWithDetails(String title, String category, String date, String time, String reminder, String repeat) {
        String finalDate;
        String finalTime = null;
        if (date != null && !date.equals(context.getString(R.string.none))) {
            finalDate = date;
        } else {
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH) + 1;
            int year = calendar.get(Calendar.YEAR);
            finalDate = String.format("%02d/%02d/%04d", day, month, year);
        }
        if (time != null && !time.equals(context.getString(R.string.none)) && !time.equals("12:00")) {
            finalTime = time;
        }
        // Create new Task using Firebase model
        Task newTask = new Task();
        String taskId = String.valueOf(System.currentTimeMillis()) + "_" + Math.random();
        newTask.setId(taskId);
        
        newTask.setTitle(title);
        newTask.setDescription("");
        newTask.setDueDate(finalDate);
        newTask.setDueTime(finalTime);
        newTask.setCategory(category);
        if (reminder != null && !reminder.equals(context.getString(R.string.none)) && finalTime != null) {
            newTask.setHasReminder(true);
            newTask.setReminderType(reminder);
        }
        if (repeat != null && !repeat.equals(context.getString(R.string.none))) {
            newTask.setRepeating(true);
            newTask.setRepeatType(repeat);
        }
        
        // Add SubTasks to the new task
        if (!tempSubTasks.isEmpty()) {
            List<SubTask> validSubTasks = new ArrayList<>();
            for (SubTask subTask : tempSubTasks) {
                if (subTask.getTitle() != null && !subTask.getTitle().trim().isEmpty()) {
                    subTask.setTaskId(taskId);
                    if (subTask.getId() == null || subTask.getId().startsWith("temp_")) {
                        subTask.setId(taskId + "_subtask_" + System.currentTimeMillis() + "_" + Math.random());
                    }
                    validSubTasks.add(subTask);
                }
            }
            if (!validSubTasks.isEmpty()) {
                newTask.setSubTasks(validSubTasks);
            }
        }
        
        // Use TaskService to add task
        taskService.addTask(newTask, new TaskService.TaskOperationCallback() {
            @Override
            public void onSuccess() {
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onTaskAdded(newTask);
                        }
                        // Clear tempSubTasks sau khi lưu thành công
                        tempSubTasks.clear();
                        // Removed unnecessary success toast
                    });
                }
            }
            @Override
            public void onError(String error) {
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Lỗi thêm task: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
