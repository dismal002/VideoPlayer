package com.softwinner.fireplayer.videoplayerui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class VideoTexture extends TextureView {
    private static final String TAG = "videoTexture";
    private int mVideoHeight;
    private int mVideoWidth;
    private int mZoomMode = 0;

    public VideoTexture(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(this.mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(this.mVideoHeight, heightMeasureSpec);
        if (this.mVideoWidth <= 0 || this.mVideoHeight <= 0) {
            setMeasuredDimension(width, height);
        } else {
            setvideoSize(width, height);
        }
    }

    private void setvideoSize(int width, int height) {
        switch (this.mZoomMode) {
            case 0:
                if (this.mVideoWidth * height <= this.mVideoHeight * width) {
                    if (this.mVideoWidth * height < this.mVideoHeight * width) {
                        width = (this.mVideoWidth * height) / this.mVideoHeight;
                        break;
                    }
                } else {
                    height = (this.mVideoHeight * width) / this.mVideoWidth;
                    break;
                }
                break;
            case 2:
                if (width > this.mVideoWidth) {
                    width = this.mVideoWidth;
                }
                if (height > this.mVideoHeight) {
                    height = this.mVideoHeight;
                    break;
                }
                break;
        }
        setMeasuredDimension(width, height);
    }

    public void setZoomMode(int mode) {
        this.mZoomMode = mode;
        if (this.mVideoWidth > 0 && this.mVideoHeight > 0) {
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
}
