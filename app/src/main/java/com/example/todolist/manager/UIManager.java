package com.example.todolist.manager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
import com.example.todolist.adapter.CategorySpinnerAdapter;
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
        
        // Use unified navigation helper with drawer initialization
        com.example.todolist.util.UnifiedNavigationHelper.setupBottomNavigation(
            activity, btnNavMenu, btnNavTasks, btnNavCalendar, null, "tasks");
            
        // Initialize drawer for MainActivity
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        if (drawerLayout != null && listener instanceof com.example.todolist.manager.NavigationDrawerManager.NavigationListener) {
            com.example.todolist.util.UnifiedNavigationHelper.initializeDrawerForActivity(
                activity, drawerLayout, (com.example.todolist.manager.NavigationDrawerManager.NavigationListener) listener);
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

    public void setupCategorySpinner(Spinner spinner) {
        new Thread(() -> {
            try {
                List<Category> categories = database.categoryDao().getAllCategories();
                Log.d("UIManager", "setupCategorySpinner: Loaded " + categories.size() + " categories from database");
                
                // Debug: Print all categories from database
                for (Category cat : categories) {
                    Log.d("UIManager", "Database category: " + cat.getName() + " (ID: " + cat.getId() + ")");
                }
                
                activity.runOnUiThread(() -> {
                    try {
                        // Use CategorySpinnerAdapter for consistency
                        CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(activity, categories);
                        spinner.setAdapter(adapter);
                        spinner.setSelection(0); // Select "không có thể loại" by default
                        
                        Log.d("UIManager", "CategorySpinnerAdapter created successfully");
                        
                    } catch (Exception e) {
                        Log.e("UIManager", "Error setting up CategorySpinnerAdapter: " + e.getMessage());
                        e.printStackTrace();
                        setupFallbackSpinner(spinner);
                    }
                });
            } catch (Exception e) {
                Log.e("UIManager", "Error loading categories from database: " + e.getMessage());
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
}
