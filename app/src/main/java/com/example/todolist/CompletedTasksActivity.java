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
public class CompletedTasksActivity extends AppCompatActivity implements CompletedTasksAdapter.OnCompletedTaskClickListener {
    private ImageView btnBack;
    private RecyclerView recyclerCompletedTasks;
    private CompletedTasksAdapter completedTasksAdapter;
    private TaskService taskService;
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
        // Initialize Firebase TaskService instead of SQLite database
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
        allCompletedTasks = new ArrayList<>();
        groupedTasks = new HashMap<>();
    }
    private void initViews() {
        try {
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
                        .setTitle(getString(R.string.delete_all_title)) // Thay tháº¿ "XĂ³a táº¥t cáº£"
                        .setMessage(getString(R.string.confirm_delete_all_completed_tasks)) // Thay tháº¿ "Báº¡n cĂ³ cháº¯c cháº¯n muá»‘n xĂ³a táº¥t cáº£ nhiá»‡m vá»¥ Ä‘Ă£ hoĂ n thĂ nh?"
                        .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteAllCompletedTasks()) // "XĂ³a"
                        .setNegativeButton(getString(R.string.cancel), null) // "Há»§y"
                        .show();
            });
        } catch (Exception e) {
        }
    }
    private void setupClickListeners() {
        // Click listeners are now handled in initViews()
    }
    private void loadCompletedTasks() {
        taskService.getCompletedTasks(new com.example.todolist.repository.BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                allCompletedTasks = tasks;
                // Group tasks by completion date
                groupTasksByDate();
                runOnUiThread(() -> {
                    completedTasksAdapter.updateGroupedTasks(groupedTasks);
                    updateEmptyState();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> 
                    Toast.makeText(CompletedTasksActivity.this, "Lỗi tải completed tasks: " + error, Toast.LENGTH_SHORT).show()
                );
            }
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
        // Sort tasks within each group by completion time (newest first)
        for (List<Task> tasks : groupedTasks.values()) {
            Collections.sort(tasks, new Comparator<Task>() {
                @Override
                public int compare(Task t1, Task t2) {
                    // Sort by completion date descending (newer tasks first)
                    if (t1.getCompletionDate() != null && t2.getCompletionDate() != null) {
                        try {
                            long time1 = Long.parseLong(t1.getCompletionDate());
                            long time2 = Long.parseLong(t2.getCompletionDate());
                            return Long.compare(time2, time1); // Newer first
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    }
                    return 0;
                }
            });
        }
    }
    private String getDateKey(Task task) {
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
        for (Task task : new ArrayList<>(allCompletedTasks)) {
            taskService.deleteTask(task, new com.example.todolist.repository.BaseRepository.DatabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    // Individual task deleted successfully
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                        Toast.makeText(CompletedTasksActivity.this, "Lỗi xóa task: " + error, Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
        // Clear local lists and update UI
        runOnUiThread(() -> {
            allCompletedTasks.clear();
            groupedTasks.clear();
            completedTasksAdapter.updateGroupedTasks(groupedTasks);
            updateEmptyState();
            finish(); // Go back to main activity
        });
    }
    @Override
    public void onCompletedTaskClick(Task task) {
        // For completed tasks, don't allow editing - just show details in read-only mode
        // We could create a read-only detail view or disable editing in TaskDetailActivity
    }
    @Override
    public void onCompletedTaskLongClick(Task task) {
        // Show action dialog for completed tasks (maybe just delete option)
        TaskActionsDialog actionsDialog = new TaskActionsDialog(this, task, new TaskActionsDialog.OnActionSelectedListener() {
            @Override
            public void onStarAction(Task task) {
                // Don't allow starring completed tasks
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
        // Mark task as incomplete and remove completion date
        new Thread(() -> {
            task.setIsCompleted(false);
            task.setCompletionDate(null); // Clear completion date when marking as incomplete
            taskService.updateTask(task);
            allCompletedTasks.remove(task);
            groupTasksByDate();
            runOnUiThread(() -> {
                completedTasksAdapter.updateGroupedTasks(groupedTasks);
                updateEmptyState();
                // Removed unnecessary toast
            });
        }).start();
    }
    private void deleteTask(Task task) {
        taskService.deleteTask(task, new com.example.todolist.repository.BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                allCompletedTasks.remove(task);
                groupTasksByDate();
                runOnUiThread(() -> {
                    completedTasksAdapter.updateGroupedTasks(groupedTasks);
                    updateEmptyState();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                    Toast.makeText(CompletedTasksActivity.this, "Lỗi xóa task: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskService != null) {
            taskService.cleanup();
        }
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
