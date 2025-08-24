package com.example.todolist.util;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.todolist.R;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import com.example.todolist.repository.BaseRepository;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
public class CalendarTaskHelper {
    public static void loadTasksForDate(Context context, String dateString, 
                                      TaskLoadListener listener) {
        TaskService taskService = new TaskService(context, null);
        taskService.getAllTasks(new BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> allTasks) {
                List<Task> tasksForDate = new ArrayList<>();
                for (Task task : allTasks) {
                    if (CalendarUtils.isTaskOnDate(task, dateString)) {
                        tasksForDate.add(task);
                    }
                }
                if (listener != null) {
                    listener.onTasksLoaded(tasksForDate);
                }
            }
            @Override
            public void onError(String error) {
                if (listener != null) {
                    listener.onTasksLoaded(new ArrayList<>());
                }
            }
        });
    }
    public static void updateTaskDisplay(Context context, LinearLayout container, 
                                       List<Task> tasks) {
        container.removeAllViews();
        if (tasks.isEmpty()) {
            addEmptyTaskMessage(context, container);
        } else {
            for (Task task : tasks) {
                View taskItemView = TaskItemViewHelper.createTaskItemView(context, task);
                container.addView(taskItemView);
            }
        }
    }
    private static void addEmptyTaskMessage(Context context, LinearLayout container) {
        TextView noTasksView = new TextView(context);
        noTasksView.setText(context.getString(R.string.no_tasks_today_message));
        noTasksView.setTextSize(16);
        noTasksView.setTextColor(Color.parseColor("#666666"));
        noTasksView.setGravity(android.view.Gravity.CENTER);
        noTasksView.setPadding(16, 32, 16, 8);
        container.addView(noTasksView);
        TextView addTaskPrompt = new TextView(context);
        addTaskPrompt.setText(context.getString(R.string.add_task_prompt));
        addTaskPrompt.setTextSize(14);
        addTaskPrompt.setTextColor(Color.parseColor("#999999"));
        addTaskPrompt.setGravity(android.view.Gravity.CENTER);
        addTaskPrompt.setPadding(16, 0, 16, 32);
        container.addView(addTaskPrompt);
    }
    public static String formatSelectedDate(Calendar selectedDate, int selectedDay) {
        return String.format("%02d/%02d/%04d", 
            selectedDay,
            selectedDate.get(Calendar.MONTH) + 1,
            selectedDate.get(Calendar.YEAR));
    }
    public interface TaskLoadListener {
        void onTasksLoaded(List<Task> tasks);
    }
}
