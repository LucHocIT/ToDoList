package com.example.todolist.repository;

import android.content.Context;

import com.example.todolist.database.ToDoDatabase;
import com.example.todolist.database.dao.SubTaskDao;
import com.example.todolist.database.entity.SubTaskEntity;
import com.example.todolist.database.mapper.SubTaskMapper;
import com.example.todolist.model.SubTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SubTaskRepository extends BaseRepository {
    
    private SubTaskDao subTaskDao;
    
    public SubTaskRepository(Context context) {
        super();
        ToDoDatabase database = ToDoDatabase.getInstance(context);
        subTaskDao = database.subTaskDao();
    }
    
    public void saveSubTask(String taskId, SubTask subTask, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                // Generate ID if not set
                if (subTask.getId() == null || subTask.getId().isEmpty()) {
                    subTask.setId(UUID.randomUUID().toString());
                }
                
                // Set taskId and timestamp
                subTask.setTaskId(taskId);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                subTask.setCreatedAt(currentDate);
                
                SubTaskEntity entity = SubTaskMapper.toEntity(subTask);
                subTaskDao.insertSubTask(entity);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lưu subtask: " + e.getMessage()));
            }
        });
    }
    
    public void updateSubTask(String taskId, SubTask subTask, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                subTask.setTaskId(taskId);
                SubTaskEntity entity = SubTaskMapper.toEntity(subTask);
                subTaskDao.updateSubTask(entity);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi cập nhật subtask: " + e.getMessage()));
            }
        });
    }
    
    public void deleteSubTask(SubTask subTask, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                SubTaskEntity entity = SubTaskMapper.toEntity(subTask);
                subTaskDao.deleteSubTask(entity);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi xóa subtask: " + e.getMessage()));
            }
        });
    }
    
    public void getSubTasksByTaskId(String taskId, RepositoryCallback<List<SubTask>> callback) {
        executeAsync(() -> {
            try {
                List<SubTaskEntity> entities = subTaskDao.getSubTasksByTaskId(taskId);
                List<SubTask> subTasks = SubTaskMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(subTasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy subtasks: " + e.getMessage()));
            }
        });
    }
    
    public void deleteSubTasksByTaskId(String taskId, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                subTaskDao.deleteSubTasksByTaskId(taskId);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi xóa subtasks theo task: " + e.getMessage()));
            }
        });
    }
    
    public void updateSubTaskCompletion(String subTaskId, boolean isCompleted, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                subTaskDao.updateSubTaskCompletion(subTaskId, isCompleted);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi cập nhật hoàn thành subtask: " + e.getMessage()));
            }
        });
    }
}
