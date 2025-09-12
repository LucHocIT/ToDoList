package com.example.todolist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.model.SubTask;
import java.util.List;

public class SubTaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SUBTASK = 0;
    private static final int TYPE_ADD_NEW = 1;
    
    private List<SubTask> subTasks;
    private OnSubTaskListener listener;
    private boolean isTaskCompleted = false;
    
    public interface OnSubTaskListener {
        void onSubTaskStatusChanged(SubTask subTask, boolean isCompleted);
        void onSubTaskTextChanged(SubTask subTask, String newText);
        void onSubTaskDeleted(SubTask subTask);
        void onAddNewSubTask();
    }
    
    public SubTaskAdapter(List<SubTask> subTasks, OnSubTaskListener listener) {
        this.subTasks = subTasks;
        this.listener = listener;
    }
    
    public void setTaskCompleted(boolean isCompleted) {
        this.isTaskCompleted = isCompleted;
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemViewType(int position) {
        return TYPE_SUBTASK; 
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subtask, parent, false);
        return new SubTaskViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        android.util.Log.d("SubTaskAdapter", "onBindViewHolder: position=" + position);
        SubTask subTask = subTasks.get(position);
        android.util.Log.d("SubTaskAdapter", "onBindViewHolder: binding subtask=" + subTask.getTitle());
        ((SubTaskViewHolder) holder).bind(subTask);
    }
    
    @Override
    public int getItemCount() {
        int count = subTasks.size();
        android.util.Log.d("SubTaskAdapter", "getItemCount: " + count);
        return count;
    }    class SubTaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBoxSubTask;
        EditText editTextSubTask;
        ImageView btnDeleteSubTask;
        
        public SubTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBoxSubTask = itemView.findViewById(R.id.checkBox_subtask);
            editTextSubTask = itemView.findViewById(R.id.editText_subtask);
            btnDeleteSubTask = itemView.findViewById(R.id.btn_delete_subtask);
        }
        
        public void bind(SubTask subTask) {
            checkBoxSubTask.setChecked(subTask.isCompleted());
            editTextSubTask.setText(subTask.getTitle());
            
            // FORCE sử dụng drawable tùy chỉnh - loại bỏ mọi tint
            checkBoxSubTask.setButtonDrawable(R.drawable.subtask_checkbox_selector);
            checkBoxSubTask.setButtonTintList(null);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                checkBoxSubTask.setButtonTintMode(null);
            }
            // Clear any background tint
            checkBoxSubTask.setBackgroundTintList(null);
            
            editTextSubTask.setEnabled(!isTaskCompleted);
            btnDeleteSubTask.setEnabled(!isTaskCompleted);

            float alpha = isTaskCompleted ? 0.6f : (subTask.isCompleted() ? 0.6f : 1.0f);
            editTextSubTask.setAlpha(alpha);
            btnDeleteSubTask.setAlpha(alpha);
            
            // Set text style based on completion status
            if (subTask.isCompleted()) {
                editTextSubTask.setPaintFlags(editTextSubTask.getPaintFlags() | 
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                editTextSubTask.setPaintFlags(editTextSubTask.getPaintFlags() & 
                    (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }
            
            checkBoxSubTask.setOnCheckedChangeListener(null);
            checkBoxSubTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isTaskCompleted && listener != null) {
                    listener.onSubTaskStatusChanged(subTask, isChecked);
                } else if (isTaskCompleted) {
                    buttonView.setChecked(subTask.isCompleted());
                }
            });
            
            // Text change listener (chỉ hoạt động khi task chưa hoàn thành)
            editTextSubTask.setOnFocusChangeListener(null);
            if (!isTaskCompleted) {
                editTextSubTask.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus && listener != null) {
                        String newText = editTextSubTask.getText().toString().trim();
                        if (!newText.equals(subTask.getTitle())) {
                            listener.onSubTaskTextChanged(subTask, newText);
                        }
                    }
                });
            }
            
            // Delete button listener (chỉ hoạt động khi task chưa hoàn thành)
            btnDeleteSubTask.setOnClickListener(null);
            if (!isTaskCompleted) {
                btnDeleteSubTask.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSubTaskDeleted(subTask);
                    }
                });
            }
        }
    }
    
    class AddNewViewHolder extends RecyclerView.ViewHolder {
        View btnAddSubTask;
        
        public AddNewViewHolder(@NonNull View itemView) {
            super(itemView);
            btnAddSubTask = itemView.findViewById(R.id.btn_add_subtask);
        }
        
        public void bind() {
            // Disable add button nếu task chính đã hoàn thành
            btnAddSubTask.setEnabled(!isTaskCompleted);
            btnAddSubTask.setAlpha(isTaskCompleted ? 0.6f : 1.0f);
            
            if (!isTaskCompleted) {
                btnAddSubTask.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddNewSubTask();
                    }
                });
            } else {
                btnAddSubTask.setOnClickListener(null);
            }
        }
    }
}
