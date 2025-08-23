package com.example.todolist.manager;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.example.todolist.R;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.model.Task;
import com.example.todolist.util.SortType;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
public class FilterManager {
    public interface FilterListener {
        void onFilterChanged(String filter);
        void onEmptyStateChanged(boolean isEmpty, String message);
    }
    private Context context;
    private FilterListener listener;
    private String currentFilter = "all";
    private SortType currentSortType = SortType.DATE_TIME;
    // UI Components
    private MaterialButton btnAll, btnWork, btnPersonal, btnFavorite;
    private LinearLayout layoutCategoriesContainer;
    private View layoutEmptyState;
    private TextView tvEmptyTitle;
    // Task lists (original)
    private List<Task> overdueTasks;
    private List<Task> todayTasks;
    private List<Task> futureTasks;
    private List<Task> completedTodayTasks;
    // Filtered lists
    private List<Task> filteredOverdueTasks;
    private List<Task> filteredTodayTasks;
    private List<Task> filteredFutureTasks;
    private List<Task> filteredCompletedTodayTasks;
    // Adapters
    private TaskAdapter overdueTasksAdapter;
    private TaskAdapter todayTasksAdapter;
    private TaskAdapter futureTasksAdapter;
    private TaskAdapter completedTodayTasksAdapter;
    public FilterManager(Context context, MaterialButton btnAll, MaterialButton btnWork, 
                        MaterialButton btnPersonal, MaterialButton btnFavorite,
                        LinearLayout layoutCategoriesContainer, View layoutEmptyState, 
                        TextView tvEmptyTitle, FilterListener listener) {
        this.context = context;
        this.btnAll = btnAll;
        this.btnWork = btnWork;
        this.btnPersonal = btnPersonal;
        this.btnFavorite = btnFavorite;
        this.layoutCategoriesContainer = layoutCategoriesContainer;
        this.layoutEmptyState = layoutEmptyState;
        this.tvEmptyTitle = tvEmptyTitle;
        this.listener = listener;
        initializeFilteredLists();
        setupFilterButtons();
    }
    private void initializeFilteredLists() {
        filteredOverdueTasks = new ArrayList<>();
        filteredTodayTasks = new ArrayList<>();
        filteredFutureTasks = new ArrayList<>();
        filteredCompletedTodayTasks = new ArrayList<>();
    }
    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> filterTasks("all"));
        btnWork.setOnClickListener(v -> filterTasks("công việc"));
        btnPersonal.setOnClickListener(v -> filterTasks("cá nhân"));
        btnFavorite.setOnClickListener(v -> filterTasks("yêu thích"));
        // Setup dynamic category button clicks
        setupDynamicCategoryClicks();
    }
    private void setupDynamicCategoryClicks() {
        // This method should be called after category buttons are created
        setupAllCategoryClicks();
    }
    public void setupAllCategoryClicks() {
        // Setup clicks for all category buttons including dynamic ones
        // Only setup for buttons that don't already have click listeners set
        int childrenCount = layoutCategoriesContainer.getChildCount();
        for (int i = 4; i < childrenCount; i++) {
            View child = layoutCategoriesContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                String categoryName = (String) button.getTag();
                if (categoryName != null && !categoryName.isEmpty()) {
                    // Clear any existing click listener to prevent multiple listeners
                    button.setOnClickListener(null);
                    // Set new click listener
                    button.setOnClickListener(v -> filterTasks(categoryName));
                }
            }
        }
    }
    public void setTaskLists(List<Task> overdueTasks, List<Task> todayTasks, 
                           List<Task> futureTasks, List<Task> completedTodayTasks) {
        this.overdueTasks = overdueTasks;
        this.todayTasks = todayTasks;
        this.futureTasks = futureTasks;
        this.completedTodayTasks = completedTodayTasks;
    }
    public void setAdapters(TaskAdapter overdueTasksAdapter, TaskAdapter todayTasksAdapter,
                          TaskAdapter futureTasksAdapter, TaskAdapter completedTodayTasksAdapter) {
        this.overdueTasksAdapter = overdueTasksAdapter;
        this.todayTasksAdapter = todayTasksAdapter;
        this.futureTasksAdapter = futureTasksAdapter;
        this.completedTodayTasksAdapter = completedTodayTasksAdapter;
    }
    public void filterTasks(String filter) {
        currentFilter = filter;
        // Update button states
        resetFilterButtons();
        highlightFilterButton(filter);
        // Filter tasks by category
        filteredOverdueTasks.clear();
        filteredTodayTasks.clear();
        filteredFutureTasks.clear();
        filteredCompletedTodayTasks.clear();
        if (filter.equalsIgnoreCase("all")) {
            // Show all tasks
            if (overdueTasks != null) filteredOverdueTasks.addAll(overdueTasks);
            if (todayTasks != null) filteredTodayTasks.addAll(todayTasks);
            if (futureTasks != null) filteredFutureTasks.addAll(futureTasks);
            if (completedTodayTasks != null) filteredCompletedTodayTasks.addAll(completedTodayTasks);
        } else {
            // Filter by specific category
            filterByCategory(overdueTasks, filteredOverdueTasks, filter);
            filterByCategory(todayTasks, filteredTodayTasks, filter);
            filterByCategory(futureTasks, filteredFutureTasks, filter);
            filterByCategory(completedTodayTasks, filteredCompletedTodayTasks, filter);
        }
        // Apply current sorting
        sortTasks();
        // Update adapters
        updateAdapters();
        // Check empty state
        updateEmptyState(filter);
        if (listener != null) {
            listener.onFilterChanged(filter);
        }
    }
    private void filterByCategory(List<Task> source, List<Task> destination, String filter) {
        if (source == null) return;
        for (Task task : source) {
            if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(filter)) {
                destination.add(task);
            }
        }
    }
    private void highlightFilterButton(String filter) {
        switch (filter.toLowerCase(Locale.getDefault())) {
            case "all":
                setButtonSelected(btnAll);
                break;
            case "cĂ´ng viá»‡c":
                setButtonSelected(btnWork);
                break;
            case "cĂ¡ nhĂ¢n":
                setButtonSelected(btnPersonal);
                break;
            case "yĂªu thĂ­ch":
                setButtonSelected(btnFavorite);
                break;
            default:
                highlightDynamicCategoryButton(filter);
                break;
        }
    }
    private void setButtonSelected(MaterialButton button) {
        button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary_blue));
        button.setTextColor(ContextCompat.getColor(context, android.R.color.white));
    }
    private void highlightDynamicCategoryButton(String categoryName) {
        for (int i = 4; i < layoutCategoriesContainer.getChildCount(); i++) {
            View child = layoutCategoriesContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                if (button.getText().toString().equalsIgnoreCase(categoryName)) {
                    setButtonSelected(button);
                    break;
                }
            }
        }
    }
    private void resetFilterButtons() {
        int grayColor = ContextCompat.getColor(context, R.color.light_gray);
        int textColor = ContextCompat.getColor(context, R.color.text_gray);
        setButtonUnselected(btnAll, grayColor, textColor);
        setButtonUnselected(btnWork, grayColor, textColor);
        setButtonUnselected(btnPersonal, grayColor, textColor);
        setButtonUnselected(btnFavorite, grayColor, textColor);
        // Reset dynamic category buttons
        for (int i = 4; i < layoutCategoriesContainer.getChildCount(); i++) {
            View child = layoutCategoriesContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                setButtonUnselected(button, grayColor, textColor);
            }
        }
    }
    private void setButtonUnselected(MaterialButton button, int bgColor, int textColor) {
        button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
        button.setTextColor(textColor);
    }
    public void setSortType(SortType sortType) {
        currentSortType = sortType;
        sortTasks();
        updateAdapters();
        String message = "Đã áp dụng sắp xếp: " + getSortTypeName(sortType);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    private String getSortTypeName(SortType sortType) {
        switch (sortType) {
            case DATE_TIME:
                return "Ngày và giờ đến hạn";
            case CREATION_TIME:
                return "Thời gian tạo tác vụ";
            case ALPHABETICAL:
                return "Bảng chữ cái A-Z";
            default:
                return "Mặc định";
        }
    }
    public void sortTasks() {
        sortTaskList(filteredOverdueTasks, currentSortType);
        sortTaskList(filteredTodayTasks, currentSortType);
        sortTaskList(filteredFutureTasks, currentSortType);
        sortTaskList(filteredCompletedTodayTasks, currentSortType);
    }
    private void sortTaskList(List<Task> tasks, SortType sortType) {
        switch (sortType) {
            case DATE_TIME:
                Collections.sort(tasks, (t1, t2) -> {
                    String dateTime1 = t1.getDueDate() + " " + t1.getDueTime();
                    String dateTime2 = t2.getDueDate() + " " + t2.getDueTime();
                    return dateTime1.compareTo(dateTime2);
                });
                break;
            case CREATION_TIME:
                Collections.sort(tasks, (t1, t2) -> t1.getId().compareToIgnoreCase(t2.getId()));
                break;
            case ALPHABETICAL:
                Collections.sort(tasks, (t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()));
                break;
        }
    }
    private void updateAdapters() {
        if (overdueTasksAdapter != null) {
            overdueTasksAdapter.updateTasks(filteredOverdueTasks);
        }
        if (todayTasksAdapter != null) {
            todayTasksAdapter.updateTasks(filteredTodayTasks);
        }
        if (futureTasksAdapter != null) {
            futureTasksAdapter.updateTasks(filteredFutureTasks);
        }
        if (completedTodayTasksAdapter != null) {
            completedTodayTasksAdapter.updateTasks(filteredCompletedTodayTasks);
        }
    }
    private void updateEmptyState(String filter) {
        boolean hasAnyTasks = !filteredOverdueTasks.isEmpty() || !filteredTodayTasks.isEmpty() ||
                !filteredFutureTasks.isEmpty() || !filteredCompletedTodayTasks.isEmpty();
        String message;
        if (hasAnyTasks) {
            layoutEmptyState.setVisibility(View.GONE);
            message = "";
        } else {
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (filter.equalsIgnoreCase("all")) {
                message = context.getString(R.string.no_tasks_message);
            } else {
                message = context.getString(R.string.no_tasks_in_category_message, filter);
            }
            tvEmptyTitle.setText(message);
        }
        if (listener != null) {
            listener.onEmptyStateChanged(!hasAnyTasks, message);
        }
    }
    // Getters
    public String getCurrentFilter() { return currentFilter; }
    public SortType getCurrentSortType() { return currentSortType; }
    public List<Task> getFilteredOverdueTasks() { return filteredOverdueTasks; }
    public List<Task> getFilteredTodayTasks() { return filteredTodayTasks; }
    public List<Task> getFilteredFutureTasks() { return filteredFutureTasks; }
    public List<Task> getFilteredCompletedTodayTasks() { return filteredCompletedTodayTasks; }
}
