package com.example.todolist;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.TaskActionsDialog;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private EditText editSearch;
    private RecyclerView recyclerSearchResults;
    private TaskAdapter searchAdapter;
    
    private TodoDatabase database;
    private List<TodoTask> allTasks;
    private List<TodoTask> searchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initDatabase();
        initViews();
        setupSearch();
        loadTasks();
    }

    private void initDatabase() {
        database = TodoDatabase.getInstance(this);
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
        new Thread(() -> {
            allTasks = database.todoDao().getAllTasks();
        }).start();
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            searchResults.clear();
            runOnUiThread(() -> searchAdapter.updateTasks(searchResults));
            return;
        }

        searchResults.clear();
        String lowercaseQuery = query.toLowerCase();
        
        for (TodoTask task : allTasks) {
            if (task.getTitle().toLowerCase().contains(lowercaseQuery) || 
                task.getDescription().toLowerCase().contains(lowercaseQuery)) {
                searchResults.add(task);
            }
        }
        
        runOnUiThread(() -> searchAdapter.updateTasks(searchResults));
    }

    // TaskAdapter.OnTaskClickListener implementation
    @Override
    public void onTaskClick(TodoTask task) {
        // Handle task click
    }

    @Override
    public void onTaskComplete(TodoTask task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        new Thread(() -> {
            database.todoDao().updateTask(task);
            runOnUiThread(() -> searchAdapter.notifyDataSetChanged());
        }).start();
    }

    @Override
    public void onTaskLongClick(TodoTask task) {
        // Show actions dialog on long click
        TaskActionsDialog actionsDialog = new TaskActionsDialog(this, task, new TaskActionsDialog.OnActionSelectedListener() {
            @Override
            public void onStarAction(TodoTask task) {
                onTaskStar(task);
            }

            @Override
            public void onDeleteAction(TodoTask task) {
                onTaskDelete(task);
            }
        });
        actionsDialog.show();
    }

    @Override
    public void onTaskStar(TodoTask task) {
        task.setImportant(!task.isImportant());
        new Thread(() -> {
            database.todoDao().updateTask(task);
            runOnUiThread(() -> searchAdapter.notifyDataSetChanged());
        }).start();
    }

    @Override
    public void onTaskDelete(TodoTask task) {
        new Thread(() -> {
            database.todoDao().deleteTask(task);
            allTasks.remove(task);
            searchResults.remove(task);
            runOnUiThread(() -> searchAdapter.updateTasks(searchResults));
        }).start();
    }
}
