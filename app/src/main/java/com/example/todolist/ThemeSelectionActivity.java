package com.example.todolist;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.adapter.ThemeAdapter;
import com.example.todolist.manager.ThemeManager;
import com.example.todolist.util.SettingsManager;
import java.util.Locale;
public class ThemeSelectionActivity extends AppCompatActivity implements ThemeAdapter.OnThemeSelectedListener {
    private ImageView btnBack;
    private RecyclerView recyclerThemes;
    private ThemeAdapter themeAdapter;
    private ThemeManager themeManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_selection);
        initViews();
        setupThemeManager();
        setupRecyclerView();
        setupClickListeners();
    }
    private void initViews() {
        btnBack = findViewById(R.id.btn_back_theme);
        recyclerThemes = findViewById(R.id.recycler_themes);
    }
    private void setupThemeManager() {
        themeManager = new ThemeManager(this, theme -> {
            if (themeAdapter != null) {
                themeAdapter.setSelectedTheme(theme);
            }
        });
    }
    private void setupRecyclerView() {
        themeAdapter = new ThemeAdapter(this, themeManager.getCurrentTheme(), this);
        recyclerThemes.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerThemes.setAdapter(themeAdapter);
    }
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
    @Override
    public void onThemeSelected(ThemeManager.ThemeColor theme) {
        themeManager.setTheme(theme);
        Toast.makeText(this, getString(R.string.theme_selected, theme.getName()), Toast.LENGTH_SHORT).show();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("theme_changed", true);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    @Override
    protected void onResume() {
        super.onResume();
        themeManager.applyCurrentTheme();
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
