package com.dismal.fireplayer.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

public class SettingsObserver extends ContentObserver {
    public static final int MSG_ACCELEROMETER_ROTATION_SETTING = 75300;
    private static final String TAG = "SettingsObserver";
    private Context mContext;
    private Handler mHandler;
    private ContentResolver mResolver;

    public SettingsObserver(Context context, Handler handler) {
        super(handler);
        this.mContext = context;
        this.mHandler = handler;
        this.mResolver = context.getContentResolver();
    }

    public void registerObserver() {
        this.mResolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), false, this);
    }

    public void unRegisterObserver() {
        this.mResolver.unregisterContentObserver(this);
    }

    public void onChange(boolean selfChange, Uri uri) {
        this.mHandler.obtainMessage(MSG_ACCELEROMETER_ROTATION_SETTING).sendToTarget();
    }
}
