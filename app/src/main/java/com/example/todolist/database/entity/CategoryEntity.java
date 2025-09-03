package com.example.todolist.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {
    
    @PrimaryKey
    @NonNull
    public String id;
    
    public String name;
    public String color;
    public String icon;
    public int sortOrder;
    public boolean isDefault;
    public String createdAt;
    public String updatedAt;

    public CategoryEntity() {
    }

    public CategoryEntity(@NonNull String id, String name, String color, String icon, 
                          int sortOrder, boolean isDefault, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
