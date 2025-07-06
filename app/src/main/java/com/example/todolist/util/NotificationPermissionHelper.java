package com.example.todolist.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Utility class để xử lý quyền thông báo
 */
public class NotificationPermissionHelper {
    
    public static final int NOTIFICATION_PERMISSION_CODE = 1001;
    
    /**
     * Kiểm tra xem có quyền thông báo không
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Trước Android 13 không cần quyền
    }
    
    /**
     * Yêu cầu quyền thông báo
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }
    
    /**
     * Kiểm tra kết quả yêu cầu quyền
     */
    public static boolean isNotificationPermissionGranted(int requestCode, int[] grantResults) {
        return requestCode == NOTIFICATION_PERMISSION_CODE 
                && grantResults.length > 0 
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}
