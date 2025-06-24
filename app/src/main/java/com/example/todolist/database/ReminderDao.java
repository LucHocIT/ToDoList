package com.example.todolist.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.todolist.model.Reminder;
import java.util.List;

@Dao
public interface ReminderDao {
    
    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY createdAt DESC")
    List<Reminder> getAllActiveReminders();
    
    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    List<Reminder> getRemindersByTaskId(int taskId);
    
    @Query("SELECT * FROM reminders WHERE id = :id")
    Reminder getReminderById(int id);
    
    @Insert
    long insertReminder(Reminder reminder);
    
    @Update
    void updateReminder(Reminder reminder);
    
    @Delete
    void deleteReminder(Reminder reminder);
    
    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    void deleteRemindersByTaskId(int taskId);
    
    @Query("UPDATE reminders SET isActive = 0 WHERE id = :id")
    void deactivateReminder(int id);
    
    @Query("UPDATE reminders SET isActive = 1 WHERE id = :id")
    void activateReminder(int id);
}
