package com.example.todolist.model;
import android.content.Context;
import com.example.todolist.R;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
public class TodoTask implements Serializable {
    private String id;
    private String title;
    private String description;
    private String dueDate;
    private String dueTime;
    private boolean isCompleted;
    private boolean isImportant;
    private String categoryId;
    private String reminderType;
    private boolean hasReminder;
    private String attachments;
    private String repeatType;
    private boolean isRepeating;
    private String completionDate;
    private long createdAt;
    private long updatedAt;
    // Default constructor required for Firebase
    public TodoTask() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    public TodoTask(Context context, String title, String description, String dueDate, String dueTime) {
        this();
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.isCompleted = false;
        this.isImportant = false;
        // Láº¥y chuá»—i tá»« strings.xml
        this.categoryId = null; // No default category
        this.reminderType = context.getString(R.string.notification);
        this.repeatType = context.getString(R.string.no_repeat);
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
        result.put("categoryId", categoryId);
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
    public void setCompleted(boolean completed) { 
        isCompleted = completed;
        updateTimestamp();
    }
    public boolean isImportant() { return isImportant; }
    public void setImportant(boolean important) { 
        isImportant = important;
        updateTimestamp();
    }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { 
        this.categoryId = categoryId;
        updateTimestamp();
    }
    // Legacy getter for compatibility (maps to categoryId)
    public String getCategory() { return categoryId; }
    public void setCategory(String category) { 
        this.categoryId = category;
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
    public void setRepeating(boolean repeating) { 
        isRepeating = repeating;
        updateTimestamp();
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
    @Override
    public String toString() {
        return "TodoTask{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", dueTime='" + dueTime + '\'' +
                ", isCompleted=" + isCompleted +
                ", isImportant=" + isImportant +
                ", categoryId='" + categoryId + '\'' +
                '}';
    }
}
