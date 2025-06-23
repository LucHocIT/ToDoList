package com.example.todolist.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.todolist.model.TodoTask;

@Database(entities = {TodoTask.class}, version = 1, exportSchema = false)
public abstract class TodoDatabase extends RoomDatabase {
    
    private static TodoDatabase instance;
    
    public abstract TodoDao todoDao();
    
    public static synchronized TodoDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    TodoDatabase.class, "todo_database")
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
