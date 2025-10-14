package com.example.todolist.manager;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.todolist.R;
public class NavigationDrawerManager {
    private static final int REQUEST_CALL_PERMISSION = 1001;
    public interface NavigationListener {
        void onThemeSelected();
        void onUtilitiesSelected();
        void onContactSelected();
        void onSettingsSelected();
    }
    private AppCompatActivity activity;
    private DrawerLayout drawerLayout;
    private NavigationListener listener;
    // Navigation menu items
    private LinearLayout navTheme;
    private LinearLayout navUtilities;
    private LinearLayout navSharedTasks;
    private LinearLayout navContact;
    private LinearLayout navSettings;
    public NavigationDrawerManager(AppCompatActivity activity, DrawerLayout drawerLayout, NavigationListener listener) {
        this.activity = activity;
        this.drawerLayout = drawerLayout;
        this.listener = listener;
        initNavigationItems();
        setupNavigationListeners();
    }
    private void initNavigationItems() {
        navTheme = activity.findViewById(R.id.nav_theme);
        navUtilities = activity.findViewById(R.id.nav_utilities);
        navSharedTasks = activity.findViewById(R.id.nav_shared_tasks);
        navContact = activity.findViewById(R.id.nav_contact);
        navSettings = activity.findViewById(R.id.nav_settings);
    }
    private void setupNavigationListeners() {
        if (navTheme != null) {
            navTheme.setOnClickListener(v -> {
                closeDrawer();
                if (listener != null) {
                    listener.onThemeSelected();
                }
                showThemeDialog();
            });
        }
        if (navUtilities != null) {
            navUtilities.setOnClickListener(v -> {
                closeDrawer();
                if (listener != null) {
                    listener.onUtilitiesSelected();
                }
                // Mở màn hình hướng dẫn tiện ích widget
                Intent widgetsIntent = new Intent(activity, com.example.todolist.WidgetsGuideActivity.class);
                activity.startActivity(widgetsIntent);
            });
        }
        if (navSharedTasks != null) {
            // Ẩn menu "Công việc chia sẻ"
            navSharedTasks.setVisibility(View.GONE);
        }
        if (navContact != null) {
            navContact.setOnClickListener(v -> {
                closeDrawer();
                if (listener != null) {
                    listener.onContactSelected();
                }
                showContactDialog();
            });
        }
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                closeDrawer();
                if (listener != null) {
                    listener.onSettingsSelected();
                }
                showSettingsDialog();
            });
        }
    }
    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
    public void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
    public boolean isDrawerOpen() {
        return drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START);
    }
    private void showThemeDialog() {
        // Má»Ÿ ThemeSelectionActivity thay vĂ¬ hiá»ƒn thá»‹ dialog Ä‘Æ¡n giáº£n
        Intent intent = new Intent(activity, com.example.todolist.ThemeSelectionActivity.class);
        activity.startActivity(intent);
    }
    
    private void showContactDialog() {
        // Táº¡o dialog vá»›i layout custom
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        // Inflate custom layout
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_contact, null);
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        // Setup click listeners
        setupContactDialogListeners(dialogView, dialog);
        dialog.show();
    }
    private void setupContactDialogListeners(View dialogView, androidx.appcompat.app.AlertDialog dialog) {
        // Email contact click
        View layoutEmailContact = dialogView.findViewById(R.id.layout_email_contact);
        layoutEmailContact.setOnClickListener(v -> {
            sendEmail();
        });
        // Phone contact click
        View layoutPhoneContact = dialogView.findViewById(R.id.layout_phone_contact);
        layoutPhoneContact.setOnClickListener(v -> {
            makePhoneCall();
        });
        // Send feedback button
        View btnSendFeedback = dialogView.findViewById(R.id.btn_send_feedback);
        btnSendFeedback.setOnClickListener(v -> {
            sendEmail();
            dialog.dismiss();
        });
        // Close button
        View btnCloseContact = dialogView.findViewById(R.id.btn_close_contact);
        btnCloseContact.setOnClickListener(v -> {
            dialog.dismiss();
        });
    }
    private void sendEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"phamluc2304@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Phản hồi về ứng dụng To-Do List");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Xin chào đội phát triển,\n\nTôi muốn gửi phản hồi về ứng dụng To-Do List:\n\n");
        try {
            activity.startActivity(Intent.createChooser(emailIntent, "Gửi email qua"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, "Không tìm thấy ứng dụng email", Toast.LENGTH_SHORT).show();
        }
    }
    private void makePhoneCall() {
        // Kiá»ƒm tra quyá»n CALL_PHONE
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            // Náº¿u chÆ°a cĂ³ quyá»n, yĂªu cáº§u quyá»n
            ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.CALL_PHONE}, 
                REQUEST_CALL_PERMISSION);
        } else {
            // Náº¿u Ä‘Ă£ cĂ³ quyá»n, thá»±c hiá»‡n cuá»™c gá»i
            performPhoneCall();
        }
    }
    private void performPhoneCall() {
        Intent phoneIntent = new Intent(Intent.ACTION_CALL);
        phoneIntent.setData(android.net.Uri.parse("tel:0354337494"));
        try {
            activity.startActivity(phoneIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, "Không thể thực hiện cuộc gọi", Toast.LENGTH_SHORT).show();
        } catch (SecurityException ex) {
            Toast.makeText(activity, "Cần cấp quyền để thực hiện cuộc gọi", Toast.LENGTH_SHORT).show();
        }
    }
    // Phương thức để xử lý kết quả yêu cầu quyền
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp, thực hiện cuộc gọi
                performPhoneCall();
            } else {
                // Quyền bị từ chối
                Toast.makeText(activity, "Cần cấp quyền gọi điện để sử dụng tính năng này", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void showSettingsDialog() {
        // Mở SettingsActivity thay vì hiển thị dialog đơn giản
        Intent intent = new Intent(activity, com.example.todolist.SettingsActivity.class);
        activity.startActivity(intent);
    }
    

}
