package com.example.todolist.service;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import com.example.todolist.manager.AuthManager;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
    private AuthManager authManager;
    
    public AttachmentService(Context context) {
        this.context = context;
        this.authManager = AuthManager.getInstance();
        if (this.authManager != null) {
            this.authManager.initialize(context);
        }
    }
    
    public void uploadAttachment(String taskId, Uri fileUri, AttachmentCallback callback) {
        // Kiểm tra xem user đã login và bật sync chưa
        if (!checkAuthAndSync(callback)) {
            return;
        }
        
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
        // Kiểm tra xem user đã login và bật sync chưa
        if (!checkAuthAndSyncForUpload(callback)) {
            return;
        }
        
        // Upload file to Firebase Storage
        try {
            // Create unique filename
            String fileName = "attachment_" + UUID.randomUUID().toString();
            String userEmail = authManager.getCurrentUserEmail();
            if (userEmail == null) {
                if (callback != null) {
                    callback.onError("User not authenticated");
                }
                return;
            }
            
            // Firebase Storage path: attachments/userEmail/taskId/fileName
            String sanitizedEmail = userEmail.replace(".", "_").replace("@", "_at_");
            String storagePath = "attachments/" + sanitizedEmail + "/" + taskId + "/" + fileName;
            
            // Get Firebase Storage reference
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference fileRef = storageRef.child(storagePath);
            
            // Start upload
            UploadTask uploadTask = fileRef.putFile(fileUri);
            
            // Listen for state changes, errors, and completion of the upload
            uploadTask.addOnProgressListener(taskSnapshot -> {
                // Calculate progress percentage
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                if (callback != null) {
                    callback.onProgress((int) progress);
                }
            }).addOnSuccessListener(taskSnapshot -> {
                // Upload completed successfully, get download URL
                fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String downloadUrl = downloadUri.toString();
                    if (callback != null) {
                        // Return both download URL and storage path as JSON-like string
                        // Format: "downloadUrl|storagePath"
                        String result = downloadUrl + "|" + storagePath;
                        callback.onSuccess(result);
                    }
                }).addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Failed to get download URL: " + e.getMessage());
                    }
                });
            }).addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onError("Upload failed: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Upload error: " + e.getMessage());
            }
        }
    }
    
    public void deleteAttachment(String storagePath, AttachmentDeleteCallback callback) {
        // Kiểm tra xem user đã login và bật sync chưa
        if (!checkAuthAndSyncForDelete(callback)) {
            return;
        }
        
        // Delete from Firebase Storage if storagePath is provided
        if (storagePath != null && !storagePath.isEmpty() && storagePath.startsWith("attachments/")) {
            try {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                StorageReference fileRef = storageRef.child(storagePath);
                
                fileRef.delete().addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }).addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Failed to delete from Firebase Storage: " + e.getMessage());
                    }
                });
                
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Delete error: " + e.getMessage());
                }
            }
        } else {
            // Fallback: try to delete as local file
            try {
                File file = new File(storagePath);
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
    
    /**
     * Kiểm tra xem user đã login và bật sync chưa cho AttachmentCallback
     * @param callback callback để trả về lỗi nếu không thỏa mãn điều kiện
     * @return true nếu đã login và bật sync, false nếu chưa
     */
    private boolean checkAuthAndSync(AttachmentCallback callback) {
        if (authManager == null) {
            if (callback != null) {
                callback.onError("Lỗi hệ thống: AuthManager không khả dụng");
            }
            return false;
        }
        
        // Kiểm tra xem user đã đăng nhập chưa
        if (!authManager.isSignedIn()) {
            if (callback != null) {
                callback.onError("Chưa đăng nhập");
            }
            return false;
        }
        
        // Kiểm tra xem user đã bật sync chưa
        if (!authManager.isSyncEnabled()) {
            if (callback != null) {
                callback.onError("Chưa bật đồng bộ hóa");
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Kiểm tra xem user đã login và bật sync chưa cho AttachmentUploadCallback
     * @param callback callback để trả về lỗi nếu không thỏa mãn điều kiện
     * @return true nếu đã login và bật sync, false nếu chưa
     */
    private boolean checkAuthAndSyncForUpload(AttachmentUploadCallback callback) {
        if (authManager == null) {
            if (callback != null) {
                callback.onError("Lỗi hệ thống: AuthManager không khả dụng");
            }
            return false;
        }
        
        // Kiểm tra xem user đã đăng nhập chưa
        if (!authManager.isSignedIn()) {
            if (callback != null) {
                callback.onError("Bạn cần đăng nhập để sử dụng tính năng upload file");
            }
            return false;
        }
        
        // Kiểm tra xem user đã bật sync chưa
        if (!authManager.isSyncEnabled()) {
            if (callback != null) {
                callback.onError("Bạn cần bật đồng bộ hóa để sử dụng tính năng upload file");
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Hiển thị toast message
     * @param message thông điệp cần hiển thị
     */
    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Kiểm tra xem user đã login và bật sync chưa cho AttachmentDeleteCallback
     * @param callback callback để trả về lỗi nếu không thỏa mãn điều kiện
     * @return true nếu đã login và bật sync, false nếu chưa
     */
    private boolean checkAuthAndSyncForDelete(AttachmentDeleteCallback callback) {
        if (authManager == null) {
            if (callback != null) {
                callback.onError("Lỗi hệ thống: AuthManager không khả dụng");
            }
            return false;
        }
        
        // Kiểm tra xem user đã đăng nhập chưa
        if (!authManager.isSignedIn()) {
            if (callback != null) {
                callback.onError("Bạn cần đăng nhập để sử dụng tính năng xóa file");
            }
            return false;
        }
        
        // Kiểm tra xem user đã bật sync chưa
        if (!authManager.isSyncEnabled()) {
            if (callback != null) {
                callback.onError("Bạn cần bật đồng bộ hóa để sử dụng tính năng xóa file");
            }
            return false;
        }
        
        return true;
    }
}
