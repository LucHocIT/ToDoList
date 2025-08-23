package com.example.todolist.callback;
import com.example.todolist.model.Task;
import java.util.List;
public interface TaskOperationCallback {
    void onSuccess();
    void onError(String error);
    interface TaskListCallback {
        void onSuccess(List<Task> tasks);
        void onError(String error);
    }
    interface TaskCallback {
        void onSuccess(Task task);
        void onError(String error);
    }
}
