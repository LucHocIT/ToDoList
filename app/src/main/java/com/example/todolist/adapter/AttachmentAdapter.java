package com.example.todolist.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.model.Attachment;
import java.io.File;
import java.util.List;

public class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.AttachmentViewHolder> {
    
    private List<Attachment> attachments;
    private Context context;
    private OnAttachmentActionListener listener;
    
    public interface OnAttachmentActionListener {
        void onAttachmentDelete(Attachment attachment);
        void onAttachmentClick(Attachment attachment);
    }
    
    public AttachmentAdapter(Context context, List<Attachment> attachments, OnAttachmentActionListener listener) {
        this.context = context;
        this.attachments = attachments;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public AttachmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attachment, parent, false);
        return new AttachmentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AttachmentViewHolder holder, int position) {
        Attachment attachment = attachments.get(position);
        holder.bind(attachment);
    }
    
    @Override
    public int getItemCount() {
        return attachments.size();
    }
    
    public void updateAttachments(List<Attachment> newAttachments) {
        this.attachments = newAttachments;
        notifyDataSetChanged();
    }
    
    class AttachmentViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconFileType;
        private TextView textFileName;
        private TextView textFileSize;
        private ImageView btnDelete;
        
        public AttachmentViewHolder(@NonNull View itemView) {
            super(itemView);
            iconFileType = itemView.findViewById(R.id.icon_file_type);
            textFileName = itemView.findViewById(R.id.text_file_name);
            textFileSize = itemView.findViewById(R.id.text_file_size);
            btnDelete = itemView.findViewById(R.id.btn_delete_attachment);
        }
        
        public void bind(Attachment attachment) {
            textFileName.setText(attachment.getFileName());
            textFileSize.setText(attachment.getFormattedFileSize());
            
            // Set icon based on file type
            setFileTypeIcon(attachment.getFileExtension());
            
            // Handle click to open file
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAttachmentClick(attachment);
                } else {
                    // Open file using download URL
                    openFileFromUrl(attachment.getDownloadUrl(), attachment.getFileName());
                }
            });
            
            // Handle delete button
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAttachmentDelete(attachment);
                }
            });
        }
        
        private void openFileFromUrl(String downloadUrl, String fileName) {
            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(downloadUrl));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Không thể mở file: " + fileName, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Link file không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        }
        
        private void setFileTypeIcon(String extension) {
            int iconResource;
            switch (extension.toLowerCase()) {
                case "pdf":
                    iconResource = R.drawable.ic_description;
                    break;
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                case "bmp":
                    iconResource = R.drawable.ic_person; // Use a generic icon for now
                    break;
                case "doc":
                case "docx":
                    iconResource = R.drawable.ic_description;
                    break;
                case "xls":
                case "xlsx":
                    iconResource = R.drawable.ic_description;
                    break;
                case "txt":
                    iconResource = R.drawable.ic_note;
                    break;
                case "mp3":
                case "wav":
                case "aac":
                    iconResource = R.drawable.ic_music_note;
                    break;
                default:
                    iconResource = R.drawable.ic_attachment;
                    break;
            }
            iconFileType.setImageResource(iconResource);
        }
    }
}
