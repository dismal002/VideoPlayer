package com.softwinner.fireplayer.ui;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.ui.VideoThumbsTextureView;
import com.softwinner.fireplayer.util.ThumbsWorker;

public class VideoThumbsTextureViewManager implements VideoThumbsTextureView.TextrueViewListener {
    private static final String TAG = "VideoThumbsTextureViewManager";
    private static Context mContext;
    private static Boolean mSuspendInProgress = false;
    private static SparseArray<ThumbsWorker.ThumbVideoInfoParcel> mVTTVSparseArray;
    private static VideoThumbsTextureViewManager sInstance;
    private int mHashCode;

    public interface AllTextrueViewDestroyListener {
        void onAllTextureViewDestory();
    }

    public static VideoThumbsTextureViewManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new VideoThumbsTextureViewManager();
            mContext = context;
            mVTTVSparseArray = new SparseArray<>();
        }
        return sInstance;
    }

    public void attachTextureView(ThumbsWorker.ThumbVideoInfoParcel tvip) {
        ImageView imageView = tvip.mImageView;
        if (imageView != null) {
            FrameLayout fl = (FrameLayout) imageView.getParent();
            tvip.mFrameLayout = fl;
            this.mHashCode = (String.valueOf(String.valueOf(tvip.mFrameLayout)) + tvip.mNum).hashCode();
            VideoThumbsTextureView vttv = new VideoThumbsTextureView(mContext, tvip.mPath, this.mHashCode);
            vttv.setListener(this);
            if (fl.getChildCount() > 1 && fl.getChildAt(1) != null) {
                fl.removeViewAt(1);
            }
            vttv.attachTextureView(fl);
            tvip.mVttv = vttv;
            if (mVTTVSparseArray.indexOfKey(this.mHashCode) < 0) {
                mVTTVSparseArray.put(this.mHashCode, tvip);
            } else {
                Log.w(TAG, "already attachTextureView");
            }
        }
    }

    public void suspendAllTextureView() {
        mSuspendInProgress = true;
        int size = mVTTVSparseArray.size();
        for (int i = 0; i < size; i++) {
            FrameLayout fl = mVTTVSparseArray.valueAt(i).mFrameLayout;
            if (fl.getChildCount() > 1 && fl.getChildAt(1) != null) {
                fl.removeViewAt(1);
            }
            if (fl.getChildCount() > 0 && fl.getChildAt(0) != null) {
                fl.getChildAt(0).setTag(R.id.imageview_weak_ref, (Object) null);
            }
        }
        mVTTVSparseArray.clear();
        mSuspendInProgress = false;
    }

    public void resumeAllTextureView() {
    }

    public void onTextureViewAvailable(int pos) {
    }

    public void onTextureViewDestory(int pos) {
        if (!mSuspendInProgress.booleanValue()) {
            mVTTVSparseArray.remove(this.mHashCode);
        }
    }
}
