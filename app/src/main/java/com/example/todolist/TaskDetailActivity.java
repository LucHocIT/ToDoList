package com.example.todolist;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.adapter.AttachmentAdapter;
import com.example.todolist.adapter.CategorySpinnerAdapter;
import com.example.todolist.model.Attachment;
import com.example.todolist.model.Category;
import com.example.todolist.model.Task;
import com.example.todolist.service.AttachmentService;
import com.example.todolist.service.TaskService;
import com.example.todolist.service.CategoryService;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.util.DateTimePickerDialog;
import com.example.todolist.util.SettingsManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class TaskDetailActivity extends AppCompatActivity implements TaskService.TaskUpdateListener, CategoryService.CategoryUpdateListener, AttachmentAdapter.OnAttachmentActionListener {
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
    private Task currentTask;
    private TaskService taskService;
    private CategoryService categoryService;
    private AttachmentService attachmentService;
    private Category selectedCategory;
    private CategorySpinnerAdapter categoryAdapter;
    private List<Category> allCategories;
    private boolean isInitialCategorySetup = true;
    private AttachmentAdapter attachmentAdapter;
    private ActivityResultLauncher<Intent> filePickerLauncher; 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        isInitialCategorySetup = true;    
        taskService = new TaskService(this, this);
        categoryService = new CategoryService(this, this);
        attachmentService = new AttachmentService(this);
        initFilePickerLauncher();
        initViews();
        loadTaskData();
        setupClickListeners();
    }
    
    private void initFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        handleSelectedFile(fileUri);
                    }
                }
            }
        );
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
        
        setupCategorySpinner();
        setupAttachmentRecyclerView();
        setupTextWatchers();
    }
    private void loadTaskData() {
        String taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId != null && !taskId.isEmpty()) {
            currentTask = taskService.getTaskByIdFromCache(taskId);
            if (currentTask != null) {
                displayTaskData();
            } else {
                taskService.getTaskById(taskId, new BaseRepository.RepositoryCallback<Task>() {
                    @Override
                    public void onSuccess(Task task) {
                        currentTask = task;
                        runOnUiThread(() -> displayTaskData());
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(TaskDetailActivity.this, "Không tìm thấy task", Toast.LENGTH_SHORT).show();
                            finish();
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
            updateAttachmentView();
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

                                    taskService.updateTask(currentTask);
                                    runOnUiThread(() -> {
                                        setResult(RESULT_OK);
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
        
        btnAddAttachment.setOnClickListener(v -> {
            if (currentTask != null && !currentTask.isCompleted()) {
                openFilePicker();
            } else {
                Toast.makeText(this, "Không thể thêm tệp tin vào nhiệm vụ đã hoàn thành", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Chọn tệp tin"));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng quản lý tệp tin", Toast.LENGTH_SHORT).show();
        }
    }
    private void showDateTimePicker() {
        if (currentTask != null && !currentTask.isCompleted()) {
            DateTimePickerDialog dialog = new DateTimePickerDialog(this, new DateTimePickerDialog.OnDateTimeSelectedListener() {
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
                    runOnUiThread(() -> setResult(RESULT_OK));
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
    private void updateTaskTitle() {
        if (currentTask != null && !currentTask.isCompleted()) {
            String newTitle = editDetailTitle.getText().toString().trim();
            if (!newTitle.isEmpty() && !newTitle.equals(currentTask.getTitle())) {
                currentTask.setTitle(newTitle);
                taskService.updateTask(currentTask);
                setResult(RESULT_OK);
            }
        }
    }
    private void setPriorityDisplay(String priority) {
        // Cập nhật label luôn hiển thị "Độ ưu tiên"
        textPriorityLabel.setText("Độ ưu tiên");
        
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
    
    private void updateCompletionStatus() {
        if (currentTask != null) {
            if (currentTask.isCompleted()) {
                // Làm mờ giao diện khi task đã hoàn thành
                editDetailTitle.setEnabled(false);
                editDetailTitle.setAlpha(0.6f);
                layoutDatePicker.setEnabled(false);
                layoutDatePicker.setAlpha(0.6f);
                spinnerCategory.setEnabled(false);
                spinnerCategory.setAlpha(0.6f);
                
                // Hiển thị thông tin hoàn thành
                if (textPriorityLabel != null) {
                    textPriorityLabel.setText("Trạng thái");
                    textPriorityValue.setText("Đã hoàn thành");
                    textPriorityValue.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            } else {
                // Khôi phục giao diện bình thường
                editDetailTitle.setEnabled(true);
                editDetailTitle.setAlpha(1.0f);
                layoutDatePicker.setEnabled(true);
                layoutDatePicker.setAlpha(1.0f);
                spinnerCategory.setEnabled(true);
                spinnerCategory.setAlpha(1.0f);
                
                // Hiển thị độ ưu tiên bình thường
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
    
    // New methods for description and attachments
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
    
    private void setupAttachmentRecyclerView() {
        recyclerAttachments.setLayoutManager(new LinearLayoutManager(this));
        attachmentAdapter = new AttachmentAdapter(this, currentTask != null ? currentTask.getAttachmentList() : null, this);
        recyclerAttachments.setAdapter(attachmentAdapter);
    }
    
    private void updateAttachmentView() {
        if (currentTask != null) {
            List<Attachment> attachments = currentTask.getAttachmentList();
            if (attachments.isEmpty()) {
                textNoAttachments.setVisibility(View.VISIBLE);
                recyclerAttachments.setVisibility(View.GONE);
            } else {
                textNoAttachments.setVisibility(View.GONE);
                recyclerAttachments.setVisibility(View.VISIBLE);
                attachmentAdapter.updateAttachments(attachments);
            }
        }
    }
    
    @Override
    public void onAttachmentDelete(Attachment attachment) {
        if (currentTask != null && !currentTask.isCompleted()) {
            // Delete from Firebase Storage first
            attachmentService.deleteAttachment(attachment.getStoragePath(), 
                new AttachmentService.AttachmentDeleteCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            // Remove from task after successful deletion from storage
                            currentTask.removeAttachment(attachment.getId());
                            taskService.updateTask(currentTask);
                            updateAttachmentView();
                            Toast.makeText(TaskDetailActivity.this, "Đã xóa tệp tin đính kèm", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            // Still remove from task even if storage deletion fails
                            currentTask.removeAttachment(attachment.getId());
                            taskService.updateTask(currentTask);
                            updateAttachmentView();
                            Toast.makeText(TaskDetailActivity.this, "Đã xóa khỏi danh sách (lỗi xóa file: " + error + ")", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
        }
    }
    
    @Override
    public void onAttachmentClick(Attachment attachment) {
        try {
            // Open file from Firebase Storage URL
            if (attachment.getDownloadUrl() != null && !attachment.getDownloadUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(attachment.getDownloadUrl()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Link file không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleSelectedFile(Uri fileUri) {
        try {
            // Get file info
            String fileName = getFileName(fileUri);
            String fileType = getContentResolver().getType(fileUri);
            long fileSize = getFileSize(fileUri);
            
            if (currentTask != null && !currentTask.isCompleted()) {
                // Show progress dialog
                android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
                progressDialog.setMessage("Đang upload tệp tin...");
                progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(100);
                progressDialog.setCancelable(false);
                progressDialog.show();
                
                // Upload to Firebase Storage
                attachmentService.uploadAttachment(fileUri, fileName, fileType, fileSize, 
                    new AttachmentService.AttachmentUploadCallback() {
                        @Override
                        public void onSuccess(Attachment attachment) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                // Add to task
                                currentTask.addAttachment(attachment);
                                taskService.updateTask(currentTask);
                                updateAttachmentView();
                                Toast.makeText(TaskDetailActivity.this, "Đã thêm tệp tin đính kèm", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(TaskDetailActivity.this, "Lỗi upload: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onProgress(int progress) {
                            runOnUiThread(() -> {
                                progressDialog.setProgress(progress);
                            });
                        }
                    });
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi thêm tệp tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    
    private long getFileSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (index != -1) {
                    return cursor.getLong(index);
                }
            }
        } catch (Exception e) {
            // Return default size if can't get actual size
        }
        return 0;
    }
    
    private void copyFile(Uri sourceUri, File destinationFile) throws Exception {
        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
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
