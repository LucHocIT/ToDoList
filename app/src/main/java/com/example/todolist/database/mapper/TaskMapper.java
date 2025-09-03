package com.example.todolist.database.mapper;

import com.example.todolist.database.entity.TaskEntity;
import com.example.todolist.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskMapper {
    
    public static TaskEntity toEntity(Task task) {
        if (task == null) return null;
        
        TaskEntity entity = new TaskEntity();
        entity.id = task.getId();
        entity.title = task.getTitle();
        entity.description = task.getDescription();
        entity.dueDate = task.getDueDate();
        entity.dueTime = task.getDueTime();
        entity.isCompleted = task.isCompleted();
        entity.isImportant = task.isImportant();
        entity.categoryId = task.getCategory();
        entity.reminderType = task.getReminderType();
        entity.hasReminder = task.hasReminder();
        entity.attachments = task.getAttachments();
        entity.repeatType = task.getRepeatType();
        entity.isRepeating = task.isRepeating();
        entity.completionDate = task.getCompletionDate();
        entity.createdAt = task.getCreatedAt();
        entity.updatedAt = task.getUpdatedAt();
        return entity;
    }
    
    public static Task fromEntity(TaskEntity entity) {
        if (entity == null) return null;
        
        Task task = new Task();
        task.setId(entity.id);
        task.setTitle(entity.title);
        task.setDescription(entity.description);
        task.setDueDate(entity.dueDate);
        task.setDueTime(entity.dueTime);
        task.setCompleted(entity.isCompleted);
        task.setImportant(entity.isImportant);
        task.setCategory(entity.categoryId);
        task.setReminderType(entity.reminderType);
        task.setHasReminder(entity.hasReminder);
        task.setAttachments(entity.attachments);
        task.setRepeatType(entity.repeatType);
        task.setRepeating(entity.isRepeating);
        task.setCompletionDate(entity.completionDate);
        task.setCreatedAt(entity.createdAt);
        task.setUpdatedAt(entity.updatedAt);
        return task;
    }
    
    public static List<Task> fromEntities(List<TaskEntity> entities) {
        if (entities == null) return new ArrayList<>();
        
        List<Task> tasks = new ArrayList<>();
        for (TaskEntity entity : entities) {
            Task task = fromEntity(entity);
            if (task != null) {
                tasks.add(task);
            }
        }
        return tasks;
    }
    
    public static List<TaskEntity> toEntities(List<Task> tasks) {
        if (tasks == null) return new ArrayList<>();
        
        List<TaskEntity> entities = new ArrayList<>();
        for (Task task : tasks) {
            TaskEntity entity = toEntity(task);
            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }
}
