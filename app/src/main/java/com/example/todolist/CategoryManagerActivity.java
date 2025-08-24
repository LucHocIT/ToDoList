package com.example.todolist;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.adapter.CategoryAdapter;
import com.example.todolist.model.Category;
import com.example.todolist.service.CategoryService;
import com.example.todolist.service.TaskService;
import com.example.todolist.util.SettingsManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
public class CategoryManagerActivity extends AppCompatActivity implements 
    CategoryAdapter.OnCategoryClickListener, 
    CategoryService.CategoryUpdateListener,
    TaskService.TaskUpdateListener {
    
    private RecyclerView recyclerCategories;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories;
    private CategoryService categoryService;
    private TaskService taskService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);
        initViews();
        setupRecyclerView();
        loadCategories();
    }
    private void initViews() {
        ImageView btnBack = findViewById(R.id.btn_back);
        ImageView btnReset = findViewById(R.id.btn_reset);
        recyclerCategories = findViewById(R.id.recycler_categories);
        
        categories = new ArrayList<>();
        categoryService = new CategoryService(this, null, this);
        taskService = new TaskService(this, this);
        
        btnBack.setOnClickListener(v -> finish());
        btnReset.setOnClickListener(v -> showResetDialog());
    }
    private void setupRecyclerView() {
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter(categories, this);
        categoryAdapter.setTaskService(taskService); 
        recyclerCategories.setAdapter(categoryAdapter);
        
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                // Don't allow moving the "Add New" item (last item)
                if (fromPosition == categories.size() - 1 || toPosition == categories.size() - 1) {
                    return false;
                }
                
                // Don't allow moving items to invalid positions
                if (fromPosition < 0 || toPosition < 0 || 
                    fromPosition >= categories.size() - 1 || toPosition >= categories.size() - 1) {
                    return false;
                }
                
                Category movedCategory = categories.remove(fromPosition);
                categories.add(toPosition, movedCategory);
                categoryAdapter.notifyItemMoved(fromPosition, toPosition);
                saveCategoryOrder();
                return true;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
            
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
            
            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerCategories);
    }
    private void loadCategories() {
        categoryService.loadCategories();
        // Also load tasks to ensure task counts are updated
        if (taskService != null) {
            taskService.loadTasks();
        }
    }
    
    private void resetDefaultCategories() {
        categoryService.clearAllCategories(new CategoryService.CategoryOperationCallback() {
            @Override
            public void onSuccess() {
                categoryService.initializeDefaultCategories(new CategoryService.CategoryOperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            loadCategories();
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(CategoryManagerActivity.this, "Lỗi tạo danh mục mặc định: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CategoryManagerActivity.this, "Lỗi xóa danh mục: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset danh mục")
                .setMessage("Bạn có chắc chắn muốn xóa tất cả danh mục và reset về 3 danh mục mặc định?\n\n" +
                           "Danh mục mặc định: Cá nhân, Yêu thích, Công việc\n\n" +
                           "Lưu ý: Tất cả nhiệm vụ sẽ được chuyển về danh mục \"Cá nhân\".")
                .setPositiveButton("Reset", (dialog, which) -> {
                    resetDefaultCategories();
                })
                .setNegativeButton("Hủy", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    @Override
    public void onCategoryClick(Category category) {
        if ("-1".equals(category.getId())) {
            showCreateCategoryDialog();
        }
    }
    
    @Override
    public void onCategoryMenuClick(Category category, View anchorView) {
        showCategoryMenu(category, anchorView);
    }
    private void showCreateCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_category, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        // Fix dialog position and background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
        EditText editCategoryName = dialogView.findViewById(R.id.edit_category_name);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnSave = dialogView.findViewById(R.id.btn_save);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_char_count);
        editCategoryName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/50");
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String categoryName = editCategoryName.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                Category newCategory = new Category();
                newCategory.setName(categoryName);
                newCategory.setColor("#4285F4");
                newCategory.setSortOrder(categories.size() - 1);
                newCategory.setIsDefault(false);
                categoryService.createCategory(newCategory, new CategoryService.CategoryOperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                        });
                    }
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(CategoryManagerActivity.this, "Lỗi tạo thể loại: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, getString(R.string.category_name_required_toast), Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
    private void showCategoryMenu(Category category, View anchorView) {
        if (category == null || anchorView == null) return;
        
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.category_menu, popup.getMenu());
        
        // Allow deletion of all categories including default ones
        // Users should be able to delete any category they want
        MenuItem deleteItem = popup.getMenu().findItem(R.id.action_delete_category);
        if (deleteItem != null) {
            deleteItem.setVisible(true); // Always show delete option
        }
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_category) {
                showEditCategoryDialog(category);
                return true;
            } else if (itemId == R.id.action_delete_category) {
                showDeleteCategoryDialog(category);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void showEditCategoryDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_category, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        // Fix dialog position and background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
        
        EditText editCategoryName = dialogView.findViewById(R.id.edit_category_name);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnSave = dialogView.findViewById(R.id.btn_save);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_char_count);
        TextView titleText = dialogView.findViewById(R.id.tv_dialog_title);
        
        editCategoryName.setText(category.getName());
        titleText.setText("Chỉnh sửa danh mục");
        btnSave.setText("Cập nhật");
        tvCharCount.setText(category.getName().length() + "/50");
        
        editCategoryName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/50");
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newCategoryName = editCategoryName.getText().toString().trim();
            if (!newCategoryName.isEmpty() && !newCategoryName.equals(category.getName())) {
                category.setName(newCategoryName);
                categoryService.updateCategory(category, new CategoryService.CategoryOperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                        });
                    }
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(CategoryManagerActivity.this, "Lỗi cập nhật: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else if (newCategoryName.isEmpty()) {
                Toast.makeText(this, getString(R.string.category_name_required_toast), Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
            }
        });
        
        dialog.show();
    }
    
    private void showDeleteCategoryDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xóa danh mục")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục \"" + category.getName() + "\"?\n\n" +
                           "Các nhiệm vụ trong danh mục này sẽ được chuyển về danh mục mặc định.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    categoryService.deleteCategory(category, new CategoryService.CategoryOperationCallback() {
                        @Override
                        public void onSuccess() {
                            // No toast needed, just delete silently
                        }
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(CategoryManagerActivity.this, "Lỗi xóa danh mục: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Hủy", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void saveCategoryOrder() {
        for (int i = 0; i < categories.size() - 1; i++) {
            Category category = categories.get(i);
            category.setSortOrder(i);
            categoryService.updateCategory(category, null); 
        }
    }

    @Override
    public void onCategoriesUpdated() {
        runOnUiThread(() -> {
            List<Category> updatedCategories = categoryService.getCategories();
            categories.clear();
            categories.addAll(updatedCategories);
            Category addNewItem = new Category();
            addNewItem.setName("");
            addNewItem.setId("-1"); 
            categories.add(addNewItem);
            categoryAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onTasksUpdated() {
        runOnUiThread(() -> {
            if (categoryAdapter != null) {
                categoryAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskService != null) {
            taskService.cleanup();
        }
        if (categoryService != null) {
            categoryService.cleanup();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }
    private Context updateBaseContextLocale(Context context) {
        String languageName = SettingsManager.getLanguage(context);
        String languageCode;
        if (languageName.equals("English")) {
            languageCode = "en";
        } else {
            languageCode = "vi";
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }
}
