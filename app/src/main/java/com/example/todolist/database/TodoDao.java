package com.example.todolist.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todolist.model.TodoTask;

import java.util.List;

@Dao
public interface TodoDao {
    @Query("SELECT * FROM todo_tasks")
    List<TodoTask> getAllTasks();
    
    @Query("SELECT * FROM todo_tasks WHERE isCompleted = 0")
    List<TodoTask> getIncompleteTasks();
    
    @Query("SELECT * FROM todo_tasks WHERE isCompleted = 1")
    List<TodoTask> getCompletedTasks();
    
    @Query("SELECT * FROM todo_tasks WHERE isImportant = 1")
    List<TodoTask> getImportantTasks();
    
    @Query("SELECT * FROM todo_tasks WHERE id = :id")
    TodoTask getTaskById(int id);
    
    @Insert
    void insertTask(TodoTask task);
    
    @Update
    void updateTask(TodoTask task);
    
    @Delete
    void deleteTask(TodoTask task);
    
    @Query("DELETE FROM todo_tasks WHERE id = :taskId")
    void deleteTaskById(int taskId);
}
