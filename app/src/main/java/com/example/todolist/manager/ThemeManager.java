package com.example.todolist.manager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.todolist.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
public class ThemeManager {
    public enum ThemeColor {
        SKY_BLUE("Sky Blue", R.color.theme_sky_blue_primary, R.color.theme_sky_blue_secondary, R.color.theme_sky_blue_light),
        GREEN("Green", R.color.theme_green_primary, R.color.theme_green_secondary, R.color.theme_green_light),
        PURPLE("Purple", R.color.theme_purple_primary, R.color.theme_purple_secondary, R.color.theme_purple_light),
        ORANGE("Orange", R.color.theme_orange_primary, R.color.theme_orange_secondary, R.color.theme_orange_light),
        RED("Red", R.color.theme_red_primary, R.color.theme_red_secondary, R.color.theme_red_light),
        TEAL("Teal", R.color.theme_teal_primary, R.color.theme_teal_secondary, R.color.theme_teal_light),
        INDIGO("Indigo", R.color.theme_indigo_primary, R.color.theme_indigo_secondary, R.color.theme_indigo_light),
        PINK("Pink", R.color.theme_pink_primary, R.color.theme_pink_secondary, R.color.theme_pink_light);
        private final String name;
        private final int primaryColorRes;
        private final int secondaryColorRes;
        private final int lightColorRes;
        ThemeColor(String name, int primaryColorRes, int secondaryColorRes, int lightColorRes) {
            this.name = name;
            this.primaryColorRes = primaryColorRes;
            this.secondaryColorRes = secondaryColorRes;
            this.lightColorRes = lightColorRes;
        }
        public String getName() {
            return name;
        }
        public int getPrimaryColorRes() {
            return primaryColorRes;
        }
        public int getSecondaryColorRes() {
            return secondaryColorRes;
        }
        public int getLightColorRes() {
            return lightColorRes;
        }
    }
    public interface ThemeChangeListener {
        void onThemeChanged(ThemeColor themeColor);
    }
    private static final String PREF_NAME = "theme_preferences";
    private static final String PREF_THEME_KEY = "selected_theme";
    private AppCompatActivity activity;
    private SharedPreferences preferences;
    private ThemeChangeListener listener;
    private ThemeColor currentTheme;
    public ThemeManager(AppCompatActivity activity, ThemeChangeListener listener) {
        this.activity = activity;
        this.listener = listener;
        this.preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadSavedTheme();
    }
    private void loadSavedTheme() {
        String savedTheme = preferences.getString(PREF_THEME_KEY, ThemeColor.SKY_BLUE.name());
        try {
            currentTheme = ThemeColor.valueOf(savedTheme);
        } catch (IllegalArgumentException e) {
            currentTheme = ThemeColor.SKY_BLUE;
        }
    }
    public void setTheme(ThemeColor theme) {
        currentTheme = theme;
        preferences.edit().putString(PREF_THEME_KEY, theme.name()).apply();
        // Apply theme immediately
        applyCurrentTheme();
        if (listener != null) {
            listener.onThemeChanged(theme);
        }
    }
    public ThemeColor getCurrentTheme() {
        return currentTheme;
    }
    public void applyCurrentTheme() {
        applyThemeToUI(currentTheme);
    }
    private void applyThemeToUI(ThemeColor theme) {
        int primaryColor = activity.getResources().getColor(theme.getPrimaryColorRes(), null);
        int secondaryColor = activity.getResources().getColor(theme.getSecondaryColorRes(), null);
        int lightColor = activity.getResources().getColor(theme.getLightColorRes(), null);
        // Apply to FloatingActionButton
        FloatingActionButton fab = activity.findViewById(R.id.fab_add);
        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        }
        // Apply to filter buttons
        applyThemeToButton(R.id.btn_all, primaryColor, lightColor);
        applyThemeToButton(R.id.btn_work, primaryColor, lightColor);
        applyThemeToButton(R.id.btn_personal, primaryColor, lightColor);
        applyThemeToButton(R.id.btn_favorite, primaryColor, lightColor);
        // Apply to navigation icons
        ImageView btnMenu = activity.findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            btnMenu.setColorFilter(primaryColor);
        }
        // Apply to bottom navigation
        applyThemeToBottomNavigation(primaryColor);
        // Update navigation drawer gradient
        updateNavigationDrawerTheme(theme);
        // Apply theme to task items background (dynamically)
        updateTaskItemsTheme(lightColor);
    }
    private void applyThemeToButton(int buttonId, int primaryColor, int lightColor) {
        MaterialButton button = activity.findViewById(buttonId);
        if (button != null) {
            // Set text color
            button.setTextColor(primaryColor);
            // Set background tint for selected state
            ColorStateList backgroundTint = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_selected},
                    new int[]{}
                },
                new int[]{
                    lightColor,
                    Color.TRANSPARENT
                }
            );
            button.setBackgroundTintList(backgroundTint);
        }
    }
    private void applyThemeToBottomNavigation(int primaryColor) {
        // Apply to bottom navigation icons
        View btnNavMenu = activity.findViewById(R.id.btn_nav_menu);
        View btnNavTasks = activity.findViewById(R.id.btn_nav_tasks);
        View btnNavCalendar = activity.findViewById(R.id.btn_nav_calendar);
        applyThemeToNavButton(btnNavMenu, primaryColor);
        applyThemeToNavButton(btnNavTasks, primaryColor);
        applyThemeToNavButton(btnNavCalendar, primaryColor);
    }
    private void applyThemeToNavButton(View navButton, int primaryColor) {
        if (navButton instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout layout = (android.widget.LinearLayout) navButton;
            if (layout.getChildCount() >= 2) {
                View iconView = layout.getChildAt(0);
                View textView = layout.getChildAt(1);
                if (iconView instanceof ImageView) {
                    ((ImageView) iconView).setColorFilter(primaryColor);
                }
                if (textView instanceof TextView) {
                    ((TextView) textView).setTextColor(primaryColor);
                }
            }
        }
    }
    private void updateNavigationDrawerTheme(ThemeColor theme) {
        // Update the navigation drawer header background
        android.widget.RelativeLayout navHeader = activity.findViewById(R.id.nav_header_container);
        if (navHeader != null) {
            int gradientRes = getThemeGradientResource(theme);
            navHeader.setBackgroundResource(gradientRes);
        }
    }
    private int getThemeGradientResource(ThemeColor theme) {
        switch (theme) {
            case GREEN:
                return R.drawable.nav_header_gradient_green;
            case PURPLE:
                return R.drawable.nav_header_gradient_purple;
            case ORANGE:
                return R.drawable.nav_header_gradient_orange;
            case RED:
                return R.drawable.nav_header_gradient_red;
            case TEAL:
                return R.drawable.nav_header_gradient_teal;
            case INDIGO:
                return R.drawable.nav_header_gradient_indigo;
            case PINK:
                return R.drawable.nav_header_gradient_pink;
            case SKY_BLUE:
            default:
                return R.drawable.nav_header_gradient;
        }
    }
    private void updateTaskItemsTheme(int lightColor) {
        int taskBgColor = lightColor;
    }
    public int getPrimaryColor() {
        return activity.getResources().getColor(currentTheme.getPrimaryColorRes(), null);
    }
    public int getSecondaryColor() {
        return activity.getResources().getColor(currentTheme.getSecondaryColorRes(), null);
    }
    public int getLightColor() {
        return activity.getResources().getColor(currentTheme.getLightColorRes(), null);
    }
}
