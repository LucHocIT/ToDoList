package com.example.todolist;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.service.TaskService;
import com.example.todolist.model.Task;
import com.example.todolist.util.SettingsManager;
import com.example.todolist.util.TaskActionsDialog;
import com.example.todolist.repository.BaseRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class SearchActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {
    private EditText editSearch;
    private RecyclerView recyclerSearchResults;
    private TaskAdapter searchAdapter;
    private TaskService taskService;
    private List<Task> allTasks;
    private List<Task> searchResults;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initTaskService();
        initViews();
        setupSearch();
        loadTasks();
    }
    private void initTaskService() {
        taskService = new TaskService(this, null);
        allTasks = new ArrayList<>();
        searchResults = new ArrayList<>();
    }
    private void initViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        editSearch = findViewById(R.id.edit_search);
        recyclerSearchResults = findViewById(R.id.recycler_search_results);
        btnBack.setOnClickListener(v -> finish());
        // Setup RecyclerView
        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new TaskAdapter(searchResults, this);
        recyclerSearchResults.setAdapter(searchAdapter);
        // Focus on search input
        editSearch.requestFocus();
    }
    private void setupSearch() {
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
    private void loadTasks() {
        taskService.getAllTasks(new BaseRepository.RepositoryCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                allTasks.clear();
                allTasks.addAll(tasks);
            }
            @Override
            public void onError(String error) {
                // Handle error if needed
            }
        });
    }
    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            searchResults.clear();
            runOnUiThread(() -> searchAdapter.updateTasks(searchResults));
            return;
        }
        searchResults.clear();
        String lowercaseQuery = query.toLowerCase();
        for (Task task : allTasks) {
            if (task.getTitle().toLowerCase().contains(lowercaseQuery) || 
                task.getDescription().toLowerCase().contains(lowercaseQuery)) {
                searchResults.add(task);
            }
        }
        runOnUiThread(() -> searchAdapter.updateTasks(searchResults));
    }
    // TaskAdapter.OnTaskClickListener implementation
    @Override
    public void onTaskClick(Task task) {
        // Handle task click
    }
    @Override
    public void onTaskComplete(Task task, boolean isCompleted) {
        task.setIsCompleted(isCompleted);
        task.setIsCompleted(isCompleted);
        
        taskService.updateTask(task);
        runOnUiThread(() -> searchAdapter.notifyDataSetChanged());
    }
    @Override
    public void onTaskLongClick(Task task) {
        // Show actions dialog on long click
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
        task.setIsImportant(!task.isImportant());
        taskService.updateTask(task);
        runOnUiThread(() -> searchAdapter.notifyDataSetChanged());
    }
    @Override
    public void onTaskDelete(Task task) {
        taskService.deleteTask(task);
        allTasks.remove(task);
        searchResults.remove(task);
        runOnUiThread(() -> searchAdapter.updateTasks(searchResults));
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
