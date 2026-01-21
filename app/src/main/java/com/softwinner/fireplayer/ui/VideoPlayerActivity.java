package com.softwinner.fireplayer.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.constant.Constants;
import com.softwinner.fireplayer.mediamanager.LocalMediaScannerService;
import com.softwinner.fireplayer.provider.LocalMediaProviderContract;
import com.softwinner.fireplayer.ui.PresentationScreenMonitor;
import com.softwinner.fireplayer.util.Utils;
import com.softwinner.fireplayer.videoplayerui.FloatVideoManager;
import com.softwinner.fireplayer.videoplayerui.PlayListMediaInfoAdapter;
import com.softwinner.fireplayer.videoplayerui.VideoSurfaceTextureView;
import java.lang.ref.WeakReference;

public class VideoPlayerActivity extends Activity implements PresentationScreenMonitor.PresentationScreenMonitorListener {
    private static final String TAG = "VideoPlayerActivity";
    private boolean mAnimateOn;
    private AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver = null;
    private boolean mBootModeInternal;
    private Handler mHandler = new HandlerExtension(this);
    private BroadcastReceiver mHdmiHotPlugObserver = null;
    /* access modifiers changed from: private */
    public PlayListMediaInfoAdapter mPlaylistMediaInfoAdapter;
    private PresentationScreenMonitor mPresentationScreenMonitor;
    private SettingsObserver mSettingsObserver;
    private boolean mSuspend = false;
    private VideoPlayerBroadcastReceiver mVideoPlayerBroadcastReceiver;
    /* access modifiers changed from: private */
    public VideoSurfaceTextureView mVideoView;

    private static class HandlerExtension extends Handler {
        WeakReference<VideoPlayerActivity> mActivity;

        HandlerExtension(VideoPlayerActivity activity) {
            this.mActivity = new WeakReference<>(activity);
        }

        public void handleMessage(Message msg) {
            VideoPlayerActivity theActivity = (VideoPlayerActivity) this.mActivity.get();
            if (theActivity != null) {
                switch (msg.what) {
                    case SettingsObserver.MSG_ACCELEROMETER_ROTATION_SETTING:
                        Utils.customOrientationAdapter(theActivity, AppConfig.CUSTOM_ORIENTATION, 0);
                        return;
                    default:
                        super.handleMessage(msg);
                        return;
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        int rotation;
        super.onCreate(savedInstanceState);
        if (!AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.INIT_PLAYLIST, false)) {
            startService(new Intent(this, LocalMediaScannerService.class).setAction(Constants.ACTION_MEDIASCAN).putExtra(LocalMediaScannerService.EXTRA_DEVPATH, Constants.MEDIA_ROOT_PATH).putExtra(LocalMediaScannerService.EXTRA_SCANTYPE, false));
            this.mVideoPlayerBroadcastReceiver = new VideoPlayerBroadcastReceiver(this, (VideoPlayerBroadcastReceiver) null);
            IntentFilter intentFilter = new IntentFilter(Constants.BROADCAST_ACTION_MEDIASCAN_FINISHED);
            intentFilter.addCategory("android.intent.category.DEFAULT");
            registerReceiver(this.mVideoPlayerBroadcastReceiver, intentFilter);
            AppConfig.getInstance(getApplicationContext()).setBoolean(AppConfig.INIT_PLAYLIST, true);
        }
        FloatVideoManager.getInstance(this).closeAllWindows();
        int customOrientation = AppConfig.getInstance(getApplicationContext()).getInt(AppConfig.CUSTOM_ORIENTATION, 0);
        if (customOrientation == 0) {
            rotation = 0;
        } else {
            rotation = Utils.convertCustomOrientation(customOrientation);
        }
        setContentView(R.layout.video_surfacetexture_view);
        View rootView = findViewById(R.id.video_container);
        rootView.setSystemUiVisibility(1792);
        Intent it = getIntent();
        this.mBootModeInternal = true;
        if (!"internal".equals(it.getStringExtra("boot-mode"))) {
            it.putExtra("boot-mode", "external");
            this.mBootModeInternal = false;
        }
        this.mVideoView = new VideoSurfaceTextureView(this, rootView, -1, it, 1);
        this.mVideoView.onCreate(savedInstanceState);
        this.mVideoView.setActivityRotionInfo(rotation);
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.buttonBrightness = 0.0f;
        winParams.flags |= 1024;
        win.setAttributes(winParams);
        win.setBackgroundDrawable((Drawable) null);
        this.mAnimateOn = true;
        this.mPresentationScreenMonitor = PresentationScreenMonitor.getInstance(getApplicationContext());
        this.mPresentationScreenMonitor.setListener(1, this);
        this.mSettingsObserver = new SettingsObserver(this, this.mHandler);
        if (AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.EARPHONE_PLUGOUT_PAUSE, false)) {
            this.mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver(this, (AudioBecomingNoisyReceiver) null);
            this.mAudioBecomingNoisyReceiver.register();
        }
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        this.mSettingsObserver.unRegisterObserver();
        this.mVideoView.suspend();
        this.mSuspend = true;
        if (this.mAnimateOn) {
            this.mVideoView.hideControls();
            overridePendingTransition(17432576, 17432577);
        }
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        this.mSettingsObserver.registerObserver();
        Utils.customOrientationAdapter(this, AppConfig.CUSTOM_ORIENTATION, 0);
        if (Utils.isSdkJB42OrAbove() && this.mPresentationScreenMonitor.getPresentationDisplay() != null) {
            Intent it = new Intent(this, FourKMainActivity.class);
            AppConfig.getInstance(getApplicationContext()).setBoolean(AppConfig.DISABLE_SWITCH_PRESENTATION, false);
            it.putExtra("switchToPresentation", true);
            it.putExtra(LocalMediaProviderContract.PATH_COLUMN, this.mVideoView.getPathName());
            startActivity(it);
            finish();
        } else if (this.mSuspend) {
            this.mVideoView.resume();
            this.mSuspend = false;
        } else {
            this.mVideoView.resume();
        }
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        if (this.mHdmiHotPlugObserver != null) {
            unregisterReceiver(this.mHdmiHotPlugObserver);
            this.mHdmiHotPlugObserver = null;
        }
        if (this.mAudioBecomingNoisyReceiver != null) {
            this.mAudioBecomingNoisyReceiver.unregister();
            this.mAudioBecomingNoisyReceiver = null;
        }
        if (this.mVideoPlayerBroadcastReceiver != null) {
            unregisterReceiver(this.mVideoPlayerBroadcastReceiver);
            this.mVideoPlayerBroadcastReceiver = null;
        }
        super.onDestroy();
    }

