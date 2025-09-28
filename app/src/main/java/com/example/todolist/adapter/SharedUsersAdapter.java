package com.example.todolist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.model.SharedUser;

import java.util.List;

public class SharedUsersAdapter extends RecyclerView.Adapter<SharedUsersAdapter.SharedUserViewHolder> {
    
    private List<SharedUser> sharedUsers;
    private OnSharedUserActionListener listener;
    private Context context;

    public interface OnSharedUserActionListener {
        void onRemoveUser(SharedUser user);
        void onTogglePermission(SharedUser user, boolean canEdit);
    }

    public SharedUsersAdapter(Context context, List<SharedUser> sharedUsers, OnSharedUserActionListener listener) {
        this.context = context;
        this.sharedUsers = sharedUsers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SharedUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shared_user, parent, false);
        return new SharedUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SharedUserViewHolder holder, int position) {
        SharedUser user = sharedUsers.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return sharedUsers.size();
    }

    class SharedUserViewHolder extends RecyclerView.ViewHolder {
        private TextView textAvatar;
        private TextView textUserName;
        private TextView textUserEmail;
        private TextView textStatus;
        private Switch switchCanEdit;
        private ImageView btnRemove;

        SharedUserViewHolder(@NonNull View itemView) {
            super(itemView);
            textAvatar = itemView.findViewById(R.id.text_avatar);
            textUserName = itemView.findViewById(R.id.text_user_name);
            textUserEmail = itemView.findViewById(R.id.text_user_email);
            textStatus = itemView.findViewById(R.id.text_status);
            switchCanEdit = itemView.findViewById(R.id.switch_can_edit);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }

        void bind(SharedUser user) {
            // Hiển thị avatar (chữ cái đầu của tên)
            String name = user.getName();
            String avatarText = (name != null && !name.isEmpty()) ? 
                String.valueOf(name.charAt(0)).toUpperCase() : "U";
            textAvatar.setText(avatarText);
            
            textUserName.setText(user.getName());
            textUserEmail.setText(user.getEmail());
            
            // Hiển thị trạng thái (compact format)
            textStatus.setText("• " + getStatusText(user.getStatus()));
            textStatus.setTextColor(getStatusColor(user.getStatus()));
            
            // Thiết lập switch permission
            switchCanEdit.setChecked(user.canEdit());
            switchCanEdit.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTogglePermission(user, isChecked);
                }
            });
            
            // Chỉ cho phép chỉnh sửa nếu user đã accept
            switchCanEdit.setEnabled(user.isAccepted());
            
            // Remove button
            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveUser(user);
                }
            });
        }

        private String getStatusText(String status) {
            switch (status) {
                case SharedUser.STATUS_ACCEPTED:
                    return "Đã tham gia";
                case SharedUser.STATUS_REJECTED:
                    return "Đã từ chối";
                case SharedUser.STATUS_PENDING:
                default:
                    return "Đang chờ";
            }
        }

        private int getStatusColor(String status) {
            switch (status) {
                case SharedUser.STATUS_ACCEPTED:
                    return androidx.core.content.ContextCompat.getColor(context, R.color.green_success);
                case SharedUser.STATUS_REJECTED:
                    return androidx.core.content.ContextCompat.getColor(context, R.color.red);
                case SharedUser.STATUS_PENDING:
                default:
                    return androidx.core.content.ContextCompat.getColor(context, R.color.theme_orange_primary);
            }
        }
    }
}