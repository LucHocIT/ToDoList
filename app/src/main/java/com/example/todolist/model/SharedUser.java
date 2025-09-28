package com.example.todolist.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SharedUser implements Serializable {
    private String email;
    private String name;
    private String userId;
    private boolean canEdit;
    private String invitedAt;
    private String acceptedAt;
    private String status; // PENDING, ACCEPTED, REJECTED

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    public SharedUser() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        this.invitedAt = currentDate;
        this.status = STATUS_PENDING;
        this.canEdit = true; // Mặc định cho phép chỉnh sửa
    }

    public SharedUser(String email, String name) {
        this();
        this.email = email;
        this.name = name;
    }

    public SharedUser(String email, String name, boolean canEdit) {
        this(email, name);
        this.canEdit = canEdit;
    }

    public void acceptInvitation() {
        this.status = STATUS_ACCEPTED;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        this.acceptedAt = dateFormat.format(new Date());
    }

    public void rejectInvitation() {
        this.status = STATUS_REJECTED;
    }

    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("email", email);
        result.put("name", name);
        result.put("userId", userId);
        result.put("canEdit", canEdit);
        result.put("invitedAt", invitedAt);
        result.put("acceptedAt", acceptedAt);
        result.put("status", status);
        return result;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean canEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }

    public String getInvitedAt() { return invitedAt; }
    public void setInvitedAt(String invitedAt) { this.invitedAt = invitedAt; }

    public String getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(String acceptedAt) { this.acceptedAt = acceptedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Helper methods
    public boolean isAccepted() {
        return STATUS_ACCEPTED.equals(status);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }

    @Override
    public String toString() {
        return "SharedUser{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", canEdit=" + canEdit +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SharedUser that = (SharedUser) obj;
        return email != null ? email.equals(that.email) : that.email == null;
    }

    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }
}