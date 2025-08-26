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
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
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
        categoryService = new CategoryService(this, this);
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

                if (fromPosition == categories.size() - 1 || toPosition == categories.size() - 1) {
                    return false;
                }

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
                        });
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
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
        showCategoryDialog(null);
    }
    
    private void showCategoryDialog(Category existingCategory) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_category, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText editCategoryName = dialogView.findViewById(R.id.edit_category_name);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnSave = dialogView.findViewById(R.id.btn_save);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_char_count);
        TextView tvDefaultLabel = dialogView.findViewById(R.id.tv_default_label);

        View colorSelector = dialogView.findViewById(R.id.color_selector);
        View selectedColorView = dialogView.findViewById(R.id.selected_color_view);
        LinearLayout colorPalette = dialogView.findViewById(R.id.color_palette);

        View colorRed = dialogView.findViewById(R.id.color_red);
        View colorOrange = dialogView.findViewById(R.id.color_orange);
        View colorGreen = dialogView.findViewById(R.id.color_green);
        View colorTeal = dialogView.findViewById(R.id.color_teal);
        View colorBlue = dialogView.findViewById(R.id.color_blue);
        View colorPurple = dialogView.findViewById(R.id.color_purple);
        View colorDefault = dialogView.findViewById(R.id.color_default);
        final String[] colorValues = {
            "#F44336", 
            "#FF9800", 
            "#4CAF50", 
            "#009688", 
            "#2196F3", 
            "#9C27B0", 
            "#4285F4"  
        };
        
        final String[] selectedColor = {colorValues[6]}; 
        if (existingCategory != null) {
            tvDialogTitle.setText("Chỉnh sửa danh mục");
            editCategoryName.setText(existingCategory.getName());
            tvCharCount.setText(existingCategory.getName().length() + "/50");
            if (existingCategory.getColor() != null) {
                selectedColor[0] = existingCategory.getColor();
                selectedColorView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(existingCategory.getColor())));
                // Ẩn chữ "Mặc định" nếu không phải màu mặc định
                if (!existingCategory.getColor().equals(colorValues[6])) {
                    tvDefaultLabel.setVisibility(View.GONE);
                }
            }
        }
        
        // Text change listener
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
        
        // Color selector click listener
        colorSelector.setOnClickListener(v -> {
            if (colorPalette.getVisibility() == View.GONE) {
                colorPalette.setVisibility(View.VISIBLE);
            } else {
                colorPalette.setVisibility(View.GONE);
            }
        });
        
        // Color option click listeners
        View.OnClickListener colorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String color = "";
                boolean isDefault = false;
                
                if (v == colorRed) color = colorValues[0];
                else if (v == colorOrange) color = colorValues[1];
                else if (v == colorGreen) color = colorValues[2];
                else if (v == colorTeal) color = colorValues[3];
                else if (v == colorBlue) color = colorValues[4];
                else if (v == colorPurple) color = colorValues[5];
                else if (v == colorDefault) {
                    color = colorValues[6];
                    isDefault = true;
                }
                
                selectedColor[0] = color;
                selectedColorView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(color)));
                if (isDefault) {
                    tvDefaultLabel.setVisibility(View.VISIBLE);
                } else {
                    tvDefaultLabel.setVisibility(View.GONE);
                }
                
                colorPalette.setVisibility(View.GONE);
            }
        };
        
        colorRed.setOnClickListener(colorClickListener);
        colorOrange.setOnClickListener(colorClickListener);
        colorGreen.setOnClickListener(colorClickListener);
        colorTeal.setOnClickListener(colorClickListener);
        colorBlue.setOnClickListener(colorClickListener);
        colorPurple.setOnClickListener(colorClickListener);
        colorDefault.setOnClickListener(colorClickListener);

        // Button listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String categoryName = editCategoryName.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                if (existingCategory != null) {
                    // Update existing category
                    existingCategory.setName(categoryName);
                    existingCategory.setColor(selectedColor[0]);
                    categoryService.updateCategory(existingCategory, new CategoryService.CategoryOperationCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                loadCategories();
                                dialog.dismiss();
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                android.widget.Toast.makeText(CategoryManagerActivity.this, 
                                    "Lỗi cập nhật danh mục: " + error, android.widget.Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    // Create new category
                    Category newCategory = new Category();
                    newCategory.setName(categoryName);
                    newCategory.setColor(selectedColor[0]);
                    newCategory.setSortOrder(categories.size() - 1);
                    newCategory.setIsDefault(false);
                    categoryService.createCategory(newCategory, new CategoryService.CategoryOperationCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                loadCategories();
                                dialog.dismiss();
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                android.widget.Toast.makeText(CategoryManagerActivity.this, 
                                    "Lỗi tạo danh mục: " + error, android.widget.Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            }
        });
        
        dialog.show();
    }
    private void showCategoryMenu(Category category, View anchorView) {
        if (category == null || anchorView == null) return;
        
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.category_menu, popup.getMenu());
        MenuItem deleteItem = popup.getMenu().findItem(R.id.action_delete_category);
        if (deleteItem != null) {
            deleteItem.setVisible(true);
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
        showCategoryDialog(category);
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
                        }
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
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
