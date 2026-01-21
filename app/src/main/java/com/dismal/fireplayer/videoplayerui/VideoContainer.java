package com.dismal.fireplayer.videoplayerui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import com.dismal.fireplayer.provider.LocalMediaProviderContract;
import com.dismal.fireplayer.ui.AppConfig;
import com.dismal.fireplayer.ui.PresentationScreenMonitor;

public class VideoContainer extends FrameLayout {
    private static final String TAG = "VideoContainer";
    private boolean handled = false;
    private double lastDistance;
    private AppConfig mConfig;
    private Context mContext;
    private ControlGestureListener mControlGestureListener;
    private MovieControlPanel mControlPanel;
    private GestureDetector mDetector;
    private VideoSurfaceTextureView mVideoView;

    public VideoContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(Context context, GestureDetector detector, VideoSurfaceTextureView videoView,
            MovieControlPanel controlpanel, ControlGestureListener controlGestureListener) {
        this.mContext = context;
        this.mDetector = detector;
        this.mControlPanel = controlpanel;
        this.mVideoView = videoView;
        this.mConfig = AppConfig.getInstance(context);
        this.mControlGestureListener = controlGestureListener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean float_window_enable = this.mConfig.getBoolean(AppConfig.FLOAT_WINDOW_ENABLE, true);
        if (this.mVideoView.isFullScreen() && event.getPointerCount() >= 2 && !this.handled && float_window_enable
                && (this.mVideoView.mWindowMode != 1 || !this.mControlPanel.getUiLockState())) {
            handleMutiTouch(event);
        }
        boolean ret = this.mDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case 1:
                this.mControlGestureListener.onUp(event);
                break;
        }
        return ret;
    }

    private void handleMutiTouch(MotionEvent event) {
        float x0 = event.getX(0);
        float y0 = event.getY(0);
        float x1 = event.getX(1);
        float y1 = event.getY(1);
        switch (event.getActionMasked()) {
            case 2:
                if (Math.sqrt(Math.pow((double) (x0 - x1), 2.0d) + Math.pow((double) (y0 - y1), 2.0d))
                        / this.lastDistance < 0.9d
                        && !PresentationScreenMonitor.getInstance(this.mContext.getApplicationContext())
                                .isWIFIDisplayOn()) {
                    if (android.os.Build.VERSION.SDK_INT >= 23
                            && !android.provider.Settings.canDrawOverlays(this.mContext)) {
                        android.widget.Toast.makeText(this.mContext, "Please grant overlay permission",
                                android.widget.Toast.LENGTH_LONG).show();
                        this.mContext.startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION",
                                android.net.Uri.parse("package:" + this.mContext.getPackageName())));
                        this.handled = true;
                        return;
                    }
                    String path = this.mVideoView.getPathName();
                    int position = this.mVideoView.getCurrentPosition();
                    int videowidth = this.mVideoView.getVideoSizeWidth();
                    int videoheight = this.mVideoView.getVideoSizeHeight();
                    Intent intent = new Intent(this.mContext, FloatVideoService.class)
                            .setAction(FloatVideoService.ACTION_SWITCH_MODE);
                    intent.putExtra(LocalMediaProviderContract.PATH_COLUMN, path);
                    intent.putExtra("position", position);
                    intent.putExtra("videowidth", videowidth);
                    intent.putExtra("videoheight", videoheight);
                    this.mContext.startService(intent);
                    ((Activity) this.mContext).finish();
                    this.handled = true;
                    return;
                }
                return;
            case 5:
                this.handled = false;
                this.lastDistance = Math.sqrt(Math.pow((double) (x0 - x1), 2.0d) + Math.pow((double) (y0 - y1), 2.0d));
                return;
            default:
                return;
        }
    }
}
