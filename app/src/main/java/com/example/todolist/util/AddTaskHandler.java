package com.example.todolist.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.todolist.R;
import com.example.todolist.adapter.CategorySpinnerAdapter;
import com.example.todolist.database.TodoDatabase;
import com.example.todolist.model.Category;
import com.example.todolist.model.TodoTask;
import com.example.todolist.notification.ReminderScheduler;

import java.util.Calendar;
import java.util.List;

/**
 * Lớp xử lý chung cho chức năng thêm nhiệm vụ
 * Có thể tái sử dụng trên toàn ứng dụng
 */
public class AddTaskHandler {
    
    private Context context;
    private TodoDatabase database;
    private OnTaskAddedListener listener;
    
    // Interface callback khi thêm task thành công
    public interface OnTaskAddedListener {
        void onTaskAdded(TodoTask task);
    }
    
    public AddTaskHandler(Context context, OnTaskAddedListener listener) {
        this.context = context;
        this.database = TodoDatabase.getInstance(context);
        this.listener = listener;
    }
    
    /**
     * Hiển thị dialog thêm nhiệm vụ
     * @param prefilledDate Ngày được chọn trước (có thể null)
     * @param prefilledCategory Danh mục được chọn trước (có thể null)
     */
    public void showAddTaskDialog(String prefilledDate, String prefilledCategory) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        
        // Làm trong suốt background để hiển thị góc bo tròn của CardView
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Khởi tạo các view
        EditText editTaskTitle = dialogView.findViewById(R.id.edit_task_title);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_save);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category_dialog);
        ImageView iconCalendar = dialogView.findViewById(R.id.icon_calendar_dialog);

        // Biến lưu trữ các giá trị được chọn
        final String[] selectedDate = {prefilledDate}; // Sử dụng ngày được truyền vào
        final String[] selectedTime = {"Không"}; // Thời gian mặc định
        final String[] selectedReminder = {"Không"}; // Nhắc nhở mặc định
        final String[] selectedRepeat = {"Không"}; // Lặp lại mặc định
        
        // Thiết lập spinner danh mục
        setupCategorySpinner(spinnerCategory, prefilledCategory);
        
        // Xử lý click vào icon lịch
        iconCalendar.setOnClickListener(v -> {
            showDateTimePickerDialog(new DateTimePickerDialog.OnDateTimeSelectedListener() {
                @Override
                public void onDateTimeSelected(String date, String time, String reminder, String repeat) {
                    selectedDate[0] = date;
                    selectedTime[0] = time;
                    selectedReminder[0] = reminder;
                    selectedRepeat[0] = repeat;
                }
            });
        });

        // Xử lý button Hủy
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Xử lý button Lưu
        btnSave.setOnClickListener(v -> {
            String title = editTaskTitle.getText().toString().trim();
            if (!title.isEmpty()) {
                String categoryName = getCategoryFromSpinner(spinnerCategory);
                createNewTaskWithDetails(title, categoryName, selectedDate[0], selectedTime[0], selectedReminder[0], selectedRepeat[0]);
                dialog.dismiss();
            } else {
                editTaskTitle.setError("Vui lòng nhập tiêu đề nhiệm vụ");
            }
        });

        dialog.show();
    }
    
    /**
     * Hiển thị dialog thêm nhiệm vụ với ngày mặc định là hôm nay
     */
    public void showAddTaskDialog() {
        showAddTaskDialog(null, null);
    }
    
    /**
     * Hiển thị dialog thêm nhiệm vụ với ngày được chọn trước
     */
    public void showAddTaskDialog(String prefilledDate) {
        showAddTaskDialog(prefilledDate, null);
    }
    
    /**
     * Thiết lập spinner danh mục
     */
    private void setupCategorySpinner(Spinner spinner, String prefilledCategory) {
        new Thread(() -> {
            List<Category> categories = database.categoryDao().getAllCategories();
            
            // Chạy trên UI thread để cập nhật spinner
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    CategorySpinnerAdapter adapter = new CategorySpinnerAdapter(context, categories);
                    spinner.setAdapter(adapter);
                    
                    // Chọn danh mục được chỉ định trước (nếu có)
                    if (prefilledCategory != null) {
                        for (int i = 0; i < categories.size(); i++) {
                            if (categories.get(i).getName().equals(prefilledCategory)) {
                                spinner.setSelection(i);
                                break;
                            }
                        }
                    }
                });
            }
        }).start();
    }
    
    /**
     * Lấy danh mục được chọn từ spinner
     */
    private String getCategoryFromSpinner(Spinner spinner) {
        try {
            CategorySpinnerAdapter adapter = (CategorySpinnerAdapter) spinner.getAdapter();
            Category selectedCategory = adapter.getCategory(spinner.getSelectedItemPosition());
            
            if (selectedCategory != null) {
                // If it's the default "no category" option (ID = 0), return null
                if (selectedCategory.getId() == 0 || 
                    selectedCategory.getName().equalsIgnoreCase("không có thể loại")) {
                    return null;
                }
                return selectedCategory.getName();
            }
            return null;
        } catch (Exception e) {
            Log.e("AddTaskHandler", "Error getting category from spinner: " + e.getMessage());
            return null; // Return null instead of default category
        }
    }
    
    /**
     * Hiển thị dialog chọn ngày giờ
     */
    private void showDateTimePickerDialog(DateTimePickerDialog.OnDateTimeSelectedListener listener) {
        DateTimePickerDialog dialog = new DateTimePickerDialog(context, listener);
        dialog.show();
    }
    
    /**
     * Tạo nhiệm vụ mới với các chi tiết đã chọn
     */
    private void createNewTaskWithDetails(String title, String category, String date, String time, String reminder, String repeat) {
        // Đặt ngày và thời gian
        String finalDate;
        String finalTime = null;
        
        // Nếu người dùng đã chọn ngày, sử dụng ngày đó
        if (date != null && !date.equals("Không")) {
            finalDate = date;
        } else {
            // Nếu không chọn ngày, đặt ngày hiện tại
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH bắt đầu từ 0
            int year = calendar.get(Calendar.YEAR);
            finalDate = String.format("%02d/%02d/%04d", day, month, year);
        }
        
        // Chỉ đặt thời gian nếu người dùng đã chọn thời gian cụ thể
        if (time != null && !time.equals("Không") && !time.equals("12:00")) { // 12:00 là mặc định
            finalTime = time;
        }
        
        TodoTask newTask = new TodoTask(title, "", finalDate, finalTime);
        newTask.setCategory(category);
        
        // Đặt nhắc nhở nếu không phải "Không" và có thời gian
        if (reminder != null && !reminder.equals("Không") && finalTime != null) {
            newTask.setHasReminder(true);
            newTask.setReminderType(reminder);
        }
        
        // Đặt lặp lại nếu không phải "Không"
        if (repeat != null && !repeat.equals("Không")) {
            newTask.setRepeating(true);
            newTask.setRepeatType(repeat);
        }
        
        // Thêm vào cơ sở dữ liệu
        new Thread(() -> {
            database.todoDao().insertTask(newTask);
            
            // Lên lịch thông báo nếu có reminder
            if (newTask.isHasReminder()) {
                ReminderScheduler scheduler = new ReminderScheduler(context);
                scheduler.scheduleTaskReminder(newTask);
            }
            
            // Chạy trên UI thread để cập nhật giao diện
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    // Gọi callback để thông báo task đã được thêm
                    if (listener != null) {
                        listener.onTaskAdded(newTask);
                    }
                    Toast.makeText(context, "Đã thêm nhiệm vụ mới", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
