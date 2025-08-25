package com.example.todolist.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaveformView extends View {
    private Paint paint;
    private List<Float> amplitudes;
    private Random random;
    private int maxAmplitude = 100;
    
    public WaveformView(Context context) {
        super(context);
        init();
    }
    
    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#2196F3"));
        paint.setStrokeWidth(3f);
        amplitudes = new ArrayList<>();
        random = new Random();
    }
    
    public void addAmplitude(float amplitude) {
        amplitudes.add(amplitude);
        if (amplitudes.size() > getWidth() / 6) {
            amplitudes.remove(0);
        }
        
        invalidate();
    }
    
    public void simulateRecording() {
        float amplitude = random.nextFloat() * maxAmplitude;
        addAmplitude(amplitude);
    }
    
    public void clear() {
        amplitudes.clear();
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (amplitudes.isEmpty()) return;
        
        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;
        
        float barWidth = 6f;
        float spacing = 2f;
        float totalBarWidth = barWidth + spacing;
        
        for (int i = 0; i < amplitudes.size(); i++) {
            float amplitude = amplitudes.get(i);
            float barHeight = (amplitude / maxAmplitude) * (height / 2);
            
            float x = width - (amplitudes.size() - i) * totalBarWidth;
            
            if (x >= 0) {
                canvas.drawRect(x, centerY - barHeight, x + barWidth, centerY, paint);
                canvas.drawRect(x, centerY, x + barWidth, centerY + barHeight, paint);
            }
        }
    }
}
