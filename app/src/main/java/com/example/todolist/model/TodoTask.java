package com.example.todolist.model;

import android.content.Context;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.todolist.R;

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
    private String repeatType;
    private boolean isRepeating;
    private String completionDate;

    public TodoTask() {
        // Required by Room
    }

    // Constructor hỗ trợ đa ngôn ngữ bằng cách truyền Context
    @Ignore
    public TodoTask(Context context, String title, String description, String dueDate, String dueTime) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;

        this.isCompleted = false;
        this.isImportant = false;

        // Lấy chuỗi từ strings.xml
        this.category = context.getString(R.string.no_category);
        this.reminderType = context.getString(R.string.notification);
        this.repeatType = context.getString(R.string.no_repeat);

        this.hasReminder = false;
        this.attachments = "";
        this.isRepeating = false;
        this.completionDate = null;
    }

    // Getter và Setter giữ nguyên như bạn đã viết
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
