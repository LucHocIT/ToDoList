package com.example.todolist.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.adapter.SharedUsersAdapter;
import com.example.todolist.model.SharedUser;
import com.example.todolist.model.TaskShare;
import com.example.todolist.service.sharing.TaskSharingService;
import com.example.todolist.service.AutoEmailService;
import com.example.todolist.manager.AuthManager;

import java.util.ArrayList;
import java.util.List;

public class ShareTaskDialog extends Dialog {
    
    private Context context;
    private String taskId;
    private String taskTitle;
    private OnShareTaskListener listener;
    
    // UI Components
    private TextView textTitle;
    private EditText editUserEmail;
    private EditText editUserName;
    private CheckBox checkCanEdit;
    private Button btnInvite;
    private Button btnCancel;
    private RecyclerView recyclerSharedUsers;
    private LinearLayout layoutSharedUsers;
    private TextView textSharedUsersTitle;
    private ImageView btnClose;
    
    // Data
    private List<SharedUser> sharedUsers;
    private SharedUsersAdapter sharedUsersAdapter;
    private TaskSharingService taskSharingService;
    private AutoEmailService autoEmailService;
    private AuthManager authManager;
    
    public interface OnShareTaskListener {
        void onTaskShared(String userEmail, String userName);
        void onUserRemoved(String userEmail);
        void onError(String error);
    }
    
    public ShareTaskDialog(@NonNull Context context, String taskId, String taskTitle, OnShareTaskListener listener) {
        super(context, R.style.Theme_ToDoList);
        this.context = context;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.listener = listener;
        this.sharedUsers = new ArrayList<>();
        this.taskSharingService = TaskSharingService.getInstance();
        this.taskSharingService.initialize(context);
        this.autoEmailService = AutoEmailService.getInstance();
        this.autoEmailService.initialize(context);
        this.authManager = AuthManager.getInstance();
        this.authManager.initialize(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_share_task);
        
        // Thiết lập kích thước dialog
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
        initViews();
        setupClickListeners();
        loadExistingSharedUsers();
    }
    
    private void initViews() {
        textTitle = findViewById(R.id.text_title);
        editUserEmail = findViewById(R.id.edit_user_email);
        editUserName = findViewById(R.id.edit_user_name);
        checkCanEdit = findViewById(R.id.check_can_edit);
        btnInvite = findViewById(R.id.btn_invite);
        btnCancel = findViewById(R.id.btn_cancel);
        recyclerSharedUsers = findViewById(R.id.recycler_shared_users);
        layoutSharedUsers = findViewById(R.id.layout_shared_users);
        textSharedUsersTitle = findViewById(R.id.text_shared_users_title);
        btnClose = findViewById(R.id.btn_close);
        
        textTitle.setText("Chia sẻ: " + taskTitle);
        checkCanEdit.setChecked(true); // Mặc định cho phép chỉnh sửa
        
        // Thiết lập RecyclerView
        sharedUsersAdapter = new SharedUsersAdapter(context, sharedUsers, new SharedUsersAdapter.OnSharedUserActionListener() {
            @Override
            public void onRemoveUser(SharedUser user) {
                removeSharedUser(user);
            }
            
            @Override
            public void onTogglePermission(SharedUser user, boolean canEdit) {
                updateUserPermission(user, canEdit);
            }
        });
        
        recyclerSharedUsers.setLayoutManager(new LinearLayoutManager(context));
        recyclerSharedUsers.setAdapter(sharedUsersAdapter);
    }
    
    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnInvite.setOnClickListener(v -> inviteUser());
        
