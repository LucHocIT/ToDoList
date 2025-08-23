package com.example.todolist.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;

import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.service.task.TaskManager;
import com.example.todolist.service.task.TaskCompletionService;
import com.example.todolist.service.task.TaskListService;
import com.example.todolist.widget.WidgetUpdateHelper;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Main TaskService orchestrator - coordinates all task operations
 * Replaces the original long TaskService by delegating to specialized sub-services
 */
public class TaskService {
    
    public interface TaskUpdateListener {
        void onTasksUpdated();
        void onError(String error);
    }

    public interface TaskOperationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    private Context context;
    private TaskRepository taskRepository;
    private TaskUpdateListener listener;
    private ValueEventListener realtimeListener;
    
    // Sub-services that handle specific functionality
    private TaskManager taskManager;              // CRUD operations + reminders
    private TaskCompletionService completionService;  // Completion/importance logic
    private TaskListService listService;         // Categorization + filtering
    
    // Firebase debouncing (inherited from original TaskService)
    private Handler firebaseUpdateHandler;
    private Runnable pendingFirebaseUpdate;
    private static final long FIREBASE_UPDATE_DELAY = 500;
    private long lastLocalUpdateTime = 0;
    private static final long LOCAL_UPDATE_PRIORITY_WINDOW = 2000;
    
    private List<Task> allTasks;

    public TaskService(Context context, TaskUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.taskRepository = new TaskRepository();
        this.firebaseUpdateHandler = new Handler(Looper.getMainLooper());
        
        // Initialize specialized sub-services
        this.taskManager = new TaskManager(context);
        this.completionService = new TaskCompletionService();
        this.listService = new TaskListService();
        
        this.allTasks = new ArrayList<>();
    }

