package com.example.todolist.service.task;

import android.content.Context;
import com.example.todolist.model.SubTask;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.SubTaskRepository;

import java.util.List;

public class SubTaskService {
    
    public interface SubTaskOperationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    private Context context;
    private SubTaskRepository subTaskRepository;
    
    public SubTaskService(Context context) {
        this.context = context;
        this.subTaskRepository = new SubTaskRepository(context);
    }
    
    public void saveSubTask(String taskId, SubTask subTask, SubTaskOperationCallback callback) {
        subTaskRepository.saveSubTask(taskId, subTask, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    public void updateSubTask(String taskId, SubTask subTask, SubTaskOperationCallback callback) {
        subTaskRepository.updateSubTask(taskId, subTask, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    public void deleteSubTask(String taskId, String subTaskId, SubTaskOperationCallback callback) {
        // First get the subtask by id, then delete it
        subTaskRepository.getSubTasksByTaskId(taskId, new BaseRepository.RepositoryCallback<List<SubTask>>() {
            @Override
            public void onSuccess(List<SubTask> subTasks) {
                for (SubTask subTask : subTasks) {
                    if (subTask.getId().equals(subTaskId)) {
                        subTaskRepository.deleteSubTask(subTask, new BaseRepository.DatabaseCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                if (callback != null) {
                                    callback.onError(error);
                                }
                            }
                        });
                        return;
                    }
                }
                if (callback != null) {
                    callback.onError("SubTask not found");
                }
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    public void saveAllSubTasks(String taskId, List<SubTask> subTasks, SubTaskOperationCallback callback) {
        // Save each subtask individually
        if (subTasks == null || subTasks.isEmpty()) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }
        
        for (SubTask subTask : subTasks) {
            subTask.setTaskId(taskId);
            saveSubTask(taskId, subTask, callback);
        }
    }
    
    public void getSubTasks(String taskId, BaseRepository.ListCallback<SubTask> callback) {
        subTaskRepository.getSubTasksByTaskId(taskId, new BaseRepository.RepositoryCallback<List<SubTask>>() {
            @Override
            public void onSuccess(List<SubTask> subTasks) {
                callback.onSuccess(subTasks);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
