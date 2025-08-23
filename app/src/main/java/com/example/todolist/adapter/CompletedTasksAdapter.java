package com.example.todolist.adapter;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.model.Task;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class CompletedTasksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_TASK_ITEM = 1;
    private List<Object> items; // Mix of String (dates) and Task objects
    private OnCompletedTaskClickListener listener;
    public interface OnCompletedTaskClickListener {
        void onCompletedTaskClick(Task task);
        void onCompletedTaskLongClick(Task task);
        void onCompletedTaskUncheck(Task task);
    }
    public CompletedTasksAdapter(Map<String, List<Task>> groupedTasks, OnCompletedTaskClickListener listener) {
        this.listener = listener;
        this.items = new ArrayList<>();
        updateGroupedTasks(groupedTasks);
    }
    public void updateGroupedTasks(Map<String, List<Task>> groupedTasks) {
        items.clear();
        // Sort dates in descending order (most recent first)
        List<String> sortedDates = new ArrayList<>(groupedTasks.keySet());
        sortedDates.sort((d1, d2) -> d2.compareTo(d1));
        for (String date : sortedDates) {
            items.add(date); // Add date header
            List<Task> tasks = groupedTasks.get(date);
            if (tasks != null) {
                items.addAll(tasks); // Add tasks for this date
            }
        }
        notifyDataSetChanged();
    }
    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_DATE_HEADER : TYPE_TASK_ITEM;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_DATE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_completed_task, parent, false);
            return new CompletedTaskViewHolder(view);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            String date = (String) items.get(position);
            ((DateHeaderViewHolder) holder).bind(date);
        } else if (holder instanceof CompletedTaskViewHolder) {
            Task task = (Task) items.get(position);
            ((CompletedTaskViewHolder) holder).bind(task);
        }
    }
    @Override
    public int getItemCount() {
        return items.size();
    }
    class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView textDate;
        private View timelineDot;
        private View timelineLine;
        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_date);
            timelineDot = itemView.findViewById(R.id.timeline_dot);
            timelineLine = itemView.findViewById(R.id.timeline_line);
        }
        public void bind(String date) {
            // Format the date for display - dd/MM/yyyy
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                Date dateObj = inputFormat.parse(date);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                textDate.setText(outputFormat.format(dateObj));
            } catch (Exception e) {
                textDate.setText(date);
            }
            // Hide line for last item
            int position = getAdapterPosition();
            if (position == getItemCount() - 1 || 
                (position < getItemCount() - 1 && items.get(position + 1) instanceof String)) {
                timelineLine.setVisibility(View.GONE);
            } else {
                timelineLine.setVisibility(View.VISIBLE);
            }
        }
    }
    class CompletedTaskViewHolder extends RecyclerView.ViewHolder {
        private ImageView checkboxComplete;
        private TextView textTaskTitle;
        private TextView textTaskDateTime;
        private ImageView iconNotification;
        private ImageView iconRepeat;
        private ImageView iconStar;
        private LinearLayout taskBackground;
        public CompletedTaskViewHolder(@NonNull View itemView) {
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
            // Show due date and time if available
            String dateTimeText = formatDateTime(task.getDueDate(), task.getDueTime());
            if (dateTimeText != null && !dateTimeText.trim().isEmpty()) {
                textTaskDateTime.setText(dateTimeText);
                textTaskDateTime.setVisibility(View.VISIBLE);
            } else {
                textTaskDateTime.setVisibility(View.GONE);
            }
            // Always checked for completed tasks, but can be unchecked by clicking
            // checkboxComplete is now an ImageView with check circle icon
            checkboxComplete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCompletedTaskUncheck(task);
                }
            });
            // Show icons based on task properties
            iconNotification.setVisibility(task.isHasReminder() ? View.VISIBLE : View.GONE);
            iconRepeat.setVisibility(task.isRepeating() ? View.VISIBLE : View.GONE);
            iconStar.setVisibility(task.isImportant() ? View.VISIBLE : View.GONE);
            // Gray out completed tasks - already handled by XML colors
            // Click listeners - disabled for completed tasks
            taskBackground.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCompletedTaskClick(task);
                }
            });
            taskBackground.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onCompletedTaskLongClick(task);
                    return true;
                }
                return false;
            });
        }
        private String formatDateTime(String dueDate, String dueTime) {
            if (dueDate == null || dueDate.equals("null") || dueDate.trim().isEmpty()) {
                return null;
            }
            if (dueTime == null || dueTime.equals("null") || dueTime.trim().isEmpty() || dueTime.equals("KhĂ´ng")) {
                // Only date, format as dd-mm
                String[] dateParts = dueDate.split("/");
                if (dateParts.length == 3) {
                    return dateParts[2] + "-" + dateParts[1]; // dd-mm format
                }
                return dueDate;
            }
            // Both date and time, format as dd-mm HH:mm
            String[] dateParts = dueDate.split("/");
            if (dateParts.length == 3) {
                return dateParts[2] + "-" + dateParts[1] + " " + dueTime; // dd-mm HH:mm format
            }
            return dueDate + " " + dueTime;
        }
    }
}
