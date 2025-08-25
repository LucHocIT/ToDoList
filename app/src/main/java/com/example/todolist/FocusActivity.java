package com.example.todolist;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FocusActivity extends AppCompatActivity {
    
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_FOCUS_DURATION = "focus_duration";  
    private TextView tvTaskTitle;
    private TextView tvTimer;
    private TextView tvMessage;
    private ProgressBar progressTimer;
    private Button btnPause;
    private Button btnStop;   
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private long totalTimeInMillis;
    private boolean timerRunning = false;
    private boolean timerPaused = false;
    private static final long DEFAULT_FOCUS_TIME = 25 * 60 * 1000; 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus);
        
        initViews();
        setupFromIntent();
        setupClickListeners();
        startTimer();
    }
    
    private void initViews() {
        tvTaskTitle = findViewById(R.id.tv_task_title);
        tvTimer = findViewById(R.id.tv_timer);
        tvMessage = findViewById(R.id.tv_message);
        progressTimer = findViewById(R.id.progress_timer);
        btnPause = findViewById(R.id.btn_pause);
        btnStop = findViewById(R.id.btn_stop);
    }
    
    private void setupFromIntent() {
        Intent intent = getIntent();
        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        long duration = intent.getLongExtra(EXTRA_FOCUS_DURATION, DEFAULT_FOCUS_TIME);
        
        // Set task title instead of default
        tvTaskTitle.setText(taskTitle != null ? taskTitle : getString(R.string.focus_session_title));
        timeLeftInMillis = duration;
        totalTimeInMillis = duration;
        progressTimer.setMax((int) (duration / 1000));
        progressTimer.setProgress((int) (duration / 1000));
        
        updateTimerText();
        // Start with active message since timer starts automatically
        tvMessage.setText(getString(R.string.focus_message_active));
    }
    
    private void setupClickListeners() {
        btnPause.setOnClickListener(v -> {
            if (timerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });
        
        btnStop.setOnClickListener(v -> {
            showExitConfirmDialog();
        });
        
        // Auto start timer when activity opens
        startTimer();
    }
    
    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
                updateProgress();
            }
            
            @Override
            public void onFinish() {
                timerRunning = false;
                tvMessage.setText(getString(R.string.focus_message_completed));
                btnPause.setText(getString(R.string.btn_complete));
                btnPause.setOnClickListener(v -> finish());
                btnStop.setVisibility(View.GONE);
            }
        }.start();
        
        timerRunning = true;
        timerPaused = false;
        btnPause.setText(getString(R.string.btn_pause));
        tvMessage.setText(getString(R.string.focus_message_active));
        
        // Hide stop button when running
        btnStop.setText(getString(R.string.btn_stop));
        btnStop.setVisibility(View.GONE);
    }
    
    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        timerPaused = true;
        
        // Update UI for paused state
        btnPause.setText(getString(R.string.btn_continue));
        tvMessage.setText(getString(R.string.focus_message_paused));
        
        // Show stop button as "KẾT THÚC" in paused state
        btnStop.setText("KẾT THÚC");
        btnStop.setVisibility(View.VISIBLE);
    }
    
    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
    }
    
    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        
        String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
        tvTimer.setText(timeLeftFormatted);
    }
    
    private void updateProgress() {
        int progress = (int) (timeLeftInMillis / 1000);
        progressTimer.setProgress(progress);
    }
    
    private void showExitConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Kết thúc phiên tập trung")
                .setMessage("Bạn có muốn kết thúc phiên tập trung không?")
                .setPositiveButton("Có", (dialog, which) -> {
                    stopTimer();
                    finish();
                })
                .setNegativeButton("Không", null)
                .show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back press during focus session - user must use stop button
        super.onBackPressed(); // Call super to avoid lint error
    }
}
