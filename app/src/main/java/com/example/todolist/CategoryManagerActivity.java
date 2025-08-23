package com.example.todolist;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.example.todolist.util.SettingsManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
public class CategoryManagerActivity extends AppCompatActivity implements 
    CategoryAdapter.OnCategoryClickListener, 
    CategoryService.CategoryUpdateListener {
    private RecyclerView recyclerCategories;
    private CategoryAdapter categoryAdapter;
    private List<Category> categories;
    private CategoryService categoryService;
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
        recyclerCategories = findViewById(R.id.recycler_categories);
        categories = new ArrayList<>();
        // Initialize CategoryService
        categoryService = new CategoryService(this, null, this);
        btnBack.setOnClickListener(v -> finish());
    }
    private void setupRecyclerView() {
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter(categories, this);
        recyclerCategories.setAdapter(categoryAdapter);
        // Add ItemTouchHelper for drag & drop
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                // Don't allow moving the "Add new" item
                if (fromPosition == categories.size() - 1 || toPosition == categories.size() - 1) {
                    return false;
                }
                Collections.swap(categories, fromPosition, toPosition);
                categoryAdapter.notifyItemMoved(fromPosition, toPosition);
                // Save new order to database
                saveCategoryOrder();
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerCategories);
    }
    private void loadCategories() {
        // Use CategoryService to load categories
        categoryService.loadCategories();
    }
    @Override
    public void onCategoryClick(Category category) {
        if ("-1".equals(category.getId())) {
            // This is the "Add new" item
            showCreateCategoryDialog();
        } else {
            // Regular category click
            Toast.makeText(this, getString(R.string.category_clicked_toast, category.getName()), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onCategoryMenuClick(Category category) {
        showCategoryMenu(category.getName());
    }
    private void showCreateCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_category, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        EditText editCategoryName = dialogView.findViewById(R.id.edit_category_name);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnSave = dialogView.findViewById(R.id.btn_save);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_char_count);
        // Update character count
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
                // Create new category using CategoryService
                Category newCategory = new Category();
                newCategory.setName(categoryName);
                newCategory.setColor("#4285F4");
                newCategory.setSortOrder(categories.size() - 1); // -1 because last item is "Add new"
                newCategory.setIsDefault(false);
                categoryService.createCategory(newCategory, new CategoryService.CategoryOperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(CategoryManagerActivity.this, getString(R.string.category_created_toast), Toast.LENGTH_SHORT).show();
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
    private void showCategoryMenu(String categoryName) {
        // Show options for edit/delete category
        Toast.makeText(this, getString(R.string.category_menu_toast, categoryName), Toast.LENGTH_SHORT).show();
    }
    private void saveCategoryOrder() {
        // Update sort order for all categories using CategoryService
        for (int i = 0; i < categories.size() - 1; i++) {
            Category category = categories.get(i);
            category.setSortOrder(i);
            categoryService.updateCategory(category, null); // Update without callback for batch operation
        }
    }
    // CategoryService.CategoryUpdateListener implementation
    @Override
    public void onCategoriesUpdated() {
        runOnUiThread(() -> {
            // Get updated categories from service
            List<Category> updatedCategories = categoryService.getCategories();
            categories.clear();
            categories.addAll(updatedCategories);
            // Add "Create new" item at the end
            Category addNewItem = new Category();
            addNewItem.setName("");
            addNewItem.setId("-1"); // Special ID for "Add new" item
            categories.add(addNewItem);
            categoryAdapter.notifyDataSetChanged();
        });
    }
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
        });
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
