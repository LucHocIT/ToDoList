package com.example.todolist.service;

import android.content.Context;
import android.net.Uri;
import com.example.todolist.model.Attachment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.UUID;

public class AttachmentService {
    
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Context context;
    
    public interface AttachmentUploadCallback {
        void onSuccess(Attachment attachment);
        void onError(String error);
        void onProgress(int progress);
    }
    
    public interface AttachmentDeleteCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public AttachmentService(Context context) {
        this.context = context;
        this.storage = FirebaseStorage.getInstance();
        this.storageRef = storage.getReference();
    }
    
    public void uploadAttachment(Uri fileUri, String fileName, String fileType, long fileSize, AttachmentUploadCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("Người dùng chưa đăng nhập");
            return;
        }
        
        // Generate unique file name
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
        String storagePath = "attachments/" + userId + "/" + uniqueFileName;
        
        StorageReference fileRef = storageRef.child(storagePath);
        
        UploadTask uploadTask = fileRef.putFile(fileUri);
        
        // Listen for upload progress
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress((int) progress);
        });
        
        // Handle upload completion
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                // Create attachment object
                Attachment attachment = new Attachment(fileName, fileType, fileSize);
                attachment.setDownloadUrl(downloadUri.toString());
                attachment.setStoragePath(storagePath);
                attachment.setId(UUID.randomUUID().toString());
                
                callback.onSuccess(attachment);
            }).addOnFailureListener(e -> {
                callback.onError("Lỗi lấy download URL: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            callback.onError("Lỗi upload file: " + e.getMessage());
        });
    }
    
    public void deleteAttachment(String storagePath, AttachmentDeleteCallback callback) {
        if (storagePath == null || storagePath.isEmpty()) {
            callback.onError("Đường dẫn file không hợp lệ");
            return;
        }
        
        StorageReference fileRef = storageRef.child(storagePath);
        fileRef.delete().addOnSuccessListener(aVoid -> {
            callback.onSuccess();
        }).addOnFailureListener(e -> {
            callback.onError("Lỗi xóa file: " + e.getMessage());
        });
    }
    
    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
    }
}
