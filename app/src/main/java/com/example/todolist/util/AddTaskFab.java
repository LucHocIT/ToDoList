package com.example.todolist.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * FloatingActionButton tùy chỉnh cho chức năng thêm nhiệm vụ
 * Có thể tái sử dụng trên toàn bộ ứng dụng
 */
public class AddTaskFab extends FloatingActionButton {
    
    private AddTaskHandler addTaskHandler;
    private String prefilledDate;
    private String prefilledCategory;
    
    public AddTaskFab(Context context) {
        super(context);
        init();
    }
    
    public AddTaskFab(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public AddTaskFab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Thiết lập listener mặc định
        setOnClickListener(v -> {
            if (addTaskHandler != null) {
                addTaskHandler.showAddTaskDialog(prefilledDate, prefilledCategory);
            }
        });
    }
    
    /**
     * Thiết lập AddTaskHandler
     */
    public void setAddTaskHandler(AddTaskHandler handler) {
        this.addTaskHandler = handler;
    }
    
    /**
     * Thiết lập ngày được chọn trước
     */
    public void setPrefilledDate(String date) {
        this.prefilledDate = date;
    }
    
    /**
     * Thiết lập danh mục được chọn trước
     */
    public void setPrefilledCategory(String category) {
        this.prefilledCategory = category;
    }
    
    /**
     * Thiết lập cả ngày và danh mục được chọn trước
     */
    public void setPrefilledData(String date, String category) {
        this.prefilledDate = date;
        this.prefilledCategory = category;
    }
    
    /**
     * Xóa dữ liệu được chọn trước
     */
    public void clearPrefilledData() {
        this.prefilledDate = null;
        this.prefilledCategory = null;
    }
}
