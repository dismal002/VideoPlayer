package com.softwinner.fireplayer.videoplayerui;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import com.softwinner.fireplayer.ui.AppConfig;

public class ControlGestureListener extends GestureDetector.SimpleOnGestureListener {
    private static final int ADJUST_MODE_BRIGHT = 2;
    private static final int ADJUST_MODE_NONE = 0;
    private static final int ADJUST_MODE_SEEKBAR = 3;
    private static final int ADJUST_MODE_VOLUME = 1;
    private static final int MIN_VELOCITY = 100;
    private static final String TAG = "ControlGestureListener";
    private AppConfig config;
    private float downX;
    private int mAdjustMode = 0;
    private Context mContext;
    private MovieControlPanel mControlPanel;
    private VideoSurfaceTextureView mVideoView;
    private MotionEvent progressMotionEventStart;

    public ControlGestureListener(Context context, VideoSurfaceTextureView view, MovieControlPanel controlpanel) {
        this.mContext = context;
        this.mVideoView = view;
        this.mControlPanel = controlpanel;
        this.config = AppConfig.getInstance(context);
    }

    public boolean onDown(MotionEvent e) {
        this.downX = e.getX();
        return this.mVideoView.isFullScreen();
    }

    public boolean onUp(MotionEvent e) {
        if (this.mAdjustMode == 3) {
            this.mControlPanel.changeSeekProgress(1, 0.0f);
        }
        this.mAdjustMode = 0;
        return false;
    }

    public boolean onSingleTapConfirmed(MotionEvent e) {
        this.mControlPanel.toggleCommonControl();
        return super.onSingleTapConfirmed(e);
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!this.mVideoView.isFullScreen()) {
            return false;
        }
        double angle = Math.atan((double) (distanceY / distanceX));
        DisplayMetrics metrics = this.mContext.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        switch (this.mAdjustMode) {
            case 1:
                this.mControlPanel.changeVolume((3.0f * distanceY) / ((float) screenHeight));
                break;
            case 2:
                this.mControlPanel.changeBrightness((3.0f * distanceY) / ((float) screenHeight));
                break;
            case 3:
                this.mControlPanel.changeSeekProgress(2, e2.getX() - this.progressMotionEventStart.getX());
                break;
            default:
                if (Math.abs(angle) <= 1.1780972450961724d) {
                    if (Math.abs(angle) < 0.39269908169872414d) {
                        this.mAdjustMode = 3;
                        this.progressMotionEventStart = e1;
                        this.mControlPanel.changeSeekProgress(0, 0.0f);
                        break;
                    }
                } else if (this.downX >= ((float) screenWidth) * 0.25f) {
                    if (this.downX > ((float) screenWidth) * 0.75f) {
                        this.mAdjustMode = 1;
                        this.mControlPanel.changeVolume((3.0f * distanceY) / ((float) screenHeight));
                        break;
                    }
                } else {
                    this.mAdjustMode = 2;
                    this.mControlPanel.changeBrightness((3.0f * distanceY) / ((float) screenHeight));
                    break;
                }
                break;
        }
        return true;
    }
}
