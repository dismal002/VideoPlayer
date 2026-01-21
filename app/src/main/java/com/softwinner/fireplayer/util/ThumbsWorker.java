package com.softwinner.fireplayer.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.provider.LocalMediaInfo;
import com.softwinner.fireplayer.provider.LocalMediaProviderContract;
import com.softwinner.fireplayer.ui.VideoThumbsTextureView;
import com.softwinner.fireplayer.ui.VideoThumbsTextureViewManager;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class ThumbsWorker {
    private static final int CORE_POOL_SIZE = 1;
    private static final int FADE_IN_TIME = 200;
    private static final int KEEP_ALIVE_TIME = 1;
    public static final int KEY_IMAGEVIEW_REF = 2131099649;
    private static final int MAXIMUM_POOL_SIZE = 1;
    private static final int MSG_THUMB_VIDEO_ATTACH = 0;
    private static final String TAG = "ThumbsWorker";
    public static final int THUMB_VIEW_MODE_PLAYLIST = 1;
    public static final int THUMB_VIEW_MODE_PREVIEW = 0;
    /* access modifiers changed from: private */
    public Boolean mAthumbEnable = false;
    protected Context mContext;
    /* access modifiers changed from: private */
    public boolean mExitTasksEarly = false;
    private boolean mFadeInBitmap = true;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (ThumbsWorker.this.mAthumbEnable.booleanValue() && !ThumbsWorker.this.mExitTasksEarly) {
                        ThumbsWorker.this.mVTTVManager.attachTextureView((ThumbVideoInfoParcel) msg.obj);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private Bitmap mLoadingBitmap;
    /* access modifiers changed from: private */
    public ThumbsCache mThumbsCache;
    private final ThreadPoolExecutor mThumbsVideoThreadPool;
    private LifoBlockingDeque<Runnable> mThumbsVideoWorkQueue;
    protected ThumbsWorkAdapter mThumbsWorkAdapter;
    /* access modifiers changed from: private */
    public VideoThumbsTextureViewManager mVTTVManager;

    public static abstract class ThumbsWorkAdapter {
        public abstract Object getItem(int i);

        public abstract int getSize();
    }

    /* access modifiers changed from: protected */
    public abstract Bitmap processThumbPic(Object obj);

    /* access modifiers changed from: protected */
    public abstract Bitmap processThumbVideo(Object obj);

    public class ThumbVideoInfoParcel {
        public FrameLayout mFrameLayout;
        public ImageView mImageView;
        public int mNum;
        public String mPath;
        public VideoThumbsTextureView mVttv;

        ThumbVideoInfoParcel(String path, ImageView imageview, int num) {
            this.mPath = path;
            this.mImageView = imageview;
            this.mNum = num;
        }
    }

    public class LifoBlockingDeque<E> extends LinkedBlockingDeque<E> {
        private static final long serialVersionUID = -4854985351588039351L;

        public LifoBlockingDeque() {
        }

        public boolean offer(E e) {
            return super.offerFirst(e);
        }

        public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
            return super.offerFirst(e, timeout, unit);
        }

        public boolean add(E e) {
            return super.offerFirst(e);
        }

        public void put(E e) throws InterruptedException {
            super.putFirst(e);
        }
    }

    class ThumbVideoCaptureRunnable implements Runnable {
        private final Object mData;
        private final ImageView mImageView;
        private final LocalMediaInfo mInfo;

        ThumbVideoCaptureRunnable(Object data, ImageView imageView) {
            this.mInfo = (LocalMediaInfo) data;
            this.mData = data;
            this.mImageView = imageView;
        }

        public void run() {
            LocalMediaInfo refMediaInfo;
            String key = String.valueOf(String.valueOf(this.mInfo.mKey)) + ".athumb";
            String tspath = ThumbsWorker.this.mThumbsCache.getThumbsStreamPathFromDiskCache(key);
            if (tspath == null) {
                ThumbsWorker.this.mThumbsCache.addThumbsStreamToDiskCache(key, ThumbsWorker.this.processThumbVideo(this.mData));
                tspath = ThumbsWorker.this.mThumbsCache.getThumbsStreamPathFromDiskCache(key);
            }
            ImageViewWeakReference imageViewWeakRef = (ImageViewWeakReference) this.mImageView.getTag(R.id.imageview_weak_ref);
            if (imageViewWeakRef != null && (refMediaInfo = imageViewWeakRef.getImageViewReference()) != null && refMediaInfo.mNum == this.mInfo.mNum && refMediaInfo.mPath == this.mInfo.mPath && !ThumbsWorker.this.mExitTasksEarly) {
                ThumbsWorker.this.mHandler.sendMessage(ThumbsWorker.this.mHandler.obtainMessage(0, new ThumbVideoInfoParcel(tspath, this.mImageView, this.mInfo.mNum)));
            }
        }
    }

    protected ThumbsWorker(Context context) {
        this.mContext = context;
        this.mThumbsVideoWorkQueue = new LifoBlockingDeque<>();
        this.mThumbsVideoThreadPool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, this.mThumbsVideoWorkQueue, new PriorityThreadFactory("video-thumbs", 10));
    }

    public void loadThumbPic(int num, Object data, View view, int mode) {
        ImageViewWeakReference imageViewWeakRef;
        LocalMediaInfo refMediaInfo;
        ImageViewWeakReference imageViewWeakRef0;
        LocalMediaInfo refMediaInfo2;
        Bitmap bitmap = null;
        LocalMediaInfo mediaInfo = (LocalMediaInfo) data;
        ((TextView) view.findViewById(R.id.grid_item_title)).setText(mediaInfo.mName);
        if (mode == 0) {
            ((TextView) view.findViewById(R.id.grid_item_timeinfo)).setText(String.valueOf(Utils.stringForTime((long) mediaInfo.mBookmark)) + "/" + Utils.stringForTime((long) mediaInfo.mDuration));
            ((TextView) view.findViewById(R.id.grid_item_sizeinfo)).setText(Utils.stringForSize(mediaInfo.mSize));
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.grid_item_bp_progress);
            if (mediaInfo.mBookmark <= 0 || mediaInfo.mDuration <= 0) {
                progressBar.setVisibility(8);
            } else {
                progressBar.setProgress((mediaInfo.mBookmark * 100) / mediaInfo.mDuration);
                progressBar.setVisibility(0);
            }
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.grid_item_image);
        if (num != 0 || (imageViewWeakRef0 = (ImageViewWeakReference) imageView.getTag(R.id.imageview_weak_ref)) == null || (refMediaInfo2 = imageViewWeakRef0.getImageViewReference()) == null || refMediaInfo2.mNum != mediaInfo.mNum || refMediaInfo2.mPath != mediaInfo.mPath) {
            if (this.mThumbsCache != null) {
                bitmap = this.mThumbsCache.getBitmapFromMemCache(String.valueOf(mediaInfo.mKey));
            }
            if (mode == 0) {
                FrameLayout fl = (FrameLayout) imageView.getParent();
                if (!(fl.getChildCount() <= 1 || (imageViewWeakRef = (ImageViewWeakReference) imageView.getTag(R.id.imageview_weak_ref)) == null || (refMediaInfo = imageViewWeakRef.getImageViewReference()) == null || ((refMediaInfo.mNum == num && refMediaInfo.mPath == mediaInfo.mPath) || fl.getChildAt(1) == null))) {
                    fl.removeViewAt(1);
                }
            }
            if (this.mAthumbEnable.booleanValue()) {
                imageView.setTag(R.id.imageview_weak_ref, new ImageViewWeakReference(mediaInfo));
            }
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                loadThumbVideo(data, imageView);
            } else if (cancelPotentialWork(data, imageView)) {
                ThumbsWorkerTask task = new ThumbsWorkerTask(imageView, mode);
                imageView.setImageDrawable(new AsyncDrawable(this.mContext.getResources(), this.mLoadingBitmap, task));
                task.execute(new Object[]{data});
            }
        }
    }

    public void loadThumbVideo(Object data, ImageView imageView) {
        if (this.mAthumbEnable.booleanValue()) {
            this.mThumbsVideoThreadPool.execute(new ThumbVideoCaptureRunnable(data, imageView));
        }
    }

    public void loadThumbs(int num, View view, int mode) {
        if (this.mThumbsWorkAdapter != null) {
            loadThumbPic(num, this.mThumbsWorkAdapter.getItem(num), view, mode);
            return;
        }
        throw new NullPointerException("Data not set, must call setAdapter() first.");
    }

    public Object getMediaInfo(int num) {
        if (this.mThumbsWorkAdapter != null) {
            return this.mThumbsWorkAdapter.getItem(num);
        }
        throw new NullPointerException("Data not set, must call setAdapter() first.");
    }

    public void setLoadingImage(Bitmap bitmap) {
        this.mLoadingBitmap = bitmap;
    }

    public void setLoadingImage(int resId) {
        this.mLoadingBitmap = BitmapFactory.decodeResource(this.mContext.getResources(), resId);
    }

    public void setImageCache(ThumbsCache cacheCallback) {
        this.mThumbsCache = cacheCallback;
    }

    public boolean getAthumbEnable() {
        return this.mAthumbEnable.booleanValue();
    }

    public void setAthumbEnable(boolean enable) {
        this.mAthumbEnable = Boolean.valueOf(enable);
    }

    public void setVideoThumbsTextureViewManager(VideoThumbsTextureViewManager vttvm) {
        this.mVTTVManager = vttvm;
    }

    public ThumbsCache getImageCache() {
        return this.mThumbsCache;
    }

    public void setImageFadeIn(boolean fadeIn) {
        this.mFadeInBitmap = fadeIn;
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        this.mExitTasksEarly = exitTasksEarly;
    }

    public static void cancelWork(ImageView imageView) {
        ThumbsWorkerTask thumbsWorkerTask = getBitmapWorkerTask(imageView);
        if (thumbsWorkerTask != null) {
            thumbsWorkerTask.cancel(true);
        }
    }

    public static boolean cancelPotentialWork(Object data, ImageView imageView) {
        ThumbsWorkerTask thumbsWorkerTask = getBitmapWorkerTask(imageView);
        if (thumbsWorkerTask == null) {
            return true;
        }
        Object bitmapData = thumbsWorkerTask.data;
        if (bitmapData != null && bitmapData.equals(data)) {
            return false;
        }
        thumbsWorkerTask.cancel(true);
        return true;
    }

    /* access modifiers changed from: private */
    public static ThumbsWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                return ((AsyncDrawable) drawable).getBitmapWorkerTask();
            }
        }
        return null;
    }

    private class ThumbsWorkerTask extends AsyncTask<Object, Void, Bitmap> {
        /* access modifiers changed from: private */
        public Object data;
        private final WeakReference<ImageView> imageViewReference;
        private int mMediaInfoGetted;
        private int mViewMode;
        private LocalMediaInfo mediaInfo;

        public ThumbsWorkerTask(ImageView imageView, int mode) {
            this.imageViewReference = new WeakReference<>(imageView);
            this.mViewMode = mode;
        }

        /* access modifiers changed from: protected */
        public Bitmap doInBackground(Object... params) {
            this.data = params[0];
            this.mediaInfo = (LocalMediaInfo) this.data;
            this.mMediaInfoGetted = this.mediaInfo.mMediaInfoGetted;
            String dataString = String.valueOf(this.mediaInfo.mKey);
            Bitmap bitmap = null;
            if (ThumbsWorker.this.mThumbsCache != null && !isCancelled() && getAttachedImageView() != null && !ThumbsWorker.this.mExitTasksEarly) {
                bitmap = ThumbsWorker.this.mThumbsCache.getBitmapFromDiskCache(dataString);
            }
            if (bitmap == null && !isCancelled() && getAttachedImageView() != null && !ThumbsWorker.this.mExitTasksEarly) {
                bitmap = ThumbsWorker.this.processThumbPic(this.mediaInfo);
            } else if (this.mediaInfo.mMediaInfoGetted != 1 && !isCancelled() && getAttachedImageView() != null && !ThumbsWorker.this.mExitTasksEarly) {
                String path = this.mediaInfo.mPath;
                Log.i(ThumbsWorker.TAG, "extra  extractMetadata path = " + path);
                if (!new File(Utils.convertMediaPathUser(path)).exists()) {
                    Log.w(ThumbsWorker.TAG, "get thumb pic error: file not exist! path=" + path);
                    return null;
                }
                String path2 = Utils.convertMediaPath(path);
                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                try {
                    metadataRetriever.setDataSource(path2);
                    String duration = metadataRetriever.extractMetadata(9);
                    String vw = metadataRetriever.extractMetadata(18);
                    String vh = metadataRetriever.extractMetadata(19);
                    if (duration != null) {
                        this.mediaInfo.mDuration = Integer.valueOf(duration).intValue();
                        this.mediaInfo.mMediaInfoGetted = 1;
                    }
                    if (vw != null) {
                        this.mediaInfo.mWidth = Integer.valueOf(vw).intValue();
                    }
                    if (vh != null) {
                        this.mediaInfo.mHeight = Integer.valueOf(vh).intValue();
                    }
                    metadataRetriever.release();
                } catch (Exception e) {
                    Log.e(ThumbsWorker.TAG, "extractMetadata Exception!");
                    e.printStackTrace();
                    metadataRetriever.release();
                } catch (Throwable th) {
                    metadataRetriever.release();
                    throw th;
                }
            }
            if (!(bitmap == null || ThumbsWorker.this.mThumbsCache == null)) {
                ThumbsWorker.this.mThumbsCache.addBitmapToCache(dataString, bitmap);
            }
            if (this.mMediaInfoGetted != this.mediaInfo.mMediaInfoGetted && this.mediaInfo.mMediaInfoGetted == 1) {
                ContentValues cv = new ContentValues();
                cv.put(LocalMediaProviderContract.DURATION_COLUMN, Integer.valueOf(this.mediaInfo.mDuration));
                cv.put(LocalMediaProviderContract.WIDTH_COLUMN, Integer.valueOf(this.mediaInfo.mWidth));
                cv.put(LocalMediaProviderContract.HEIGHT_COLUMN, Integer.valueOf(this.mediaInfo.mHeight));
                cv.put(LocalMediaProviderContract.MEDIAINFO_GETTED_COLUMN, Integer.valueOf(this.mediaInfo.mMediaInfoGetted));
                Log.v(ThumbsWorker.TAG, "saveToDB path=" + this.mediaInfo.mPath + " mDuration =" + this.mediaInfo.mDuration);
                ThumbsWorker.this.mContext.getContentResolver().update(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, cv, "hashcode=?", new String[]{String.valueOf(this.mediaInfo.mKey)});
            }
            return bitmap;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Bitmap bitmap) {
            if (isCancelled() || ThumbsWorker.this.mExitTasksEarly) {
                bitmap = null;
            }
            ImageView imageView = getAttachedImageView();
            if (imageView == null) {
                return;
            }
            if (this.mViewMode == 0) {
                FrameLayout fl = (FrameLayout) imageView.getParent();
                if (this.mMediaInfoGetted != this.mediaInfo.mMediaInfoGetted) {
                    TextView infoTextView = (TextView) ((View) fl.getParent().getParent()).findViewById(R.id.grid_item_timeinfo);
                    String strText = String.valueOf(Utils.stringForTime((long) this.mediaInfo.mBookmark)) + " / " + Utils.stringForTime((long) this.mediaInfo.mDuration);
                    if (infoTextView != null) {
                        infoTextView.setText(strText);
                    }
                }
                if (bitmap != null) {
                    ThumbsWorker.this.setImageBitmap(imageView, bitmap);
                    if (!ThumbsWorker.this.mExitTasksEarly) {
                        ThumbsWorker.this.loadThumbVideo(this.data, imageView);
                    }
                } else if (fl.getChildCount() > 1 && fl.getChildAt(1) != null) {
                    fl.removeViewAt(1);
                }
            } else if (bitmap != null) {
                ThumbsWorker.this.setImageBitmap(imageView, bitmap);
            }
        }

        private ImageView getAttachedImageView() {
            ImageView imageView = (ImageView) this.imageViewReference.get();
            if (this == ThumbsWorker.getBitmapWorkerTask(imageView)) {
                return imageView;
            }
            return null;
        }
    }

    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<ThumbsWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, ThumbsWorkerTask thumbsWorkerTask) {
            super(res, bitmap);
            this.bitmapWorkerTaskReference = new WeakReference<>(thumbsWorkerTask);
        }

        public ThumbsWorkerTask getBitmapWorkerTask() {
            return (ThumbsWorkerTask) this.bitmapWorkerTaskReference.get();
        }
    }

    private static class ImageViewWeakReference {
        private final WeakReference<LocalMediaInfo> imageViewReference;

        public ImageViewWeakReference(LocalMediaInfo data) {
            this.imageViewReference = new WeakReference<>(data);
        }

        public LocalMediaInfo getImageViewReference() {
            return (LocalMediaInfo) this.imageViewReference.get();
        }
    }

    /* access modifiers changed from: private */
    public void setImageBitmap(ImageView imageView, Bitmap bitmap) {
        if (this.mFadeInBitmap) {
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{new ColorDrawable(17170445), new BitmapDrawable(this.mContext.getResources(), bitmap)});
            imageView.setBackgroundDrawable(new BitmapDrawable(this.mContext.getResources(), this.mLoadingBitmap));
            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
            return;
        }
        imageView.setImageBitmap(bitmap);
    }

    public void setAdapter(ThumbsWorkAdapter adapter) {
        this.mThumbsWorkAdapter = adapter;
    }

    public ThumbsWorkAdapter getAdapter() {
        return this.mThumbsWorkAdapter;
    }
}
