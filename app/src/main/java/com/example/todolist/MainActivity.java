package com.example.todolist;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.recyclerview.widget.ItemTouchHelper;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.TodoTask;
import com.example.todolist.util.SwipeToRevealHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private RecyclerView recyclerIncompleteTasks;
    private RecyclerView recyclerCompletedTasks;
    private TaskAdapter incompleteTasksAdapter;
    private TaskAdapter completedTasksAdapter;
    private FloatingActionButton fabAdd;
    private MaterialButton btnAll, btnWork, btnPersonal;
    
    private TodoDatabase database;
    private List<TodoTask> allTasks;
    private List<TodoTask> incompleteTasks;
    private List<TodoTask> completedTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDatabase();
        initViews();
        setupRecyclerViews();
        setupClickListeners();
        loadTasks();
    }
    
    private void initDatabase() {
        database = TodoDatabase.getInstance(this);
        allTasks = new ArrayList<>();
        incompleteTasks = new ArrayList<>();
        completedTasks = new ArrayList<>();
    }

    private void initViews() {
        recyclerIncompleteTasks = findViewById(R.id.recycler_incomplete_tasks);
        recyclerCompletedTasks = findViewById(R.id.recycler_completed_tasks);
        fabAdd = findViewById(R.id.fab_add);
        btnAll = findViewById(R.id.btn_all);
        btnWork = findViewById(R.id.btn_work);
        btnPersonal = findViewById(R.id.btn_personal);
    }

    private void setupRecyclerViews() {
        // Incomplete tasks RecyclerView
        recyclerIncompleteTasks.setLayoutManager(new LinearLayoutManager(this));
        incompleteTasksAdapter = new TaskAdapter(incompleteTasks, this);
        recyclerIncompleteTasks.setAdapter(incompleteTasksAdapter);
        
        // Add swipe gesture for incomplete tasks
        ItemTouchHelper incompleteHelper = new ItemTouchHelper(new SwipeToRevealHelper() {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Handle swipe completion if needed
            }
            
            @Override
            public void onStarClicked(int position) {
                if (position < incompleteTasks.size()) {
                    onTaskStar(incompleteTasks.get(position));
                }
            }
            
            @Override
            public void onCalendarClicked(int position) {
                if (position < incompleteTasks.size()) {
                    onTaskCalendar(incompleteTasks.get(position));
                }
            }
            
            @Override
            public void onDeleteClicked(int position) {
                if (position < incompleteTasks.size()) {
                    onTaskDelete(incompleteTasks.get(position));
                }
            }
        });
        incompleteHelper.attachToRecyclerView(recyclerIncompleteTasks);

        // Completed tasks RecyclerView
        recyclerCompletedTasks.setLayoutManager(new LinearLayoutManager(this));
        completedTasksAdapter = new TaskAdapter(completedTasks, this);
        recyclerCompletedTasks.setAdapter(completedTasksAdapter);
    }

    private void setupClickListeners() {
        fabAdd.setOnClickListener(v -> showAddTaskDialog());
        
        btnAll.setOnClickListener(v -> filterTasks("all"));
        btnWork.setOnClickListener(v -> filterTasks("work"));
        btnPersonal.setOnClickListener(v -> filterTasks("personal"));
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        
        // Make dialog background transparent so CardView corners show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        EditText editTaskTitle = dialogView.findViewById(R.id.edit_task_title);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_save);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String title = editTaskTitle.getText().toString().trim();
            if (!title.isEmpty()) {
                createNewTask(title);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng nhập tiêu đề nhiệm vụ", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void createNewTask(String title) {
        TodoTask newTask = new TodoTask(title, "", "2025/05/25", "22:00");
        
        // Add to database
        new Thread(() -> {
            database.todoDao().insertTask(newTask);
            runOnUiThread(this::loadTasks);
        }).start();
        
        Toast.makeText(this, "Đã thêm nhiệm vụ mới", Toast.LENGTH_SHORT).show();
    }

    private void loadTasks() {
        new Thread(() -> {
            allTasks = database.todoDao().getAllTasks();
            
            // Add sample data if empty
            if (allTasks.isEmpty()) {
                addSampleData();
                allTasks = database.todoDao().getAllTasks();
            }
            
            runOnUiThread(() -> {
                updateTaskLists();
                incompleteTasksAdapter.updateTasks(incompleteTasks);
                completedTasksAdapter.updateTasks(completedTasks);
            });
        }).start();
    }
    
    private void addSampleData() {
        TodoTask sampleTask = new TodoTask(
            "Chúc ngủ ngon, đã đến giờ đi ngủ", 
            "", 
            "2025/05/25", 
            "22:00"
        );
        sampleTask.setHasReminder(true);
        database.todoDao().insertTask(sampleTask);
    }

    private void updateTaskLists() {
        incompleteTasks.clear();
        completedTasks.clear();
        
        for (TodoTask task : allTasks) {
            if (task.isCompleted()) {
                completedTasks.add(task);
            } else {
                incompleteTasks.add(task);
            }
        }
    }

    private void filterTasks(String filter) {
        // Update button states
        resetFilterButtons();
        switch (filter) {
            case "all":
                btnAll.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnAll.setTextColor(getColor(android.R.color.white));
                break;
            case "work":
                btnWork.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnWork.setTextColor(getColor(android.R.color.white));
                break;
            case "personal":
                btnPersonal.setBackgroundTintList(getColorStateList(R.color.primary_blue));
                btnPersonal.setTextColor(getColor(android.R.color.white));
                break;
        }
        
        // Filter logic would go here
        loadTasks();
    }

    private void resetFilterButtons() {
        int grayColor = getColor(R.color.light_gray);
        int textColor = getColor(R.color.text_gray);
        
        btnAll.setBackgroundTintList(getColorStateList(R.color.light_gray));
        btnAll.setTextColor(textColor);
        btnWork.setBackgroundTintList(getColorStateList(R.color.light_gray));
        btnWork.setTextColor(textColor);
        btnPersonal.setBackgroundTintList(getColorStateList(R.color.light_gray));
        btnPersonal.setTextColor(textColor);
    }

    // TaskAdapter.OnTaskClickListener implementation
    @Override
    public void onTaskClick(TodoTask task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }

    @Override
    public void onTaskComplete(TodoTask task, boolean isCompleted) {
        task.setCompleted(isCompleted);
        new Thread(() -> {
            database.todoDao().updateTask(task);
            runOnUiThread(this::loadTasks);
        }).start();
    }

    @Override
    public void onTaskStar(TodoTask task) {
        task.setImportant(!task.isImportant());
        new Thread(() -> {
            database.todoDao().updateTask(task);
            runOnUiThread(this::loadTasks);
        }).start();
        Toast.makeText(this, task.isImportant() ? "Đã đánh dấu quan trọng" : "Đã bỏ đánh dấu quan trọng", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskCalendar(TodoTask task) {
        // Open calendar/date picker
        Toast.makeText(this, "Mở lịch để chỉnh sửa ngày", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskDelete(TodoTask task) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa nhiệm vụ")
                .setMessage("Bạn có chắc chắn muốn xóa nhiệm vụ này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    new Thread(() -> {
                        database.todoDao().deleteTask(task);
                        runOnUiThread(this::loadTasks);
                    }).start();
                    Toast.makeText(this, "Đã xóa nhiệm vụ", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}