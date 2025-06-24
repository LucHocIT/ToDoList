package com.example.todolist.adapter;

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
import com.example.todolist.model.TodoTask;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    
    private List<TodoTask> tasks;
    private OnTaskClickListener listener;
    
    public interface OnTaskClickListener {
        void onTaskClick(TodoTask task);
        void onTaskComplete(TodoTask task, boolean isCompleted);
        void onTaskStar(TodoTask task);
        void onTaskCalendar(TodoTask task);
        void onTaskDelete(TodoTask task);
    }
    
    public TaskAdapter(List<TodoTask> tasks, OnTaskClickListener listener) {
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
        TodoTask task = tasks.get(position);
        holder.bind(task);
    }
    
    @Override
    public int getItemCount() {
        return tasks.size();
    }
    
    public void updateTasks(List<TodoTask> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }
      class TaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkboxComplete;
        private TextView textTaskTitle;
        private TextView textTaskDateTime;
        private ImageView iconNotification;
        private ImageView iconFlag;
        private LinearLayout taskBackground;
        
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxComplete = itemView.findViewById(R.id.checkbox_complete);
            textTaskTitle = itemView.findViewById(R.id.text_task_title);
            textTaskDateTime = itemView.findViewById(R.id.text_task_datetime);
            iconNotification = itemView.findViewById(R.id.icon_notification);
            iconFlag = itemView.findViewById(R.id.icon_flag);
            taskBackground = itemView.findViewById(R.id.task_background);
        }
          public void bind(TodoTask task) {
            textTaskTitle.setText(task.getTitle());
            textTaskDateTime.setText(task.getDueDate() + " " + task.getDueTime());
            checkboxComplete.setChecked(task.isCompleted());
            
            // Show/hide notification icon
            iconNotification.setVisibility(task.isHasReminder() ? View.VISIBLE : View.GONE);
            
            // Show/hide importance flag
            iconFlag.setVisibility(task.isImportant() ? View.VISIBLE : View.GONE);
            
            // Strike through text if completed
            if (task.isCompleted()) {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                textTaskTitle.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
            } else {
                textTaskTitle.setPaintFlags(textTaskTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                textTaskTitle.setTextColor(itemView.getContext().getColor(R.color.black));
            }
            
            // Click listeners
            taskBackground.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
            
            checkboxComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTaskComplete(task, isChecked);
                }
            });            
            // Reset item position on bind
            itemView.setTranslationX(0);
        }
    }
}
