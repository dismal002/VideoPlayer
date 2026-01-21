package com.dismal.fireplayer.videoplayerui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.dismal.fireplayer.R;
import com.dismal.fireplayer.constant.Constants;
import com.dismal.fireplayer.floatwindow.FloatWindowService;
import com.dismal.fireplayer.provider.LocalMediaInfo;
import com.dismal.fireplayer.provider.LocalMediaProviderContract;
import com.dismal.fireplayer.ui.AppConfig;
import com.dismal.fireplayer.ui.FourKApplication;
import com.dismal.fireplayer.ui.MediaPlayerEventInterface;
import com.dismal.fireplayer.ui.PresentationScreenMonitor;
import com.dismal.fireplayer.ui.VideoPlayerActivity;
import com.dismal.fireplayer.util.Utils;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

@TargetApi(16)
public class VideoSurfaceTextureView
        implements TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener, SurfaceHolder.Callback,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnInfoListener, MediaPlayer.OnTimedTextListener {
    public static final int DISPLAY_2D_LEFT = 1;
    public static final int DISPLAY_2D_ORIGINAL = 0;
    public static final int DISPLAY_2D_TOP = 2;
    private static final int DISPLAY_3D_HDMI_START_INDX = 3;
    public static final int DISPLAY_3D_LEFT_RIGHT_HDMI = 3;
    public static final int DISPLAY_3D_TOP_BOTTOM_HDMI = 4;
    private static final String KEY_3DMODE_INDEX = "x3dmode";
    private static final String KEY_AUDIO_TRACK_INDEX = "audiotrack_index";
    private static final String KEY_PATH_NAME = "video_path_name";
    private static final String KEY_PLAY_STATE = "play_state";
    private static final String KEY_SUB_TRACK_INDEX = "subtrack_index";
    static final int PLAYMODE_PLAY_ONCE = 0;
    static final int PLAYMODE_RANDOM = 4;
    static final int PLAYMODE_REPEAT_ALL = 1;
    static final int PLAYMODE_REPEAT_ONE = 3;
    static final int PLAYMODE_SEQUENCE = 2;
    private static final int STATE_COMPLETED = 6;
    private static final int STATE_CREATING = 0;
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 1;
    private static final int STATE_PAUSED = 5;
    private static final int STATE_PLAYING = 4;
    private static final int STATE_PREPARED = 3;
    private static final int STATE_PREPARING = 2;
    public static final int WINDOW_FLOAT = 0;
    public static final int WINDOW_NORMAL = 1;
    public static final int WINDOW_PRESENTATION = 2;
    public static final int ZOOM_FULL_SCREEN_SIZE = 1;
    public static final int ZOOM_FULL_VIDEO_SIZE = 0;
    public static final int ZOOM_ORIGIN_VIDEO_SIZE = 2;
    /* access modifiers changed from: private */
    public String TAG = "VideoSurfaceTextureView";
    private boolean isFullScreen = false;
    private boolean isMute = false;
    private int m3DMode = 0;
    private int mActivityRotation;
    private AppConfig mAppConfig;
    private ArrayList<AwTrackInfo> mAudioTrackInfo = new ArrayList<>();
    private boolean mBuildin3dScreen = false;
    // private Camera mCameraDevice = null;
    // private Camera.CameraInfo mCameraInfo = null;
    private String mCharset;
    private VideoContainer mContainer;
    private ContentResolver mContentResolver;
    private Context mContext;
    private MovieControlPanel mControlPanel;
    private int mCurrentBufferPercentage;
    private int mCurrentIndex = 0;
    private int mCurrentState = 0;
    private int mDefaultCameraId = 0;
    private DisplayManager mDisplayManager;
    private int mDuration;
    private boolean mExtActivity;
    private GestureDetector mGestureDetector;
    private Handler mHandler = new Handler();
    private int mId;
    private boolean mIsNetworkVideo = false;
    private MediaPlayer mMediaPlayer;
    private MediaPlayerEventInterface.MeidaPlayerEventListener mMeidaPlayerEventListener = null;
    private int mNumberOfCameras = 0;
    private String mPathName;
    private ArrayList<String> mPlayList;
    private int mPlayMode = 0;
    private PlayPauseListener mPlayPauseListener;
    Runnable mPlayRunnable = new Runnable() {
        public void run() {
            VideoSurfaceTextureView.this.play();
        }
    };
    private boolean mPlayWhenResume = true;
    private int mSaved3DMode = 0;
    private int mSavedAudioTrackIdx = 0;
    private int mSavedSubTrackIdx = 0;
    private int mSeekWhenPrepared = 0;
    // private SmartListener mSmartListener = null;
    private boolean mSubColorUDF = false;
    private boolean mSubSizeUDF = false;
    private int mSubcolor;
    private int mSubdelay = 0;
    private boolean mSubgate = false;
    private int mSubsize;
    private ArrayList<String> mSubtitleFilesArray = new ArrayList<>();
    private boolean mSurfaceCreated;
    private boolean mSuspendingFix = false;
    private ArrayList<AwTrackInfo> mTimedTextTrackInfo = new ArrayList<>();
    private String mTitleName;
    private Uri mUri;
    private int mVideoHeight = 0;
    private VideoSurface mVideoSurface;
    private VideoTexture mVideoTexture;
    private int mVideoWidth = 0;
    private PowerManager.WakeLock mWakeLock = null;
    public int mWindowMode = 0;
    private int mZoomMode = 0;
    private Surface surface;
    private boolean toFinish = true;

    public interface PlayPauseListener {
        void updatePlayPause(boolean z);
    }

    public VideoSurfaceTextureView(Context context, View contentview, int id, Intent intent, int windowMode) {
        this.mContext = context;
        this.mId = id;
        this.mWindowMode = windowMode;
        this.mUri = intent.getData();
        this.mPathName = intent.getStringExtra(LocalMediaProviderContract.PATH_COLUMN);
        this.mTitleName = intent.getStringExtra("title");
        this.mSeekWhenPrepared = intent.getIntExtra("position", 0);
        this.mExtActivity = "external".equals(intent.getStringExtra("boot-mode"));
        this.mIsNetworkVideo = false;
        this.mPlayList = FourKApplication.getPlayList();
        if (intent.getAction() == FloatVideoService.ACTION_PLAY_NETURI) {
            this.mIsNetworkVideo = true;
        } else if (intent.getAction() == "android.intent.action.VIEW") {
            initActionView(intent);
        } else {
            initPlayList(this.mPathName);
        }
        this.mSurfaceCreated = false;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mContainer = (VideoContainer) contentview.findViewById(R.id.video_container);
        this.mVideoSurface = (VideoSurface) contentview.findViewById(R.id.video_surface_view);
        this.mVideoTexture = (VideoTexture) contentview.findViewById(R.id.video_surface_texture);
        this.mVideoTexture.setSurfaceTextureListener(this);
        this.mVideoSurface.getHolder().addCallback(this);
        this.mControlPanel = new MovieControlPanel(context, this, this.mWindowMode);
        if (this.mTitleName != null) {
            this.mControlPanel.setCustomTitle(this.mTitleName);
        }
        this.mAppConfig = AppConfig.getInstance(context.getApplicationContext());
        this.mZoomMode = this.mAppConfig.getInt(AppConfig.ZOOM, 0);
        setZoomMode(this.mZoomMode);
        this.mPlayMode = this.mAppConfig.getInt(AppConfig.PlayMode, 0);
        this.mBuildin3dScreen = this.mAppConfig.getBoolean(AppConfig.BUILDIN_3DSCREEN, false);
        this.mControlPanel.showCommonControl(true);
        ControlGestureListener gestureListener = new ControlGestureListener(context, this, this.mControlPanel);
        this.mGestureDetector = new GestureDetector(context, gestureListener);
        this.mContainer.init(this.mContext, this.mGestureDetector, this, this.mControlPanel, gestureListener);
        resetSubTrack();
        if (this.mWindowMode >= 1) {
            this.isFullScreen = true;
            this.mControlPanel.onFullScreen(this.isFullScreen);
        }
        if (this.mWindowMode == 0 && !this.mExtActivity) {
            Log.d(this.TAG, "cool mode internal activity");
            this.mContainer.removeView(this.mVideoSurface);
        }
        // if (this.mAppConfig.getBoolean(AppConfig.SMART_DETECTION_ENABLE, false)) {
        // this.mSmartListener = new SmartListener(this, (SmartListener) null);
        // }
        Log.d(this.TAG, "VideoSurfaceTextureView onCreate()");
    }

    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.mPlayWhenResume = savedInstanceState.getBoolean(KEY_PLAY_STATE);
            this.mSavedSubTrackIdx = savedInstanceState.getInt(KEY_SUB_TRACK_INDEX);
            this.mSavedAudioTrackIdx = savedInstanceState.getInt(KEY_AUDIO_TRACK_INDEX);
            this.mSaved3DMode = savedInstanceState.getInt(KEY_3DMODE_INDEX);
            this.mPathName = savedInstanceState.getString(KEY_PATH_NAME);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_PLAY_STATE, this.mPlayWhenResume);
        outState.putInt(KEY_SUB_TRACK_INDEX, this.mSavedSubTrackIdx);
        outState.putInt(KEY_AUDIO_TRACK_INDEX, this.mSavedAudioTrackIdx);
        outState.putInt(KEY_3DMODE_INDEX, this.mSaved3DMode);
        outState.putString(KEY_PATH_NAME, this.mPathName);
    }

    private void initActionView(Intent intent) {
        Uri uri = intent.getData();
        String scheme = uri.getScheme();
        if (scheme == null) {
            this.mPathName = uri.getPath();
            return;
        }
        if (scheme.equals(LocalMediaProviderContract.SCHEME)) {
            Cursor c = this.mContext.getContentResolver().query(uri, new String[] { "_data" }, (String) null,
                    (String[]) null, (String) null);
            if (c != null) {
                c.moveToFirst();
                this.mPathName = c.getString(0);
                c.close();
            }
        } else if (scheme.equals("file")) {
            this.mPathName = uri.getPath();
        }
        if (scheme.equals("rtsp") || scheme.equals("http") || scheme.equals("https")) {
            this.mPathName = uri.toString();
            this.mIsNetworkVideo = true;
        } else if (this.mPathName != null) {
            initPlayList(this.mPathName);
        } else {
            this.mPathName = uri.getPath();
            initPlayList(this.mPathName);
        }
        try {
            this.mPathName = URLDecoder.decode(this.mPathName, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
        }
    }

    public void hideControls() {
        this.mControlPanel.showCommonControl(false);
    }

    public void showCommonControls() {
        this.mControlPanel.showCommonControl(true);
    }

    public int getZoomMode() {
        return this.mZoomMode;
    }

    public int getVideoSizeWidth() {
        return this.mVideoWidth;
    }

    public int getVideoSizeHeight() {
        return this.mVideoHeight;
    }

    public void setZoomMode(int mode) {
        this.mZoomMode = mode;
        this.mVideoSurface.setZoomMode(mode);
        this.mVideoTexture.setZoomMode(mode);
    }

    public boolean isFullScreen() {
        return this.isFullScreen;
    }

    public boolean isNormalWindow() {
        return this.mWindowMode >= 1;
    }

    public boolean onClose(int id) {
        // releaseSmartCamera();
        updateBookmark(getCurrentPosition());
        release();
        return false;
    }

    public void onHide() {
        // releaseSmartCamera();
        this.mPlayWhenResume = isPlaying();
        this.mSeekWhenPrepared = getCurrentPosition();
        release();
    }

    public void release() {
        set3DMode(0, false);
        this.mControlPanel.recycleExtraViews();
        this.mTimedTextTrackInfo.clear();
        this.mAudioTrackInfo.clear();
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.reset();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
        }
        this.mCurrentState = 1;
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (this.mWindowMode == 0) {
            this.mVideoSurface.setVisibility(4);
            this.surface = new Surface(surfaceTexture);
            openSource(this.mPathName);
        }
    }

    @TargetApi(16)
    public void onPrepared(MediaPlayer mp) {
        MediaPlayer.TrackInfo[] trackInfos = null;
        try {
            trackInfos = this.mMediaPlayer.getTrackInfo();
        } catch (Exception e) {
            Log.e(this.TAG, "getTrackInfo error!");
        }
        if (trackInfos != null && trackInfos.length > 0) {
            for (int i = 0; i < trackInfos.length; i++) {
                if (trackInfos[i] != null) {
                    AwTrackInfo ti = new AwTrackInfo();
                    ti.index = i;
                    ti.name = trackInfos[i].getLanguage();
                    if (trackInfos[i].getTrackType() == 3) {
                        this.mTimedTextTrackInfo.add(ti);
                    } else if (trackInfos[i].getTrackType() == 2) {
                        this.mAudioTrackInfo.add(ti);
                    }
                }
            }
            setSubGate(this.mSubgate);
        }
        if (this.mTimedTextTrackInfo.size() > 0 && this.mSubgate) {
            this.mMediaPlayer.selectTrack(this.mTimedTextTrackInfo.get(this.mSavedSubTrackIdx).index);
        }
        this.mCurrentState = 3;
        this.mControlPanel.showLoadingProgress(false);
        if (this.mSeekWhenPrepared > 0) {
            this.mMediaPlayer.seekTo(this.mSeekWhenPrepared);
        }
        if (this.isMute) {
            this.mMediaPlayer.setVolume(0.0f, 0.0f);
        }
        setSubCharset(this.mCharset);
        if (this.mSavedAudioTrackIdx > 0) {
            switchAudioTrack(this.mSavedAudioTrackIdx);
        }
        this.mMediaPlayer.start();
        this.mSeekWhenPrepared = 0;
        this.mCurrentState = 4;
        if (this.mPlayPauseListener != null) {
            this.mPlayPauseListener.updatePlayPause(true);
        }
    }

    public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
        Log.v(this.TAG, "wxh = " + w + " : " + h);
        if (this.mVideoWidth == 0 && this.mVideoHeight == 0) {
            this.mVideoWidth = w;
            this.mVideoHeight = h;
        }
        this.mVideoSurface.setVideoSize(w, h);
        this.mVideoTexture.setVideoSize(w, h);
    }

    private void releaseSurfaceByHand() throws InstantiationException, IllegalAccessException {
        try {
            Method method = this.mMediaPlayer.getClass().getMethod("releaseSurfaceByHand", new Class[0]);
            if (method != null) {
                try {
                    method.invoke(this.mMediaPlayer, new Object[0]);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e2) {
                    e2.printStackTrace();
                } catch (InvocationTargetException e3) {
                    e3.printStackTrace();
                }
            }
        } catch (NoSuchMethodException e4) {
            Log.w(this.TAG, "no such method releaseSurfaceByHand");
        }
    }

    public void onCompletion(MediaPlayer mp) {
        // releaseSmartCamera();
        set3DMode(0, false);
        updateBookmark(0);
        this.mSeekWhenPrepared = 0;
        this.mMediaPlayer.stop();
        this.mCurrentState = 6;
        this.mCurrentIndex = nextIndex(1);
        if (this.toFinish || this.mWindowMode == 0 || this.mIsNetworkVideo) {
            if (!this.toFinish && this.mWindowMode == 0) {
                try {
                    releaseSurfaceByHand();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                updatePathName(this.mCurrentIndex);
                resetSubTrack();
                if (!openSource(this.mPathName) && (this.mContext instanceof Activity)) {
                    ((Activity) this.mContext).finish();
                }
            }
            if (this.mMeidaPlayerEventListener != null) {
                this.mMeidaPlayerEventListener.onCompletion();
            }
            if (this.mContext instanceof Activity) {
                ((Activity) this.mContext).finish();
            }
            this.mControlPanel.updatePausePlay(false);
            this.mControlPanel.onVideoComplete();
        } else {
            try {
                releaseSurfaceByHand();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            updatePathName(this.mCurrentIndex);
            resetSubTrack();
            if (!openSource(this.mPathName) && (this.mContext instanceof Activity)) {
                ((Activity) this.mContext).finish();
            }
        }
        this.mControlPanel.updateTitleView((String) null);
        this.mControlPanel.recycleExtraViews();
        this.mControlPanel.showCommonControl(true);
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        int messageId;
        Log.e(this.TAG, "onError().." + what + "," + extra);
        this.mCurrentState = -1;
        if (this.mWindowMode == 0) {
            releaseWakeLock();
        }
        if (what == 200) {
            messageId = 17039381;
        } else if (what == 900) {
            messageId = R.string.VideoView_error_system_busy;
        } else {
            messageId = 17039377;
        }
        Toast.makeText(this.mContext, messageId, 0).show();
        if (this.mWindowMode == 0) {
            FloatWindowService.close(this.mContext, FloatVideoService.class, this.mId);
        } else if (this.mWindowMode == 1) {
            ((VideoPlayerActivity) this.mContext).finish();
        } else if (this.mWindowMode == 2 && this.mMeidaPlayerEventListener != null) {
            this.mMeidaPlayerEventListener.onMediaPlayerError();
        }
        return true;
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface2, int width, int height) {
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface2) {
        Log.d("VideoSurfaceTextureView", "onSurfaceTextureDestroyed1 ");
        releaseWakeLock();
        surface2.release();
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface2) {
    }

    private void acquireWakeLock() {
        if (this.mWakeLock == null) {
            this.mWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(10, "floatvideo");
            this.mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
    }

    public void setPlayPauseListener(PlayPauseListener l) {
        this.mPlayPauseListener = l;
    }

    public void setMeidaPlayerEventListener(MediaPlayerEventInterface.MeidaPlayerEventListener l) {
        this.mMeidaPlayerEventListener = l;
    }

    public boolean inPauseState() {
        return this.mCurrentState == 5;
    }

    private boolean inWaitingPreparedState() {
        return this.mCurrentState == 2;
    }

    private boolean inPlaybackState() {
        return this.mMediaPlayer != null && this.mCurrentState >= 3;
    }

    public boolean inCompleteState() {
        return this.mCurrentState == 6;
    }

    public void saveState() {
        this.mSaved3DMode = getCurrent3DMode();
    }

    public void suspend() {
        boolean updateBookmark = true;
        // releaseSmartCamera();
        this.mSuspendingFix = true;
        if (this.mMediaPlayer != null) {
            this.mPlayWhenResume = isPlaying();
            this.mSeekWhenPrepared = getCurrentPosition();
            this.mControlPanel.updatePausePlay(false);
            if (this.mCurrentState < 3) {
                updateBookmark = false;
            }
            saveState();
            release();
            if (updateBookmark) {
                updateBookmark(this.mSeekWhenPrepared);
            }
        }
    }

    public void resume() {
        this.mSuspendingFix = false;
        int smartEnabled = 0;
        String configStr = this.mAppConfig.getString(AppConfig.SUBCOLOR_UDF, (String) null);
        if (configStr != null && configStr.equals(this.mPathName)) {
            this.mSubColorUDF = true;
        }
        String configStr2 = this.mAppConfig.getString(AppConfig.SUBSIZE_UDF, (String) null);
        if (configStr2 != null && configStr2.equals(this.mPathName)) {
            this.mSubSizeUDF = true;
        }
        if (this.mSurfaceCreated || this.mWindowMode == 0) {
            openSource(this.mPathName);
        }
        try {
            smartEnabled = Settings.System.getInt(this.mContext.getContentResolver(), "smartscreen_pause_enable");
        } catch (Settings.SettingNotFoundException e) {
        }
        // if (smartEnabled == 1 && isFullScreen()) {
        // startSmartCamera();
        // }
    }

    private void updateBookmark(int bookmark) {
        ContentValues cv = new ContentValues();
        cv.put(LocalMediaProviderContract.BOOKMARK_COLUMN, Integer.valueOf(bookmark));
        int latestPlayTime = AppConfig.getInstance(this.mContext.getApplicationContext())
                .getInt(AppConfig.LATEST_PLAYTIME, 0) + 1;
        AppConfig.getInstance(this.mContext.getApplicationContext()).setInt(AppConfig.LATEST_PLAYTIME, latestPlayTime);
        cv.put(LocalMediaProviderContract.LATEST_PLAY_COLUMN, Integer.valueOf(latestPlayTime));
        this.mContentResolver.update(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, cv, "path=?",
                new String[] { this.mPathName });
        Intent it = new Intent();
        it.setAction(Constants.BROADCAST_ACTION_BOOKMARK);
        it.putExtra(Constants.EXTENDED_BOOKMARK_PATH, this.mPathName);
        it.putExtra(Constants.EXTENDED_BOOKMARK_DURATION, bookmark);
        it.addCategory("android.intent.category.DEFAULT");
        this.mContext.sendBroadcast(it);
    }

    private int getBookmark(String path) {
        String[] projection = { LocalMediaProviderContract.ROW_ID, LocalMediaProviderContract.BOOKMARK_COLUMN };
        Cursor cursor = this.mContentResolver.query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, projection,
                "path=?", new String[] { path }, (String) null);
        if (cursor == null || !cursor.moveToFirst()) {
            return 0;
        }
        int bookmark = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.BOOKMARK_COLUMN));
        cursor.close();
        return bookmark;
    }

    public void play() {
        if (this.mCurrentState == 5 || this.mCurrentState == 3) {
            this.mMediaPlayer.start();
            this.mCurrentState = 4;
            if (this.mPlayPauseListener != null) {
                this.mPlayPauseListener.updatePlayPause(true);
            }
            if (this.mWindowMode == 0) {
                acquireWakeLock();
            }
        } else if (this.mCurrentState == 6 || this.mCurrentState == 1) {
            openSource(this.mPathName);
        } else if (this.mCurrentState == -1) {
            Log.w(this.TAG, "re-open at mCurrentState STATE_ERROR");
            openSource(this.mPathName);
        } else {
            Log.e(this.TAG, "call play() but do nothing mCurrentState=" + this.mCurrentState);
        }
    }

    public void pause() {
        if (this.mCurrentState == 4) {
            this.mSeekWhenPrepared = this.mMediaPlayer.getCurrentPosition();
            this.mMediaPlayer.pause();
            this.mCurrentState = 5;
            if (this.mPlayPauseListener != null) {
                this.mPlayPauseListener.updatePlayPause(false);
            }
            if (this.mWindowMode == 0) {
                releaseWakeLock();
                return;
            }
            return;
        }
        Log.e(this.TAG, "call pause() but do nothing mCurrentState=" + this.mCurrentState);
    }

    public void pauseSomeTime(int msec) {
        pause();
        this.mHandler.removeCallbacks(this.mPlayRunnable);
        this.mHandler.postDelayed(this.mPlayRunnable, (long) msec);
    }

    public void seekTo(int msec) {
        if (inWaitingPreparedState()) {
            this.mSeekWhenPrepared = msec;
        } else if (this.mCurrentState == 6) {
            this.mSeekWhenPrepared = msec;
            this.mCurrentIndex = nextIndex(1);
            updatePathName(this.mCurrentIndex);
            openSource(this.mPathName);
        } else if (inPlaybackState()) {
            this.mMediaPlayer.seekTo(msec);
        }
    }

    public void next() {
        if (!this.mIsNetworkVideo) {
            this.mPlayList = FourKApplication.getPlayList();
            int size = this.mPlayList.size();
            if (size != 0) {
                updateBookmark(getCurrentPosition());
                this.mPlayWhenResume = true;
                this.mSeekWhenPrepared = 0;
                if (this.mPlayMode == 4) {
                    this.mCurrentIndex = nextIndex(1);
                } else {
                    this.mCurrentIndex = (this.mCurrentIndex + 1) % size;
                }
                updatePathName(this.mCurrentIndex);
                resetSubTrack();
                openSource(this.mPathName);
                this.mControlPanel.onShow();
            }
        }
    }

    public void previous() {
        if (!this.mIsNetworkVideo) {
            this.mPlayList = FourKApplication.getPlayList();
            int size = this.mPlayList.size();
            if (size != 0) {
                updateBookmark(getCurrentPosition());
                this.mPlayWhenResume = true;
                this.mSeekWhenPrepared = 0;
                if (this.mPlayMode == 4) {
                    this.mCurrentIndex = nextIndex(1);
                } else {
                    this.mCurrentIndex = ((this.mCurrentIndex - 1) + size) % size;
                }
                updatePathName(this.mCurrentIndex);
                resetSubTrack();
                openSource(this.mPathName);
                this.mControlPanel.onShow();
            }
        }
    }

    public void openNewSource(String path) {
        updateBookmark(getCurrentPosition());
        this.mPlayWhenResume = true;
        this.mSeekWhenPrepared = 0;
        this.mPathName = path;
        resetSubTrack();
        openSource(this.mPathName);
        this.mControlPanel.onShow();
    }

    private void resetSubTrack() {
        String configStr = this.mAppConfig.getString(AppConfig.SUB_TRACK_INDEX, (String) null);
        if (configStr == null || !configStr.substring(configStr.indexOf(35) + 1).equals(this.mPathName)) {
            this.mSavedSubTrackIdx = 0;
        } else {
            this.mSavedSubTrackIdx = Integer.parseInt(configStr.substring(0, configStr.indexOf(35)));
        }
        String configStr2 = this.mAppConfig.getString(AppConfig.AUDIO_TRACK_INDEX, (String) null);
        if (configStr2 == null || !configStr2.substring(configStr2.indexOf(35) + 1).equals(this.mPathName)) {
            this.mSavedAudioTrackIdx = 0;
        } else {
            this.mSavedAudioTrackIdx = Integer.parseInt(configStr2.substring(0, configStr2.indexOf(35)));
        }
    }

    private void updatePathName(int index) {
        if (this.mPlayList != null && index < this.mPlayList.size()) {
            this.mPathName = this.mPlayList.get(index);
        }
    }

    public int getCurrentPosition() {
        if (this.mCurrentState == 5 || this.mCurrentState == 3) {
            return this.mSeekWhenPrepared;
        }
        if (isPlaying()) {
            return this.mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (!inPlaybackState()) {
            return 0;
        }
        if (this.mDuration == 0) {
            this.mDuration = this.mMediaPlayer.getDuration();
        }
        return this.mDuration;
    }

    public boolean isPlaying() {
        return this.mCurrentState == 4;
    }

    private int nextIndex(int step) {
        int size = this.mPlayList.size();
        int next = this.mCurrentIndex;
        this.mPlayWhenResume = true;
        this.toFinish = true;
        if (size == 0) {
            switch (this.mPlayMode) {
                case 1:
                case 3:
                case 4:
                    this.toFinish = false;
                    return next;
                default:
                    return next;
            }
        } else {
            switch (this.mPlayMode) {
                case 1:
                    int next2 = ((this.mCurrentIndex + step) + size) % size;
                    this.toFinish = false;
                    return next2;
                case 2:
                    int next3 = ((this.mCurrentIndex + step) + size) % size;
                    if (next3 <= 0) {
                        return next3;
                    }
                    this.toFinish = false;
                    return next3;
                case 3:
                    this.toFinish = false;
                    return next;
                case 4:
                    int next4 = new Random().nextInt(size);
                    this.toFinish = false;
                    return next4;
                default:
                    return next;
            }
        }
    }

    private void searchExternalSubtileFiles(String vPath) {
        File vFolderPath = new File(vPath.substring(0, vPath.lastIndexOf("/")));
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase(Locale.US).matches(
                        "^.*?\\.(idx|sub|smi|srt|rt|txt|ssa|aqt|jss|js|ass|vsf|tts|stl|zeg|ovr|dks|lrc|pan|sbt|vkt|pjs|mpl|scr|psb|asc|rtf|s2k|sst|son|ssts)$");
            }
        };
        this.mSubtitleFilesArray.clear();
        File[] files = vFolderPath.listFiles(filter);
        if (files != null) {
            String vFileBaseName = vPath.substring(0, vPath.lastIndexOf("."));
            for (File f : files) {
                if (f.isFile()) {
                    String path = f.getPath();
                    if (path.startsWith(vFileBaseName)) {
                        Log.v(this.TAG, "add subtitle: " + path);
                        this.mSubtitleFilesArray.add(path);
                    }
                }
            }
        }
    }

    @TargetApi(16)
    private void setTimedText() {
        int idxPos = -1;
        int subPos = -1;
        for (int i = 0; i < this.mSubtitleFilesArray.size(); i++) {
            String subPath = this.mSubtitleFilesArray.get(i);
            if (subPath.endsWith(".idx")) {
                idxPos = i;
            } else if (subPath.endsWith(".sub")) {
                subPos = i;
            }
        }
        if (idxPos < 0 || subPos < 0) {
            for (int i2 = 0; i2 < this.mSubtitleFilesArray.size(); i2++) {
                String subPath2 = this.mSubtitleFilesArray.get(i2);
                String mime = ("text/" + subPath2.substring(subPath2.lastIndexOf(".") + 1)).toLowerCase(Locale.US);
                if (mime.equals("text/srt")) {
                    mime = "application/x-subrip";
                } else if (mime.equals("text/sub")) {
                    mime = "application/sub";
                }
                Log.v(this.TAG, "addTimedTextSource=" + subPath2 + " mime=" + mime);
                try {
                    this.mMediaPlayer.addTimedTextSource(subPath2, mime);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e2) {
                    e2.printStackTrace();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            return;
        }
        try {
            this.mMediaPlayer.addTimedTextSource(this.mSubtitleFilesArray.get(idxPos), "application/idx-sub");
            Log.v(this.TAG, "addTimedTextSource 0=" + this.mSubtitleFilesArray.get(subPos));
            this.mMediaPlayer.addTimedTextSource(this.mSubtitleFilesArray.get(subPos), "application/sub");
        } catch (IllegalArgumentException e4) {
            e4.printStackTrace();
        } catch (IllegalStateException e5) {
            e5.printStackTrace();
        } catch (IOException e6) {
            e6.printStackTrace();
        }
    }

    @TargetApi(16)
    public boolean openSource(String path) {
        this.mPathName = path;
        this.mSuspendingFix = false;
        searchExternalSubtileFiles(path);
        if (this.mSeekWhenPrepared == 0) {
            this.mSeekWhenPrepared = getBookmark(this.mPathName);
        }
        this.mControlPanel.reset();
        this.mControlPanel.showLoadingProgress(true);
        if (this.mPathName == null) {
            Log.e(this.TAG, "can not open source, mUri = null");
            return false;
        }
        Intent intent = new Intent("com.android.music.musicservicecommand");
        intent.putExtra("command", "pause");
        this.mContext.sendBroadcast(intent);
        release();
        // Check if path is readable instead of hardcoding /mnt/usbhost
        File pathFile = new File(this.mPathName);
        if (pathFile.canRead()) {
            if (this.mWindowMode == 0) {
                acquireWakeLock();
                FloatVideoManager.getInstance(this.mContext).requestAudioFocus();
            }
            if (this.mMeidaPlayerEventListener != null) {
                this.mMeidaPlayerEventListener.onNewSource(this.mPathName);
            }
            this.mMediaPlayer = new MediaPlayer();
            initVideoPlayer();
            try {
                if (this.mWindowMode == 0 && this.surface != null) {
                    this.mMediaPlayer.setSurface(this.surface);
                } else if (this.mWindowMode >= 1 && this.mVideoSurface.getHolder() != null) {
                    this.mMediaPlayer.setDisplay(this.mVideoSurface.getHolder());
                }
                this.mMediaPlayer.setOnPreparedListener(this);
                this.mMediaPlayer.setOnBufferingUpdateListener(this);
                this.mMediaPlayer.setOnCompletionListener(this);
                this.mMediaPlayer.setOnPreparedListener(this);
                this.mMediaPlayer.setOnVideoSizeChangedListener(this);
                this.mMediaPlayer.setOnInfoListener(this);
                this.mMediaPlayer.setOnErrorListener(this);
                this.mMediaPlayer.setOnTimedTextListener(this);
                this.mMediaPlayer.setAudioStreamType(3);
                this.mVideoWidth = 0;
                this.mVideoHeight = 0;
                if (this.mUri == null || this.mUri.getPath().startsWith("/storage")
                        || this.mUri.getPath().startsWith(Constants.MEDIA_ROOT_PATH)) {
                    this.mMediaPlayer.setDataSource(Utils.convertMediaPath(this.mPathName));
                } else {
                    this.mMediaPlayer.setDataSource(this.mContext, this.mUri);
                }
                setTimedText();
                this.mMediaPlayer.setScreenOnWhilePlaying(true);
                this.mMediaPlayer.prepareAsync();
                this.mCurrentState = 2;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e2) {
                e2.printStackTrace();
            } catch (IllegalStateException e3) {
                e3.printStackTrace();
            } catch (IOException e4) {
                this.mCurrentState = -1;
                e4.printStackTrace();
                Toast.makeText(this.mContext, R.string.VideoView_error_not_support, 0).show();
                if (this.mContext instanceof Activity) {
                    ((Activity) this.mContext).finish();
                }
                release();
            }
            return true;
        }
        Log.v(this.TAG, "check file read fail!");
        this.mCurrentState = 6;
        this.mControlPanel.showCommonControl(true);
        this.mControlPanel.showLoadingProgress(false);
        return false;
    }

    private void initVideoPlayer() {
        this.mDuration = 0;
        this.mCurrentBufferPercentage = 0;
        this.m3DMode = 0;
        if (this.mSaved3DMode > 0) {
            this.m3DMode = this.mSaved3DMode;
            set3DMode(this.m3DMode, false);
            this.mSaved3DMode = 0;
        }
        if (this.mWindowMode == 0 && !this.isFullScreen) {
            this.mSubgate = false;
        } else if (this.mWindowMode >= 1) {
            this.mSubgate = this.mAppConfig.getBoolean(AppConfig.SUBGATE, true);
        }
        this.mSubcolor = this.mAppConfig.getInt(AppConfig.SUBCOLOR, -1);
        this.mSubsize = this.mAppConfig.getInt(AppConfig.SUBSIZE, 30);
        this.mSubdelay = this.mAppConfig.getInt(AppConfig.SUB_DELAY, 0);
        this.mCharset = this.mContext.getResources().getStringArray(R.array.subtitle_charset_values)[this.mAppConfig
                .getInt(AppConfig.SUBCODING, 1)];
        if (!this.mPathName.equals(this.mAppConfig.getString(AppConfig.SUB_OLD_PATH, (String) null))) {
            this.mControlPanel.setSubColor(this.mSubcolor, true);
            this.mControlPanel.setSubFontSize(this.mSubsize, false);
            this.mAppConfig.setString(AppConfig.SUB_OLD_PATH, this.mPathName);
            this.mSubdelay = 0;
            this.mAppConfig.setInt(AppConfig.SUB_DELAY, 0);
        } else {
            if (this.mAppConfig.getString(AppConfig.SUBCOLOR_UDF, (String) null) != null) {
                this.mControlPanel.setSubColor(this.mSubcolor, true);
            } else {
                this.mControlPanel.setSubColor(this.mSubcolor, false);
            }
            if (this.mAppConfig.getString(AppConfig.SUBSIZE_UDF, (String) null) != null) {
                this.mControlPanel.setSubFontSize(this.mSubsize, true);
            } else {
                this.mControlPanel.setSubFontSize(this.mSubsize, false);
            }
        }
        this.mSubColorUDF = false;
        this.mSubSizeUDF = false;
    }

    public ArrayList<AwTrackInfo> getSubList() {
        if (this.mMediaPlayer == null || this.mCurrentState >= 6) {
            return null;
        }
        return this.mTimedTextTrackInfo;
    }

    public int getCurSub() {
        if (this.mMediaPlayer != null) {
            return this.mSavedSubTrackIdx;
        }
        return 0;
    }

    public int switchSub(int index) {
        if (this.mMediaPlayer == null || this.mTimedTextTrackInfo.size() <= index) {
            return 0;
        }
        this.mMediaPlayer.selectTrack(this.mTimedTextTrackInfo.get(index).index);
        this.mSavedSubTrackIdx = index;
        this.mAppConfig.setString(AppConfig.SUB_TRACK_INDEX, String.valueOf(index) + "#" + this.mPathName);
        return 0;
    }

    public void setSubGate(boolean showSub) {
        if (this.mTimedTextTrackInfo.size() > this.mSavedSubTrackIdx) {
            if (!showSub) {
                try {
                    this.mMediaPlayer.deselectTrack(this.mTimedTextTrackInfo.get(this.mSavedSubTrackIdx).index);
                } catch (Exception e) {
                    Log.e(this.TAG, "MediaPlayer deselectTrack error!");
                }
            } else {
                try {
                    this.mMediaPlayer.selectTrack(this.mTimedTextTrackInfo.get(this.mSavedSubTrackIdx).index);
                } catch (Exception e2) {
                    Log.e(this.TAG, "MediaPlayer selectTrack error!");
                }
            }
            this.mSubgate = showSub;
        }
    }

    public boolean getSubGate() {
        return this.mSubgate;
    }

    public int setSubColor(int color) {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        this.mControlPanel.setSubColor(color, true);
        this.mAppConfig.setString(AppConfig.SUBCOLOR_UDF, this.mPathName);
        return this.mSubcolor;
    }

    public int getSubColor() {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        return this.mControlPanel.getSubColor();
    }

    public int setSubFontSize(int size) {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        this.mControlPanel.setSubFontSize(size, true);
        this.mAppConfig.setString(AppConfig.SUBSIZE_UDF, this.mPathName);
        return 0;
    }

    public int getSubFontSize() {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        return this.mControlPanel.getSubFontSize();
    }

    public void setSubCharset(String charset) {
        if (this.mMediaPlayer != null) {
            Utils.invokeMethod(this.mMediaPlayer, "setSubCharset", new Object[] { charset });
        }
    }

    public String getSubCharset() {
        if (this.mMediaPlayer == null) {
            return null;
        }
        return com.dismal.fireplayer.util.ReflectionUtil.getSubCharset(this.mMediaPlayer);
    }

    public int setSubDelay(int time) {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        this.mAppConfig.setInt(AppConfig.SUB_DELAY, time);
        return com.dismal.fireplayer.util.ReflectionUtil.setSubDelay(this.mMediaPlayer, time);
    }

    public int getSubDelay() {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        this.mSubdelay = this.mAppConfig.getInt(AppConfig.SUB_DELAY, 0);
        return this.mSubdelay;
    }

    public ArrayList<AwTrackInfo> getAudioTrackList() {
        if (this.mMediaPlayer == null || this.mCurrentState >= 6) {
            return null;
        }
        return this.mAudioTrackInfo;
    }

    public int getCurAudioTrack() {
        if (this.mMediaPlayer != null) {
            return this.mSavedAudioTrackIdx;
        }
        return -1;
    }

    public int switchAudioTrack(int index) {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        if (this.mAudioTrackInfo.size() > index) {
            this.mMediaPlayer.selectTrack(this.mAudioTrackInfo.get(index).index);
            this.mSavedAudioTrackIdx = index;
            this.mAppConfig.setString(AppConfig.AUDIO_TRACK_INDEX, String.valueOf(index) + "#" + this.mPathName);
        }
        return 0;
    }

    public String[] get3DModeList() {
        String[] sarr = this.mContext.getResources().getStringArray(R.array.mode_3d_list);
        if (this.mBuildin3dScreen) {
            for (int i = 1; i < Math.min(sarr.length, 3); i++) {
                if (sarr[i].endsWith("(HDMI)")) {
                    sarr[i] = sarr[i].substring(0, sarr[i].indexOf("(HDMI)"));
                }
            }
        }
        return sarr;
    }

    public int getCurrent3DMode() {
        return this.m3DMode;
    }

    public boolean set3DMode(int mode3d, boolean mManual) {
        int mode3dToSet;
        if (!Utils.isSdkJB42OrAbove() || !Utils.isSdkSoftwinner()) {
            return false;
        }
        PresentationScreenMonitor presentationScreenMonitor = PresentationScreenMonitor
                .getInstance(this.mContext.getApplicationContext());
        Display display = presentationScreenMonitor.getPresentationDisplay();
        if (mode3d > 0) {
            mode3dToSet = mode3d + 2;
        } else {
            mode3dToSet = 0;
        }
        this.mVideoSurface.set3DMode(mode3dToSet);
        this.mDisplayManager = presentationScreenMonitor.getDisplayManager();
        if (display != null && com.dismal.fireplayer.util.ReflectionUtil.getDisplayType(display) == 2) {
            Log.v(this.TAG, "set 3DMode hdmi =" + mode3d);
            Utils.invokeMethod(this.mDisplayManager, "setDisplay3DMode",
                    new Object[] {
                            Integer.valueOf(com.dismal.fireplayer.util.ReflectionUtil.getDisplayType(display)),
                            Integer.valueOf(mode3dToSet) });
            if (mManual) {
                if (mode3dToSet >= 3) {
                    setZoomMode(1);
                } else {
                    setZoomMode(0);
                }
            }
        } else if (this.mBuildin3dScreen) {
            Log.v(this.TAG, "set 3DMode buildin =" + mode3d);
            Utils.invokeMethod(this.mDisplayManager, "setDisplay3DMode",
                    new Object[] { 1, Integer.valueOf(mode3dToSet) });
            if (mManual) {
                if (mode3dToSet >= 3) {
                    setZoomMode(0);
                } else {
                    setZoomMode(0);
                }
            }
        } else if (mode3dToSet >= 3) {
            Log.w(this.TAG, "set 3d mode fail!");
            return false;
        } else {
            Log.v(this.TAG, "set 3DMode buildin =" + mode3d);
            Utils.invokeMethod(this.mDisplayManager, "setDisplay3DMode",
                    new Object[] { 1, Integer.valueOf(mode3dToSet) });
            if (isHdmi3DMode() && mode3d == 0) {
                Utils.invokeMethod(this.mDisplayManager, "setDisplay3DMode",
                        new Object[] { 2, Integer.valueOf(mode3dToSet) });
            }
        }
        this.m3DMode = mode3d;
        return true;
    }

    public boolean isHdmi3DMode() {
        return this.m3DMode >= 3;
    }

    public int setAnaglaghType(int type) {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        return 0;
    }

    public int getAnaglaghType() {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        return 0;
    }

    public int setInputDimensionType(int type) {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        return 0;
    }

    public int getInputDimensionType() {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        return 0;
    }

    public int setOutputDimensionType(int type) {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        return 0;
    }

    public int getOutputDimensionType() {
        if (this.mMediaPlayer == null) {
            return -1;
        }
        return 0;
    }

    public int getBufferPercentage() {
        if (this.mMediaPlayer != null) {
            return this.mCurrentBufferPercentage;
        }
        return 0;
    }

    public void setPlayMode(int mode) {
        this.mPlayMode = mode;
    }

    public void setMute(boolean mute) {
        if (this.mMediaPlayer != null) {
            this.isMute = mute;
            if (mute) {
                this.mMediaPlayer.setVolume(0.0f, 0.0f);
            } else {
                this.mMediaPlayer.setVolume(0.7f, 0.7f);
            }
        }
    }

    public String getVideoName() {
        if (this.mPathName != null) {
            int index = this.mPathName.lastIndexOf(47);
            int length = this.mPathName.length();
            if (index >= 0 && index < length - 1) {
                return this.mPathName.substring(index + 1);
            }
        }
        return "";
    }

    public String getPathName() {
        return this.mPathName;
    }

    public int getId() {
        return this.mId;
    }

    public VideoContainer getContainer() {
        return this.mContainer;
    }

    public WindowManager.LayoutParams getWindowLp() {
        return ((FloatWindowService) this.mContext).getWindow(this.mId).getLayoutParams();
    }

    public void setWindowLp(WindowManager.LayoutParams lp) {
        ((FloatWindowService) this.mContext).updateViewLayout(this.mId, (FloatWindowService.FloatLayoutParams) lp);
    }

    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d(this.TAG, "onInfo()..what = " + what + ", extra = " + extra);
        if (what == 701) {
            this.mControlPanel.showLoadingProgress(true);
            return true;
        } else if (what != 702) {
            return false;
        } else {
            this.mControlPanel.showLoadingProgress(false);
            return true;
        }
    }

    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        this.mCurrentBufferPercentage = percent;
        this.mControlPanel.onBufferingUpdate(percent);
    }

    public void onFullScreen(boolean isFull) {
        if (this.isFullScreen != isFull) {
            this.isFullScreen = isFull;
            View content = this.mContainer.getRootView().findViewById(R.id.content);
            if (isFull) {
                content.setBackground((Drawable) null);
                content.setPadding(0, 0, 0, 0);
                if (this.mWindowMode == 0) {
                    FloatVideoManager.getInstance(this.mContext).getWindow(this.mId).setFormat(2);
                }
                content.requestLayout();
                this.mSubgate = true;
            } else {
                this.mSubgate = false;
                setZoomMode(0);
                content.setBackgroundResource(R.drawable.float_bg);
                if (this.mWindowMode == 0) {
                    FloatVideoManager.getInstance(this.mContext).getWindow(this.mId).setFormat(1);
                }
            }
            this.mControlPanel.onFullScreen(isFull);
            FloatVideoManager.getInstance(this.mContext).onFullScreen(this.mId, isFull);
            if (this.mWindowMode == 0) {
                this.mControlPanel.showCommonControl(true);
            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (this.mWindowMode >= 1) {
            this.mVideoTexture.setVisibility(4);
            this.mSurfaceCreated = true;
            if (!this.mSuspendingFix) {
                openSource(this.mPathName);
            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (this.mWindowMode >= 1) {
            releaseSmartCamera();
            release();
            this.mSurfaceCreated = false;
        }
    }

    private void initPlayList(String name) {
        if (this.mPlayList.size() == 0) {
            this.mPlayList = createRelatedPlaylist(this.mPathName.substring(0, name.lastIndexOf(47)));
        }
        this.mCurrentIndex = this.mPlayList.indexOf(name);
        if (this.mCurrentIndex < 0) {
            this.mCurrentIndex = 0;
        }
    }

    private ArrayList<String> createRelatedPlaylist(String folder) {
        ArrayList<String> playList = new ArrayList<>();
        String[] projection = { LocalMediaProviderContract.ROW_ID, LocalMediaProviderContract.DIR_COLUMN,
                LocalMediaProviderContract.PATH_COLUMN };
        Cursor cursor = this.mContext.getContentResolver().query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI,
                projection, "(activevideo=1) AND (" + "dir like ?" + ")", new String[] { folder },
                LocalMediaProviderContract.NAME_COLUMN);
        if (cursor != null && cursor.moveToFirst()) {
            int itemSize = cursor.getCount();
            for (int i = 0; i < itemSize; i++) {
                LocalMediaInfo mediaInfo = new LocalMediaInfo();
                mediaInfo.mPath = cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.PATH_COLUMN));
                playList.add(mediaInfo.mPath);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return playList;
    }

    public VideoSurface getVideoSurfaceView() {
        if (this.mWindowMode < 1) {
            return null;
        }
        return this.mVideoSurface;
    }

    public void setActivityRotionInfo(int rotation) {
        this.mActivityRotation = rotation;
    }

    public void lockRotation(boolean lock) {
        if (this.mWindowMode == 1 && this.mActivityRotation != 0 && this.mActivityRotation != 1
                && this.mActivityRotation != 8 && this.mActivityRotation != 9) {
            Activity activity = (Activity) this.mContext;
            if (Settings.System.getInt(activity.getContentResolver(), "accelerometer_rotation", 1) == 0) {
                return;
            }
            if (!lock) {
                activity.setRequestedOrientation(this.mActivityRotation);
                return;
            }
            switch (activity.getResources().getConfiguration().orientation) {
                case 1:
                    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                    if (rotation == 1 || rotation == 2) {
                        activity.setRequestedOrientation(9);
                        return;
                    } else {
                        activity.setRequestedOrientation(1);
                        return;
                    }
                case 2:
                    int rotation2 = activity.getWindowManager().getDefaultDisplay().getRotation();
                    if (rotation2 == 0 || rotation2 == 1) {
                        activity.setRequestedOrientation(0);
                        return;
                    } else {
                        activity.setRequestedOrientation(8);
                        return;
                    }
                default:
                    return;
            }
        }
    }

    public void onTimedText(MediaPlayer arg0, TimedText tt) {
        this.mControlPanel.setSubTitile(tt);
    }

    public class AwTrackInfo {
        public String charset;
        int index;
        public String name;

        public AwTrackInfo() {
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        this.mZoomMode = this.mAppConfig.getInt(AppConfig.ZOOM, 0);
        setZoomMode(this.mZoomMode);
        this.mControlPanel.updateZoomBttnDrawable(this.mZoomMode);
        this.mControlPanel.removeAllSubtitleLayerView();
    }

    public void startSmartCamera() {
        Log.d(this.TAG, "startSmartCamera - disabled");
    }

    private void _disabled_startSmartCamera() {
    }

    public void releaseSmartCamera() {
        Log.d(this.TAG, "releaseSmartCamera - disabled for privacy");
        /*
         * if (this.mCameraDevice != null) {
         * Method mMethod = null;
         * try {
         * mMethod = Camera.class.getMethod("stopSmart", new Class[0]);
         * } catch (NoSuchMethodException e) {
         * Log.w(this.TAG, "no such method stopSmart");
         * }
         * if (mMethod != null) {
         * this.mCameraDevice.stopSmart();
         * }
         * this.mCameraDevice.release();
         * this.mCameraDevice = null;
         * }
         */
    }

    /*
     * private final class SmartListener implements Camera.SmartDetectionListener {
     * private SmartListener() {
     * }
     * 
     * synthetic SmartListener(VideoSurfaceTextureView videoSurfaceTextureView,
     * SmartListener smartListener) {
     * this();
     * }
     * 
     * public void onSmartDetection(int value, Camera camera) {
     * Log.d(VideoSurfaceTextureView.this.TAG, "onSmartDetection value:" + value);
     * if (value == 16 || value == 32 || value == 64 || value == 128) {
     * if (!VideoSurfaceTextureView.this.isPlaying()) {
     * Log.d(VideoSurfaceTextureView.this.TAG, "playVideo");
     * VideoSurfaceTextureView.this.play();
     * }
     * } else if (VideoSurfaceTextureView.this.isPlaying()) {
     * Log.d(VideoSurfaceTextureView.this.TAG, "pauseVideo");
     * VideoSurfaceTextureView.this.pause();
     * }
     * }
     * }
     */
}
