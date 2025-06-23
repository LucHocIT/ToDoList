# Todo List Android App

Ứng dụng Todo List được xây dựng bằng Android với Java, có giao diện tiếng Việt và các tính năng quản lý nhiệm vụ cơ bản.

## Tính năng đã hoàn thành

### 🎨 Giao diện
- ✅ Giao diện chính với 3 tab filter: "Tất cả", "Công việc", "Cá nhân"
- ✅ Bottom navigation với 4 mục: Menu, Nhiệm vụ, Lịch, Của tôi
- ✅ Header với nút back và menu
- ✅ Danh sách nhiệm vụ phân chia thành "Trước" và "Đã hoàn thành"
- ✅ Task items có background xanh sky nhạt (#E3F2FD) và bo tròn góc
- ✅ Floating Action Button hình tròn để thêm nhiệm vụ mới

### 📝 Quản lý nhiệm vụ
- ✅ Thêm nhiệm vụ mới qua dialog với giao diện bo tròn
- ✅ Checkbox hình tròn trắng với viền đen, hiển thị dấu tick khi hoàn thành
- ✅ Đánh dấu nhiệm vụ hoàn thành/chưa hoàn thành
- ✅ Hiển thị thời gian và ngày tháng cho từng nhiệm vụ
- ✅ Icon thông báo cho các nhiệm vụ có reminder

### 🔄 Tương tác
- ✅ Swipe từ trái sang phải để hiển thị các action buttons (Star, Calendar, Delete)
- ✅ Swipe chỉ hiển thị một phần nhỏ (120px) thay vì toàn bộ item
- ✅ Click vào nhiệm vụ để mở màn hình chỉnh sửa chi tiết
- ✅ Dialog thêm nhiệm vụ với các icon: Calendar, Time, Share

### 💾 Lưu trữ dữ liệu
- ✅ Sử dụng SQLite với Room Database
- ✅ Model TodoTask với các thuộc tính: title, description, dueDate, dueTime, isCompleted, isImportant, category, reminder, attachments
- ✅ DAO interface với các phương thức CRUD cơ bản
- ✅ Database singleton pattern

### 🏗️ Architecture
- ✅ Activity chính (MainActivity) quản lý danh sách nhiệm vụ
- ✅ Activity chi tiết (TaskDetailActivity) để chỉnh sửa nhiệm vụ
- ✅ RecyclerView Adapter với ViewHolder pattern
- ✅ SwipeToRevealHelper để xử lý gesture vuốt
- ✅ Material Design components

## Cấu trúc dự án

```
app/
├── src/main/
│   ├── java/com/example/todolist/
│   │   ├── MainActivity.java              # Màn hình chính
│   │   ├── TaskDetailActivity.java        # Màn hình chi tiết nhiệm vụ
│   │   ├── adapter/
│   │   │   └── TaskAdapter.java           # Adapter cho RecyclerView
│   │   ├── database/
│   │   │   ├── TodoDatabase.java          # Room database
│   │   │   └── TodoDao.java               # Data Access Object
│   │   ├── model/
│   │   │   └── TodoTask.java              # Model class cho nhiệm vụ
│   │   └── util/
│   │       └── SwipeToRevealHelper.java   # Helper cho swipe gesture
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml          # Layout màn hình chính
│       │   ├── activity_task_detail.xml   # Layout màn hình chi tiết
│       │   ├── item_task.xml              # Layout item nhiệm vụ
│       │   └── dialog_add_task.xml        # Layout dialog thêm nhiệm vụ
│       ├── drawable/
│       │   ├── checkbox_selector.xml      # Checkbox custom
│       │   └── ic_*.xml                   # Các icon vector
│       └── values/
│           ├── colors.xml                 # Màu sắc
│           ├── strings.xml                # Chuỗi text
│           └── themes.xml                 # Theme
```

## Dependencies sử dụng

- Room Database (2.6.1) - SQLite ORM
- RecyclerView (1.3.2) - Danh sách cuộn
- CardView (1.0.0) - Card layout
- Material Design Components - UI components

## Cách chạy ứng dụng

1. Mở project trong Android Studio
2. Sync Gradle files
3. Build và chạy trên device/emulator
4. Minimum SDK: 35

## Màn hình chính

- **Tab filters**: Lọc nhiệm vụ theo loại
- **Danh sách nhiệm vụ**: Hiển thị nhiệm vụ chưa hoàn thành và đã hoàn thành
- **FAB**: Thêm nhiệm vụ mới
- **Bottom navigation**: Điều hướng giữa các màn hình

## Tương tác

- **Tap checkbox**: Đánh dấu hoàn thành/chưa hoàn thành
- **Tap item**: Mở màn hình chỉnh sửa
- **Swipe left**: Hiển thị action buttons (Star/Calendar/Delete)
- **Tap FAB**: Mở dialog thêm nhiệm vụ

## Ghi chú

- Ứng dụng được thiết kế theo Material Design
- Giao diện hoàn toàn bằng tiếng Việt
- Sử dụng SQLite để lưu trữ dữ liệu local
- Có data mẫu khi khởi chạy lần đầu
