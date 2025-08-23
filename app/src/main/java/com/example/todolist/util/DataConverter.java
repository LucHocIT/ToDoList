package com.example.todolist.util;
import com.example.todolist.model.Task;
import com.example.todolist.model.Category;
import java.util.ArrayList;
import java.util.List;

public class DataConverter {
    /**
     * Validate Task object before Firebase operations
     * @param task Task to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidTask(Task task) {
        if (task == null) return false;
        // Title is required
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return false;
        }
        // Category should not be null
        if (task.getCategory() == null) {
            task.setCategory("KhĂ´ng cĂ³ thá»ƒ loáº¡i");
        }
        // Reminder type should not be null
        if (task.getReminderType() == null) {
            task.setReminderType("ThĂ´ng bĂ¡o");
        }
        // Repeat type should not be null
        if (task.getRepeatType() == null) {
            task.setRepeatType("KhĂ´ng láº·p láº¡i");
        }
        return true;
    }
    /**
     * Validate Category object before Firebase operations
     * @param category Category to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidCategory(Category category) {
        if (category == null) return false;
        // Name is required
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            return false;
        }
        // Color should not be null
        if (category.getColor() == null) {
            category.setColor("#4CAF50"); // Default green
        }
        return true;
    }
    /**
     * Clean and normalize task title
     * @param title Raw title
     * @return Cleaned title
     */
    public static String cleanTaskTitle(String title) {
        if (title == null) return "";
        return title.trim().replaceAll("\\s+", " ");
    }
    /**
     * Clean and normalize category name
     * @param name Raw category name
     * @return Cleaned category name
     */
    public static String cleanCategoryName(String name) {
        if (name == null) return "";
        return name.trim().replaceAll("\\s+", " ");
    }
    /**
     * Create default task for testing
     * @return A valid default task
     */
    public static Task createDefaultTask() {
        Task task = new Task();
        task.setTitle("Sample Task");
        task.setDescription("This is a sample task created by Firebase");
        task.setCategory("CĂ¡ nhĂ¢n");
        task.setReminderType("ThĂ´ng bĂ¡o");
        task.setRepeatType("KhĂ´ng láº·p láº¡i");
        task.setCompleted(false);
        task.setImportant(false);
        task.setHasReminder(false);
        task.setRepeating(false);
        // Set current date
        long now = System.currentTimeMillis();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        String dateStr = String.format("%04d/%02d/%02d",
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        );
        task.setDueDate(dateStr);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }
    /**
     * Create default category for testing
     * @param name Category name
     * @param color Category color
     * @return A valid default category
     */
    public static Category createDefaultCategory(String name, String color) {
        Category category = new Category();
        category.setName(name);
        category.setColor(color);
        category.setSortOrder(0);
        category.setIsDefault(false);
        long now = System.currentTimeMillis();
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        return category;
    }
    /**
     * Validate list of tasks
     * @param tasks List of tasks to validate
     * @return List of valid tasks only
     */
    public static List<Task> validateTasks(List<Task> tasks) {
        List<Task> validTasks = new ArrayList<>();
        if (tasks != null) {
            for (Task task : tasks) {
                if (isValidTask(task)) {
                    validTasks.add(task);
                }
            }
        }
        return validTasks;
    }
    /**
     * Validate list of categories
     * @param categories List of categories to validate
     * @return List of valid categories only
     */
    public static List<Category> validateCategories(List<Category> categories) {
        List<Category> validCategories = new ArrayList<>();
        if (categories != null) {
            for (Category category : categories) {
                if (isValidCategory(category)) {
                    validCategories.add(category);
                }
            }
        }
        return validCategories;
    }
}
