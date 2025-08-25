package com.example.todolist.service.task;

import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import com.example.todolist.repository.BaseRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskRepeatService {
    
    public interface RepeatTaskCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface TaskCreator {
        void addTaskWithoutRepeat(Task task, BaseRepository.DatabaseCallback<String> callback);
    }
    
    /**
     * Tạo các task lặp lại cho task có repeat
     * Tạo các instance cho 1 tháng tiếp theo
     */
    public static void createRepeatInstances(Task originalTask, TaskCreator taskCreator, RepeatTaskCallback callback) {
        if (!originalTask.isRepeating() || originalTask.getRepeatType() == null || originalTask.getRepeatType().equals("Không")) {
            if (callback != null) callback.onSuccess();
            return;
        }
        
        String repeatType = originalTask.getRepeatType();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        try {
            Date originalDate = dateFormat.parse(originalTask.getDueDate());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(originalDate);
            
            // Tạo instance cho 4 tuần tiếp theo (đủ để hiển thị)
            for (int i = 1; i <= 4; i++) {
                Calendar nextDate = (Calendar) calendar.clone();
                
                switch (repeatType) {
                    case "Hàng ngày":
                    case "Hằng ngày":
                        nextDate.add(Calendar.DAY_OF_MONTH, i);
                        break;
                    case "Hàng tuần":
                    case "Hằng tuần":
                        nextDate.add(Calendar.WEEK_OF_YEAR, i);
                        break;
                    case "Hàng tháng":
                    case "Hằng tháng":
                        nextDate.add(Calendar.MONTH, i);
                        break;
                    case "Hàng năm":
                    case "Hằng năm":
                        nextDate.add(Calendar.YEAR, i);
                        break;
                    default:
                        continue;
                }
                
                // Tạo task clone
                Task clonedTask = cloneTaskForDate(originalTask, dateFormat.format(nextDate.getTime()));
                
                // Thêm task clone (không tạo repeat instances cho clone)
                taskCreator.addTaskWithoutRepeat(clonedTask, new BaseRepository.DatabaseCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        // Instance tạo thành công
                    }

                    @Override
                    public void onError(String error) {
                        // Log error nhưng không fail cả flow
                    }
                });
            }
            
            if (callback != null) callback.onSuccess();
            
        } catch (Exception e) {
            if (callback != null) callback.onError("Lỗi tạo repeat instances: " + e.getMessage());
        }
    }
    
    /**
     * Clone task cho ngày mới
     */
    private static Task cloneTaskForDate(Task originalTask, String newDate) {
        Task clonedTask = new Task();
        
        // Copy tất cả properties trừ ID và completion status
        clonedTask.setTitle(originalTask.getTitle());
        clonedTask.setDescription(originalTask.getDescription());
        clonedTask.setDueDate(newDate);
        clonedTask.setDueTime(originalTask.getDueTime());
        clonedTask.setCategory(originalTask.getCategory());
        clonedTask.setReminderType(originalTask.getReminderType());
        clonedTask.setHasReminder(originalTask.isHasReminder());
        clonedTask.setAttachments(originalTask.getAttachments());
        clonedTask.setIsImportant(originalTask.isImportant());
        
        // KHÔNG copy repeat properties để tránh infinite loop
        clonedTask.setRepeatType("Không");
        clonedTask.setIsRepeating(false);
        
        // Chưa hoàn thành
        clonedTask.setIsCompleted(false);
        clonedTask.setCompletionDate(null);
        
        return clonedTask;
    }
    
    /**
     * Kiểm tra xem có cần tạo thêm repeat instances không
     */
    public static boolean needsRepeatInstances(Task task) {
        return task.isRepeating() && 
               task.getRepeatType() != null && 
               !task.getRepeatType().equals("Không");
    }
}
