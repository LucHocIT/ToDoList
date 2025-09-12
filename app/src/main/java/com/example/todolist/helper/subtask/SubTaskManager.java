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

    public void cleanupEmptySubTasks() {
        if (currentTask != null && currentTask.getSubTasks() != null) {
            List<SubTask> emptySubTasks = SubTaskUtils.getEmptySubTasks(currentTask.getSubTasks());

            if (!emptySubTasks.isEmpty()) {
                for (SubTask emptySubTask : emptySubTasks) {
                    currentTask.removeSubTask(emptySubTask);
                }
                
                // Update the entire task through proper sync
                if (callback != null) {
                    callback.onTaskUpdated(currentTask);
                }
            }
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
        if (subTaskAdapter != null && currentTask != null) {
            // Update the adapter's data instead of creating a new adapter
            List<SubTask> subTasks = currentTask.getSubTasks() != null ? currentTask.getSubTasks() : new ArrayList<>();
            android.util.Log.d("SubTaskManager", "refreshSubTasks: updating adapter with " + subTasks.size() + " subtasks");
            for (int i = 0; i < subTasks.size(); i++) {
                SubTask st = subTasks.get(i);
                android.util.Log.d("SubTaskManager", "refreshSubTasks: subtask[" + i + "] = title:'" + st.getTitle() + "', completed:" + st.isCompleted() + ", id:" + st.getId());
            }
            
            subTaskAdapter = new SubTaskAdapter(subTasks, this);
            subTaskAdapter.setTaskCompleted(currentTask.isCompleted());
            recyclerSubTasks.setAdapter(subTaskAdapter);
            android.util.Log.d("SubTaskManager", "refreshSubTasks: adapter set to RecyclerView");
        } else {
            android.util.Log.d("SubTaskManager", "refreshSubTasks: subTaskAdapter=" + (subTaskAdapter != null) + ", currentTask=" + (currentTask != null));
        }
    }

    @Override
    public void onSubTaskStatusChanged(SubTask subTask, boolean isCompleted) {
        android.util.Log.d("SubTaskManager", "onSubTaskStatusChanged: subTask.id=" + subTask.getId() + ", title='" + subTask.getTitle() + "', isCompleted=" + isCompleted);
        
        // Simple update like AddTaskHandler
        subTask.setCompleted(isCompleted);
        android.util.Log.d("SubTaskManager", "onSubTaskStatusChanged: set completed status, now subTask.isCompleted()=" + subTask.isCompleted());
        
        // Save to database via callback
        if (callback != null && currentTask != null) {
            android.util.Log.d("SubTaskManager", "onSubTaskStatusChanged: calling callback.onTaskUpdated");
            callback.onTaskUpdated(currentTask);
        }
        
        // Update UI
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> {
                android.util.Log.d("SubTaskManager", "onSubTaskStatusChanged: calling notifyDataSetChanged");
                if (subTaskAdapter != null) {
                    subTaskAdapter.notifyDataSetChanged();
                }
            });
        }
    }
    
    @Override
    public void onSubTaskTextChanged(SubTask subTask, String newText) {
        android.util.Log.d("SubTaskManager", "onSubTaskTextChanged: " + newText);
        if (!newText.isEmpty()) {
            // Simple update like AddTaskHandler
            subTask.setTitle(newText);
            android.util.Log.d("SubTaskManager", "onSubTaskTextChanged: updated subtask title");
            
            // Save to database via callback
            if (callback != null && currentTask != null) {
                android.util.Log.d("SubTaskManager", "onSubTaskTextChanged: calling callback.onTaskUpdated");
                callback.onTaskUpdated(currentTask);
            }
        } else {
            onSubTaskDeleted(subTask);
        }
    }
    
    @Override
    public void onSubTaskDeleted(SubTask subTask) {
        android.util.Log.d("SubTaskManager", "onSubTaskDeleted: deleting subtask with id=" + subTask.getId());
        
        if (currentTask != null && currentTask.getSubTasks() != null) {
            int position = currentTask.getSubTasks().indexOf(subTask);
            if (position != -1) {
                currentTask.getSubTasks().remove(position);
                android.util.Log.d("SubTaskManager", "onSubTaskDeleted: removed from currentTask, now has " + currentTask.getSubTasks().size() + " subtasks");
                
                // Update adapter like AddTaskHandler
                if (subTaskAdapter != null) {
                    subTaskAdapter.notifyItemRemoved(position);
                }
                
                // Save to database via callback
                if (callback != null) {
                    callback.onTaskUpdated(currentTask);
                }
            }
        }
    }
    
    @Override
    public void onAddNewSubTask() {
        android.util.Log.d("SubTaskManager", "onAddNewSubTask called");
        
        // Create new SubTask with temp ID like AddTaskHandler
        SubTask newSubTask = new SubTask();
        newSubTask.setTitle("");
        newSubTask.setId("temp_" + System.currentTimeMillis());
        android.util.Log.d("SubTaskManager", "onAddNewSubTask: created new subtask with ID=" + newSubTask.getId());
        
        // Make the RecyclerView visible if needed
        if (recyclerSubTasks != null && recyclerSubTasks.getVisibility() == View.GONE) {
            recyclerSubTasks.setVisibility(View.VISIBLE);
        }
        
        // Add to current task's subtasks - ENSURE PROPER LIST INITIALIZATION
        if (currentTask != null) {
            if (currentTask.getSubTasks() == null) {
                currentTask.setSubTasks(new ArrayList<>());
                android.util.Log.d("SubTaskManager", "onAddNewSubTask: initialized new SubTasks list");
            }
            
            android.util.Log.d("SubTaskManager", "onAddNewSubTask: before adding, currentTask has " + currentTask.getSubTasks().size() + " subtasks");
            currentTask.getSubTasks().add(newSubTask);
            android.util.Log.d("SubTaskManager", "onAddNewSubTask: after adding, currentTask has " + currentTask.getSubTasks().size() + " subtasks");
            
            // Refresh adapter immediately with new data like AddTaskHandler
            if (subTaskAdapter != null) {
                android.util.Log.d("SubTaskManager", "onAddNewSubTask: refreshing adapter with notifyDataSetChanged");
                refreshSubTasks();
            }
            
            // Save to database via callback
            if (callback != null) {
                android.util.Log.d("SubTaskManager", "onAddNewSubTask: calling callback.onTaskUpdated");
                callback.onTaskUpdated(currentTask);
            }
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
