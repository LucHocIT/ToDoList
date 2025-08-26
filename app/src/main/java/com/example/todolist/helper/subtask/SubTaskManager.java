package com.example.todolist.helper.subtask;

import android.content.Context;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.adapter.SubTaskAdapter;
import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;
import com.example.todolist.service.task.SubTaskService;
import com.example.todolist.helper.taskdetail.TaskDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SubTaskManager implements SubTaskAdapter.OnSubTaskListener {
    
    private Context context;
    private RecyclerView recyclerSubTasks;
    private SubTaskAdapter subTaskAdapter;
    private SubTaskService subTaskService;
    private TaskDataManager taskDataManager;
    private Task currentTask;
    private SubTaskManagerCallback callback;
    
    public interface SubTaskManagerCallback {
        void showToast(String message);
        void onTaskUpdated(Task task);
    }
    
    public SubTaskManager(Context context, RecyclerView recyclerSubTasks, 
                         TaskDataManager taskDataManager, SubTaskManagerCallback callback) {
        this.context = context;
        this.recyclerSubTasks = recyclerSubTasks;
        this.taskDataManager = taskDataManager;
        this.callback = callback;
        this.subTaskService = new SubTaskService(context);
        setupSubTaskRecyclerView();
    }

    private void setupSubTaskRecyclerView() {
        subTaskAdapter = new SubTaskAdapter(new ArrayList<>(), this);
        recyclerSubTasks.setLayoutManager(new LinearLayoutManager(context));
        recyclerSubTasks.setAdapter(subTaskAdapter);
    }

    public void setCurrentTask(Task task) {
        this.currentTask = task;
        if (task != null) {
            subTaskAdapter.setTaskCompleted(task.isCompleted());
            refreshSubTasks();
        }
    }

    public void loadSubTasksFromDatabase() {
        if (currentTask != null && currentTask.getId() != null) {
            subTaskService.getSubTasks(currentTask.getId(), new com.example.todolist.repository.BaseRepository.ListCallback<SubTask>() {
                @Override
                public void onSuccess(List<SubTask> subTasks) {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            currentTask.setSubTasks(subTasks);
                            refreshSubTasks();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (currentTask.getSubTasks() == null) {
                                currentTask.setSubTasks(new ArrayList<>());
                            }
                            refreshSubTasks();
                        });
                    }
                }
            });
        } else {
            refreshSubTasks();
        }
    }

    public void onTaskCompletionChanged(boolean isCompleted) {
        // Disable subtasks when task is completed
        if (recyclerSubTasks != null) {
            recyclerSubTasks.setEnabled(!isCompleted);
            recyclerSubTasks.setAlpha(isCompleted ? 0.6f : 1.0f);
        }

        if (isCompleted && currentTask != null && currentTask.getSubTasks() != null) {
            for (SubTask subTask : currentTask.getSubTasks()) {
                if (!subTask.isCompleted()) {
                    subTask.setCompleted(true);
                    // Update each subtask in Firebase
                    subTaskService.updateSubTask(currentTask.getId(), subTask, null);
                }
            }
        }

        if (subTaskAdapter != null) {
            subTaskAdapter.setTaskCompleted(isCompleted);
            subTaskAdapter.notifyDataSetChanged();
        }
    }

    public void cleanupEmptySubTasks() {
        if (currentTask != null && currentTask.getSubTasks() != null) {
            List<SubTask> emptySubTasks = SubTaskUtils.getEmptySubTasks(currentTask.getSubTasks());

            for (SubTask emptySubTask : emptySubTasks) {
                subTaskService.deleteSubTask(currentTask.getId(), emptySubTask.getId(), null);
                currentTask.removeSubTask(emptySubTask);
            }
            
            if (!emptySubTasks.isEmpty() && callback != null) {
                callback.onTaskUpdated(currentTask);
            }
        }
    }

    public void markAllSubTasksAsCompleted() {
        if (currentTask != null) {
            List<SubTask> modifiedSubTasks = SubTaskUtils.markAllSubTasksAsCompleted(currentTask);
            
            if (!modifiedSubTasks.isEmpty()) {
                android.util.Log.d("SubTaskManager", "Auto-completing " + modifiedSubTasks.size() + " subtasks");
                
                for (SubTask subTask : modifiedSubTasks) {
                    android.util.Log.d("SubTaskManager", "Completing subtask: " + subTask.getTitle());
                    // Update each subtask in Firebase
                    subTaskService.updateSubTask(currentTask.getId(), subTask, null);
                }

                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> refreshSubTasks());
                }
            }
        }
    }

    private void refreshSubTasks() {
        if (subTaskAdapter != null && currentTask != null) {
            subTaskAdapter = new SubTaskAdapter(currentTask.getSubTasks(), this);
            if (currentTask != null) {
                subTaskAdapter.setTaskCompleted(currentTask.isCompleted());
            }
            recyclerSubTasks.setAdapter(subTaskAdapter);
        }
    }

    @Override
    public void onSubTaskStatusChanged(SubTask subTask, boolean isCompleted) {
        subTask.setCompleted(isCompleted);
        if (currentTask != null) {
            subTaskService.updateSubTask(currentTask.getId(), subTask, new SubTaskService.SubTaskOperationCallback() {
                @Override
                public void onSuccess() {
                    // Update task in database
                    if (callback != null) {
                        callback.onTaskUpdated(currentTask);
                    }
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> subTaskAdapter.notifyDataSetChanged());
                    }
                }

                @Override
                public void onError(String error) {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (callback != null) {
                                callback.showToast("Lỗi cập nhật subtask: " + error);
                            }
                            // Revert changes
                            subTask.setCompleted(!isCompleted);
                            subTaskAdapter.notifyDataSetChanged();
                        });
                    }
                }
            });
        }
    }
    
    @Override
    public void onSubTaskTextChanged(SubTask subTask, String newText) {
        if (!newText.isEmpty()) {
            subTask.setTitle(newText);
            if (currentTask != null) {
                subTaskService.saveSubTask(currentTask.getId(), subTask, new SubTaskService.SubTaskOperationCallback() {
                    @Override
                    public void onSuccess() {
                        // Update task in database
                        if (callback != null) {
                            callback.onTaskUpdated(currentTask);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                if (callback != null) {
                                    callback.showToast("Lỗi cập nhật subtask: " + error);
                                }
                            });
                        }
                    }
                });
            }
        } else {
            onSubTaskDeleted(subTask);
        }
    }
    
    @Override
    public void onSubTaskDeleted(SubTask subTask) {
        if (currentTask != null) {
            // Delete from Firebase first
            subTaskService.deleteSubTask(currentTask.getId(), subTask.getId(), new SubTaskService.SubTaskOperationCallback() {
                @Override
                public void onSuccess() {
                    // Remove from local task
                    currentTask.removeSubTask(subTask);
                    if (callback != null) {
                        callback.onTaskUpdated(currentTask);
                    }
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> subTaskAdapter.notifyDataSetChanged());
                    }
                }

                @Override
                public void onError(String error) {
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (callback != null) {
                                callback.showToast("Lỗi xóa subtask: " + error);
                            }
                        });
                    }
                }
            });
        }
    }
    
    @Override
    public void onAddNewSubTask() {
        if (currentTask != null) {
            SubTask newSubTask = new SubTask("", currentTask.getId());
            newSubTask.setId(UUID.randomUUID().toString());
            currentTask.addSubTask(newSubTask);
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> subTaskAdapter.notifyDataSetChanged());
            }
        }
    }

    public SubTaskAdapter getSubTaskAdapter() {
        return subTaskAdapter;
    }

    public boolean areAllSubTasksCompleted() {
        return SubTaskUtils.areAllSubTasksCompleted(currentTask);
    }

    public String getSubTasksInfo() {
        return SubTaskUtils.getSubTasksProgressInfo(currentTask);
    }

    public float getSubTasksCompletionPercentage() {
        return SubTaskUtils.getSubTasksCompletionPercentage(currentTask);
    }
}
