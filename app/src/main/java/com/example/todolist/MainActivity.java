package com.example.todolist;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;
import com.example.todolist.model.Category;
import com.example.todolist.util.TaskActionsDialog;
import com.example.todolist.util.TaskSortDialog;
import com.example.todolist.util.SortType;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private RecyclerView recyclerIncompleteTasks;
    private RecyclerView recyclerCompletedTasks;
    private TaskAdapter incompleteTasksAdapter;
    private TaskAdapter completedTasksAdapter;
    private FloatingActionButton fabAdd;
    private MaterialButton btnAll, btnWork, btnPersonal, btnFavorite;
    private ImageView btnMenu;
    
    // Search components
    private LinearLayout layoutFilterTabs;
    private LinearLayout layoutSearch;
    private LinearLayout layoutCategoriesContainer;
    private EditText editSearch;
    private ImageView btnCancelSearch;
    
    // Empty state
    private View layoutEmptyState;
    private TextView tvEmptyTitle;
    
    // Current filter
    private String currentFilter = "all";
    
    // Current sort type
    private SortType currentSortType = SortType.DATE_TIME;
    
    private TodoDatabase database;
    private List<TodoTask> allTasks;
    private List<TodoTask> incompleteTasks;
    private List<TodoTask> completedTasks;
    private List<TodoTask> filteredIncompleteTasks;
    private List<TodoTask> filteredCompletedTasks;
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
    
    private void initDatabase() {
        database = TodoDatabase.getInstance(this);
        allTasks = new ArrayList<>();
        incompleteTasks = new ArrayList<>();
        completedTasks = new ArrayList<>();
        filteredIncompleteTasks = new ArrayList<>();
        filteredCompletedTasks = new ArrayList<>();
        categories = new ArrayList<>();
    }

    private void initViews() {
        recyclerIncompleteTasks = findViewById(R.id.recycler_incomplete_tasks);
        recyclerCompletedTasks = findViewById(R.id.recycler_completed_tasks);
        fabAdd = findViewById(R.id.fab_add);
        btnAll = findViewById(R.id.btn_all);
        btnWork = findViewById(R.id.btn_work);
        btnPersonal = findViewById(R.id.btn_personal);
        btnFavorite = findViewById(R.id.btn_favorite);
        btnMenu = findViewById(R.id.btn_menu);
        
        // Search components
        layoutFilterTabs = findViewById(R.id.layout_filter_tabs);
        layoutSearch = findViewById(R.id.layout_search);
        layoutCategoriesContainer = findViewById(R.id.layout_categories_container);
        editSearch = findViewById(R.id.edit_search);
        btnCancelSearch = findViewById(R.id.btn_cancel_search);
        
        // Empty state
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
    }

    private void setupRecyclerViews() {
        // Incomplete tasks RecyclerView
        recyclerIncompleteTasks.setLayoutManager(new LinearLayoutManager(this));
        incompleteTasksAdapter = new TaskAdapter(incompleteTasks, this);
        recyclerIncompleteTasks.setAdapter(incompleteTasksAdapter);

        // Completed tasks RecyclerView
        recyclerCompletedTasks.setLayoutManager(new LinearLayoutManager(this));
        completedTasksAdapter = new TaskAdapter(completedTasks, this);
        recyclerCompletedTasks.setAdapter(completedTasksAdapter);
    }

    private void setupClickListeners() {
        fabAdd.setOnClickListener(v -> showAddTaskDialog());
        
        btnAll.setOnClickListener(v -> filterTasks("all"));
        btnWork.setOnClickListener(v -> filterTasks("công việc"));
        btnPersonal.setOnClickListener(v -> filterTasks("cá nhân"));
        btnFavorite.setOnClickListener(v -> filterTasks("yêu thích"));
        
        btnMenu.setOnClickListener(v -> showPopupMenu(v));
        
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

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        
        // Make dialog background transparent so CardView corners show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        EditText editTaskTitle = dialogView.findViewById(R.id.edit_task_title);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_save);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String title = editTaskTitle.getText().toString().trim();
            if (!title.isEmpty()) {
                createNewTask(title);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng nhập tiêu đề nhiệm vụ", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void createNewTask(String title) {
        TodoTask newTask = new TodoTask(title, "", "2025/06/25", "12:00");
        
        // Set category based on current filter
        if (!currentFilter.equalsIgnoreCase("all")) {
            newTask.setCategory(currentFilter);
        } else {
            newTask.setCategory("Công việc"); // Default category
        }
        
        // Add to database
        new Thread(() -> {
            database.todoDao().insertTask(newTask);
            runOnUiThread(this::loadTasks);
        }).start();
        
        Toast.makeText(this, "Đã thêm nhiệm vụ mới", Toast.LENGTH_SHORT).show();
    }

    private void loadTasks() {
        new Thread(() -> {
            allTasks = database.todoDao().getAllTasks();
            
            // Add sample data if empty
            if (allTasks.isEmpty()) {
                addSampleData();
                allTasks = database.todoDao().getAllTasks();
            }
            
            runOnUiThread(() -> {
                updateTaskLists();
                // Apply current filter after loading tasks
                filterTasks(currentFilter);
            });
        }).start();
    }
    
    private void addSampleData() {
        // Add sample tasks for different categories
        TodoTask workTask = new TodoTask(
            "Hoàn thành báo cáo tháng", 
            "", 
            "2025/05/25", 
            "22:00"
        );
        workTask.setCategory("Công việc");
        workTask.setHasReminder(true);
        database.todoDao().insertTask(workTask);
        
        TodoTask personalTask = new TodoTask(
            "Mua sắm thực phẩm cho tuần", 
            "", 
            "2025/05/26", 
            "10:00"
        );
        personalTask.setCategory("Cá nhân");
        database.todoDao().insertTask(personalTask);
        
        TodoTask favoriteTask = new TodoTask(
            "Đọc sách yêu thích", 
            "", 
            "2025/05/26", 
            "20:00"
        );
        favoriteTask.setCategory("Yêu thích");
        database.todoDao().insertTask(favoriteTask);
    }

    private void updateTaskLists() {
        incompleteTasks.clear();
        completedTasks.clear();
        
        for (TodoTask task : allTasks) {
            if (task.isCompleted()) {
                completedTasks.add(task);
            } else {
                incompleteTasks.add(task);
            }
        }
    }
    
    private void enterSearchMode() {
        isSearchMode = true;
        layoutFilterTabs.setVisibility(View.GONE);
        layoutSearch.setVisibility(View.VISIBLE);
        editSearch.requestFocus();
        
        // Initialize filtered lists with all tasks
        filteredIncompleteTasks.clear();
        filteredCompletedTasks.clear();
        filteredIncompleteTasks.addAll(incompleteTasks);
        filteredCompletedTasks.addAll(completedTasks);
    }
    
    private void exitSearchMode() {
        isSearchMode = false;
        layoutFilterTabs.setVisibility(View.VISIBLE);
        layoutSearch.setVisibility(View.GONE);
        editSearch.setText("");
        
        // Restore original lists
        incompleteTasksAdapter.updateTasks(incompleteTasks);
        completedTasksAdapter.updateTasks(completedTasks);
    }
    
    private void performSearch(String query) {
        if (!isSearchMode) return;
        
        filteredIncompleteTasks.clear();
        filteredCompletedTasks.clear();
        
        if (query.trim().isEmpty()) {
            // Show all tasks if search is empty
            filteredIncompleteTasks.addAll(incompleteTasks);
            filteredCompletedTasks.addAll(completedTasks);
        } else {
            // Filter tasks based on query
            String lowerQuery = query.toLowerCase(Locale.getDefault()).trim();
            
            for (TodoTask task : incompleteTasks) {
                if (task.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase(Locale.getDefault()).contains(lowerQuery))) {
                    filteredIncompleteTasks.add(task);
                }
            }
            
            for (TodoTask task : completedTasks) {
                if (task.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase(Locale.getDefault()).contains(lowerQuery))) {
                    filteredCompletedTasks.add(task);
                }
            }
        }
        
        // Update adapters with filtered results
        incompleteTasksAdapter.updateTasks(filteredIncompleteTasks);
        completedTasksAdapter.updateTasks(filteredCompletedTasks);
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
        filteredIncompleteTasks.clear();
        filteredCompletedTasks.clear();
        
        if (filter.equalsIgnoreCase("all")) {
            // Show all tasks
            filteredIncompleteTasks.addAll(incompleteTasks);
            filteredCompletedTasks.addAll(completedTasks);
        } else {
            // Filter by specific category
            for (TodoTask task : incompleteTasks) {
                if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(filter)) {
                    filteredIncompleteTasks.add(task);
                }
            }
            
            for (TodoTask task : completedTasks) {
                if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(filter)) {
                    filteredCompletedTasks.add(task);
                }
            }
        }
        
        // Update adapters
        incompleteTasksAdapter.updateTasks(filteredIncompleteTasks);
        completedTasksAdapter.updateTasks(filteredCompletedTasks);
        
        // Apply current sorting
        sortTasks();
        
        // Show/hide empty state
        boolean hasAnyTasks = !filteredIncompleteTasks.isEmpty() || !filteredCompletedTasks.isEmpty();
        if (hasAnyTasks) {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerIncompleteTasks.setVisibility(View.VISIBLE);
            recyclerCompletedTasks.setVisibility(View.VISIBLE);
        } else {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerIncompleteTasks.setVisibility(View.GONE);
            recyclerCompletedTasks.setVisibility(View.GONE);
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
        startActivity(intent);
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
        // Sort incomplete tasks
        sortTaskList(filteredIncompleteTasks, currentSortType);
        sortTaskList(incompleteTasks, currentSortType);
        
        // Sort completed tasks
        sortTaskList(filteredCompletedTasks, currentSortType);
        sortTaskList(completedTasks, currentSortType);
        
        // Update adapters
        incompleteTasksAdapter.updateTasks(filteredIncompleteTasks);
        completedTasksAdapter.updateTasks(filteredCompletedTasks);
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
}