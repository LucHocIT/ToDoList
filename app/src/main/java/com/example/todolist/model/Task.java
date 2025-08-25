package com.example.todolist.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
    private String createdAt;    
    private String updatedAt;     
    public Task() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        this.createdAt = currentDate;
        this.updatedAt = currentDate;
    }

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
        result.put("updatedAt", updatedAt);
        return result;
    }

    public void updateTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.updatedAt = dateFormat.format(new Date());
    }

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
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            this.completionDate = formatter.format(new Date());
        } else {
            this.completionDate = null;
        }
        updateTimestamp();
    }
    public void setCompleted(boolean completed) { 
        setIsCompleted(completed);
    }
    public boolean isImportant() { return isImportant; }
    public void setIsImportant(boolean important) { 
        this.isImportant = important;
        updateTimestamp();
    }

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
    public void setRepeating(boolean repeating) { 
        setIsRepeating(repeating);
    }
    public String getCompletionDate() { return completionDate; }
    
    public void setCompletionDate(Object completionDate) { 
        if (completionDate instanceof String) {
            this.completionDate = (String) completionDate;
        } else if (completionDate instanceof Long) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            this.completionDate = dateFormat.format(new Date((Long) completionDate));
        } else if (completionDate != null) {
            this.completionDate = completionDate.toString();
        }
        updateTimestamp();
    }
    
    @Exclude
    public void setCompletionDateFromLong(Long completionDate) { 
        if (completionDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            this.completionDate = dateFormat.format(new Date(completionDate));
        }
    }
    public String getCreatedAt() { return createdAt; }
    
    public void setCreatedAt(Object createdAt) { 
        if (createdAt instanceof String) {
            this.createdAt = (String) createdAt;
        } else if (createdAt instanceof Long) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            this.createdAt = dateFormat.format(new Date((Long) createdAt));
        } else if (createdAt != null) {
            this.createdAt = createdAt.toString();
        }
    }
    
    @Exclude
    public void setCreatedAtFromLong(Long createdAt) { 
        if (createdAt != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            this.createdAt = dateFormat.format(new Date(createdAt));
        }
    }
    
    public String getUpdatedAt() { return updatedAt; }
    
    public void setUpdatedAt(Object updatedAt) { 
        if (updatedAt instanceof String) {
            this.updatedAt = (String) updatedAt;
        } else if (updatedAt instanceof Long) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            this.updatedAt = dateFormat.format(new Date((Long) updatedAt));
        } else if (updatedAt != null) {
            this.updatedAt = updatedAt.toString();
        }
    }
    
    @Exclude
    public void setUpdatedAtFromLong(Long updatedAt) { 
        if (updatedAt != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            this.updatedAt = dateFormat.format(new Date(updatedAt));
        }
    }
    
    public String getCategoryId() { return category; }
    public void setCategoryId(String categoryId) { 
        this.category = categoryId;
        updateTimestamp();
    }
    public String getReminder() { 
        return reminderType != null ? reminderType : "Không"; 
    }
    public void setReminder(String reminder) { 
        this.reminderType = reminder;
        this.hasReminder = reminder != null && !reminder.equals("Không");
        updateTimestamp();
    }
    
    public String getPriority() { 
        return isImportant ? "Cao" : "Thấp"; 
    }
    public void setPriority(String priority) { 
        this.isImportant = "Cao".equals(priority);
        updateTimestamp();
    }
    
    public String getRepeat() { 
        return repeatType != null ? repeatType : "Không"; 
    }
    public void setRepeat(String repeat) { 
        this.repeatType = repeat;
        this.isRepeating = repeat != null && !repeat.equals("Không");
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
