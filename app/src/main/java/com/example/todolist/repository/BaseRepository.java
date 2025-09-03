package com.example.todolist.repository;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseRepository {

    public interface DatabaseCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public interface Callback {
        void onSuccess();
        void onError(String error);
    }

    public interface ListCallback<T> {
        void onSuccess(List<T> result);
        void onError(String error);
    }
    
    protected ExecutorService executorService;
    protected Handler mainHandler;
    
    public BaseRepository() {
        executorService = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    protected void executeAsync(Runnable task) {
        executorService.execute(task);
    }
    
    protected void runOnMainThread(Runnable task) {
        mainHandler.post(task);
    }
    
    protected void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
