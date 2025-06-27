package com.example.todolist;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;
import com.example.todolist.model.Category;
import com.example.todolist.util.TaskActionsDialog;
import com.example.todolist.util.TaskSortDialog;
import com.example.todolist.util.SortType;
import com.example.todolist.util.DateTimePickerDialog;
import com.example.todolist.util.AddTaskHandler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    // Section layouts for show/hide functionality
    private LinearLayout sectionOverdueTasks;
    private LinearLayout sectionTodayTasks;
    private LinearLayout sectionFutureTasks;
    private LinearLayout sectionCompletedTodayTasks;

    // RecyclerViews
    private RecyclerView recyclerOverdueTasks;
    private RecyclerView recyclerTodayTasks; 
    private RecyclerView recyclerFutureTasks;
    private RecyclerView recyclerCompletedTodayTasks;
    private TaskAdapter overdueTasksAdapter;
    private TaskAdapter todayTasksAdapter;
    private TaskAdapter futureTasksAdapter;
    private TaskAdapter completedTodayTasksAdapter;
    private FloatingActionButton fabAdd;
    private MaterialButton btnAll, btnWork, btnPersonal, btnFavorite;
    private ImageView btnMenu;
    
    private static final int REQUEST_TASK_DETAIL = 1001;
    private LinearLayout layoutSearch;
    private LinearLayout layoutCategoriesContainer;
    private EditText editSearch;
    private ImageView btnCancelSearch;
    
    // Empty state
    private View layoutEmptyState;
    private TextView tvEmptyTitle;
    
    // Check all completed tasks link
    private TextView textCheckAllCompleted;
    
    // Section headers for collapse/expand
    private LinearLayout headerOverdueTasks;
    private LinearLayout headerTodayTasks;
    private LinearLayout headerFutureTasks;
    private LinearLayout headerCompletedTodayTasks;
    private ImageView iconExpandOverdue;
    private ImageView iconExpandToday;
    private ImageView iconExpandFuture;
    private ImageView iconExpandCompleted;
    
    // Collapse state tracking
    private boolean isOverdueCollapsed = false;
    private boolean isTodayCollapsed = false;
    private boolean isFutureCollapsed = false;
    private boolean isCompletedCollapsed = false;
    
    // Current filter
    private String currentFilter = "all";
    
    // Current sort type
    private SortType currentSortType = SortType.DATE_TIME;
    
    private TodoDatabase database;
    private AddTaskHandler addTaskHandler;
    private List<TodoTask> allTasks;
    private List<TodoTask> overdueTasks;
    private List<TodoTask> todayTasks;
    private List<TodoTask> futureTasks;
    private List<TodoTask> completedTodayTasks;
    private List<TodoTask> filteredOverdueTasks;
    private List<TodoTask> filteredTodayTasks;
    private List<TodoTask> filteredFutureTasks;
    private List<TodoTask> filteredCompletedTodayTasks;
    private List<Category> categories;
    private boolean isSearchMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDatabase();
        initViews();
        setupRecyclerViews();
        setupClickListeners();
        loadCategories();
        loadTasks();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload categories when returning from CategoryManager
        loadCategories();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TASK_DETAIL && resultCode == RESULT_OK) {
            // Reload data when returning from TaskDetailActivity with changes
            loadTasks();
        }
    }
    
    private void initDatabase() {
        database = TodoDatabase.getInstance(this);
        allTasks = new ArrayList<>();
        overdueTasks = new ArrayList<>();
        todayTasks = new ArrayList<>();
        futureTasks = new ArrayList<>();
        completedTodayTasks = new ArrayList<>();
        filteredOverdueTasks = new ArrayList<>();
        filteredTodayTasks = new ArrayList<>();
        filteredFutureTasks = new ArrayList<>();
        filteredCompletedTodayTasks = new ArrayList<>();
        categories = new ArrayList<>();
    }

    private void initViews() {
        sectionOverdueTasks = findViewById(R.id.section_overdue_tasks);
        sectionTodayTasks = findViewById(R.id.section_today_tasks);
        sectionFutureTasks = findViewById(R.id.section_future_tasks);
        sectionCompletedTodayTasks = findViewById(R.id.section_completed_today_tasks);
        
        recyclerOverdueTasks = findViewById(R.id.recycler_overdue_tasks);
        recyclerTodayTasks = findViewById(R.id.recycler_today_tasks);
        recyclerFutureTasks = findViewById(R.id.recycler_future_tasks);
        recyclerCompletedTodayTasks = findViewById(R.id.recycler_completed_today_tasks);
        fabAdd = findViewById(R.id.fab_add);
        btnAll = findViewById(R.id.btn_all);
        btnWork = findViewById(R.id.btn_work);
        btnPersonal = findViewById(R.id.btn_personal);
        btnFavorite = findViewById(R.id.btn_favorite);
        btnMenu = findViewById(R.id.btn_menu);
        
        // Search components
        // layoutFilterTabs = findViewById(R.id.layout_filter_tabs);
        layoutSearch = findViewById(R.id.layout_search);
        layoutCategoriesContainer = findViewById(R.id.layout_categories_container);
        editSearch = findViewById(R.id.edit_search);
        btnCancelSearch = findViewById(R.id.btn_cancel_search);
        
        // Empty state
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        
        // Check all completed tasks link
        textCheckAllCompleted = findViewById(R.id.text_check_all_completed);
        
        // Section headers
        headerOverdueTasks = findViewById(R.id.header_overdue_tasks);
        headerTodayTasks = findViewById(R.id.header_today_tasks);
        headerFutureTasks = findViewById(R.id.header_future_tasks);
        headerCompletedTodayTasks = findViewById(R.id.header_completed_today_tasks);
        iconExpandOverdue = findViewById(R.id.icon_expand_overdue);
        iconExpandToday = findViewById(R.id.icon_expand_today);
        iconExpandFuture = findViewById(R.id.icon_expand_future);
        iconExpandCompleted = findViewById(R.id.icon_expand_completed);
    }

    private void setupRecyclerViews() {
        // Overdue tasks RecyclerView
        recyclerOverdueTasks.setLayoutManager(new LinearLayoutManager(this));
        overdueTasksAdapter = new TaskAdapter(overdueTasks, this);
        recyclerOverdueTasks.setAdapter(overdueTasksAdapter);

        // Today tasks RecyclerView
        recyclerTodayTasks.setLayoutManager(new LinearLayoutManager(this));
        todayTasksAdapter = new TaskAdapter(todayTasks, this);
        recyclerTodayTasks.setAdapter(todayTasksAdapter);

        // Future tasks RecyclerView
        recyclerFutureTasks.setLayoutManager(new LinearLayoutManager(this));
        futureTasksAdapter = new TaskAdapter(futureTasks, this);
        recyclerFutureTasks.setAdapter(futureTasksAdapter);

        // Completed today tasks RecyclerView
        recyclerCompletedTodayTasks.setLayoutManager(new LinearLayoutManager(this));
        completedTodayTasksAdapter = new TaskAdapter(completedTodayTasks, this);
        recyclerCompletedTodayTasks.setAdapter(completedTodayTasksAdapter);
    }

    private void setupClickListeners() {
        // Khởi tạo AddTaskHandler
        addTaskHandler = new AddTaskHandler(this, task -> {
            // Callback khi task được thêm thành công - reload danh sách tasks
            loadTasks();
        });
        
        fabAdd.setOnClickListener(v -> {
            // Sử dụng AddTaskHandler thay vì showAddTaskDialog
            String categoryFilter = currentFilter.equals("all") ? null : currentFilter;
            addTaskHandler.showAddTaskDialog(null, categoryFilter);
        });
        
        btnAll.setOnClickListener(v -> filterTasks("all"));
        btnWork.setOnClickListener(v -> filterTasks("công việc"));
        btnPersonal.setOnClickListener(v -> filterTasks("cá nhân"));
        btnFavorite.setOnClickListener(v -> filterTasks("yêu thích"));
        
        btnMenu.setOnClickListener(v -> showPopupMenu(v));
        
        // Bottom navigation
        setupBottomNavigation();
        
        // Search listeners
        btnCancelSearch.setOnClickListener(v -> exitSearchMode());
        
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Section header click listeners for collapse/expand
        headerOverdueTasks.setOnClickListener(v -> toggleSection("overdue"));
        headerTodayTasks.setOnClickListener(v -> toggleSection("today"));
        headerFutureTasks.setOnClickListener(v -> toggleSection("future"));
        headerCompletedTodayTasks.setOnClickListener(v -> toggleSection("completed"));
        
        // Check all completed tasks click listener
        textCheckAllCompleted.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompletedTasksActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupBottomNavigation() {
        LinearLayout btnNavMenu = findViewById(R.id.btn_nav_menu);
        LinearLayout btnNavTasks = findViewById(R.id.btn_nav_tasks);
        LinearLayout btnNavCalendar = findViewById(R.id.btn_nav_calendar);
        LinearLayout btnNavProfile = findViewById(R.id.btn_nav_profile);
        
        if (btnNavCalendar != null) {
            btnNavCalendar.setOnClickListener(v -> {
                Intent intent = new Intent(this, CalendarActivity.class);
                startActivity(intent);
            });
        }
        
        if (btnNavMenu != null) {
            btnNavMenu.setOnClickListener(v -> {
                Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> {
                Toast.makeText(this, "Của tôi", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void loadCategories() {
        new Thread(() -> {
            // Load categories from database
            categories = database.categoryDao().getAllCategories();
            
            // If no categories exist, create default ones
            if (categories.isEmpty()) {
                createDefaultCategories();
                categories = database.categoryDao().getAllCategories();
            }
            
            runOnUiThread(() -> {
                // Clear existing dynamic categories (keep the 4 default buttons: all, work, personal, favorite)
                clearDynamicCategories();
                
                // Add categories from database (skip default ones that are already in layout)
                for (Category category : categories) {
                    if (!isDefaultCategory(category.getName())) {
                        addDynamicCategoryButton(category);
                    }
                }
            });
        }).start();
    }
    
    // Method to clear all data and start fresh (for testing)
    private void clearAllDataAndReset() {
        new Thread(() -> {
            // Clear all categories and tasks
            database.clearAllTables();
            
            // Recreate defaults
            createDefaultCategories();
            
            runOnUiThread(() -> {
                loadCategories();
                loadTasks();
            });
        }).start();
    }
    
    private void createDefaultCategories() {
        // Check if default categories already exist to avoid duplicates
        Category existingWork = database.categoryDao().getCategoryByName("Công việc");
        Category existingPersonal = database.categoryDao().getCategoryByName("Cá nhân");
        Category existingFavorite = database.categoryDao().getCategoryByName("Yêu thích");
        
        if (existingWork == null) {
            Category workCategory = new Category("Công việc", "#FF9800", 1, true);
            database.categoryDao().insertCategory(workCategory);
        }
        
        if (existingPersonal == null) {
            Category personalCategory = new Category("Cá nhân", "#9C27B0", 2, true);
            database.categoryDao().insertCategory(personalCategory);
        }
        
        if (existingFavorite == null) {
            Category favoriteCategory = new Category("Yêu thích", "#E91E63", 3, true);
            database.categoryDao().insertCategory(favoriteCategory);
        }
    }
    
    private boolean isDefaultCategory(String categoryName) {
        return categoryName.equals("Công việc") || 
               categoryName.equals("Cá nhân") ||
               categoryName.equals("Yêu thích");
    }
    
    private void clearDynamicCategories() {
        // Remove all views after the 4th child (all, work, personal, favorite + dynamic ones)
        int childCount = layoutCategoriesContainer.getChildCount();
        if (childCount > 4) {
            layoutCategoriesContainer.removeViews(4, childCount - 4);
        }
    }
    
    private void addDynamicCategoryButton(Category category) {
        MaterialButton categoryButton = new MaterialButton(this);
        categoryButton.setText(category.getName());
        categoryButton.setTextSize(14);
        categoryButton.setMinWidth(getDp(100));
        categoryButton.setPadding(getDp(16), 0, getDp(16), 0);
        
        // Set default style (unselected)
        categoryButton.setTextColor(getColor(R.color.text_gray));
        categoryButton.setBackgroundTintList(getColorStateList(R.color.light_gray));
        
        // Set layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                getDp(40)
        );
        params.setMarginEnd(getDp(12));
        categoryButton.setLayoutParams(params);
        
        // Set click listener
        categoryButton.setOnClickListener(v -> {
            filterTasks(category.getName());
            // Highlight clicked button
            resetFilterButtons();
            categoryButton.setBackgroundTintList(getColorStateList(R.color.primary_blue));
            categoryButton.setTextColor(getColor(android.R.color.white));
        });
        
        // Add to container
        layoutCategoriesContainer.addView(categoryButton);
    }
    
    private int getDp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void loadTasks() {
        new Thread(() -> {
            allTasks = database.todoDao().getAllTasks();
            
            runOnUiThread(() -> {
                updateTaskLists();
                // Apply current filter after loading tasks
                filterTasks(currentFilter);
            });
        }).start();
    }

    private void updateTaskLists() {
        overdueTasks.clear();
        todayTasks.clear();
        futureTasks.clear();
        completedTodayTasks.clear();
        
        // Get current date and time
        Calendar now = Calendar.getInstance();
        String todayDateStr = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(now.getTime());
        
        for (TodoTask task : allTasks) {
            if (task.isCompleted()) {
                // Check if completed today
                if (isTaskCompletedToday(task, todayDateStr)) {
                    completedTodayTasks.add(task);
                }
            } else {
                // Categorize incomplete tasks by time
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
        // For now, we assume all completed tasks are completed today
        // In a real implementation, you might track completion date
        return true;
    }
    
    private int getTaskTimeCategory(TodoTask task, Calendar now, String todayDateStr) {
        try {
            String taskDateStr = task.getDueDate();
            String taskTimeStr = task.getDueTime();
            
            // Parse task date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date taskDate = dateFormat.parse(taskDateStr);
            Date todayDate = dateFormat.parse(todayDateStr);
            
            if (taskDate.before(todayDate)) {
                // Task is from a previous day - overdue
                return 0;
            } else if (taskDate.equals(todayDate)) {
                // Task is today - regardless of time, put in "Today" section
                // Time color will be handled in adapter (red if overdue)
                return 1;
            } else {
                // Task is in the future
                return 2;
            }
        } catch (Exception e) {
            // If date parsing fails, consider as today
            return 1;
        }
    }
    
    private void enterSearchMode() {
        isSearchMode = true;
        // layoutFilterTabs.setVisibility(View.GONE);
        layoutSearch.setVisibility(View.VISIBLE);
        editSearch.requestFocus();
        
        // Initialize filtered lists with all tasks
        filteredOverdueTasks.clear();
        filteredTodayTasks.clear();
        filteredFutureTasks.clear();
        filteredCompletedTodayTasks.clear();
        filteredOverdueTasks.addAll(overdueTasks);
        filteredTodayTasks.addAll(todayTasks);
        filteredFutureTasks.addAll(futureTasks);
        filteredCompletedTodayTasks.addAll(completedTodayTasks);
    }
    
    private void exitSearchMode() {
        isSearchMode = false;
        // layoutFilterTabs.setVisibility(View.VISIBLE);
        layoutSearch.setVisibility(View.GONE);
        editSearch.setText("");
        
        // Restore original lists
        overdueTasksAdapter.updateTasks(overdueTasks);
        todayTasksAdapter.updateTasks(todayTasks);
        futureTasksAdapter.updateTasks(futureTasks);
        completedTodayTasksAdapter.updateTasks(completedTodayTasks);
        
        // Update section visibility based on original lists
        filteredOverdueTasks.clear();
        filteredTodayTasks.clear();
        filteredFutureTasks.clear();
        filteredCompletedTodayTasks.clear();
        filteredOverdueTasks.addAll(overdueTasks);
        filteredTodayTasks.addAll(todayTasks);
        filteredFutureTasks.addAll(futureTasks);
        filteredCompletedTodayTasks.addAll(completedTodayTasks);
        updateSectionVisibility();
    }
    
    private void performSearch(String query) {
        if (!isSearchMode) return;
        
        filteredOverdueTasks.clear();
        filteredTodayTasks.clear();
        filteredFutureTasks.clear();
        filteredCompletedTodayTasks.clear();
        
        if (query.trim().isEmpty()) {
            // Show all tasks if search is empty
            filteredOverdueTasks.addAll(overdueTasks);
            filteredTodayTasks.addAll(todayTasks);
            filteredFutureTasks.addAll(futureTasks);
            filteredCompletedTodayTasks.addAll(completedTodayTasks);
        } else {
            // Filter tasks based on query
            String lowerQuery = query.toLowerCase(Locale.getDefault()).trim();
            
            // Filter overdue tasks
            for (TodoTask task : overdueTasks) {
                if (task.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase(Locale.getDefault()).contains(lowerQuery))) {
                    filteredOverdueTasks.add(task);
                }
            }
            
            // Filter today tasks
            for (TodoTask task : todayTasks) {
                if (task.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase(Locale.getDefault()).contains(lowerQuery))) {
                    filteredTodayTasks.add(task);
                }
            }
            
            // Filter future tasks
            for (TodoTask task : futureTasks) {
                if (task.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase(Locale.getDefault()).contains(lowerQuery))) {
                    filteredFutureTasks.add(task);
                }
            }
            
            // Filter completed today tasks
            for (TodoTask task : completedTodayTasks) {
                if (task.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase(Locale.getDefault()).contains(lowerQuery))) {
                    filteredCompletedTodayTasks.add(task);
                }
            }
        }
        
        // Update adapters with filtered results
        overdueTasksAdapter.updateTasks(filteredOverdueTasks);
        todayTasksAdapter.updateTasks(filteredTodayTasks);
        futureTasksAdapter.updateTasks(filteredFutureTasks);
        completedTodayTasksAdapter.updateTasks(filteredCompletedTodayTasks);
        
        // Update section visibility
        updateSectionVisibility();
    }

    private void filterTasks(String filter) {
        currentFilter = filter;
        
        // Update button states
        resetFilterButtons();
        switch (filter.toLowerCase(Locale.getDefault())) {
            case "all":
                btnAll.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnAll.setTextColor(getColor(android.R.color.white));
                break;
            case "công việc":
                btnWork.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnWork.setTextColor(getColor(android.R.color.white));
                break;
            case "cá nhân":
                btnPersonal.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnPersonal.setTextColor(getColor(android.R.color.white));
                break;
            case "yêu thích":
                btnFavorite.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnFavorite.setTextColor(getColor(android.R.color.white));
                break;
            default:
                // For dynamic categories, find and highlight the button
                highlightDynamicCategoryButton(filter);
                break;
        }
        
        // Filter tasks by category
        filteredOverdueTasks.clear();
        filteredTodayTasks.clear();
        filteredFutureTasks.clear();
        filteredCompletedTodayTasks.clear();
        
        if (filter.equalsIgnoreCase("all")) {
            // Show all tasks
            filteredOverdueTasks.addAll(overdueTasks);
            filteredTodayTasks.addAll(todayTasks);
            filteredFutureTasks.addAll(futureTasks);
            filteredCompletedTodayTasks.addAll(completedTodayTasks);
        } else {
            // Filter by specific category
            for (TodoTask task : overdueTasks) {
                if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(filter)) {
                    filteredOverdueTasks.add(task);
                }
            }
            
            for (TodoTask task : todayTasks) {
                if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(filter)) {
                    filteredTodayTasks.add(task);
                }
            }
            
            for (TodoTask task : futureTasks) {
                if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(filter)) {
                    filteredFutureTasks.add(task);
                }
            }
            
            for (TodoTask task : completedTodayTasks) {
                if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(filter)) {
                    filteredCompletedTodayTasks.add(task);
                }
            }
        }
        
        // Update adapters
        overdueTasksAdapter.updateTasks(filteredOverdueTasks);
        todayTasksAdapter.updateTasks(filteredTodayTasks);
        futureTasksAdapter.updateTasks(filteredFutureTasks);
        completedTodayTasksAdapter.updateTasks(filteredCompletedTodayTasks);
        
        // Apply current sorting
        sortTasks();
        
        // Update section visibility
        updateSectionVisibility();
        
        // Show/hide empty state
        boolean hasAnyTasks = !filteredOverdueTasks.isEmpty() || !filteredTodayTasks.isEmpty() || 
                              !filteredFutureTasks.isEmpty() || !filteredCompletedTodayTasks.isEmpty();
        if (hasAnyTasks) {
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (filter.equalsIgnoreCase("all")) {
                tvEmptyTitle.setText("Không có nhiệm vụ nào.");
            } else {
                tvEmptyTitle.setText("Không có nhiệm vụ nào trong danh mục \"" + filter + "\".");
            }
        }
    }
    
    private void highlightDynamicCategoryButton(String categoryName) {
        // Find and highlight the dynamic category button
        for (int i = 4; i < layoutCategoriesContainer.getChildCount(); i++) {
            View child = layoutCategoriesContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                if (button.getText().toString().equalsIgnoreCase(categoryName)) {
                    button.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                    button.setTextColor(getColor(android.R.color.white));
                    break;
                }
            }
        }
    }

    private void resetFilterButtons() {
        int grayColor = getColor(R.color.light_gray);
        int textColor = getColor(R.color.text_gray);
        
        btnAll.setBackgroundTintList(getColorStateList(R.color.light_gray));
        btnAll.setTextColor(textColor);
        btnWork.setBackgroundTintList(getColorStateList(R.color.light_gray));
        btnWork.setTextColor(textColor);
        btnPersonal.setBackgroundTintList(getColorStateList(R.color.light_gray));
        btnPersonal.setTextColor(textColor);
        btnFavorite.setBackgroundTintList(getColorStateList(R.color.light_gray));
        btnFavorite.setTextColor(textColor);
        
        // Reset dynamic category buttons
        for (int i = 4; i < layoutCategoriesContainer.getChildCount(); i++) {
            View child = layoutCategoriesContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                button.setBackgroundTintList(getColorStateList(R.color.light_gray));
                button.setTextColor(textColor);
            }
        }
    }

    // TaskAdapter.OnTaskClickListener implementation
    @Override
    public void onTaskClick(TodoTask task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivityForResult(intent, REQUEST_TASK_DETAIL);
    }

    @Override
    public void onTaskComplete(TodoTask task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        new Thread(() -> {
            database.todoDao().updateTask(task);
            runOnUiThread(this::loadTasks);
        }).start();
    }

    @Override
    public void onTaskLongClick(TodoTask task) {
        // Show actions dialog on long click
        TaskActionsDialog actionsDialog = new TaskActionsDialog(this, task, new TaskActionsDialog.OnActionSelectedListener() {
            @Override
            public void onStarAction(TodoTask task) {
                onTaskStar(task);
            }

            @Override
            public void onDeleteAction(TodoTask task) {
                onTaskDelete(task);
            }
        });
        actionsDialog.show();
    }

    @Override
    public void onTaskStar(TodoTask task) {
        task.setImportant(!task.isImportant());
        new Thread(() -> {
            database.todoDao().updateTask(task);
            runOnUiThread(this::loadTasks);
        }).start();
        Toast.makeText(this, task.isImportant() ? "Đã đánh dấu quan trọng" : "Đã bỏ đánh dấu quan trọng", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskDelete(TodoTask task) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa nhiệm vụ")
                .setMessage("Bạn có chắc chắn muốn xóa nhiệm vụ này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    new Thread(() -> {
                        database.todoDao().deleteTask(task);
                        runOnUiThread(this::loadTasks);
                    }).start();
                    Toast.makeText(this, "Đã xóa nhiệm vụ", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_category_manager) {
                startActivity(new Intent(this, CategoryManagerActivity.class));
                return true;
            } else if (item.getItemId() == R.id.menu_search) {
                enterSearchMode();
                return true;
            } else if (item.getItemId() == R.id.menu_sort_tasks) {
                showSortDialog();
                return true;
            } else if (item.getItemId() == R.id.menu_reset_data) {
                // Reset all data for testing
                new AlertDialog.Builder(this)
                    .setTitle("Reset dữ liệu")
                    .setMessage("Bạn có chắc chắn muốn xóa tất cả dữ liệu và tạo lại từ đầu?")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        clearAllDataAndReset();
                        Toast.makeText(this, "Đã reset tất cả dữ liệu", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void showSortDialog() {
        TaskSortDialog sortDialog = new TaskSortDialog(this, sortType -> {
            currentSortType = sortType;
            // Apply sorting to current tasks
            sortTasks();
            Toast.makeText(this, "Đã áp dụng sắp xếp: " + getSortTypeName(sortType), Toast.LENGTH_SHORT).show();
        });
        sortDialog.show();
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
    
    private void sortTasks() {
        // Sort overdue tasks
        sortTaskList(filteredOverdueTasks, currentSortType);
        sortTaskList(overdueTasks, currentSortType);
        
        // Sort today tasks
        sortTaskList(filteredTodayTasks, currentSortType);
        sortTaskList(todayTasks, currentSortType);
        
        // Sort future tasks
        sortTaskList(filteredFutureTasks, currentSortType);
        sortTaskList(futureTasks, currentSortType);
        
        // Sort completed today tasks
        sortTaskList(filteredCompletedTodayTasks, currentSortType);
        sortTaskList(completedTodayTasks, currentSortType);
        
        // Update adapters
        overdueTasksAdapter.updateTasks(filteredOverdueTasks);
        todayTasksAdapter.updateTasks(filteredTodayTasks);
        futureTasksAdapter.updateTasks(filteredFutureTasks);
        completedTodayTasksAdapter.updateTasks(filteredCompletedTodayTasks);
    }
    
    private void sortTaskList(List<TodoTask> tasks, SortType sortType) {
        switch (sortType) {
            case DATE_TIME:
                Collections.sort(tasks, new Comparator<TodoTask>() {
                    @Override
                    public int compare(TodoTask t1, TodoTask t2) {
                        // Sort by date, then by time
                        String dateTime1 = t1.getDueDate() + " " + t1.getDueTime();
                        String dateTime2 = t2.getDueDate() + " " + t2.getDueTime();
                        return dateTime1.compareTo(dateTime2);
                    }
                });
                break;
                
            case CREATION_TIME:
                Collections.sort(tasks, new Comparator<TodoTask>() {
                    @Override
                    public int compare(TodoTask t1, TodoTask t2) {
                        // Sort by task ID (newer tasks have higher IDs)
                        return Integer.compare(t1.getId(), t2.getId());
                    }
                });
                break;
                
            case ALPHABETICAL:
                Collections.sort(tasks, new Comparator<TodoTask>() {
                    @Override
                    public int compare(TodoTask t1, TodoTask t2) {
                        // Sort alphabetically by title
                        return t1.getTitle().compareToIgnoreCase(t2.getTitle());
                    }
                });
                break;
        }
    }
    
    private void showDateTimePickerDialog(DateTimePickerDialog.OnDateTimeSelectedListener listener) {
        DateTimePickerDialog dateTimeDialog = new DateTimePickerDialog(this, listener);
        dateTimeDialog.show();
    }

    private void setupCategorySpinner(Spinner spinner) {
        new Thread(() -> {
            try {
                List<Category> categories = database.categoryDao().getAllCategories();
                
                // Add default categories if empty
                if (categories.isEmpty()) {
                    createDefaultCategories();
                    categories = database.categoryDao().getAllCategories();
                }
                
                final List<Category> finalCategories = categories;
                runOnUiThread(() -> {
                    try {
                        // Create string list for spinner with "No category" option
                        List<String> categoryNames = new ArrayList<>();
                        categoryNames.add("Không có thể loại"); // First option
                        for (Category category : finalCategories) {
                            categoryNames.add(category.getName());
                        }
                        
                        // Use simple ArrayAdapter with built-in layout
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this, 
                            android.R.layout.simple_spinner_item, 
                            categoryNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                        
                        // Set default selection to "No category"
                        spinner.setSelection(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Fallback: create simple adapter with default categories
                        List<String> defaultCategories = new ArrayList<>();
                        defaultCategories.add("Không có thể loại");
                        defaultCategories.add("Công việc");
                        defaultCategories.add("Cá nhân");
                        defaultCategories.add("Yêu thích");
                        
                        ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(
                            MainActivity.this, 
                            android.R.layout.simple_spinner_item, 
                            defaultCategories
                        );
                        fallbackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(fallbackAdapter);
                        spinner.setSelection(0);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Final fallback on UI thread
                runOnUiThread(() -> {
                    List<String> defaultCategories = new ArrayList<>();
                    defaultCategories.add("Không có thể loại");
                    defaultCategories.add("Công việc");
                    defaultCategories.add("Cá nhân");
                    defaultCategories.add("Yêu thích");
                    
                    ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(
                        MainActivity.this, 
                        android.R.layout.simple_spinner_item, 
                        defaultCategories
                    );
                    fallbackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(fallbackAdapter);
                    spinner.setSelection(0);
                });
            }
        }).start();
    }
    
    private void toggleSection(String sectionType) {
        switch (sectionType) {
            case "overdue":
                isOverdueCollapsed = !isOverdueCollapsed;
                recyclerOverdueTasks.setVisibility(isOverdueCollapsed ? View.GONE : View.VISIBLE);
                iconExpandOverdue.setImageResource(isOverdueCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
                break;
            case "today":
                isTodayCollapsed = !isTodayCollapsed;
                recyclerTodayTasks.setVisibility(isTodayCollapsed ? View.GONE : View.VISIBLE);
                iconExpandToday.setImageResource(isTodayCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
                break;
            case "future":
                isFutureCollapsed = !isFutureCollapsed;
                recyclerFutureTasks.setVisibility(isFutureCollapsed ? View.GONE : View.VISIBLE);
                iconExpandFuture.setImageResource(isFutureCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
                break;
            case "completed":
                isCompletedCollapsed = !isCompletedCollapsed;
                recyclerCompletedTodayTasks.setVisibility(isCompletedCollapsed ? View.GONE : View.VISIBLE);
                iconExpandCompleted.setImageResource(isCompletedCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
                break;
        }
    }
    
    private void updateSectionVisibility() {
        // Show/hide overdue section
        if (filteredOverdueTasks.isEmpty()) {
            sectionOverdueTasks.setVisibility(View.GONE);
        } else {
            sectionOverdueTasks.setVisibility(View.VISIBLE);
        }
        
        // Show/hide today section
        if (filteredTodayTasks.isEmpty()) {
            sectionTodayTasks.setVisibility(View.GONE);
        } else {
            sectionTodayTasks.setVisibility(View.VISIBLE);
        }
        
        // Show/hide future section
        if (filteredFutureTasks.isEmpty()) {
            sectionFutureTasks.setVisibility(View.GONE);
        } else {
            sectionFutureTasks.setVisibility(View.VISIBLE);
        }
        
        // Show/hide completed today section
        if (filteredCompletedTodayTasks.isEmpty()) {
            sectionCompletedTodayTasks.setVisibility(View.GONE);
        } else {
            sectionCompletedTodayTasks.setVisibility(View.VISIBLE);
        }
    }
}