package com.example.todolist.repository.task;

import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * TaskQueryRepository - Xá»­ lĂ½ cĂ¡c truy váº¥n vĂ  lá»c dá»¯ liá»‡u Task
 */
public class TaskQueryRepository extends BaseRepository {
    
    private TaskCrudRepository crudRepository;

    public TaskQueryRepository() {
        this.crudRepository = new TaskCrudRepository();
    }

    public void searchTasks(String query, RepositoryCallback<List<Task>> callback) {
        crudRepository.getTaskRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Task> results = new ArrayList<>();
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setId(taskSnapshot.getKey());
                        
                        if (query == null || query.trim().isEmpty() ||
                            task.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                            (task.getDescription() != null && 
                             task.getDescription().toLowerCase().contains(query.toLowerCase()))) {
                            results.add(task);
                        }
                    }
                }
                
                callback.onSuccess(results);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Lá»—i tim kiem: " + databaseError.getMessage());
            }
        });
    }

    public void getTasksByCategory(String categoryId, RepositoryCallback<List<Task>> callback) {
        crudRepository.getTaskRef().orderByChild("categoryId").equalTo(categoryId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Task> tasks = new ArrayList<>();
                    
                    for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                        Task task = taskSnapshot.getValue(Task.class);
                        if (task != null) {
                            task.setId(taskSnapshot.getKey());
                            tasks.add(task);
                        }
                    }
                    
                    callback.onSuccess(tasks);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    callback.onError("Lá»—i láº¥y tasks theo category: " + databaseError.getMessage());
                }
            });
    }

    public void getTasksByDate(String date, RepositoryCallback<List<Task>> callback) {
        crudRepository.getTaskRef().orderByChild("dueDate").equalTo(date)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Task> tasks = new ArrayList<>();
                    
                    for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                        Task task = taskSnapshot.getValue(Task.class);
                        if (task != null) {
                            task.setId(taskSnapshot.getKey());
                            tasks.add(task);
                        }
                    }
                    
                    callback.onSuccess(tasks);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    callback.onError("Lá»—i láº¥y tasks theo ngĂ y: " + databaseError.getMessage());
                }
            });
    }

    public void getCompletedTasks(RepositoryCallback<List<Task>> callback) {
        crudRepository.getTaskRef().orderByChild("isCompleted").equalTo(true)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Task> tasks = new ArrayList<>();
                    
                    for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                        Task task = taskSnapshot.getValue(Task.class);
                        if (task != null) {
                            task.setId(taskSnapshot.getKey());
                            tasks.add(task);
                        }
                    }
                    
                    callback.onSuccess(tasks);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    callback.onError("Lá»—i láº¥y completed tasks: " + databaseError.getMessage());
                }
            });
    }

    public void getIncompleteTasks(RepositoryCallback<List<Task>> callback) {
        crudRepository.getTaskRef().orderByChild("isCompleted").equalTo(false)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Task> tasks = new ArrayList<>();
                    
                    for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                        Task task = taskSnapshot.getValue(Task.class);
                        if (task != null) {
                            task.setId(taskSnapshot.getKey());
                            tasks.add(task);
                        }
                    }
                    
                    callback.onSuccess(tasks);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    callback.onError("Lá»—i láº¥y incomplete tasks: " + databaseError.getMessage());
                }
            });
    }

    public void getTodayTasks(RepositoryCallback<List<Task>> callback) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(calendar.getTime());
        
        getTasksByDate(today, callback);
    }

    public void getOverdueTasks(RepositoryCallback<List<Task>> callback) {
        crudRepository.getTaskRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Task> overdueTasks = new ArrayList<>();
                Calendar today = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayString = dateFormat.format(today.getTime());
                
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null && !task.isCompleted() && task.getDueDate() != null) {
                        task.setId(taskSnapshot.getKey());
                        
                        try {
                            if (task.getDueDate().compareTo(todayString) < 0) {
                                overdueTasks.add(task);
                            }
                        } catch (Exception e) {
                            // Ignore invalid date formats
                        }
                    }
                }
                
                callback.onSuccess(overdueTasks);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Lá»—i láº¥y overdue tasks: " + databaseError.getMessage());
            }
        });
    }

    public void getImportantTasks(RepositoryCallback<List<Task>> callback) {
        crudRepository.getTaskRef().orderByChild("isImportant").equalTo(true)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Task> tasks = new ArrayList<>();
                    
                    for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                        Task task = taskSnapshot.getValue(Task.class);
                        if (task != null) {
                            task.setId(taskSnapshot.getKey());
                            tasks.add(task);
                        }
                    }
                    
                    callback.onSuccess(tasks);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    callback.onError("Lá»—i láº¥y important tasks: " + databaseError.getMessage());
                }
            });
    }
}
