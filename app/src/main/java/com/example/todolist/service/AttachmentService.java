package com.example.todolist.service;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class AttachmentService {
    
    public interface AttachmentCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface AttachmentListCallback {
        void onSuccess(List<String> attachments);
        void onError(String error);
    }
    
    public interface AttachmentUploadCallback {
        void onSuccess(String filePath);
        void onError(String error);
        void onProgress(int progress);
    }
    
    public interface AttachmentDeleteCallback {
        void onSuccess();
        void onError(String error);
    }
    
    private Context context;
    
    public AttachmentService(Context context) {
        this.context = context;
    }
    
    public void uploadAttachment(String taskId, Uri fileUri, AttachmentCallback callback) {
        // For SQLite version, we just store file paths locally
        try {
            String fileName = "attachment_" + System.currentTimeMillis() + ".jpg";
            File attachmentDir = new File(context.getFilesDir(), "attachments");
            if (!attachmentDir.exists()) {
                attachmentDir.mkdirs();
            }
            
            File attachmentFile = new File(attachmentDir, fileName);
            // In a real implementation, you would copy the file here
            
            if (callback != null) {
                callback.onSuccess();
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }
    
    public void deleteAttachment(String taskId, String attachmentUrl, AttachmentCallback callback) {
        try {
            File file = new File(attachmentUrl);
            if (file.exists()) {
                file.delete();
            }
            
            if (callback != null) {
                callback.onSuccess();
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }
    
    public void getAttachments(String taskId, AttachmentListCallback callback) {
        try {
            File attachmentDir = new File(context.getFilesDir(), "attachments");
            List<String> attachments = new ArrayList<>();
            
            if (attachmentDir.exists()) {
                File[] files = attachmentDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        attachments.add(file.getAbsolutePath());
                    }
                }
            }
            
            if (callback != null) {
                callback.onSuccess(attachments);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }
    
    public void uploadAttachment(String taskId, Uri fileUri, AttachmentUploadCallback callback) {
        // For SQLite version, we just store file paths locally
        try {
            String fileName = "attachment_" + System.currentTimeMillis() + ".jpg";
            String filePath = context.getFilesDir() + "/attachments/" + taskId + "/" + fileName;
            
            // Simulate progress
            if (callback != null) {
                callback.onProgress(50);
                callback.onProgress(100);
                callback.onSuccess(filePath);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }
    
    public void deleteAttachment(String filePath, AttachmentDeleteCallback callback) {
        try {
            File file = new File(filePath);
            boolean deleted = file.delete();
            
            if (callback != null) {
                if (deleted) {
                    callback.onSuccess();
                } else {
                    callback.onError("Failed to delete file");
                }
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }
}
