package com.example.todolist.repository.task;

import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.util.FirebaseHelper;
import com.google.firebase.database.DatabaseReference;

public class TaskCrudRepository extends BaseRepository {
    
    private final FirebaseHelper firebaseHelper;
    private final DatabaseReference taskRef;

    public TaskCrudRepository() {
        this.firebaseHelper = FirebaseHelper.getInstance();
        this.taskRef = firebaseHelper.getTasksReference();
    }

    public void addTask(Task task, DatabaseCallback<String> callback) {
        String taskId = firebaseHelper.generateTaskId();
        if (taskId != null) {
            task.setId(taskId);
            DatabaseReference taskRefForId = firebaseHelper.getTaskReference(taskId);
            taskRefForId.setValue(task)
                .addOnCompleteListener(firebaseTask -> {
                    if (firebaseTask.isSuccessful()) {
                        callback.onSuccess(taskId);
                    } else {
                        callback.onError("Lỗi thêm task: " + 
                            (firebaseTask.getException() != null ? 
                                firebaseTask.getException().getMessage() : "Unknown error"));
                    }
                });
        } else {
            callback.onError("Không thể tạo ID cho task");
        }
    }

    public void updateTask(Task task, DatabaseCallback<Boolean> callback) {
        if (task.getId() == null) {
            callback.onError("Task ID không hợp lệ");
            return;
        }

        taskRef.child(task.getId()).setValue(task)
            .addOnCompleteListener(firebaseTask -> {
                if (firebaseTask.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Lỗi cập nhật task: " + 
                        (firebaseTask.getException() != null ? 
                            firebaseTask.getException().getMessage() : "Unknown error"));
                }
            });
    }

    public void deleteTask(Task task, DatabaseCallback<Boolean> callback) {
        if (task.getId() == null) {
            callback.onError("Task ID không hợp lệ");
            return;
        }

        taskRef.child(task.getId()).removeValue()
            .addOnCompleteListener(firebaseTask -> {
                if (firebaseTask.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Lỗi xóa task: " + 
                        (firebaseTask.getException() != null ? 
                            firebaseTask.getException().getMessage() : "Unknown error"));
                }
            });
    }

    public void getTaskById(String taskId, RepositoryCallback<Task> callback) {
        taskRef.child(taskId).get()
            .addOnCompleteListener(firebaseTask -> {
                if (firebaseTask.isSuccessful()) {
                    Task task = firebaseTask.getResult().getValue(Task.class);
                    if (task != null) {
                        task.setId(taskId);
                        callback.onSuccess(task);
                    } else {
                        callback.onError("Task không tồn tại");
                    }
                } else {
                    callback.onError("Lỗi lấy task: " + 
                        (firebaseTask.getException() != null ? 
                            firebaseTask.getException().getMessage() : "Unknown error"));
                }
            });
    }

    public void updateTaskCompletion(String taskId, boolean isCompleted, DatabaseCallback<Boolean> callback) {
        taskRef.child(taskId).child("isCompleted").setValue(isCompleted)
            .addOnCompleteListener(firebaseTask -> {
                if (firebaseTask.isSuccessful()) {
                    String completionDate = null;
                    if (isCompleted) {
                        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                        completionDate = formatter.format(new java.util.Date());
                    }
                    taskRef.child(taskId).child("completionDate").setValue(completionDate)
                        .addOnCompleteListener(dateTask -> {
                            if (dateTask.isSuccessful()) {
                                callback.onSuccess(true);
                            } else {
                                callback.onError("Lỗi cập nhật ngày hoàn thành: " + 
                                    (dateTask.getException() != null ? 
                                        dateTask.getException().getMessage() : "Unknown error"));
                            }
                        });
                } else {
                    callback.onError("Lỗi cập nhật trạng thái task: " + 
                        (firebaseTask.getException() != null ? 
                            firebaseTask.getException().getMessage() : "Unknown error"));
                }
            });
    }

    public void updateTaskImportance(String taskId, boolean isImportant, DatabaseCallback<Boolean> callback) {
        taskRef.child(taskId).child("isImportant").setValue(isImportant)
            .addOnCompleteListener(firebaseTask -> {
                if (firebaseTask.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("Lỗi cập nhật tầm quan trọng task: " + 
                        (firebaseTask.getException() != null ? 
                            firebaseTask.getException().getMessage() : "Unknown error"));
                }
            });
    }

    public DatabaseReference getTaskRef() {
        return taskRef;
    }
}
