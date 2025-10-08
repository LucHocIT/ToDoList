package com.example.todolist.helper.subtask;

import android.content.Context;
import android.view.View;
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
        android.util.Log.d("SubTaskManager", "loadSubTasksFromDatabase: currentTask=" + (currentTask != null ? currentTask.getId() : "null"));
        if (currentTask != null && currentTask.getId() != null) {
            subTaskService.getSubTasks(currentTask.getId(), new com.example.todolist.repository.BaseRepository.ListCallback<SubTask>() {
                @Override
                public void onSuccess(List<SubTask> subTasks) {
                    android.util.Log.d("SubTaskManager", "loadSubTasksFromDatabase onSuccess: found " + subTasks.size() + " subtasks");
                    for (int i = 0; i < subTasks.size(); i++) {
                        SubTask st = subTasks.get(i);
                        android.util.Log.d("SubTaskManager", "loadSubTasksFromDatabase: subtask[" + i + "] = title:'" + st.getTitle() + "', completed:" + st.isCompleted() + ", id:" + st.getId());
                    }
                    
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            currentTask.setSubTasks(subTasks);
                            android.util.Log.d("SubTaskManager", "loadSubTasksFromDatabase: set subtasks to currentTask, now has " + currentTask.getSubTasks().size() + " subtasks");
                            refreshSubTasks();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    android.util.Log.d("SubTaskManager", "loadSubTasksFromDatabase onError: " + error);
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
            android.util.Log.d("SubTaskManager", "loadSubTasksFromDatabase: currentTask is null or has no ID");
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
                }
            }
            if (callback != null) {
                callback.onTaskUpdated(currentTask);
            }
        }

        if (subTaskAdapter != null) {
            subTaskAdapter.setTaskCompleted(isCompleted);
            subTaskAdapter.notifyDataSetChanged();
        }
    }

    public void markAllSubTasksAsCompleted() {
        if (currentTask != null) {
            List<SubTask> modifiedSubTasks = SubTaskUtils.markAllSubTasksAsCompleted(currentTask);
            
            if (!modifiedSubTasks.isEmpty()) {
                if (callback != null) {
                    callback.onTaskUpdated(currentTask);
                }

                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> refreshSubTasks());
                }
            }
        }
    }

    private void refreshSubTasks() {
        android.util.Log.d("SubTaskManager", "refreshSubTasks called");
        if (currentTask != null) {
            List<SubTask> subTasks = currentTask.getSubTasks() != null ? currentTask.getSubTasks() : new ArrayList<>();
            android.util.Log.d("SubTaskManager", "refreshSubTasks: updating adapter with " + subTasks.size() + " subtasks");
            for (int i = 0; i < subTasks.size(); i++) {
                SubTask st = subTasks.get(i);
                android.util.Log.d("SubTaskManager", "refreshSubTasks: subtask[" + i + "] = title:'" + st.getTitle() + "', completed:" + st.isCompleted() + ", id:" + st.getId());
            }
            
            if (subTaskAdapter == null) {
                // Create new adapter only if it doesn't exist
                subTaskAdapter = new SubTaskAdapter(subTasks, this);
                subTaskAdapter.setTaskCompleted(currentTask.isCompleted());
                recyclerSubTasks.setAdapter(subTaskAdapter);
                android.util.Log.d("SubTaskManager", "refreshSubTasks: created new adapter");
            } else {
                // Update existing adapter's data
                subTaskAdapter.updateSubTasks(subTasks);
                subTaskAdapter.setTaskCompleted(currentTask.isCompleted());
                subTaskAdapter.notifyDataSetChanged();
                android.util.Log.d("SubTaskManager", "refreshSubTasks: updated existing adapter");
            }
        } else {
            android.util.Log.d("SubTaskManager", "refreshSubTasks: currentTask is null");
        }
    }

    @Override
    public void onSubTaskStatusChanged(SubTask subTask, boolean isCompleted) {
        android.util.Log.d("SubTaskManager", "onSubTaskStatusChanged: subTask.id=" + subTask.getId() + ", title='" + subTask.getTitle() + "', isCompleted=" + isCompleted);
        
        subTask.setCompleted(isCompleted);
        android.util.Log.d("SubTaskManager", "onSubTaskStatusChanged: set completed status, now subTask.isCompleted()=" + subTask.isCompleted());
        
        if (currentTask != null) {
            // Save directly to SubTask database instead of through Task update
            subTaskService.updateSubTask(currentTask.getId(), subTask, new SubTaskService.SubTaskOperationCallback() {
                @Override
                public void onSuccess() {
                    android.util.Log.d("SubTaskManager", "onSubTaskStatusChanged: saved successfully to database");
                    // Update UI on success
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            android.util.Log.d("SubTaskManager", "onSubTaskStatusChanged: calling notifyDataSetChanged");
                            if (subTaskAdapter != null) {
                                subTaskAdapter.notifyDataSetChanged();
                            }
                            if (callback != null) {
                                callback.onTaskUpdated(currentTask);
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("SubTaskManager", "onSubTaskStatusChanged: error saving to database: " + error);
                    // Revert local change on error
                    subTask.setCompleted(!isCompleted);
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (subTaskAdapter != null) {
                                subTaskAdapter.notifyDataSetChanged();
                            }
                            if (callback != null) {
                                callback.showToast("Lỗi cập nhật subtask: " + error);
                            }
                        });
                    }
                }
            });
        }
    }
    
    @Override
    public void onSubTaskTextChanged(SubTask subTask, String newText) {
        android.util.Log.d("SubTaskManager", "onSubTaskTextChanged: " + newText);
        
        subTask.setTitle(newText);
        android.util.Log.d("SubTaskManager", "onSubTaskTextChanged: updated subtask title");
        
        if (currentTask != null) {
            boolean isNewSubTask = subTask.getId().startsWith("temp_");
            
            if (isNewSubTask) {
                // Generate proper ID for new subtask
                subTask.setId(currentTask.getId() + "_subtask_" + System.currentTimeMillis() + "_" + ((int)(Math.random() * 10000)));
                android.util.Log.d("SubTaskManager", "onSubTaskTextChanged: new subtask, generated ID=" + subTask.getId());
                
                // Save new subtask to database
                subTaskService.saveSubTask(currentTask.getId(), subTask, new SubTaskService.SubTaskOperationCallback() {
                    @Override
                    public void onSuccess() {
                        android.util.Log.d("SubTaskManager", "onSubTaskTextChanged: new subtask saved successfully to database");
                        
                        if (callback != null) {
                            callback.onTaskUpdated(currentTask);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("SubTaskManager", "onSubTaskTextChanged: error saving new subtask to database: " + error);
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                if (callback != null) {
                                    callback.showToast("Lỗi lưu subtask: " + error);
                                }
                            });
                        }
                    }
                });
            } else {
                // Update existing subtask
                subTaskService.updateSubTask(currentTask.getId(), subTask, new SubTaskService.SubTaskOperationCallback() {
                    @Override
                    public void onSuccess() {
                        android.util.Log.d("SubTaskManager", "onSubTaskTextChanged: existing subtask updated successfully to database");
                        
                        if (callback != null) {
                            callback.onTaskUpdated(currentTask);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("SubTaskManager", "onSubTaskTextChanged: error updating subtask to database: " + error);
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
        }
    }
    
    @Override
    public void onSubTaskDeleted(SubTask subTask) {
        android.util.Log.d("SubTaskManager", "onSubTaskDeleted: deleting subtask with id=" + subTask.getId());
        
        if (currentTask != null && currentTask.getSubTasks() != null) {
            int position = currentTask.getSubTasks().indexOf(subTask);
            if (position != -1) {
                // Remove from local list first
                currentTask.getSubTasks().remove(position);
                android.util.Log.d("SubTaskManager", "onSubTaskDeleted: removed from currentTask, now has " + currentTask.getSubTasks().size() + " subtasks");
                
                // Update adapter immediately
                if (subTaskAdapter != null) {
                    subTaskAdapter.notifyItemRemoved(position);
                }
                
                // Check if this is a temp subtask (not saved to database yet)
                if (subTask.getId().startsWith("temp_")) {
                    android.util.Log.d("SubTaskManager", "onSubTaskDeleted: temp subtask, no need to delete from database");
                    // Just remove from UI, no need to delete from database
                    if (callback != null) {
                        callback.onTaskUpdated(currentTask);
                    }
                    return;
                }
                
                // Delete from SubTask database
                subTaskService.deleteSubTask(currentTask.getId(), subTask.getId(), new SubTaskService.SubTaskOperationCallback() {
                    @Override
                    public void onSuccess() {
                        android.util.Log.d("SubTaskManager", "onSubTaskDeleted: deleted successfully from database");

                        if (callback != null) {
                            callback.onTaskUpdated(currentTask);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("SubTaskManager", "onSubTaskDeleted: error deleting from database: " + error);
                        // Restore the subtask on error
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                currentTask.getSubTasks().add(position, subTask);
                                if (subTaskAdapter != null) {
                                    subTaskAdapter.notifyItemInserted(position);
                                }
                                if (callback != null) {
                                    callback.showToast("Lỗi xóa subtask: " + error);
                                }
                            });
                        }
                    }
                });
            }
        }
    }
    
    @Override
    public void onAddNewSubTask() {
        android.util.Log.d("SubTaskManager", "onAddNewSubTask called");
        
        SubTask newSubTask = new SubTask();
        newSubTask.setTitle("");
        newSubTask.setId("temp_" + System.currentTimeMillis());
        android.util.Log.d("SubTaskManager", "onAddNewSubTask: created new subtask with ID=" + newSubTask.getId());
        
        // Make the RecyclerView visible if needed
        if (recyclerSubTasks != null && recyclerSubTasks.getVisibility() == View.GONE) {
            recyclerSubTasks.setVisibility(View.VISIBLE);
        }
        
        if (currentTask != null) {
            if (currentTask.getSubTasks() == null) {
                currentTask.setSubTasks(new ArrayList<>());
                android.util.Log.d("SubTaskManager", "onAddNewSubTask: initialized new SubTasks list");
            }
            
            // Set taskId for the new subtask
            newSubTask.setTaskId(currentTask.getId());
            
            android.util.Log.d("SubTaskManager", "onAddNewSubTask: before adding, currentTask has " + currentTask.getSubTasks().size() + " subtasks");
            currentTask.getSubTasks().add(newSubTask);
            android.util.Log.d("SubTaskManager", "onAddNewSubTask: after adding, currentTask has " + currentTask.getSubTasks().size() + " subtasks");
            
            if (subTaskAdapter != null) {
                android.util.Log.d("SubTaskManager", "onAddNewSubTask: notifying adapter about new item");
                subTaskAdapter.updateSubTasks(currentTask.getSubTasks());
                subTaskAdapter.notifyItemInserted(currentTask.getSubTasks().size() - 1);
            }
            android.util.Log.d("SubTaskManager", "onAddNewSubTask: subtask created in memory, will be saved when user enters text");
            
        } else {
            android.util.Log.d("SubTaskManager", "onAddNewSubTask: currentTask is null");
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
