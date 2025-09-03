package com.example.todolist.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "subtasks",
        foreignKeys = @ForeignKey(
            entity = TaskEntity.class,
            parentColumns = "id",
            childColumns = "taskId",
            onDelete = ForeignKey.CASCADE
        ))
public class SubTaskEntity {
    
    @PrimaryKey
    @NonNull
    public String id;
    
    public String taskId;
    public String title;
    public boolean isCompleted;
    public String createdAt;

    public SubTaskEntity() {
    }

    public SubTaskEntity(@NonNull String id, String taskId, String title, 
                         boolean isCompleted, String createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.title = title;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }
}
