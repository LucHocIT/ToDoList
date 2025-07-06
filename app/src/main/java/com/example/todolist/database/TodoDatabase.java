package com.example.todolist.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.todolist.model.TodoTask;
import com.example.todolist.model.Category;

@Database(entities = {TodoTask.class, Category.class}, version = 6, exportSchema = false)
public abstract class TodoDatabase extends RoomDatabase {
    
    private static TodoDatabase instance;
    
    public abstract TodoDao todoDao();
    public abstract CategoryDao categoryDao();
    
    public static synchronized TodoDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    TodoDatabase.class, "todo_database")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration() // For version upgrade
                    .build();
        }
        return instance;
    }
    
    // Method to clear all data
    public void clearAllTables() {
        todoDao().deleteAllTasks();
        categoryDao().deleteAllCategories();
    }
}
