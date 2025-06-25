package com.example.todolist.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.todolist.R;
import com.example.todolist.model.TodoTask;

public class TaskActionsDialog {
    
    public interface OnActionSelectedListener {
        void onStarAction(TodoTask task);
        void onDeleteAction(TodoTask task);
    }
    
    private Context context;
    private TodoTask task;
    private OnActionSelectedListener listener;
    private AlertDialog dialog;
    
    public TaskActionsDialog(Context context, TodoTask task, OnActionSelectedListener listener) {
        this.context = context;
        this.task = task;
        this.listener = listener;
        createDialog();
    }
    
    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_task_actions, null);
        builder.setView(dialogView);
        
        dialog = builder.create();
        
        // Make dialog background transparent so CardView corners show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        setupClickListeners(dialogView);
        updateStarText(dialogView);
    }
    
    private void setupClickListeners(View dialogView) {
        // Star action
        dialogView.findViewById(R.id.layout_action_star).setOnClickListener(v -> {
            if (listener != null) {
                listener.onStarAction(task);
            }
            dialog.dismiss();
        });

        // Delete action
        dialogView.findViewById(R.id.layout_action_delete).setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAction(task);
            }
            dialog.dismiss();
        });
    }
    
    private void updateStarText(View dialogView) {
        TextView starText = dialogView.findViewById(R.id.tv_star_text);
        if (task.isImportant()) {
            starText.setText("Bỏ đánh dấu quan trọng");
        } else {
            starText.setText("Đánh dấu quan trọng");
        }
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
