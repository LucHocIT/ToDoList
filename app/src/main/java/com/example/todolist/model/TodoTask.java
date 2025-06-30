package com.example.todolist.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "todo_tasks")
public class TodoTask {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
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
    private String repeatType; // Không có, Hàng ngày, Hàng tuần, Hàng tháng
    private boolean isRepeating;
    private String completionDate; // Ngày thực tế hoàn thành task
    
    public TodoTask() {
    }
    
    @Ignore
    public TodoTask(String title, String description, String dueDate, String dueTime) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.isCompleted = false;
        this.isImportant = false;
        this.category = "Không có thể loại";
        this.reminderType = "Thông báo";
        this.hasReminder = false;
        this.attachments = "";
        this.repeatType = "Không có";
        this.isRepeating = false;
        this.completionDate = null;
    }
    
    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDueDate() { return dueDate; }
    public String getDueTime() { return dueTime; }
    public boolean isCompleted() { return isCompleted; }
    public boolean isImportant() { return isImportant; }
    public String getCategory() { return category; }
    public String getReminderType() { return reminderType; }
    public boolean isHasReminder() { return hasReminder; }
    public String getAttachments() { return attachments; }
    public String getRepeatType() { return repeatType; }
    public boolean isRepeating() { return isRepeating; }
    public String getCompletionDate() { return completionDate; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public void setImportant(boolean important) { isImportant = important; }
    public void setCategory(String category) { this.category = category; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }
    public void setHasReminder(boolean hasReminder) { this.hasReminder = hasReminder; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }
    public void setRepeating(boolean repeating) { isRepeating = repeating; }
    public void setCompletionDate(String completionDate) { this.completionDate = completionDate; }
}
