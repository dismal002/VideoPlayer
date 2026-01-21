package com.dismal.fireplayer.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import androidx.fragment.app.FragmentActivity;
import androidx.collection.LruCache;
import android.util.Log;
import com.dismal.fireplayer.R;
import java.io.File;

public class ThumbsCache {
    private static final boolean DEFAULT_CLEAR_DISK_CACHE_ON_START = false;
    /* access modifiers changed from: private */
    public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 70;
    private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
    private static final int DEFAULT_DISK_CACHE_SIZE = 10485760;
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
    private static final int DEFAULT_MEM_CACHE_SIZE = 5242880;
    private static final String TAG = "ThumbsCache";
    private DiskLruCache mDiskCache;
    private LruCache<String, Bitmap> mMemoryCache;

    public ThumbsCache(Context context, ImageCacheParams cacheParams) {
        init(context, cacheParams);
    }

    public ThumbsCache(Context context, String uniqueName) {
        init(context, new ImageCacheParams(uniqueName));
    }

    public static ThumbsCache findOrCreateCache(FragmentActivity activity, String uniqueName) {
        return findOrCreateCache(activity, new ImageCacheParams(uniqueName));
    }

    public static ThumbsCache findOrCreateCache(FragmentActivity activity, ImageCacheParams cacheParams) {
        RetainFragment mRetainFragment = RetainFragment.findOrCreateRetainFragment(activity.getSupportFragmentManager());
        ThumbsCache imageCache = (ThumbsCache) mRetainFragment.getObject();
        if (imageCache != null) {
            return imageCache;
        }
        ThumbsCache imageCache2 = new ThumbsCache((Context) activity, cacheParams);
        mRetainFragment.setObject(imageCache2);
        return imageCache2;
    }

    private void init(Context context, ImageCacheParams cacheParams) {
        File diskCacheDir = DiskLruCache.getDiskCacheDir(context, cacheParams.uniqueName);
        if (cacheParams.diskCacheEnabled) {
            this.mDiskCache = DiskLruCache.openCache(context, diskCacheDir, (long) cacheParams.diskCacheSize);
            if (this.mDiskCache != null) {
                this.mDiskCache.setCompressParams(cacheParams.compressFormat, cacheParams.compressQuality);
                if (cacheParams.clearDiskCacheOnStart) {
                    this.mDiskCache.clearCache();
                }
            } else {
                Log.w(TAG, "init disk cache fail!");
                new AlertDialog.Builder(context).setMessage(String.format(context.getResources().getString(R.string.str_low_diskspace), new Object[]{Integer.valueOf(cacheParams.diskCacheSize / 1048576)})).setCancelable(false).setIcon(R.drawable.ic_4klogo48).setTitle(context.getResources().getString(R.string.app_name)).setPositiveButton(context.getResources().getString(R.string.close), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }
        if (cacheParams.memoryCacheEnabled) {
            this.mMemoryCache = new LruCache<String, Bitmap>(cacheParams.memCacheSize) {
                /* access modifiers changed from: protected */
                public int sizeOf(String key, Bitmap bitmap) {
                    return Utils.getBitmapSize(bitmap);
                }
            };
        }
    }

    public void addBitmapToCache(String data, Bitmap bitmap) {
        if (data != null && bitmap != null) {
            if (this.mMemoryCache != null && this.mMemoryCache.get(data) == null) {
                this.mMemoryCache.put(data, bitmap);
            }
            if (this.mDiskCache != null && !this.mDiskCache.containsKey(data)) {
                this.mDiskCache.put(data, bitmap);
            }
        }
    }

    public void addThumbsStreamToDiskCache(String data, Object stream) {
        if (data != null && stream != null && this.mDiskCache != null && !this.mDiskCache.containsKey(data)) {
            this.mDiskCache.put(data, stream);
        }
    }

    public String getThumbsStreamPathFromDiskCache(String data) {
        if (this.mDiskCache != null) {
            return this.mDiskCache.getPath(data);
        }
        return null;
    }

    public Bitmap getBitmapFromMemCache(String data) {
        Bitmap memBitmap;
        if (this.mMemoryCache == null || (memBitmap = this.mMemoryCache.get(data)) == null) {
            return null;
        }
        return memBitmap;
    }

    public Bitmap getBitmapFromDiskCache(String data) {
        if (this.mDiskCache != null) {
            return this.mDiskCache.get(data);
        }
        return null;
    }

    public void clearCaches() {
        this.mDiskCache.clearCache();
        this.mMemoryCache.evictAll();
    }

    public static class ImageCacheParams {
        public boolean clearDiskCacheOnStart = false;
        public Bitmap.CompressFormat compressFormat = ThumbsCache.DEFAULT_COMPRESS_FORMAT;
        public int compressQuality = ThumbsCache.DEFAULT_COMPRESS_QUALITY;
        public boolean diskCacheEnabled = true;
        public int diskCacheSize = ThumbsCache.DEFAULT_DISK_CACHE_SIZE;
        public int memCacheSize = ThumbsCache.DEFAULT_MEM_CACHE_SIZE;
        public boolean memoryCacheEnabled = true;
        public String uniqueName;

        public ImageCacheParams(String uniqueName2) {
            this.uniqueName = uniqueName2;
        }
    }
}
