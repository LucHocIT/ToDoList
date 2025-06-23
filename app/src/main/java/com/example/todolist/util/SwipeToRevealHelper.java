package com.example.todolist.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;

public abstract class SwipeToRevealHelper extends ItemTouchHelper.SimpleCallback {
    
    private int starColor;
    private int calendarColor;
    private int deleteColor;
    private Drawable starIcon;
    private Drawable calendarIcon;
    private Drawable deleteIcon;
    private Paint paint;    public SwipeToRevealHelper() {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        
        starColor = Color.parseColor("#4285F4");
        calendarColor = Color.parseColor("#4285F4");
        deleteColor = Color.parseColor("#F44336");
        
        paint = new Paint();
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            
            // Only allow swiping to the left to reveal actions
            if (dX < 0) {
                float swipeWidth = Math.abs(dX);
                float maxSwipeWidth = 120; // Reduced swipe width for partial reveal
                
                if (swipeWidth > maxSwipeWidth) {
                    dX = -maxSwipeWidth;
                }
                
                // Move the item view to show actions behind
                itemView.setTranslationX(dX);
                
                // Draw action buttons behind the item
                int itemHeight = itemView.getHeight();
                float actionWidth = maxSwipeWidth / 3; // 3 actions
                
                // Initialize icons if not already done
                if (starIcon == null) {
                    starIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_star);
                    calendarIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_calendar);
                    deleteIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_delete);
                }
                
                // Draw delete background (rightmost)
                paint.setColor(deleteColor);
                RectF deleteBackground = new RectF(itemView.getRight() - actionWidth, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                c.drawRect(deleteBackground, paint);
                
                // Draw calendar background (middle)
                paint.setColor(calendarColor);
                RectF calendarBackground = new RectF(itemView.getRight() - actionWidth * 2, itemView.getTop(), itemView.getRight() - actionWidth, itemView.getBottom());
                c.drawRect(calendarBackground, paint);
                
                // Draw star background (leftmost)
                paint.setColor(starColor);
                RectF starBackground = new RectF(itemView.getRight() - actionWidth * 3, itemView.getTop(), itemView.getRight() - actionWidth * 2, itemView.getBottom());
                c.drawRect(starBackground, paint);
                
                // Draw icons
                drawIcon(c, deleteIcon, deleteBackground, itemHeight);
                drawIcon(c, calendarIcon, calendarBackground, itemHeight);
                drawIcon(c, starIcon, starBackground, itemHeight);
                
            } else {
                // Reset translation when not swiping left
                itemView.setTranslationX(0);
            }
        } else {
            // Reset when not swiping
            viewHolder.itemView.setTranslationX(0);
        }
    }
    
    private void drawIcon(Canvas c, Drawable icon, RectF background, int itemHeight) {
        if (icon != null) {
            int iconTop = (int) (background.top + (itemHeight - icon.getIntrinsicHeight()) / 2);
            int iconLeft = (int) (background.left + (background.width() - icon.getIntrinsicWidth()) / 2);
            int iconRight = iconLeft + icon.getIntrinsicWidth();
            int iconBottom = iconTop + icon.getIntrinsicHeight();
            
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            icon.setTint(Color.WHITE);
            icon.draw(c);
        }
    }
    
    public abstract void onStarClicked(int position);
    public abstract void onCalendarClicked(int position);
    public abstract void onDeleteClicked(int position);
}
