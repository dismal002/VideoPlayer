package com.dismal.fireplayer.constant;

import android.os.Build;
import android.os.Environment;
import java.util.Locale;

public final class Constants {
    public static final String ACTION_MEDIASCAN = "com.dismal.fireplayer.ACTION_MEDIASCAN";
    public static final String ACTION_VIEW_IMAGE = "com.dismal.fireplayer.ACTION_VIEW_IMAGE";
    public static final String ACTION_ZOOM_IMAGE = "com.dismal.fireplayer.ACTION_ZOOM_IMAGE";
    public static final CharSequence BLANK = " ";
    public static final String BROADCAST_ACTION_BOOKMARK = "com.dismal.fireplayer.BROADCAST.bookmark";
    public static final String BROADCAST_ACTION_MEDIASCAN_FINISHED = "com.dismal.fireplayer.BROADCAST.mediascan_finish";
    public static final String BROADCAST_ACTION_MEDIASCAN_START = "com.dismal.fireplayer.BROADCAST.mediascan_start";
    public static final String EXTENDED_BOOKMARK_DURATION = "com.dismal.fireplayer.BROADCAST.bookmarkduration";
    public static final String EXTENDED_BOOKMARK_PATH = "com.dismal.fireplayer.BROADCAST.bookmarkpath";
    public static final String EXTENDED_DATA_STATUS = "com.dismal.fireplayer.STATUS";
    public static final String EXTENDED_FULLSCREEN = "com.dismal.fireplayer.EXTENDED_FULLSCREEN";
    public static final String EXTENDED_STATUS_LOG = "com.dismal.fireplayer.LOG";
    public static final boolean LOGD = true;
    public static final boolean LOGV = false;
    // Use standard Android external storage directory
    // This will be /storage/emulated/0 on most modern devices
    public static final String MEDIA_ROOT_PATH = Environment.getExternalStorageDirectory() != null
            ? Environment.getExternalStorageDirectory().getAbsolutePath()
            : "/storage/emulated/0";
    public static final String[] PREDEF_CATEGORY = { MEDIA_ROOT_PATH, "RecentPlayed", "RecordVides", "Favorites" };
    public static final int STATE_ACTION_COMPLETE = 5;
    public static final int STATE_ACTION_CONNECTING = 1;
    public static final int STATE_ACTION_PARSING = 2;
    public static final int STATE_ACTION_PART_COMPLETE = 4;
    public static final int STATE_ACTION_STARTED = 0;
    public static final int STATE_ACTION_WRITING = 3;
    public static final int STATE_LOG = -1;
    public static final String USER_AGENT = ("Mozilla/5.0 (Linux; U; Android " + Build.VERSION.RELEASE + ";"
            + Locale.getDefault().toString() + "; " + Build.DEVICE + "/" + Build.ID + ")");
    public static final String VIDEO_FOLDER_FRAGMENT_TAG = "com.dismal.fireplayer.VIDEO_FOLDER_FRAGMENT_TAG";
    public static final String VIDEO_THUMBS_FRAGMENT_TAG = "com.dismal.fireplayer.VIDEO_THUMBS_FRAGMENT_TAG";
}
