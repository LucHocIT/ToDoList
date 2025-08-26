package com.example.todolist.repository;

import androidx.annotation.NonNull;
import com.example.todolist.model.Task;
import com.example.todolist.repository.task.TaskCrudRepository;
import com.example.todolist.repository.task.TaskQueryRepository;
import com.example.todolist.repository.task.TaskListenerRepository;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class TaskRepository extends BaseRepository {
    
    // Sub-repositories that handle specific functionality
    private TaskCrudRepository crudRepository;      // CRUD operations
    private TaskQueryRepository queryRepository;    // Query and filtering
    private TaskListenerRepository listenerRepository; // Realtime listeners
    
    public TaskRepository() {
        this.crudRepository = new TaskCrudRepository();
        this.queryRepository = new TaskQueryRepository();
        this.listenerRepository = new TaskListenerRepository();
    }

    // === CRUD OPERATIONS - Delegate to TaskCrudRepository ===
    public void addTask(Task task, DatabaseCallback<String> callback) {
        crudRepository.addTask(task, callback);
    }

    public void updateTask(Task task, DatabaseCallback<Boolean> callback) {
        crudRepository.updateTask(task, callback);
    }

    public void deleteTask(Task task, DatabaseCallback<Boolean> callback) {
        crudRepository.deleteTask(task, callback);
    }

    public void getTaskById(String taskId, RepositoryCallback<Task> callback) {
        crudRepository.getTaskById(taskId, callback);
    }

    public void updateTaskCompletion(String taskId, boolean isCompleted, DatabaseCallback<Boolean> callback) {
        crudRepository.updateTaskCompletion(taskId, isCompleted, callback);
    }

    public void updateTaskImportance(String taskId, boolean isImportant, DatabaseCallback<Boolean> callback) {
        crudRepository.updateTaskImportance(taskId, isImportant, callback);
    }

    // === QUERY OPERATIONS - Delegate to TaskQueryRepository ===
    public void searchTasks(String query, RepositoryCallback<List<Task>> callback) {
        queryRepository.searchTasks(query, callback);
    }

    public void getTasksByCategory(String categoryId, RepositoryCallback<List<Task>> callback) {
        queryRepository.getTasksByCategory(categoryId, callback);
    }

    public void getTasksByDate(String date, RepositoryCallback<List<Task>> callback) {
        queryRepository.getTasksByDate(date, callback);
    }

    public void getCompletedTasks(RepositoryCallback<List<Task>> callback) {
        queryRepository.getCompletedTasks(callback);
    }

    public void getIncompleteTasks(RepositoryCallback<List<Task>> callback) {
        queryRepository.getIncompleteTasks(callback);
    }

    public void getTodayTasks(RepositoryCallback<List<Task>> callback) {
        queryRepository.getTodayTasks(callback);
    }

    public void getOverdueTasks(RepositoryCallback<List<Task>> callback) {
        queryRepository.getOverdueTasks(callback);
    }

    public void getImportantTasks(RepositoryCallback<List<Task>> callback) {
        queryRepository.getImportantTasks(callback);
    }

    // === LISTENER OPERATIONS - Delegate to TaskListenerRepository ===
    public ValueEventListener addTasksRealtimeListener(ListCallback<Task> callback) {
        return listenerRepository.addTasksRealtimeListener(callback);
    }

    public void removeTasksListener(ValueEventListener listener) {
        listenerRepository.removeTasksListener(listener);
    }

    public ValueEventListener addTaskListener(String taskId, RepositoryCallback<Task> callback) {
        return listenerRepository.addTaskListener(taskId, callback);
    }

    public void removeTaskListener(String taskId, ValueEventListener listener) {
        listenerRepository.removeTaskListener(taskId, listener);
    }

    public void getAllTasks(ListCallback<Task> callback) {
        listenerRepository.getAllTasks(callback);
    }

    public void syncTasksWithLocal(List<Task> localTasks, ListCallback<Task> callback) {
        listenerRepository.syncTasksWithLocal(localTasks, callback);
    }

    // === LEGACY COMPATIBILITY METHODS ===
    @Deprecated
    public void getAllTasks(RepositoryCallback<List<Task>> callback) {
        getAllTasks(new ListCallback<Task>() {
            @Override
            public void onSuccess(List<Task> tasks) {
                callback.onSuccess(tasks);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    // === SUBTASK OPERATIONS ===
    public void getSubTasks(String taskId, ListCallback<com.example.todolist.model.SubTask> callback) {
        com.example.todolist.util.FirebaseHelper firebaseHelper = com.example.todolist.util.FirebaseHelper.getInstance();
        firebaseHelper.getSubTasksReference().child(taskId)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        java.util.List<com.example.todolist.model.SubTask> subTasks = new java.util.ArrayList<>();
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            com.example.todolist.model.SubTask subTask = child.getValue(com.example.todolist.model.SubTask.class);
                            if (subTask != null) {
                                subTask.setId(child.getKey());
                                subTasks.add(subTask);
                            }
                        }
                        callback.onSuccess(subTasks);
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }
}
