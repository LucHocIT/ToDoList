package com.example.todolist.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Category implements Serializable {
    private String id;
    private String name;
    private String color;
    private String icon;
    private int sortOrder;
    private boolean isDefault;
    private String createdAt; 
    private String updatedAt; 
    public Category() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        this.createdAt = currentDate;
        this.updatedAt = currentDate;
    }
    public Category(String name, String color) {
        this();
        this.name = name;
        this.color = color;
        this.icon = null;
        this.sortOrder = 0;
        this.isDefault = false;
    }
    public Category(String name, String color, int sortOrder, boolean isDefault) {
        this();
        this.name = name;
        this.color = color;
        this.icon = null;
        this.sortOrder = sortOrder;
        this.isDefault = isDefault;
    }
    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("color", color);
        result.put("icon", icon);
        result.put("sortOrder", sortOrder);
        result.put("isDefault", isDefault);
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        return result;
    }
    
    // Convert from Map for Firebase
    public static Category fromMap(Map<String, Object> map) {
        Category category = new Category();
        category.setId((String) map.get("id"));
        category.setName((String) map.get("name"));
        category.setColor((String) map.get("color"));
        category.setIcon((String) map.get("icon"));
        
        Object sortOrder = map.get("sortOrder");
        if (sortOrder instanceof Long) {
            category.setSortOrder(((Long) sortOrder).intValue());
        } else if (sortOrder instanceof Integer) {
            category.setSortOrder((Integer) sortOrder);
        }
        
        Object isDefault = map.get("isDefault");
        if (isDefault instanceof Boolean) {
            category.setIsDefault((Boolean) isDefault);
        }
        
        category.setCreatedAt(map.get("createdAt"));
        category.setUpdatedAt(map.get("updatedAt"));
        
        return category;
    }
    // Update timestamp
    public void updateTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.updatedAt = dateFormat.format(new Date());
    }
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
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { 
        this.icon = icon;
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
    public void setDefault(boolean isDefault) { 
        this.isDefault = isDefault;
        updateTimestamp();
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
    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", icon='" + icon + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
}
