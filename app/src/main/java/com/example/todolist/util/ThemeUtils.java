package com.example.todolist.util;

import android.content.Context;
import android.widget.ImageView;

import com.example.todolist.R;
import com.example.todolist.manager.ThemeManager;

public class ThemeUtils {
    
    public static void updateNavigationHeaderGradient(Context context, ImageView headerBackground, ThemeManager.ThemeColor theme) {
        int gradientDrawable;
        
        switch (theme) {
            case GREEN:
                gradientDrawable = R.drawable.nav_header_gradient_green;
                break;
            case PURPLE:
                gradientDrawable = R.drawable.nav_header_gradient_purple;
                break;
            case ORANGE:
                gradientDrawable = R.drawable.nav_header_gradient_orange;
                break;
            case RED:
                gradientDrawable = R.drawable.nav_header_gradient_red;
                break;
            case TEAL:
                gradientDrawable = R.drawable.nav_header_gradient_teal;
                break;
            case INDIGO:
                gradientDrawable = R.drawable.nav_header_gradient_indigo;
                break;
            case PINK:
                gradientDrawable = R.drawable.nav_header_gradient_pink;
                break;
            case SKY_BLUE:
            default:
                gradientDrawable = R.drawable.nav_header_gradient;
                break;
        }
        
        if (headerBackground != null) {
            headerBackground.setBackgroundResource(gradientDrawable);
        }
    }
    
    public static int getThemeGradientResource(ThemeManager.ThemeColor theme) {
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
}
