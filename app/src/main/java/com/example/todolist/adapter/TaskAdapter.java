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
            String dateTimeText = formatDateTime(task.getDueDate(), task.getDueTime());
            if (dateTimeText != null && !dateTimeText.trim().isEmpty() && 
                !dateTimeText.equals("null null") && !dateTimeText.contains("null") &&
                !dateTimeText.equals("Không") && !dateTimeText.equals("null")) {
                textTaskDateTime.setText(dateTimeText);
                textTaskDateTime.setVisibility(View.VISIBLE);
                boolean isOverdueToday = isTaskOverdueToday(task);
                if (isOverdueToday) {
                    textTaskDateTime.setTextColor(Color.RED);
                } else {
                    textTaskDateTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_gray));
                }
            } else {
                textTaskDateTime.setVisibility(View.GONE);
            }
            checkboxComplete.setOnCheckedChangeListener(null);
            boolean currentTaskCompleted = task.isCompleted();
            checkboxComplete.setChecked(currentTaskCompleted);
            boolean hasValidTime = task.getDueTime() != null && !task.getDueTime().trim().isEmpty() && !task.getDueTime().equals("null");
            iconNotification.setVisibility(task.isHasReminder() && hasValidTime ? View.VISIBLE : View.GONE);
            iconRepeat.setVisibility(task.isRepeating() && 
                                   task.getRepeatType() != null && 
                                   !task.getRepeatType().equals("Không") &&
                                   !task.getRepeatType().equals("Không có") ? View.VISIBLE : View.GONE);
            iconStar.setVisibility(task.isImportant() ? View.VISIBLE : View.GONE);
            if (task.isCompleted()) {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                textTaskTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
            } else {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                textTaskTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
            }

            taskBackground.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            taskBackground.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onTaskLongClick(task);
                    return true;
                }
                return false;
            });
            checkboxComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked == currentTaskCompleted || isChecked == task.isCompleted()) {
                    return; 
                }
                if (listener != null) {
                    listener.onTaskComplete(task, isChecked);
                }
            });            
            itemView.setTranslationX(0);
        }
        private String formatDateTime(String dueDate, String dueTime) {
            try {
                if (dueDate == null || dueDate.trim().isEmpty() || dueDate.equals("null") || dueDate.equals("KhĂ´ng")) {
                    return ""; // No date set, only show title
                }
                String todayDateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                if (dueDate.equals(todayDateStr)) {
                    if (dueTime != null && !dueTime.trim().isEmpty() && !dueTime.equals("null") && !dueTime.equals("KhĂ´ng")) {
                        return dueTime; 
                    } else {
                        return ""; 
                    }
                }
                if (dueTime == null || dueTime.trim().isEmpty() || dueTime.equals("null") || dueTime.equals("KhĂ´ng")) {
                    // Only date available, format: dd-mm
                    String[] dateParts = dueDate.split("/");
                    if (dateParts.length == 3) {
                        return dateParts[2] + "-" + dateParts[1]; 
                    }
                    return dueDate;
                }
                String[] dateParts = dueDate.split("/");
                if (dateParts.length == 3) {
                    return dateParts[2] + "-" + dateParts[1] + " " + dueTime;
                }
            } catch (Exception e) {

            }
            if ((dueDate == null || dueDate.equals("null") || dueDate.equals("Không")) && 
                (dueTime == null || dueTime.equals("null") || dueTime.equals("Không"))) {
                return "";
            }
            return (dueDate != null && !dueDate.equals("null") && !dueDate.equals("Không") ? dueDate : "") + 
                   (dueTime != null && !dueTime.equals("null") && !dueTime.equals("Không") ? " " + dueTime : "");
        }
        private boolean isTaskOverdueToday(Task task) {
            try {
                Calendar now = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String todayDateStr = dateFormat.format(now.getTime());
                String currentTimeStr = timeFormat.format(now.getTime());
                if (task.getDueDate().equals(todayDateStr)) {
                    return task.getDueTime().compareTo(currentTimeStr) < 0;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
