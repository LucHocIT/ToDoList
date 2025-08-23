package com.example.todolist.adapter;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.model.Task;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> tasks;
    private OnTaskClickListener listener;
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskComplete(Task task, boolean isCompleted);
        void onTaskLongClick(Task task);
        void onTaskStar(Task task);
        void onTaskDelete(Task task);
    }
    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }
    @Override
    public int getItemCount() {
        return tasks.size();
    }
    public void updateTasks(List<Task> newTasks) {
        for (int i = 0; i < newTasks.size(); i++) {
            Task task = newTasks.get(i);
        }
        this.tasks = newTasks;
        notifyDataSetChanged();
    }
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkboxComplete;
        private TextView textTaskTitle;
        private TextView textTaskDateTime;
        private ImageView iconNotification;
        private ImageView iconRepeat;
        private ImageView iconStar;
        private LinearLayout taskBackground;
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxComplete = itemView.findViewById(R.id.checkbox_complete);
            textTaskTitle = itemView.findViewById(R.id.text_task_title);
            textTaskDateTime = itemView.findViewById(R.id.text_task_datetime);
            iconNotification = itemView.findViewById(R.id.icon_notification);
            iconRepeat = itemView.findViewById(R.id.icon_repeat);
            iconStar = itemView.findViewById(R.id.icon_star);
            taskBackground = itemView.findViewById(R.id.task_background);
        }
        public void bind(Task task) {
            textTaskTitle.setText(task.getTitle());
            // Format date and time display
            String dateTimeText = formatDateTime(task.getDueDate(), task.getDueTime());
            // Only show date/time if it's not empty or null and not default values
            if (dateTimeText != null && !dateTimeText.trim().isEmpty() && 
                !dateTimeText.equals("null null") && !dateTimeText.contains("null") &&
                !dateTimeText.equals("Không") && !dateTimeText.equals("null")) {
                textTaskDateTime.setText(dateTimeText);
                textTaskDateTime.setVisibility(View.VISIBLE);
                // Check if task is overdue but still today
                boolean isOverdueToday = isTaskOverdueToday(task);
                if (isOverdueToday) {
                    textTaskDateTime.setTextColor(Color.RED);
                } else {
                    textTaskDateTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_gray));
                }
            } else {
                textTaskDateTime.setVisibility(View.GONE);
            }
            // Set checkbox state without triggering listener - IMPORTANT: Clear listener first!
            checkboxComplete.setOnCheckedChangeListener(null);
            // Store current task state for comparison
            boolean currentTaskCompleted = task.isCompleted();
            checkboxComplete.setChecked(currentTaskCompleted);
            // Show/hide notification icon (bell) - only if has reminder AND has time
            boolean hasValidTime = task.getDueTime() != null && !task.getDueTime().trim().isEmpty() && !task.getDueTime().equals("null");
            iconNotification.setVisibility(task.isHasReminder() && hasValidTime ? View.VISIBLE : View.GONE);
            // Show/hide repeat icon
            iconRepeat.setVisibility(task.isRepeating() && 
                                   task.getRepeatType() != null && 
                                   !task.getRepeatType().equals("Không") &&
                                   !task.getRepeatType().equals("Không có") ? View.VISIBLE : View.GONE);
            // Show/hide star icon for importance
            iconStar.setVisibility(task.isImportant() ? View.VISIBLE : View.GONE);
            // Strike through text if completed
            if (task.isCompleted()) {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                textTaskTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
            } else {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                textTaskTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
            }
            // Click listeners
            taskBackground.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
            // Long click listener for action menu
            taskBackground.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onTaskLongClick(task);
                    return true;
                }
                return false;
            });
            // Set checkbox listener after setting the state - with improved validation
            checkboxComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Strict validation to prevent false triggers
                if (isChecked == currentTaskCompleted || isChecked == task.isCompleted()) {
                    return; // No actual change, ignore
                }
                if (listener != null) {
                    listener.onTaskComplete(task, isChecked);
                }
            });            
            // Reset item position on bind
            itemView.setTranslationX(0);
        }
        private String formatDateTime(String dueDate, String dueTime) {
            try {
                // Check for null or empty values
                if (dueDate == null || dueDate.trim().isEmpty() || dueDate.equals("null") || dueDate.equals("KhĂ´ng")) {
                    return ""; // No date set, only show title
                }
                // Get today's date
                String todayDateStr = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date());
                // Check if it's today's date
                if (dueDate.equals(todayDateStr)) {
                    // For today's tasks, only show time if available
                    if (dueTime != null && !dueTime.trim().isEmpty() && !dueTime.equals("null") && !dueTime.equals("KhĂ´ng")) {
                        return dueTime; // Only show time for today's tasks
                    } else {
                        return ""; // Today's task without specific time, only show title
                    }
                }
                // For other dates (not today)
                if (dueTime == null || dueTime.trim().isEmpty() || dueTime.equals("null") || dueTime.equals("KhĂ´ng")) {
                    // Only date available, format: dd-mm
                    String[] dateParts = dueDate.split("/");
                    if (dateParts.length == 3) {
                        return dateParts[2] + "-" + dateParts[1]; // dd-mm format
                    }
                    return dueDate;
                }
                // Both date and time available for other dates, format: dd-mm HH:mm
                String[] dateParts = dueDate.split("/");
                if (dateParts.length == 3) {
                    return dateParts[2] + "-" + dateParts[1] + " " + dueTime; // dd-mm HH:mm format
                }
            } catch (Exception e) {
                // Fallback for any parsing errors
            }
            // Final fallback
            if ((dueDate == null || dueDate.equals("null") || dueDate.equals("Không")) && 
                (dueTime == null || dueTime.equals("null") || dueTime.equals("Không"))) {
                return "";
            }
            return (dueDate != null && !dueDate.equals("null") && !dueDate.equals("Không") ? dueDate : "") + 
                   (dueTime != null && !dueTime.equals("null") && !dueTime.equals("Không") ? " " + dueTime : "");
        }
        private boolean isTaskOverdueToday(Task task) {
            try {
                // Get current date and time
                Calendar now = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String todayDateStr = dateFormat.format(now.getTime());
                String currentTimeStr = timeFormat.format(now.getTime());
                // Check if task is today
                if (task.getDueDate().equals(todayDateStr)) {
                    // Compare time
                    return task.getDueTime().compareTo(currentTimeStr) < 0;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
