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
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.adapter.AttachmentAdapter;
import com.example.todolist.model.Attachment;
import com.example.todolist.model.Task;
import com.example.todolist.service.AttachmentService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttachmentHandler implements AttachmentAdapter.OnAttachmentActionListener {
    private static final int PERMISSION_REQUEST_CAMERA = 1001;
    private static final int PERMISSION_REQUEST_AUDIO = 1002;
    private static final int PERMISSION_REQUEST_STORAGE = 1003;

    private AppCompatActivity activity;
    private AttachmentService attachmentService;
    private AttachmentAdapter attachmentAdapter;
    private RecyclerView recyclerAttachments;
    private View textNoAttachments;
    
    // Activity result launchers
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> imageCaptureForActivityResult;
    private ActivityResultLauncher<Intent> videoCaptureForActivityResult;
    private Uri capturedImageUri;
    private Uri capturedVideoUri;
    private MediaRecorder mediaRecorder;
    private String audioFileName;

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

        imageCaptureForActivityResult = activity.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && capturedImageUri != null) {
                    handleSelectedFile(capturedImageUri);
                }
            }
        );
        
        videoCaptureForActivityResult = activity.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && capturedVideoUri != null) {
                    handleSelectedFile(capturedVideoUri);
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
        } else {
            taskUpdateCallback.showToast("Không thể thêm tệp tin vào nhiệm vụ đã hoàn thành");
        }
    }

    private void checkPermissionAndChooseImage() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_CAMERA);
        } else {
            showImageChooserDialog();
        }
    }
    
    private void checkPermissionAndChooseVideo() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_CAMERA);
        } else {
            showVideoChooserDialog();
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

    private void showImageChooserDialog() {
        String[] options = {"Chụp ảnh", "Chọn từ thư viện"};
        new AlertDialog.Builder(activity)
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

    private void showVideoChooserDialog() {
        String[] options = {"Quay video", "Chọn từ thư viện"};
        new AlertDialog.Builder(activity)
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

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                capturedImageUri = FileProvider.getUriForFile(activity,
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

    private void captureVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            File videoFile = createVideoFile();
            if (videoFile != null) {
                capturedVideoUri = FileProvider.getUriForFile(activity,
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

    private void startAudioRecording() {
        showAudioRecordingDialog();
    }
    
    private void showAudioRecordingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Ghi âm")
            .setMessage("Nhấn 'Bắt đầu' để bắt đầu ghi âm")
            .setPositiveButton("Bắt đầu", (dialog, which) -> startRecording())
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
            taskUpdateCallback.showToast("Lỗi khi bắt đầu ghi âm: " + e.getMessage());
        }
    }
    
    private void showRecordingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Đang ghi âm...")
            .setMessage("Nhấn 'Dừng' để kết thúc ghi âm")
            .setPositiveButton("Dừng", (dialog, which) -> stopRecording())
            .setCancelable(false)
            .show();
    }
    
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                
                File audioFile = new File(audioFileName);
                if (audioFile.exists()) {
                    Uri audioUri = Uri.fromFile(audioFile);
                    handleSelectedFile(audioUri);
                }
                
                taskUpdateCallback.showToast("Ghi âm hoàn tất");
                
            } catch (Exception e) {
                e.printStackTrace();
                taskUpdateCallback.showToast("Lỗi khi dừng ghi âm: " + e.getMessage());
            }
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

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
            File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
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
            File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            return File.createTempFile(audioFileName, ".3gp", storageDir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void handleSelectedFile(Uri fileUri) {
        try {
            String fileName = getFileName(fileUri);
            String fileType = activity.getContentResolver().getType(fileUri);
            long fileSize = getFileSize(fileUri);
            
            Task currentTask = taskUpdateCallback.getCurrentTask();
            if (currentTask != null && !currentTask.isCompleted()) {
                ProgressDialog progressDialog = new ProgressDialog(activity);
                progressDialog.setMessage("Đang upload tệp tin...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(100);
                progressDialog.setCancelable(false);
                progressDialog.show();
                
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
                            activity.runOnUiThread(() -> progressDialog.setProgress(progress));
                        }
                    });
            }
            
        } catch (Exception e) {
            taskUpdateCallback.showToast("Lỗi khi thêm tệp tin: " + e.getMessage());
        }
    }

    public void updateAttachmentView() {
        Task currentTask = taskUpdateCallback.getCurrentTask();
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
            // Return default size if can't get actual size
        }
        return 0;
    }

    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showImageChooserDialog();
                } else {
                    taskUpdateCallback.showToast("Cần quyền truy cập camera để chụp ảnh/quay video");
                }
                break;
                
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
    }
}
