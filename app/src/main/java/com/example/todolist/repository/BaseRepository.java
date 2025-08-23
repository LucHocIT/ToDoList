package com.example.todolist.repository;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;
import java.util.Map;
public abstract class BaseRepository {
    /**
     * Interface for database operation callbacks
     */
    public interface DatabaseCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    /**
     * Interface for repository operation callbacks (alias for backwards compatibility)
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    /**
     * Interface for list operation callbacks
     */
    public interface ListCallback<T> {
        void onSuccess(java.util.List<T> result);
        void onError(String error);
    }
    /**
     * Create or update data in Firebase
     * @param reference Database reference
     * @param data Data to save
     * @param callback Callback for result
     */
    protected void saveData(DatabaseReference reference, Map<String, Object> data, DatabaseCallback<Boolean> callback) {
        reference.updateChildren(data)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Failed to save data: " + e.getMessage());
                    }
                });
    }
    /**
     * Read single data from Firebase
     * @param reference Database reference
     * @param callback Callback for result
     */
    protected void readData(DatabaseReference reference, DatabaseCallback<DataSnapshot> callback) {
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (callback != null) {
                    callback.onSuccess(snapshot);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError("Failed to read data: " + error.getMessage());
                }
            }
        });
    }
    /**
     * Delete data from Firebase
     * @param reference Database reference
     * @param callback Callback for result
     */
    protected void deleteData(DatabaseReference reference, DatabaseCallback<Boolean> callback) {
        reference.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError("Failed to delete data: " + e.getMessage());
                    }
                });
    }
    /**
     * Add real-time listener to Firebase reference
     * @param reference Database reference
     * @param callback Callback for data changes
     * @return ValueEventListener for later removal
     */
    protected ValueEventListener addRealtimeListener(DatabaseReference reference, DatabaseCallback<DataSnapshot> callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (callback != null) {
                    callback.onSuccess(snapshot);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError("Listener cancelled: " + error.getMessage());
                }
            }
        };
        reference.addValueEventListener(listener);
        return listener;
    }
    /**
     * Remove listener from Firebase reference
     * @param reference Database reference
     * @param listener Listener to remove
     */
    protected void removeListener(DatabaseReference reference, ValueEventListener listener) {
        if (reference != null && listener != null) {
            reference.removeEventListener(listener);
        }
    }
    /**
     * Query data with filters
     * @param query Firebase query
     * @param callback Callback for result
     */
    protected void queryData(Query query, DatabaseCallback<DataSnapshot> callback) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (callback != null) {
                    callback.onSuccess(snapshot);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError("Query failed: " + error.getMessage());
                }
            }
        });
    }
    /**
     * Check if data exists
     * @param reference Database reference
     * @param callback Callback for result
     */
    protected void checkDataExists(DatabaseReference reference, DatabaseCallback<Boolean> callback) {
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (callback != null) {
                    callback.onSuccess(snapshot.exists());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError("Check existence failed: " + error.getMessage());
                }
            }
        });
    }
    /**
     * Handle common Firebase errors
     * @param error DatabaseError
     * @return User-friendly error message
     */
    protected String handleFirebaseError(DatabaseError error) {
        switch (error.getCode()) {
            case DatabaseError.PERMISSION_DENIED:
                return "Không có quyền truy cập dữ liệu";
            case DatabaseError.NETWORK_ERROR:
                return "Lỗi kết nối mạng";
            case DatabaseError.UNAVAILABLE:
                return "Dịch vụ tạm thời không khả dụng";
            case DatabaseError.OPERATION_FAILED:
                return "Thao tác thất bại";
            default:
                return "Lỗi không xác định: " + error.getMessage();
        }
    }
}
