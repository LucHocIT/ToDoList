package com.example.todolist;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.adapter.CompletedTasksAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.SettingsManager;
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
        Log.d("CompletedTasks", "onCreate started");
        
        try {
            setContentView(R.layout.activity_completed_tasks);
            Log.d("CompletedTasks", "setContentView completed");
            
            initDatabase();
            Log.d("CompletedTasks", "initDatabase completed");
            
            initViews();
            Log.d("CompletedTasks", "initViews completed");
            
            setupClickListeners();
            Log.d("CompletedTasks", "setupClickListeners completed");
            
            loadCompletedTasks();
            Log.d("CompletedTasks", "loadCompletedTasks completed");
            
        } catch (Exception e) {
            Log.e("CompletedTasks", "Error in onCreate: " + e.getMessage(), e);
        }
    }
    
    private void initDatabase() {
        database = TodoDatabase.getInstance(this);
        allCompletedTasks = new ArrayList<>();
        groupedTasks = new HashMap<>();
    }
    
    private void initViews() {
        Log.d("CompletedTasks", "initViews started");
        
        try {
            btnBack = findViewById(R.id.btn_back);
            Log.d("CompletedTasks", "btnBack found");
            
            ImageView btnDeleteAll = findViewById(R.id.btn_delete_all);
            Log.d("CompletedTasks", "btnDeleteAll found");
            
            recyclerCompletedTasks = findViewById(R.id.recycler_completed_tasks);
            Log.d("CompletedTasks", "recyclerCompletedTasks found");
            
            // Setup RecyclerView
            recyclerCompletedTasks.setLayoutManager(new LinearLayoutManager(this));
            Log.d("CompletedTasks", "LayoutManager set");
            
            completedTasksAdapter = new CompletedTasksAdapter(groupedTasks, this);
            Log.d("CompletedTasks", "Adapter created");
            
            recyclerCompletedTasks.setAdapter(completedTasksAdapter);
            Log.d("CompletedTasks", "Adapter set");
            
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
            
            Log.d("CompletedTasks", "Click listeners set");
            
        } catch (Exception e) {
            Log.e("CompletedTasks", "Error in initViews: " + e.getMessage(), e);
        }
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
                updateEmptyState();
            });
        }).start();
    }
    
    private void updateEmptyState() {
        View emptyState = findViewById(R.id.empty_state);
        if (allCompletedTasks.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerCompletedTasks.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerCompletedTasks.setVisibility(View.VISIBLE);
        }
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
        // Use completion date instead of due date for completed tasks
        String completionDate = task.getCompletionDate();
        if (completionDate != null && !completionDate.isEmpty()) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                Date date = inputFormat.parse(completionDate);
                
                Calendar taskCal = Calendar.getInstance();
                taskCal.setTime(date);
                
                Calendar today = Calendar.getInstance();
                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DAY_OF_YEAR, -1);
                
                if (isSameDay(taskCal, today)) {
                    return completionDate; // Use actual completion date
                } else if (isSameDay(taskCal, yesterday)) {
                    return completionDate; // Use actual completion date
                } else {
                    return completionDate;
                }
            } catch (Exception e) {
                return completionDate;
            }
        }
        
        // Fallback to current date if no completion date (shouldn't happen for completed tasks)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        return dateFormat.format(new Date());
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
                updateEmptyState();
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

    @Override
    public void onCompletedTaskUncheck(TodoTask task) {
        // Mark task as incomplete and remove completion date
        new Thread(() -> {
            task.setCompleted(false);
            task.setCompletionDate(null); // Clear completion date when marking as incomplete
            database.todoDao().updateTask(task);
            
            allCompletedTasks.remove(task);
            groupTasksByDate();
            
            runOnUiThread(() -> {
                completedTasksAdapter.updateGroupedTasks(groupedTasks);
                updateEmptyState();
                Toast.makeText(this, "Nhiệm vụ đã được đánh dấu chưa hoàn thành", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    
    private void deleteTask(TodoTask task) {
        new Thread(() -> {
            database.todoDao().deleteTask(task);
            allCompletedTasks.remove(task);
            groupTasksByDate();
            
            runOnUiThread(() -> {
                completedTasksAdapter.updateGroupedTasks(groupedTasks);
                updateEmptyState();
            });
        }).start();
    }
    
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }
    
    private Context updateBaseContextLocale(Context context) {
        String languageName = SettingsManager.getLanguage(context);
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
}