    public void onBackPressed() {
        Intent it = new Intent(this, FourKMainActivity.class);
        it.putExtra("clearPresentationPath", true);
        setResult(-1, it);
        super.onBackPressed();
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mVideoView.onSaveInstanceState(outState);
    }

    public void finish() {
        super.finish();
    }

    public void onPresentationDisplayChanged(Display presentationDisplay) {
        if (presentationDisplay != null) {
            Intent it = new Intent(this, FourKMainActivity.class);
            it.putExtra("switchToPresentation", true);
            it.putExtra(LocalMediaProviderContract.PATH_COLUMN, this.mVideoView.getPathName());
            if (this.mBootModeInternal) {
                setResult(-1, it);
                this.mAnimateOn = false;
            } else {
                startActivity(it);
            }
            finish();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mVideoView.onConfigurationChanged(newConfig);
    }

    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {
        private AudioBecomingNoisyReceiver() {
        }

        /* synthetic */ AudioBecomingNoisyReceiver(VideoPlayerActivity videoPlayerActivity, AudioBecomingNoisyReceiver audioBecomingNoisyReceiver) {
            this();
        }

        public void register() {
            VideoPlayerActivity.this.registerReceiver(this, new IntentFilter("android.media.AUDIO_BECOMING_NOISY"));
        }

        public void unregister() {
            VideoPlayerActivity.this.unregisterReceiver(this);
        }

        public void onReceive(Context context, Intent intent) {
            VideoPlayerActivity.this.mVideoView.pause();
            VideoPlayerActivity.this.mVideoView.showCommonControls();
        }
    }

    private class VideoPlayerBroadcastReceiver extends BroadcastReceiver {
        private VideoPlayerBroadcastReceiver() {
        }

        /* synthetic */ VideoPlayerBroadcastReceiver(VideoPlayerActivity videoPlayerActivity, VideoPlayerBroadcastReceiver videoPlayerBroadcastReceiver) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == Constants.BROADCAST_ACTION_MEDIASCAN_FINISHED) {
                VideoPlayerActivity.this.mPlaylistMediaInfoAdapter = PlayListMediaInfoAdapter.getInstance(context);
                VideoPlayerActivity.this.mPlaylistMediaInfoAdapter.loadPlayListMediaInfo("dir like ?", new String[]{"/%"});
            }
        }
    }
}
