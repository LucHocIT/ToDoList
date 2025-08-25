package com.example.todolist.util;

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;
import android.widget.NumberPicker;
import com.example.todolist.R;

public class TimeSelectionDialog extends Dialog {
    
    public interface OnTimeSelectedListener {
        void onTimeSelected(int minutes);
        void onCancel();
    }
    
    private OnTimeSelectedListener listener;
    private NumberPicker numberPicker;
    private int[] timeValues = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 90, 120, 150}; // Available time options

    public TimeSelectionDialog(Context context, OnTimeSelectedListener listener) {
        super(context);
        this.listener = listener;
        initDialog();
    }
    
    private void initDialog() {
        setContentView(R.layout.dialog_time_selector);
        setCancelable(true);
        
        // Setup NumberPicker
        numberPicker = findViewById(R.id.number_picker_time);
        setupNumberPicker();
        
        // Setup buttons
        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnStart = findViewById(R.id.btn_start);
        
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
            dismiss();
        });
        
        btnStart.setOnClickListener(v -> {
            if (listener != null) {
                int selectedValue = timeValues[numberPicker.getValue()];
                listener.onTimeSelected(selectedValue);
            }
            dismiss();
        });
    }
    
    private void setupNumberPicker() {
        // Create display values
        String[] displayValues = new String[timeValues.length];
        for (int i = 0; i < timeValues.length; i++) {
            displayValues[i] = String.valueOf(timeValues[i]);
        }
        
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(timeValues.length - 1);
        numberPicker.setDisplayedValues(displayValues);
        numberPicker.setWrapSelectorWheel(false);
        
        // Set default value to 55 minutes (index 10)
        int defaultIndex = findIndexOfValue(55);
        if (defaultIndex != -1) {
            numberPicker.setValue(defaultIndex);
        }
    }
    
    private int findIndexOfValue(int value) {
        for (int i = 0; i < timeValues.length; i++) {
            if (timeValues[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
