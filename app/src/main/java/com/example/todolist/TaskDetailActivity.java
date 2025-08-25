package com.example.todolist;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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
    
    // Permission request codes
    private static final int PERMISSION_REQUEST_CAMERA = 1001;
    private static final int PERMISSION_REQUEST_AUDIO = 1002;
    private static final int PERMISSION_REQUEST_STORAGE = 1003;
    
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
    
    // Activity result launchers
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> imageCaptureForActivityResult;
    private ActivityResultLauncher<Intent> videoCaptureForActivityResult;
    private ActivityResultLauncher<Intent> audioRecordingForActivityResult;
    private Uri capturedImageUri;
    private Uri capturedVideoUri;
    private MediaRecorder mediaRecorder;
    private String audioFileName; 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        isInitialCategorySetup = true;    
        taskService = new TaskService(this, this);
        categoryService = new CategoryService(this, this);
        attachmentService = new AttachmentService(this);
        initActivityResultLaunchers();
        initViews();
        loadTaskData();
        setupClickListeners();
    }
    
    private void initActivityResultLaunchers() {
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

        imageCaptureForActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && capturedImageUri != null) {
                    handleSelectedFile(capturedImageUri);
                }
            }
        );
        
        // Video capture launcher
        videoCaptureForActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && capturedVideoUri != null) {
                    handleSelectedFile(capturedVideoUri);
                }
            }
        );
        
        // Audio recording launcher
        audioRecordingForActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && audioFileName != null) {
                    File audioFile = new File(audioFileName);
                    if (audioFile.exists()) {
                        Uri audioUri = Uri.fromFile(audioFile);
                        handleSelectedFile(audioUri);
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
                showFileTypeDialog();
            } else {
                Toast.makeText(this, "Không thể thêm tệp tin vào nhiệm vụ đã hoàn thành", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showFileTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_file_type_chooser, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // Set up click listeners for each option
        dialogView.findViewById(R.id.btn_choose_image).setOnClickListener(v -> {
            dialog.dismiss();
            checkPermissionAndChooseImage();
        });
        
        dialogView.findViewById(R.id.btn_choose_video).setOnClickListener(v -> {
            dialog.dismiss();
            checkPermissionAndChooseVideo();
        });
        
        dialogView.findViewById(R.id.btn_record_audio).setOnClickListener(v -> {
            dialog.dismiss();
            checkPermissionAndRecordAudio();
        });
        
        dialogView.findViewById(R.id.btn_choose_file).setOnClickListener(v -> {
            dialog.dismiss();
            openFilePicker();
        });
        
        dialog.show();
    }
    
    // Permission checking methods
    private void checkPermissionAndChooseImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_CAMERA);
        } else {
            showImageChooserDialog();
        }
    }
    
    private void checkPermissionAndChooseVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_CAMERA);
        } else {
            showVideoChooserDialog();
        }
    }
    
    private void checkPermissionAndRecordAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_AUDIO);
        } else {
            startAudioRecording();
        }
    }
    
    // Image chooser methods
    private void showImageChooserDialog() {
        String[] options = {"Chụp ảnh", "Chọn từ thư viện"};
        new AlertDialog.Builder(this)
            .setTitle("Chọn ảnh")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    captureImage();
                } else {
                    chooseImageFromGallery();
                }
            })
            .show();
    }
    
    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                capturedImageUri = FileProvider.getUriForFile(this,
                    "com.example.todolist.fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
                imageCaptureForActivityResult.launch(intent);
            }
        }
    }
    
    private void chooseImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        filePickerLauncher.launch(intent);
    }
    
    // Video chooser methods
    private void showVideoChooserDialog() {
        String[] options = {"Quay video", "Chọn từ thư viện"};
        new AlertDialog.Builder(this)
            .setTitle("Chọn video")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    captureVideo();
                } else {
                    chooseVideoFromGallery();
                }
            })
            .show();
    }
    
    private void captureVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File videoFile = createVideoFile();
            if (videoFile != null) {
                capturedVideoUri = FileProvider.getUriForFile(this,
                    "com.example.todolist.fileprovider", videoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedVideoUri);
                videoCaptureForActivityResult.launch(intent);
            }
        }
    }
    
    private void chooseVideoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        filePickerLauncher.launch(intent);
    }
    
    // Audio recording methods
    private void startAudioRecording() {
        showAudioRecordingDialog();
    }
    
    private void showAudioRecordingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ghi âm")
            .setMessage("Nhấn 'Bắt đầu' để bắt đầu ghi âm")
            .setPositiveButton("Bắt đầu", (dialog, which) -> {
                startRecording();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
    
    private void startRecording() {
        try {
            audioFileName = createAudioFile().getAbsolutePath();
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFileName);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            showRecordingDialog();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi bắt đầu ghi âm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showRecordingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đang ghi âm...")
            .setMessage("Nhấn 'Dừng' để kết thúc ghi âm")
            .setPositiveButton("Dừng", (dialog, which) -> {
                stopRecording();
            })
            .setCancelable(false)
            .show();
    }
    
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                
                // Add the recorded audio file as attachment
                File audioFile = new File(audioFileName);
                if (audioFile.exists()) {
                    Uri audioUri = Uri.fromFile(audioFile);
                    handleSelectedFile(audioUri);
                }
                
                Toast.makeText(this, "Ghi âm hoàn tất", Toast.LENGTH_SHORT).show();
                
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi dừng ghi âm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // File creation helper methods
    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private File createVideoFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String videoFileName = "MP4_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            return File.createTempFile(videoFileName, ".mp4", storageDir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private File createAudioFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String audioFileName = "AUDIO_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            return File.createTempFile(audioFileName, ".3gp", storageDir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
    
    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Check if this was for image or video
                    if (permissions[0].equals(Manifest.permission.CAMERA)) {
                        showImageChooserDialog(); // Default to image, you might want to track which was requested
                    }
                } else {
                    Toast.makeText(this, "Cần quyền truy cập camera để chụp ảnh/quay video", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case PERMISSION_REQUEST_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAudioRecording();
                } else {
                    Toast.makeText(this, "Cần quyền ghi âm để ghi âm thanh", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case PERMISSION_REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker();
                } else {
                    Toast.makeText(this, "Cần quyền truy cập bộ nhớ để chọn tệp tin", Toast.LENGTH_SHORT).show();
                }
                break;
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
        // Cleanup MediaRecorder if still recording
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (Exception e) {
                // Ignore errors during cleanup
            }
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
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (currentTask != null && currentTask.getAttachmentList() != null && 
                    position < currentTask.getAttachmentList().size()) {
                    Attachment attachment = currentTask.getAttachmentList().get(position);
                    String fileName = attachment.getFileName().toLowerCase();
                    
                    // Ảnh và video chiếm 1 span (3 cột)
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                        fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                        fileName.endsWith(".mp4") || fileName.endsWith(".avi") || 
                        fileName.endsWith(".mov") || fileName.endsWith(".wmv") ||
                        fileName.endsWith(".mkv")) {
                        return 1; // 1/3 của row
                    } else {
                        // File âm thanh và tài liệu chiếm toàn bộ row (3 span)
                        return 3;
                    }
                }
                return 1;
            }
        });
        recyclerAttachments.setLayoutManager(gridLayoutManager);
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
