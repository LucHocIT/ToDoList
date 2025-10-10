# 🔔 Báo Cáo Sửa Lỗi Hệ Thống Thông Báo

## 📋 Các Vấn Đề Đã Tìm Thấy và Sửa

### 1. ❌ MinSDK Quá Cao
**Vấn đề:** `minSdk = 35` trong `build.gradle.kts` quá cao, gây lỗi không tương thích với nhiều thiết bị Android.

**Giải pháp:** ✅ Đã giảm xuống `minSdk = 26` (Android 8.0 Oreo)
- Hỗ trợ rộng rãi hơn cho các thiết bị
- Vẫn hỗ trợ đầy đủ tính năng AlarmManager và Notification

**File đã sửa:** `app/build.gradle.kts`

---

### 2. ❌ Intent Action Không Chính Xác
**Vấn đề:** Intent action trong `NotificationReceiver` không khớp với khai báo trong `AndroidManifest.xml`

**Trước:**
- NotificationReceiver.java: `ACTION_REMINDER = "reminder_notification"`
- AndroidManifest.xml: `<action android:name="reminder_notification" />`

**Sau:** ✅
- NotificationReceiver.java: `ACTION_REMINDER = "com.example.todolist.action.REMINDER_NOTIFICATION"`
- AndroidManifest.xml: `<action android:name="com.example.todolist.action.REMINDER_NOTIFICATION" />`

**Lý do:** Android yêu cầu action name phải có package prefix để tránh xung đột và đảm bảo BroadcastReceiver nhận được Intent đúng cách.

**Files đã sửa:**
- `app/src/main/java/com/example/todolist/notification/NotificationReceiver.java`
- `app/src/main/AndroidManifest.xml`

---

### 3. 📊 Thêm Logging Chi Tiết
**Vấn đề:** Không có log để debug khi thông báo không hoạt động

**Giải pháp:** ✅ Đã thêm logging chi tiết vào:
1. **ReminderScheduler.java:**
   - Log khi schedule reminder/due notification
   - Log thời gian trigger
   - Log khi thời gian đã qua
   - Log request code của PendingIntent

2. **NotificationReceiver.java:**
   - Log khi nhận Intent
   - Log action và taskId
   - Log khi hiển thị notification
   - Log khi task đã completed

3. **NotificationHelper.java:**
   - Log khi tạo notification
   - Log khi notification disabled
   - Log khi notification được hiển thị thành công

**Lợi ích:** Giúp debug dễ dàng hơn thông qua Logcat

---

## 🔍 Các Vấn Đề Có Thể Còn Tồn Tại

### 1. ⚠️ Quyền Thông Báo (Android 13+)
Trên Android 13 (API 33) trở lên, cần yêu cầu quyền `POST_NOTIFICATIONS` runtime.

**Kiểm tra:** Đảm bảo app yêu cầu quyền này khi chạy lần đầu.

### 2. ⚠️ Quyền Schedule Exact Alarm (Android 12+)
Trên Android 12 (API 31) trở lên, cần quyền đặc biệt `SCHEDULE_EXACT_ALARM`.

**Kiểm tra:** Đảm bảo user đã cấp quyền này trong Settings.

### 3. ⚠️ Battery Optimization
Nếu app bị tối ưu hóa pin, alarm có thể không kích hoạt đúng lúc.

**Giải pháp:** Yêu cầu user tắt battery optimization cho app.

---

## 🧪 Hướng Dẫn Test

### Bước 1: Clean và Rebuild Project
```bash
# Trên Windows (cmd)
cd e:\Android\ToDoList-1
gradlew clean
gradlew assembleDebug
```

### Bước 2: Cài Đặt App Lên Thiết Bị
- Uninstall app cũ nếu có
- Install app mới từ `app/build/outputs/apk/debug/app-debug.apk`

### Bước 3: Cấp Quyền Cần Thiết
1. **Quyền Thông Báo:**
   - Settings → Apps → ToDoList → Notifications → Allow

2. **Quyền Alarms & Reminders (Android 12+):**
   - Settings → Apps → ToDoList → Alarms & reminders → Allow

3. **Battery Optimization:**
   - Settings → Apps → ToDoList → Battery → Unrestricted

### Bước 4: Tạo Task Test
1. Tạo task mới với:
   - Title: "Test Notification"
   - Due Date: Ngày hôm nay
   - Due Time: 2-3 phút sau thời gian hiện tại
   - Reminder: "5 phút trước"
   - Bật "Nhắc nhở"

