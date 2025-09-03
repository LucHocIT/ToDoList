package com.example.todolist.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todolist.database.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    LiveData<List<CategoryEntity>> getAllCategoriesLiveData();
    
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    List<CategoryEntity> getAllCategories();
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    CategoryEntity getCategoryById(String categoryId);
    
    @Query("SELECT * FROM categories WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    List<CategoryEntity> searchCategories(String query);
    
    @Query("SELECT * FROM categories WHERE color = :color ORDER BY name ASC")
    List<CategoryEntity> getCategoriesByColor(String color);
    
    @Query("SELECT * FROM categories WHERE isDefault = 1 ORDER BY sortOrder ASC")
    List<CategoryEntity> getDefaultCategories();
    
    @Insert
    void insertCategory(CategoryEntity category);
    
    @Update
    void updateCategory(CategoryEntity category);
    
    @Delete
    void deleteCategory(CategoryEntity category);
    
    @Query("DELETE FROM categories")
    void deleteAllCategories();
    
    @Query("SELECT COUNT(*) FROM categories WHERE name = :name AND id != :excludeId")
    int countCategoriesWithName(String name, String excludeId);
    
    @Query("SELECT COUNT(*) FROM categories WHERE name = :name")
    int countCategoriesWithName(String name);
}
