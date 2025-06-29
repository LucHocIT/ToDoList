package com.example.todolist.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.todolist.R;

public class WidgetsDialog {
    
    private Context context;
    private Dialog dialog;
    
    public WidgetsDialog(Context context) {
        this.context = context;
        createDialog();
    }
    
    private void createDialog() {
        dialog = new Dialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_widgets, null);
        dialog.setContentView(dialogView);
        
        // Make dialog fullscreen width with some margin
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        setupClickListeners(dialogView);
    }
    
    private void setupClickListeners(View dialogView) {
        // Close button
        ImageView btnClose = dialogView.findViewById(R.id.btn_close_dialog);
        btnClose.setOnClickListener(v -> dismiss());
        
        // Widget preview clicks
        LinearLayout widgetSmallTask = dialogView.findViewById(R.id.widget_small_task);
        LinearLayout widgetMediumTask = dialogView.findViewById(R.id.widget_medium_task);
        LinearLayout widgetCalendar = dialogView.findViewById(R.id.widget_calendar);
        LinearLayout widgetQuickAdd = dialogView.findViewById(R.id.widget_quick_add);
        
        widgetSmallTask.setOnClickListener(v -> showWidgetInstructions("Widget Nhiệm vụ Nhỏ"));
        widgetMediumTask.setOnClickListener(v -> showWidgetInstructions("Widget Nhiệm vụ Trung"));
        widgetCalendar.setOnClickListener(v -> showWidgetInstructions("Widget Lịch"));
        widgetQuickAdd.setOnClickListener(v -> showWidgetInstructions("Widget Thêm nhanh"));
        
        // Click outside to dismiss
        dialogView.setOnClickListener(v -> dismiss());
        
        // Prevent dialog content clicks from dismissing
        LinearLayout dialogContent = dialogView.findViewById(R.id.dialog_content);
        if (dialogContent != null) {
            dialogContent.setOnClickListener(v -> {
                // Do nothing - prevent dismissal
            });
        }
    }
    
    private void showWidgetInstructions(String widgetName) {
        String message = "Để thêm " + widgetName + ":\\n" +
                        "1. Nhấn giữ màn hình chính\\n" +
                        "2. Chọn 'Widget' hoặc 'Tiện ích'\\n" +
                        "3. Tìm 'To-Do List'\\n" +
                        "4. Kéo widget vào vị trí mong muốn\\n\\n" +
                        "Bạn có muốn mở cài đặt widget không?";
        
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle("Hướng dẫn thêm " + widgetName)
               .setMessage(message)
               .setPositiveButton("Mở cài đặt", (dialogInterface, i) -> {
                   openWidgetSettings();
               })
               .setNegativeButton("Đóng", null)
               .show();
    }
    
    private void openWidgetSettings() {
        try {
            // Try to open widget picker
            Intent intent = new Intent();
            intent.setAction("android.appwidget.action.APPWIDGET_PICK");
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                // Fallback to app settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
                Toast.makeText(context, "Vui lòng tìm mục Widget trong cài đặt", Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Toast.makeText(context, "Không thể mở cài đặt widget. Vui lòng thêm widget thủ công.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }
    
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
