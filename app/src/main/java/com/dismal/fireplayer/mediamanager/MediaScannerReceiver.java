package com.dismal.fireplayer.mediamanager;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.util.Log;
import com.dismal.fireplayer.provider.LocalMediaProviderContract;

public class MediaScannerReceiver extends BroadcastReceiver {
    public static final String INTENT_ACTION_DELETE_FILE = "android.intent.action.dismal.MEDIA_SCANNER_DELETE_FILE";
    private static final String TAG = "MediaScannerReceiver4k";

    public void onReceive(Context context, Intent intent) {
        String path;
        String action = intent.getAction();
        Uri uri = intent.getData();
        Log.v(TAG, ">>>>>>>> onReceive action=" + action + " uri=" + uri);
        if (uri.getScheme().equals("file")) {
            Boolean scanFile = false;
            String path2 = uri.getPath();
            Log.d(TAG, "action: " + action + " path: " + path2);
            if (!action.equals("android.intent.action.MEDIA_SCANNER_SCAN_FILE") && !action.equals("android.hardware.action.NEW_VIDEO")) {
                action.equals(INTENT_ACTION_DELETE_FILE);
            }
            mediaScan(context, path2, action, scanFile.booleanValue());
        } else if (uri.getScheme().equals(LocalMediaProviderContract.SCHEME) && (path = uri2FilePath(context, uri)) != null) {
            mediaScan(context, path, action, true);
        }
    }

    private void mediaScan(Context context, String rootPath, String action, boolean isFile) {
        context.startService(new Intent(context, LocalMediaScannerService.class).setAction(action).putExtra(LocalMediaScannerService.EXTRA_DEVPATH, rootPath).putExtra(LocalMediaScannerService.EXTRA_SCANTYPE, isFile));
    }

    @TargetApi(16)
    private String uri2FilePath(Context context, Uri videoUri) {
        String path = null;
        Cursor c = context.getContentResolver().query(videoUri, new String[]{"_data"}, (String) null, (String[]) null, (String) null, (CancellationSignal) null);
        if (c != null && c.moveToFirst()) {
            try {
                path = c.getString(c.getColumnIndex("_data"));
            } finally {
                c.close();
            }
        }
        return path;
    }
}
