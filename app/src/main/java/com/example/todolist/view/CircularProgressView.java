package com.example.todolist.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF rectF;
    private int progress = 0;
    private int maxProgress = 100;
    
    public CircularProgressView(Context context) {
        super(context);
        init();
    }
    
    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(8 * getResources().getDisplayMetrics().density);
        backgroundPaint.setColor(0xFFE0E0E0);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(8 * getResources().getDisplayMetrics().density);
        progressPaint.setColor(0xFF4285F4);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        
        rectF = new RectF();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float strokeWidth = backgroundPaint.getStrokeWidth();
        float padding = strokeWidth / 2;
        rectF.set(padding, padding, w - padding, h - padding);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw background circle
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, 
                (getWidth() - backgroundPaint.getStrokeWidth()) / 2f, backgroundPaint);
        
        // Draw progress arc
        if (progress > 0) {
            float sweepAngle = (360f * progress) / maxProgress;
            canvas.drawArc(rectF, -90, sweepAngle, false, progressPaint);
        }
    }
    
    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(progress, maxProgress));
        invalidate();
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
        invalidate();
    }
}
