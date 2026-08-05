package com.dodo.yash808;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;
import org.json.JSONArray;

public class TodoWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, android.os.Bundle newOptions) {
        updateWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        
        if ("UPDATE_TODO_WIDGET".equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, TodoWidgetProvider.class));
            for (int id : ids) {
                updateWidget(context, manager, id);
            }
        } else if ("WIDGET_CHANGE_THEME".equals(action)) {
            // Theme cycle karne ka logic
            SharedPreferences prefs = context.getSharedPreferences("todo_data", Context.MODE_PRIVATE);
            String currentTheme = prefs.getString("widgetTheme", "auto");
            String newTheme = "auto";
            if (currentTheme.equals("auto")) newTheme = "light";
            else if (currentTheme.equals("light")) newTheme = "dark";
            else if (currentTheme.equals("dark")) newTheme = "auto";
            
            prefs.edit().putString("widgetTheme", newTheme).apply();
            
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, TodoWidgetProvider.class));
            for (int id : ids) {
                updateWidget(context, manager, id);
            }
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_todo);
        SharedPreferences prefs = context.getSharedPreferences("todo_data", Context.MODE_PRIVATE);
        
        // Theme determine karo
        String widgetTheme = prefs.getString("widgetTheme", "auto");
        boolean isAppDark = "true".equals(prefs.getString("darkMode", "false"));
        boolean isDark;
        
        if (widgetTheme.equals("light")) isDark = false;
        else if (widgetTheme.equals("dark")) isDark = true;
        else isDark = isAppDark; // Auto mode
        
        int bgColor = isDark ? 0xFF0D0D0D : 0xFFFFFFFF;
        int titleColor = isDark ? 0xFF8AB4F8 : 0xFF1A73E8;
        int emptyColor = isDark ? 0xFF9AA0A6 : 0xFF9AA0A6;
        int btnColor = isDark ? 0xFF9AA0A6 : 0xFF5f6368;

        views.setInt(R.id.widgetRoot, "setBackgroundColor", bgColor);
        views.setTextColor(R.id.widgetTitle, titleColor);
        views.setTextColor(R.id.widgetEmpty, emptyColor);
        
        // Theme button ka text aur color set karo
        String themeText = "🎨 " + (widgetTheme.equals("light") ? "Light" : widgetTheme.equals("dark") ? "Dark" : "Auto");
        views.setTextViewText(R.id.widgetThemeBtn, themeText);
        views.setTextColor(R.id.widgetThemeBtn, btnColor);

        // Theme button par click listener
        Intent themeIntent = new Intent(context, TodoWidgetProvider.class);
        themeIntent.setAction("WIDGET_CHANGE_THEME");
        themeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent themePI = PendingIntent.getBroadcast(context, appWidgetId, themeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetThemeBtn, themePI);

        // List setup
        boolean hasTasks = false;
        try {
            JSONArray tasks = new JSONArray(prefs.getString("tasks", "[]"));
            for (int i = 0; i < tasks.length(); i++) {
                if (!tasks.getJSONObject(i).optBoolean("done", false)) {
                    hasTasks = true;
                    break;
                }
            }
            if (!hasTasks) {
                JSONArray entries = new JSONArray(prefs.getString("entries", "[]"));
                if (entries.length() > 0) hasTasks = true; 
            }
        } catch (Exception e) {}

        if (hasTasks) {
            views.setViewVisibility(R.id.widgetEmpty, android.view.View.GONE);
            views.setViewVisibility(R.id.widgetListView, android.view.View.VISIBLE);
            
            Intent intent = new Intent(context, TodoWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.widgetListView, intent);
        } else {
            views.setViewVisibility(R.id.widgetEmpty, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widgetListView, android.view.View.GONE);
        }

        Intent openIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (openIntent != null) {
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);
        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListView);
    }
}