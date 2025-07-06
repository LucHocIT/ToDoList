# ToDoList - Ứng dụng quản lý công việc

Ứng dụng To-Do List được phát triển bằng Android Java, giúp người dùng quản lý công việc hàng ngày một cách hiệu quả.

## Tính năng chính

### Quản lý nhiệm vụ

- Tạo, chỉnh sửa, xóa nhiệm vụ
- Đánh dấu nhiệm vụ hoàn thành
- Phân loại nhiệm vụ theo danh mục (Công việc, Cá nhân, Yêu thích)
- Đặt độ ưu tiên cho nhiệm vụ (Thấp, Trung bình, Cao)
- Tìm kiếm nhiệm vụ theo từ khóa

### Thời gian và nhắc nhở

- Đặt ngày và giờ đến hạn
- Tạo nhắc nhở (5 phút, 10 phút, 15 phút, 30 phút, 1 giờ, 1 ngày trước)
- Lặp lại nhiệm vụ (Hàng ngày, hàng tuần, hàng tháng, hàng năm)
- Xem nhiệm vụ theo thời gian (Quá hạn, Hôm nay, Tương lai)

### Giao diện và tùy chỉnh

- Hỗ trợ đa ngôn ngữ (Tiếng Việt, English)
- Chế độ sáng/tối
- Nhiều chủ đề màu sắc
- Giao diện Material Design

### Tiện ích

- Widget lịch (3x4) hiển thị trên màn hình chính
- Widget mini (1x1) để thêm nhiệm vụ nhanh
- Thống kê nhiệm vụ đã hoàn thành
- Sao lưu và khôi phục dữ liệu

## Yêu cầu hệ thống

- Android 5.0 (API level 21) trở lên
- Bộ nhớ: 50MB trống

## Cài đặt

1. Clone repository:

```bash
git clone https://github.com/LucHocIT/ToDoList.git
```

2. Mở project trong Android Studio

3. Sync project với Gradle files

4. Chạy ứng dụng trên thiết bị hoặc emulator


## Công nghệ sử dụng

- **Language**: Java
- **Database**: Room (SQLite)
- **Architecture**: MVVM pattern
- **UI**: Material Design Components
- **Notification**: AlarmManager
- **Widget**: App Widget Provider

## Phiên bản

**Phiên bản hiện tại**: 1.0

### Tính năng chính v1.0

- Quản lý nhiệm vụ cơ bản
- Phân loại theo danh mục
- Nhắc nhở và lặp lại
- Đa ngôn ngữ
- Widget màn hình chính


## Đóng góp

Nếu bạn muốn đóng góp cho dự án:

1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit thay đổi (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## Hướng dẫn sử dụng

### Tạo nhiệm vụ mới

1. Nhấn nút "+" ở màn hình chính
2. Nhập tiêu đề nhiệm vụ
3. Chọn danh mục, thời gian, độ ưu tiên
4. Nhấn "Lưu"

### Quản lý danh mục

1. Vào Menu > Quản lý danh mục
2. Nhấn "+" để tạo danh mục mới
3. Kéo thả để sắp xếp thứ tự

### Cài đặt thông báo

1. Vào Settings > Thông báo
2. Bật/tắt thông báo
3. Chọn thời gian nhắc nhở mặc định
4. Chọn âm thanh thông báo

### Thay đổi ngôn ngữ

1. Vào Settings > Ngôn ngữ
2. Chọn ngôn ngữ mong muốn
3. Khởi động lại ứng dụng
