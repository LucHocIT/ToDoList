package com.example.todolist.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todolist.database.entity.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    LiveData<List<TaskEntity>> getAllTasksLiveData();
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    List<TaskEntity> getAllTasks();
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    TaskEntity getTaskById(String taskId);
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC")
    List<TaskEntity> getIncompleteTasks();
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completionDate DESC")
    List<TaskEntity> getCompletedTasks();
    
    @Query("SELECT * FROM tasks WHERE dueDate = :date ORDER BY dueTime ASC")
    List<TaskEntity> getTasksByDate(String date);
    
    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    List<TaskEntity> getTasksByCategory(String categoryId);
    
    @Query("SELECT * FROM tasks WHERE isImportant = 1 ORDER BY dueDate ASC")
    List<TaskEntity> getImportantTasks();
    
    @Query("SELECT * FROM tasks WHERE dueDate = :todayDate ORDER BY dueTime ASC")
    List<TaskEntity> getTodayTasks(String todayDate);
    
    @Query("SELECT * FROM tasks WHERE dueDate < :todayDate AND isCompleted = 0 ORDER BY dueDate DESC")
    List<TaskEntity> getOverdueTasks(String todayDate);
    
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    List<TaskEntity> searchTasks(String query);
    
    @Insert
    void insertTask(TaskEntity task);
    
    @Update
    void updateTask(TaskEntity task);
    
    @Delete
    void deleteTask(TaskEntity task);
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted, completionDate = :completionDate, updatedAt = :updatedAt WHERE id = :taskId")
    void updateTaskCompletion(String taskId, boolean isCompleted, String completionDate, String updatedAt);
    
    @Query("UPDATE tasks SET isImportant = :isImportant, updatedAt = :updatedAt WHERE id = :taskId")
    void updateTaskImportance(String taskId, boolean isImportant, String updatedAt);
    
    @Query("DELETE FROM tasks")
    void deleteAllTasks();
}
