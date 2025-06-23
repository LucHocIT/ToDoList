package com.example.todolist.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String name;
    private String color;
    private int sortOrder;
    private boolean isDefault;

    public Category() {}

    public Category(String name, int taskCount, String color) {
        this.name = name;
        this.color = color;
        this.sortOrder = 0;
        this.isDefault = false;
    }

    public Category(String name, String color, int sortOrder, boolean isDefault) {
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
        this.isDefault = isDefault;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    // Helper method to get task count (will be calculated dynamically)
    public int getTaskCount() {
        // This will be calculated from TodoTask table
        return 0;
    }
}
