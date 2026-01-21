package com.softwinner.fireplayer.ui;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import com.softwinner.fireplayer.provider.LocalMediaInfo;
import java.util.ArrayList;

public class FourKApplication extends Application {
    public static final String TAG = "FourKApplication";
    private static FourKApplication instance;
    private static ArrayList<String> mPlayList = new ArrayList<>();

    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "System is running low on memory");
    }

    public static Context getAppContext() {
        return instance;
    }

    public static Resources getAppResources() {
        if (instance == null) {
            return null;
        }
        return instance.getResources();
    }

    public static void setPlayList(ArrayList<LocalMediaInfo> mediaInfoArray) {
        mPlayList.clear();
        for (int i = 0; i < mediaInfoArray.size(); i++) {
            mPlayList.add(mediaInfoArray.get(i).mPath);
        }
    }

    public static ArrayList<String> getPlayList() {
        return mPlayList;
    }
}
