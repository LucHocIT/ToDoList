package com.example.todolist.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SubTask implements Serializable {
    private String id;
    private String title;
    private boolean isCompleted;
    private String createdAt;
    private String taskId; // ID của task cha

    public SubTask() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        this.createdAt = currentDate;
        this.isCompleted = false;
    }

    public SubTask(String title, String taskId) {
        this();
        this.title = title;
        this.taskId = taskId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("isCompleted", isCompleted);
        result.put("createdAt", createdAt);
        result.put("taskId", taskId);
        return result;
    }

    public static SubTask fromMap(java.util.Map<String, Object> data) {
        SubTask subTask = new SubTask();
        subTask.setId((String) data.get("id"));
        subTask.setTitle((String) data.get("title"));
        subTask.setCompleted(Boolean.TRUE.equals(data.get("isCompleted")));
        subTask.setCreatedAt((String) data.get("createdAt"));
        subTask.setTaskId((String) data.get("taskId"));
        return subTask;
    }
}
