package com.dismal.fireplayer.videoplayerui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.TimedText;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.dismal.fireplayer.R;
import com.dismal.fireplayer.floatwindow.FloatWindowService;
import com.dismal.fireplayer.provider.LocalMediaInfo;
import com.dismal.fireplayer.provider.LocalMediaProviderContract;
import com.dismal.fireplayer.ui.AppConfig;
import com.dismal.fireplayer.ui.PresentationScreenMonitor;
import com.dismal.fireplayer.ui.VideoPlayerActivity;
import com.dismal.fireplayer.util.Utils;
import com.dismal.fireplayer.videoplayerui.VideoSurfaceTextureView;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@TargetApi(16)
class MovieControlPanel
        implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, AdapterView.OnItemClickListener {
    private static final int HIDE_VOL_BRI_BAR_TIMEOUT = 2000;
    private static final int MSG_FADE_OUT_INFO = 7;
    private static final int MSG_HIDE_UI = 0;
    private static final int MSG_HIDE_VOL_BRI_BAR = 4;
    private static final int MSG_SET_SUB_DELAY = 5;
    private static final int MSG_START_NETWORK_SPEED = 2;
    private static final int MSG_UPDATE_NETWORK_SPEED = 3;
    private static final int MSG_UPDATE_SEEK_TIME = 1;
    private static final int MSG_UPDATE_VIDEOINFO = 6;
    private static final int SCREEN_BRIGHTNESS_MAX = 255;
    private static final int SEEKBAR_MAX = 1000;
    private static final int SEEK_STEP_TIME = 1000;
    private static final int SUBTITLE_BOTTOM_OFFSET = 80;
    private static final int SYSTEM_BAR_HEIGHT = 48;
    private static final String TAG = "MovieControlPanel";
    private static final int UPDATE_NETWORK_SPEED_PERIOD = 1000;
    private boolean DISABLEBACKGRPOUNDFUNC = false;
    private int HIDE_TIME_OUT = 4000;
    final int[] ProgressDrawables = { R.drawable.ic_volume_level_1, R.drawable.ic_volume_level_2,
            R.drawable.ic_volume_level_3, R.drawable.ic_volume_level_4, R.drawable.ic_volume_level_5,
            R.drawable.ic_volume_level_6, R.drawable.ic_volume_level_7, R.drawable.ic_volume_level_8,
            R.drawable.ic_volume_level_9, R.drawable.ic_volume_level_10, R.drawable.ic_volume_level_11,
            R.drawable.ic_volume_level_12 };
    private int SCREEN_BRIGHTNESS_MIN;
    private PlaylistViewAdapter mAdapterPlaylist;
    private AudioManager mAudioManager;
    private View mBottomPanel;
    private boolean mBuffering = true;
    private ImageView mCloseButton;
    /* access modifiers changed from: private */
    public final AppConfig mConfig;
    private final Context mContext;
    private boolean mControllerShowing = false;
    private TextView mCurTimeTv;
    private int mCurrentBrightness;
    private int mCurrentSubColor = -1;
    private int mCurrentSubFontSize = -1;
    private int mCurrentVolume;
    private String mCustomTitle = null;
    private ImageView mDLNAButton;
    private boolean mDisableAutoHide = false;
    private boolean mDragging;
    private ImageView mExpandTrButton;
    private ImageView mFavoritesButton;
    private ImageView mFloatWindowButton;
    /* access modifiers changed from: private */
    public Handler mHandler = new HandlerExtension(this);
    private TextView mInfo;
    private boolean mIsFloatWindow;
    private boolean mIsNetworkStream = false;
    private boolean mIsPlaying = true;
    boolean mLastDispAlignBottom = true;
    /* access modifiers changed from: private */
    public long mLastRecvBytes;
    /* access modifiers changed from: private */
    public int mLastSystemUiVis = 0;
    private FrameLayout.LayoutParams mLayoutParams;
    private ImageView mLockedButton;
    private int mMaxVolume;
    private int mMaxVolumeLevel;
    private DisplayMetrics mMetric;
    private View mMiscPanel;
    private int mMovieDuration;
    private ImageView mNextButton;
    private ImageView mPlayPauseButton;
    private ImageView mPrevButton;
    private View mProgressContainerLoading;
    /* access modifiers changed from: private */
    public View mProgressContainerVolume;
    private ProgressView mProgressLevelView;
    private TextView mProgressLoadingInfo;
    private ImageView mProgressTypeView;
    /* access modifiers changed from: private */
    public final Runnable mRemoveBackground = new Runnable() {
        public void run() {
            if (!MovieControlPanel.this.mConfig.getBoolean(AppConfig.DEMO_MODE, false)) {
                MovieControlPanel.this.mRoot.setBackground((Drawable) null);
            }
        }
    };
    /* access modifiers changed from: private */
    public final FrameLayout mRoot;
    private int mSecondaryProgress;
    private SeekBar mSeekBar;
    private int mSeekSecondStep = 0;
    private int mSeekStartPos;
    private TextView mSeekStepTv;
    private int mSeekToPos;
    private ImageView mSettingBttn;
    private SettingDialog mSettingDialog = null;
    private int mSubColor = -1;
    private int mSubFontSize = 30;
    private FrameLayout mSubLayerView;
    float mTextFontScale = 1.0f;
    private TextSubtitleInfo mTextSubtitleInfo = new TextSubtitleInfo();
    private TextView mTitleTv;
    private View mTopPanel;
    private TextView mTotalTimeTv;
    private boolean mUiLocked = false;
    private ImageView mUnLockButton;
    private ImageView mUsageIV = null;
    private boolean mUserDefSubColor = false;
    private boolean mUserDefSubFontSize = false;
    private String mVideoBitRate;
    private String mVideoCodingFormat;
    private String mVideoFrameRate;
    private TextView mVideoInfoTv;
    private ImageView mVideoListButton;
    /* access modifiers changed from: private */
    public ListView mVideoListView;
    private String mVideoSize;
    /* access modifiers changed from: private */
    public final VideoSurfaceTextureView mVideoView;
    int mVideoViewBottom = 0;
    int mVideoViewBottomMargin;
    int mVideoViewHeight;
    int mVideoViewLeft = 0;
    boolean mVideoViewPosValid = false;
    int mVideoViewRight = 0;
    int mVideoViewTop = 0;
    int mVideoViewWidth;
    private View mView;
    private final int mWindowMode;
    private ImageView mZoomBttn;

    private static class HandlerExtension extends Handler {
        WeakReference<MovieControlPanel> theContext;

        HandlerExtension(MovieControlPanel activity) {
            this.theContext = new WeakReference<>(activity);
        }

        public void handleMessage(Message msg) {
            MovieControlPanel theCtx = (MovieControlPanel) this.theContext.get();
            if (theCtx != null) {
                switch (msg.what) {
                    case 0:
                        theCtx.showCommonControl(false);
                        return;
                    case 1:
                        theCtx.updateSeekTime();
                        theCtx.mHandler.sendMessageDelayed(obtainMessage(1), 1000);
                        return;
                    case 2:
                        theCtx.mLastRecvBytes = Utils.getNetworkSpeed();
                        theCtx.mHandler.sendMessageDelayed(theCtx.mHandler.obtainMessage(3), 1000);
                        return;
                    case 3:
                        theCtx.updateNetworkSpeed();
                        return;
                    case 4:
                        theCtx.mProgressContainerVolume.setVisibility(8);
                        return;
                    case 5:
                        theCtx.mVideoView.setSubDelay(theCtx.mConfig.getInt(AppConfig.SUB_DELAY, 0));
                        break;
                    case 7:
                        break;
                    default:
                        return;
                }
                theCtx.fadeOutInfo();
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateNetworkSpeed() {
        long currBytes = Utils.getNetworkSpeed();
        double bytesPerSecond2 = ((double) (((currBytes - this.mLastRecvBytes) * 10) / 1024)) / 10.0d;
        this.mLastRecvBytes = currBytes;
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), 1000);
        this.mProgressLoadingInfo.setText(String.valueOf(bytesPerSecond2) + "kB/s");
    }

    public void fadeOutInfo() {
        if (this.mInfo != null) {
            if (this.mInfo.getVisibility() == 0) {
                this.mInfo.startAnimation(AnimationUtils.loadAnimation(this.mContext, 17432577));
            }
            this.mInfo.setVisibility(4);
        }
    }

    private void showBackgound(boolean show) {
        if (!this.DISABLEBACKGRPOUNDFUNC && this.mWindowMode == 2) {
            if (show) {
                this.mHandler.removeCallbacks(this.mRemoveBackground);
                this.mRoot.setBackgroundColor(-16777216);
                return;
            }
            this.mHandler.removeCallbacks(this.mRemoveBackground);
            if (!this.mVideoView.getSubGate() || this.mVideoView.getSubList() == null
                    || this.mVideoView.getSubList().size() <= 0) {
                this.mHandler.postDelayed(this.mRemoveBackground, 2000);
            }
        }
    }

    public class TextViewInfo {
        int index;
        public FrameLayout.LayoutParams layoutParams;
        int subtitledID;
        String text;
        TextView textView;
        boolean used;

        public TextViewInfo() {
        }
    }

    public class TextSubtitleInfo {
        /* access modifiers changed from: private */
        public List<TextViewInfo> mTextViewInfoList;

        public TextSubtitleInfo() {
            this.mTextViewInfoList = null;
            this.mTextViewInfoList = new ArrayList();
        }

        public void addTextViewInfo(TextViewInfo textViewInfo) {
            this.mTextViewInfoList.add(textViewInfo);
        }

        public void removeTextViewInfo(TextViewInfo textViewInfo) {
            for (int i = 0; i < this.mTextViewInfoList.size(); i++) {
                if (this.mTextViewInfoList.get(i).text == null) {
                    this.mTextViewInfoList.remove(this.mTextViewInfoList.get(i));
                    return;
                }
                if (this.mTextViewInfoList.get(i).text.equals(textViewInfo.text)) {
                    this.mTextViewInfoList.remove(this.mTextViewInfoList.get(i));
                }
            }
        }

        public void removeAllTextViewInfo() {
            this.mTextViewInfoList.clear();
        }

        public int getNumberOfTextViewInfo() {
            return this.mTextViewInfoList.size();
        }

        public TextViewInfo findTextViewInfo(int subtitle_id) {
            for (int i = 0; i < this.mTextViewInfoList.size(); i++) {
                if (this.mTextViewInfoList.get(i).subtitledID == subtitle_id) {
                    return this.mTextViewInfoList.get(i);
                }
            }
            return null;
        }
    }

    MovieControlPanel(Context context, VideoSurfaceTextureView videoView, int windowMode) {
        this.mContext = context;
        this.mVideoView = videoView;
        this.mWindowMode = windowMode;
        this.mConfig = AppConfig.getInstance(this.mContext);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mMaxVolumeLevel = this.mAudioManager.getStreamMaxVolume(3);
        this.mMaxVolume = this.mMaxVolumeLevel * 100;
        this.mCurrentVolume = this.mAudioManager.getStreamVolume(3) * 100;
        this.mMetric = new DisplayMetrics();
        this.mMetric = context.getApplicationContext().getResources().getDisplayMetrics();
        this.SCREEN_BRIGHTNESS_MIN = AppConfig.getInstance(context).getInt(AppConfig.BRIGHTNESS_MIN, 20);
        this.mCurrentBrightness = AppConfig.getInstance(context).getInt(AppConfig.BRIGHTNESS, 0);
        if (this.mCurrentBrightness == 0) {
            this.mCurrentBrightness = getBrightness(this.SCREEN_BRIGHTNESS_MIN);
        }
        if (this.mWindowMode == 2) {
            this.HIDE_TIME_OUT = 2500;
            if (PresentationScreenMonitor.getInstance(context).getPresentationDisplay() != null) {
                Point sizeP = new Point();
                PresentationScreenMonitor.getInstance(context).getPresentationDisplay().getSize(sizeP);
                this.mMetric = new DisplayMetrics();
                this.mMetric.widthPixels = sizeP.x;
                this.mMetric.heightPixels = sizeP.y;
                this.mMetric.density = 1.0f;
            }
        }
        this.mRoot = this.mVideoView.getContainer();
        this.mRoot.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            public void onSystemUiVisibilityChange(int visibility) {
                int diff = MovieControlPanel.this.mLastSystemUiVis ^ visibility;
                MovieControlPanel.this.mLastSystemUiVis = visibility;
                Log.v(MovieControlPanel.TAG, "onSystemUiVisibilityChange: " + visibility);
                if ((diff & 2) == 0 || (visibility & 2) != 0) {
                    MovieControlPanel.this.mHandler.removeCallbacks(MovieControlPanel.this.mRemoveBackground);
                    if (!MovieControlPanel.this.mVideoView.getSubGate()
                            || MovieControlPanel.this.mVideoView.getSubList() == null
                            || MovieControlPanel.this.mVideoView.getSubList().size() <= 0) {
                        MovieControlPanel.this.mHandler.postDelayed(MovieControlPanel.this.mRemoveBackground, 2000);
                        return;
                    }
                    return;
                }
                MovieControlPanel.this.showCommonControl(true);
                MovieControlPanel.this.mHandler.removeCallbacks(MovieControlPanel.this.mRemoveBackground);
                MovieControlPanel.this.mRoot.setBackgroundColor(-16777216);
            }
        });
        this.mRoot.addView(getSubLayerView(), getLayoutParams());
        this.mSubLayerView.setVisibility(8);
        LinearLayout ll = (LinearLayout) View.inflate(this.mContext, R.layout.vplayer_fps_info, (ViewGroup) null);
        this.mVideoInfoTv = (TextView) ll.findViewById(R.id.video_info);
        this.mRoot.addView(ll, getLayoutParams());
        this.mRoot.addView(getView(), getLayoutParams());
        this.mVideoCodingFormat = this.mContext.getResources().getString(R.string.video_coding_format);
        this.mVideoSize = this.mContext.getResources().getString(R.string.video_size);
        this.mVideoFrameRate = this.mContext.getResources().getString(R.string.video_frame_rate);
        this.mVideoBitRate = this.mContext.getResources().getString(R.string.video_bit_rate);
    }

    public View getSubLayerView() {
        if (this.mSubLayerView == null) {
            this.mSubLayerView = (FrameLayout) View.inflate(this.mContext, R.layout.vplayer_subtitle_layer,
                    (ViewGroup) null);
        }
        return this.mSubLayerView;
    }

    public View getView() {
        if (this.mView == null) {
            this.mIsFloatWindow = this.mWindowMode == 0;
            if (this.mIsFloatWindow) {
                this.mView = View.inflate(this.mContext, R.layout.vplayer_controller_msize, (ViewGroup) null);
            } else {
                this.mView = View.inflate(this.mContext, R.layout.vplayer_controller, (ViewGroup) null);
            }
            this.mView.setFitsSystemWindows(true);
            this.mCurTimeTv = (TextView) this.mView.findViewById(R.id.cur_time);
            this.mTotalTimeTv = (TextView) this.mView.findViewById(R.id.total_time);
            this.mSeekStepTv = (TextView) this.mView.findViewById(R.id.seek_step_progress);
            this.mInfo = (TextView) this.mView.findViewById(R.id.player_overlay_info);
            this.mZoomBttn = (ImageView) this.mView.findViewById(R.id.zoom_bttn);
            if (this.mZoomBttn != null) {
                this.mZoomBttn.setOnClickListener(this);
                updateZoomBttnDrawable(this.mConfig.getInt(AppConfig.ZOOM, 0));
            }
            this.mSettingBttn = (ImageView) this.mView.findViewById(R.id.setting_bttn);
            if (this.mSettingBttn != null) {
                this.mSettingBttn.setOnClickListener(this);
            }
            this.mPrevButton = (ImageView) this.mView.findViewById(R.id.previous_bttn);
            if (this.mPrevButton != null) {
                this.mPrevButton.setOnClickListener(this);
            }
            this.mNextButton = (ImageView) this.mView.findViewById(R.id.next_bttn);
            if (this.mNextButton != null) {
                this.mNextButton.setOnClickListener(this);
            }
            this.mPlayPauseButton = (ImageView) this.mView.findViewById(R.id.playpause_bttn);
            this.mPlayPauseButton.setOnClickListener(this);
            this.mSeekBar = (SeekBar) this.mView.findViewById(R.id.seekbar);
            this.mSeekBar.setOnSeekBarChangeListener(this);
            this.mSeekBar.setMax(1000);
            this.mTopPanel = this.mView.findViewById(R.id.vplayer_top_panel);
            this.mTopPanel.setOnClickListener(this);
            this.mBottomPanel = this.mView.findViewById(R.id.vplayer_bottom_panel);
            this.mBottomPanel.setOnClickListener(this);
            this.mMiscPanel = this.mView.findViewById(R.id.misc_container);
            this.mProgressContainerVolume = this.mView.findViewById(R.id.progress_container_volume);
            this.mProgressContainerLoading = this.mView.findViewById(R.id.progress_container_loading);
            this.mProgressLoadingInfo = (TextView) this.mView.findViewById(R.id.loading_info);
            this.mVideoView.setPlayPauseListener(new VideoSurfaceTextureView.PlayPauseListener() {
                public void updatePlayPause(boolean isPlaying) {
                    MovieControlPanel.this.updatePausePlay(isPlaying);
                }
            });
            this.mTitleTv = (TextView) this.mView.findViewById(R.id.title);
            this.mCloseButton = (ImageView) this.mView.findViewById(R.id.close_button);
            if (this.mCloseButton != null) {
                this.mCloseButton.setOnClickListener(this);
            }
            this.mFloatWindowButton = (ImageView) this.mView.findViewById(R.id.float_window_button);
            if (AppConfig.getInstance(this.mContext).getBoolean(AppConfig.HIDE_ICON_FLOATWINDOW, false)) {
                this.mFloatWindowButton.setVisibility(8);
            } else {
                this.mFloatWindowButton.setOnClickListener(this);
            }
            this.mVideoListButton = (ImageView) this.mView.findViewById(R.id.videolist_bttn);
            if (this.mVideoListButton != null) {
                this.mVideoListButton.setOnClickListener(this);
            }
            this.mFavoritesButton = (ImageView) this.mView.findViewById(R.id.favorites_bttn);
            if (this.mFavoritesButton != null) {
                this.mFavoritesButton.setOnClickListener(this);
            }
            this.mDLNAButton = (ImageView) this.mView.findViewById(R.id.dlna_bttn);
            if (this.mDLNAButton != null) {
                this.mDLNAButton.setOnClickListener(this);
            }
            this.mUnLockButton = (ImageView) this.mView.findViewById(R.id.unlock_bttn);
            if (this.mUnLockButton != null) {
                this.mUnLockButton.setOnClickListener(this);
            }
            this.mLockedButton = (ImageView) this.mView.findViewById(R.id.locked_bttn);
            if (this.mLockedButton != null) {
                this.mLockedButton.setOnClickListener(this);
            }
            this.mProgressLevelView = (ProgressView) this.mView.findViewById(R.id.progress_level);
            this.mProgressTypeView = (ImageView) this.mView.findViewById(R.id.progress_type);
            this.mProgressTypeView.setOnClickListener(this);
            this.mVideoListView = (ListView) this.mView.findViewById(R.id.listview_videolist);
            if (this.mVideoListView != null) {
                this.mAdapterPlaylist = new PlaylistViewAdapter(this.mContext, this.mVideoView.getPathName());
                this.mVideoListView.setAdapter(this.mAdapterPlaylist);
                this.mVideoListView.setOnItemClickListener(this);
                this.mVideoListView.setVisibility(8);
                this.mVideoListView.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent ev) {
                        MovieControlPanel.this.resetHideTimeOut();
                        return false;
                    }
                });
            }
            this.mView.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent ev) {
                    MovieControlPanel.this.resetHideTimeOut();
                    return false;
                }
            });
            if (this.mWindowMode == 1) {
                this.mUsageIV = (ImageView) this.mView.findViewById(R.id.vplayer_usage);
                if (AppConfig.getInstance(this.mContext).getBoolean(AppConfig.VPLAYER_USAGE2, true)) {
                    this.mDisableAutoHide = true;
                    this.mUsageIV.setVisibility(0);
                    AppConfig.getInstance(this.mContext).setBoolean(AppConfig.VPLAYER_USAGE2, false);
                }
            }
            this.mHandler.sendEmptyMessage(6);
        }
        return this.mView;
    }

    /* access modifiers changed from: private */
    public void resetHideTimeOut() {
        if (this.mIsPlaying && !this.mDisableAutoHide && !this.mBuffering) {
            this.mHandler.removeMessages(0);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), (long) this.HIDE_TIME_OUT);
        }
    }

    private void lockUi(boolean lock) {
        if (lock) {
            this.mTopPanel.setVisibility(8);
            this.mBottomPanel.setVisibility(8);
            if (this.mMiscPanel != null) {
                this.mMiscPanel.setVisibility(8);
            }
        } else {
            this.mTopPanel.setVisibility(0);
            this.mBottomPanel.setVisibility(0);
            if (this.mMiscPanel != null) {
                this.mMiscPanel.setVisibility(0);
            }
        }
        this.mVideoView.lockRotation(lock);
        this.mProgressContainerVolume.setVisibility(8);
    }

    private int getProgressDrawable(int level) {
        return this.ProgressDrawables[level];
    }

    public void changeVolume(float change) {
        if (!this.mUiLocked) {
            this.mCurrentVolume = (int) (((float) this.mCurrentVolume) + (((float) this.mMaxVolume) * change));
            if (this.mCurrentVolume > this.mMaxVolume) {
                this.mCurrentVolume = this.mMaxVolume;
            } else if (this.mCurrentVolume < 0) {
                this.mCurrentVolume = 0;
            }
            this.mAudioManager.setStreamVolume(3, this.mCurrentVolume / 100, 0);
            int level = (this.mCurrentVolume * this.mMaxVolumeLevel) / this.mMaxVolume;
            if (this.mCurrentVolume == 0) {
                this.mProgressTypeView.setBackgroundResource(R.drawable.ic_sound_off);
            } else {
                this.mProgressTypeView.setBackgroundResource(R.drawable.ic_sound_on);
            }
            this.mProgressLevelView.changeVolume(level, this.mMaxVolumeLevel);
            resetHideTimeOut();
            this.mHandler.removeMessages(4);
            this.mProgressContainerVolume.setVisibility(0);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), 2000);
        }
    }

    private int getBrightness(int defaultValue) {
        int brightness = defaultValue;
        try {
            brightness = Settings.System.getInt(this.mContext.getContentResolver(), "screen_brightness");
        } catch (Settings.SettingNotFoundException e) {
        }
        if (brightness < this.SCREEN_BRIGHTNESS_MIN) {
            brightness = this.SCREEN_BRIGHTNESS_MIN;
        } else if (brightness > 255) {
            brightness = 255;
        }
        AppConfig.getInstance(this.mContext).setInt(AppConfig.BRIGHTNESS, brightness);
        return brightness;
    }

    private void changeBrightnessIml() {
        if (this.mVideoView.isNormalWindow()) {
            WindowManager.LayoutParams lp = ((VideoPlayerActivity) this.mContext).getWindow().getAttributes();
            lp.screenBrightness = ((float) this.mCurrentBrightness) / 255.0f;
            ((VideoPlayerActivity) this.mContext).getWindow().setAttributes(lp);
        } else {
            WindowManager.LayoutParams lp2 = this.mVideoView.getWindowLp();
            lp2.screenBrightness = ((float) this.mCurrentBrightness) / 255.0f;
            this.mVideoView.setWindowLp(lp2);
        }
        AppConfig.getInstance(this.mContext).setInt(AppConfig.BRIGHTNESS, this.mCurrentBrightness);
    }

    public void changeBrightness(float change) {
        if (!this.mUiLocked) {
            this.mCurrentBrightness = (int) (((float) this.mCurrentBrightness)
                    + (((float) (255 - this.SCREEN_BRIGHTNESS_MIN)) * change));
            if (this.mCurrentBrightness > 255) {
                this.mCurrentBrightness = 255;
            } else if (this.mCurrentBrightness < this.SCREEN_BRIGHTNESS_MIN) {
                this.mCurrentBrightness = this.SCREEN_BRIGHTNESS_MIN;
            }
            changeBrightnessIml();
            if (this.mCurrentBrightness == this.SCREEN_BRIGHTNESS_MIN) {
                this.mProgressTypeView.setBackgroundResource(R.drawable.ic_brightness_off);
            } else {
                this.mProgressTypeView.setBackgroundResource(R.drawable.ic_brightness_on);
            }
            this.mProgressLevelView
                    .changeBrightness(((this.mCurrentBrightness - this.SCREEN_BRIGHTNESS_MIN) * this.mMaxVolumeLevel)
                            / (255 - this.SCREEN_BRIGHTNESS_MIN), this.mMaxVolumeLevel);
            resetHideTimeOut();
            this.mHandler.removeMessages(4);
            this.mProgressContainerVolume.setVisibility(0);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), 2000);
        }
    }

    public FrameLayout.LayoutParams getLayoutParams() {
        if (this.mLayoutParams == null) {
            this.mLayoutParams = new FrameLayout.LayoutParams(-1, -1, 119);
        }
        return this.mLayoutParams;
    }

    public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
        if (fromuser) {
            float newPosition = ((float) this.mVideoView.getDuration()) * (((float) progress) / 1000.0f);
            this.mVideoView.seekTo(Math.round(newPosition));
            if (this.mCurTimeTv != null) {
                this.mCurTimeTv.setText(Utils.stringForTime((long) ((int) newPosition)));
            }
            resetHideTimeOut();
        }
    }

    public void onStartTrackingTouch(SeekBar bar) {
        this.mDragging = true;
    }

    public void onStopTrackingTouch(SeekBar bar) {
        this.mDragging = false;
        resetHideTimeOut();
    }

    /* access modifiers changed from: private */
    public void updateSeekTime() {
        if (!this.mDragging && this.mVideoView.isPlaying()) {
            updateSeekBar(this.mVideoView.getCurrentPosition());
        }
    }

    public void updateSeekBar(int position) {
        long duration = (long) this.mVideoView.getDuration();
        if (this.mSeekBar != null) {
            if (duration > 0) {
                this.mSeekBar.setProgress(Math.round(1000.0f * (((float) position) / ((float) duration))));
            } else {
                this.mSeekBar.setProgress(0);
            }
        }
        if (this.mTotalTimeTv != null) {
            this.mTotalTimeTv.setText(Utils.stringForTime((long) ((int) duration)));
        }
        if (this.mCurTimeTv != null) {
            this.mCurTimeTv.setText(Utils.stringForTime((long) position));
        }
    }

    public void onVideoComplete() {
        this.mCurTimeTv.setText(Utils.stringForTime(0));
        this.mSeekBar.setProgress(0);
    }

    public void onShow() {
        getView().setVisibility(0);
        lockUi(this.mUiLocked);
        if (!this.mUiLocked) {
            this.mView.startAnimation(AnimationUtils.loadAnimation(this.mContext, R.anim.fade_in));
            updateZoomBttnDrawable(this.mVideoView.getZoomMode());
            if (this.mCustomTitle != null) {
                this.mTitleTv.setText(this.mCustomTitle);
            } else {
                this.mTitleTv.setText(this.mVideoView.getVideoName());
            }
            updateSeekTime();
            this.mSeekBar.setSecondaryProgress(this.mSecondaryProgress);
            this.mHandler.sendEmptyMessage(1);
        }
        resetHideTimeOut();
    }

    private void onHide() {
        if (this.mVideoListView != null) {
            this.mVideoListView.setVisibility(8);
        }
        if (this.mUsageIV != null) {
            this.mUsageIV.setVisibility(8);
        }
        this.mDisableAutoHide = false;
        getView().setVisibility(8);
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(0);
    }

    public void updateZoomBttnDrawable(int zoom) {
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

    public void onZoomButtonClick() {
        int zoom = (this.mVideoView.getZoomMode() + 1) % 3;
        switch (zoom) {
            case 0:
                showInfo((int) R.string.full_video_size, 1000);
                break;
            case 1:
                showInfo((int) R.string.full_screen_size, 1000);
                break;
            case 2:
                showInfo((int) R.string.origin_video_size, 1000);
                break;
        }
        this.mVideoView.setZoomMode(zoom);
        updateZoomBttnDrawable(zoom);
        this.mConfig.setInt(AppConfig.ZOOM, zoom);
    }

    public void onFullScreen(boolean isFullScreen) {
        if (isFullScreen) {
            this.mSettingBttn.setVisibility(0);
            this.mZoomBttn.setVisibility(0);
            return;
        }
        this.mZoomBttn.setVisibility(8);
        this.mSettingBttn.setVisibility(8);
    }

    private void toggleSettingDialog() {
        if (this.mSettingDialog == null) {
            this.mSettingDialog = new SettingDialog(this.mContext, R.style.SettingDialog, this.mVideoView);
            this.mSettingDialog.setCancelable(true);
        }
        if (!this.mSettingDialog.isShowing()) {
            this.mSettingDialog.show();
        } else {
            dissmissSettingDialog();
        }
    }

    private void dissmissSettingDialog() {
        if (this.mSettingDialog != null) {
            this.mSettingDialog.dismiss();
            this.mSettingDialog = null;
        }
    }

    public void setProgressLoadingVisibility(int visibility) {
        this.mProgressContainerLoading.setVisibility(visibility);
    }

    public void updatePausePlay(boolean isPlaying) {
        this.mIsPlaying = isPlaying;
        if (isPlaying) {
            this.mPlayPauseButton.setBackgroundResource(R.drawable.bttn_pause_m);
            if (!this.mDisableAutoHide && !this.mBuffering) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), (long) this.HIDE_TIME_OUT);
                return;
            }
            return;
        }
        this.mPlayPauseButton.setBackgroundResource(R.drawable.bttn_play_m);
        this.mHandler.removeMessages(0);
    }

    public void updateTitleView(String title) {
        if (title != null) {
            this.mTitleTv.setText(title);
        } else if (this.mCustomTitle != null) {
            this.mTitleTv.setText(this.mCustomTitle);
        } else {
            this.mTitleTv.setText(this.mVideoView.getVideoName());
        }
    }

    private void updateFavoriteFactor(String path) {
        ContentValues cv = new ContentValues();
        int favorite = AppConfig.getInstance(this.mContext.getApplicationContext()).getInt(AppConfig.FAVORITE_FACTOR, 0)
                + 1;
        AppConfig.getInstance(this.mContext.getApplicationContext()).setInt(AppConfig.FAVORITE_FACTOR, favorite);
        cv.put(LocalMediaProviderContract.FAVORITE_COLUMN, Integer.valueOf(favorite));
        this.mContext.getContentResolver().update(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, cv, "path=?",
                new String[] { path });
    }

    public boolean getUiLockState() {
        return this.mUiLocked;
    }

    public void onClick(View view) {
        resetHideTimeOut();
        int smartEnabled = 0;
        switch (view.getId()) {
            case R.id.zoom_bttn:
                onZoomButtonClick();
                return;
            case R.id.previous_bttn:
                this.mVideoView.previous();
                try {
                    smartEnabled = Settings.System.getInt(this.mContext.getContentResolver(),
                            "smartscreen_pause_enable");
                } catch (Settings.SettingNotFoundException e) {
                }
                if (smartEnabled == 1 && this.mVideoView.isFullScreen()) {
                    this.mVideoView.startSmartCamera();
                    return;
                }
                return;
            case R.id.playpause_bttn:
                if (this.mVideoView.isPlaying()) {
                    this.mVideoView.pause();
                    return;
                }
                this.mVideoView.play();
                try {
                    smartEnabled = Settings.System.getInt(this.mContext.getContentResolver(),
                            "smartscreen_pause_enable");
                } catch (Settings.SettingNotFoundException e2) {
                }
                if (smartEnabled == 1 && this.mVideoView.isFullScreen()) {
                    this.mVideoView.startSmartCamera();
                    return;
                }
                return;
            case R.id.next_bttn:
                this.mVideoView.next();
                try {
                    smartEnabled = Settings.System.getInt(this.mContext.getContentResolver(),
                            "smartscreen_pause_enable");
                } catch (Settings.SettingNotFoundException e3) {
                }
                if (smartEnabled == 1 && this.mVideoView.isFullScreen()) {
                    this.mVideoView.startSmartCamera();
                    return;
                }
                return;
            case R.id.setting_bttn:
                toggleSettingDialog();
                return;
            case R.id.unlock_bttn:
                if (!this.mUiLocked) {
                    this.mUiLocked = true;
                }
                lockUi(this.mUiLocked);
                this.mLockedButton.setVisibility(0);
                return;
            case R.id.videolist_bttn:
                if (this.mVideoListView.getVisibility() == 8) {
                    this.mVideoListView.setVisibility(0);
                    this.mMiscPanel.startAnimation(AnimationUtils.loadAnimation(this.mContext, R.anim.slide_in_right));
                    return;
                }
                Animation animation = AnimationUtils.loadAnimation(this.mContext, 17432579);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    public void onAnimationStart(Animation arg0) {
                    }

                    public void onAnimationRepeat(Animation arg0) {
                    }

                    public void onAnimationEnd(Animation arg0) {
                        MovieControlPanel.this.mVideoListView.setVisibility(8);
                    }
                });
                this.mMiscPanel.startAnimation(animation);
                return;
            case R.id.float_window_button:
                if (this.mVideoView.isFullScreen()) {
                    if (android.os.Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this.mContext)) {
                        Toast.makeText(this.mContext, "Please grant overlay permission", Toast.LENGTH_LONG).show();
                        this.mContext.startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION",
                                android.net.Uri.parse("package:" + this.mContext.getPackageName())));
                        return;
                    }
                    Log.v("fuqiang", "fullscreen to window");
                    if (!PresentationScreenMonitor.getInstance(this.mContext.getApplicationContext())
                            .isWIFIDisplayOn()) {
                        String path = this.mVideoView.getPathName();
                        int videowidth = this.mVideoView.getVideoSizeWidth();
                        int videoheight = this.mVideoView.getVideoSizeHeight();
                        int position = this.mVideoView.getCurrentPosition();
                        Intent intent = new Intent(this.mContext, FloatVideoService.class)
                                .setAction(FloatVideoService.ACTION_SWITCH_MODE);
                        intent.putExtra(LocalMediaProviderContract.PATH_COLUMN, path);
                        intent.putExtra("position", position);
                        intent.putExtra("videowidth", videowidth);
                        intent.putExtra("videoheight", videoheight);
                        this.mContext.startService(intent);
                        ((Activity) this.mContext).finish();
                        return;
                    }
                    return;
                }
                Log.v("fuqiang", "window to fullscreen");
                FloatVideoManager.getInstance(this.mContext).closeAllWindows();
                String path2 = this.mVideoView.getPathName();
                int position2 = this.mVideoView.getCurrentPosition();
                Intent intent2 = new Intent(this.mContext, VideoPlayerActivity.class);
                intent2.putExtra(LocalMediaProviderContract.PATH_COLUMN, path2);
                intent2.putExtra("position", position2).putExtra("boot-mode", "external");
                intent2.setFlags(268435456);
                this.mContext.startActivity(intent2);
                return;
            case R.id.favorites_bttn:
                updateFavoriteFactor(this.mVideoView.getPathName());
                Toast.makeText(this.mContext, this.mContext.getResources().getString(R.string.added_to_favorite), 0)
                        .show();
                return;
            case R.id.locked_bttn:
                if (this.mUiLocked) {
                    this.mUiLocked = false;
                }
                lockUi(this.mUiLocked);
                this.mLockedButton.setVisibility(8);
                return;
            case R.id.close_button:
                FloatWindowService.close(this.mContext, FloatVideoService.class, this.mVideoView.getId());
                return;
            default:
                return;
        }
    }

    private void showInfo(String text, int duration) {
        this.mInfo.setVisibility(0);
        this.mInfo.setText(text);
        this.mHandler.removeMessages(7);
        this.mHandler.sendEmptyMessageDelayed(7, (long) duration);
    }

    private void showInfo(int textid, int duration) {
        this.mInfo.setVisibility(0);
        this.mInfo.setText(textid);
        this.mHandler.removeMessages(7);
        this.mHandler.sendEmptyMessageDelayed(7, (long) duration);
    }

    public void onItemClick(AdapterView<?> adapterView, View arg1, int position, long id) {
        this.mVideoView.openNewSource(((LocalMediaInfo) this.mAdapterPlaylist.getItem(position)).mPath);
        onShow();
    }

    private void showSystemUi(boolean visible) {
        if (this.mWindowMode == 1) {
            int flag = 1792;
            if (!visible) {
                flag = 1792 | 7;
            }
            this.mRoot.setSystemUiVisibility(flag);
        }
    }

    private void updateNetworkStreamFlag() {
        String name = this.mVideoView.getPathName();
        Log.v(TAG, "name is:" + name);
        if (name.startsWith("http://") || name.startsWith("https://")) {
            this.mIsNetworkStream = true;
        } else {
            this.mIsNetworkStream = false;
        }
    }

    public void showLoadingProgress(boolean visible) {
        this.mBuffering = visible;
        if (visible) {
            if (this.mWindowMode == 0) {
                this.mPlayPauseButton.setVisibility(8);
            }
            updateNetworkStreamFlag();
            if (this.mIsNetworkStream) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
            }
            this.mHandler.removeMessages(0);
            setProgressLoadingVisibility(0);
            return;
        }
        if (this.mIsNetworkStream) {
            this.mHandler.removeMessages(3);
        }
        setProgressLoadingVisibility(8);
        resetHideTimeOut();
        if (this.mWindowMode == 0) {
            this.mPlayPauseButton.setVisibility(0);
        }
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    public void showCommonControl(boolean show) {
        if (show) {
            if (!this.mControllerShowing) {
                showBackgound(true);
                onShow();
                this.mControllerShowing = true;
                showSystemUi(true);
            }
        } else if (this.mIsPlaying) {
            onHide();
            showSystemUi(false);
            showBackgound(false);
            this.mControllerShowing = false;
        }
    }

    public void toggleCommonControl() {
        if (!this.mBuffering || !this.mControllerShowing) {
            showCommonControl(!this.mControllerShowing);
            if (!this.mControllerShowing) {
                dissmissSettingDialog();
            }
        }
    }

    public void onBufferingUpdate(int percent) {
        this.mSecondaryProgress = percent * 10;
        if (this.mControllerShowing) {
            this.mSeekBar.setSecondaryProgress(this.mSecondaryProgress);
        }
    }

    public void setCustomTitle(String title) {
        this.mCustomTitle = title;
    }

    public void recycleExtraViews() {
        dissmissSettingDialog();
    }

    private void applyTextStyle(TimedText tt, TextView tv) {
        List<?> styleList = com.dismal.fireplayer.util.TimedTextUtil.AWExtend_getStyleList(tt);
        if (styleList != null) {
            for (int i = 0; i < styleList.size(); i++) {
                Object style = styleList.get(i);
                if (style == null)
                    continue;
                try {
                    if (!this.mUserDefSubColor) {
                        int colorRGBA = style.getClass().getField("colorRGBA").getInt(style);
                        this.mCurrentSubColor = colorRGBA;
                        tv.setTextColor(colorRGBA);
                    }
                    if (!this.mUserDefSubFontSize) {
                        com.dismal.fireplayer.util.TimedTextUtil.AWExtend_getTextScreenBounds(tt);
                    }
                    boolean isBold = style.getClass().getField("isBold").getBoolean(style);
                    boolean isItalic = style.getClass().getField("isItalic").getBoolean(style);
                    if (isBold && isItalic) {
                        tv.setTypeface(Typeface.defaultFromStyle(3));
                    } else if (isBold) {
                        tv.setTypeface(Typeface.defaultFromStyle(1));
                    } else if (isItalic) {
                        tv.setTypeface(Typeface.defaultFromStyle(2));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error applying text style: " + e.getMessage());
                }
            }
        }
    }

    private void calcSubPosition(TimedText tt, FrameLayout.LayoutParams layoutParams) {
        Rect textRect = tt.getBounds();
        Rect screenRect = com.dismal.fireplayer.util.TimedTextUtil.AWExtend_getTextScreenBounds(tt);
        if (textRect != null && screenRect != null) {
            Rect maginRect = new Rect();
            int videoViewWidth = this.mMetric.widthPixels;
            int videoViewHeight = this.mMetric.heightPixels;
            maginRect.left = textRect.left - screenRect.left;
            maginRect.top = textRect.top - screenRect.top;
            maginRect.right = screenRect.right - textRect.right;
            maginRect.bottom = screenRect.bottom - textRect.bottom;
            if (this.mVideoViewPosValid) {
                videoViewWidth = this.mVideoViewWidth;
                videoViewHeight = this.mVideoViewHeight;
                this.mTextFontScale = ((float) videoViewWidth) / (this.mMetric.density * ((float) screenRect.right));
            }
            if (screenRect.right != 0) {
                maginRect.left = (maginRect.left * videoViewWidth) / screenRect.right;
                maginRect.right = (maginRect.right * videoViewWidth) / screenRect.right;
                maginRect.left = (int) TypedValue.applyDimension(0, (float) maginRect.left, this.mMetric);
                maginRect.right = (int) TypedValue.applyDimension(0, (float) maginRect.right, this.mMetric);
            }
            if (screenRect.bottom != 0) {
                maginRect.top = (maginRect.top * videoViewHeight) / screenRect.bottom;
                maginRect.bottom = (maginRect.bottom * videoViewHeight) / screenRect.bottom;
                maginRect.top = (int) TypedValue.applyDimension(0, (float) maginRect.top, this.mMetric);
                maginRect.bottom = (int) TypedValue.applyDimension(0, (float) maginRect.bottom, this.mMetric);
            }
            switch (com.dismal.fireplayer.util.TimedTextUtil.AWExtend_getSubDispPos(tt)) {
                case 17:
                    layoutParams.gravity = 51;
                    layoutParams.leftMargin = maginRect.left;
                    layoutParams.topMargin = this.mVideoViewTop + maginRect.top;
                    return;
                case 18:
                    layoutParams.gravity = 49;
                    layoutParams.topMargin = this.mVideoViewTop + maginRect.top;
                    return;
                case 19:
                    layoutParams.gravity = 53;
                    layoutParams.rightMargin = maginRect.right;
                    layoutParams.topMargin = this.mVideoViewTop + maginRect.top;
                    return;
                case 33:
                    layoutParams.gravity = 19;
                    layoutParams.leftMargin = maginRect.left;
                    return;
                case 34:
                    layoutParams.gravity = 17;
                    return;
                case 35:
                    layoutParams.gravity = 21;
                    layoutParams.rightMargin = maginRect.right;
                    return;
                case 49:
                    layoutParams.gravity = 83;
                    layoutParams.leftMargin = maginRect.left;
                    layoutParams.bottomMargin = maginRect.bottom + this.mVideoViewBottomMargin;
                    return;
                case 50:
                    layoutParams.gravity = 81;
                    layoutParams.bottomMargin = maginRect.bottom + this.mVideoViewBottomMargin;
                    return;
                case 51:
                    layoutParams.gravity = 85;
                    layoutParams.rightMargin = maginRect.right;
                    layoutParams.bottomMargin = maginRect.bottom + this.mVideoViewBottomMargin;
                    return;
                default:
                    return;
            }
        }
    }

    private int getFontHeight(float fontSize) {
        Paint paint = new Paint();
        paint.setTextSize(fontSize);
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int) Math.ceil((double) (fm.descent - fm.top));
    }

    public void setSubTitile(TimedText tt) {
        int systemBarHeight = SYSTEM_BAR_HEIGHT;
        if (tt == null) {
            removeAllSubtitleLayerView();
            return;
        }
        if (this.mWindowMode == 2) {
            systemBarHeight = 0;
        }
        this.mVideoViewBottomMargin = (int) (((float) systemBarHeight) * this.mMetric.density);
        if (this.mVideoView.getVideoSurfaceView() != null) {
            this.mVideoViewLeft = this.mVideoView.getVideoSurfaceView().getLeft();
            if (this.mVideoViewLeft == 0) {
                this.mVideoViewTop = this.mVideoView.getVideoSurfaceView().getTop();
                this.mVideoViewRight = this.mVideoView.getVideoSurfaceView().getRight();
                this.mVideoViewBottom = this.mVideoView.getVideoSurfaceView().getBottom();
                this.mVideoViewWidth = this.mVideoViewRight - this.mVideoViewLeft;
                this.mVideoViewHeight = this.mVideoViewBottom - this.mVideoViewTop;
                this.mVideoViewBottomMargin = Math.max(this.mVideoViewBottomMargin,
                        (this.mMetric.heightPixels - this.mVideoViewBottom)
                                + ((int) (((float) systemBarHeight) * this.mMetric.density)));
                this.mVideoViewPosValid = true;
            }
        }
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
        layoutParams.gravity = 81;
        if (com.dismal.fireplayer.util.TimedTextUtil.AWExtend_getBitmapSubtitleFlag(tt) == 1) {
            Bitmap bm = Bitmap.createBitmap(com.dismal.fireplayer.util.TimedTextUtil.AWExtend_getBitmap(tt));
            ImageView iv = new ImageView(this.mContext);
            float scaleRatio = this.mMetric.density;
            iv.setImageBitmap(bm);
            this.mSubLayerView.setVisibility(0);
            layoutParams.bottomMargin = Math.max(this.mVideoViewBottomMargin,
                    (int) (((float) systemBarHeight) * this.mMetric.density));
            if (this.mVideoView.getVideoSizeWidth() > 0) {
                scaleRatio = (((float) this.mMetric.widthPixels) * 1.0f)
                        / ((float) Math.max(this.mVideoView.getVideoSizeWidth(), bm.getWidth()));
            }
            if (scaleRatio != 1.0f) {
                layoutParams.width = (int) (((float) bm.getWidth()) * scaleRatio);
                layoutParams.height = (int) (((float) bm.getHeight()) * scaleRatio);
            }
            this.mSubLayerView.addView(iv, layoutParams);
        } else if (com.dismal.fireplayer.util.TimedTextUtil.AWExtend_getHideSubFlag(tt) != 0) {
            int id = 0;
            try {
                Method mMethod = TimedText.class.getMethod("AWExtend_getSubtitleID", new Class[0]);
                mMethod.setAccessible(true);
                if (mMethod != null) {
                    try {
                        id = ((Integer) mMethod.invoke(tt, new Object[0])).intValue();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e2) {
                        e2.printStackTrace();
                    } catch (InvocationTargetException e3) {
                        e3.printStackTrace();
                    }
                }
            } catch (NoSuchMethodException e4) {
                Log.w(TAG, "no such method AWExtend_getSubtitleID");
            }
            TextViewInfo mTextViewInfo = this.mTextSubtitleInfo.findTextViewInfo(id);
            if (mTextViewInfo != null) {
                this.mTextSubtitleInfo.removeTextViewInfo(mTextViewInfo);
                this.mSubLayerView.removeView(this.mSubLayerView.findViewWithTag(mTextViewInfo.text));
            }
        } else {
            TextView tv = new TextView(this.mContext);
            tv.setTag(tt.getText());
            tv.setTextSize(2, (float) this.mSubFontSize);
            tv.setTextColor(this.mSubColor);
            tv.setGravity(17);
            tv.setShadowLayer(2.0f, 2.0f, 2.0f, -16777216);
            String text = tt.getText();
            if (text != null && text.endsWith("\n")) {
                text = text.substring(0, text.lastIndexOf("\n"));
            }
            tv.setText(text);
            TextViewInfo mTextViewInfo2 = new TextViewInfo();
            mTextViewInfo2.textView = tv;
            mTextViewInfo2.text = tt.getText();
            mTextViewInfo2.used = false;
            mTextViewInfo2.layoutParams = layoutParams;
            if (com.dismal.fireplayer.util.TimedTextUtil.AWExtend_getSubDispPos(tt) != 0) {
                calcSubPosition(tt, layoutParams);
            } else {
                layoutParams.bottomMargin = this.mVideoViewBottomMargin;
                if (this.mSubLayerView.getChildCount() <= 0 || !this.mLastDispAlignBottom) {
                    this.mLastDispAlignBottom = true;
                } else {
                    layoutParams.bottomMargin = (int) (((float) layoutParams.bottomMargin)
                            + (((float) getFontHeight((float) this.mSubFontSize)) * this.mMetric.density));
                    this.mLastDispAlignBottom = false;
                }
            }
            applyTextStyle(tt, tv);
            try {
                Method mMethod2 = TimedText.class.getMethod("AWExtend_getSubtitleID", new Class[0]);
                mMethod2.setAccessible(true);
                if (mMethod2 != null) {
                    try {
                        mTextViewInfo2.subtitledID = ((Integer) mMethod2.invoke(tt, new Object[0])).intValue();
                    } catch (IllegalArgumentException e5) {
                        e5.printStackTrace();
                    } catch (IllegalAccessException e6) {
                        e6.printStackTrace();
                    } catch (InvocationTargetException e7) {
                        e7.printStackTrace();
                    }
                }
            } catch (NoSuchMethodException e8) {
                Log.w(TAG, "no such method AWExtend_getSubtitleID");
            }
            for (int i = 0; i < this.mTextSubtitleInfo.mTextViewInfoList.size(); i++) {
                if (this.mTextSubtitleInfo.mTextViewInfoList.get(i) != null && mTextViewInfo2 != null
                        && ((TextViewInfo) this.mTextSubtitleInfo.mTextViewInfoList.get(i)).layoutParams != null
                        && mTextViewInfo2.layoutParams != null
                        && ((TextViewInfo) this.mTextSubtitleInfo.mTextViewInfoList
                                .get(i)).layoutParams.bottomMargin == mTextViewInfo2.layoutParams.bottomMargin
                        && ((TextViewInfo) this.mTextSubtitleInfo.mTextViewInfoList.get(
                                i)).layoutParams.leftMargin == ((TextViewInfo) this.mTextSubtitleInfo.mTextViewInfoList
                                        .get(i)).layoutParams.leftMargin
                        && ((TextViewInfo) this.mTextSubtitleInfo.mTextViewInfoList.get(
                                i)).layoutParams.rightMargin == ((TextViewInfo) this.mTextSubtitleInfo.mTextViewInfoList
                                        .get(i)).layoutParams.rightMargin
                        && ((TextViewInfo) this.mTextSubtitleInfo.mTextViewInfoList.get(
                                i)).layoutParams.topMargin == ((TextViewInfo) this.mTextSubtitleInfo.mTextViewInfoList
                                        .get(i)).layoutParams.rightMargin) {
                    Log.d(TAG, "two subtitles with same position !!!");
                    layoutParams.bottomMargin += SUBTITLE_BOTTOM_OFFSET;
                    mTextViewInfo2.layoutParams = layoutParams;
                }
            }
            this.mSubLayerView.setVisibility(0);
            this.mSubLayerView.addView(tv, layoutParams);
            mTextViewInfo2.used = true;
            this.mTextSubtitleInfo.addTextViewInfo(mTextViewInfo2);
        }
    }

    public void removeAllSubtitleLayerView() {
        this.mTextSubtitleInfo.removeAllTextViewInfo();
        this.mSubLayerView.removeAllViews();
    }

    public void reset() {
        this.mUserDefSubFontSize = false;
        this.mCurrentSubFontSize = -1;
        this.mCurrentSubColor = -1;
        removeAllSubtitleLayerView();
    }

    public void setSubColor(int color, boolean userDef) {
        this.mUserDefSubColor = userDef;
        this.mSubColor = color;
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5), 1000);
        for (int i = 0; i < this.mSubLayerView.getChildCount(); i++) {
            View v = this.mSubLayerView.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(color);
            }
        }
    }

    public int getSubColor() {
        if (this.mUserDefSubColor || this.mCurrentSubColor == -1) {
            return this.mSubColor;
        }
        return this.mCurrentSubColor;
    }

    public void setSubFontSize(int size, boolean userDef) {
        this.mUserDefSubFontSize = userDef;
        this.mSubFontSize = size;
        for (int i = 0; i < this.mSubLayerView.getChildCount(); i++) {
            View v = this.mSubLayerView.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextSize((float) size);
            }
        }
    }

    public int getSubFontSize() {
        if (this.mUserDefSubFontSize || this.mCurrentSubFontSize == -1) {
            return this.mSubFontSize;
        }
        return this.mCurrentSubFontSize;
    }

    public void changeSeekProgress(int mode, float change) {
        String seekText;
        if (!this.mUiLocked) {
            if (mode == 0) {
                int currentPosition = this.mVideoView.getCurrentPosition();
                this.mSeekToPos = currentPosition;
                this.mSeekStartPos = currentPosition;
                this.mMovieDuration = this.mVideoView.getDuration();
                this.mProgressContainerVolume.setVisibility(8);
                this.mSeekStepTv.setVisibility(0);
            } else if (mode == 1) {
                this.mVideoView.seekTo(this.mSeekToPos);
                this.mVideoView.play();
                this.mSeekStepTv.setVisibility(8);
            } else if (mode == 2) {
                this.mSeekSecondStep = (int) (change / (10.0f * this.mMetric.density));
                this.mSeekToPos = this.mSeekStartPos + (this.mSeekSecondStep * 1000);
                this.mSeekToPos = Math.max(1, this.mSeekToPos);
                if (this.mMovieDuration > 0) {
                    this.mSeekToPos = Math.min(this.mMovieDuration, this.mSeekToPos);
                }
                updateSeekBar(this.mSeekToPos);
                String seekText2 = String.valueOf("") + " [ ";
                if (this.mSeekSecondStep >= 0) {
                    seekText = String.valueOf(seekText2) + "+";
                } else {
                    seekText = String.valueOf(seekText2) + "-";
                    this.mSeekSecondStep = -this.mSeekSecondStep;
                }
                this.mSeekStepTv
                        .setText(
                                String.valueOf(String
                                        .valueOf(String.valueOf(String.valueOf(seekText) + String.format("%02d",
                                                new Object[] { Integer.valueOf(this.mSeekSecondStep) })) + "  ")
                                        + Utils.stringForTime((long) this.mSeekToPos)) + " ] ");
            }
        }
    }
}