2. Save task

### Bước 5: Kiểm Tra Logcat
Mở Android Studio Logcat và filter:
```
tag:ReminderScheduler|NotificationReceiver|NotificationHelper
```

Bạn sẽ thấy các log như:
```
D/ReminderScheduler: scheduleTaskReminder called for task: Test Notification
D/ReminderScheduler: Task details - dueDate: 10/10/2025, dueTime: 14:30, reminderType: 5 phút trước
D/ReminderScheduler: Minutes before due: 5
D/ReminderScheduler: Scheduling reminder notification at: Thu Oct 10 14:25:00 GMT+07:00 2025
D/ReminderScheduler: Creating PendingIntent with requestCode: 123456789
D/ReminderScheduler: Alarm set using setExactAndAllowWhileIdle
```

### Bước 6: Đợi Notification
- Đợi đến thời gian nhắc nhở
- Kiểm tra xem notification có xuất hiện không
- Kiểm tra Logcat xem có log từ NotificationReceiver không

---

## 🐛 Debug Nếu Vẫn Không Hoạt Động

### 1. Kiểm tra Notification Settings
```java
// Trong SettingsActivity hoặc MainActivity, thêm debug info
boolean notificationsEnabled = SettingsManager.isNotificationsEnabled(this);
Log.d("DEBUG", "Notifications enabled in settings: " + notificationsEnabled);
```

### 2. Kiểm tra System Notification Permission
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
        != PackageManager.PERMISSION_GRANTED) {
        Log.e("DEBUG", "POST_NOTIFICATIONS permission not granted!");
    }
}
```

### 3. Kiểm tra AlarmManager Can Schedule
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    if (!alarmManager.canScheduleExactAlarms()) {
        Log.e("DEBUG", "Cannot schedule exact alarms!");
        // Mở settings để user cấp quyền
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        startActivity(intent);
    }
}
```

### 4. Test Thủ Công với NotificationDebugHelper
Nếu có file `NotificationDebugHelper.java`, sử dụng nó để test trực tiếp:
```java
NotificationDebugHelper debugHelper = new NotificationDebugHelper(context);
debugHelper.testImmediateNotification(); // Test notification ngay lập tức
```

---

## 📝 Tóm Tắt Thay Đổi Code

### Files Đã Sửa:

1. **app/build.gradle.kts**
   - minSdk: 35 → 26

2. **app/src/main/AndroidManifest.xml**
   - Intent filter actions đã được cập nhật với full package name

3. **NotificationReceiver.java**
   - ACTION constants đã được cập nhật
   - Thêm logging chi tiết

4. **ReminderScheduler.java**
   - Thêm logging chi tiết cho việc schedule alarm

5. **NotificationHelper.java**
   - Thêm logging chi tiết cho việc hiển thị notification

---

## ✅ Checklist Trước Khi Test

- [ ] Clean và rebuild project
- [ ] Uninstall app cũ
- [ ] Install app mới
- [ ] Cấp quyền POST_NOTIFICATIONS (Android 13+)
- [ ] Cấp quyền SCHEDULE_EXACT_ALARM (Android 12+)
- [ ] Tắt Battery Optimization
- [ ] Bật Notifications trong app settings
- [ ] Tạo task test với thời gian gần
- [ ] Mở Logcat với filter phù hợp
- [ ] Đợi và kiểm tra notification

---

## 🎯 Kết Quả Mong Đợi

Sau khi sửa lỗi, hệ thống thông báo sẽ:

1. ✅ Schedule alarm chính xác cho reminder và due notification
2. ✅ BroadcastReceiver nhận được Intent đúng lúc
3. ✅ Notification hiển thị với nội dung đầy đủ
4. ✅ Log chi tiết giúp debug nếu có vấn đề
5. ✅ Hoạt động trên nhiều phiên bản Android (từ 8.0+)

---

## 📞 Nếu Vẫn Gặp Vấn Đề

Kiểm tra Logcat với filter: `ReminderScheduler|NotificationReceiver|NotificationHelper`

Tìm kiếm các log error hoặc log cho thấy:
- Task bị skip vì completed
- Thời gian đã qua (in the past)
- Notifications disabled
- Permission denied

Gửi log để được hỗ trợ thêm!
