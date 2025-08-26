package com.example.todolist.helper.taskdetail;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.adapter.AttachmentAdapter;
import com.example.todolist.model.Attachment;
import com.example.todolist.model.Task;
import com.example.todolist.service.AttachmentService;
import com.example.todolist.view.WaveformView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttachmentHandler implements AttachmentAdapter.OnAttachmentActionListener {
    private static final int PERMISSION_REQUEST_AUDIO = 1002;
    private static final int PERMISSION_REQUEST_STORAGE = 1003;

    private AppCompatActivity activity;
    private AttachmentService attachmentService;
    private AttachmentAdapter attachmentAdapter;
    private RecyclerView recyclerAttachments;
    private View textNoAttachments;
    
    // Activity result launchers
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private MediaRecorder mediaRecorder;
    private String audioFileName;
    
    // Audio recording UI
    private AlertDialog audioRecordingDialog;
    private TextView timerText;
    private WaveformView waveformView;
    private FloatingActionButton btnStartRecording, btnCancelRecording, btnPauseResume, btnSaveRecording;
    private View recordingControls, recordingStatus;
    
    // Recording state
    private boolean isRecording = false;
    private boolean isPaused = false;
    private long recordingStartTime = 0;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private Handler waveformHandler;
    private Runnable waveformRunnable;

    public interface TaskUpdateCallback {
        void updateTask(Task task);
        Task getCurrentTask();
        void showToast(String message);
    }

    private TaskUpdateCallback taskUpdateCallback;

    public AttachmentHandler(AppCompatActivity activity, RecyclerView recyclerAttachments, 
                           View textNoAttachments, TaskUpdateCallback callback) {
        this.activity = activity;
        this.recyclerAttachments = recyclerAttachments;
        this.textNoAttachments = textNoAttachments;
        this.taskUpdateCallback = callback;
        this.attachmentService = new AttachmentService(activity);
        
        initActivityResultLaunchers();
        setupAttachmentRecyclerView();
    }

    private void initActivityResultLaunchers() {
        filePickerLauncher = activity.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        handleSelectedFile(fileUri);
                    }
                }
            }
        );
    }

    private void setupAttachmentRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(activity, 3);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Task currentTask = taskUpdateCallback.getCurrentTask();
                if (currentTask != null && currentTask.getAttachmentList() != null && 
                    position < currentTask.getAttachmentList().size()) {
                    Attachment attachment = currentTask.getAttachmentList().get(position);
                    String fileName = attachment.getFileName().toLowerCase();
                    
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                        fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                        fileName.endsWith(".mp4") || fileName.endsWith(".avi") || 
                        fileName.endsWith(".mov") || fileName.endsWith(".wmv") ||
                        fileName.endsWith(".mkv")) {
                        return 1;
                    } else {
                        return 3;
                    }
                }
                return 1;
            }
        });
        recyclerAttachments.setLayoutManager(gridLayoutManager);
        Task currentTask = taskUpdateCallback.getCurrentTask();
        attachmentAdapter = new AttachmentAdapter(activity, 
            currentTask != null ? currentTask.getAttachmentList() : null, this);
        recyclerAttachments.setAdapter(attachmentAdapter);
    }

    public void showFileTypeDialog() {
        Task currentTask = taskUpdateCallback.getCurrentTask();
        if (currentTask != null && !currentTask.isCompleted()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_file_type_chooser, null);
            builder.setView(dialogView);
            
            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setGravity(Gravity.BOTTOM);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            
            dialogView.findViewById(R.id.btn_choose_image).setOnClickListener(v -> {
                dialog.dismiss();
                chooseImageFromGallery();
            });
            
            dialogView.findViewById(R.id.btn_choose_video).setOnClickListener(v -> {
                dialog.dismiss();
                chooseVideoFromGallery();
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
        } else {
            taskUpdateCallback.showToast("Không thể thêm tệp tin vào nhiệm vụ đã hoàn thành");
        }
    }

    private void checkPermissionAndRecordAudio() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_AUDIO);
        } else {
            startAudioRecording();
        }
    }

    private void chooseImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        filePickerLauncher.launch(intent);
    }

    private void chooseVideoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        filePickerLauncher.launch(intent);
    }

    private void startAudioRecording() {
        showNewAudioRecordingDialog();
    }
    
    private void showNewAudioRecordingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_audio_recorder, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        audioRecordingDialog = builder.create();
        
        // Đặt dialog ở cuối màn hình
        if (audioRecordingDialog.getWindow() != null) {
            audioRecordingDialog.getWindow().setGravity(Gravity.BOTTOM);
            audioRecordingDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            audioRecordingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Initialize UI elements
        timerText = dialogView.findViewById(R.id.text_timer);
        waveformView = dialogView.findViewById(R.id.waveform_view);
        btnStartRecording = dialogView.findViewById(R.id.btn_start_recording);
        btnCancelRecording = dialogView.findViewById(R.id.btn_cancel_recording);
        btnPauseResume = dialogView.findViewById(R.id.btn_pause_resume);
        btnSaveRecording = dialogView.findViewById(R.id.btn_save_recording);
        recordingControls = dialogView.findViewById(R.id.layout_recording_controls);
        recordingStatus = dialogView.findViewById(R.id.text_recording_status);
        
        // Set click listeners
        btnStartRecording.setOnClickListener(v -> startRecording());
        btnCancelRecording.setOnClickListener(v -> cancelRecording());
        btnPauseResume.setOnClickListener(v -> togglePauseResume());
        btnSaveRecording.setOnClickListener(v -> stopAndSaveRecording());
        
        // Initialize handlers
        timerHandler = new Handler(Looper.getMainLooper());
        waveformHandler = new Handler(Looper.getMainLooper());
        
        audioRecordingDialog.show();
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
            
            // Update UI to recording state
            isRecording = true;
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            
            btnStartRecording.setVisibility(View.GONE);
            recordingStatus.setVisibility(View.GONE);
            timerText.setVisibility(View.VISIBLE);
            waveformView.setVisibility(View.VISIBLE);
            recordingControls.setVisibility(View.VISIBLE);
            
            // Start timer
            startTimer();
            
            // Start waveform animation
            startWaveformAnimation();
            
        } catch (Exception e) {
            e.printStackTrace();
            taskUpdateCallback.showToast("Lỗi khi bắt đầu ghi âm: " + e.getMessage());
        }
    }
    
    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording && !isPaused) {
                    long elapsed = System.currentTimeMillis() - recordingStartTime;
                    updateTimerDisplay(elapsed);
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }
    
    private void updateTimerDisplay(long elapsed) {
        int seconds = (int) (elapsed / 1000) % 60;
        int minutes = (int) (elapsed / (1000 * 60)) % 60;
        String timeString = String.format("%02d:%02d", minutes, seconds);
        timerText.setText(timeString);
    }
    
    private void startWaveformAnimation() {
        waveformRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording && !isPaused) {
                    waveformView.simulateRecording();
                    waveformHandler.postDelayed(this, 100);
                }
            }
        };
        waveformHandler.post(waveformRunnable);
    }
    
    private void togglePauseResume() {
        if (isPaused) {
            // Resume recording
            isPaused = false;
            btnPauseResume.setImageResource(android.R.drawable.ic_media_pause);
            startTimer();
            startWaveformAnimation();
        } else {
            // Pause recording
            isPaused = true;
            btnPauseResume.setImageResource(android.R.drawable.ic_media_play);
            if (timerRunnable != null) {
                timerHandler.removeCallbacks(timerRunnable);
            }
            if (waveformRunnable != null) {
                waveformHandler.removeCallbacks(waveformRunnable);
            }
        }
    }
    
    private void cancelRecording() {
        stopRecording();
        
        // Delete the recorded file
        if (audioFileName != null) {
            File file = new File(audioFileName);
            if (file.exists()) {
                file.delete();
            }
        }
        
        audioRecordingDialog.dismiss();
        resetRecordingState();
    }
    
    private void stopAndSaveRecording() {
        stopRecording();
        audioRecordingDialog.dismiss();
        showAudioNameDialog();
    }
    
    private void stopRecording() {
        isRecording = false;
        
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (Exception e) {
                // Ignore errors during stop
            }
        }
        
        // Stop timers
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (waveformRunnable != null) {
            waveformHandler.removeCallbacks(waveformRunnable);
        }
    }
    
    private void showAudioNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_audio_name, null);
        builder.setView(dialogView);
        
        AlertDialog nameDialog = builder.create();
        
        TextInputEditText editAudioName = dialogView.findViewById(R.id.edit_audio_name);
        TextView textCharCount = dialogView.findViewById(R.id.text_char_count);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_name);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_name);
        
        // Set default name with timestamp
        String defaultName = "Ghi âm " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        editAudioName.setText(defaultName);
        editAudioName.setSelection(editAudioName.getText().length());
        
        // Character counter
        editAudioName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textCharCount.setText(s.length() + "/50");
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        textCharCount.setText(defaultName.length() + "/50");
        
        btnCancel.setOnClickListener(v -> {
            nameDialog.dismiss();
            // Delete the recorded file
            if (audioFileName != null) {
                File file = new File(audioFileName);
                if (file.exists()) {
                    file.delete();
                }
            }
            resetRecordingState();
        });
        
        btnConfirm.setOnClickListener(v -> {
            String audioName = editAudioName.getText().toString().trim();
            if (audioName.isEmpty()) {
                taskUpdateCallback.showToast("Vui lòng nhập tên cho bản ghi âm");
                return;
            }
            
            nameDialog.dismiss();
            uploadAudioFile(audioName);
        });
        
        nameDialog.show();
    }
    
    private void uploadAudioFile(String fileName) {
        if (audioFileName != null) {
            File audioFile = new File(audioFileName);
            if (audioFile.exists()) {
                Uri fileUri = Uri.fromFile(audioFile);
                String finalFileName = fileName + ".3gp";
                long fileSize = audioFile.length();
                
                handleSelectedFile(fileUri, finalFileName, "audio/3gpp", fileSize);
            }
        }
        resetRecordingState();
    }
    
    private void resetRecordingState() {
        isRecording = false;
        isPaused = false;
        recordingStartTime = 0;
        audioFileName = null;
        
        if (waveformView != null) {
            waveformView.clear();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Chọn tệp tin"));
        } catch (android.content.ActivityNotFoundException e) {
            taskUpdateCallback.showToast("Không tìm thấy ứng dụng quản lý tệp tin");
        }
    }

    private File createAudioFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String audioFileName = "AUDIO_" + timeStamp + "_";
            File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            return File.createTempFile(audioFileName, ".3gp", storageDir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void handleSelectedFile(Uri fileUri) {
        handleSelectedFile(fileUri, null, null, 0);
    }
    
    private void handleSelectedFile(Uri fileUri, String customFileName, String customFileType, long customFileSize) {
        try {
            String fileName = customFileName != null ? customFileName : getFileName(fileUri);
            String fileType = customFileType != null ? customFileType : activity.getContentResolver().getType(fileUri);
            long fileSize = customFileSize > 0 ? customFileSize : getFileSize(fileUri);
            
            Task currentTask = taskUpdateCallback.getCurrentTask();
            if (currentTask != null && !currentTask.isCompleted()) {
                // Create custom upload dialog
                showUploadProgressDialog(fileName, fileSize, fileUri, fileType);
            }
            
        } catch (Exception e) {
            taskUpdateCallback.showToast("Lỗi khi thêm tệp tin: " + e.getMessage());
        }
    }
    
    private void showUploadProgressDialog(String fileName, long fileSize, Uri fileUri, String fileType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_upload_progress, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        AlertDialog progressDialog = builder.create();
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Initialize views
        TextView textFileName = dialogView.findViewById(R.id.text_file_name);
        TextView textFileSize = dialogView.findViewById(R.id.text_file_size);
        TextView textProgressPercent = dialogView.findViewById(R.id.text_progress_percent);
        TextView textStatus = dialogView.findViewById(R.id.text_status);
        com.example.todolist.view.CircularProgressView circularProgress = dialogView.findViewById(R.id.circular_progress);
        
        android.widget.ImageView dinosaur = dialogView.findViewById(R.id.dinosaur);
        android.widget.ImageView elephant = dialogView.findViewById(R.id.elephant); 
        android.view.View progressLine = dialogView.findViewById(R.id.progress_line);
        
        // Start animations
        android.view.animation.Animation dinosaurAnim = android.view.animation.AnimationUtils.loadAnimation(activity, R.anim.dinosaur_walk);
        android.view.animation.Animation elephantAnim = android.view.animation.AnimationUtils.loadAnimation(activity, R.anim.elephant_walk);
        dinosaur.startAnimation(dinosaurAnim);
        elephant.startAnimation(elephantAnim);
        
        // Set file info
        textFileName.setText(fileName);
        textFileSize.setText(formatFileSize(fileSize));
        textProgressPercent.setText("0%");
        // Ẩn text status, chỉ hiển thị animation
        textStatus.setVisibility(android.view.View.GONE);
        circularProgress.setProgress(0);
        
        progressDialog.show();
        
        // Start upload
        Task currentTask = taskUpdateCallback.getCurrentTask();
        attachmentService.uploadAttachment(fileUri, fileName, fileType, fileSize, 
            new AttachmentService.AttachmentUploadCallback() {
                @Override
                public void onSuccess(Attachment attachment) {
                    activity.runOnUiThread(() -> {
                        progressDialog.dismiss();
                        currentTask.addAttachment(attachment);
                        taskUpdateCallback.updateTask(currentTask);
                        updateAttachmentView();
                        taskUpdateCallback.showToast("Đã thêm tệp tin đính kèm");
                    });
                }

                @Override
                public void onError(String error) {
                    activity.runOnUiThread(() -> {
                        progressDialog.dismiss();
                        taskUpdateCallback.showToast("Lỗi upload: " + error);
                    });
                }

                @Override
                public void onProgress(int progress) {
                    activity.runOnUiThread(() -> {
                        circularProgress.setProgress(progress);
                        textProgressPercent.setText(progress + "%");
                        
                        // Update progress line width
                        android.view.ViewGroup.LayoutParams params = progressLine.getLayoutParams();
                        if (params instanceof android.widget.RelativeLayout.LayoutParams) {
                            android.widget.RelativeLayout.LayoutParams relParams = (android.widget.RelativeLayout.LayoutParams) params;
                            // Calculate width based on progress (max width minus margins)
                            int maxWidth = dialogView.getWidth() - 64 * 2; // 32dp margins on each side
                            relParams.width = (int) (maxWidth * progress / 100f);
                            progressLine.setLayoutParams(relParams);
                        }
                        
                        // Move elephant along the treestreet road based on progress
                        android.view.ViewGroup.LayoutParams elephantParams = elephant.getLayoutParams();
                        if (elephantParams instanceof android.widget.RelativeLayout.LayoutParams) {
                            android.widget.RelativeLayout.LayoutParams elephantRelParams = (android.widget.RelativeLayout.LayoutParams) elephantParams;
                            // Calculate elephant position based on progress
                            // Start at 32dp margin (left side), move to right side minus elephant width
                            int startMargin = 32; // dp converted to pixels
                            int containerWidth = dialogView.getWidth() - 64; // Total available width minus margins
                            int elephantWidth = 32; // Elephant width in dp
                            int maxPosition = containerWidth - elephantWidth;
                            
                            int elephantPosition = startMargin + (int) (maxPosition * progress / 100f);
                            elephantRelParams.leftMargin = elephantPosition;
                            elephant.setLayoutParams(elephantRelParams);
                        }
                        
                        // Chỉ cập nhật khi hoàn thành
                        if (progress >= 100) {
                            // Play eat animation - khủng long ăn voi
                            dinosaur.clearAnimation();
                            android.view.animation.Animation eatAnim = android.view.animation.AnimationUtils.loadAnimation(activity, R.anim.elephant_eaten);
                            elephant.startAnimation(eatAnim);
                        }
                    });
                }
            });
    }
    
    private String formatFileSize(long fileSize) {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    public void updateAttachmentView() {
        Task currentTask = taskUpdateCallback.getCurrentTask();
        if (currentTask != null) {
            List<Attachment> attachments = currentTask.getAttachmentList();
            if (attachments.isEmpty()) {
                if (textNoAttachments != null) {
                    textNoAttachments.setVisibility(View.VISIBLE);
                }
                recyclerAttachments.setVisibility(View.GONE);
            } else {
                if (textNoAttachments != null) {
                    textNoAttachments.setVisibility(View.GONE);
                }
                recyclerAttachments.setVisibility(View.VISIBLE);
                attachmentAdapter.updateAttachments(attachments);
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
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
        try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (index != -1) {
                    return cursor.getLong(index);
                }
            }
        } catch (Exception e) {

        }
        return 0;
    }

    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAudioRecording();
                } else {
                    taskUpdateCallback.showToast("Cần quyền ghi âm để ghi âm thanh");
                }
                break;
                
            case PERMISSION_REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker();
                } else {
                    taskUpdateCallback.showToast("Cần quyền truy cập bộ nhớ để chọn tệp tin");
                }
                break;
        }
    }

    @Override
    public void onAttachmentDelete(Attachment attachment) {
        Task currentTask = taskUpdateCallback.getCurrentTask();
        if (currentTask != null && !currentTask.isCompleted()) {
            attachmentService.deleteAttachment(attachment.getStoragePath(), 
                new AttachmentService.AttachmentDeleteCallback() {
                    @Override
                    public void onSuccess() {
                        activity.runOnUiThread(() -> {
                            currentTask.removeAttachment(attachment.getId());
                            taskUpdateCallback.updateTask(currentTask);
                            updateAttachmentView();
                            taskUpdateCallback.showToast("Đã xóa tệp tin đính kèm");
                        });
                    }

                    @Override
                    public void onError(String error) {
                        activity.runOnUiThread(() -> {
                            currentTask.removeAttachment(attachment.getId());
                            taskUpdateCallback.updateTask(currentTask);
                            updateAttachmentView();
                            taskUpdateCallback.showToast("Đã xóa khỏi danh sách (lỗi xóa file: " + error + ")");
                        });
                    }
                });
        }
    }

    @Override
    public void onAttachmentClick(Attachment attachment) {
        try {
            if (attachment.getDownloadUrl() != null && !attachment.getDownloadUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(attachment.getDownloadUrl()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } else {
                taskUpdateCallback.showToast("Link file không hợp lệ");
            }
        } catch (Exception e) {
            taskUpdateCallback.showToast("Không thể mở file: " + e.getMessage());
        }
    }

    public void cleanup() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (Exception e) {
                // Ignore errors during cleanup
            }
        }
        
        // Cleanup handlers
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (waveformHandler != null && waveformRunnable != null) {
            waveformHandler.removeCallbacks(waveformRunnable);
        }
        
        // Reset recording state
        resetRecordingState();
    }
}
