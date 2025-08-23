package com.example.todolist.model;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
/**
 * Category model for Firebase Realtime Database
 * Clean version without Room annotations, implements Serializable
 */
public class Category implements Serializable {
    private String id;
    private String name;
    private String color;
    private int sortOrder;
    private boolean isDefault;
    private long createdAt;
    private long updatedAt;
    // Default constructor required for Firebase
    public Category() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    public Category(String name, String color) {
        this();
        this.name = name;
        this.color = color;
        this.sortOrder = 0;
        this.isDefault = false;
    }
    public Category(String name, String color, int sortOrder, boolean isDefault) {
        this();
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
        this.isDefault = isDefault;
    }
    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("color", color);
        result.put("sortOrder", sortOrder);
        result.put("isDefault", isDefault);
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
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name;
        updateTimestamp();
    }
    public String getColor() { return color; }
    public void setColor(String color) { 
        this.color = color;
        updateTimestamp();
    }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { 
        this.sortOrder = sortOrder;
        updateTimestamp();
    }
    public boolean getIsDefault() { return isDefault; }
    public boolean isDefault() { return isDefault; }
    public void setIsDefault(boolean isDefault) { 
        this.isDefault = isDefault;
        updateTimestamp();
    }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
}
