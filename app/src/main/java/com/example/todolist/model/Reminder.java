package com.example.todolist.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class Reminder {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int taskId;
    private String taskTitle;
    private String reminderType; // "daily", "weekly", etc.
    private String reminderTime; // "22:00"
    private String selectedDays; // "2,3,4,5,6" (Mon-Fri)
    private boolean isActive;
    private long createdAt;
    
    public Reminder() {
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }
    
    public Reminder(int taskId, String taskTitle, String reminderType, String reminderTime, String selectedDays) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.reminderType = reminderType;
        this.reminderTime = reminderTime;
        this.selectedDays = selectedDays;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    
    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }
    
    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }
    
    public String getSelectedDays() { return selectedDays; }
    public void setSelectedDays(String selectedDays) { this.selectedDays = selectedDays; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
