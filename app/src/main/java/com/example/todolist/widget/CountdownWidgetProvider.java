package com.example.todolist.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;

import com.example.todolist.R;
import com.example.todolist.model.CountdownEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CountdownWidgetProvider extends AppWidgetProvider {
    
    private static final String PREFS_NAME = "CountdownWidgetPrefs";
    private static final String PREF_PREFIX_KEY = "countdown_widget_";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateCountdownWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Xóa preferences khi widget bị xóa
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        for (int appWidgetId : appWidgetIds) {
            editor.remove(PREF_PREFIX_KEY + appWidgetId + "_title");
            editor.remove(PREF_PREFIX_KEY + appWidgetId + "_icon");
            editor.remove(PREF_PREFIX_KEY + appWidgetId + "_date");
            editor.remove(PREF_PREFIX_KEY + appWidgetId + "_calc_type");
        }
        editor.apply();
    }

    @Override
    public void onEnabled(Context context) {
        // Widget đầu tiên được tạo
    }

    @Override
    public void onDisabled(Context context) {
        // Widget cuối cùng bị xóa
    }

    public static void updateCountdownWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Lấy dữ liệu từ SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String title = prefs.getString(PREF_PREFIX_KEY + appWidgetId + "_title", "Sự kiện");
        int iconResId = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_icon", R.drawable.lich);
        
        // Sử dụng ngày được set thay vì current time
        long targetDateMillis = prefs.getLong(PREF_PREFIX_KEY + appWidgetId + "_date", -1);
        if (targetDateMillis == -1) {
            // Nếu chưa có ngày được set, tạo default 1 ngày từ bây giờ
            targetDateMillis = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        }
        
        int calcType = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_calc_type", 1);

        // Tạo CountdownEvent để tính toán
        CountdownEvent event = new CountdownEvent();
        event.setTitle(title);
        event.setIconResourceId(iconResId);
        event.setTargetDate(new Date(targetDateMillis));
        event.setCalculationType(calcType);

        // Cập nhật RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_countdown_4x1);
        
        // Set title
        views.setTextViewText(R.id.widget_countdown_title, title);
        
        // Set date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        views.setTextViewText(R.id.widget_countdown_date, dateFormat.format(event.getTargetDate()));
        
        // Set icon
        views.setImageViewResource(R.id.widget_countdown_icon, iconResId);
        
        // Set countdown
        String displayText = event.getDisplayText();
        String number = displayText.replaceAll("[^0-9]", "");
        views.setTextViewText(R.id.widget_countdown_number, number.isEmpty() ? "0" : number);
        views.setTextViewText(R.id.widget_countdown_unit, "D");

        // Thêm click listener
        Intent intent = new Intent(context, com.example.todolist.widget.CountdownWidgetConfigActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_countdown_icon, pendingIntent);

        // Cập nhật widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void saveWidgetData(Context context, int appWidgetId, String title, 
                                     int iconResId, Date targetDate, int calcType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_PREFIX_KEY + appWidgetId + "_title", title);
        editor.putInt(PREF_PREFIX_KEY + appWidgetId + "_icon", iconResId);
        editor.putLong(PREF_PREFIX_KEY + appWidgetId + "_date", targetDate.getTime());
        editor.putInt(PREF_PREFIX_KEY + appWidgetId + "_calc_type", calcType);
        editor.apply();
    }
}
