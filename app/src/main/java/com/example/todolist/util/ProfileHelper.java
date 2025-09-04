package com.example.todolist.util;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupMenu;

import com.example.todolist.R;
import com.example.todolist.model.Task;
import com.example.todolist.model.Category;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.service.CategoryService;
import com.example.todolist.view.SimplePieChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileHelper {
    
    private Context context;
    private TaskRepository taskRepository;
    
    public ProfileHelper(Context context) {
        this.context = context;
        this.taskRepository = new TaskRepository(context);
    }

    public interface WeekNavigationListener {
        void onWeekChanged(String weekRange);
        void onNextWeekVisibilityChanged(boolean visible);
    }

    public interface StatisticsListener {
        void onStatisticsLoaded(int completedTasks, int pendingTasks);
        void onStatisticsError();
    }

    public interface ChartUpdateListener {
        void onChartUpdated(int[] taskCounts, boolean hasData);
    }

    public interface PieChartListener {
        void onPieChartUpdated(Map<String, Integer> categoryCount);
    }
    
    public boolean isSameWeek(Calendar week1, Calendar week2) {
        return week1.get(Calendar.YEAR) == week2.get(Calendar.YEAR) &&
               week1.get(Calendar.WEEK_OF_YEAR) == week2.get(Calendar.WEEK_OF_YEAR);
    }

    public void updateWeekDisplay(Calendar currentWeekStart, WeekNavigationListener listener) {
        SimpleDateFormat format = new SimpleDateFormat("M/d", Locale.getDefault());
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        
        String weekRange = format.format(currentWeekStart.getTime()) + "-" + format.format(weekEnd.getTime());
        
        // Check if current week is the actual current week
        Calendar currentActualWeek = Calendar.getInstance();
        int dayOfWeek = currentActualWeek.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        currentActualWeek.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
        
        // Hide next week button if we're viewing current week
        boolean showNextWeek = !isSameWeek(currentWeekStart, currentActualWeek);
        
        listener.onWeekChanged(weekRange);
        listener.onNextWeekVisibilityChanged(showNextWeek);
    }
    
    /**
     * Load and calculate statistics
     */
    public void loadStatistics(StatisticsListener listener) {
        taskRepository.getAllTasks(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                int completedTasks = 0;
                int pendingTasks = 0;
                
                for (Task task : tasks) {
                    if (task.isCompleted()) {
                        completedTasks++;
                    } else {
                        pendingTasks++;
                    }
                }
                
                listener.onStatisticsLoaded(completedTasks, pendingTasks);
            }

            @Override
            public void onError(String error) {
                listener.onStatisticsError();
            }
        });
    }

    public void updateWeeklyChart(Calendar currentWeekStart, ChartUpdateListener listener) {
        taskRepository.getCompletedTasks(new TaskRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> completedTasks) {
                int[] taskCounts = calculateWeeklyTaskCounts(completedTasks, currentWeekStart);
                boolean hasData = hasAnyData(taskCounts);
                listener.onChartUpdated(taskCounts, hasData);
            }

            @Override
            public void onError(String error) {
                listener.onChartUpdated(new int[7], false);
            }
        });
    }

    private int[] calculateWeeklyTaskCounts(List<Task> completedTasks, Calendar currentWeekStart) {
        Calendar weekStart = (Calendar) currentWeekStart.clone();
        int[] taskCounts = new int[7]; // Sunday=0, Monday=1, ..., Saturday=6
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        for (Task task : completedTasks) {
            if (task.isCompleted() && task.getCompletionDate() != null) {
                try {
                    Calendar taskDate = Calendar.getInstance();
                    taskDate.setTime(dateFormat.parse(task.getCompletionDate()));
                    
                    Calendar weekEnd = (Calendar) weekStart.clone();
                    weekEnd.add(Calendar.DAY_OF_YEAR, 6);
                    
                    if (!taskDate.before(weekStart) && !taskDate.after(weekEnd)) {
                        int dayOfWeek = taskDate.get(Calendar.DAY_OF_WEEK) - 1;
                        taskCounts[dayOfWeek]++;
                    }
                } catch (Exception e) {
                    // Skip invalid dates
                }
            }
        }
        
        return taskCounts;
    }
    
    /**
     * Check if any day has data
     */
    private boolean hasAnyData(int[] taskCounts) {
        for (int count : taskCounts) {
            if (count > 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Update pie chart with category data
     */
    public void updatePieChart(PieChartListener listener) {
        taskRepository.getIncompleteTasks(new TaskRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> incompleteTasks) {
                CategoryService categoryService = new CategoryService(context, null);
                categoryService.getAllCategories(new BaseRepository.ListCallback<Category>() {
                    @Override
                    public void onSuccess(List<Category> categories) {
                        Map<String, Integer> categoryCount = calculateCategoryCounts(incompleteTasks, categories);
                        listener.onPieChartUpdated(categoryCount);
                    }

                    @Override
                    public void onError(String error) {
                        listener.onPieChartUpdated(new HashMap<>());
                    }
                });
            }

            @Override
            public void onError(String error) {
                listener.onPieChartUpdated(new HashMap<>());
            }
        });
    }
    
    /**
     * Calculate category counts for pie chart
     */
    private Map<String, Integer> calculateCategoryCounts(List<Task> incompleteTasks, List<Category> categories) {
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, String> categoryNames = new HashMap<>();
        
        for (Category category : categories) {
            categoryNames.put(category.getId(), category.getName());
        }
        
        for (Task task : incompleteTasks) {
            String categoryId = task.getCategoryId();
            String categoryName;
            
            if (categoryId == null || categoryId.isEmpty()) {
                categoryName = "Không có thể loại";
            } else {
                categoryName = categoryNames.getOrDefault(categoryId, "Không có thể loại");
            }
            
            categoryCount.put(categoryName, categoryCount.getOrDefault(categoryName, 0) + 1);
        }
        
        return categoryCount;
    }
    
    /**
     * Create and show filter popup menu
     */
    public void showFilterDialog(View anchor, TextView filterText) {
        PopupMenu popup = new PopupMenu(context, anchor);
        
        popup.getMenu().add(0, 0, 0, "Tất cả");
        popup.getMenu().add(0, 1, 1, "Trong 7 ngày nữa");
        popup.getMenu().add(0, 2, 2, "Trong 30 ngày nữa");
        
        popup.setOnMenuItemClickListener(item -> {
            filterText.setText(item.getTitle());
            // TODO: Apply filter logic based on item.getItemId()
            return true;
        });
        
        popup.show();
    }
    
    /**
     * Update category legend with colors and counts
     */
    public void updateCategoryLegend(LinearLayout legendContainer, Map<String, Integer> categoryCount) {
        legendContainer.removeAllViews();
        
        int[] colors = {
            0xFF5C9CFF, 0xFF9CC3FF, 0xFFC8DFFF, 0xFF4A90E2, 0xFF2E7BD6,
            0xFF8BB8FF, 0xFF6FA8FF, 0xFF5A9BFF, 0xFFB8D4FF, 0xFF7AB3FF
        };
        int colorIndex = 0;
        
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            String categoryName = entry.getKey();
            Integer count = entry.getValue();
            
            LinearLayout legendItem = createLegendItem(categoryName, count, colors[colorIndex % colors.length]);
            legendContainer.addView(legendItem);
            colorIndex++;
        }
        
        if (categoryCount.isEmpty()) {
            TextView noDataText = createNoDataText();
            legendContainer.addView(noDataText);
        }
    }

    private LinearLayout createLegendItem(String categoryName, Integer count, int color) {
        LinearLayout legendItem = new LinearLayout(context);
        legendItem.setOrientation(LinearLayout.HORIZONTAL);
        legendItem.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.bottomMargin = (int) (8 * context.getResources().getDisplayMetrics().density);
        legendItem.setLayoutParams(itemParams);
        
        // Color indicator
        View colorView = new View(context);
        LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(
            (int) (12 * context.getResources().getDisplayMetrics().density),
            (int) (12 * context.getResources().getDisplayMetrics().density)
        );
        colorParams.rightMargin = (int) (8 * context.getResources().getDisplayMetrics().density);
        colorView.setLayoutParams(colorParams);
        colorView.setBackgroundColor(color);
        legendItem.addView(colorView);

        TextView nameTextView = new TextView(context);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        nameTextView.setLayoutParams(nameParams);
        nameTextView.setText(categoryName);
        nameTextView.setTextSize(14);
        nameTextView.setTextColor(context.getColor(android.R.color.black));
        legendItem.addView(nameTextView);

        TextView countTextView = new TextView(context);
        countTextView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        countTextView.setText(String.valueOf(count));
        countTextView.setTextSize(14);
        countTextView.setTextColor(0xFF666666);
        legendItem.addView(countTextView);
        
        return legendItem;
    }

    private TextView createNoDataText() {
        TextView noDataText = new TextView(context);
        noDataText.setText("Không có nhiệm vụ chưa hoàn thành");
        noDataText.setTextSize(14);
        noDataText.setTextColor(0xFF999999);
        noDataText.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams noDataParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        noDataParams.topMargin = (int) (16 * context.getResources().getDisplayMetrics().density);
        noDataText.setLayoutParams(noDataParams);
        return noDataText;
    }
}
