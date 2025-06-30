package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.dialog.WidgetsDialog;
import com.example.todolist.manager.CategoryManager;
import com.example.todolist.manager.FilterManager;
import com.example.todolist.manager.NavigationDrawerManager;
import com.example.todolist.manager.SearchManager;
import com.example.todolist.manager.SectionManager;
import com.example.todolist.manager.TaskManager;
import com.example.todolist.manager.ThemeManager;
import com.example.todolist.manager.UIManager;
import com.example.todolist.model.TodoTask;
import com.example.todolist.notification.NotificationHelper;
import com.example.todolist.util.AddTaskHandler;
import com.example.todolist.util.NotificationPermissionHelper;
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
    UIManager.UIListener,
    NavigationDrawerManager.NavigationListener,
    ThemeManager.ThemeChangeListener {

    // Managers
    private TaskManager taskManager;
    private CategoryManager categoryManager;
    private SearchManager searchManager;
    private FilterManager filterManager;
    private SectionManager sectionManager;
    private UIManager uiManager;
    private NavigationDrawerManager navigationDrawerManager;
    private ThemeManager themeManager;
    
    // Core UI Components
    private DrawerLayout drawerLayout;
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
        
        // Handle notification click
        handleNotificationIntent();
        
        // Handle drawer open intent
        handleDrawerIntent();
        
        // Handle quick add from widget
        handleQuickAddIntent();
        
        // Request notification permission for Android 13+
        NotificationPermissionHelper.requestNotificationPermission(this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        categoryManager.loadCategories();
        
        // Reschedule all reminders when app resumes
        taskManager.rescheduleAllReminders();
        
        // Apply current theme
        if (themeManager != null) {
            themeManager.applyCurrentTheme();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TASK_DETAIL && resultCode == RESULT_OK) {
            taskManager.loadTasks();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // Xử lý quyền thông báo
        if (NotificationPermissionHelper.isNotificationPermissionGranted(requestCode, grantResults)) {
            Toast.makeText(this, "Đã cho phép thông báo", Toast.LENGTH_SHORT).show();
            // Reschedule all reminders now that we have permission
            taskManager.rescheduleAllReminders();
        } else if (requestCode == NotificationPermissionHelper.NOTIFICATION_PERMISSION_CODE) {
            Toast.makeText(this, "Cần quyền thông báo để nhận lời nhắc", Toast.LENGTH_LONG).show();
        }
        
        // Xử lý quyền gọi điện từ NavigationDrawerManager
        if (navigationDrawerManager != null) {
            navigationDrawerManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    
    private void initDatabase() {
        database = TodoDatabase.getInstance(this);
    }

    private void initViews() {
        // Core UI components
        drawerLayout = findViewById(R.id.drawer_layout);
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
        searchManager = new SearchManager(layoutSearch, findViewById(R.id.layout_filter_tabs), editSearch, btnCancelSearch, this);
        
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
        
        // Initialize NavigationDrawerManager
        navigationDrawerManager = new NavigationDrawerManager(this, drawerLayout, this);
        
        // Initialize ThemeManager
        themeManager = new ThemeManager(this, this);
        
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
    
    /**
     * Xử lý khi nhấn vào thông báo
     */
    private void handleNotificationIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            int taskId = intent.getIntExtra(NotificationHelper.EXTRA_TASK_ID, -1);
            String action = intent.getStringExtra("action");
            
            if (taskId != -1) {
                // Tìm và hiển thị task được click từ notification
                new Thread(() -> {
                    TodoTask task = database.todoDao().getTaskById(taskId);
                    if (task != null) {
                        runOnUiThread(() -> {
                            // Mở TaskDetailActivity để xem chi tiết task
                            Intent detailIntent = new Intent(this, TaskDetailActivity.class);
                            detailIntent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
                            startActivityForResult(detailIntent, REQUEST_TASK_DETAIL);
                            
                            // Hiển thị thông báo tùy theo action
                            if (NotificationHelper.ACTION_REMINDER.equals(action)) {
                                Toast.makeText(this, "Bạn có lời nhắc cho: " + task.getTitle(), Toast.LENGTH_LONG).show();
                            } else if (NotificationHelper.ACTION_DUE.equals(action)) {
                                Toast.makeText(this, "Nhiệm vụ đã đến hạn: " + task.getTitle(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).start();
            }
        }
    }

    /**
     * Xử lý khi cần mở navigation drawer từ activity khác
     */
    private void handleDrawerIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("open_drawer", false)) {
            // Mở drawer sau khi layout đã sẵn sàng
            findViewById(android.R.id.content).post(() -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    /**
     * Xử lý intent quick add từ mini widget
     */
    private void handleQuickAddIntent() {
        Intent intent = getIntent();
        if (intent != null && "com.example.todolist.QUICK_ADD_TASK".equals(intent.getAction())) {
            // Mở dialog thêm task sau khi layout đã sẵn sàng
            findViewById(android.R.id.content).post(() -> {
                if (addTaskHandler != null) {
                    addTaskHandler.showAddTaskDialog();
                }
            });
        }
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
            // Only setup click listeners for dynamic category buttons after they are created
            // Don't call updateDynamicCategoryButtons again to prevent duplication
            if (filterManager != null) {
                // Use a delay to ensure buttons are fully created
                new android.os.Handler().postDelayed(() -> {
                    filterManager.setupAllCategoryClicks();
                }, 100); // Reduced delay
            }
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
        if ("menu_drawer".equals(section)) {
            navigationDrawerManager.openDrawer();
        } else {
            Toast.makeText(this, section, Toast.LENGTH_SHORT).show();
        }
    }
    
    // NavigationDrawerManager.NavigationListener implementation
    @Override
    public void onThemeSelected() {
        // Handle theme selection - đã được xử lý trong NavigationDrawerManager
    }
    
    @Override
    public void onUtilitiesSelected() {
        // Handle utilities selection - show widgets dialog
        WidgetsDialog widgetsDialog = new WidgetsDialog(this);
        widgetsDialog.show();
        // Close drawer after opening dialog
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
    
    @Override
    public void onContactSelected() {
        // Handle contact selection
    }
    
    @Override
    public void onSettingsSelected() {
        // Handle settings selection
    }
    
    // ThemeManager.ThemeChangeListener implementation
    @Override
    public void onThemeChanged(ThemeManager.ThemeColor themeColor) {
        // Theme has been changed, apply immediately
        if (themeManager != null) {
            themeManager.applyCurrentTheme();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (navigationDrawerManager.isDrawerOpen()) {
            navigationDrawerManager.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
}