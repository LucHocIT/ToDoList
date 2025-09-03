package com.example.todolist.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.todolist.manager.AuthManager;
import com.example.todolist.manager.FirebaseSyncManager;
import com.example.todolist.service.TaskService;

public class FirebaseMigrationHelper {
    private static final String TAG = "FirebaseMigrationHelper";
    private static final String PREF_NAME = "migration_prefs";
    private static final String KEY_MIGRATED_TO_NEW_SYNC = "migrated_to_new_sync";

    public static void checkAndMigrate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean hasMigrated = prefs.getBoolean(KEY_MIGRATED_TO_NEW_SYNC, false);
        
        if (!hasMigrated) {
            Log.d(TAG, "Starting migration to new Firebase sync logic...");
            performMigration(context);
            prefs.edit().putBoolean(KEY_MIGRATED_TO_NEW_SYNC, true).apply();
            Log.d(TAG, "Migration completed.");
        }
    }

    private static void performMigration(Context context) {
        AuthManager authManager = AuthManager.getInstance();
        authManager.initialize(context);
        if (authManager.isSignedIn()) {
            Log.d(TAG, "User was already logged in, keeping login state");
            Log.d(TAG, "Sync is disabled by default - user can enable in settings");
            authManager.setSyncEnabled(false);
        }
    }

    public static void enableSyncForLoggedInUser(Context context, 
                                                TaskService taskService,
                                                Runnable onSuccess, 
                                                Runnable onError) {
        AuthManager authManager = AuthManager.getInstance();
        
        if (!authManager.isSignedIn()) {
            Log.w(TAG, "User not logged in, cannot enable sync");
            if (onError != null) onError.run();
            return;
        }
        
        authManager.setSyncEnabled(true);
        taskService.syncAllTasksToFirebase(new FirebaseSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Sync enabled and all tasks uploaded: " + message);
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to upload tasks after enabling sync: " + error);
                // Tắt sync lại nếu upload fail
                authManager.setSyncEnabled(false);
                if (onError != null) onError.run();
            }
        });
    }

    public static void resetMigrationState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_MIGRATED_TO_NEW_SYNC).apply();
        Log.d(TAG, "Migration state reset");
    }
}
