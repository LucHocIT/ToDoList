package com.example.todolist.service;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.repository.CategoryRepository;
import com.google.firebase.database.FirebaseDatabase;

public class SyncService {
    public interface SyncStatusListener {
        void onSyncStatusChanged(boolean isOnline);
        void onSyncCompleted();
        void onSyncError(String error);
    }
    private static final String TAG = "SyncService";
    private Context context;
    private TaskRepository taskRepository;
    private CategoryRepository categoryRepository;
    private SyncStatusListener listener;
    private ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
    private Handler mainHandler;
    private boolean isOnline = false;
    private boolean isSyncing = false;
    public SyncService(Context context, SyncStatusListener listener) {
        this.context = context;
        this.listener = listener;
        this.taskRepository = new TaskRepository();
        this.categoryRepository = new CategoryRepository();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initializeNetworkMonitoring();
        checkInitialConnectivity();
    }

    private void initializeNetworkMonitoring() {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new NetworkCallback();
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        try {
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        } catch (Exception e) {
        }
    }

    private void checkInitialConnectivity() {
        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            boolean connected = capabilities != null && 
                               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            updateConnectionStatus(connected);
        } catch (Exception e) {
            updateConnectionStatus(false);
        }
    }

    private void updateConnectionStatus(boolean connected) {
        boolean wasOnline = isOnline;
        isOnline = connected;
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onSyncStatusChanged(isOnline);
            }
        });

        if (connected && !wasOnline) {
            performSyncWhenOnline();
        }
    }

    private void performSyncWhenOnline() {
        if (isSyncing) return;
        isSyncing = true;
        // Force Firebase to sync offline changes
        FirebaseDatabase.getInstance().goOnline();
        // Small delay to allow Firebase to establish connection
        mainHandler.postDelayed(() -> {
            isSyncing = false;
            if (listener != null) {
                listener.onSyncCompleted();
            }
        }, 2000);
    }

    public void forceSync() {
        if (!isOnline) {
            if (listener != null) {
                listener.onSyncError("Không có kết nối mạng");
            }
            return;
        }
        if (isSyncing) {
            if (listener != null) {
                listener.onSyncError("Đang sync, vui lòng đợi");
            }
            return;
        }
        performSyncWhenOnline();
    }

    public boolean isOnline() {
        return isOnline;
    }

    public boolean isSyncing() {
        return isSyncing;
    }

    public void handleOfflineMode() {
        FirebaseDatabase.getInstance().goOffline();
    }

    public void handleOnlineMode() {
        FirebaseDatabase.getInstance().goOnline();
    }

    public void testFirebaseConnection(BaseRepository.DatabaseCallback<Boolean> callback) {
        if (!isOnline) {
            callback.onError("Không có kết nối mạng");
            return;
        }
        taskRepository.getAllTasks(new BaseRepository.ListCallback<com.example.todolist.model.Task>() {
            @Override
            public void onSuccess(java.util.List<com.example.todolist.model.Task> result) {
                callback.onSuccess(true);
            }
            @Override
            public void onError(String error) {
                callback.onError("Lỗi kết nối Firebase: " + error);
            }
        });
    }

    public void getSyncStats(SyncStatsCallback callback) {
        SyncStats stats = new SyncStats();
        stats.isOnline = isOnline;
        stats.isSyncing = isSyncing;
        stats.lastSyncTime = System.currentTimeMillis();
        if (callback != null) {
            callback.onStatsRetrieved(stats);
        }
    }

    public void cleanup() {
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
        }
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            updateConnectionStatus(true);
        }
        @Override
        public void onLost(@NonNull Network network) {
            updateConnectionStatus(false);
        }
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            updateConnectionStatus(hasInternet && hasValidated);
        }
    }

    public static class SyncStats {
        public boolean isOnline;
        public boolean isSyncing;
        public long lastSyncTime;
        public int pendingUploads;
        public int pendingDownloads;
    }

    public interface SyncStatsCallback {
        void onStatsRetrieved(SyncStats stats);
    }
}
