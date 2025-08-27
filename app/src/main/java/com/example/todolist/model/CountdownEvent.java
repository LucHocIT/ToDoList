package com.example.todolist.model;

import java.io.Serializable;
import java.util.Date;

public class CountdownEvent implements Serializable {
    private Long id;
    private String title;
    private int iconResourceId;
    private Date targetDate;
    private int calculationType; // 0: số ngày, 1: ngày còn lại
    private String description;
    private boolean isActive;
    private Date createdDate;

    public CountdownEvent() {
        this.createdDate = new Date();
        this.isActive = true;
        this.calculationType = 1; // Mặc định là ngày còn lại
    }

    public CountdownEvent(String title, int iconResourceId, Date targetDate, int calculationType) {
        this();
        this.title = title;
        this.iconResourceId = iconResourceId;
        this.targetDate = targetDate;
        this.calculationType = calculationType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public void setIconResourceId(int iconResourceId) {
        this.iconResourceId = iconResourceId;
    }

    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    public int getCalculationType() {
        return calculationType;
    }

    public void setCalculationType(int calculationType) {
        this.calculationType = calculationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Tính số ngày giữa ngày hiện tại và ngày mục tiêu
     */
    public long getDaysUntilTarget() {
        if (targetDate == null) return 0;
        
        Date now = new Date();
        long diffInMillis = targetDate.getTime() - now.getTime();
        return diffInMillis / (1000 * 60 * 60 * 24);
    }

    /**
     * Tính số ngày từ ngày tạo đến hiện tại
     */
    public long getDaysSinceCreated() {
        if (createdDate == null) return 0;
        
        Date now = new Date();
        long diffInMillis = now.getTime() - createdDate.getTime();
        return diffInMillis / (1000 * 60 * 60 * 24);
    }

    /**
     * Lấy text hiển thị dựa trên loại tính toán
     */
    public String getDisplayText() {
        if (calculationType == 0) { // Số ngày
            return String.valueOf(getDaysSinceCreated()) + "D";
        } else { // Ngày còn lại
            long days = getDaysUntilTarget();
            if (days < 0) {
                return "0D"; // Đã qua
            }
            return String.valueOf(days) + "D";
        }
    }
}
