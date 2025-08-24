package com.example.todolist.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.model.Category;
import com.example.todolist.service.TaskService;
import com.example.todolist.repository.BaseRepository;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_ADD_NEW = 1;
    
    private List<Category> categories;
    private OnCategoryClickListener listener;
    private TaskService taskService;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
        void onCategoryMenuClick(Category category, View anchorView);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }
    
    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public int getItemViewType(int position) {
        return "-1".equals(categories.get(position).getId()) ? TYPE_ADD_NEW : TYPE_CATEGORY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD_NEW) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_category, parent, false);
            return new AddNewViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Category category = categories.get(position);
        if (holder instanceof CategoryViewHolder) {
            ((CategoryViewHolder) holder).bind(category);
        } else if (holder instanceof AddNewViewHolder) {
            ((AddNewViewHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvTaskCount;
        ImageView imgCategoryIcon, btnCategoryMenu, imgDragHandle;
        FrameLayout iconBackground;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvTaskCount = itemView.findViewById(R.id.tv_task_count);
            imgCategoryIcon = itemView.findViewById(R.id.img_category_icon);
            btnCategoryMenu = itemView.findViewById(R.id.btn_category_menu);
            imgDragHandle = itemView.findViewById(R.id.img_drag_handle);
            iconBackground = (FrameLayout) imgCategoryIcon.getParent();
        }

        public void bind(Category category) {
            tvCategoryName.setText(category.getName());
            updateTaskCount(category);
            
            // Set background color
            try {
                iconBackground.getBackground().setTint(Color.parseColor(category.getColor()));
            } catch (Exception e) {
                iconBackground.getBackground().setTint(Color.parseColor("#4285F4"));
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
            
            btnCategoryMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryMenuClick(category, v);
                }
            });
        }
        
        private void updateTaskCount(Category category) {
            if (taskService != null) {
                taskService.getTasksByCategory(category.getId(), new BaseRepository.RepositoryCallback<List<com.example.todolist.model.Task>>() {
                    @Override
                    public void onSuccess(List<com.example.todolist.model.Task> tasks) {
                        // Count only uncompleted tasks
                        int activeTaskCount = 0;
                        for (com.example.todolist.model.Task task : tasks) {
                            if (!task.isCompleted()) {
                                activeTaskCount++;
                            }
                        }
                        final int finalActiveTaskCount = activeTaskCount;
                        
                        // Update UI on main thread
                        if (tvTaskCount != null) {
                            tvTaskCount.post(() -> {
                                String taskCountText = finalActiveTaskCount + " nhiệm vụ";
                                tvTaskCount.setText(taskCountText);
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (tvTaskCount != null) {
                            tvTaskCount.post(() -> tvTaskCount.setText("0 nhiệm vụ"));
                        }
                    }
                });
            } else {
                tvTaskCount.setText("0 nhiệm vụ");
            }
        }
    }

    class AddNewViewHolder extends RecyclerView.ViewHolder {
        public AddNewViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind() {
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    Category addNewCategory = new Category();
                    addNewCategory.setId("-1");
                    addNewCategory.setName("");
                    listener.onCategoryClick(addNewCategory);
                }
            });
        }
    }
}
