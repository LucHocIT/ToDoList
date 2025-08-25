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
        List<Task> tasks = taskService.getAllTasksFromCache();
        allTasks.clear();
        for (Task task : tasks) {
            TodoTask todoTask = convertTaskToTodoTask(task);
            allTasks.add(todoTask);
        }
        updateTaskLists();
        if (listener != null) {
            listener.onTasksUpdated();
        }
        WidgetUpdateHelper.updateAllWidgets(context);
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
        return todoTask;
    }
    private void updateTaskLists() {
        overdueTasks.clear();
        todayTasks.clear();
        futureTasks.clear();
        completedTodayTasks.clear();
        Calendar now = Calendar.getInstance();
        String todayDateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.getTime());
        for (TodoTask task : allTasks) {
            if (task.isCompleted()) {
                if (isTaskCompletedToday(task, todayDateStr)) {
                    completedTodayTasks.add(task);
                }
            } else {
                int timeCategory = getTaskTimeCategory(task, now, todayDateStr);
                switch (timeCategory) {
                    case 0: 
                        overdueTasks.add(task);
                        break;
                    case 1: 
                        todayTasks.add(task);
                        break;
                    case 2: 
                        futureTasks.add(task);
                        break;
                }
            }
        }
    }
    private boolean isTaskCompletedToday(TodoTask task, String todayDateStr) {
        String completionDate = task.getCompletionDate();
        if (completionDate == null || completionDate.isEmpty()) {
            return false; 
        }
        return completionDate.equals(todayDateStr);
    }
    private int getTaskTimeCategory(TodoTask task, Calendar now, String todayDateStr) {
        try {
            String taskDateStr = task.getDueDate();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date taskDate = dateFormat.parse(taskDateStr);
            Date todayDate = dateFormat.parse(todayDateStr);
            if (taskDate.before(todayDate)) {
                return 0;
            } else if (taskDate.equals(todayDate)) {
                return 1; 
            } else {
                return 2; 
            }
        } catch (Exception e) {
            return 1; 
        }
    }
    public void completeTask(TodoTask task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        if (isCompleted) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            task.setCompletionDate(dateFormat.format(new Date()));
        } else {
            task.setCompletionDate(null);
        }
        // Convert TodoTask to Task for update
        Task taskToUpdate = convertTodoTaskToTask(task);
        taskService.updateTask(taskToUpdate);
        ReminderScheduler scheduler = new ReminderScheduler(context);
        if (isCompleted) {
            try {
                int taskId = Integer.parseInt(task.getId());
                scheduler.cancelTaskReminders(taskId);
            } catch (NumberFormatException e) {

            }
        } else {
            if (task.isHasReminder()) {
                Task taskForScheduler = convertTodoTaskToTask(task);
                scheduler.scheduleTaskReminder(taskForScheduler);
            }
        }
        loadTasks();
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
        return task;
    }
    public void toggleTaskImportant(TodoTask task) {
        task.setImportant(!task.isImportant());
        Task taskToUpdate = convertTodoTaskToTask(task);
        taskService.updateTask(taskToUpdate);
        loadTasks();
        String message = task.isImportant() ? "Đã đánh dấu quan trọng" : "Đã bỏ đánh dấu quan trọng";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    public void deleteTask(TodoTask task) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_task_title))
                .setMessage(context.getString(R.string.confirm_delete_task_message))
                .setPositiveButton(context.getString(R.string.delete_button_text), (dialog, which) -> { 
                    Task taskToDelete = convertTodoTaskToTask(task);
                    taskService.deleteTask(taskToDelete);
                    loadTasks();
                    Toast.makeText(context, context.getString(R.string.task_deleted_toast), Toast.LENGTH_SHORT).show(); 
                })
                .setNegativeButton(context.getString(R.string.cancel_button_text), null) 
                .show();
    }

    public List<TodoTask> getAllTasks() { return allTasks; }
    public List<TodoTask> getOverdueTasks() { return overdueTasks; }
    public List<TodoTask> getTodayTasks() { return todayTasks; }
    public List<TodoTask> getFutureTasks() { return futureTasks; }
    public List<TodoTask> getCompletedTodayTasks() { return completedTodayTasks; }
    public void rescheduleAllReminders() {
        List<Task> tasks = taskService.getAllTasksFromCache();
        
        ReminderScheduler scheduler = new ReminderScheduler(context);
        for (Task task : tasks) {
            if (!task.isCompleted() && task.isHasReminder()) {
                scheduler.scheduleTaskReminder(task);
            }
        }
    }
}
