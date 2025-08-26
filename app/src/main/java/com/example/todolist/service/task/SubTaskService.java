package com.example.todolist.service.task;

import android.content.Context;
import com.example.todolist.model.SubTask;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.task.SubTaskRepository;

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
        this.subTaskRepository = new SubTaskRepository();
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
        subTaskRepository.deleteSubTask(taskId, subTaskId, new BaseRepository.DatabaseCallback<Boolean>() {
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
    
    public void saveAllSubTasks(String taskId, List<SubTask> subTasks, SubTaskOperationCallback callback) {
        subTaskRepository.saveAllSubTasks(taskId, subTasks, new BaseRepository.DatabaseCallback<Boolean>() {
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
    
    public void getSubTasks(String taskId, BaseRepository.ListCallback<SubTask> callback) {
        subTaskRepository.getSubTasks(taskId, callback);
    }
}
