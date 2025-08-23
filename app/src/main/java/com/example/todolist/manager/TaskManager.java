package com.example.todolist.manager;
import android.content.Context;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.todolist.R;
import com.example.todolist.model.TodoTask;
import com.example.todolist.notification.ReminderScheduler;
import com.example.todolist.widget.WidgetUpdateHelper;
import com.example.todolist.service.TaskService;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.model.Task;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class TaskManager {
    public interface TaskUpdateListener {
        void onTasksUpdated();
    }
    private Context context;
    private TaskService taskService;
    private TaskUpdateListener listener;
    private List<TodoTask> allTasks;
    private List<TodoTask> overdueTasks;
    private List<TodoTask> todayTasks;
    private List<TodoTask> futureTasks;
    private List<TodoTask> completedTodayTasks;
    public TaskManager(Context context, TaskUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.taskService = new TaskService(context, null);
        initializeLists();
    }
    private void initializeLists() {
        allTasks = new ArrayList<>();
        overdueTasks = new ArrayList<>();
        todayTasks = new ArrayList<>();
        futureTasks = new ArrayList<>();
        completedTodayTasks = new ArrayList<>();
    }
    public void loadTasks() {
        taskService.getAllTasks(new BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                // Convert Task to TodoTask for compatibility
                allTasks.clear();
                for (Task task : tasks) {
                    TodoTask todoTask = convertTaskToTodoTask(task);
                    allTasks.add(todoTask);
                }
                updateTaskLists();
                if (listener != null) {
                    listener.onTasksUpdated();
                }
                // Update widget whenever tasks are loaded
                WidgetUpdateHelper.updateAllWidgets(context);
            }
            @Override
            public void onError(String error) {
                // Handle error
            }
        });
    }
    private TodoTask convertTaskToTodoTask(Task task) {
        TodoTask todoTask = new TodoTask();
        todoTask.setId(task.getId());
        todoTask.setTitle(task.getTitle());
        todoTask.setDescription(task.getDescription());
        todoTask.setDueDate(task.getDueDate());
        todoTask.setDueTime(task.getDueTime());
        todoTask.setCompleted(task.isCompleted());
        todoTask.setImportant(task.isImportant());
        todoTask.setCategoryId(task.getCategoryId());
        todoTask.setReminderType(task.getReminderType());
        todoTask.setHasReminder(task.isHasReminder());
        todoTask.setAttachments(task.getAttachments());
        todoTask.setRepeatType(task.getRepeatType());
        // Handle other fields as needed
        return todoTask;
    }
    private void updateTaskLists() {
        overdueTasks.clear();
        todayTasks.clear();
        futureTasks.clear();
        completedTodayTasks.clear();
        Calendar now = Calendar.getInstance();
        String todayDateStr = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(now.getTime());
        for (TodoTask task : allTasks) {
            if (task.isCompleted()) {
                if (isTaskCompletedToday(task, todayDateStr)) {
                    completedTodayTasks.add(task);
                }
            } else {
                int timeCategory = getTaskTimeCategory(task, now, todayDateStr);
                switch (timeCategory) {
                    case 0: // Overdue
                        overdueTasks.add(task);
                        break;
                    case 1: // Today
                        todayTasks.add(task);
                        break;
                    case 2: // Future
                        futureTasks.add(task);
                        break;
                }
            }
        }
    }
    private boolean isTaskCompletedToday(TodoTask task, String todayDateStr) {
        // Check if task was completed today by comparing completion date
        String completionDate = task.getCompletionDate();
        if (completionDate == null || completionDate.isEmpty()) {
            return false; // No completion date means not completed or very old completion
        }
        // Compare completion date with today's date
        return completionDate.equals(todayDateStr);
    }
    private int getTaskTimeCategory(TodoTask task, Calendar now, String todayDateStr) {
        try {
            String taskDateStr = task.getDueDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date taskDate = dateFormat.parse(taskDateStr);
            Date todayDate = dateFormat.parse(todayDateStr);
            if (taskDate.before(todayDate)) {
                return 0; // Overdue
            } else if (taskDate.equals(todayDate)) {
                return 1; // Today
            } else {
                return 2; // Future
            }
        } catch (Exception e) {
            return 1; // Default to today
        }
    }
    public void completeTask(TodoTask task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        // Set completion date when marking as completed, clear when marking as incomplete
        if (isCompleted) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            task.setCompletionDate(dateFormat.format(new Date()));
        } else {
            task.setCompletionDate(null);
        }
        // Convert TodoTask to Task for update
        Task taskToUpdate = convertTodoTaskToTask(task);
        taskService.updateTask(taskToUpdate, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                // Handle reminder scheduling based on completion status
                ReminderScheduler scheduler = new ReminderScheduler(context);
                if (isCompleted) {
                    // Cancel reminders for completed tasks
                    try {
                        int taskId = Integer.parseInt(task.getId());
                        scheduler.cancelTaskReminders(taskId);
                    } catch (NumberFormatException e) {
                        // Handle invalid task ID
                    }
                } else {
                    // Reschedule reminders for uncompleted tasks
                    if (task.isHasReminder()) {
                        // Create Task object for scheduler
                        Task taskForScheduler = convertTodoTaskToTask(task);
                        scheduler.scheduleTaskReminder(taskForScheduler);
                    }
                }
                loadTasks();
            }
            @Override
            public void onError(String error) {
                // Handle error
            }
        });
    }
    private Task convertTodoTaskToTask(TodoTask todoTask) {
        Task task = new Task();
        task.setId(todoTask.getId());
        task.setTitle(todoTask.getTitle());
        task.setDescription(todoTask.getDescription());
        task.setDueDate(todoTask.getDueDate());
        task.setDueTime(todoTask.getDueTime());
        task.setIsCompleted(todoTask.isCompleted());
        task.setIsImportant(todoTask.isImportant());
        task.setCategoryId(todoTask.getCategoryId());
        task.setReminderType(todoTask.getReminderType());
        task.setHasReminder(todoTask.isHasReminder());
        task.setAttachments(todoTask.getAttachments());
        task.setRepeatType(todoTask.getRepeatType());
        // Handle other fields as needed
        return task;
    }
    public void toggleTaskImportant(TodoTask task) {
        task.setImportant(!task.isImportant());
        // Convert TodoTask to Task for update
        Task taskToUpdate = convertTodoTaskToTask(task);
        taskService.updateTask(taskToUpdate, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadTasks();
            }
            @Override
            public void onError(String error) {
                // Handle error
            }
        });
        String message = task.isImportant() ? "Đã đánh dấu quan trọng" : "Đã bỏ đánh dấu quan trọng";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    public void deleteTask(TodoTask task) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_task_title))
                .setMessage(context.getString(R.string.confirm_delete_task_message))
                .setPositiveButton(context.getString(R.string.delete_button_text), (dialog, which) -> { // Thay thế "Xóa"
                    Task taskToDelete = convertTodoTaskToTask(task);
                    taskService.deleteTask(taskToDelete, new BaseRepository.DatabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            loadTasks();
                        }
                        @Override
                        public void onError(String error) {
                            // Handle error
                        }
                    });
                    Toast.makeText(context, context.getString(R.string.task_deleted_toast), Toast.LENGTH_SHORT).show(); // Thay thế "Đã xóa nhiệm vụ"
                })
                .setNegativeButton(context.getString(R.string.cancel_button_text), null) // Thay thế "Hủy"
                .show();
    }
    // Getters
    public List<TodoTask> getAllTasks() { return allTasks; }
    public List<TodoTask> getOverdueTasks() { return overdueTasks; }
    public List<TodoTask> getTodayTasks() { return todayTasks; }
    public List<TodoTask> getFutureTasks() { return futureTasks; }
    public List<TodoTask> getCompletedTodayTasks() { return completedTodayTasks; }
    /**
     * Lên lịch lại tất cả thông báo cho các task chưa hoàn thành
     */
    public void rescheduleAllReminders() {
        taskService.getAllTasks(new BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                ReminderScheduler scheduler = new ReminderScheduler(context);
                for (Task task : tasks) {
                    if (!task.isCompleted() && task.isHasReminder()) {
                        scheduler.scheduleTaskReminder(task);
                    }
                }
            }
            @Override
            public void onError(String error) {
                // Handle error
            }
        });
    }
}
