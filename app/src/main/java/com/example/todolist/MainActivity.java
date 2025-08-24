package com.example.todolist;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
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
import com.example.todolist.dialog.WidgetsDialog;
import com.example.todolist.service.CategoryService;
import com.example.todolist.manager.FilterManager;
import com.example.todolist.manager.NavigationDrawerManager;
import com.example.todolist.manager.SearchManager;
import com.example.todolist.manager.SectionManager;
import com.example.todolist.manager.ThemeManager;
import com.example.todolist.manager.UIManager;
import com.example.todolist.model.Task;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
import com.example.todolist.notification.NotificationHelper;
import com.example.todolist.util.AddTaskHandler;
import com.example.todolist.util.NotificationPermissionHelper;
import com.example.todolist.util.SortType;
import com.example.todolist.util.TaskActionsDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements 
    TaskAdapter.OnTaskClickListener,
    TaskService.TaskUpdateListener,
    CategoryService.CategoryUpdateListener,
    SearchManager.SearchListener,
    FilterManager.FilterListener,
    UIManager.UIListener,
    NavigationDrawerManager.NavigationListener,
    ThemeManager.ThemeChangeListener {
    // Managers
    private TaskService taskService;
    private CategoryService categoryService;
    private SearchManager searchManager;
    private FilterManager filterManager;
    private SectionManager sectionManager;
    private UIManager uiManager;
    private NavigationDrawerManager navigationDrawerManager;
    private ThemeManager themeManager;
    // Core UI Components
    private DrawerLayout drawerLayout;
    private FloatingActionButton fabAdd;
    private ImageView btnMenu;
    private LinearLayout layoutSearch;
    private LinearLayout layoutCategoriesContainer;
    private EditText editSearch;
    private ImageView btnCancelSearch;
    private TextView tvEmptyTitle;
    // Other components
    private AddTaskHandler addTaskHandler;
    private static final int REQUEST_TASK_DETAIL = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply saved language before setting content view
        applyLanguageFromSettings();
        setContentView(R.layout.activity_main);
        initViews();
        initManagers();
        setupAddTaskHandler();
        loadData();
        // Setup first time install tracking
        setupFirstTimeInstall();
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
        taskService.rescheduleAllReminders();
        // Apply current theme
        if (themeManager != null) {
            themeManager.applyCurrentTheme();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TASK_DETAIL && resultCode == RESULT_OK) {
            taskService.loadTasks();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Xá»­ lĂ½ quyá»n thĂ´ng bĂ¡o
        if (NotificationPermissionHelper.isNotificationPermissionGranted(requestCode, grantResults)) {
            // Notification permission granted - no need for toast
            // Reschedule all reminders now that we have permission
            taskService.rescheduleAllReminders();
        } else if (requestCode == NotificationPermissionHelper.NOTIFICATION_PERMISSION_CODE) {
            // Only show toast for important permission denials
            Toast.makeText(this, "Cần quyền thông báo để nhận lời nhắc", Toast.LENGTH_LONG).show();
        }
        // Xử lý quyền gọi điện từ NavigationDrawerManager
        if (navigationDrawerManager != null) {
            navigationDrawerManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private void initViews() {
        // Core UI components
        drawerLayout = findViewById(R.id.drawer_layout);
        fabAdd = findViewById(R.id.fab_add);
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
        // Initialize TaskService
        taskService = new TaskService(this, this);
        // Initialize CategoryService
        categoryService = new CategoryService(this, this);
        searchManager = new SearchManager(layoutSearch, findViewById(R.id.layout_filter_tabs), editSearch, btnCancelSearch, this);
        // Initialize FilterManager
        filterManager = new FilterManager(this, layoutCategoriesContainer, 
                findViewById(R.id.layout_empty_state), tvEmptyTitle, this);
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
                taskService.getOverdueTasks(),
                taskService.getTodayTasks(),
                taskService.getFutureTasks(),
                taskService.getCompletedTodayTasks(),
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
        addTaskHandler = new AddTaskHandler(this, task -> taskService.loadTasks());
        fabAdd.setOnClickListener(v -> {
            String categoryFilter = filterManager.getCurrentFilter().equals("all") ? null : filterManager.getCurrentFilter();
            addTaskHandler.showAddTaskDialog(null, categoryFilter);
        });
    }
    private void loadData() {
        // Load categories first with initialization, then tasks
        categoryService.initializeDefaultCategories();
        // Don't call categoryService.loadCategories() here as it's already called in initializeDefaultCategories()
        taskService.loadTasks();
    }
    /**
     * Xá»­ lĂ½ khi nháº¥n vĂ o thĂ´ng bĂ¡o
     */
    private void handleNotificationIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            int taskId = intent.getIntExtra(NotificationHelper.EXTRA_TASK_ID, -1);
            String action = intent.getStringExtra("action");
            if (taskId != -1) {
                // Find and display task clicked from notification
                taskService.getTaskById(String.valueOf(taskId), new com.example.todolist.repository.BaseRepository.RepositoryCallback<Task>() {
                    @Override
                    public void onSuccess(Task task) {
                        if (task != null) {
                            // Open TaskDetailActivity to view task details
                            Intent detailIntent = new Intent(MainActivity.this, TaskDetailActivity.class);
                            detailIntent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
                            startActivityForResult(detailIntent, REQUEST_TASK_DETAIL);
                            // Show notification based on action
                            if (NotificationHelper.ACTION_REMINDER.equals(action)) {
                                Toast.makeText(MainActivity.this, "Bạn có lời nhắc cho: " + task.getTitle(), Toast.LENGTH_LONG).show();
                            } else if (NotificationHelper.ACTION_DUE.equals(action)) {
                                Toast.makeText(MainActivity.this, "Nhiệm vụ đã đến hạn: " + task.getTitle(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    @Override
                    public void onError(String error) {
                        // Handle error
                    }
                });
            }
        }
    }
    /**
     * Xá»­ lĂ½ khi cáº§n má»Ÿ navigation drawer tá»« activity khĂ¡c
     */
    private void handleDrawerIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("open_drawer", false)) {
            // Má»Ÿ drawer sau khi layout Ä‘Ă£ sáºµn sĂ ng
            findViewById(android.R.id.content).post(() -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }
    /**
     * Xá»­ lĂ½ intent quick add tá»« mini widget
     */
    private void handleQuickAddIntent() {
        Intent intent = getIntent();
        if (intent != null && "com.example.todolist.QUICK_ADD_TASK".equals(intent.getAction())) {
            // Má»Ÿ dialog thĂªm task sau khi layout Ä‘Ă£ sáºµn sĂ ng
            findViewById(android.R.id.content).post(() -> {
                if (addTaskHandler != null) {
                    addTaskHandler.showAddTaskDialog();
                }
            });
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the current intent
        // Handle quick add from widget when app is already running
        if ("com.example.todolist.QUICK_ADD_TASK".equals(intent.getAction())) {
            if (addTaskHandler != null) {
                addTaskHandler.showAddTaskDialog();
            }
        }
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }
    private Context updateBaseContextLocale(Context context) {
        String languageName = com.example.todolist.util.SettingsManager.getLanguage(context);
        String languageCode;
        if (languageName.equals("English")) {
            languageCode = "en";
        } else {
            languageCode = "vi";
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    @Override
    public void onTaskClick(Task task) {
        if (task.getId() != null && !task.getId().trim().isEmpty()) {
            Intent intent = new Intent(this, TaskDetailActivity.class);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
            startActivityForResult(intent, REQUEST_TASK_DETAIL);
        } else {
            Toast.makeText(this, "Lá»—i: KhĂ´ng thá»ƒ má»Ÿ chi tiáº¿t nhiá»‡m vá»¥", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onTaskComplete(Task task, boolean isCompleted) {
        if (task.getId() != null && !task.getId().trim().isEmpty()) {
            taskService.completeTask(task, isCompleted);
        } else {
            Toast.makeText(this, "Lỗi: Không thể cập nhật nhiệm vụ", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onTaskLongClick(Task task) {
        TaskActionsDialog actionsDialog = new TaskActionsDialog(this, task, new TaskActionsDialog.OnActionSelectedListener() {
            @Override
            public void onStarAction(Task task) {
                onTaskStar(task);
            }
            @Override
            public void onDeleteAction(Task task) {
                onTaskDelete(task);
            }
        });
        actionsDialog.show();
    }
    @Override
    public void onTaskStar(Task task) {
        taskService.toggleTaskImportant(task);
    }
    @Override
    public void onTaskDelete(Task task) {
        taskService.deleteTask(task);
    }
    // TaskService.TaskUpdateListener implementation
    @Override
    public void onTasksUpdated() {
        runOnUiThread(() -> {
            // Update managers with new task data
            searchManager.setTaskLists(taskService.getOverdueTasks(), taskService.getTodayTasks(),
                    taskService.getFutureTasks(), taskService.getCompletedTodayTasks());
            filterManager.setTaskLists(taskService.getOverdueTasks(), taskService.getTodayTasks(),
                    taskService.getFutureTasks(), taskService.getCompletedTodayTasks());
            // Apply current filter
            filterManager.filterTasks(filterManager.getCurrentFilter());
            Log.d("MainActivity", "UI updated - Today tasks: " + taskService.getTodayTasks().size() + 
                  ", Completed today: " + taskService.getCompletedTodayTasks().size());
            // Update section visibility
            sectionManager.updateSectionVisibility(
                    filterManager.getFilteredOverdueTasks(),
                    filterManager.getFilteredTodayTasks(),
                    filterManager.getFilteredFutureTasks(),
                    filterManager.getFilteredCompletedTodayTasks()
            );
        });
    }
    @Override
    public void onError(String error) {
        runOnUiThread(() -> 
            android.widget.Toast.makeText(this, "Error: " + error, android.widget.Toast.LENGTH_SHORT).show()
        );
    }
    // CategoryService.CategoryUpdateListener implementation  
    @Override
    public void onCategoriesUpdated() {
        // Refresh filter buttons when categories are updated
        if (filterManager != null) {
            filterManager.refreshCategories();
        }
    }
    // SearchManager.SearchListener implementation
    @Override
    public void onSearchModeChanged(boolean isSearchMode) {
        // Handle search mode changes if needed
    }
    @Override
    public void onSearchResults(List<Task> overdueResults, List<Task> todayResults,
                               List<Task> futureResults, List<Task> completedResults) {
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
    @Override
    public void onMenuItemSelected(int itemId) {
        if (itemId == R.id.menu_search) {
            searchManager.enterSearchMode();
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
            // Remove unnecessary toast for navigation
        }
    }
    // NavigationDrawerManager.NavigationListener implementation
    @Override
    public void onThemeSelected() {
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
    private void applyLanguageFromSettings() {
        String savedLanguage = com.example.todolist.util.SettingsManager.getLanguage(this);
        String languageCode;
        if (savedLanguage.equals("English")) {
            languageCode = "en";
        } else {
            languageCode = "vi";
        }
        java.util.Locale locale = new java.util.Locale(languageCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    private void setupFirstTimeInstall() {
        android.content.SharedPreferences preferences = getSharedPreferences("TodoApp", MODE_PRIVATE);
        if (!preferences.contains("install_time")) {
            preferences.edit()
                    .putLong("install_time", System.currentTimeMillis())
                    .putString("user_name", "Người dùng")
                    .putInt("current_streak", 0)
                    .putInt("longest_streak", 0)
                    .apply();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskService != null) {
            taskService.cleanup();
        }
        if (categoryService != null) {
            categoryService.cleanup();
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
