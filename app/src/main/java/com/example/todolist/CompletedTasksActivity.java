package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.adapter.CompletedTasksAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.TaskActionsDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CompletedTasksActivity extends AppCompatActivity implements CompletedTasksAdapter.OnCompletedTaskClickListener {

    private ImageView btnBack;
    private RecyclerView recyclerCompletedTasks;
    private CompletedTasksAdapter completedTasksAdapter;
    
    private TodoDatabase database;
    private List<TodoTask> allCompletedTasks;
    private Map<String, List<TodoTask>> groupedTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_tasks);
        
        initDatabase();
        initViews();
        setupClickListeners();
        loadCompletedTasks();
    }
    
    private void initDatabase() {
        database = TodoDatabase.getInstance(this);
        allCompletedTasks = new ArrayList<>();
        groupedTasks = new HashMap<>();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ImageView btnDeleteAll = findViewById(R.id.btn_delete_all);
        recyclerCompletedTasks = findViewById(R.id.recycler_completed_tasks);
        
        // Setup RecyclerView
        recyclerCompletedTasks.setLayoutManager(new LinearLayoutManager(this));
        completedTasksAdapter = new CompletedTasksAdapter(groupedTasks, this);
        recyclerCompletedTasks.setAdapter(completedTasksAdapter);
        
        // Setup click listeners
        btnBack.setOnClickListener(v -> finish());
        
        btnDeleteAll.setOnClickListener(v -> {
            // Show confirmation dialog and delete all completed tasks
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa tất cả")
                .setMessage("Bạn có chắc chắn muốn xóa tất cả nhiệm vụ đã hoàn thành?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteAllCompletedTasks())
                .setNegativeButton("Hủy", null)
                .show();
        });
    }
    
    private void setupClickListeners() {
        // Click listeners are now handled in initViews()
    }
    
    private void loadCompletedTasks() {
        new Thread(() -> {
            // Get all completed tasks
            allCompletedTasks = database.todoDao().getCompletedTasks();
            
            // Group tasks by completion date
            groupTasksByDate();
            
            runOnUiThread(() -> {
                completedTasksAdapter.updateGroupedTasks(groupedTasks);
            });
        }).start();
    }
    
    private void groupTasksByDate() {
        groupedTasks.clear();
        
        for (TodoTask task : allCompletedTasks) {
            String dateKey = getDateKey(task);
            
            if (!groupedTasks.containsKey(dateKey)) {
                groupedTasks.put(dateKey, new ArrayList<>());
            }
            groupedTasks.get(dateKey).add(task);
        }
        
        // Sort tasks within each group by completion time (newest first)
        for (List<TodoTask> tasks : groupedTasks.values()) {
            Collections.sort(tasks, new Comparator<TodoTask>() {
                @Override
                public int compare(TodoTask t1, TodoTask t2) {
                    // Sort by ID descending (newer tasks have higher IDs)
                    return Integer.compare(t2.getId(), t1.getId());
                }
            });
        }
    }
    
    private String getDateKey(TodoTask task) {
        // For now, we'll use the due date as completion date
        // In a real app, you'd have a separate completion date field
        String dueDate = task.getDueDate();
        if (dueDate != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                Date date = inputFormat.parse(dueDate);
                
                Calendar taskCal = Calendar.getInstance();
                taskCal.setTime(date);
                
                Calendar today = Calendar.getInstance();
                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DAY_OF_YEAR, -1);
                
                if (isSameDay(taskCal, today)) {
                    return "2025/06/25"; // Today's date
                } else if (isSameDay(taskCal, yesterday)) {
                    return "2025/06/23"; // Yesterday's date  
                } else {
                    return dueDate;
                }
            } catch (Exception e) {
                return dueDate != null ? dueDate : "2025/06/24";
            }
        }
        return "2025/06/24"; // Default date
    }
    
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    private void deleteAllCompletedTasks() {
        new Thread(() -> {
            for (TodoTask task : allCompletedTasks) {
                database.todoDao().deleteTask(task);
            }
            
            runOnUiThread(() -> {
                allCompletedTasks.clear();
                groupedTasks.clear();
                completedTasksAdapter.updateGroupedTasks(groupedTasks);
                finish(); // Go back to main activity
            });
        }).start();
    }

    @Override
    public void onCompletedTaskClick(TodoTask task) {
        // For completed tasks, don't allow editing - just show details in read-only mode
        // We could create a read-only detail view or disable editing in TaskDetailActivity
    }

    @Override
    public void onCompletedTaskLongClick(TodoTask task) {
        // Show action dialog for completed tasks (maybe just delete option)
        TaskActionsDialog actionsDialog = new TaskActionsDialog(this, task, new TaskActionsDialog.OnActionSelectedListener() {
            @Override
            public void onStarAction(TodoTask task) {
                // Don't allow starring completed tasks
            }

            @Override
            public void onDeleteAction(TodoTask task) {
                deleteTask(task);
            }
        });
        actionsDialog.show();
    }
    
    private void deleteTask(TodoTask task) {
        new Thread(() -> {
            database.todoDao().deleteTask(task);
            allCompletedTasks.remove(task);
            groupTasksByDate();
            
            runOnUiThread(() -> {
                completedTasksAdapter.updateGroupedTasks(groupedTasks);
            });
        }).start();
    }
}
