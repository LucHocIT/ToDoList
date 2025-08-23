package com.example.todolist.model;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
/**
 * Task model for Firebase Realtime Database
 * Clean, lightweight version without Room annotations
 * Implements Serializable for Firebase compatibility
 */
public class Task implements Serializable {
    private String id;
    private String title;
    private String description;
    private String dueDate;
    private String dueTime;
    private boolean isCompleted;
    private boolean isImportant;
    private String category;
    private String reminderType;
    private boolean hasReminder;
    private String attachments;
    private String repeatType;
    private boolean isRepeating;
    private String completionDate;
    private long createdAt;
    private long updatedAt;
    // Default constructor required for Firebase
    public Task() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    // Primary constructor
    public Task(String title, String description, String dueDate, String dueTime) {
        this();
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.isCompleted = false;
        this.isImportant = false;
        this.category = "Không có thể loại";
        this.reminderType = "Thông báo";
        this.repeatType = "Không lặp lại";
        this.hasReminder = false;
        this.attachments = "";
        this.isRepeating = false;
        this.completionDate = null;
    }
    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("description", description);
        result.put("dueDate", dueDate);
        result.put("dueTime", dueTime);
        result.put("isCompleted", isCompleted);
        result.put("isImportant", isImportant);
        result.put("category", category);
        result.put("reminderType", reminderType);
        result.put("hasReminder", hasReminder);
        result.put("attachments", attachments);
        result.put("repeatType", repeatType);
        result.put("isRepeating", isRepeating);
        result.put("completionDate", completionDate);
        result.put("createdAt", createdAt);
        result.put("updatedAt", System.currentTimeMillis());
        return result;
    }
    // Update timestamp
    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { 
        this.title = title; 
        updateTimestamp();
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description;
        updateTimestamp();
    }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { 
        this.dueDate = dueDate;
        updateTimestamp();
    }
    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { 
        this.dueTime = dueTime;
        updateTimestamp();
    }
    public boolean isCompleted() { return isCompleted; }
    public void setIsCompleted(boolean completed) { 
        this.isCompleted = completed;
        if (completed) {
            this.completionDate = String.valueOf(System.currentTimeMillis());
        } else {
            this.completionDate = null;
        }
        updateTimestamp();
    }
    // Firebase compatibility setter (delegates to setIsCompleted)
    public void setCompleted(boolean completed) { 
        setIsCompleted(completed);
    }
    public boolean isImportant() { return isImportant; }
    public void setIsImportant(boolean important) { 
        this.isImportant = important;
        updateTimestamp();
    }
    // Firebase compatibility setter (delegates to setIsImportant)
    public void setImportant(boolean important) { 
        setIsImportant(important);
    }
    public String getCategory() { return category; }
    public void setCategory(String category) { 
        this.category = category;
        updateTimestamp();
    }
    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { 
        this.reminderType = reminderType;
        updateTimestamp();
    }
    public boolean isHasReminder() { return hasReminder; }
    public void setHasReminder(boolean hasReminder) { 
        this.hasReminder = hasReminder;
        updateTimestamp();
    }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { 
        this.attachments = attachments;
        updateTimestamp();
    }
    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { 
        this.repeatType = repeatType;
        updateTimestamp();
    }
    public boolean isRepeating() { return isRepeating; }
    public void setIsRepeating(boolean repeating) { 
        this.isRepeating = repeating;
        updateTimestamp();
    }
    // Firebase compatibility setter (delegates to setIsRepeating)
    public void setRepeating(boolean repeating) { 
        setIsRepeating(repeating);
    }
    public String getCompletionDate() { return completionDate; }
    public void setCompletionDate(String completionDate) { 
        this.completionDate = completionDate;
        updateTimestamp();
    }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    // Compatibility methods for existing code
    public String getCategoryId() { return category; }
    public void setCategoryId(String categoryId) { 
        this.category = categoryId;
        updateTimestamp();
    }
    public String getReminder() { return reminderType; }
    public void setReminder(String reminder) { 
        this.reminderType = reminder;
        updateTimestamp();
    }
    public String getPriority() { 
        return isImportant ? "Cao" : "Thấp"; 
    }
    public void setPriority(String priority) { 
        this.isImportant = "Cao".equals(priority);
        updateTimestamp();
    }
    public String getRepeat() { return repeatType; }
    public void setRepeat(String repeat) { 
        this.repeatType = repeat;
        updateTimestamp();
    }
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", isCompleted=" + isCompleted +
                '}';
    }
}
