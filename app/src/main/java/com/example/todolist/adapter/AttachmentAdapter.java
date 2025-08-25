package com.example.todolist.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.todolist.R;
import com.example.todolist.model.Attachment;
import java.io.File;
import java.util.List;

public class AttachmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_MEDIA = 1; 
    private static final int VIEW_TYPE_FILE = 2;  
    
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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_MEDIA) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attachment_grid, parent, false);
            return new MediaViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attachment, parent, false);
            return new FileViewHolder(view);
            }
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        Attachment attachment = attachments.get(position);
        String fileName = attachment.getFileName().toLowerCase();
        
        // Kiểm tra xem có phải ảnh hoặc video không
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
            fileName.endsWith(".png") || fileName.endsWith(".gif") ||
            fileName.endsWith(".mp4") || fileName.endsWith(".avi") || 
            fileName.endsWith(".mov") || fileName.endsWith(".wmv") ||
            fileName.endsWith(".mkv")) {
            return VIEW_TYPE_MEDIA;
        } else {
            return VIEW_TYPE_FILE;
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Attachment attachment = attachments.get(position);
        if (holder instanceof MediaViewHolder) {
            ((MediaViewHolder) holder).bind(attachment);
        } else if (holder instanceof FileViewHolder) {
            ((FileViewHolder) holder).bind(attachment);
        }
    }
    
    @Override
    public int getItemCount() {
        return attachments.size();
    }
    
    public void updateAttachments(List<Attachment> newAttachments) {
        this.attachments = newAttachments;
        notifyDataSetChanged();
    }
    
    // ViewHolder cho ảnh và video (dùng grid layout)
    class MediaViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconFileType;
        private ImageView btnDelete;
        private RelativeLayout previewContainer;
        private ImageView imagePreview;
        private ImageView videoThumbnail;
        private ImageView playOverlay;
        
        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            iconFileType = itemView.findViewById(R.id.icon_file_type);
            btnDelete = itemView.findViewById(R.id.btn_delete_attachment);
            previewContainer = itemView.findViewById(R.id.preview_container);
            imagePreview = itemView.findViewById(R.id.image_preview);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
            playOverlay = itemView.findViewById(R.id.play_overlay);
        }
        
        public void bind(Attachment attachment) {
            String extension = attachment.getFileExtension().toLowerCase();
            
            // Hide all preview elements first
            previewContainer.setVisibility(View.GONE);
            imagePreview.setVisibility(View.GONE);
            videoThumbnail.setVisibility(View.GONE);
            playOverlay.setVisibility(View.GONE);
            iconFileType.setVisibility(View.VISIBLE);
            
            // Handle preview for images and videos
            if (isImageFile(extension)) {
                showImagePreview(attachment);
            } else if (isVideoFile(extension)) {
                showVideoPreview(attachment);
            }
            
            // Handle click to open file
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAttachmentClick(attachment);
                } else {
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
        
        private boolean isImageFile(String extension) {
            return extension.equals("jpg") || extension.equals("jpeg") || 
                   extension.equals("png") || extension.equals("gif") || 
                   extension.equals("bmp") || extension.equals("webp");
        }
        
        private boolean isVideoFile(String extension) {
            return extension.equals("mp4") || extension.equals("avi") || 
                   extension.equals("mov") || extension.equals("wmv") || 
                   extension.equals("3gp") || extension.equals("mkv");
        }
        
        private void showImagePreview(Attachment attachment) {
            previewContainer.setVisibility(View.VISIBLE);
            imagePreview.setVisibility(View.VISIBLE);
            iconFileType.setVisibility(View.GONE);
            
            if (attachment.getDownloadUrl() != null && !attachment.getDownloadUrl().isEmpty()) {
                Glide.with(context)
                    .load(attachment.getDownloadUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(imagePreview);
            }
        }
        
        private void showVideoPreview(Attachment attachment) {
            previewContainer.setVisibility(View.VISIBLE);
            videoThumbnail.setVisibility(View.VISIBLE);
            playOverlay.setVisibility(View.VISIBLE);
            iconFileType.setVisibility(View.GONE);
            
            if (attachment.getDownloadUrl() != null && !attachment.getDownloadUrl().isEmpty()) {
                Glide.with(context)
                    .load(attachment.getDownloadUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(videoThumbnail);
            }
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
    }
    
    // ViewHolder cho file âm thanh và tài liệu (dùng layout ngang có tên)
    class FileViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconFileType;
        private TextView textFileName;
        private TextView textFileSize;
        private ImageView btnDelete;
        
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            iconFileType = itemView.findViewById(R.id.icon_file_type);
            textFileName = itemView.findViewById(R.id.text_file_name);
            textFileSize = itemView.findViewById(R.id.text_file_size);
            btnDelete = itemView.findViewById(R.id.btn_delete_attachment);
        }
        
        public void bind(Attachment attachment) {
            textFileName.setText(attachment.getFileName());
            textFileSize.setText(attachment.getFormattedFileSize());
            
            String extension = attachment.getFileExtension().toLowerCase();
            setFileTypeIcon(extension);
            
            // Handle click to open file
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAttachmentClick(attachment);
                } else {
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
        
        private void setFileTypeIcon(String extension) {
            int iconResource;
            switch (extension.toLowerCase()) {
                case "pdf":
                    iconResource = R.drawable.ic_description;
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
                case "3gp":
                    iconResource = R.drawable.ic_music_note;
                    break;
                default:
                    iconResource = R.drawable.ic_attachment;
                    break;
            }
            iconFileType.setImageResource(iconResource);
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
    }
