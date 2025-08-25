package com.example.todolist.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.todolist.R;
import com.example.todolist.model.Task;

public class TaskActionsDialog {
    public interface OnActionSelectedListener {
        void onStarAction(Task task);
        void onDeleteAction(Task task);
    }
    
    private Context context;
    private Task task;
    private OnActionSelectedListener listener;
    private AlertDialog dialog;
    
    public TaskActionsDialog(Context context, Task task, OnActionSelectedListener listener) {
        this.context = context;
        this.task = task;
        this.listener = listener;
        createDialog();
    }
    
    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_task_actions, null);
        
        setupViews(dialogView);
        
        builder.setView(dialogView);
        dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
    
    private void setupViews(View dialogView) {
        LinearLayout starLayout = dialogView.findViewById(R.id.layout_action_star);
        LinearLayout deleteLayout = dialogView.findViewById(R.id.layout_action_delete);
        TextView starText = dialogView.findViewById(R.id.tv_star_text);
        TextView deleteText = dialogView.findViewById(R.id.tv_cl_text);
        if (task.isImportant()) {
            starText.setText("Bỏ đánh dấu quan trọng");
        } else {
            starText.setText("Đánh dấu quan trọng");
        }
        
        starLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStarAction(task);
            }
            dismiss();
        });
        
        deleteLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAction(task);
            }
            dismiss();
        });
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
    
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