    public void loadTasks() {
        realtimeListener = taskRepository.addTasksRealtimeListener(new BaseRepository.ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                handleFirebaseTasksUpdate(tasks);
            }

            @Override
            public void onError(String error) {
                if (listener != null) {
                    listener.onError("Lỗi tải tasks: " + error);
                }
            }
        });
    }
    
    private void handleFirebaseTasksUpdate(List<Task> tasks) {
        android.util.Log.d("TaskService", "Firebase tasks received: " + tasks.size());
        for (Task task : tasks) {
            android.util.Log.d("TaskService", "Task: " + task.getTitle() + ", Date: " + task.getDueDate() + ", Completed: " + task.isCompleted());
        }
        
        if (pendingFirebaseUpdate != null) {
            firebaseUpdateHandler.removeCallbacks(pendingFirebaseUpdate);
        }
        
        pendingFirebaseUpdate = () -> {
            long timeSinceLastLocalUpdate = System.currentTimeMillis() - lastLocalUpdateTime;
            if (timeSinceLastLocalUpdate < LOCAL_UPDATE_PRIORITY_WINDOW) {
                pendingFirebaseUpdate = null;
                return;
            }
            
            allTasks = tasks;
            listService.categorizeTasks(allTasks);
            
            android.util.Log.d("TaskService", "After categorization - Today: " + listService.getTodayTasks().size() + 
                              ", Overdue: " + listService.getOverdueTasks().size() + 
                              ", Future: " + listService.getFutureTasks().size());
            
            notifyListener();
            WidgetUpdateHelper.updateAllWidgets(context);
            pendingFirebaseUpdate = null;
        };
        
        firebaseUpdateHandler.postDelayed(pendingFirebaseUpdate, FIREBASE_UPDATE_DELAY);
    }
    
    private void notifyListener() {
        if (listener != null) {
            listener.onTasksUpdated();
        }
    }

    // === CORE OPERATIONS - Delegate to TaskManager ===
    public void addTask(Task task) {
        taskManager.addTask(task, null);
    }
    
    public void addTask(Task task, TaskOperationCallback callback) {
        taskManager.addTask(task, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String taskId) {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void updateTask(Task task) {
        taskManager.updateTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                // Success handled by Firebase listener
            }

            @Override
            public void onError(String error) {
                if (listener != null) {
                    listener.onError("Lỗi cập nhật task: " + error);
                }
            }
        });
    }

    public void updateTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        taskManager.updateTask(task, callback);
    }

    public void deleteTask(Task task) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa task này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    taskManager.deleteTask(task, new BaseRepository.DatabaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            // Success handled by Firebase listener
                        }

                        @Override
                        public void onError(String error) {
                            if (listener != null) {
                                listener.onError("Lỗi xóa task: " + error);
                            }
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    public void deleteTask(Task task, BaseRepository.DatabaseCallback<Boolean> callback) {
        taskManager.deleteTask(task, callback);
    }

    // === COMPLETION OPERATIONS - Delegate to TaskCompletionService ===
    public void completeTask(Task task, boolean isCompleted) {
        lastLocalUpdateTime = System.currentTimeMillis();
        cancelPendingFirebaseUpdates();
        
        // Update local state immediately
        completionService.completeTask(task, isCompleted, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                updateTaskInList(task);
                listService.categorizeTasks(allTasks);
                notifyListener();
            }

            @Override
            public void onError(String error) {
                // Revert on error
                task.setIsCompleted(!isCompleted);
                task.setCompletionDate(isCompleted ? null : String.valueOf(System.currentTimeMillis()));
                
                updateTaskInList(task);
                listService.categorizeTasks(allTasks);
                notifyListener();
                
                if (listener != null) {
                    listener.onError("Lỗi cập nhật trạng thái task: " + error);
                }
            }
        });
    }

    public void toggleTaskCompletion(Task task) {
        completeTask(task, !task.isCompleted());
    }
    
    public void toggleTaskImportance(Task task) {
        completionService.toggleTaskImportance(task, null);
    }

    // === LIST OPERATIONS - Delegate to TaskListService ===
    public List<Task> getAllTasks() { return new ArrayList<>(allTasks); }
    public List<Task> getOverdueTasks() { return listService.getOverdueTasks(); }
    public List<Task> getTodayTasks() { return listService.getTodayTasks(); }
    public List<Task> getFutureTasks() { return listService.getFutureTasks(); }
    public List<Task> getCompletedTodayTasks() { return listService.getCompletedTodayTasks(); }
    
    public List<Task> getIncompleteTasks() { 
        List<Task> incomplete = new ArrayList<>();
        for (Task task : allTasks) {
            if (!task.isCompleted()) {
                incomplete.add(task);
            }
        }
        return incomplete;
    }
    
    // === ASYNC OPERATIONS ===
    public void getAllTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        callback.onSuccess(getAllTasks());
    }
    
    public void searchTasks(String query, BaseRepository.RepositoryCallback<List<Task>> callback) {
        taskManager.searchTasks(query, callback);
    }
    
    public void getTasksByCategory(String categoryId, BaseRepository.RepositoryCallback<List<Task>> callback) {
        listService.getTasksByCategory(categoryId, callback);
    }
    
    public void getTaskById(String taskId, BaseRepository.RepositoryCallback<Task> callback) {
        taskManager.getTaskById(taskId, callback);
    }
    
    public void getTasksByDate(String date, BaseRepository.RepositoryCallback<List<Task>> callback) {
        listService.getTasksByDate(date, callback);
    }
    
    public void getCompletedTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        completionService.getCompletedTasks(callback);
    }
    
    public void getUncompletedTasks(BaseRepository.RepositoryCallback<List<Task>> callback) {
        completionService.getIncompleteTasks(callback);
    }
    
    // === UTILITIES ===
    private void updateTaskInList(Task updatedTask) {
        if (allTasks != null && updatedTask.getId() != null) {
            for (int i = 0; i < allTasks.size(); i++) {
                Task task = allTasks.get(i);
                if (updatedTask.getId().equals(task.getId())) {
                    allTasks.set(i, updatedTask);
                    break;
                }
            }
        }
    }
    
    private void cancelPendingFirebaseUpdates() {
        if (pendingFirebaseUpdate != null) {
            firebaseUpdateHandler.removeCallbacks(pendingFirebaseUpdate);
            pendingFirebaseUpdate = null;
        }
    }

    public void rescheduleAllReminders() {
        // Delegate to taskManager if needed
        if (taskManager != null) {
        }
    }
    
    public void toggleTaskImportant(Task task) {
        task.setIsImportant(!task.isImportant());
        updateTask(task);
    }
    
    public void cleanup() {
        if (realtimeListener != null) {
            taskRepository.removeTasksListener(realtimeListener);
        }
    }
}
