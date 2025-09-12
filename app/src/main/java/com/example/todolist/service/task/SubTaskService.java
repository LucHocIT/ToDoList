package com.example.todolist.service.task;

import android.content.Context;
import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.SubTaskRepository;
import com.example.todolist.service.TaskService;

import java.util.List;

public class SubTaskService {
    
    public interface SubTaskOperationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    private Context context;
    private SubTaskRepository subTaskRepository;
    private TaskService taskService; // Add TaskService reference
    
    public SubTaskService(Context context) {
        this.context = context;
        this.subTaskRepository = new SubTaskRepository(context);
    }
    
    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
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
    
    public void completeAllSubTasks(com.example.todolist.model.Task task) {
        if (task.getSubTasks() != null && !task.getSubTasks().isEmpty()) {
            for (com.example.todolist.model.SubTask subTask : task.getSubTasks()) {
                if (!subTask.isCompleted()) {
                    subTask.setCompleted(true);
                    updateSubTask(task.getId(), subTask, null);
                }
            }
        }
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

    public void loadSubTasksForTask(String taskId, SubTaskOperationCallback callback) {
        getSubTasks(taskId, new BaseRepository.ListCallback<SubTask>() {
            @Override
            public void onSuccess(List<SubTask> subTasks) {
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
    
    public void loadSubTasksForAllTasks() {

    }

    public void saveSubTaskThroughTask(Task task, SubTask subTask, SubTaskOperationCallback callback) {
        if (taskService == null) {
            saveSubTask(task.getId(), subTask, callback);
            return;
        }

        if (task.getSubTasks() == null) {
            task.setSubTasks(new java.util.ArrayList<>());
        }

        boolean found = false;
        for (int i = 0; i < task.getSubTasks().size(); i++) {
            if (task.getSubTasks().get(i).getId().equals(subTask.getId())) {
                task.getSubTasks().set(i, subTask);
                found = true;
                break;
            }
        }
        if (!found) {
            task.addSubTask(subTask);
        }

        taskService.updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    public void updateSubTaskThroughTask(Task task, SubTask subTask, SubTaskOperationCallback callback) {
        saveSubTaskThroughTask(task, subTask, callback);
    }
    
    public void deleteSubTaskThroughTask(Task task, String subTaskId, SubTaskOperationCallback callback) {
        if (taskService == null) {
            deleteSubTask(task.getId(), subTaskId, callback);
            return;
        }

        if (task.getSubTasks() != null) {
            task.getSubTasks().removeIf(subTask -> subTask.getId().equals(subTaskId));
        }
        taskService.updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
}
