package com.example.todolist.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.todolist.R;
import com.example.todolist.adapter.CategorySpinnerAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Category;
import com.example.todolist.model.TodoTask;
import com.example.todolist.notification.ReminderScheduler;

import java.util.Calendar;
import java.util.List;

public class AddTaskHandler {

    private Context context;
    private TodoDatabase database;
    private OnTaskAddedListener listener;

    public interface OnTaskAddedListener {
        void onTaskAdded(TodoTask task);
    }

    public AddTaskHandler(Context context, OnTaskAddedListener listener) {
        this.context = context;
        this.database = TodoDatabase.getInstance(context);
        this.listener = listener;
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

    private void setupCategorySpinner(Spinner spinner, String prefilledCategory) {
        new Thread(() -> {
            List<Category> categories = database.categoryDao().getAllCategories();
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
        }).start();
    }

    private String getCategoryFromSpinner(Spinner spinner) {
        try {
            CategorySpinnerAdapter adapter = (CategorySpinnerAdapter) spinner.getAdapter();
            Category selectedCategory = adapter.getCategory(spinner.getSelectedItemPosition());

            if (selectedCategory != null) {
                if (selectedCategory.getId() == 0 ||
                        selectedCategory.getName().equalsIgnoreCase(context.getString(R.string.no_category))) {
                    return null;
                }
                return selectedCategory.getName();
            }
            return null;
        } catch (Exception e) {
            Log.e("AddTaskHandler", "Error getting category from spinner: " + e.getMessage());
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

        // Truyền context vào constructor TodoTask
        TodoTask newTask = new TodoTask(context, title, "", finalDate, finalTime);
        newTask.setCategory(category);

        if (reminder != null && !reminder.equals(context.getString(R.string.none)) && finalTime != null) {
            newTask.setHasReminder(true);
            newTask.setReminderType(reminder);
        }

        if (repeat != null && !repeat.equals(context.getString(R.string.none))) {
            newTask.setRepeating(true);
            newTask.setRepeatType(repeat);
        }

        new Thread(() -> {
            database.todoDao().insertTask(newTask);

            if (newTask.isHasReminder()) {
                ReminderScheduler scheduler = new ReminderScheduler(context);
                scheduler.scheduleTaskReminder(newTask);
            }

            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onTaskAdded(newTask);
                    }
                    Toast.makeText(context, context.getString(R.string.task_added), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
