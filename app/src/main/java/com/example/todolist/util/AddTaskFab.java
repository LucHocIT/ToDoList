package com.example.todolist.util;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AddTaskFab extends FloatingActionButton {
    private AddTaskHandler addTaskHandler;
    private String prefilledDate;
    private String prefilledCategory;
    public AddTaskFab(Context context) {
        super(context);
        init();
    }
    public AddTaskFab(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public AddTaskFab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        setOnClickListener(v -> {
            if (addTaskHandler != null) {
                addTaskHandler.showAddTaskDialog(prefilledDate, prefilledCategory);
            }
        });
    }

    public void setAddTaskHandler(AddTaskHandler handler) {
        this.addTaskHandler = handler;
    }

    public void setPrefilledDate(String date) {
        this.prefilledDate = date;
    }

    public void setPrefilledCategory(String category) {
        this.prefilledCategory = category;
    }

    public void setPrefilledData(String date, String category) {
        this.prefilledDate = date;
        this.prefilledCategory = category;
    }
    
    public void clearPrefilledData() {
        this.prefilledDate = null;
        this.prefilledCategory = null;
    }
}
