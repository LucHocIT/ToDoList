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

    public static void updateTaskForNextRepeat(Task task) {
        if (!task.isRepeating() || task.getRepeatType() == null || task.getRepeatType().equals("Không")) {
            return;
        }
        
        String repeatType = task.getRepeatType();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        try {
            Date currentDate = dateFormat.parse(task.getDueDate());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            
            // Tính ngày tiếp theo dựa trên loại lặp lại
            switch (repeatType) {
                case "Hàng ngày":
                case "Hằng ngày":
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case "Hàng tuần":
                case "Hằng tuần":
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case "Hàng tháng":
                case "Hằng tháng":
                    calendar.add(Calendar.MONTH, 1);
                    break;
                case "Hàng năm":
                case "Hằng năm":
                    calendar.add(Calendar.YEAR, 1);
                    break;
            }

            task.setDueDate(dateFormat.format(calendar.getTime()));
            task.setIsCompleted(false);
            task.setCompletionDate(null);
            
        } catch (Exception e) {
            android.util.Log.e("TaskRepeatService", "Error updating repeat task: " + e.getMessage());
        }
    }
    
    @Deprecated
    public static void createRepeatInstances(Task originalTask, TaskCreator taskCreator, RepeatTaskCallback callback) {
        // Không làm gì cả - chỉ callback success
        if (callback != null) callback.onSuccess();
    }

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

    public static boolean needsRepeatInstances(Task task) {
        return false;
    }
}
