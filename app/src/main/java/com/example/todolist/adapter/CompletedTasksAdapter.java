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
    private List<Object> items; 
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
        List<String> sortedDates = new ArrayList<>(groupedTasks.keySet());
        sortedDates.sort((d1, d2) -> d2.compareTo(d1));
        for (String date : sortedDates) {
            items.add(date);
            List<Task> tasks = groupedTasks.get(date);
            if (tasks != null) {
                items.addAll(tasks);
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

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_date);
            timelineDot = itemView.findViewById(R.id.timeline_dot);
        }

        public void bind(String date) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date dateObj = inputFormat.parse(date);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM", Locale.getDefault());
                textDate.setText(outputFormat.format(dateObj));
                String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                if (date.equals(today) || getAdapterPosition() == 1) {
                    timelineDot.setBackgroundResource(R.drawable.timeline_dot_current);
                } else {
                    timelineDot.setBackgroundResource(R.drawable.timeline_dot);
                }
            } catch (Exception e) {
                try {
                    SimpleDateFormat legacyFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                    Date dateObj = legacyFormat.parse(date);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM", Locale.getDefault());
                    textDate.setText(outputFormat.format(dateObj));

                    String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                    String formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateObj);
                    if (formattedDate.equals(today) || getAdapterPosition() == 1) {
                        timelineDot.setBackgroundResource(R.drawable.timeline_dot_current);
                    } else {
                        timelineDot.setBackgroundResource(R.drawable.timeline_dot);
                    }
                } catch (Exception ex) {
                    textDate.setText(date);
                    timelineDot.setBackgroundResource(R.drawable.timeline_dot);
                }
            }
        }
    }
    class CompletedTaskViewHolder extends RecyclerView.ViewHolder {
        private ImageView checkboxComplete;
        private TextView textTaskTitle;
        private TextView textTaskDateTime;
        private ImageView iconNotification;
        private ImageView iconRepeat;
        private ImageView iconNotes;
        private ImageView iconAttachment;
        private ImageView iconSubtask;
        private ImageView iconStar;
        private LinearLayout taskBackground;
        public CompletedTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxComplete = itemView.findViewById(R.id.checkbox_complete);
            textTaskTitle = itemView.findViewById(R.id.text_task_title);
            textTaskDateTime = itemView.findViewById(R.id.text_task_datetime);
            iconNotification = itemView.findViewById(R.id.icon_notification);
            iconRepeat = itemView.findViewById(R.id.icon_repeat);
            iconNotes = itemView.findViewById(R.id.icon_notes);
            iconAttachment = itemView.findViewById(R.id.icon_attachment);
            iconSubtask = itemView.findViewById(R.id.icon_subtask);
            iconStar = itemView.findViewById(R.id.icon_star);
            taskBackground = itemView.findViewById(R.id.task_background);
        }
        public void bind(Task task) {
            textTaskTitle.setText(task.getTitle());
            String dateTimeText = formatDateTime(task.getDueDate(), task.getDueTime());
            if (dateTimeText != null && !dateTimeText.trim().isEmpty()) {
                textTaskDateTime.setText(dateTimeText);
                textTaskDateTime.setVisibility(View.VISIBLE);
            } else {
                textTaskDateTime.setVisibility(View.GONE);
            }
            checkboxComplete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCompletedTaskUncheck(task);
                }
            });

            iconNotification.setVisibility(task.isHasReminder() ? View.VISIBLE : View.GONE);
            iconRepeat.setVisibility(task.isRepeating() ? View.VISIBLE : View.GONE);
            iconNotes.setVisibility(task.getDescription() != null && 
                                   !task.getDescription().trim().isEmpty() ? View.VISIBLE : View.GONE);
            iconAttachment.setVisibility(task.hasAttachments() ? View.VISIBLE : View.GONE);           
            iconSubtask.setVisibility(task.getSubTasks() != null && !task.getSubTasks().isEmpty() ? View.VISIBLE : View.GONE);
            iconStar.setVisibility(task.isImportant() ? View.VISIBLE : View.GONE);
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
            
            // Check if time is empty, null, or "Không"
            if (dueTime == null || dueTime.equals("null") || dueTime.trim().isEmpty() || dueTime.equals("Không")) {
                // Only date, format as dd-MM
                try {
                    String[] dateParts = dueDate.split("/");
                    if (dateParts.length == 3) {
                        return dateParts[0] + "-" + dateParts[1]; // dd-MM format
                    }
                } catch (Exception e) {
                    // Fallback to original date format
                }
                return dueDate;
            }
            
            // Both date and time, format as dd-MM HH:mm
            try {
                String[] dateParts = dueDate.split("/");
                if (dateParts.length == 3) {
                    return dateParts[0] + "-" + dateParts[1] + " " + dueTime; // dd-MM HH:mm format
                }
            } catch (Exception e) {
                // Fallback
            }
            return dueDate + " " + dueTime;
        }
    }
}
