package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.manager.CategoryManager;
import com.example.todolist.manager.FilterManager;
import com.example.todolist.manager.SearchManager;
import com.example.todolist.manager.SectionManager;
import com.example.todolist.manager.TaskManager;
import com.example.todolist.manager.UIManager;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.AddTaskHandler;
import com.example.todolist.util.SortType;
import com.example.todolist.util.TaskActionsDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity implements 
    TaskAdapter.OnTaskClickListener,
    TaskManager.TaskUpdateListener,
    CategoryManager.CategoryUpdateListener,
    SearchManager.SearchListener,
    FilterManager.FilterListener,
    UIManager.UIListener {

    // Managers
    private TaskManager taskManager;
    private CategoryManager categoryManager;
    private SearchManager searchManager;
    private FilterManager filterManager;
    private SectionManager sectionManager;
    private UIManager uiManager;
    
    // Core UI Components
    private FloatingActionButton fabAdd;
    private MaterialButton btnAll, btnWork, btnPersonal, btnFavorite;
    private ImageView btnMenu;
    private LinearLayout layoutSearch;
    private LinearLayout layoutCategoriesContainer;
    private EditText editSearch;
    private ImageView btnCancelSearch;
    private TextView tvEmptyTitle;
    
    // Other components
    private TodoDatabase database;
    private AddTaskHandler addTaskHandler;
    
    private static final int REQUEST_TASK_DETAIL = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDatabase();
        initViews();
        initManagers();
        setupAddTaskHandler();
        loadData();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        categoryManager.loadCategories();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TASK_DETAIL && resultCode == RESULT_OK) {
            taskManager.loadTasks();
        }
    }
    
    private void initDatabase() {
        database = TodoDatabase.getInstance(this);
    }

    private void initViews() {
        // Core UI components
        fabAdd = findViewById(R.id.fab_add);
        btnAll = findViewById(R.id.btn_all);
        btnWork = findViewById(R.id.btn_work);
        btnPersonal = findViewById(R.id.btn_personal);
        btnFavorite = findViewById(R.id.btn_favorite);
        btnMenu = findViewById(R.id.btn_menu);
        
        // Search components
        layoutSearch = findViewById(R.id.layout_search);
        layoutCategoriesContainer = findViewById(R.id.layout_categories_container);
        editSearch = findViewById(R.id.edit_search);
        btnCancelSearch = findViewById(R.id.btn_cancel_search);
        
        // Empty state
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
    }
    
    private void initManagers() {
        // Initialize TaskManager
        taskManager = new TaskManager(this, this);
        
        // Initialize CategoryManager
        categoryManager = new CategoryManager(this, layoutCategoriesContainer, this);
        
        // Initialize SearchManager
        searchManager = new SearchManager(layoutSearch, editSearch, btnCancelSearch, this);
        
        // Initialize FilterManager
        filterManager = new FilterManager(this, btnAll, btnWork, btnPersonal, btnFavorite,
                layoutCategoriesContainer, findViewById(R.id.layout_empty_state), tvEmptyTitle, this);
        
        // Initialize SectionManager
        sectionManager = new SectionManager(
                findViewById(R.id.section_overdue_tasks),
                findViewById(R.id.section_today_tasks),
                findViewById(R.id.section_future_tasks),
                findViewById(R.id.section_completed_today_tasks),
                findViewById(R.id.recycler_overdue_tasks),
                findViewById(R.id.recycler_today_tasks),
                findViewById(R.id.recycler_future_tasks),
                findViewById(R.id.recycler_completed_today_tasks),
                findViewById(R.id.header_overdue_tasks),
                findViewById(R.id.header_today_tasks),
                findViewById(R.id.header_future_tasks),
                findViewById(R.id.header_completed_today_tasks),
                findViewById(R.id.icon_expand_overdue),
                findViewById(R.id.icon_expand_today),
                findViewById(R.id.icon_expand_future),
                findViewById(R.id.icon_expand_completed)
        );
        
        // Initialize UIManager
        uiManager = new UIManager(this, fabAdd, btnMenu, findViewById(R.id.text_check_all_completed), this);
        
        // Setup RecyclerViews and get adapters
        TaskAdapter[] adapters = uiManager.setupRecyclerViews(
                findViewById(R.id.recycler_overdue_tasks),
                findViewById(R.id.recycler_today_tasks),
                findViewById(R.id.recycler_future_tasks),
                findViewById(R.id.recycler_completed_today_tasks),
                taskManager.getOverdueTasks(),
                taskManager.getTodayTasks(),
                taskManager.getFutureTasks(),
                taskManager.getCompletedTodayTasks(),
                this
        );
        
        // Set adapters to FilterManager
        if (adapters != null && adapters.length == 4) {
            filterManager.setAdapters(adapters[0], adapters[1], adapters[2], adapters[3]);
        }
        
        // Setup bottom navigation
        uiManager.setupBottomNavigation();
    }
    
    private void setupAddTaskHandler() {
        addTaskHandler = new AddTaskHandler(this, task -> taskManager.loadTasks());
        
        fabAdd.setOnClickListener(v -> {
            String categoryFilter = filterManager.getCurrentFilter().equals("all") ? null : filterManager.getCurrentFilter();
            addTaskHandler.showAddTaskDialog(null, categoryFilter);
        });
    }
    
    private void loadData() {
        categoryManager.loadCategories();
        taskManager.loadTasks();
    }

    
    // ==================== INTERFACE IMPLEMENTATIONS ====================
    
    // TaskAdapter.OnTaskClickListener implementation
    @Override
    public void onTaskClick(TodoTask task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivityForResult(intent, REQUEST_TASK_DETAIL);
    }

    @Override
    public void onTaskComplete(TodoTask task, boolean isCompleted) {
        taskManager.completeTask(task, isCompleted);
    }

    @Override
    public void onTaskLongClick(TodoTask task) {
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
        taskManager.toggleTaskImportant(task);
    }

    @Override
    public void onTaskDelete(TodoTask task) {
        taskManager.deleteTask(task);
    }
    
    // TaskManager.TaskUpdateListener implementation
    @Override
    public void onTasksUpdated() {
        runOnUiThread(() -> {
            // Update managers with new task data
            searchManager.setTaskLists(taskManager.getOverdueTasks(), taskManager.getTodayTasks(),
                    taskManager.getFutureTasks(), taskManager.getCompletedTodayTasks());
            
            filterManager.setTaskLists(taskManager.getOverdueTasks(), taskManager.getTodayTasks(),
                    taskManager.getFutureTasks(), taskManager.getCompletedTodayTasks());
            
            // Apply current filter
            filterManager.filterTasks(filterManager.getCurrentFilter());
            
            // Update section visibility
            sectionManager.updateSectionVisibility(
                    filterManager.getFilteredOverdueTasks(),
                    filterManager.getFilteredTodayTasks(),
                    filterManager.getFilteredFutureTasks(),
                    filterManager.getFilteredCompletedTodayTasks()
            );
        });
    }
    
    // CategoryManager.CategoryUpdateListener implementation  
    @Override
    public void onCategoriesUpdated() {
        runOnUiThread(() -> {
            categoryManager.updateDynamicCategoryButtons();
        });
    }
    
    // SearchManager.SearchListener implementation
    @Override
    public void onSearchModeChanged(boolean isSearchMode) {
        // Handle search mode changes if needed
    }
    
    @Override
    public void onSearchResults(List<TodoTask> overdueResults, List<TodoTask> todayResults,
                               List<TodoTask> futureResults, List<TodoTask> completedResults) {
        // Update section visibility with search results
        sectionManager.updateSectionVisibility(overdueResults, todayResults, futureResults, completedResults);
    }
    
    // FilterManager.FilterListener implementation
    @Override
    public void onFilterChanged(String filter) {
        // Update section visibility when filter changes
        sectionManager.updateSectionVisibility(
                filterManager.getFilteredOverdueTasks(),
                filterManager.getFilteredTodayTasks(),
                filterManager.getFilteredFutureTasks(),
                filterManager.getFilteredCompletedTodayTasks()
        );
    }
    
    @Override
    public void onEmptyStateChanged(boolean isEmpty, String message) {
        // Empty state is handled by FilterManager
    }
    
    // UIManager.UIListener implementation
    @Override
    public void onMenuItemSelected(int itemId) {
        if (itemId == R.id.menu_search) {
            searchManager.enterSearchMode();
        } else if (itemId == R.id.menu_reset_data) {
            categoryManager.clearAllDataAndReset();
        }
    }
    
    @Override
    public void onSortTypeSelected(SortType sortType) {
        filterManager.setSortType(sortType);
    }
    
    @Override
    public void onBottomNavigation(String section) {
        Toast.makeText(this, section, Toast.LENGTH_SHORT).show();
    }
}