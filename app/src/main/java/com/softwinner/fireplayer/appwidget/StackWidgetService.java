package com.softwinner.fireplayer.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.provider.LocalMediaInfo;
import com.softwinner.fireplayer.provider.LocalMediaProviderContract;
import com.softwinner.fireplayer.ui.AppConfig;
import com.softwinner.fireplayer.util.DiskLruCache;
import com.softwinner.fireplayer.videoplayerui.FloatVideoService;
import java.io.File;
import java.util.ArrayList;

public class StackWidgetService extends RemoteViewsService {
    public static final String EXTRA_WIDGET_ID = "widgetId";
    private static final int MEDIA_ITEM_COUNT = 16;
    private static final String TAG = "AppWidgetService";

    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteVsFactory(getApplicationContext());
    }

    private static class RemoteVsFactory implements RemoteViewsService.RemoteViewsFactory {
        private final Context mContext;
        private File mDiskCacheDir;
        private ArrayList<LocalMediaInfo> mSource = new ArrayList<>();

        public RemoteVsFactory(Context ctx) {
            this.mContext = ctx;
        }

        private void loadRecentPlayDatasource() {
            this.mDiskCacheDir = DiskLruCache.getDiskCacheDir(this.mContext, "thumbs");
            String[] selectionArgs = {String.valueOf(AppConfig.getInstance(this.mContext).getInt(AppConfig.RECORDED_PLAYTIME, 0))};
            String[] projection = {LocalMediaProviderContract.ROW_ID, LocalMediaProviderContract.DIR_COLUMN, LocalMediaProviderContract.NAME_COLUMN, LocalMediaProviderContract.PATH_COLUMN, LocalMediaProviderContract.DURATION_COLUMN, LocalMediaProviderContract.BOOKMARK_COLUMN, LocalMediaProviderContract.HASHCODE_COLUMN};
            this.mSource.clear();
            Cursor cursor = this.mContext.getContentResolver().query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, projection, "(activevideo=1) AND (" + "latestplaytime>?" + ")", selectionArgs, "latestplaytime desc");
            if (cursor != null && cursor.moveToFirst()) {
                int mItemSize = cursor.getCount();
                if (mItemSize > 16) {
                    mItemSize = 16;
                }
                for (int i = 0; i < mItemSize; i++) {
                    LocalMediaInfo mediaInfo = new LocalMediaInfo();
                    mediaInfo.mName = cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.NAME_COLUMN));
                    mediaInfo.mKey = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.HASHCODE_COLUMN));
                    mediaInfo.mDuration = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.DURATION_COLUMN));
                    mediaInfo.mBookmark = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.BOOKMARK_COLUMN));
                    mediaInfo.mPath = cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.PATH_COLUMN));
                    this.mSource.add(mediaInfo);
                    cursor.moveToNext();
                }
                cursor.close();
            }
        }

        public void onCreate() {
            Log.d(StackWidgetService.TAG, "###########onCreate()..");
            loadRecentPlayDatasource();
            AppWidgetManager.getInstance(this.mContext).notifyAppWidgetViewDataChanged(AppWidgetManager.getInstance(this.mContext).getAppWidgetIds(new ComponentName(this.mContext, VideoAppWidgetProvider.class)), R.id.appwidget_stack_view);
        }

        public void onDataSetChanged() {
            loadRecentPlayDatasource();
        }

        public void onDestroy() {
            Log.d(StackWidgetService.TAG, "###########onDestroy()..");
        }

        public int getCount() {
            return this.mSource.size();
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public RemoteViews getLoadingView() {
            RemoteViews rv = new RemoteViews(this.mContext.getPackageName(), R.layout.widget_loading_item);
            rv.setProgressBar(R.id.appwidget_loading_item, 0, 0, true);
            return rv;
        }

        private Bitmap getMicroBitmap(LocalMediaInfo mediaInfo) {
            String thumbPath = DiskLruCache.createFilePath(this.mDiskCacheDir, String.valueOf(mediaInfo.mKey));
            if (new File(thumbPath).exists()) {
                return BitmapFactory.decodeFile(thumbPath);
            }
            return null;
        }

        public RemoteViews getViewAt(int position) {
            if (position >= this.mSource.size()) {
                return null;
            }
            LocalMediaInfo mediaInfo = this.mSource.get(position);
            Bitmap bitmap = getMicroBitmap(mediaInfo);
            RemoteViews views = new RemoteViews(this.mContext.getPackageName(), R.layout.widget_stack_video_item);
            views.setImageViewBitmap(R.id.appwidget_media_item, bitmap);
            views.setOnClickFillInIntent(R.id.appwidget_media_item, new Intent().setAction(FloatVideoService.ACTION_SWITCH_MODE).putExtra(LocalMediaProviderContract.PATH_COLUMN, mediaInfo.mPath));
            views.setOnClickFillInIntent(R.id.appwidget_media_play, new Intent().setAction(FloatVideoService.ACTION_SWITCH_MODE).putExtra(LocalMediaProviderContract.PATH_COLUMN, mediaInfo.mPath));
            return views;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public boolean hasStableIds() {
            return true;
        }
    }
}
