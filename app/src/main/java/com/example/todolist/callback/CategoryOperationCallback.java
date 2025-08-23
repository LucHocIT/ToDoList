package com.example.todolist.callback;
import com.example.todolist.model.Category;
import java.util.List;
public interface CategoryOperationCallback {
    void onSuccess();
    void onError(String error);
    interface CategoryListCallback {
        void onSuccess(List<Category> categories);
        void onError(String error);
    }
    interface CategoryCallback {
        void onSuccess(Category category);
        void onError(String error);
    }
}
