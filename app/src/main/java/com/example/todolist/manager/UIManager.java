package com.example.todolist.manager;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.CalendarActivity;
import com.example.todolist.CategoryManagerActivity;
import com.example.todolist.CompletedTasksActivity;
import com.example.todolist.R;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Category;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.TaskSortDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class UIManager {
    
    public interface UIListener {
        void onMenuItemSelected(int itemId);
        void onSortTypeSelected(com.example.todolist.util.SortType sortType);
        void onBottomNavigation(String section);
    }
    
    private AppCompatActivity activity;
    private UIListener listener;
    private TodoDatabase database;
    
    // UI Components
    private FloatingActionButton fabAdd;
    private ImageView btnMenu;
    private TextView textCheckAllCompleted;
    
    public UIManager(AppCompatActivity activity, FloatingActionButton fabAdd, ImageView btnMenu,
                    TextView textCheckAllCompleted, UIListener listener) {
        this.activity = activity;
        this.fabAdd = fabAdd;
        this.btnMenu = btnMenu;
        this.textCheckAllCompleted = textCheckAllCompleted;
        this.listener = listener;
        this.database = TodoDatabase.getInstance(activity);
        
        setupUIListeners();
    }
    
    private void setupUIListeners() {
        btnMenu.setOnClickListener(v -> showPopupMenu(v));
        
        textCheckAllCompleted.setOnClickListener(v -> {
            Intent intent = new Intent(activity, CompletedTasksActivity.class);
            activity.startActivity(intent);
        });
    }
    
    public TaskAdapter[] setupRecyclerViews(RecyclerView recyclerOverdueTasks, RecyclerView recyclerTodayTasks,
                                  RecyclerView recyclerFutureTasks, RecyclerView recyclerCompletedTodayTasks,
                                  List<TodoTask> overdueTasks, List<TodoTask> todayTasks,
                                  List<TodoTask> futureTasks, List<TodoTask> completedTodayTasks,
                                  TaskAdapter.OnTaskClickListener taskClickListener) {
        
        // Setup RecyclerViews and Adapters
        recyclerOverdueTasks.setLayoutManager(new LinearLayoutManager(activity));
        TaskAdapter overdueAdapter = new TaskAdapter(overdueTasks, taskClickListener);
        recyclerOverdueTasks.setAdapter(overdueAdapter);

        recyclerTodayTasks.setLayoutManager(new LinearLayoutManager(activity));
        TaskAdapter todayAdapter = new TaskAdapter(todayTasks, taskClickListener);
        recyclerTodayTasks.setAdapter(todayAdapter);

        recyclerFutureTasks.setLayoutManager(new LinearLayoutManager(activity));
        TaskAdapter futureAdapter = new TaskAdapter(futureTasks, taskClickListener);
        recyclerFutureTasks.setAdapter(futureAdapter);

        recyclerCompletedTodayTasks.setLayoutManager(new LinearLayoutManager(activity));
        TaskAdapter completedAdapter = new TaskAdapter(completedTodayTasks, taskClickListener);
        recyclerCompletedTodayTasks.setAdapter(completedAdapter);
        
        return new TaskAdapter[] { overdueAdapter, todayAdapter, futureAdapter, completedAdapter };
    }
    
    public void setupBottomNavigation() {
        LinearLayout btnNavMenu = activity.findViewById(R.id.btn_nav_menu);
        LinearLayout btnNavTasks = activity.findViewById(R.id.btn_nav_tasks);
        LinearLayout btnNavCalendar = activity.findViewById(R.id.btn_nav_calendar);
        LinearLayout btnNavProfile = activity.findViewById(R.id.btn_nav_profile);
        
        if (btnNavCalendar != null) {
            btnNavCalendar.setOnClickListener(v -> {
                Intent intent = new Intent(activity, CalendarActivity.class);
                activity.startActivity(intent);
            });
        }
        
        if (btnNavMenu != null) {
            btnNavMenu.setOnClickListener(v -> {
                // Open navigation drawer instead of showing toast
                if (listener != null) {
                    listener.onBottomNavigation("menu_drawer");
                }
            });
        }
        
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> {
                Toast.makeText(activity, "Của tôi", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(activity, anchor);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_category_manager) {
                activity.startActivity(new Intent(activity, CategoryManagerActivity.class));
                return true;
            } else if (item.getItemId() == R.id.menu_search) {
                if (listener != null) listener.onMenuItemSelected(R.id.menu_search);
                return true;
            } else if (item.getItemId() == R.id.menu_sort_tasks) {
                showSortDialog();
                return true;
            } else if (item.getItemId() == R.id.menu_reset_data) {
                showResetDataDialog();
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void showSortDialog() {
        TaskSortDialog sortDialog = new TaskSortDialog(activity, sortType -> {
            if (listener != null) {
                listener.onSortTypeSelected(sortType);
            }
        });
        sortDialog.show();
    }
    
    private void showResetDataDialog() {
        new AlertDialog.Builder(activity)
            .setTitle("Reset dữ liệu")
            .setMessage("Bạn có chắc chắn muốn xóa tất cả dữ liệu và tạo lại từ đầu?")
            .setPositiveButton("Reset", (dialog, which) -> {
                if (listener != null) {
                    listener.onMenuItemSelected(R.id.menu_reset_data);
                }
                Toast.makeText(activity, "Đã reset tất cả dữ liệu", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
    
    public void setupCategorySpinner(Spinner spinner) {
        new Thread(() -> {
            try {
                List<Category> categories = database.categoryDao().getAllCategories();
                
                if (categories.isEmpty()) {
                    createDefaultCategories();
                    categories = database.categoryDao().getAllCategories();
                }
                
                final List<Category> finalCategories = categories;
                activity.runOnUiThread(() -> {
                    try {
                        List<String> categoryNames = new ArrayList<>();
                        categoryNames.add("Không có thể loại");
                        for (Category category : finalCategories) {
                            categoryNames.add(category.getName());
                        }
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            activity, 
                            android.R.layout.simple_spinner_item, 
                            categoryNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                        spinner.setSelection(0);
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        setupFallbackSpinner(spinner);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> setupFallbackSpinner(spinner));
            }
        }).start();
    }
    
    private void setupFallbackSpinner(Spinner spinner) {
        List<String> defaultCategories = new ArrayList<>();
        defaultCategories.add("Không có thể loại");
        defaultCategories.add("Công việc");
        defaultCategories.add("Cá nhân");
        defaultCategories.add("Yêu thích");
        
        ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(
            activity, 
            android.R.layout.simple_spinner_item, 
            defaultCategories
        );
        fallbackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(fallbackAdapter);
        spinner.setSelection(0);
    }
    
    private void createDefaultCategories() {
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
}
