package com.example.todolist.repository.task;

import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskListenerRepository - Xử lý realtime listeners và sync dữ liệu
 */
public class TaskListenerRepository extends BaseRepository {
    
    private TaskCrudRepository crudRepository;

    public TaskListenerRepository() {
        this.crudRepository = new TaskCrudRepository();
    }

    public ValueEventListener addTasksRealtimeListener(ListCallback<Task> callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Task> tasks = new ArrayList<>();
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setId(taskSnapshot.getKey());
                        tasks.add(task);
                    }
                }
                
                callback.onSuccess(tasks);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Lỗi realtime listener: " + databaseError.getMessage());
            }
        };
        
        crudRepository.getTaskRef().addValueEventListener(listener);
        return listener;
    }

    public void removeTasksListener(ValueEventListener listener) {
        if (listener != null) {
            crudRepository.getTaskRef().removeEventListener(listener);
        }
    }

    public ValueEventListener addTaskListener(String taskId, RepositoryCallback<Task> callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Task task = dataSnapshot.getValue(Task.class);
                if (task != null) {
                    task.setId(taskId);
                    callback.onSuccess(task);
                } else {
                    callback.onError("Task không tồn tại");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Lỗi task listener: " + databaseError.getMessage());
            }
        };
        
        crudRepository.getTaskRef().child(taskId).addValueEventListener(listener);
        return listener;
    }

    public void removeTaskListener(String taskId, ValueEventListener listener) {
        if (listener != null) {
            crudRepository.getTaskRef().child(taskId).removeEventListener(listener);
        }
    }

    public void getAllTasks(ListCallback<Task> callback) {
        crudRepository.getTaskRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Task> tasks = new ArrayList<>();
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setId(taskSnapshot.getKey());
                        tasks.add(task);
                    }
                }
                
                callback.onSuccess(tasks);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Lỗi lấy tất cả tasks: " + databaseError.getMessage());
            }
        });
    }

    public void syncTasksWithLocal(List<Task> localTasks, ListCallback<Task> callback) {
        // This method can be used for offline sync functionality
        // For now, just return the Firebase data
        getAllTasks(callback);
    }
}
