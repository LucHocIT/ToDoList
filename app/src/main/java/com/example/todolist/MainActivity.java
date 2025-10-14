package com.example.todolist;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
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
import com.example.todolist.model.Category;
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
import java.util.List;
import com.example.todolist.util.AddTaskHandler;
import com.example.todolist.util.FirebaseMigrationHelper;
import com.example.todolist.util.NotificationPermissionHelper;
import com.example.todolist.util.SortType;
import com.example.todolist.util.TaskActionsDialog;
import com.example.todolist.cache.SharedTaskCacheManager;
import com.example.todolist.service.sharing.SharedTaskSyncService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.todolist.view.BottomNavigationManager;
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
    private SharedTaskCacheManager sharedTaskCacheManager;
    private SharedTaskSyncService sharedTaskSyncService;
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
    private BroadcastReceiver taskRefreshReceiver;
    
    private static final int REQUEST_TASK_DETAIL = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyLanguageFromSettings();
        setContentView(R.layout.activity_main);
        initViews();
        initManagers();
        setupAddTaskHandler();
        loadData();
        setupFirstTimeInstall();
        handleNotificationIntent();
        handleDrawerIntent();
        handleQuickAddIntent();
        handleJoinTaskIntent();
        NotificationPermissionHelper.requestNotificationPermission(this);
        setupTaskRefreshReceiver();
    }
    
    private void setupTaskRefreshReceiver() {
        taskRefreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.todolist.REFRESH_TASKS".equals(intent.getAction())) {
                    taskService.forceReloadSharedTasks();
                    taskService.loadTasks();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.example.todolist.REFRESH_TASKS");
    
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(taskRefreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(taskRefreshReceiver, filter);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        taskService.rescheduleAllReminders();
        
        // Force reload shared tasks để đảm bảo realtime sync
        taskService.forceReloadSharedTasks();
        
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
        if (NotificationPermissionHelper.isNotificationPermissionGranted(requestCode, grantResults)) {
            taskService.rescheduleAllReminders();
        } else if (requestCode == NotificationPermissionHelper.NOTIFICATION_PERMISSION_CODE) {
            Toast.makeText(this, "Cần quyền thông báo để nhận lời nhắc", Toast.LENGTH_LONG).show();
        }

        if (navigationDrawerManager != null) {
            navigationDrawerManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        fabAdd = findViewById(R.id.fab_add);
        btnMenu = findViewById(R.id.btn_menu);
        layoutSearch = findViewById(R.id.layout_search);
        layoutCategoriesContainer = findViewById(R.id.layout_categories_container);
        editSearch = findViewById(R.id.edit_search);
        btnCancelSearch = findViewById(R.id.btn_cancel_search);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
    }
    private void initManagers() {
        FirebaseMigrationHelper.checkAndMigrate(this);
        
        taskService = new TaskService(this, this);
        categoryService = new CategoryService(this, this);
        searchManager = new SearchManager(layoutSearch, findViewById(R.id.layout_filter_tabs), editSearch, btnCancelSearch, this);
        filterManager = new FilterManager(this, layoutCategoriesContainer, 
                findViewById(R.id.layout_empty_state), tvEmptyTitle, this);
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
        uiManager = new UIManager(this, fabAdd, btnMenu, findViewById(R.id.text_check_all_completed), this);
        navigationDrawerManager = new NavigationDrawerManager(this, drawerLayout, this);
        themeManager = new ThemeManager(this, this);
        
        // Initialize shared task sync service
        sharedTaskSyncService = SharedTaskSyncService.getInstance();
        sharedTaskSyncService.initialize(this);
        sharedTaskSyncService.addUpdateListener(new SharedTaskSyncService.SharedTaskUpdateListener() {
            @Override
            public void onSharedTaskUpdated(Task task) {
                runOnUiThread(() -> taskService.loadTasks());
            }

            @Override
            public void onSubTaskUpdated(String taskId, com.example.todolist.model.SubTask subTask) {
                runOnUiThread(() -> taskService.loadTasks());
            }

            @Override
            public void onTaskSharingChanged(com.example.todolist.model.TaskShare taskShare) {
                runOnUiThread(() -> taskService.loadTasks());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi sync shared task: " + error, Toast.LENGTH_SHORT).show());
            }
        });
        
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
        if (adapters != null && adapters.length == 4) {
            filterManager.setAdapters(adapters[0], adapters[1], adapters[2], adapters[3]);
        }
        
        // Setup unified bottom navigation
        BottomNavigationManager.setupForActivity(this, BottomNavigationManager.SCREEN_TASKS);
    }
    private void setupAddTaskHandler() {
        addTaskHandler = new AddTaskHandler(this, task -> taskService.loadTasks());
        fabAdd.setOnClickListener(v -> {
            String categoryFilter = filterManager.getCurrentFilter().equals("all") ? null : filterManager.getCurrentFilter();
            addTaskHandler.showAddTaskDialog(null, categoryFilter);
        });
    }
    private void loadData() {
        categoryService.initializeDefaultCategories(new CategoryService.CategoryOperationCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(String error) {

            }
        });
        taskService.loadTasks();
    }

    private void handleNotificationIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("open_task_detail", false)) {
                String taskId = intent.getStringExtra("task_id");
                if (taskId != null) {
                    findViewById(android.R.id.content).post(() -> {
                        Task task = taskService.getTaskByIdFromCache(taskId);
                        if (task != null) {
                            Intent detailIntent = new Intent(MainActivity.this, TaskDetailActivity.class);
                            detailIntent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
                            startActivityForResult(detailIntent, REQUEST_TASK_DETAIL);
                        }
                    });
                }
                return;
            }

            int taskId = intent.getIntExtra(NotificationHelper.EXTRA_TASK_ID, -1);
            String taskIdStr = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID);
            String action = intent.getStringExtra("action");
            
            // Try both int and string task ID for backward compatibility
            if (taskId != -1) {
                Task task = taskService.getTaskByIdFromCache(String.valueOf(taskId));
                if (task != null) {
                    Intent detailIntent = new Intent(MainActivity.this, TaskDetailActivity.class);
                    detailIntent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
                    startActivityForResult(detailIntent, REQUEST_TASK_DETAIL);
                }
            } else if (taskIdStr != null) {
                Task task = taskService.getTaskByIdFromCache(taskIdStr);
                if (task != null) {
                    Intent detailIntent = new Intent(MainActivity.this, TaskDetailActivity.class);
                    detailIntent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
                    startActivityForResult(detailIntent, REQUEST_TASK_DETAIL);
                    if (NotificationHelper.ACTION_REMINDER.equals(action)) {
                        Toast.makeText(MainActivity.this, "Bạn có lời nhắc cho: " + task.getTitle(), Toast.LENGTH_LONG).show();
                    } else if (NotificationHelper.ACTION_DUE.equals(action)) {
                        Toast.makeText(MainActivity.this, "Nhiệm vụ đã đến hạn: " + task.getTitle(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private void handleJoinTaskIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("force_reload_tasks", false)) {
            String joinedTaskId = intent.getStringExtra("joined_task_id");

            taskService.forceReloadSharedTasks();
            // Kiểm tra và cập nhật shared status của tất cả tasks
            taskService.checkAndUpdateAllSharedStatus();

            if (joinedTaskId != null) {
                Toast.makeText(this, "🎉 Task được chia sẻ đã được thêm vào danh sách!", Toast.LENGTH_LONG).show();
            }

            intent.removeExtra("force_reload_tasks");
            intent.removeExtra("joined_task_id");
        }
    }

    private void handleDrawerIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("open_drawer", false)) {
            findViewById(android.R.id.content).post(() -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    private void handleQuickAddIntent() {
        Intent intent = getIntent();
        if (intent != null && "com.example.todolist.QUICK_ADD_TASK".equals(intent.getAction())) {
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
        setIntent(intent); 
        handleJoinTaskIntent();
        
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
            Toast.makeText(this, "Lỗi: Không thể mở chi tiết nhiệm vụ", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onTasksUpdated() {
        runOnUiThread(() -> {
            loadSubTasksAndUpdateUI();
        });
    }
    
    private void loadSubTasksAndUpdateUI() {
        taskService.loadSubTasksForAllTasks();
        new android.os.Handler().postDelayed(() -> {
            searchManager.setTaskLists(taskService.getOverdueTasks(), taskService.getTodayTasks(),
                    taskService.getFutureTasks(), taskService.getCompletedTodayTasks());
            filterManager.setTaskLists(taskService.getOverdueTasks(), taskService.getTodayTasks(),
                    taskService.getFutureTasks(), taskService.getCompletedTodayTasks());
            filterManager.filterTasks(filterManager.getCurrentFilter());
            sectionManager.updateSectionVisibility(
                    filterManager.getFilteredOverdueTasks(),
                    filterManager.getFilteredTodayTasks(),
                    filterManager.getFilteredFutureTasks(),
                    filterManager.getFilteredCompletedTodayTasks()
            );
        }, 100); 
    }
    @Override
    public void onError(String error) {
        runOnUiThread(() -> 
            android.widget.Toast.makeText(this, "Error: " + error, android.widget.Toast.LENGTH_SHORT).show()
        );
    }
    @Override
    public void onCategoriesUpdated(List<Category> categories) {
        if (filterManager != null) {
            filterManager.refreshCategories();
        }
    }

    @Override
    public void onSearchModeChanged(boolean isSearchMode) {

    }
    @Override
    public void onSearchResults(List<Task> overdueResults, List<Task> todayResults,
                               List<Task> futureResults, List<Task> completedResults) {
        sectionManager.updateSectionVisibility(overdueResults, todayResults, futureResults, completedResults);
    }

    @Override
    public void onFilterChanged(String filter) {
        sectionManager.updateSectionVisibility(
                filterManager.getFilteredOverdueTasks(),
                filterManager.getFilteredTodayTasks(),
                filterManager.getFilteredFutureTasks(),
                filterManager.getFilteredCompletedTodayTasks()
        );
    }
    @Override
    public void onEmptyStateChanged(boolean isEmpty, String message) {

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

        }
    }

    @Override
    public void onThemeSelected() {
    }
    @Override
    public void onUtilitiesSelected() {
        // Widget functionality has been moved to a separate screen
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
    @Override
    public void onContactSelected() {

    }
    @Override
    public void onSettingsSelected() {

    }

    @Override
    public void onThemeChanged(ThemeManager.ThemeColor themeColor) {
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
        if (sharedTaskSyncService != null) {
            sharedTaskSyncService.stopListeningForAllTasks();
        }
        if (taskRefreshReceiver != null) {
            unregisterReceiver(taskRefreshReceiver);
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
