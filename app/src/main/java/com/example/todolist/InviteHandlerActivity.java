package com.example.todolist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.todolist.manager.AuthManager;
import com.example.todolist.service.sharing.TaskSharingService;

public class InviteHandlerActivity extends AppCompatActivity {
    private static final String TAG = "InviteHandlerActivity";
    private AuthManager authManager;
    private TaskSharingService taskSharingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Không cần layout vì đây là activity xử lý logic
        authManager = AuthManager.getInstance();
        authManager.initialize(this);
        taskSharingService = TaskSharingService.getInstance();
        taskSharingService.initialize(this);
        
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri data = intent.getData();
        
        if (data != null) {
            handleInviteLink(data);
        } else {
            // Không có data, trở về MainActivity
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        }
    }

    private void handleInviteLink(Uri uri) {
        try {
            String taskId = uri.getQueryParameter("taskId");
            String shareId = uri.getQueryParameter("shareId");
            String invitedEmail = uri.getQueryParameter("email");

            if (taskId == null || shareId == null || invitedEmail == null) {
                showErrorAndFinish("Link mời không hợp lệ");
                return;
            }

            // Kiểm tra user đã đăng nhập chưa
            String currentUserEmail = authManager.getCurrentUserEmail();
            if (currentUserEmail == null) {
                showLoginRequired(uri.toString());
                return;
            }

            // Kiểm tra email có khớp không
            if (!invitedEmail.equalsIgnoreCase(currentUserEmail)) {
                showErrorAndFinish("Bạn cần đăng nhập bằng email: " + invitedEmail);
                return;
            }

            // Hiển thị loading và join task
            Toast.makeText(this, "🔄 Đang tham gia task...", Toast.LENGTH_SHORT).show();
            joinTask(taskId);

        } catch (Exception e) {
            showErrorAndFinish("Lỗi xử lý link mời: " + e.getMessage());
        }
    }

    private void joinTask(String taskId) {
        String currentUserEmail = authManager.getCurrentUserEmail();
        
        // Cập nhật trạng thái user thành ACCEPTED trong Firebase
        taskSharingService.acceptTaskInvitation(taskId, currentUserEmail, new TaskSharingService.SharingCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(InviteHandlerActivity.this, "✅ Đã tham gia task thành công!", Toast.LENGTH_LONG).show();
                    
                    // Gửi broadcast để refresh tasks trong MainActivity
                    Intent refreshIntent = new Intent("com.example.todolist.REFRESH_TASKS");
                    sendBroadcast(refreshIntent);
                    
                    // Mở TaskDetailActivity
                    Intent taskIntent = new Intent(InviteHandlerActivity.this, TaskDetailActivity.class);
                    taskIntent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId);
                    taskIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(taskIntent);
                    
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(InviteHandlerActivity.this, "❌ Lỗi tham gia task: " + error, Toast.LENGTH_LONG).show();
                    
                    // Vẫn mở task để user có thể xem
                    Intent taskIntent = new Intent(InviteHandlerActivity.this, TaskDetailActivity.class);
                    taskIntent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId);
                    taskIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(taskIntent);
                    
                    finish();
                });
            }
        });
    }

    private void showErrorAndFinish(String error) {
        Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
        
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        
        finish();
    }

    private void showLoginRequired(String pendingUri) {
        Toast.makeText(this, "🔐 Vui lòng đăng nhập để tham gia task", Toast.LENGTH_LONG).show();
        
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("pending_invite_uri", pendingUri);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        
        finish();
    }
}