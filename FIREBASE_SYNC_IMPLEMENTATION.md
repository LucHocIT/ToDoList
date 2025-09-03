# Firebase Sync Logic Implementation

## Tổng quan

Đã triển khai logic Firebase theo yêu cầu với 3 trạng thái khác nhau:

### 1. Chưa đăng nhập
- **Hành vi**: Lưu dữ liệu vào SQLite local như hiện tại
- **Không có**: Firebase sync, cache vẫn hoạt động cho UI
- **File liên quan**: `TaskService.java`, `TaskRepository.java`

### 2. Đã đăng nhập nhưng chưa bật sync
- **Hành vi**: 
  - Lưu vào SQLite local
  - Lưu vào cache (optimistic UI)
  - **KHÔNG** đồng bộ lên Firebase
- **Quản lý**: `AuthManager.isSyncEnabled() = false`
- **Lợi ích**: User có thể sử dụng offline hoàn toàn

### 3. Đã đăng nhập và đã bật sync
- **Hành vi**:
  - Lưu vào cache (optimistic UI)
  - Lưu vào SQLite local
  - Đồng bộ lên Firebase ngay lập tức
- **Quản lý**: `AuthManager.shouldSyncToFirebase() = true`
- **Lợi ích**: Optimistic UI + backup cloud

## Các file đã thay đổi

### 1. AuthManager.java
- Thêm `KEY_SYNC_ENABLED` để quản lý trạng thái sync
- Thêm methods: `isSyncEnabled()`, `setSyncEnabled()`, `shouldSyncToFirebase()`
- Tự động tắt sync khi đăng xuất

### 2. FirebaseSyncManager.java (Mới)
- Quản lý tất cả operations với Firebase Firestore
- Methods: `addTaskToFirebase()`, `updateTaskInFirebase()`, `deleteTaskFromFirebase()`
- Chỉ hoạt động khi `shouldSyncToFirebase() = true`
- Xử lý lỗi gracefully (nếu Firebase fail thì local vẫn OK)

### 3. TaskService.java
- Cập nhật logic cho `addTask()`, `updateTask()`, `deleteTask()`
- Luôn lưu local trước, sau đó sync Firebase (nếu enabled)
- Optimistic UI: cache được update ngay lập tức
- Thêm `syncAllTasksToFirebase()` để sync hàng loạt khi bật sync
- Thêm `loadAndMergeFromFirebase()` để merge data khi cần

### 4. SyncAccountActivity.java
- Tích hợp với logic mới
- Switch để bật/tắt sync
- Tự động sync tất cả tasks khi bật sync lần đầu
- UI cập nhật theo trạng thái login và sync

## Flow hoạt động

### Khi thêm task mới:
```
1. User tạo task
2. Lưu vào cache (UI update ngay)
3. Lưu vào SQLite
   - Thành công: Kiểm tra sync setting
     - Nếu shouldSyncToFirebase() = true: Sync lên Firebase
     - Nếu false: Dừng ở local
   - Thất bại: Rollback cache
```

### Khi bật sync:
```
1. User bật switch sync trong SyncAccountActivity
2. AuthManager.setSyncEnabled(true)
3. Tự động gọi TaskService.syncAllTasksToFirebase()
4. Upload tất cả local tasks lên Firebase
5. Các operation sau đó sẽ sync real-time
```

### Khi đăng xuất:
```
1. AuthManager.signOut()
2. Tự động setSyncEnabled(false)
3. Các operation sau chỉ lưu local
```

## Ưu điểm của implementation này

1. **Optimistic UI**: User thấy thay đổi ngay lập tức
2. **Offline-first**: App hoạt động tốt ngay cả khi không có mạng
3. **Graceful degradation**: Nếu Firebase fail thì local vẫn OK
4. **User control**: User tự quyết định khi nào muốn sync
5. **Performance**: Không có blocking operations
6. **Consistency**: SQLite luôn là source of truth

## Cách test

1. **Test chưa login**: Tạo task -> chỉ có trong SQLite
2. **Test login chưa sync**: Đăng nhập -> tạo task -> chỉ có trong SQLite + cache
3. **Test login và sync**: Bật sync -> tạo task -> có trong SQLite + Firebase
4. **Test bật sync**: Bật sync với existing tasks -> tất cả được upload lên Firebase

## Tối ưu hoá trong tương lai

1. Conflict resolution khi merge Firebase data
2. Batch sync để giảm API calls
3. Retry mechanism cho Firebase operations
4. Progressive sync (sync từ từ thay vì một lúc)
5. Sync status indicators trong UI
