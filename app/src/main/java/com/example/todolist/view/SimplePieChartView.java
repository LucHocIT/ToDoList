    package com.example.todolist.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimplePieChartView extends View {
    
    private List<PieSlice> slices = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rectF = new RectF();
    
    // Colors matching the sample image
    private int[] colors = {
        0xFF5C9CFF,  // Blue - Danh sách yêu thích
        0xFF9CC3FF,  // Light Blue - không có thể loại 
        0xFFC8DFFF,  // Lighter Blue - Công việc
        0xFF4A90E2,  // Medium Blue
        0xFF2E7BD6,  // Dark Blue
        0xFF8BB8FF,  // Sky Blue
        0xFF6FA8FF,  // Ocean Blue
        0xFF5A9BFF,  
        0xFFB8D4FF,  
        0xFF7AB3FF   
    };
    
    public SimplePieChartView(Context context) {
        super(context);
        init();
    }
    
    public SimplePieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public SimplePieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        paint.setStyle(Paint.Style.FILL);
    }
    
    public void setData(Map<String, Integer> categoryCount) {
        slices.clear();
        
        if (categoryCount.isEmpty()) {
            invalidate();
            return;
        }
        
        // Calculate total
        int total = 0;
        for (Integer count : categoryCount.values()) {
            total += count;
        }
        
        // Create slices
        int colorIndex = 0;
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            String categoryName = entry.getKey();
            Integer count = entry.getValue();
            
            float percentage = (float) count / total;
            int color = colors[colorIndex % colors.length];
            
            slices.add(new PieSlice(categoryName, count, percentage, color));
            colorIndex++;
        }
        
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (slices.isEmpty()) {
            // Draw empty circle
            paint.setColor(0xFFE0E0E0);
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = Math.min(centerX, centerY) * 0.8f;
            canvas.drawCircle(centerX, centerY, radius, paint);
            return;
        }
        
        // Setup drawing area
        float padding = getWidth() * 0.1f;
        rectF.set(padding, padding, getWidth() - padding, getHeight() - padding);
        
        // Draw slices
        float startAngle = -90f; // Start from top
        for (PieSlice slice : slices) {
            paint.setColor(slice.color);
            float sweepAngle = slice.percentage * 360f;
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            startAngle += sweepAngle;
        }
        
        // Draw center circle (donut effect like in sample)
        paint.setColor(0xFFFFFFFF); // White center
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float innerRadius = Math.min(centerX, centerY) * 0.4f;
        canvas.drawCircle(centerX, centerY, innerRadius, paint);
    }
    
    private static class PieSlice {
        String label;
        int value;
        float percentage;
        int color;
        
        PieSlice(String label, int value, float percentage, int color) {
            this.label = label;
            this.value = value;
            this.percentage = percentage;
            this.color = color;
        }
    }
}
