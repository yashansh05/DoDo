package com.dodo.yash808;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TodoWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private JSONArray undoneTasks;
    private boolean isDark;

    public TodoWidgetFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDataSetChanged() {
        SharedPreferences prefs = context.getSharedPreferences("todo_data", Context.MODE_PRIVATE);
        
        // Widget theme check karo
        String widgetTheme = prefs.getString("widgetTheme", "auto");
        boolean isAppDark = "true".equals(prefs.getString("darkMode", "false"));
        
        if (widgetTheme.equals("light")) isDark = false;
        else if (widgetTheme.equals("dark")) isDark = true;
        else isDark = isAppDark;
        
        String tasksJson = prefs.getString("tasks", "[]");
        String entriesJson = prefs.getString("entries", "[]");
        undoneTasks = new JSONArray();
        
        try {
            JSONArray tasks = new JSONArray(tasksJson);
            JSONArray entries = new JSONArray(entriesJson);
            JSONArray revTasks = generateReviewTasks(entries);
            
            for (int i = 0; i < tasks.length(); i++) {
                JSONObject task = tasks.getJSONObject(i);
                if (!task.optBoolean("done", false)) undoneTasks.put(task);
            }
            for (int i = 0; i < revTasks.length(); i++) {
                JSONObject task = revTasks.getJSONObject(i);
                if (!task.optBoolean("done", false)) undoneTasks.put(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONArray generateReviewTasks(JSONArray entries) {
        JSONArray reviewTasksList = new JSONArray();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(today.getTime());
        
        int[] intervals = {1, 3, 7, 14, 30, 60, 90, 180, 365};

        for (int i = 0; i < entries.length(); i++) {
            try {
                JSONObject entry = entries.getJSONObject(i);
                String dateStr = entry.optString("date", "");
                if (dateStr.isEmpty()) continue;
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                java.util.Date parsedDate = sdf.parse(dateStr);
                if (parsedDate == null) continue;
                
                Calendar entryCal = Calendar.getInstance();
                entryCal.setTime(parsedDate);
                entryCal.set(Calendar.HOUR_OF_DAY, 0);
                entryCal.set(Calendar.MINUTE, 0);
                entryCal.set(Calendar.SECOND, 0);
                entryCal.set(Calendar.MILLISECOND, 0);
                
                long diffMillis = today.getTimeInMillis() - entryCal.getTimeInMillis();
                int daysDiff = (int) (diffMillis / (1000 * 60 * 60 * 24));
                
                JSONArray reviewed = entry.optJSONArray("reviewedIntervals");
                if (reviewed == null) reviewed = new JSONArray();
                
                String content = entry.optString("content", "");
                String name = "Review: " + (content.length() > 60 ? content.substring(0, 60) + "..." : content);
                
                for (int interval : intervals) {
                    if (daysDiff >= interval) {
                        JSONObject reviewRecord = null;
                        for (int j = 0; j < reviewed.length(); j++) {
                            JSONObject r = reviewed.getJSONObject(j);
                            if (r.optInt("interval") == interval) {
                                reviewRecord = r;
                                break;
                            }
                        }
                        
                        JSONObject revTask = new JSONObject();
                        revTask.put("id", "rev-" + entry.optLong("id", 0) + "-" + interval);
                        revTask.put("name", name);
                        revTask.put("isReview", true);
                        revTask.put("hours", 0.5);
                        revTask.put("priority", "review");
                        
                        if (reviewRecord == null) {
                            revTask.put("done", false);
                            reviewTasksList.put(revTask);
                            break;
                        } else {
                            revTask.put("done", true);
                            reviewTasksList.put(revTask);
                            String doneDate = reviewRecord.optString("doneDate", "");
                            if (!doneDate.equals(todayStr)) {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {}
        }
        return reviewTasksList;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_item);
        try {
            JSONObject task = undoneTasks.getJSONObject(position);
            String name = task.optString("name", "Untitled");
            if (name.length() > 40) name = name.substring(0, 37) + "...";
            
            String priority = task.optString("priority", "medium");
            String icon;
            if (task.optBoolean("isReview", false)) icon = "♻️ ";
            else if (priority.equals("high")) icon = "🟠 ";
            else if (priority.equals("medium")) icon = "🟡 ";
            else if (priority.equals("low")) icon = "🟢 ";
            else icon = "• ";

            double hours = task.optDouble("hours", 0);
            String hoursStr = (hours > 0) ? " [" + hours + "h]" : "";
            
            int textColor = isDark ? 0xFFE8EAED : 0xFF202124;
            
            views.setTextViewText(R.id.widgetItemText, icon + name + hoursStr);
            views.setTextColor(R.id.widgetItemText, textColor);
            
            Intent fillInIntent = new Intent();
            android.app.PendingIntent pi = android.app.PendingIntent.getActivity(context, 0, 
                context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()), 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetItemText, pi);
            
        } catch (Exception e) {}
        return views;
    }

    @Override
    public int getCount() {
        return undoneTasks == null ? 0 : undoneTasks.length();
    }

    @Override
    public long getItemId(int position) { return position; }
    @Override
    public boolean hasStableIds() { return true; }
    @Override
    public RemoteViews getLoadingView() { return null; }
    @Override
    public int getViewTypeCount() { return 1; }
    @Override
    public void onDestroy() {}
}