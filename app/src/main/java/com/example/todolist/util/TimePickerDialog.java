package com.example.todolist.util;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.example.todolist.R;
import com.google.android.material.button.MaterialButton;

public class TimePickerDialog {
    
    public interface OnTimeSelectedListener {
        void onTimeSelected(String time, boolean hasTime);
    }
    
    private Context context;
    private OnTimeSelectedListener listener;
    private Dialog dialog;
    private TextView tvSelectedTime;
    private String selectedTime = "2:09";
    private boolean hasTime = false;
    
    public TimePickerDialog(Context context, OnTimeSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        createDialog();
    }
    
    private void createDialog() {
        dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        initViews(view);
        setupClickListeners(view);
    }
    
    private void initViews(View view) {
        tvSelectedTime = view.findViewById(R.id.tv_selected_time);
        tvSelectedTime.setText(selectedTime);
    }
    
    private void setupClickListeners(View view) {
        // Quick time selection buttons
        MaterialButton btnNoTime = view.findViewById(R.id.btn_no_time);
        MaterialButton btn0700 = view.findViewById(R.id.btn_07_00);
        MaterialButton btn0900 = view.findViewById(R.id.btn_09_00);
        MaterialButton btn1000 = view.findViewById(R.id.btn_10_00);
        MaterialButton btn1200 = view.findViewById(R.id.btn_12_00);
        MaterialButton btn1400 = view.findViewById(R.id.btn_14_00);
        MaterialButton btn1600 = view.findViewById(R.id.btn_16_00);
        MaterialButton btn1800 = view.findViewById(R.id.btn_18_00);
        
        btnNoTime.setOnClickListener(v -> {
            hasTime = false;
            selectedTime = "";
            updateTimeDisplay();
        });
        
        btn0700.setOnClickListener(v -> setTime("07:00"));
        btn0900.setOnClickListener(v -> setTime("09:00"));
        btn1000.setOnClickListener(v -> setTime("10:00"));
        btn1200.setOnClickListener(v -> setTime("12:00"));
        btn1400.setOnClickListener(v -> setTime("14:00"));
        btn1600.setOnClickListener(v -> setTime("16:00"));
        btn1800.setOnClickListener(v -> setTime("18:00"));
        
        // Action buttons
        TextView btnCancel = view.findViewById(R.id.btn_cancel_time);
        TextView btnDone = view.findViewById(R.id.btn_done_time);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDone.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTimeSelected(selectedTime, hasTime);
            }
            dialog.dismiss();
        });
        
        // TODO: Add clock face interaction for custom time selection
        setupClockInteraction(view);
    }
    
    private void setTime(String time) {
        hasTime = true;
        selectedTime = time;
        updateTimeDisplay();
    }
    
    private void updateTimeDisplay() {
        if (hasTime) {
            tvSelectedTime.setText(selectedTime);
        } else {
            tvSelectedTime.setText("--:--");
        }
    }
    
    private void setupClockInteraction(View view) {
        // TODO: Implement touch interaction with clock face
        // This would involve handling touch events on the clock circle
        // and calculating the time based on touch position
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
