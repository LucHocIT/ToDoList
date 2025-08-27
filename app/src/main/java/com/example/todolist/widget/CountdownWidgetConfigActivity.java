package com.example.todolist.widget;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.todolist.R;
import com.example.todolist.adapter.SimpleIconAdapter;
import com.example.todolist.model.CountdownEvent;

import java.util.Calendar;

public class CountdownWidgetConfigActivity extends Activity {
    
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private int selectedIconResource = R.drawable.lich;
    private Calendar selectedDate;
    private String calculationType = "Ngày còn lại";
    
    // UI Components
    private LinearLayout mainScreen;
    private LinearLayout iconSelectionScreen;
    private ImageView selectedIcon;
    private ImageView previewIcon;
    private EditText eventNameEdit;
    private TextView characterCount;
    private Spinner calculationSpinner;
    private TextView dateText;
    private GridView iconGrid;
    
    // Icon resources - 8 icons như ảnh mẫu
    private int[] iconResources = {
        R.drawable.lich,           // Calendar
        R.drawable.tim,            // Heart
        R.drawable.grift,          // Gift
        R.drawable.bubble,         // Balloons
        R.drawable.banhsinhnhat,   // Cake
        R.drawable.dulich,         // Vacation
        R.drawable.baikiemtra,     // Ring/Exam
        R.drawable.nhan            // Ring
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown_widget_config);
        
        // Get widget ID from intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        
        // Set result to CANCELED by default
        setResult(RESULT_CANCELED);
        
        initViews();
        setupUI();
        setupClickListeners();
        
        // Initialize with current date
        selectedDate = Calendar.getInstance();
        updateDateDisplay();
    }
    
    private void initViews() {
        mainScreen = findViewById(R.id.mainScreen);
        iconSelectionScreen = findViewById(R.id.iconSelectionScreen);
        selectedIcon = findViewById(R.id.selectedIcon);
        previewIcon = findViewById(R.id.previewIcon);
        eventNameEdit = findViewById(R.id.et_event_title);
        characterCount = findViewById(R.id.characterCount);
        calculationSpinner = findViewById(R.id.spinner_calculation_type);
        dateText = findViewById(R.id.tv_selected_date);
        iconGrid = findViewById(R.id.grid_view_icons);
    }
    
    private void setupUI() {
        // Setup calculation type spinner
        String[] calculationTypes = {"Ngày còn lại", "Số ngày"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, calculationTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        calculationSpinner.setAdapter(adapter);
        
        calculationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculationType = calculationTypes[position];
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Setup character counter
        eventNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                characterCount.setText(s.length() + "/30");
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Setup icon grid
        SimpleIconAdapter iconAdapter = new SimpleIconAdapter(this, iconResources);
        iconGrid.setAdapter(iconAdapter);
        
        iconGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedIconResource = iconResources[position];
                selectedIcon.setImageResource(selectedIconResource);
                previewIcon.setImageResource(selectedIconResource);
                
                // Cập nhật selected position trong adapter
                ((SimpleIconAdapter) iconGrid.getAdapter()).setSelectedPosition(position);
            }
        });
    }
    
    private void setupClickListeners() {
        // Back button
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iconSelectionScreen.getVisibility() == View.VISIBLE) {
                    // Nếu đang ở màn hình chọn icon, quay về màn hình chính
                    showMainScreen();
                } else {
                    // Nếu đang ở màn hình chính, thoát activity
                    finish();
                }
            }
        });
        
        // Icon container click - chuyển sang màn hình chọn icon
        View iconContainer = findViewById(R.id.iconContainer);
        iconContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showIconSelectionScreen();
            }
        });
        
        // Date picker
        dateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
        
        // Save button main screen
        Button saveButton = findViewById(R.id.btn_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWidget();
            }
        });
        
        // Save button icon screen
        Button saveIconButton = findViewById(R.id.btn_save_icon);
        saveIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMainScreen();
            }
        });
    }
    
    private void showMainScreen() {
        mainScreen.setVisibility(View.VISIBLE);
        iconSelectionScreen.setVisibility(View.GONE);
    }
    
    private void showIconSelectionScreen() {
        mainScreen.setVisibility(View.GONE);
        iconSelectionScreen.setVisibility(View.VISIBLE);
        previewIcon.setImageResource(selectedIconResource);
    }
    
    private void showDatePicker() {
        Calendar currentDate = Calendar.getInstance();
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedDate.set(year, month, dayOfMonth);
                        updateDateDisplay();
                    }
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.show();
    }
    
    private void updateDateDisplay() {
        String dateString = String.format("%04d/%02d/%02d",
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.DAY_OF_MONTH));
        dateText.setText(dateString);
    }
    
    private void saveWidget() {
        String eventName = eventNameEdit.getText().toString().trim();
        
        if (eventName.isEmpty()) {
            eventName = "Sự kiện";
        }
        
        // Create countdown event
        CountdownEvent event = new CountdownEvent(
                eventName,
                selectedIconResource,
                selectedDate.getTime(),
                calculationType.equals("Ngày còn lại") ? 1 : 0
        );
        
        // Save widget data
        saveWidgetData(appWidgetId, event);
        
        // Update widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        CountdownWidgetProvider.updateCountdownWidget(this, appWidgetManager, appWidgetId);
        
        // Return success
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
    
    private void saveWidgetData(int widgetId, CountdownEvent event) {
        SharedPreferences prefs = getSharedPreferences("CountdownWidgetPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putString("countdown_widget_" + widgetId + "_title", event.getTitle());
        editor.putLong("countdown_widget_" + widgetId + "_date", event.getTargetDate().getTime());
        editor.putInt("countdown_widget_" + widgetId + "_icon", event.getIconResourceId());
        editor.putInt("countdown_widget_" + widgetId + "_calc_type", event.getCalculationType());
        
        editor.apply();
    }
    
    @Override
    public void onBackPressed() {
        if (iconSelectionScreen.getVisibility() == View.VISIBLE) {
            showMainScreen();
        } else {
            super.onBackPressed();
        }
    }
}
