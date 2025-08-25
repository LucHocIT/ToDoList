package com.example.todolist;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.adapter.CompletedTasksAdapter;
import com.example.todolist.cache.TaskCache;
import com.example.todolist.model.Task;
import com.example.todolist.service.TaskService;
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
public class CompletedTasksActivity extends AppCompatActivity implements CompletedTasksAdapter.OnCompletedTaskClickListener, TaskCache.TaskCacheListener {
    private ImageView btnBack;
    private RecyclerView recyclerCompletedTasks;
    private CompletedTasksAdapter completedTasksAdapter;
    private TaskService taskService;
    private TaskCache taskCache;
    private List<Task> allCompletedTasks;
    private Map<String, List<Task>> groupedTasks;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_completed_tasks);
            initDatabase();
            initViews();
            setupClickListeners();
            loadCompletedTasks();
        } catch (Exception e) {
        }
    }
    private void initDatabase() {
        taskService = new TaskService(this, new TaskService.TaskUpdateListener() {
            @Override
            public void onTasksUpdated() {
                runOnUiThread(() -> loadCompletedTasks());
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> 
                    Toast.makeText(CompletedTasksActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
        taskCache = TaskCache.getInstance();
        taskCache.addListener(this);
        
        allCompletedTasks = new ArrayList<>();
        groupedTasks = new HashMap<>();
    }
    private void initViews() {
        try {
            btnBack = findViewById(R.id.btn_back);
            ImageView btnDeleteAll = findViewById(R.id.btn_delete_all);
            recyclerCompletedTasks = findViewById(R.id.recycler_completed_tasks);
            recyclerCompletedTasks.setLayoutManager(new LinearLayoutManager(this));
            completedTasksAdapter = new CompletedTasksAdapter(groupedTasks, this);
            recyclerCompletedTasks.setAdapter(completedTasksAdapter);
            btnBack.setOnClickListener(v -> finish());
            btnDeleteAll.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete_all_title)) 
                        .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteAllCompletedTasks()) 
                        .setNegativeButton(getString(R.string.cancel), null) 
                        .show();
            });
        } catch (Exception e) {
        }
    }
    private void setupClickListeners() {

    }
    private void loadCompletedTasks() {
      allCompletedTasks = taskService.getCompletedTasksFromCache();
        groupTasksByDate();
        runOnUiThread(() -> {
            completedTasksAdapter.updateGroupedTasks(groupedTasks);
            updateEmptyState();
        });
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
        for (Task task : allCompletedTasks) {
            String dateKey = getDateKey(task);
            if (!groupedTasks.containsKey(dateKey)) {
                groupedTasks.put(dateKey, new ArrayList<>());
            }
            groupedTasks.get(dateKey).add(task);
        }

        for (List<Task> tasks : groupedTasks.values()) {
            Collections.sort(tasks, new Comparator<Task>() {
                @Override
                public int compare(Task t1, Task t2) {
                    if (t1.getCompletionDate() != null && t2.getCompletionDate() != null) {
                        return t2.getCompletionDate().compareTo(t1.getCompletionDate());
                    }
                    return 0;
                }
            });
        }
    }
    private String getDateKey(Task task) {

        String completionDate = task.getCompletionDate();
        if (completionDate != null && !completionDate.isEmpty()) {
            return completionDate;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(new Date());
    }
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    private void deleteAllCompletedTasks() {
        for (Task task : new ArrayList<>(allCompletedTasks)) {
            // Sử dụng optimistic delete - UI sẽ được cập nhật qua TaskCache listener
            taskService.deleteTask(task);
        }
        runOnUiThread(() -> {
            finish(); 
        });
    }
    @Override
    public void onCompletedTaskClick(Task task) {
    }
    @Override
    public void onCompletedTaskLongClick(Task task) {
        TaskActionsDialog actionsDialog = new TaskActionsDialog(this, task, new TaskActionsDialog.OnActionSelectedListener() {
            @Override
            public void onStarAction(Task task) {
            }
            @Override
            public void onDeleteAction(Task task) {
                deleteTask(task);
            }
        });
        actionsDialog.show();
    }
    @Override
    public void onCompletedTaskUncheck(Task task) {
        task.setIsCompleted(false);
        task.setCompletionDate(null); 
        // Sử dụng optimistic update - UI sẽ được cập nhật qua TaskCache listener
        taskService.updateTask(task);
    }
    private void deleteTask(Task task) {
        // Sử dụng optimistic delete - UI sẽ được cập nhật qua TaskCache listener
        taskService.deleteTask(task);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskCache != null) {
            taskCache.removeListener(this);
        }
        if (taskService != null) {
            taskService.cleanup();
        }
    }
    @Override
    public void onTasksUpdated(List<Task> tasks) {
        loadCompletedTasks();
    }

    @Override
    public void onTaskAdded(Task task) {
        if (task.isCompleted()) {
            loadCompletedTasks();
        }
    }

    @Override
    public void onTaskUpdated(Task task) {
        loadCompletedTasks();
    }

    @Override
    public void onTaskDeleted(String taskId) {
        loadCompletedTasks();
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
