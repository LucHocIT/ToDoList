package com.example.todolist.manager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.model.Task;
import java.util.List;
public class SectionManager {
    // Section layouts
    private LinearLayout sectionOverdueTasks;
    private LinearLayout sectionTodayTasks;
    private LinearLayout sectionFutureTasks;
    private LinearLayout sectionCompletedTodayTasks;
    // RecyclerViews
    private RecyclerView recyclerOverdueTasks;
    private RecyclerView recyclerTodayTasks;
    private RecyclerView recyclerFutureTasks;
    private RecyclerView recyclerCompletedTodayTasks;
    // Section headers
    private LinearLayout headerOverdueTasks;
    private LinearLayout headerTodayTasks;
    private LinearLayout headerFutureTasks;
    private LinearLayout headerCompletedTodayTasks;
    // Expand/collapse icons
    private ImageView iconExpandOverdue;
    private ImageView iconExpandToday;
    private ImageView iconExpandFuture;
    private ImageView iconExpandCompleted;
    // Collapse state tracking
    private boolean isOverdueCollapsed = false;
    private boolean isTodayCollapsed = false;
    private boolean isFutureCollapsed = false;
    private boolean isCompletedCollapsed = false;
    public SectionManager(LinearLayout sectionOverdueTasks, LinearLayout sectionTodayTasks,
                         LinearLayout sectionFutureTasks, LinearLayout sectionCompletedTodayTasks,
                         RecyclerView recyclerOverdueTasks, RecyclerView recyclerTodayTasks,
                         RecyclerView recyclerFutureTasks, RecyclerView recyclerCompletedTodayTasks,
                         LinearLayout headerOverdueTasks, LinearLayout headerTodayTasks,
                         LinearLayout headerFutureTasks, LinearLayout headerCompletedTodayTasks,
                         ImageView iconExpandOverdue, ImageView iconExpandToday,
                         ImageView iconExpandFuture, ImageView iconExpandCompleted) {
        this.sectionOverdueTasks = sectionOverdueTasks;
        this.sectionTodayTasks = sectionTodayTasks;
        this.sectionFutureTasks = sectionFutureTasks;
        this.sectionCompletedTodayTasks = sectionCompletedTodayTasks;
        this.recyclerOverdueTasks = recyclerOverdueTasks;
        this.recyclerTodayTasks = recyclerTodayTasks;
        this.recyclerFutureTasks = recyclerFutureTasks;
        this.recyclerCompletedTodayTasks = recyclerCompletedTodayTasks;
        this.headerOverdueTasks = headerOverdueTasks;
        this.headerTodayTasks = headerTodayTasks;
        this.headerFutureTasks = headerFutureTasks;
        this.headerCompletedTodayTasks = headerCompletedTodayTasks;
        this.iconExpandOverdue = iconExpandOverdue;
        this.iconExpandToday = iconExpandToday;
        this.iconExpandFuture = iconExpandFuture;
        this.iconExpandCompleted = iconExpandCompleted;
        setupHeaderClickListeners();
    }
    private void setupHeaderClickListeners() {
        headerOverdueTasks.setOnClickListener(v -> toggleSection("overdue"));
        headerTodayTasks.setOnClickListener(v -> toggleSection("today"));
        headerFutureTasks.setOnClickListener(v -> toggleSection("future"));
        headerCompletedTodayTasks.setOnClickListener(v -> toggleSection("completed"));
    }
    public void toggleSection(String sectionType) {
        switch (sectionType) {
            case "overdue":
                isOverdueCollapsed = !isOverdueCollapsed;
                recyclerOverdueTasks.setVisibility(isOverdueCollapsed ? View.GONE : View.VISIBLE);
                iconExpandOverdue.setImageResource(isOverdueCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
                break;
            case "today":
                isTodayCollapsed = !isTodayCollapsed;
                recyclerTodayTasks.setVisibility(isTodayCollapsed ? View.GONE : View.VISIBLE);
                iconExpandToday.setImageResource(isTodayCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
                break;
            case "future":
                isFutureCollapsed = !isFutureCollapsed;
                recyclerFutureTasks.setVisibility(isFutureCollapsed ? View.GONE : View.VISIBLE);
                iconExpandFuture.setImageResource(isFutureCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
                break;
            case "completed":
                isCompletedCollapsed = !isCompletedCollapsed;
                recyclerCompletedTodayTasks.setVisibility(isCompletedCollapsed ? View.GONE : View.VISIBLE);
                iconExpandCompleted.setImageResource(isCompletedCollapsed ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
                break;
        }
    }
    public void updateSectionVisibility(List<Task> filteredOverdueTasks, List<Task> filteredTodayTasks,
                                       List<Task> filteredFutureTasks, List<Task> filteredCompletedTodayTasks) {
        // Show/hide overdue section
        if (filteredOverdueTasks.isEmpty()) {
            sectionOverdueTasks.setVisibility(View.GONE);
        } else {
            sectionOverdueTasks.setVisibility(View.VISIBLE);
        }
        // Show/hide today section
        if (filteredTodayTasks.isEmpty()) {
            sectionTodayTasks.setVisibility(View.GONE);
        } else {
            sectionTodayTasks.setVisibility(View.VISIBLE);
        }
        // Show/hide future section
        if (filteredFutureTasks.isEmpty()) {
            sectionFutureTasks.setVisibility(View.GONE);
        } else {
            sectionFutureTasks.setVisibility(View.VISIBLE);
        }
        // Show/hide completed today section
        if (filteredCompletedTodayTasks.isEmpty()) {
            sectionCompletedTodayTasks.setVisibility(View.GONE);
        } else {
            sectionCompletedTodayTasks.setVisibility(View.VISIBLE);
        }
    }
}
