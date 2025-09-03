package com.example.todolist.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todolist.database.entity.SubTaskEntity;

import java.util.List;

@Dao
public interface SubTaskDao {
    
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    List<SubTaskEntity> getSubTasksByTaskId(String taskId);
    
    @Query("SELECT * FROM subtasks WHERE id = :subTaskId")
    SubTaskEntity getSubTaskById(String subTaskId);
    
    @Insert
    void insertSubTask(SubTaskEntity subTask);
    
    @Update
    void updateSubTask(SubTaskEntity subTask);
    
    @Delete
    void deleteSubTask(SubTaskEntity subTask);
    
    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    void deleteSubTasksByTaskId(String taskId);
    
    @Query("DELETE FROM subtasks")
    void deleteAllSubTasks();
    
    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subTaskId")
    void updateSubTaskCompletion(String subTaskId, boolean isCompleted);
}
