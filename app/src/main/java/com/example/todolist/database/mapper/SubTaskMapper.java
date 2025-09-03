package com.example.todolist.database.mapper;

import com.example.todolist.database.entity.SubTaskEntity;
import com.example.todolist.model.SubTask;

import java.util.ArrayList;
import java.util.List;

public class SubTaskMapper {
    
    public static SubTaskEntity toEntity(SubTask subTask) {
        if (subTask == null) return null;
        
        SubTaskEntity entity = new SubTaskEntity();
        entity.id = subTask.getId();
        entity.taskId = subTask.getTaskId();
        entity.title = subTask.getTitle();
        entity.isCompleted = subTask.isCompleted();
        entity.createdAt = subTask.getCreatedAt();
        return entity;
    }
    
    public static SubTask fromEntity(SubTaskEntity entity) {
        if (entity == null) return null;
        
        SubTask subTask = new SubTask();
        subTask.setId(entity.id);
        subTask.setTaskId(entity.taskId);
        subTask.setTitle(entity.title);
        subTask.setCompleted(entity.isCompleted);
        subTask.setCreatedAt(entity.createdAt);
        return subTask;
    }
    
    public static List<SubTask> fromEntities(List<SubTaskEntity> entities) {
        if (entities == null) return new ArrayList<>();
        
        List<SubTask> subTasks = new ArrayList<>();
        for (SubTaskEntity entity : entities) {
            SubTask subTask = fromEntity(entity);
            if (subTask != null) {
                subTasks.add(subTask);
            }
        }
        return subTasks;
    }
    
    public static List<SubTaskEntity> toEntities(List<SubTask> subTasks) {
        if (subTasks == null) return new ArrayList<>();
        
        List<SubTaskEntity> entities = new ArrayList<>();
        for (SubTask subTask : subTasks) {
            SubTaskEntity entity = toEntity(subTask);
            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }
}
