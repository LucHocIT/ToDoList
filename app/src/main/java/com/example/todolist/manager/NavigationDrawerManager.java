package com.example.todolist.manager;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.todolist.R;

public class NavigationDrawerManager {
    
    public interface NavigationListener {
        void onThemeSelected();
        void onUtilitiesSelected();
        void onContactSelected();
        void onSettingsSelected();
    }
    
    private AppCompatActivity activity;
    private DrawerLayout drawerLayout;
    private NavigationListener listener;
    
    // Navigation menu items
    private LinearLayout navTheme;
    private LinearLayout navUtilities;
    private LinearLayout navContact;
    private LinearLayout navSettings;
    
    public NavigationDrawerManager(AppCompatActivity activity, DrawerLayout drawerLayout, NavigationListener listener) {
        this.activity = activity;
        this.drawerLayout = drawerLayout;
        this.listener = listener;
        
        initNavigationItems();
        setupNavigationListeners();
    }
    
    private void initNavigationItems() {
        navTheme = activity.findViewById(R.id.nav_theme);
        navUtilities = activity.findViewById(R.id.nav_utilities);
        navContact = activity.findViewById(R.id.nav_contact);
        navSettings = activity.findViewById(R.id.nav_settings);
    }
    
    private void setupNavigationListeners() {
        if (navTheme != null) {
            navTheme.setOnClickListener(v -> {
                closeDrawer();
                if (listener != null) {
                    listener.onThemeSelected();
                }
                showThemeDialog();
            });
        }
        
        if (navUtilities != null) {
            navUtilities.setOnClickListener(v -> {
                closeDrawer();
                if (listener != null) {
                    listener.onUtilitiesSelected();
                }
                showUtilitiesDialog();
            });
        }
        
        if (navContact != null) {
            navContact.setOnClickListener(v -> {
                closeDrawer();
                if (listener != null) {
                    listener.onContactSelected();
                }
                showContactDialog();
            });
        }
        
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                closeDrawer();
                if (listener != null) {
                    listener.onSettingsSelected();
                }
                showSettingsDialog();
            });
        }
    }
    
    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
    
    public void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
    
    public boolean isDrawerOpen() {
        return drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START);
    }
    
    private void showThemeDialog() {
        // Mở ThemeSelectionActivity thay vì hiển thị dialog đơn giản
        Intent intent = new Intent(activity, com.example.todolist.ThemeSelectionActivity.class);
        activity.startActivity(intent);
    }
    
    private void showUtilitiesDialog() {
        // Hiển thị dialog tiện ích
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        builder.setTitle("Tiện ích");
        
        String[] utilities = {"Xuất dữ liệu", "Nhập dữ liệu", "Sao lưu", "Khôi phục"};
        builder.setItems(utilities, (dialog, which) -> {
            String selectedUtility = utilities[which];
            Toast.makeText(activity, "Đã chọn: " + selectedUtility, Toast.LENGTH_SHORT).show();
            // TODO: Implement utility functions
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    
    private void showContactDialog() {
        // Hiển thị thông tin liên hệ
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        builder.setTitle("Liên hệ");
        builder.setMessage("Ứng dụng To-Do List\n\n" +
                "Phiên bản: 1.0\n" +
                "Nhà phát triển: Your Name\n" +
                "Email: contact@todolist.com\n" +
                "Website: www.todolist.com");
        
        builder.setPositiveButton("Đóng", null);
        builder.setNeutralButton("Gửi phản hồi", (dialog, which) -> {
            // Mở email app để gửi phản hồi
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"contact@todolist.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Phản hồi về ứng dụng To-Do List");
            
            try {
                activity.startActivity(Intent.createChooser(emailIntent, "Gửi email"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(activity, "Không tìm thấy ứng dụng email", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.show();
    }
    
    private void showSettingsDialog() {
        // Hiển thị dialog cài đặt
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        builder.setTitle("Cài đặt");
        
        String[] settings = {"Thông báo", "Âm thanh", "Ngôn ngữ", "Về ứng dụng"};
        builder.setItems(settings, (dialog, which) -> {
            String selectedSetting = settings[which];
            Toast.makeText(activity, "Đã chọn: " + selectedSetting, Toast.LENGTH_SHORT).show();
            // TODO: Implement settings functions
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
}
