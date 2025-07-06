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
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Category;
import com.example.todolist.model.TodoTask;
import com.example.todolist.notification.ReminderScheduler;
import com.example.todolist.util.DateTimePickerDialog;
import com.example.todolist.util.SettingsManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {

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
        textPriorityLabel = findViewById(R.id.text_priority_label);
        textRepeatValue = findViewById(R.id.text_repeat_value);
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
                runOnUiThread(this::displayTaskData);
            }).start();
        }
    }

    private void displayTaskData() {
        if (currentTask != null) {
            editDetailTitle.setText(currentTask.getTitle());

            // Format due date as dd/MM/yyyy
            String formattedDate = formatDateDisplay(currentTask.getDueDate());
            // Sử dụng tài nguyên chuỗi cho "Không"
            textDueDate.setText(formattedDate != null ? formattedDate : getString(R.string.none));

            // Sử dụng tài nguyên chuỗi cho "Không"
            textTime.setText(currentTask.getDueTime() != null ? currentTask.getDueTime() : getString(R.string.none));
            // Sử dụng tài nguyên chuỗi cho "Không"
            textReminderValue.setText(currentTask.getReminderType() != null ? currentTask.getReminderType() : getString(R.string.none));

            // Set priority/status based on completion status
            if (currentTask.isCompleted()) {
                // Hiển thị trạng thái cho task đã hoàn thành
                // Sử dụng tài nguyên chuỗi cho "Trạng thái"
                textPriorityLabel.setText(getString(R.string.status_label));
                // Sử dụng tài nguyên chuỗi cho "Đã hoàn thành"
                textPriorityValue.setText(getString(R.string.status_completed));
            } else {
                // Hiển thị độ ưu tiên cho task chưa hoàn thành
                // Sử dụng tài nguyên chuỗi cho "Độ ưu tiên"
                textPriorityLabel.setText(getString(R.string.priority_label));
                // Sử dụng tài nguyên chuỗi cho "Có" và "Không"
                textPriorityValue.setText(currentTask.isImportant() ? getString(R.string.yes) : getString(R.string.none));
            }

            // Set repeat information
            // Sửa chuỗi cứng "Không có" thành tài nguyên chuỗi
            textRepeatValue.setText(currentTask.getRepeatType() != null ? currentTask.getRepeatType() : getString(R.string.none));

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

            // Disable editing if task is completed
            if (currentTask.isCompleted()) {
                disableEditingForCompletedTask();
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
                        if (selectedCat != null && currentTask != null && !currentTask.isCompleted()) {
                            selectedCategory = selectedCat;
                            // Update task category
                            if (selectedCat.getId() == 0) {
                                currentTask.setCategory(null); // "không có thể loại" - CHỈ LÀ COMMENT, KHÔNG HIỂN THỊ
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

        // Date picker click listener - only if task is not completed
        layoutDatePicker.setOnClickListener(v -> {
            if (currentTask != null && !currentTask.isCompleted()) {
                showDateTimePicker();
            }
        });
    }

    private void showDateTimePicker() {
        DateTimePickerDialog dateTimeDialog = new DateTimePickerDialog(this,
                (date, time, reminder, repeat) -> {
                    // Update task data
                    if (currentTask != null) {
                        currentTask.setDueDate(date);
                        // Sửa chuỗi cứng "Không" thành tài nguyên chuỗi
                        if (!getString(R.string.none).equals(time)) {
                            currentTask.setDueTime(time);
                        } else { // Nếu là "Không" thì đặt lại về null để tránh lưu chuỗi "Không" vào DB
                            currentTask.setDueTime(null);
                        }

                        // Always update reminder, whether it's set to a value or "Không"
                        currentTask.setReminderType(reminder);
                        // Sửa chuỗi cứng "Không" thành tài nguyên chuỗi
                        currentTask.setHasReminder(!getString(R.string.none).equals(reminder));

                        // Set repeat information
                        // Sửa chuỗi cứng "Không" thành tài nguyên chuỗi
                        if (!getString(R.string.none).equals(repeat)) {
                            currentTask.setRepeatType(repeat);
                            currentTask.setRepeating(true);
                        } else {
                            // Sửa chuỗi cứng "Không" thành tài nguyên chuỗi
                            currentTask.setRepeatType(getString(R.string.none));
                            currentTask.setRepeating(false);
                        }

                        // Update UI - format date properly
                        String formattedDate = formatDateDisplay(date);
                        // Sửa chuỗi cứng "Không" thành tài nguyên chuỗi
                        textDueDate.setText(formattedDate != null ? formattedDate : getString(R.string.none));
                        // Sửa chuỗi cứng "Không" thành tài nguyên chuỗi
                        if (!getString(R.string.none).equals(time)) {
                            textTime.setText(time);
                        } else {
                            // Sửa chuỗi cứng "Không" thành tài nguyên chuỗi
                            textTime.setText(getString(R.string.none));
                        }

                        // Always update reminder display
                        textReminderValue.setText(reminder);

                        // Sửa Repeat display
                        textRepeatValue.setText(repeat);


                        // Save to database
                        new Thread(() -> {
                            database.todoDao().updateTask(currentTask);

                            // Update reminder scheduling
                            ReminderScheduler scheduler = new ReminderScheduler(TaskDetailActivity.this);

                            // Cancel existing reminders for this task
                            scheduler.cancelTaskReminders(currentTask.getId());

                            // Schedule new reminders if task has reminder
                            if (currentTask.isHasReminder()) {
                                scheduler.scheduleTaskReminder(currentTask);
                            }

                            runOnUiThread(() -> {
                                setResult(RESULT_OK);
                                Toast.makeText(TaskDetailActivity.this, getString(R.string.time_updated_toast), Toast.LENGTH_SHORT).show();
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
                    // Sửa chuỗi cứng "Không" thành tài nguyên chuỗi
                    currentTask.getRepeatType() != null ? currentTask.getRepeatType() : getString(R.string.none)
            );
        }

        dateTimeDialog.show();
    }

    private void disableEditingForCompletedTask() {
        // Disable title editing
        editDetailTitle.setEnabled(false);
        editDetailTitle.setTextColor(getColor(R.color.gray_text)); // Sử dụng R.color.gray_text

        // Disable category spinner
        spinnerCategory.setEnabled(false);

        // Make date picker non-clickable
        layoutDatePicker.setClickable(false);
        layoutDatePicker.setAlpha(0.6f);

        // Change priority text to show "Đã hoàn thành"
        // Sử dụng tài nguyên chuỗi cho "Đã hoàn thành"
        textPriorityValue.setText(getString(R.string.status_completed));
        textPriorityValue.setTextColor(getColor(R.color.green_success)); // Sử dụng R.color.green_success
    }

    private String formatDateDisplay(String dateStr) {
        // Sửa chuỗi cứng "Không" thành tài nguyên chuỗi
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.equals("null") || dateStr.equals(getString(R.string.none))) {
            return null;
        }

        try {
            // Parse from yyyy/MM/dd format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);

            // Format to dd/MM/yyyy
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr; // Return original if parsing fails
        }
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