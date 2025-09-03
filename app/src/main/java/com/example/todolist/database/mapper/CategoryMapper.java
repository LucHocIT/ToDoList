package com.example.todolist.database.mapper;

import com.example.todolist.database.entity.CategoryEntity;
import com.example.todolist.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryMapper {
    
    public static CategoryEntity toEntity(Category category) {
        if (category == null) return null;
        
        CategoryEntity entity = new CategoryEntity();
        entity.id = category.getId();
        entity.name = category.getName();
        entity.color = category.getColor();
        entity.icon = category.getIcon();
        entity.sortOrder = category.getSortOrder();
        entity.isDefault = category.isDefault();
        entity.createdAt = category.getCreatedAt();
        entity.updatedAt = category.getUpdatedAt();
        return entity;
    }
    
    public static Category fromEntity(CategoryEntity entity) {
        if (entity == null) return null;
        
        Category category = new Category();
        category.setId(entity.id);
        category.setName(entity.name);
        category.setColor(entity.color);
        category.setIcon(entity.icon);
        category.setSortOrder(entity.sortOrder);
        category.setDefault(entity.isDefault);
        category.setCreatedAt(entity.createdAt);
        category.setUpdatedAt(entity.updatedAt);
        return category;
    }
    
    public static List<Category> fromEntities(List<CategoryEntity> entities) {
        if (entities == null) return new ArrayList<>();
        
        List<Category> categories = new ArrayList<>();
        for (CategoryEntity entity : entities) {
            Category category = fromEntity(entity);
            if (category != null) {
                categories.add(category);
            }
        }
        return categories;
    }
    
    public static List<CategoryEntity> toEntities(List<Category> categories) {
        if (categories == null) return new ArrayList<>();
        
        List<CategoryEntity> entities = new ArrayList<>();
        for (Category category : categories) {
            CategoryEntity entity = toEntity(category);
            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }
}
