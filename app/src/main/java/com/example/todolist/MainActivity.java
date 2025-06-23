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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.recyclerview.widget.ItemTouchHelper;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;
import com.example.todolist.model.Category;
import com.example.todolist.util.SwipeToRevealHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private RecyclerView recyclerIncompleteTasks;
    private RecyclerView recyclerCompletedTasks;
    private TaskAdapter incompleteTasksAdapter;
    private TaskAdapter completedTasksAdapter;
    private FloatingActionButton fabAdd;
    private MaterialButton btnAll, btnWork, btnPersonal;
    private ImageView btnMenu;
    
    // Search components
    private LinearLayout layoutFilterTabs;
    private LinearLayout layoutSearch;
    private LinearLayout layoutCategoriesContainer;
    private EditText editSearch;
    private ImageView btnCancelSearch;
    
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
        btnMenu = findViewById(R.id.btn_menu);
        
        // Search components
        layoutFilterTabs = findViewById(R.id.layout_filter_tabs);
        layoutSearch = findViewById(R.id.layout_search);
        layoutCategoriesContainer = findViewById(R.id.layout_categories_container);
        editSearch = findViewById(R.id.edit_search);
        btnCancelSearch = findViewById(R.id.btn_cancel_search);
    }

    private void setupRecyclerViews() {
        // Incomplete tasks RecyclerView
        recyclerIncompleteTasks.setLayoutManager(new LinearLayoutManager(this));
        incompleteTasksAdapter = new TaskAdapter(incompleteTasks, this);
        recyclerIncompleteTasks.setAdapter(incompleteTasksAdapter);
        
        // Add swipe gesture for incomplete tasks
        ItemTouchHelper incompleteHelper = new ItemTouchHelper(new SwipeToRevealHelper() {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Handle swipe completion if needed
            }
            
            @Override
            public void onStarClicked(int position) {
                if (position < incompleteTasks.size()) {
                    onTaskStar(incompleteTasks.get(position));
                }
            }
            
            @Override
            public void onCalendarClicked(int position) {
                if (position < incompleteTasks.size()) {
                    onTaskCalendar(incompleteTasks.get(position));
                }
            }
            
            @Override
            public void onDeleteClicked(int position) {
                if (position < incompleteTasks.size()) {
                    onTaskDelete(incompleteTasks.get(position));
                }
            }
        });
        incompleteHelper.attachToRecyclerView(recyclerIncompleteTasks);

        // Completed tasks RecyclerView
        recyclerCompletedTasks.setLayoutManager(new LinearLayoutManager(this));
        completedTasksAdapter = new TaskAdapter(completedTasks, this);
        recyclerCompletedTasks.setAdapter(completedTasksAdapter);
    }

    private void setupClickListeners() {
        fabAdd.setOnClickListener(v -> showAddTaskDialog());
        
        btnAll.setOnClickListener(v -> filterTasks("all"));
        btnWork.setOnClickListener(v -> filterTasks("work"));
        btnPersonal.setOnClickListener(v -> filterTasks("personal"));
        
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
                // Clear existing dynamic categories (keep the 3 default buttons)
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
    
    private void createDefaultCategories() {
        // Create default categories
        Category allCategory = new Category("Tất cả", "#4285F4", 0, true);
        Category workCategory = new Category("Công việc", "#FF9800", 1, true);
        Category personalCategory = new Category("Cá nhân", "#9C27B0", 2, true);
        
        database.categoryDao().insertCategory(allCategory);
        database.categoryDao().insertCategory(workCategory);
        database.categoryDao().insertCategory(personalCategory);
        
        // Add some additional sample categories
        Category studyCategory = new Category("Học tập", "#2196F3", 3, false);
        Category healthCategory = new Category("Sức khỏe", "#4CAF50", 4, false);
        Category familyCategory = new Category("Gia đình", "#FF5722", 5, false);
        
        database.categoryDao().insertCategory(studyCategory);
        database.categoryDao().insertCategory(healthCategory);
        database.categoryDao().insertCategory(familyCategory);
    }
    
    private boolean isDefaultCategory(String categoryName) {
        return categoryName.equals("Tất cả") || 
               categoryName.equals("Công việc") || 
               categoryName.equals("Cá nhân");
    }
    
    private void clearDynamicCategories() {
        // Remove all views after the 3rd child (default categories + dynamic ones)
        int childCount = layoutCategoriesContainer.getChildCount();
        if (childCount > 3) {
            layoutCategoriesContainer.removeViews(3, childCount - 3);
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
        categoryButton.setOnClickListener(v -> filterTasks(category.getName().toLowerCase()));
        
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
        TodoTask newTask = new TodoTask(title, "", "2025/05/25", "22:00");
        
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
                incompleteTasksAdapter.updateTasks(incompleteTasks);
                completedTasksAdapter.updateTasks(completedTasks);
            });
        }).start();
    }
    
    private void addSampleData() {
        TodoTask sampleTask = new TodoTask(
            "Chúc ngủ ngon, đã đến giờ đi ngủ", 
            "", 
            "2025/05/25", 
            "22:00"
        );
        sampleTask.setHasReminder(true);
        database.todoDao().insertTask(sampleTask);
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
            String lowerQuery = query.toLowerCase().trim();
            
            for (TodoTask task : incompleteTasks) {
                if (task.getTitle().toLowerCase().contains(lowerQuery) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerQuery))) {
                    filteredIncompleteTasks.add(task);
                }
            }
            
            for (TodoTask task : completedTasks) {
                if (task.getTitle().toLowerCase().contains(lowerQuery) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerQuery))) {
                    filteredCompletedTasks.add(task);
                }
            }
        }
        
        // Update adapters with filtered results
        incompleteTasksAdapter.updateTasks(filteredIncompleteTasks);
        completedTasksAdapter.updateTasks(filteredCompletedTasks);
    }

    private void filterTasks(String filter) {
        // Update button states
        resetFilterButtons();
        switch (filter) {
            case "all":
                btnAll.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnAll.setTextColor(getColor(android.R.color.white));
                break;
            case "work":
                btnWork.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnWork.setTextColor(getColor(android.R.color.white));
                break;
            case "personal":
                btnPersonal.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnPersonal.setTextColor(getColor(android.R.color.white));
                break;
        }
        
        // Filter logic would go here
        loadTasks();
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
    public void onTaskStar(TodoTask task) {
        task.setImportant(!task.isImportant());
        new Thread(() -> {
            database.todoDao().updateTask(task);
            runOnUiThread(this::loadTasks);
        }).start();
        Toast.makeText(this, task.isImportant() ? "Đã đánh dấu quan trọng" : "Đã bỏ đánh dấu quan trọng", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskCalendar(TodoTask task) {
        // Open calendar/date picker
        Toast.makeText(this, "Mở lịch để chỉnh sửa ngày", Toast.LENGTH_SHORT).show();
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
            }
            return false;
        });
        
        popup.show();
    }
}