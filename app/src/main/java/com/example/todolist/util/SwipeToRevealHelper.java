package com.example.todolist.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;

public abstract class SwipeToRevealHelper extends ItemTouchHelper.SimpleCallback {
    
    private static final String TAG = "SwipeToRevealHelper";
    
    private int starColor;
    private int calendarColor;
    private int deleteColor;
    private Drawable starIcon;
    private Drawable calendarIcon;
    private Drawable deleteIcon;
    private Paint paint;
    private RecyclerView recyclerView;    public SwipeToRevealHelper() {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT); // Allow both LEFT and RIGHT swipe
        
        starColor = Color.parseColor("#4285F4");
        calendarColor = Color.parseColor("#4285F4");
        deleteColor = Color.parseColor("#F44336");
        
        paint = new Paint();
    }    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Don't auto-dismiss the swipe, let user interact with actions
        // Keep the current translation position
        View itemView = viewHolder.itemView;
        float currentTranslation = itemView.getTranslationX();
        
        Log.d(TAG, "onSwiped: direction=" + direction + ", currentTranslation=" + currentTranslation);
        
        // If swiping right to close, allow it
        if (direction == ItemTouchHelper.RIGHT && currentTranslation < 0) {
            // User is swiping right to close, reset position
            itemView.setTranslationX(0);
        } else if (direction == ItemTouchHelper.LEFT) {
            // User swiped left to reveal actions, keep current position
            // Don't reset automatically
        }
    }
    
    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.3f; // Require 30% swipe to trigger
    }
    
    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 0.5f; // Make it easier to swipe back
    }
    
    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return defaultValue * 0.5f; // Make it easier to trigger swipe
    }      @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            
            Log.d(TAG, "onChildDraw: dX=" + dX + ", isCurrentlyActive=" + isCurrentlyActive + ", currentTranslation=" + itemView.getTranslationX());
            
            if (dX < 0) {
                // Swiping left to reveal actions
                float swipeWidth = Math.abs(dX);
                float density = itemView.getContext().getResources().getDisplayMetrics().density;
                float maxSwipeWidth = 240 * density; // Width for 3 action buttons (80dp each)
                
                // Limit swipe distance
                if (swipeWidth > maxSwipeWidth) {
                    dX = -maxSwipeWidth;
                    swipeWidth = maxSwipeWidth;
                }
                
                // Draw action buttons behind the item
                drawActionButtons(c, itemView, swipeWidth);
                
            } else if (dX > 0) {
                // Swiping right - could be closing or normal swipe
                float currentTranslation = itemView.getTranslationX();
                
                if (currentTranslation < 0) {
                    // Item was swiped left, now swiping right to close
                    float newTranslation = Math.min(0, currentTranslation + dX);
                    dX = newTranslation;
                    
                    // Still draw action buttons if not fully closed
                    if (newTranslation < 0) {
                        drawActionButtons(c, itemView, Math.abs(newTranslation));
                    }
                } else {
                    // Normal state, don't allow right swipe beyond 0
                    dX = 0;
                }
            }
            
            // Apply the translation
            itemView.setTranslationX(dX);
            
        } else {
            // For other states, call default implementation
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
    
    private void drawActionButtons(Canvas c, View itemView, float swipeWidth) {
        int itemHeight = itemView.getHeight();
        float actionWidth = 80 * itemView.getContext().getResources().getDisplayMetrics().density; // Convert 80dp to pixels
        
        // Initialize icons if not already done
        if (starIcon == null) {
            starIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_star);
            calendarIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_calendar);
            deleteIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_delete);
        }
        
        float rightEdge = itemView.getRight();
        
        // Only draw buttons that are visible based on swipe distance
        if (swipeWidth >= actionWidth) {
            // Draw delete background (rightmost - red)
            paint.setColor(deleteColor);
            RectF deleteBackground = new RectF(rightEdge - actionWidth, itemView.getTop(), rightEdge, itemView.getBottom());
            c.drawRect(deleteBackground, paint);
            drawIcon(c, deleteIcon, deleteBackground, itemHeight);
        }
        
        if (swipeWidth >= actionWidth * 2) {
            // Draw calendar background (middle - blue)
            paint.setColor(calendarColor);
            RectF calendarBackground = new RectF(rightEdge - actionWidth * 2, itemView.getTop(), rightEdge - actionWidth, itemView.getBottom());
            c.drawRect(calendarBackground, paint);
            drawIcon(c, calendarIcon, calendarBackground, itemHeight);
        }
        
        if (swipeWidth >= actionWidth * 3) {
            // Draw star background (leftmost - blue)
            paint.setColor(starColor);
            RectF starBackground = new RectF(rightEdge - actionWidth * 3, itemView.getTop(), rightEdge - actionWidth * 2, itemView.getBottom());
            c.drawRect(starBackground, paint);
            drawIcon(c, starIcon, starBackground, itemHeight);
        }
    }
      private void drawIcon(Canvas c, Drawable icon, RectF background, int itemHeight) {
        if (icon != null) {
            // Use a fixed icon size in pixels (32dp converted to pixels)
            int iconSize = 96; // Approximately 32dp in most screen densities
            
            int iconTop = (int) (background.top + (itemHeight - iconSize) / 2);
            int iconLeft = (int) (background.left + (background.width() - iconSize) / 2);
            int iconRight = iconLeft + iconSize;
            int iconBottom = iconTop + iconSize;
            
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            icon.setTint(Color.WHITE);
            icon.draw(c);
        }
    }
    
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }    // Method to check if a click happened on the revealed action buttons
    public boolean handleActionClick(RecyclerView.ViewHolder viewHolder, float x, float y) {
        View itemView = viewHolder.itemView;
        float translationX = itemView.getTranslationX();
        
        Log.d(TAG, "handleActionClick: x=" + x + ", y=" + y + ", translationX=" + translationX);
        
        // Only handle clicks if item is swiped (showing actions)
        if (translationX >= 0) {
            Log.d(TAG, "Item not swiped, ignoring click");
            return false;
        }
        
        // Convert dp to pixels
        float actionWidth = 80 * itemView.getContext().getResources().getDisplayMetrics().density;
        float rightEdge = itemView.getRight();
        int position = viewHolder.getAdapterPosition();
        
        Log.d(TAG, "actionWidth=" + actionWidth + ", rightEdge=" + rightEdge + ", position=" + position);
        
        // Check if position is valid
        if (position == RecyclerView.NO_POSITION) {
            Log.d(TAG, "Invalid position");
            return false;
        }
        
        // Calculate the visible action area (the revealed part)
        float swipedWidth = Math.abs(translationX);
        float leftBoundary = rightEdge - swipedWidth;
        
        Log.d(TAG, "swipedWidth=" + swipedWidth + ", leftBoundary=" + leftBoundary);
        
        // Check if click is within the revealed action area
        if (x < leftBoundary || x > rightEdge) {
            Log.d(TAG, "Click outside revealed area");
            return false;
        }
          // Check delete button (rightmost)
        if (x >= rightEdge - actionWidth && x <= rightEdge) {
            Log.d(TAG, "Delete button clicked");
            onDeleteClicked(position);
            // Don't reset position automatically - let user swipe back manually
            return true;
        }
        
        // Check calendar button (middle) - only if enough area is revealed
        if (swipedWidth >= actionWidth * 2 && x >= rightEdge - actionWidth * 2 && x <= rightEdge - actionWidth) {
            Log.d(TAG, "Calendar button clicked");
            onCalendarClicked(position);
            // Don't reset position automatically - let user swipe back manually
            return true;
        }
        
        // Check star button (leftmost) - only if enough area is revealed
        if (swipedWidth >= actionWidth * 3 && x >= rightEdge - actionWidth * 3 && x <= rightEdge - actionWidth * 2) {
            Log.d(TAG, "Star button clicked");
            onStarClicked(position);
            // Don't reset position automatically - let user swipe back manually
            return true;
        }
        
        Log.d(TAG, "No button clicked");
        return false;
    }
    
    // Method to reset item position (close the swipe)
    public void resetItemPosition(View itemView) {
        itemView.animate()
                .translationX(0)
                .setDuration(200)
                .start();
    }
    
    // Method to close all opened swipe actions
    public void closeAllSwipeActions() {
        if (recyclerView == null) return;
        
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            if (child != null && child.getTranslationX() != 0) {
                resetItemPosition(child);
            }
        }
    }
    
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }
    
    public abstract void onStarClicked(int position);
    public abstract void onCalendarClicked(int position);
    public abstract void onDeleteClicked(int position);
}
