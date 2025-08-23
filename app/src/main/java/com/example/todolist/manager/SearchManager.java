package com.example.todolist.manager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
public class SearchManager {
    public interface SearchListener {
        void onSearchModeChanged(boolean isSearchMode);
        void onSearchResults(List<Task> overdueResults, List<Task> todayResults, 
                            List<Task> futureResults, List<Task> completedResults);
    }
    private SearchListener listener;
    private boolean isSearchMode = false;
    // UI Components
    private LinearLayout layoutSearch;
    private LinearLayout layoutFilterTabs; // Add reference to filter tabs
    private EditText editSearch;
    private ImageView btnCancelSearch;
    // Task lists
    private List<Task> overdueTasks;
    private List<Task> todayTasks;
    private List<Task> futureTasks;
    private List<Task> completedTodayTasks;
    // Filtered results
    private List<Task> filteredOverdueTasks;
    private List<Task> filteredTodayTasks;
    private List<Task> filteredFutureTasks;
    private List<Task> filteredCompletedTodayTasks;
    public SearchManager(LinearLayout layoutSearch, LinearLayout layoutFilterTabs, EditText editSearch, ImageView btnCancelSearch, SearchListener listener) {
        this.layoutSearch = layoutSearch;
        this.layoutFilterTabs = layoutFilterTabs;
        this.editSearch = editSearch;
        this.btnCancelSearch = btnCancelSearch;
        this.listener = listener;
        initializeFilteredLists();
        setupSearchListeners();
    }
    private void initializeFilteredLists() {
        filteredOverdueTasks = new ArrayList<>();
        filteredTodayTasks = new ArrayList<>();
        filteredFutureTasks = new ArrayList<>();
        filteredCompletedTodayTasks = new ArrayList<>();
    }
    private void setupSearchListeners() {
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
    public void setTaskLists(List<Task> overdueTasks, List<Task> todayTasks, 
                           List<Task> futureTasks, List<Task> completedTodayTasks) {
        this.overdueTasks = overdueTasks;
        this.todayTasks = todayTasks;
        this.futureTasks = futureTasks;
        this.completedTodayTasks = completedTodayTasks;
    }
    public void enterSearchMode() {
        isSearchMode = true;
        layoutFilterTabs.setVisibility(View.GONE); // Hide filter tabs
        layoutSearch.setVisibility(View.VISIBLE);
        editSearch.requestFocus();
        // Initialize with all tasks
        filteredOverdueTasks.clear();
        filteredTodayTasks.clear();
        filteredFutureTasks.clear();
        filteredCompletedTodayTasks.clear();
        if (overdueTasks != null) filteredOverdueTasks.addAll(overdueTasks);
        if (todayTasks != null) filteredTodayTasks.addAll(todayTasks);
        if (futureTasks != null) filteredFutureTasks.addAll(futureTasks);
        if (completedTodayTasks != null) filteredCompletedTodayTasks.addAll(completedTodayTasks);
        if (listener != null) {
            listener.onSearchModeChanged(true);
        }
    }
    public void exitSearchMode() {
        isSearchMode = false;
        layoutSearch.setVisibility(View.GONE);
        layoutFilterTabs.setVisibility(View.VISIBLE); // Show filter tabs again
        editSearch.setText("");
        if (listener != null) {
            listener.onSearchModeChanged(false);
        }
    }
    private void performSearch(String query) {
        if (!isSearchMode) return;
        filteredOverdueTasks.clear();
        filteredTodayTasks.clear();
        filteredFutureTasks.clear();
        filteredCompletedTodayTasks.clear();
        if (query.trim().isEmpty()) {
            // Show all tasks if search is empty
            if (overdueTasks != null) filteredOverdueTasks.addAll(overdueTasks);
            if (todayTasks != null) filteredTodayTasks.addAll(todayTasks);
            if (futureTasks != null) filteredFutureTasks.addAll(futureTasks);
            if (completedTodayTasks != null) filteredCompletedTodayTasks.addAll(completedTodayTasks);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault()).trim();
            // Filter each task list
            filterTaskList(overdueTasks, filteredOverdueTasks, lowerQuery);
            filterTaskList(todayTasks, filteredTodayTasks, lowerQuery);
            filterTaskList(futureTasks, filteredFutureTasks, lowerQuery);
            filterTaskList(completedTodayTasks, filteredCompletedTodayTasks, lowerQuery);
        }
        if (listener != null) {
            listener.onSearchResults(filteredOverdueTasks, filteredTodayTasks, 
                                   filteredFutureTasks, filteredCompletedTodayTasks);
        }
    }
    private void filterTaskList(List<Task> source, List<Task> destination, String query) {
        if (source == null) return;
        for (Task task : source) {
            if (task.getTitle().toLowerCase(Locale.getDefault()).contains(query) ||
                (task.getDescription() != null && task.getDescription().toLowerCase(Locale.getDefault()).contains(query))) {
                destination.add(task);
            }
        }
    }
    // Getters
    public boolean isSearchMode() { return isSearchMode; }
    public List<Task> getFilteredOverdueTasks() { return filteredOverdueTasks; }
    public List<Task> getFilteredTodayTasks() { return filteredTodayTasks; }
    public List<Task> getFilteredFutureTasks() { return filteredFutureTasks; }
    public List<Task> getFilteredCompletedTodayTasks() { return filteredCompletedTodayTasks; }
}
