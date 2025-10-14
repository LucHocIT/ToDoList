package com.example.todolist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.todolist.widget.CountdownWidgetConfigActivity;

public class WidgetsGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widgets_guide);
        
        setupToolbar();
        setupWidgetCards();
        setupInstructionButton();
    }

    private void setupToolbar() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupWidgetCards() {
        // Calendar Widget Card
        CardView calendarCard = findViewById(R.id.card_calendar_widget);
        if (calendarCard != null) {
            calendarCard.setOnClickListener(v -> showCalendarWidgetInstructions());
        }

        // Countdown Widget Card
        CardView countdownCard = findViewById(R.id.card_countdown_widget);
        if (countdownCard != null) {
            countdownCard.setOnClickListener(v -> {
                // Mở activity cấu hình countdown widget để người dùng tạo thử
                Intent intent = new Intent(this, CountdownWidgetConfigActivity.class);
                startActivity(intent);
            });
        }

        // Mini Quick Add Widget Card
        CardView miniCard = findViewById(R.id.card_mini_widget);
        if (miniCard != null) {
            miniCard.setOnClickListener(v -> showMiniWidgetInstructions());
        }

        // Calendar Task Widget Card
        CardView calendarTaskCard = findViewById(R.id.card_calendar_task_widget);
        if (calendarTaskCard != null) {
            calendarTaskCard.setOnClickListener(v -> showCalendarTaskWidgetInstructions());
        }
    }

    private void setupInstructionButton() {
        LinearLayout instructionButton = findViewById(R.id.btn_add_widget_instruction);
        if (instructionButton != null) {
            instructionButton.setOnClickListener(v -> openWidgetSettings());
        }
    }

    private void showCalendarWidgetInstructions() {
        showWidgetDetailDialog(
            "📅 Tiện ích Lịch Mini",
            "Widget lịch nhỏ gọn hiển thị tháng hiện tại với các công việc của bạn.\n\n" +
            "✨ Tính năng:\n" +
            "• Xem lịch tháng hiện tại\n" +
            "• Đếm số công việc trong ngày\n" +
            "• Chuyển đổi tháng dễ dàng\n" +
            "• Nhấn vào ngày để xem chi tiết\n\n" +
            "📐 Kích thước: 4x3 hoặc 5x4\n\n" +
            "💡 Mẹo: Đặt widget này ở màn hình chính để luôn theo dõi lịch trình!",
            "ic_calendar"
        );
    }

    private void showMiniWidgetInstructions() {
        showWidgetDetailDialog(
            "➕ Tiện ích Thêm Nhanh",
            "Widget nhỏ gọn 1x1 giúp bạn thêm công việc siêu nhanh!\n\n" +
            "✨ Tính năng:\n" +
            "• Hiển thị số công việc hiện tại\n" +
            "• Thêm việc mới chỉ với 1 chạm\n" +
            "• Thiết kế gradient đẹp mắt\n" +
            "• Cập nhật thời gian thực\n\n" +
            "📐 Kích thước: 1x1 (siêu nhỏ gọn)\n\n" +
            "💡 Mẹo: Đặt nhiều widget này ở các màn hình khác nhau để truy cập nhanh!",
            "ic_add"
        );
    }

    private void showCalendarTaskWidgetInstructions() {
        showWidgetDetailDialog(
            "📋 Tiện ích Lịch Công Việc",
            "Widget lịch đầy đủ với danh sách công việc chi tiết theo ngày.\n\n" +
            "✨ Tính năng:\n" +
            "• Xem lịch tháng với nhiều chi tiết\n" +
            "• Danh sách công việc theo ngày được chọn\n" +
            "• Thêm việc mới trực tiếp từ widget\n" +
            "• Đánh dấu hoàn thành nhanh\n" +
            "• Xem giờ và độ ưu tiên của công việc\n\n" +
            "📐 Kích thước: 4x2 hoặc 5x3\n\n" +
            "💡 Mẹo: Widget này phù hợp cho màn hình chính khi bạn muốn xem tổng quan!",
            "ic_calendar_task"
        );
    }

    private void showWidgetDetailDialog(String title, String description, String iconName) {
        // Tạo dialog chi tiết về widget
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(this);
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_widget_detail, null);
        
        TextView titleView = dialogView.findViewById(R.id.widget_detail_title);
        TextView descView = dialogView.findViewById(R.id.widget_detail_description);
        
        if (titleView != null) titleView.setText(title);
        if (descView != null) descView.setText(description);
        
        builder.setView(dialogView)
               .setPositiveButton("Đã hiểu", (dialog, which) -> dialog.dismiss())
               .setNeutralButton("Thêm Widget", (dialog, which) -> {
                   openWidgetSettings();
                   dialog.dismiss();
               });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void openWidgetSettings() {
        // Mở màn hình widget picker của hệ thống
        try {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            // Fallback: Mở cài đặt tổng quát
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                // Không thể mở cài đặt
            }
        }
    }

    private void showAddWidgetTutorial() {
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(this);
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_widget_tutorial, null);
        
        builder.setView(dialogView)
               .setPositiveButton("Đã hiểu", (dialog, which) -> dialog.dismiss())
               .setNeutralButton("Mở Cài đặt", (dialog, which) -> {
                   openWidgetSettings();
                   dialog.dismiss();
               });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
}
