package com.softwinner.fireplayer.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.util.Utils;
import com.softwinner.fireplayer.videoplayerui.VideoSurfaceTextureView;
import java.lang.ref.WeakReference;

public class RemoteControlPanel implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private static final int MSG_UPDATE_SEEK_TIME = 10;
    private static final int SEEKBAR_MAX = 1000;
    private static final int SEEK_STEP_TIME = 1000;
    private static final String TAG = "RemoteControlPanel";
    private final AppConfig config;
    private RemoteControlPanelCallback mCallback;
    private final Context mContext;
    private TextView mCurTimeTv;
    private boolean mDragging;
    /* access modifiers changed from: private */
    public Handler mHandler = new HandlerExtension(this);
    private FrameLayout.LayoutParams mLayoutParams;
    private ImageView mNextButton;
    private ImageView mPlayBttn;
    private ImageView mPrevButton;
    private SeekBar mSeekBar;
    private ImageView mSettingBttn;
    private ImageView mStopBttn;
    private TextView mTitleTv;
    private TextView mTotalTimeTv;
    private VideoSurfaceTextureView mVideoView;
    private View mView;
    private ImageView mZoomBttn;

    public interface RemoteControlPanelCallback {
        void onRemoteMediaClose();
    }

    private static class HandlerExtension extends Handler {
        WeakReference<RemoteControlPanel> mExternalClass;

        HandlerExtension(RemoteControlPanel activity) {
            this.mExternalClass = new WeakReference<>(activity);
        }

        public void handleMessage(Message msg) {
            RemoteControlPanel theExternalClass = (RemoteControlPanel) this.mExternalClass.get();
            if (theExternalClass != null) {
                switch (msg.what) {
                    case 10:
                        theExternalClass.updateSeekTime();
                        theExternalClass.mHandler.sendMessageDelayed(obtainMessage(10), 1000);
                        return;
                    default:
                        return;
                }
            }
        }
    }

    public RemoteControlPanel(Context context, VideoSurfaceTextureView videoView, Handler handler) {
        this.mContext = context;
        this.mVideoView = videoView;
        this.config = AppConfig.getInstance(this.mContext.getApplicationContext());
    }

    public View getView() {
        if (this.mView == null) {
            this.mView = View.inflate(this.mContext, R.layout.remote_control_bar, (ViewGroup) null);
            this.mSeekBar = (SeekBar) this.mView.findViewById(R.id.seekbar);
            this.mCurTimeTv = (TextView) this.mView.findViewById(R.id.cur_time);
            this.mTotalTimeTv = (TextView) this.mView.findViewById(R.id.total_time);
            this.mTitleTv = (TextView) this.mView.findViewById(R.id.title);
            this.mTitleTv.setText(this.mVideoView.getVideoName());
            this.mPlayBttn = (ImageView) this.mView.findViewById(R.id.playpause_bttn);
            this.mZoomBttn = (ImageView) this.mView.findViewById(R.id.zoom_bttn);
            this.mSettingBttn = (ImageView) this.mView.findViewById(R.id.setting_bttn);
            this.mStopBttn = (ImageView) this.mView.findViewById(R.id.stop_bttn);
            this.mZoomBttn.setOnClickListener(this);
            this.mSettingBttn.setOnClickListener(this);
            this.mStopBttn.setOnClickListener(this);
            this.mPlayBttn.setOnClickListener(this);
            this.mPrevButton = (ImageView) this.mView.findViewById(R.id.previous_bttn);
            this.mNextButton = (ImageView) this.mView.findViewById(R.id.next_bttn);
            this.mPrevButton.setOnClickListener(this);
            this.mNextButton.setOnClickListener(this);
            updateZoomBttnDrawable(this.config.getInt(AppConfig.ZOOM, 0));
            this.mView.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent ev) {
                    return true;
                }
            });
            this.mSeekBar.setOnSeekBarChangeListener(this);
            this.mSeekBar.setMax(1000);
        }
        return this.mView;
    }

    public FrameLayout.LayoutParams getLayoutParams() {
        if (this.mLayoutParams == null) {
            this.mLayoutParams = new FrameLayout.LayoutParams(-1, -2, 80);
        }
        return this.mLayoutParams;
    }

    public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
        if (fromuser) {
            float newPosition = ((float) this.mVideoView.getDuration()) * (((float) progress) / 1000.0f);
            if (this.mVideoView.inCompleteState()) {
                updatePlayBttnDrawable(false);
            }
            this.mVideoView.seekTo(Math.round(newPosition));
            this.mVideoView.showCommonControls();
            if (this.mCurTimeTv != null) {
                this.mCurTimeTv.setText(Utils.stringForTime((long) ((int) newPosition)));
            }
        }
    }

    public void onStartTrackingTouch(SeekBar bar) {
        this.mDragging = true;
    }

    public void onStopTrackingTouch(SeekBar bar) {
        this.mDragging = false;
    }

    /* access modifiers changed from: private */
    public void updateSeekTime() {
        if (!this.mDragging && !this.mVideoView.inPauseState()) {
            int position = this.mVideoView.getCurrentPosition();
            long duration = (long) this.mVideoView.getDuration();
            if (this.mSeekBar != null && duration > 0) {
                this.mSeekBar.setProgress(Math.round(1000.0f * (((float) position) / ((float) duration))));
            }
            if (this.mTotalTimeTv != null) {
                this.mTotalTimeTv.setText(Utils.stringForTime((long) ((int) duration)));
            }
            if (this.mCurTimeTv != null) {
                this.mCurTimeTv.setText(Utils.stringForTime((long) position));
            }
            if (this.mVideoView.inCompleteState()) {
            }
        }
    }

    public void onShow(boolean auto) {
        boolean z = false;
        this.mView.startAnimation(AnimationUtils.loadAnimation(this.mContext, R.anim.fade_in));
        updateZoomBttnDrawable(this.mVideoView.getZoomMode());
        if (auto) {
            if (!this.mVideoView.isPlaying()) {
                z = true;
            }
            updatePlayBttnDrawable(z);
        } else {
            updatePlayBttnDrawable(false);
        }
        if (this.mVideoView.inCompleteState()) {
            this.mHandler.removeMessages(10);
            this.mSeekBar.setProgress(1000);
            this.mCurTimeTv.setText(Utils.stringForTime((long) this.mVideoView.getDuration()));
            return;
        }
        this.mHandler.removeMessages(10);
        this.mHandler.sendEmptyMessage(10);
    }

    public void onHide() {
        this.mView.startAnimation(AnimationUtils.loadAnimation(this.mContext, 17432577));
        this.mHandler.removeMessages(10);
    }

    private void updateZoomBttnDrawable(int zoom) {
        int drawable;
        if (this.mZoomBttn != null) {
            if (zoom == 1) {
                drawable = R.drawable.bttn_screen_full;
            } else if (zoom == 2) {
                drawable = R.drawable.bttn_screen_origin;
            } else {
                drawable = R.drawable.bttn_screen_draw;
            }
            this.mZoomBttn.setBackgroundResource(drawable);
        }
    }

    public void updatePlayBttnDrawable(boolean play) {
        int drawable;
        if (play) {
            drawable = R.drawable.bttn_play_m;
        } else {
            drawable = R.drawable.bttn_pause_m;
        }
        this.mPlayBttn.setBackgroundResource(drawable);
    }

    public void updateVideoView(VideoSurfaceTextureView vv) {
        this.mVideoView = vv;
        this.mTitleTv.setText(this.mVideoView.getVideoName());
    }

    public void updateTitleView() {
        this.mTitleTv.setText(this.mVideoView.getVideoName());
    }

    public void onPlayPauseButtonClick(View v) {
        if (this.mVideoView.isPlaying()) {
            this.mVideoView.pause();
            updatePlayBttnDrawable(true);
        } else {
            this.mVideoView.play();
            updatePlayBttnDrawable(false);
        }
        this.mVideoView.showCommonControls();
    }

    public void onZoomButtonClick(View v) {
        int zoom = (this.mVideoView.getZoomMode() + 1) % 3;
        this.mVideoView.setZoomMode(zoom);
        updateZoomBttnDrawable(zoom);
        this.config.setInt(AppConfig.ZOOM, zoom);
    }

    public void onSettingButtonClick(View v) {
    }

    private void onStopButtonClick(View v) {
        this.mCallback.onRemoteMediaClose();
    }

    public void setListener(RemoteControlPanelCallback callback) {
        this.mCallback = callback;
    }

    public void onFullScreen(boolean isFull) {
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.zoom_bttn:
                onZoomButtonClick(v);
                return;
            case R.id.previous_bttn:
                this.mVideoView.previous();
                updateTitleView();
                return;
            case R.id.playpause_bttn:
                onPlayPauseButtonClick(v);
                return;
            case R.id.next_bttn:
                this.mVideoView.next();
                updateTitleView();
                return;
            case R.id.setting_bttn:
                onSettingButtonClick(v);
                return;
            case R.id.stop_bttn:
                onStopButtonClick(v);
                return;
            default:
                return;
        }
    }
}