        // Auto-fill name từ email
        editUserEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && TextUtils.isEmpty(editUserName.getText())) {
                String email = editUserEmail.getText().toString().trim();
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    String name = email.substring(0, email.indexOf('@'));
                    editUserName.setText(name);
                }
            }
        });
    }
    
    private void loadExistingSharedUsers() {
        taskSharingService.getTaskShare(taskId, new TaskSharingService.TaskShareCallback() {
            @Override
            public void onTaskShareLoaded(TaskShare taskShare) {
                if (taskShare.getSharedUsers() != null && !taskShare.getSharedUsers().isEmpty()) {
                    sharedUsers.clear();
                    sharedUsers.addAll(taskShare.getSharedUsers());
                    
                    // Cập nhật UI trên main thread
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            updateSharedUsersView();
                        });
                    }
                }
            }
            
            @Override
            public void onError(String error) {
                // Task chưa được chia sẻ hoặc lỗi - không cần làm gì
            }
        });
    }
    
    private void inviteUser() {
        String email = editUserEmail.getText().toString().trim();
        String name = editUserName.getText().toString().trim();
        boolean canEdit = checkCanEdit.isChecked();
        
        // Validate input
        if (TextUtils.isEmpty(email)) {
            editUserEmail.setError("Vui lòng nhập email");
            return;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editUserEmail.setError("Email không hợp lệ");
            return;
        }
        
        if (TextUtils.isEmpty(name)) {
            editUserName.setError("Vui lòng nhập tên");
            return;
        }
        
        // Kiểm tra xem user đã được invite chưa
        for (SharedUser user : sharedUsers) {
            if (user.getEmail().equals(email)) {
                Toast.makeText(context, "Người dùng đã được mời", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Disable button để tránh double click
        btnInvite.setEnabled(false);
        
        taskSharingService.shareTask(taskId, email, name, canEdit, new TaskSharingService.SharingCallback() {
            @Override
            public void onSuccess(String message) {
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        // Thêm user vào danh sách
                        SharedUser newUser = new SharedUser(email, name, canEdit);
                        sharedUsers.add(newUser);
                        updateSharedUsersView();
                        
                        // Clear input fields
                        editUserEmail.setText("");
                        editUserName.setText("");
                        checkCanEdit.setChecked(true);
                        
                        // Gửi email mời
                        sendInvitationEmail(email, name);
                        
                        // Gửi broadcast để refresh tasks (không tự động đánh dấu là shared)
                        if (context instanceof android.app.Activity) {
                            Intent refreshIntent = new Intent("com.example.todolist.REFRESH_TASKS");
                            context.sendBroadcast(refreshIntent);
                        }
                        
                        btnInvite.setEnabled(true);
                        
                        if (listener != null) {
                            listener.onTaskShared(email, name);
                        }
                        
                        Toast.makeText(context, "Đã mời thành công", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        btnInvite.setEnabled(true);
                        
                        if (listener != null) {
                            listener.onError(error);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Gửi email mời chia sẻ task
     */
    private void sendInvitationEmail(String recipientEmail, String recipientName) {
        final String inviterName;
        String tempName = authManager.getCurrentUserName();
        if (tempName == null) {
            tempName = authManager.getCurrentUserEmail();
        }
        if (tempName == null) {
            tempName = "Người dùng ToDoList";
        }
        inviterName = tempName;

        // Tạo shareId (có thể lấy từ Firebase sau khi tạo thành công)
        String shareId = "share_" + System.currentTimeMillis();

        // Gửi email tự động
        autoEmailService.sendTaskInvitationEmail(
            recipientEmail, 
            recipientName, 
            taskTitle, 
            inviterName, 
            taskId, 
            shareId, 
            new AutoEmailService.EmailSendCallback() {
                @Override
                public void onEmailSent(String message) {
                    // Email đã gửi thành công - không cần hiển thị dialog
                }

                @Override
                public void onError(String error) {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            new androidx.appcompat.app.AlertDialog.Builder(context)
                                .setTitle("⚠️ Không thể gửi email tự động")
                                .setMessage(error + "\n\nBạn có thể:\n" +
                                          "• Thử gửi thủ công qua app email\n" +
                                          "• Copy link mời để gửi qua tin nhắn\n" +
                                          "• Thử lại sau")
                                .setPositiveButton("OK", null)
                                .setNeutralButton("Gửi thủ công", (dialog, which) -> {
                                    // Fallback về cách gửi email thủ công
                                    String finalRecipientEmail = recipientEmail;
                                    String finalRecipientName = recipientName;
                                    String finalInviterName = inviterName;
                                    sendManualEmail(finalRecipientEmail, finalRecipientName, finalInviterName);
                                })
                                .setNegativeButton("Copy Link", (dialog, which) -> {
                                    String finalRecipientEmail = recipientEmail;
                                    copyInviteLink(finalRecipientEmail);
                                })
                                .show();
                        });
                    }
                }
            }
        );
    }

    /**
     * Fallback: Gửi email thủ công qua app email
     */
    private void sendManualEmail(String recipientEmail, String recipientName, String inviterName) {
        String shareId = "share_" + System.currentTimeMillis();
        String inviteUrl = "https://todolist.app/invite?taskId=" + taskId + "&shareId=" + shareId + "&email=" + recipientEmail;
        
        String subject = "🎯 Lời mời chia sẻ task: " + taskTitle;
        String emailBody = "Xin chào " + recipientName + ",\n\n" +
                          inviterName + " đã mời bạn cộng tác trong task: \"" + taskTitle + "\"\n\n" +
                          "Để tham gia, vui lòng click vào link: " + inviteUrl + "\n\n" +
                          "Cảm ơn bạn!\n" +
                          "Đội ngũ ToDoList";
        
        try {
            android.content.Intent emailIntent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
            emailIntent.setData(android.net.Uri.parse("mailto:" + recipientEmail));
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailBody);
            emailIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (emailIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(emailIntent);
                Toast.makeText(context, "📧 Đã mở ứng dụng email", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "⚠️ Không có ứng dụng email", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "⚠️ Lỗi mở email: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Copy invite link vào clipboard
     */
    private void copyInviteLink(String recipientEmail) {
        String shareId = "share_" + System.currentTimeMillis();
        String inviteUrl = "https://todolist.app/invite?taskId=" + taskId + "&shareId=" + shareId + "&email=" + recipientEmail;
        
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("ToDoList Invite Link", inviteUrl);
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(context, "📋 Đã copy link mời vào clipboard!\nBạn có thể gửi link này cho " + recipientEmail, Toast.LENGTH_LONG).show();
    }
    
    private void removeSharedUser(SharedUser user) {
        new androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Xóa quyền truy cập")
            .setMessage("Bạn có chắc muốn xóa quyền truy cập của " + user.getName() + "?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                // TODO: Implement remove user từ Firebase
                sharedUsers.remove(user);
                updateSharedUsersView();
                
                if (listener != null) {
                    listener.onUserRemoved(user.getEmail());
                }
                
                Toast.makeText(context, "Đã xóa quyền truy cập", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
    
    private void updateUserPermission(SharedUser user, boolean canEdit) {
        user.setCanEdit(canEdit);
        // TODO: Cập nhật permission trong Firebase
        
        String message = canEdit ? "Đã cấp quyền chỉnh sửa" : "Đã thu hồi quyền chỉnh sửa";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    private void updateSharedUsersView() {
        if (sharedUsers.isEmpty()) {
            layoutSharedUsers.setVisibility(View.GONE);
        } else {
            layoutSharedUsers.setVisibility(View.VISIBLE);
            textSharedUsersTitle.setText("Đã chia sẻ với (" + sharedUsers.size() + " người)");
        }
        
        sharedUsersAdapter.notifyDataSetChanged();
    }
    
    public void setSharedUsers(List<SharedUser> users) {
        if (users != null) {
            this.sharedUsers.clear();
            this.sharedUsers.addAll(users);
            updateSharedUsersView();
        }
    }
}