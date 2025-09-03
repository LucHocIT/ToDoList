package com.example.todolist.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.todolist.database.dao.CategoryDao;
import com.example.todolist.database.dao.SubTaskDao;
import com.example.todolist.database.dao.TaskDao;
import com.example.todolist.database.entity.CategoryEntity;
import com.example.todolist.database.entity.SubTaskEntity;
import com.example.todolist.database.entity.TaskEntity;

@Database(
    entities = {TaskEntity.class, CategoryEntity.class, SubTaskEntity.class},
    version = 1,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class ToDoDatabase extends RoomDatabase {
    
    private static volatile ToDoDatabase INSTANCE;
    
    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    public abstract SubTaskDao subTaskDao();
    
    public static ToDoDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ToDoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        ToDoDatabase.class,
                        "todo_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    public static void destroyInstance() {
        INSTANCE = null;
    }
}
