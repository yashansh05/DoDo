package com.dodo.yash808;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class TodoWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TodoWidgetFactory(getApplicationContext());
    }
}