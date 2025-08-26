package com.example.todolist.manager;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.example.todolist.R;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.model.Category;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.CategoryRepository;
import com.example.todolist.util.SortType;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private LinearLayout layoutCategoriesContainer;
    private View layoutEmptyState;
    private TextView tvEmptyTitle;
    
    // Firebase
    private CategoryRepository categoryRepository;
    private List<Category> categories;
    private MaterialButton btnAll;
    private List<MaterialButton> categoryButtons;
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
    
    public FilterManager(Context context, LinearLayout layoutCategoriesContainer, 
                        View layoutEmptyState, TextView tvEmptyTitle, FilterListener listener) {
        this.context = context;
        this.layoutCategoriesContainer = layoutCategoriesContainer;
        this.layoutEmptyState = layoutEmptyState;
        this.tvEmptyTitle = tvEmptyTitle;
        this.listener = listener;
        
        this.categoryRepository = new CategoryRepository();
        this.categories = new ArrayList<>();
        this.categoryButtons = new ArrayList<>();
        
        initializeFilteredLists();
        loadCategoriesFromFirebase();
    }
    private void initializeFilteredLists() {
        filteredOverdueTasks = new ArrayList<>();
        filteredTodayTasks = new ArrayList<>();
        filteredFutureTasks = new ArrayList<>();
        filteredCompletedTodayTasks = new ArrayList<>();
    }
    
    private void loadCategoriesFromFirebase() {
        categoryRepository.getAllCategories(new BaseRepository.ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categoriesList) {
                categories = categoriesList;
                createFilterButtons();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Lỗi tải categories: " + error, Toast.LENGTH_SHORT).show();
                createDefaultButtons();
            }
        });
    }
    
    private void createFilterButtons() {
        if (layoutCategoriesContainer == null) return;
        layoutCategoriesContainer.removeAllViews();
        categoryButtons.clear();
        createAllTasksButton();
        for (Category category : categories) {
            createCategoryButton(category);
        }
    }
    
    private void createDefaultButtons() {
        if (layoutCategoriesContainer == null) return;
        
        layoutCategoriesContainer.removeAllViews();
        categoryButtons.clear();
        createAllTasksButton();
    }
    
    private void createAllTasksButton() {
        btnAll = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnAll.setText(context.getString(R.string.category_all));
        btnAll.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_category));
        btnAll.setIconGravity(MaterialButton.ICON_GRAVITY_START);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                120 
        );
        params.setMargins(0, 0, 20, 0); 
        btnAll.setLayoutParams(params);
        btnAll.setPadding(24, 8, 24, 8);
        
        // Apply default selected style
        btnAll.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary_blue));
        btnAll.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        
        btnAll.setOnClickListener(v -> filterTasks("all"));
        
        layoutCategoriesContainer.addView(btnAll);
        categoryButtons.add(btnAll);
    }
    
    private void createCategoryButton(Category category) {
        MaterialButton categoryBtn = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        categoryBtn.setText(category.getName());
        
        // Set category color if available
        if (category.getColor() != null && !category.getColor().isEmpty()) {
            try {
                int color = Color.parseColor(category.getColor());
                categoryBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
                categoryBtn.setTextColor(color);
                categoryBtn.setIconTint(android.content.res.ColorStateList.valueOf(color));
            } catch (Exception e) {
                // Use default color if parsing fails
                categoryBtn.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
                categoryBtn.setTextColor(ContextCompat.getColor(context, R.color.text_gray));
            }
        } else {
            categoryBtn.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_gray));
            categoryBtn.setTextColor(ContextCompat.getColor(context, R.color.text_gray));
        }
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                120  
        );
        params.setMargins(0, 0, 20, 0); 
        categoryBtn.setLayoutParams(params);
        categoryBtn.setPadding(24, 8, 24, 8);
        
        categoryBtn.setOnClickListener(v -> {
            // Filter by category ID instead of name
            filterTasks(category.getId());
        });
        
        layoutCategoriesContainer.addView(categoryBtn);
        categoryButtons.add(categoryBtn);
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
        resetAllFilterButtons();
        highlightFilterButton(filter);
        
        filteredOverdueTasks.clear();
        filteredTodayTasks.clear();
        filteredFutureTasks.clear();
        filteredCompletedTodayTasks.clear();

        if (filter.equalsIgnoreCase("all")) {
            if (overdueTasks != null) filteredOverdueTasks.addAll(overdueTasks);
            if (todayTasks != null) filteredTodayTasks.addAll(todayTasks);
            if (futureTasks != null) filteredFutureTasks.addAll(futureTasks);
            if (completedTodayTasks != null) filteredCompletedTodayTasks.addAll(completedTodayTasks);
        } else {
            filterByCategory(overdueTasks, filteredOverdueTasks, filter);
            filterByCategory(todayTasks, filteredTodayTasks, filter);
            filterByCategory(futureTasks, filteredFutureTasks, filter);
            filterByCategory(completedTodayTasks, filteredCompletedTodayTasks, filter);
        }

        sortTasks();

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
            String taskCategory = task.getCategory();
            if (taskCategory != null && taskCategory.equalsIgnoreCase(filter)) {
                destination.add(task);
            }
        }
    }
    private void highlightFilterButton(String filter) {
        resetAllFilterButtons();
        
        if (filter.equalsIgnoreCase("all") && btnAll != null) {
            setButtonSelected(btnAll);
        } else {
            // Find category button by ID - need to match with categories list
            for (int i = 0; i < categoryButtons.size(); i++) {
                MaterialButton button = categoryButtons.get(i);
                if (button != btnAll) {
                    int categoryIndex = i - 1; 
                    if (categoryIndex >= 0 && categoryIndex < categories.size()) {
                        Category category = categories.get(categoryIndex);
                        if (category.getId().equals(filter)) {
                            setButtonSelected(button);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    private void resetAllFilterButtons() {
        int grayColor = ContextCompat.getColor(context, R.color.light_gray);
        int textColor = ContextCompat.getColor(context, R.color.text_gray);
        
        for (MaterialButton button : categoryButtons) {
            setButtonUnselected(button, grayColor, textColor);
        }
    }
    private void setButtonSelected(MaterialButton button) {
        button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary_blue));
        button.setTextColor(ContextCompat.getColor(context, android.R.color.white));
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
            message = "Nhiệm vụ trống";
            tvEmptyTitle.setText(message);
        }
        if (listener != null) {
            listener.onEmptyStateChanged(!hasAnyTasks, message);
        }
    }
    
    // Method to refresh categories from Firebase
    public void refreshCategories() {
        loadCategoriesFromFirebase();
    }
    
    // Getters
    public String getCurrentFilter() { return currentFilter; }
    public SortType getCurrentSortType() { return currentSortType; }
    public List<Task> getFilteredOverdueTasks() { return filteredOverdueTasks; }
    public List<Task> getFilteredTodayTasks() { return filteredTodayTasks; }
    public List<Task> getFilteredFutureTasks() { return filteredFutureTasks; }
    public List<Task> getFilteredCompletedTodayTasks() { return filteredCompletedTodayTasks; }
    public List<Category> getCategories() { return new ArrayList<>(categories); }
}
