package com.example.todolist.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todolist.model.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    void insertCategory(Category category);

    @Update
    void updateCategory(Category category);

    @Delete
    void deleteCategory(Category category);

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    List<Category> getAllCategories();

    @Query("SELECT * FROM categories WHERE isDefault = 1 ORDER BY sortOrder ASC")
    List<Category> getDefaultCategories();

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    Category getCategoryById(int categoryId);

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    Category getCategoryByName(String name);

    @Query("UPDATE categories SET sortOrder = :newOrder WHERE id = :categoryId")
    void updateCategorySortOrder(int categoryId, int newOrder);

    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCount();

    @Query("DELETE FROM categories WHERE isDefault = 0")
    void deleteUserCategories();
}
