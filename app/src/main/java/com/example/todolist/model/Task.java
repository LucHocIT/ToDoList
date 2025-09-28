package com.example.todolist.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private Long lastModified;
    private List<SubTask> subTasks;
    private boolean isShared; // Đánh dấu task có được chia sẻ không     
    public Task() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        this.createdAt = currentDate;
        this.updatedAt = currentDate;
        this.lastModified = System.currentTimeMillis();
        this.subTasks = new ArrayList<>();
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
        result.put("lastModified", lastModified);
        result.put("subTasks", subTasks);
        result.put("isShared", isShared);
        return result;
    }

    public void updateTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.updatedAt = dateFormat.format(new Date());
        this.lastModified = System.currentTimeMillis();
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
    public boolean hasReminder() { return hasReminder; }
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
    
    
    public void setUpdatedAtFromLong(Long updatedAt) { 
        if (updatedAt != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            this.updatedAt = dateFormat.format(new Date(updatedAt));
        }
    }
    
    public Long getLastModified() { return lastModified; }
    
    public void setLastModified(Long lastModified) { 
        this.lastModified = lastModified;
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
    
    // Attachment helper methods
    
    public List<Attachment> getAttachmentList() {
        if (attachments == null || attachments.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Attachment>>(){}.getType();
            return gson.fromJson(attachments, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    
    public void setAttachmentList(List<Attachment> attachmentList) {
        if (attachmentList == null || attachmentList.isEmpty()) {
            this.attachments = "";
        } else {
            try {
                Gson gson = new Gson();
                this.attachments = gson.toJson(attachmentList);
            } catch (Exception e) {
                this.attachments = "";
            }
        }
        updateTimestamp();
    }
    
    
    public void addAttachment(Attachment attachment) {
        List<Attachment> currentList = getAttachmentList();
        attachment.setId(String.valueOf(System.currentTimeMillis()));
        currentList.add(attachment);
        setAttachmentList(currentList);
    }
    
    
    public void removeAttachment(String attachmentId) {
        List<Attachment> currentList = getAttachmentList();
        currentList.removeIf(attachment -> attachmentId.equals(attachment.getId()));
        setAttachmentList(currentList);
    }
    
    
    public boolean hasAttachments() {
        return !getAttachmentList().isEmpty();
    }
    
    // SubTasks getter/setter
    public List<SubTask> getSubTasks() {
        return subTasks != null ? subTasks : new ArrayList<>();
    }
    
    public void setSubTasks(List<SubTask> subTasks) {
        this.subTasks = subTasks;
        updateTimestamp();
    }
    
    public void addSubTask(SubTask subTask) {
        if (this.subTasks == null) {
            this.subTasks = new ArrayList<>();
        }
        this.subTasks.add(subTask);
        updateTimestamp();
    }
    
    public void removeSubTask(SubTask subTask) {
        if (this.subTasks != null) {
            this.subTasks.remove(subTask);
            updateTimestamp();
        }
    }
    
    public boolean isShared() { return isShared; }
    public void setShared(boolean shared) { 
        this.isShared = shared;
        updateTimestamp();
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", isCompleted=" + isCompleted +
                ", isShared=" + isShared +
                '}';
    }
}
