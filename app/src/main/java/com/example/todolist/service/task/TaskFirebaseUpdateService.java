package com.example.todolist.service.task;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.todolist.model.Task;
import com.example.todolist.repository.BaseRepository;

import java.util.List;

/**
 * Service để xử lý Firebase update với độ trễ và priority
 */
public class TaskFirebaseUpdateService {
    
    private Handler firebaseUpdateHandler;
    private Runnable pendingFirebaseUpdate;
    private long lastLocalUpdateTime;
    private static final long LOCAL_UPDATE_PRIORITY_WINDOW = 1000;
    private static final long FIREBASE_UPDATE_DELAY = 500;
    
    public interface FirebaseUpdateListener {
        void onFirebaseTasksUpdated(List<Task> tasks);
    }
    
    public TaskFirebaseUpdateService() {
        this.firebaseUpdateHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Xử lý Firebase task update với delay
     */
    public void handleFirebaseTasksUpdate(List<Task> tasks, FirebaseUpdateListener listener) {
        if (pendingFirebaseUpdate != null) {
            firebaseUpdateHandler.removeCallbacks(pendingFirebaseUpdate);
        }
        
        pendingFirebaseUpdate = () -> {
            long timeSinceLastLocalUpdate = System.currentTimeMillis() - lastLocalUpdateTime;
            if (timeSinceLastLocalUpdate < LOCAL_UPDATE_PRIORITY_WINDOW) {
                pendingFirebaseUpdate = null;
                return;
            }
            
            if (listener != null) {
                listener.onFirebaseTasksUpdated(tasks);
            }
            pendingFirebaseUpdate = null;
        };
        
        firebaseUpdateHandler.postDelayed(pendingFirebaseUpdate, FIREBASE_UPDATE_DELAY);
    }
    
    /**
     * Cancel pending Firebase updates
     */
    public void cancelPendingFirebaseUpdates() {
        if (pendingFirebaseUpdate != null) {
            firebaseUpdateHandler.removeCallbacks(pendingFirebaseUpdate);
            pendingFirebaseUpdate = null;
        }
    }
    
    /**
     * Update timestamp cho local update
     */
    public void markLocalUpdate() {
        lastLocalUpdateTime = System.currentTimeMillis();
        cancelPendingFirebaseUpdates();
    }
}
