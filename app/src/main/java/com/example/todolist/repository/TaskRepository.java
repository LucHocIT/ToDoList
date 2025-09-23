package com.example.todolist.repository;

import android.content.Context;

import com.example.todolist.database.ToDoDatabase;
import com.example.todolist.database.dao.TaskDao;
import com.example.todolist.database.dao.SubTaskDao;
import com.example.todolist.database.entity.TaskEntity;
import com.example.todolist.database.entity.SubTaskEntity;
import com.example.todolist.database.mapper.TaskMapper;
import com.example.todolist.database.mapper.SubTaskMapper;
import com.example.todolist.model.Task;
import com.example.todolist.model.SubTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TaskRepository extends BaseRepository {
    
    private TaskDao taskDao;
    private SubTaskDao subTaskDao;
    
    public TaskRepository(Context context) {
        super();
        ToDoDatabase database = ToDoDatabase.getInstance(context);
        taskDao = database.taskDao();
        subTaskDao = database.subTaskDao();
    }
    
    // === CRUD OPERATIONS ===
    
    public void addTask(Task task, DatabaseCallback<String> callback) {
        executeAsync(() -> {
            try {
                // Generate ID if not set
                if (task.getId() == null || task.getId().isEmpty()) {
                    task.setId(UUID.randomUUID().toString());
                }
                
                // Set timestamps
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                task.setCreatedAt(currentDate);
                task.setUpdatedAt(currentDate);
                
                TaskEntity entity = TaskMapper.toEntity(task);
                taskDao.insertTask(entity);
                runOnMainThread(() -> callback.onSuccess(entity.id));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi thêm task: " + e.getMessage()));
            }
        });
    }
    
    public void updateTask(Task task, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                // Update timestamp
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                task.setUpdatedAt(currentDate);
                
                TaskEntity entity = TaskMapper.toEntity(task);
                taskDao.updateTask(entity);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi cập nhật task: " + e.getMessage()));
            }
        });
    }
    
    public void deleteTask(Task task, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                TaskEntity entity = TaskMapper.toEntity(task);
                taskDao.deleteTask(entity);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi xóa task: " + e.getMessage()));
            }
        });
    }
    
    public void getTaskById(String taskId, RepositoryCallback<Task> callback) {
        executeAsync(() -> {
            try {
                TaskEntity entity = taskDao.getTaskById(taskId);
                Task task = TaskMapper.fromEntity(entity);
                runOnMainThread(() -> callback.onSuccess(task));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy task: " + e.getMessage()));
            }
        });
    }
    
    public void updateTaskCompletion(String taskId, boolean isCompleted, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                String completionDate = isCompleted ? currentDate : null;
                
                taskDao.updateTaskCompletion(taskId, isCompleted, completionDate, currentDate);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi cập nhật hoàn thành: " + e.getMessage()));
            }
        });
    }
    
    public void updateTaskImportance(String taskId, boolean isImportant, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                
                taskDao.updateTaskImportance(taskId, isImportant, currentDate);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi cập nhật quan trọng: " + e.getMessage()));
            }
        });
    }
    
    // === QUERY OPERATIONS ===
    
    public void getAllTasks(ListCallback<Task> callback) {
        executeAsync(() -> {
            try {
                List<TaskEntity> entities = taskDao.getAllTasks();
                List<Task> tasks = TaskMapper.fromEntities(entities);

                for (Task task : tasks) {
                    if (task.getId() != null) {
                        List<SubTaskEntity> subTaskEntities = subTaskDao.getSubTasksByTaskId(task.getId());
                        List<SubTask> subTasks = SubTaskMapper.fromEntities(subTaskEntities);
                        task.setSubTasks(subTasks);
                    }
                }
                
                runOnMainThread(() -> callback.onSuccess(tasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy danh sách task: " + e.getMessage()));
            }
        });
    }
    
    public void searchTasks(String query, RepositoryCallback<List<Task>> callback) {
        executeAsync(() -> {
            try {
                List<TaskEntity> entities = taskDao.searchTasks(query);
                List<Task> tasks = TaskMapper.fromEntities(entities);

                for (Task task : tasks) {
                    if (task.getId() != null) {
                        List<SubTaskEntity> subTaskEntities = subTaskDao.getSubTasksByTaskId(task.getId());
                        List<SubTask> subTasks = SubTaskMapper.fromEntities(subTaskEntities);
                        task.setSubTasks(subTasks);
                    }
                }
                
                runOnMainThread(() -> callback.onSuccess(tasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi tìm kiếm task: " + e.getMessage()));
            }
        });
    }
    
    public void getTasksByCategory(String categoryId, RepositoryCallback<List<Task>> callback) {
        executeAsync(() -> {
            try {
                List<TaskEntity> entities = taskDao.getTasksByCategory(categoryId);
                List<Task> tasks = TaskMapper.fromEntities(entities);

                for (Task task : tasks) {
                    if (task.getId() != null) {
                        List<SubTaskEntity> subTaskEntities = subTaskDao.getSubTasksByTaskId(task.getId());
                        List<SubTask> subTasks = SubTaskMapper.fromEntities(subTaskEntities);
                        task.setSubTasks(subTasks);
                    }
                }
                
                runOnMainThread(() -> callback.onSuccess(tasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy task theo danh mục: " + e.getMessage()));
            }
        });
    }
    
    public void getTasksByDate(String date, RepositoryCallback<List<Task>> callback) {
        executeAsync(() -> {
            try {
                List<TaskEntity> entities = taskDao.getTasksByDate(date);
                List<Task> tasks = TaskMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(tasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy task theo ngày: " + e.getMessage()));
            }
        });
    }
    
    public void getCompletedTasks(RepositoryCallback<List<Task>> callback) {
        executeAsync(() -> {
            try {
                List<TaskEntity> entities = taskDao.getCompletedTasks();
                List<Task> tasks = TaskMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(tasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy task đã hoàn thành: " + e.getMessage()));
            }
        });
    }
    
    public void getIncompleteTasks(RepositoryCallback<List<Task>> callback) {
        executeAsync(() -> {
            try {
                List<TaskEntity> entities = taskDao.getIncompleteTasks();
                List<Task> tasks = TaskMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(tasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy task chưa hoàn thành: " + e.getMessage()));
            }
        });
    }
    
    public void getTodayTasks(RepositoryCallback<List<Task>> callback) {
        executeAsync(() -> {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String todayDate = dateFormat.format(new Date());
                
                List<TaskEntity> entities = taskDao.getTodayTasks(todayDate);
                List<Task> tasks = TaskMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(tasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy task hôm nay: " + e.getMessage()));
            }
        });
    }
    
    public void getOverdueTasks(RepositoryCallback<List<Task>> callback) {
        executeAsync(() -> {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String todayDate = dateFormat.format(new Date());
                
                List<TaskEntity> entities = taskDao.getOverdueTasks(todayDate);
                List<Task> tasks = TaskMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(tasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy task quá hạn: " + e.getMessage()));
            }
        });
    }
    
    public void getImportantTasks(RepositoryCallback<List<Task>> callback) {
        executeAsync(() -> {
            try {
                List<TaskEntity> entities = taskDao.getImportantTasks();
                List<Task> tasks = TaskMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(tasks));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy task quan trọng: " + e.getMessage()));
            }
        });
    }
}
