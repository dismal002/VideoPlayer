package com.dismal.fireplayer.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.dismal.fireplayer.R;
import com.dismal.fireplayer.ui.FourKMainActivity;
import com.dismal.fireplayer.videoplayerui.FloatVideoService;

public class VideoAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "VideoAppWidgetProvider";

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildWidgetViews(context));
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public static RemoteViews buildWidgetViews(Context context) {
        return buildStackVideoWidget(context);
    }

    private static RemoteViews buildStackVideoWidget(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_stack_video_style);
        views.setRemoteAdapter(R.id.appwidget_stack_view, new Intent(context, StackWidgetService.class));
        views.setEmptyView(R.id.appwidget_stack_view, R.id.appwidget_empty_view);
        views.setPendingIntentTemplate(R.id.appwidget_stack_view, PendingIntent.getService(context, 0, new Intent(context, FloatVideoService.class), 134217728));
        views.setOnClickPendingIntent(R.id.appwidget_icon, PendingIntent.getActivity(context, 0, new Intent(context, FourKMainActivity.class), 134217728));
        return views;
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }
}
