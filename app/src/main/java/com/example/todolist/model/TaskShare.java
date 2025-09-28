package com.example.todolist.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskShare implements Serializable {
    private String id;
    private String taskId;
    private String ownerId;
    private String ownerEmail;
    private String ownerName;
    private List<SharedUser> sharedUsers;
    private String createdAt;
    private String updatedAt;
    private boolean isActive;

    public TaskShare() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        this.createdAt = currentDate;
        this.updatedAt = currentDate;
        this.sharedUsers = new ArrayList<>();
        this.isActive = true;
    }

    public TaskShare(String taskId, String ownerId, String ownerEmail, String ownerName) {
        this();
        this.taskId = taskId;
        this.ownerId = ownerId;
        this.ownerEmail = ownerEmail;
        this.ownerName = ownerName;
    }

    public void updateTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        this.updatedAt = dateFormat.format(new Date());
    }

    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("taskId", taskId);
        result.put("ownerId", ownerId);
        result.put("ownerEmail", ownerEmail);
        result.put("ownerName", ownerName);
        
        List<Map<String, Object>> usersList = new ArrayList<>();
        if (sharedUsers != null) {
            for (SharedUser user : sharedUsers) {
                usersList.add(user.toMap());
            }
        }
        result.put("sharedUsers", usersList);
        
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        result.put("isActive", isActive);
        return result;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { 
        this.taskId = taskId;
        updateTimestamp();
    }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { 
        this.ownerId = ownerId;
        updateTimestamp();
    }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { 
        this.ownerEmail = ownerEmail;
        updateTimestamp();
    }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { 
        this.ownerName = ownerName;
        updateTimestamp();
    }

    public List<SharedUser> getSharedUsers() { return sharedUsers; }
    public void setSharedUsers(List<SharedUser> sharedUsers) { 
        this.sharedUsers = sharedUsers;
        updateTimestamp();
    }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { 
        this.isActive = active;
        updateTimestamp();
    }

    // Helper methods
    public void addSharedUser(SharedUser user) {
        if (this.sharedUsers == null) {
            this.sharedUsers = new ArrayList<>();
        }
        this.sharedUsers.add(user);
        updateTimestamp();
    }

    public void removeSharedUser(String userEmail) {
        if (this.sharedUsers != null) {
            this.sharedUsers.removeIf(user -> user.getEmail().equals(userEmail));
            updateTimestamp();
        }
    }

    public boolean isUserShared(String userEmail) {
        if (this.sharedUsers == null) return false;
        return this.sharedUsers.stream().anyMatch(user -> user.getEmail().equals(userEmail));
    }

    public SharedUser getSharedUser(String userEmail) {
        if (this.sharedUsers == null) return null;
        return this.sharedUsers.stream()
                .filter(user -> user.getEmail().equals(userEmail))
                .findFirst()
                .orElse(null);
    }

    public boolean isOwner(String userEmail) {
        return ownerEmail != null && ownerEmail.equals(userEmail);
    }

    public boolean canUserEdit(String userEmail) {
        if (isOwner(userEmail)) return true;
        if (sharedUsers == null) return false;
        return sharedUsers.stream()
                .anyMatch(user -> user.getEmail().equals(userEmail) && user.canEdit());
    }

    @Override
    public String toString() {
        return "TaskShare{" +
                "id='" + id + '\'' +
                ", taskId='" + taskId + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", sharedUsers=" + (sharedUsers != null ? sharedUsers.size() : 0) +
                '}';
    }
}