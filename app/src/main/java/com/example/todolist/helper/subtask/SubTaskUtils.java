package com.example.todolist.helper.subtask;

import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;

import java.util.ArrayList;
import java.util.List;

public class SubTaskUtils {
    
    public static boolean areAllSubTasksCompleted(Task task) {
        if (task == null || task.getSubTasks() == null || task.getSubTasks().isEmpty()) {
            return true;
        }
        
        for (SubTask subTask : task.getSubTasks()) {
            if (!subTask.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    public static int getCompletedSubTasksCount(Task task) {
        if (task == null || task.getSubTasks() == null || task.getSubTasks().isEmpty()) {
            return 0;
        }
        
        int completedCount = 0;
        for (SubTask subTask : task.getSubTasks()) {
            if (subTask.isCompleted()) {
                completedCount++;
            }
        }
        return completedCount;
    }

    public static int getTotalSubTasksCount(Task task) {
        if (task == null || task.getSubTasks() == null) {
            return 0;
        }
        return task.getSubTasks().size();
    }

    public static String getSubTasksProgressInfo(Task task) {
        if (task == null || task.getSubTasks() == null || task.getSubTasks().isEmpty()) {
            return "Không có subtask";
        }
        
        int completedCount = getCompletedSubTasksCount(task);
        int totalCount = getTotalSubTasksCount(task);
        
        return completedCount + "/" + totalCount + " subtasks hoàn thành";
    }

    public static float getSubTasksCompletionPercentage(Task task) {
        if (task == null || task.getSubTasks() == null || task.getSubTasks().isEmpty()) {
            return 100.0f; // No subtasks means 100% complete
        }
        
        int completedCount = getCompletedSubTasksCount(task);
        int totalCount = getTotalSubTasksCount(task);
        
        return (float) completedCount / totalCount * 100.0f;
    }

    public static List<SubTask> getEmptySubTasks(List<SubTask> subTasks) {
        List<SubTask> emptySubTasks = new ArrayList<>();
        if (subTasks == null) {
            return emptySubTasks;
        }
        
        for (SubTask subTask : subTasks) {
            if (subTask.getTitle() == null || subTask.getTitle().trim().isEmpty()) {
                emptySubTasks.add(subTask);
            }
        }
        return emptySubTasks;
    }

    public static boolean isValidSubTask(SubTask subTask) {
        return subTask != null && 
               subTask.getTitle() != null && 
               !subTask.getTitle().trim().isEmpty();
    }

    public static List<SubTask> markAllSubTasksAsCompleted(Task task) {
        List<SubTask> modifiedSubTasks = new ArrayList<>();
        if (task == null || task.getSubTasks() == null) {
            return modifiedSubTasks;
        }
        
        for (SubTask subTask : task.getSubTasks()) {
            if (!subTask.isCompleted()) {
                subTask.setCompleted(true);
                modifiedSubTasks.add(subTask);
            }
        }
        return modifiedSubTasks;
    }

    public static List<SubTask> getIncompleteSubTasks(Task task) {
        List<SubTask> incompleteSubTasks = new ArrayList<>();
        if (task == null || task.getSubTasks() == null) {
            return incompleteSubTasks;
        }
        
        for (SubTask subTask : task.getSubTasks()) {
            if (!subTask.isCompleted()) {
                incompleteSubTasks.add(subTask);
            }
        }
        return incompleteSubTasks;
    }
}
