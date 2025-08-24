package com.example.todolist.util;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.todolist.model.Task;
/**
 * TaskActionsDialog - Firebase version
 * Shows action options for tasks (star/unstar, delete)
 */
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
        // Create simple LinearLayout
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(20, 20, 20, 20);
        // Setup views
        setupViews(view);
        builder.setView(view);
        dialog = builder.create();
    }
    private void setupViews(View view) {
        // Use standard Android layout views
        TextView titleText = new TextView(context);
        titleText.setText(task.getTitle());
        titleText.setTextSize(18);
        titleText.setPadding(20, 20, 20, 10);
        TextView starText = new TextView(context);
        TextView deleteText = new TextView(context);
        // Setup star action
        if (task.isImportant()) {
            starText.setText("â­ Bật đánh dấu quan trọng");
        } else {
            starText.setText("â˜† Đánh dấu quan trọng");
        }
        starText.setPadding(20, 10, 20, 10);
        starText.setTextSize(16);
        deleteText.setText("đŸ—‘ï¸ Xóa");
        deleteText.setPadding(20, 10, 20, 20);
        deleteText.setTextSize(16);
        // Set click listeners
        starText.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStarAction(task);
            }
            dismiss();
        });
        deleteText.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAction(task);
            }
            dismiss();
        });
        // Add views to container
        ((LinearLayout) view).addView(titleText);
        ((LinearLayout) view).addView(starText);  
        ((LinearLayout) view).addView(deleteText);
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
