package com.example.todolist.repository.task;

import androidx.annotation.NonNull;
import com.example.todolist.model.SubTask;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.util.FirebaseHelper;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SubTaskRepository extends BaseRepository {
    
    private FirebaseHelper firebaseHelper;
    
    public SubTaskRepository() {
        this.firebaseHelper = FirebaseHelper.getInstance();
    }
    
    public void saveSubTask(String taskId, SubTask subTask, DatabaseCallback<Boolean> callback) {
        DatabaseReference subTaskRef = firebaseHelper.getSubTasksReference()
                .child(taskId)
                .child(subTask.getId());
        
        subTaskRef.setValue(subTask)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
    
    public void updateSubTask(String taskId, SubTask subTask, DatabaseCallback<Boolean> callback) {
        saveSubTask(taskId, subTask, callback);
    }
    
    public void deleteSubTask(String taskId, String subTaskId, DatabaseCallback<Boolean> callback) {
        DatabaseReference subTaskRef = firebaseHelper.getSubTasksReference()
                .child(taskId)
                .child(subTaskId);
        
        subTaskRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
    
    public void saveAllSubTasks(String taskId, List<SubTask> subTasks, DatabaseCallback<Boolean> callback) {
        DatabaseReference taskSubTasksRef = firebaseHelper.getSubTasksReference().child(taskId);
        
        // Clear existing subtasks first
        taskSubTasksRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (subTasks == null || subTasks.isEmpty()) {
                        if (callback != null) {
                            callback.onSuccess(true);
                        }
                        return;
                    }
                    
                    // Save new subtasks
                    for (SubTask subTask : subTasks) {
                        taskSubTasksRef.child(subTask.getId()).setValue(subTask);
                    }
                    
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
    
    public void getSubTasks(String taskId, ListCallback<SubTask> callback) {
        DatabaseReference subTasksRef = firebaseHelper.getSubTasksReference().child(taskId);
        
        subTasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SubTask> subTasks = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SubTask subTask = child.getValue(SubTask.class);
                    if (subTask != null) {
                        subTask.setId(child.getKey());
                        subTasks.add(subTask);
                    }
                }
                callback.onSuccess(subTasks);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }
}
