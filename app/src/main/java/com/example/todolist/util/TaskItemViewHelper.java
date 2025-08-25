package com.example.todolist.util;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.todolist.R;
import com.example.todolist.TaskDetailActivity;
import com.example.todolist.model.Task;

public class TaskItemViewHelper {
    public static View createTaskItemView(Context context, Task task) {
        LinearLayout taskItem = new LinearLayout(context);
        taskItem.setOrientation(LinearLayout.HORIZONTAL);
        taskItem.setPadding(0, CalendarUtils.dpToPx(context, 6), CalendarUtils.dpToPx(context, 16), CalendarUtils.dpToPx(context, 6));
        taskItem.setGravity(android.view.Gravity.CENTER_VERTICAL);
        taskItem.setBackgroundResource(R.drawable.task_item_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(CalendarUtils.dpToPx(context, 8), 0, CalendarUtils.dpToPx(context, 8), CalendarUtils.dpToPx(context, 4));
        taskItem.setLayoutParams(params);
        taskItem.setElevation(4f);

        // Blue left border
        View leftBorder = new View(context);
        if (task.isCompleted()) {
            leftBorder.setBackgroundResource(R.drawable.task_left_border_completed);
        } else {
            leftBorder.setBackgroundResource(R.drawable.task_left_border);
        }
        LinearLayout.LayoutParams borderParams = new LinearLayout.LayoutParams(CalendarUtils.dpToPx(context, 6), LinearLayout.LayoutParams.MATCH_PARENT);
        leftBorder.setLayoutParams(borderParams);
        taskItem.addView(leftBorder);

        // Content container
        LinearLayout contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(CalendarUtils.dpToPx(context, 16), CalendarUtils.dpToPx(context, 4), 0, CalendarUtils.dpToPx(context, 4));
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        contentParams.weight = 1;
        contentContainer.setLayoutParams(contentParams);

        // Task title
        TextView taskTitle = new TextView(context);
        taskTitle.setText(task.getTitle());
        taskTitle.setTextSize(14);
        taskTitle.setTextColor(Color.parseColor("#000000"));
        taskTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        if (task.isCompleted()) {
            taskTitle.setPaintFlags(taskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            taskTitle.setTextColor(Color.parseColor("#888888"));
        }
        contentContainer.addView(taskTitle);

        // Time and icons container
        LinearLayout timeIconContainer = new LinearLayout(context);
        timeIconContainer.setOrientation(LinearLayout.HORIZONTAL);
        timeIconContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
        timeIconContainer.setPadding(0, CalendarUtils.dpToPx(context, 2), 0, 0);

        // Task time
        if (task.getDueTime() != null && !task.getDueTime().equals("KhĂ´ng")) {
            TextView timeText = new TextView(context);
            timeText.setText(task.getDueTime());
            timeText.setTextSize(12);
            boolean isOverdue = CalendarUtils.isTimeOverdue(task.getDueTime());
            if (isOverdue && !task.isCompleted()) {
                timeText.setTextColor(Color.parseColor("#FF0000"));
            } else {
                timeText.setTextColor(Color.parseColor("#000000"));
            }
            timeText.setPadding(0, 0, CalendarUtils.dpToPx(context, 8), 0);
            timeIconContainer.addView(timeText);
        }

        // Notification icon
        if (task.isHasReminder()) {
            ImageView notificationIcon = new ImageView(context);
            notificationIcon.setImageResource(R.drawable.ic_notifications);
            notificationIcon.setColorFilter(Color.parseColor("#666666"));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(CalendarUtils.dpToPx(context, 18), CalendarUtils.dpToPx(context, 18));
            iconParams.setMarginEnd(CalendarUtils.dpToPx(context, 6));
            notificationIcon.setLayoutParams(iconParams);
            timeIconContainer.addView(notificationIcon);
        }

        // Repeat icon
        if (task.isRepeating()) {
            ImageView repeatIcon = new ImageView(context);
            repeatIcon.setImageResource(R.drawable.ic_repeat);
            repeatIcon.setColorFilter(Color.parseColor("#666666"));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(CalendarUtils.dpToPx(context, 18), CalendarUtils.dpToPx(context, 18));
            iconParams.setMarginEnd(CalendarUtils.dpToPx(context, 6));
            repeatIcon.setLayoutParams(iconParams);
            timeIconContainer.addView(repeatIcon);
        }

        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            ImageView notesIcon = new ImageView(context);
            notesIcon.setImageResource(R.drawable.ic_note);
            notesIcon.setColorFilter(Color.parseColor("#666666"));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(CalendarUtils.dpToPx(context, 18), CalendarUtils.dpToPx(context, 18));
            iconParams.setMarginEnd(CalendarUtils.dpToPx(context, 6));
            notesIcon.setLayoutParams(iconParams);
            timeIconContainer.addView(notesIcon);
        }

        if (task.hasAttachments()) {
            ImageView attachmentIcon = new ImageView(context);
            attachmentIcon.setImageResource(R.drawable.ic_attachment);
            attachmentIcon.setColorFilter(Color.parseColor("#666666"));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(CalendarUtils.dpToPx(context, 18), CalendarUtils.dpToPx(context, 18));
            attachmentIcon.setLayoutParams(iconParams);
            timeIconContainer.addView(attachmentIcon);
        }

        contentContainer.addView(timeIconContainer);
        taskItem.addView(contentContainer);

        if (task.isImportant()) {
            ImageView starIcon = new ImageView(context);
            starIcon.setImageResource(R.drawable.ic_star_filled);
            LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(CalendarUtils.dpToPx(context, 20), CalendarUtils.dpToPx(context, 20));
            starParams.setMarginEnd(CalendarUtils.dpToPx(context, 8));
            starIcon.setLayoutParams(starParams);
            taskItem.addView(starIcon);
        }

        // Click listener
        taskItem.setOnClickListener(v -> {
            Intent intent = new Intent(context, TaskDetailActivity.class);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
            context.startActivity(intent);
        });

        return taskItem;
    }
}