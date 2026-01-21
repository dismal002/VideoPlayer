package com.softwinner.fireplayer.mediamanager;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import com.softwinner.fireplayer.provider.LocalMediaProviderContract;
import com.softwinner.fireplayer.ui.AppConfig;
import java.io.File;
import java.io.FileFilter;
import java.util.Locale;
import java.util.Vector;

public class LocalMediaScanner {
    private static final String TAG = "LocalMediaScanner";
    private static LocalMediaScanner mLocalMediaScanner;
    private static FileFilter mVideoFilter;
    /* access modifiers changed from: private */
    public static String mVideoFilterSuffix;
    private Vector<ContentValues> mAddedRecorders;
    private SparseArray<String> mExsitRecordersArray = new SparseArray<>();
    private SparseArray<String> mOriginalRecordersArray;

    public void scan(String devPath, SparseArray<String> originalRecorderArray, Vector<ContentValues> addedRecorders,
            String escapePath) {
        if (devPath == null) {
            throw new IllegalArgumentException("scan root path is null !");
        }
        Log.v(TAG, "scanVideosInFolder: " + devPath);
        this.mExsitRecordersArray.clear();
        this.mOriginalRecordersArray = originalRecorderArray;
        this.mAddedRecorders = addedRecorders;
        scanVideosInFolder(new File(devPath), mVideoFilter, escapePath);
    }

    public LocalMediaScanner(Context context, int type) {
        mVideoFilter = getVideoFilter();
    }

    public static LocalMediaScanner getInstance(Context context) {
        if (mLocalMediaScanner == null) {
            mLocalMediaScanner = new LocalMediaScanner(context, 0);
            mVideoFilterSuffix = AppConfig.getInstance(context.getApplicationContext()).getString(
                    AppConfig.CUSTOM_VIDEO_FILTER,
                    "avi|wmv|rmvb|mkv|m4v|mov|mpg|rm|flv|pmp|vob|asf|3gp|mpeg|ram|divx|m4p|m4b|mp4|f4v|3gpp|3g2|3gpp2|webm|ts|tp|m2ts|3dv|3dm|m1v");
            mVideoFilterSuffix = "^.*?\\.(" + mVideoFilterSuffix + ")$";
        }
        return mLocalMediaScanner;
    }

    public static FileFilter getVideoFilter() {
        return new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory()
                        || file.getName().toLowerCase(Locale.US).matches(LocalMediaScanner.mVideoFilterSuffix);
            }
        };
    }

    public SparseArray<String> getExsitRecordersArray() {
        return this.mExsitRecordersArray;
    }

    private void scanVideosInFolder(File file, FileFilter filter, String escapePath) {
        File[] files = file.listFiles(filter);
        if (files != null) {
            for (File f : files) {
                if (!f.isDirectory() || !f.canRead()) {
                    if (f.isFile()) {
                        String path = f.getPath();
                        int hashCode = (String.valueOf(path) + f.length()).hashCode();
                        if (this.mOriginalRecordersArray.indexOfKey(hashCode) >= 0) {
                            this.mExsitRecordersArray.put(hashCode, (String) null);
                        } else {
                            ContentValues cv = new ContentValues();
                            cv.put(LocalMediaProviderContract.PATH_COLUMN, path);
                            cv.put(LocalMediaProviderContract.DIR_COLUMN, f.getParent());
                            cv.put(LocalMediaProviderContract.NAME_COLUMN, f.getName());
                            cv.put(LocalMediaProviderContract.FILESIZE_COLUMN, Long.valueOf(f.length()));
                            cv.put(LocalMediaProviderContract.HASHCODE_COLUMN, Integer.valueOf(hashCode));
                            cv.put(LocalMediaProviderContract.ACTIVE_VIDEO_COLUMN, 1);
                            this.mAddedRecorders.add(cv);
                        }
                    }
                } else if (!f.getName().startsWith(".") && !f.getPath().equals(escapePath)) {
                    scanVideosInFolder(f, filter, escapePath);
                }
            }
        }
    }
}
