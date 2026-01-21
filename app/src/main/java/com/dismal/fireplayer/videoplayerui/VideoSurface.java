package com.dismal.fireplayer.videoplayerui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

public class VideoSurface extends SurfaceView {
    private static final String TAG = "videoSurface";
    private int mMode3D = 0;
    private int mVideoHeight;
    private int mVideoWidth;
    private int mZoomMode = 0;

    public VideoSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(this.mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(this.mVideoHeight, heightMeasureSpec);
        if (this.mVideoWidth <= 0 || this.mVideoHeight <= 0) {
            Log.v(TAG, "setMeasuredDimension 0: " + width + "x" + height);
            setMeasuredDimension(width, height);
            return;
        }
        setvideoSize(width, height);
    }

    private void setvideoSize(int width, int height) {
        int videoHeight = this.mMode3D == 4 ? this.mVideoHeight / 2 : this.mVideoHeight;
        switch (this.mZoomMode) {
            case 0:
                if (this.mVideoWidth * height <= width * videoHeight) {
                    if (this.mVideoWidth * height < width * videoHeight) {
                        width = (this.mVideoWidth * height) / videoHeight;
                        break;
                    }
                } else {
                    height = (width * videoHeight) / this.mVideoWidth;
                    break;
                }
                break;
            case 2:
                if (width > this.mVideoWidth) {
                    width = this.mVideoWidth;
                }
                if (height > videoHeight) {
                    height = videoHeight;
                    break;
                }
                break;
        }
        Log.v(TAG, "setMeasuredDimension 1: " + width + "x" + height);
        setMeasuredDimension(width, height);
    }

    public void setZoomMode(int mode) {
        this.mZoomMode = mode;
        if (this.mVideoWidth > 0 && this.mVideoHeight > 0) {
            getHolder().setFixedSize(this.mVideoWidth, this.mVideoHeight);
            requestLayout();
            invalidate();
        }
    }

    public void setVideoSize(int width, int height) {
        this.mVideoWidth = width;
        this.mVideoHeight = height;
        requestLayout();
        invalidate();
    }

    public void set3DMode(int mode3d) {
        this.mMode3D = mode3d;
    }
}
