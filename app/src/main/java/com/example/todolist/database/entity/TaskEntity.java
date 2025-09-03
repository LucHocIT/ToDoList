package com.example.todolist.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {
    
    @PrimaryKey
    @NonNull
    public String id;
    
    public String title;
    public String description;
    public String dueDate;
    public String dueTime;
    public boolean isCompleted;
    public boolean isImportant;
    public String categoryId;
    public String reminderType;
    public boolean hasReminder;
    public String attachments;
    public String repeatType;
    public boolean isRepeating;
    public String completionDate;
    public String createdAt;
    public String updatedAt;

    public TaskEntity() {
    }

    public TaskEntity(@NonNull String id, String title, String description, String dueDate, 
                      String dueTime, boolean isCompleted, boolean isImportant, String categoryId,
                      String reminderType, boolean hasReminder, String attachments, String repeatType,
                      boolean isRepeating, String completionDate, String createdAt, String updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.isCompleted = isCompleted;
        this.isImportant = isImportant;
        this.categoryId = categoryId;
        this.reminderType = reminderType;
        this.hasReminder = hasReminder;
        this.attachments = attachments;
        this.repeatType = repeatType;
        this.isRepeating = isRepeating;
        this.completionDate = completionDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
