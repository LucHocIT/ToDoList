package com.example.todolist.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

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
