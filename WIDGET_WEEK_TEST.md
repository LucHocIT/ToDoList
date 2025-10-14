# Widget Lịch Tuần 4x4 - Hướng Dẫn Test

## Các tính năng đã triển khai:

### 1. Header với nút điều hướng
- ✅ Background ảnh header (sử dụng @drawable/background_calendar)
- ✅ Hiển thị tháng/năm (MM/yyyy như 10/2025)
- ✅ Nút ◀ bên trái để sang tuần trước
- ✅ Nút ▶ bên phải để sang tuần sau
- ✅ Tự động chuyển tháng khi di chuyển giữa các tuần

### 2. Hàng ngày trong tuần
- ✅ Hiển thị S M T W T F S
- ✅ Hiển thị số ngày tương ứng (9-15)
- ✅ Ngày hiện tại màu xanh (#4285F4)
- ✅ Ngày được chọn màu xanh + gạch chân
- ✅ Click được để chọn ngày xem task

### 3. Danh sách task
- ✅ Hiển thị task của ngày được chọn
- ✅ Sử dụng CalendarUtils.isTaskOnDate() giống widget 4x2
- ✅ Tự động loại bỏ ký tự VIP (👑)
- ✅ Hiển thị checkbox, tiêu đề, thời gian
- ✅ Phân biệt task hoàn thành/chưa hoàn thành
- ✅ Empty state khi không có task

## Cách test widget:

### Bước 1: Kết nối thiết bị Android
```bash
adb devices
```

### Bước 2: Cài đặt ứng dụng
```bash
cd E:\Android\ToDoList-1
.\gradlew installDebug
```

### Bước 3: Thêm widget vào home screen
1. Long press trên màn hình home
2. Chọn "Widgets"
3. Tìm "ToDoList"
4. Chọn "Week Calendar 4x4" (hoặc tên widget tương tự)
5. Kéo thả lên màn hình home

### Bước 4: Test các tính năng

#### Test điều hướng tuần:
1. Click nút ◀ (trái) để xem tuần trước
2. Click nút ▶ (phải) để xem tuần sau
3. Kiểm tra xem tháng/năm có tự động chuyển không khi qua tháng mới

#### Test chọn ngày:
1. Click vào các số ngày (9, 10, 11, ...)
2. Kiểm tra xem số đó có được gạch chân màu xanh không
3. Kiểm tra xem danh sách task bên dưới có thay đổi không

#### Test hiển thị task:
1. Tạo task trong app với ngày cụ thể
2. Quay lại widget và chọn ngày đó
3. Kiểm tra xem task có hiển thị không
4. Kiểm tra thời gian task có hiển thị đúng không
5. Kiểm tra checkbox có đúng trạng thái không

## Debug nếu không hoạt động:

### Vấn đề 1: Widget không hiển thị
- Xóa widget và thêm lại
- Restart launcher (hoặc restart thiết bị)
- Check logcat: `adb logcat | grep WeekCalendarWidget`

### Vấn đề 2: Task không hiển thị
- Check xem TaskCache có dữ liệu không
- Check format ngày (phải là dd/MM/yyyy)
- Check logcat cho exception

### Vấn đề 3: Click không hoạt động
- Check PendingIntent có được đăng ký đúng không
- Check AndroidManifest.xml có khai báo widget receiver không
- Xóa và thêm lại widget

## Log debug quan trọng:

### Xem log của widget:
```bash
adb logcat | grep "WeekCalendarWidget\|TaskCache\|CalendarUtils"
```

### Xem tất cả log của app:
```bash
adb logcat | grep "com.example.todolist"
```

### Clear log và xem log mới:
```bash
adb logcat -c
adb logcat | grep "WeekCalendarWidget"
```

## File cần kiểm tra nếu có lỗi:

1. `WeekCalendarWidget.java` - Logic chính của widget
2. `widget_week_calendar_4x4.xml` - Layout của widget
3. `week_calendar_widget_info.xml` - Cấu hình widget
4. `AndroidManifest.xml` - Đăng ký widget receiver
5. `CalendarUtils.java` - Logic lọc task theo ngày

## Ghi chú kỹ thuật:

### Format ngày:
- Widget sử dụng format: `dd/MM/yyyy` (ví dụ: 14/10/2025)
- Phải match với format của Task trong database

### Lọc task:
- Sử dụng `CalendarUtils.isTaskOnDate(task, dateString)`
- Giống logic của widget 4x2 calendar
- Hỗ trợ repeating tasks

### SharedPreferences:
- Lưu `current_year`, `current_month`, `current_week`, `selected_day`
- Key: "week_calendar_widget_prefs"

### PendingIntent request codes:
- Day clicks: 100-106 (cho 7 ngày)
- Prev week: 201
- Next week: 202
