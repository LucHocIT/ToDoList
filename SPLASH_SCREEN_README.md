# 🎨 Splash Screen - ToDoList App

## Tổng quan
Splash Screen đẹp mắt với hiệu ứng animation tuyệt vời khi khởi động ứng dụng ToDoList.

## ✨ Các tính năng nổi bật

### 1. **Gradient Background** 
- Background chuyển màu gradient từ tím (#667eea) sang tím đậm (#764ba2)
- Tạo cảm giác hiện đại và chuyên nghiệp

### 2. **Hiệu ứng vòng tròn lan tỏa (Ripple Circles)**
- 3 vòng tròn đồng tâm với độ mờ khác nhau
- Animation scale và fade-in tuần tự
- Hiệu ứng pulse (nhấp nháy nhẹ) sau khi xuất hiện hoàn toàn

### 3. **Logo Animation**
- Logo app (avatarapp.jpg) xuất hiện với hiệu ứng:
  - **Scale**: từ 0 lên 1.15 rồi về 1.0 (bounce effect)
  - **Rotation**: xoay nhẹ từ -5° về 0° 
  - **Fade-in**: từ trong suốt đến hiển thị hoàn toàn
- Logo nằm trong vòng tròn trắng với shadow đẹp mắt

### 4. **Shadow Effect**
- Shadow mờ dưới logo tạo hiệu ứng nổi 3D
- Radial gradient từ đen đến trong suốt

### 5. **Text Animation**
- Tên app và tagline xuất hiện với hiệu ứng:
  - Translate từ dưới lên
  - Fade-in mượt mà
- Font chữ đẹp với letter-spacing tối ưu

### 6. **Loading Indicator**
- ProgressBar tròn với màu trắng
- Text "Đang tải..." phía dưới
- Xuất hiện sau khi các animation chính hoàn thành

## 🎬 Timeline Animation

```
0ms    : Splash screen xuất hiện
0ms    : Circle outer bắt đầu animation
150ms  : Circle middle bắt đầu animation  
200ms  : Logo và shadow bắt đầu animation
300ms  : Circle inner bắt đầu animation
600ms  : App name xuất hiện
800ms  : Tagline xuất hiện
1200ms : Loading indicator xuất hiện
1500ms : Pulse animation bắt đầu cho các circles
3000ms : Chuyển sang MainActivity
```

## 📁 Cấu trúc Files

### Java
- `SplashActivity.java` - Logic và animation controller

### Layout
- `activity_splash.xml` - Giao diện splash screen

### Drawables
- `splash_background.xml` - Gradient background
- `splash_circle_outer.xml` - Vòng tròn ngoài cùng
- `splash_circle_middle.xml` - Vòng tròn giữa
- `splash_circle_inner.xml` - Vòng tròn trong
- `splash_logo_shadow.xml` - Shadow cho logo
- `splash_logo_background.xml` - Background trắng cho logo

### Animations
- `splash_logo_animation.xml` - Animation cho logo
- `splash_text_animation.xml` - Animation cho text
- `splash_circle_animation.xml` - Animation cho circles
- `splash_loading_animation.xml` - Animation cho loading
- `splash_pulse_animation.xml` - Pulse effect

### Theme
- `SplashTheme` trong `themes.xml` - Theme fullscreen

## 🎨 Màu sắc sử dụng

| Element | Color Code | Description |
|---------|-----------|-------------|
| Background Start | #667eea | Tím nhạt |
| Background Center | #764ba2 | Tím đậm |
| Circle Outer | #15FFFFFF | Trắng 15% opacity |
| Circle Middle | #25FFFFFF | Trắng 25% opacity |
| Circle Inner | #35FFFFFF | Trắng 35% opacity |
| Logo Background | #FFFFFF | Trắng 100% |
| Text Color | #FFFFFF | Trắng 100% |
| Tagline Color | #B3FFFFFF | Trắng 70% opacity |

## ⚙️ Tùy chỉnh

### Thay đổi thời gian hiển thị
Trong `SplashActivity.java`, thay đổi:
```java
private static final int SPLASH_DURATION = 3000; // 3 giây
```

### Thay đổi màu background
Trong `splash_background.xml`:
```xml
<gradient
    android:startColor="#YOUR_COLOR"
    android:centerColor="#YOUR_COLOR"
    android:endColor="#YOUR_COLOR" />
```

### Tắt animation nào đó
Trong `SplashActivity.java`, comment out dòng tương ứng trong method `startAnimations()`.

## 🚀 Cách hoạt động

1. **SplashActivity** được đặt làm LAUNCHER trong AndroidManifest
2. Khi app khởi động, SplashActivity hiển thị đầu tiên
3. Các animation được trigger tuần tự theo timeline
4. Sau 3 giây, tự động chuyển sang MainActivity với fade transition
5. SplashActivity được finish() để giải phóng bộ nhớ

## 📱 Tối ưu hóa

- Sử dụng ObjectAnimator thay vì XML animation cho performance tốt hơn
- Logo được cache để tránh load lại
- Disable back button để tránh người dùng quay lại splash
- Sử dụng Handler thay vì Thread để tránh memory leak
- Window background được set trong theme để hiển thị ngay lập tức

## 🎯 Best Practices

✅ Fullscreen mode cho trải nghiệm tốt nhất
✅ Transparent status bar và navigation bar
✅ Portrait orientation lock
✅ Disable back button
✅ Smooth transitions giữa các activities
✅ Memory-efficient animations

---

**Tạo bởi**: ToDoList Development Team
**Phiên bản**: 1.0
**Ngày cập nhật**: 15/10/2025
