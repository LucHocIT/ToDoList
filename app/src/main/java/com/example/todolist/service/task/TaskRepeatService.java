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
     * Cập nhật ngày đến hạn cho task lặp lại khi hoàn thành
     * Thay vì tạo nhiều task clone, chỉ cần cập nhật ngày đến hạn
     */
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
            
            // Cập nhật ngày đến hạn
            task.setDueDate(dateFormat.format(calendar.getTime()));
            
            // Đặt lại trạng thái chưa hoàn thành
            task.setIsCompleted(false);
            task.setCompletionDate(null);
            
        } catch (Exception e) {
            android.util.Log.e("TaskRepeatService", "Error updating repeat task: " + e.getMessage());
        }
    }
    
    /**
     * DEPRECATED: Không còn tạo instances clone nữa
     * Giữ lại để tương thích với code cũ
     */
    @Deprecated
    public static void createRepeatInstances(Task originalTask, TaskCreator taskCreator, RepeatTaskCallback callback) {
        // Không làm gì cả - chỉ callback success
        if (callback != null) callback.onSuccess();
    }
    
    /**
     * Clone task cho ngày mới (giữ lại cho future use)
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
        // Không cần tạo instances nữa
        return false;
    }
}
