package com.softwinner.fireplayer.videoplayerui;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.floatwindow.FloatWindowService;
import com.softwinner.fireplayer.floatwindow.Window;
import com.softwinner.fireplayer.ui.AppConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class FloatVideoManager implements AudioManager.OnAudioFocusChangeListener {
    public static int DEFAULT_HEIGHT = 0;
    public static int DEFAULT_WIDTH = 0;
    private static int MAX_VIDEO_VIEW = 4;
    public static int MAX_VIDEO_VIEW_TO_DISABLE_ANIM = 1;
    private static final int MSG_ANIMATE_TIMEOUT = 0;
    private static final int MSG_RESUME_VIDEOS = 1;
    private static final String TAG = "FloatVideoManager";
    private static int coverMargin;
    private static Thread delayedResumeVideosThread = null;
    /* access modifiers changed from: private */
    public static int displayHeight;
    /* access modifiers changed from: private */
    public static int displayWidth;
    private static ArrayList<Integer> idList = new ArrayList<>();
    private static int mAppMode;
    /* access modifiers changed from: private */
    public static Context mContext;
    private static FloatVideoManager mInstance = null;
    private static int mS1080p;
    private static int mS2160p;
    private static int mS720p;
    private static int mSother;
    private static int stackMargin;
    private int density;
    /* access modifiers changed from: private */
    public boolean mAudioFocusLoss = false;
    private AudioManager mAudioManager;
    private int mFullscreenId = -1;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    FloatVideoManager.this.mInAimation = false;
                    return;
                case 1:
                    FloatVideoManager.this.startAllVideos();
                    return;
                default:
                    return;
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mInAimation = false;
    private OnFullscreenListener mListener;
    private MyOrientationEventListener mOrientationLisner;
    private SuspendReceiver mSuspendReceiver = null;
    private FloatVideoService service;
    private HashMap<Integer, VideoSurfaceTextureView> videoViewList = new HashMap<>();
    private ArrayDeque<Window> windowList = new ArrayDeque<>();

    public interface OnFullscreenListener {
        void onFullScreen(boolean z);

        void onVideoClose();

        void onVideoOpen();
    }

    public static FloatVideoManager getInstance(Context context) {
        if (mInstance == null) {
            mContext = context;
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            stackMargin = context.getResources().getDimensionPixelSize(R.dimen.stack_margin_size);
            coverMargin = context.getResources().getDimensionPixelSize(R.dimen.cover_size);
            mInstance = new FloatVideoManager();
            mAppMode = AppConfig.getInstance(context.getApplicationContext()).getAppMode();
            mS2160p = AppConfig.getInstance(context.getApplicationContext()).getInt(AppConfig.CUSTOM_S2160P, 1);
            mS1080p = AppConfig.getInstance(context.getApplicationContext()).getInt(AppConfig.CUSTOM_S1080P, 1);
            mS720p = AppConfig.getInstance(context.getApplicationContext()).getInt(AppConfig.CUSTOM_S720P, 1);
            mSother = AppConfig.getInstance(context.getApplicationContext()).getInt(AppConfig.CUSTOM_SOTHER, 1);
            for (int i = 1; i <= MAX_VIDEO_VIEW; i++) {
                idList.add(Integer.valueOf(i));
            }
        }
        return mInstance;
    }

    public void setService(FloatVideoService service2) {
        this.service = service2;
    }

    public void setListener(OnFullscreenListener listenr) {
        this.mListener = listenr;
    }

    public boolean openFloatView(String path) {
        int curId = getAvailableId();
        if (curId == 0) {
            return false;
        }
        FloatWindowService.show(mContext, FloatVideoService.class, curId, path);
        return true;
    }

    public void addFlowVideo(VideoSurfaceTextureView videoView, int id) {
        this.videoViewList.put(Integer.valueOf(id), videoView);
    }

    public VideoSurfaceTextureView getFlowVideo(int id) {
        return this.videoViewList.get(Integer.valueOf(id));
    }

    private boolean checkMaxSizeSupport() {
        int s2160p = 0;
        int s1080p = 0;
        int s720p = 0;
        int sother = 0;
        for (VideoSurfaceTextureView vv : this.videoViewList.values()) {
            int w = vv.getVideoSizeWidth();
            int h = vv.getVideoSizeHeight();
            if (w * h >= 5529600) {
                s2160p++;
                s1080p += 2;
                s720p += 4;
                sother += 4;
            } else if (w * h > 1024000) {
                s1080p++;
                s720p += 2;
                sother += 2;
            } else if (w * h >= 737280) {
                s720p++;
                sother++;
            } else {
                sother++;
            }
        }
        if (s2160p >= mS2160p || s1080p >= mS1080p || s720p >= mS720p || sother >= mSother) {
            return false;
        }
        return true;
    }

    public int getAvailableId() {
        if (idList.size() != MAX_VIDEO_VIEW && !checkMaxSizeSupport()) {
            Log.d(TAG, "max video reach!");
            return 0;
        } else if (idList.size() != 0) {
            return idList.remove(0).intValue();
        } else {
            return 0;
        }
    }

    public Window getTopWindow() {
        if (this.windowList.size() == 0) {
            return null;
        }
        return this.windowList.peek();
    }

    public int getTopWindowId() {
        if (this.windowList.size() == 0) {
            return -1;
        }
        return this.windowList.peek().id;
    }

    public int getFullScreenWindowId() {
        if (this.mFullscreenId < 0 || getFlowVideo(this.mFullscreenId) == null) {
            return -1;
        }
        return this.mFullscreenId;
    }

    public String getVideoFullPath(int id) {
        return getFlowVideo(id).getPathName();
    }

    public Window getWindow(int id) {
        Window window = null;
        Iterator<Window> it = this.windowList.iterator();
        while (it.hasNext()) {
            window = it.next();
            if (window.id == id) {
                return window;
            }
        }
        return window;
    }

    public void onBringToFront(int id, Window window) {
        if (getTopWindow() != null) {
            getFlowVideo(getTopWindow().id).setMute(true);
        }
        getFlowVideo(id).setMute(false);
        this.windowList.remove(window);
        this.windowList.push(window);
    }

    public int getWindowCount() {
        return this.windowList.size();
    }

    public void onShow(int id, Window window) {
        Window cur_window = getTopWindow();
        if (cur_window != null) {
            getFlowVideo(cur_window.id).setMute(true);
        }
        Window cur_window2 = getCoverWindow(0, window.originalParams);
        if (cur_window2 != null) {
            cur_window2.startAnimate(Math.min(window.originalParams.x + stackMargin, displayWidth), Math.max(window.originalParams.y - stackMargin, 0), 300);
        }
        this.windowList.push(window);
        if (this.windowList.size() == MAX_VIDEO_VIEW_TO_DISABLE_ANIM && this.mListener != null) {
            this.mListener.onVideoOpen();
        }
    }

    public void onHide(int id, Window window) {
        this.windowList.remove(window);
        getFlowVideo(id).onHide();
        Window cur_window = getTopWindow();
        if (cur_window != null) {
            getFlowVideo(cur_window.id).setMute(false);
        }
    }

    public void onClose(int id, Window window) {
        this.windowList.remove(window);
        idList.add(Integer.valueOf(id));
        getFlowVideo(id).onClose(id);
        this.videoViewList.remove(Integer.valueOf(id));
        Window cur_window = getTopWindow();
        if (cur_window != null) {
            getFlowVideo(cur_window.id).setMute(false);
        }
        if (this.windowList.size() == MAX_VIDEO_VIEW_TO_DISABLE_ANIM - 1 && this.mListener != null) {
            this.mListener.onVideoClose();
        }
    }

    public void onFullScreen(int id, boolean isFull) {
        if (this.mListener != null) {
            this.mListener.onFullScreen(isFull);
        }
        Set<Integer> ids = this.videoViewList.keySet();
        if (isFull) {
            this.mFullscreenId = id;
            for (Integer intValue : ids) {
                int curId = intValue.intValue();
                if (curId != id) {
                    getFlowVideo(curId).pause();
                }
            }
            return;
        }
        this.mFullscreenId = -1;
        for (Integer intValue2 : ids) {
            int curId2 = intValue2.intValue();
            if (curId2 != id) {
                getFlowVideo(curId2).play();
            }
        }
    }

    public void onUpdate(int id, Window window, FloatWindowService.FloatLayoutParams params) {
        Window coverWindow;
        if (!window.isAnimating && !this.mInAimation && !window.isFullScreen && (coverWindow = getCoverWindow(id, params)) != null) {
            coverWindow.startAnimate(Math.min(params.x + stackMargin, displayWidth), Math.max(params.y - stackMargin, 0), 300);
        }
    }

    public void pauseAllVideos() {
        for (Integer intValue : this.videoViewList.keySet()) {
            getFlowVideo(intValue.intValue()).suspend();
        }
    }

    public void startAllVideos() {
        for (Integer intValue : this.videoViewList.keySet()) {
            getFlowVideo(intValue.intValue()).resume();
        }
    }

    public void closeAllWindows() {
        Iterator<Window> it = this.windowList.iterator();
        while (it.hasNext()) {
            this.service.close(it.next().id);
        }
    }

    public void closeWindow(int id) {
        this.service.close(id);
    }

    private Window getCoverWindow(int id, FloatWindowService.FloatLayoutParams params) {
        Window[] windows = new Window[this.windowList.size()];
        this.windowList.toArray(windows);
        for (int i = 0; i < windows.length; i++) {
            int deltaX = Math.abs(windows[i].getLayoutParams().x - params.x);
            int deltaY = Math.abs(windows[i].getLayoutParams().y - params.y);
            if (deltaX < coverMargin && deltaY < coverMargin && id != windows[i].id) {
                return windows[i];
            }
        }
        return null;
    }

    public void tile() {
        int size = this.windowList.size();
        if (size >= 1) {
            Window[] windows = new Window[size];
            this.windowList.toArray(windows);
            for (int i = 0; i < size; i++) {
                windows[i].originalParams.width = DEFAULT_WIDTH;
                windows[i].originalParams.height = DEFAULT_HEIGHT;
                int x = Math.max(0, ((displayWidth / 2) - DEFAULT_WIDTH) / 2);
                int y = Math.max(this.density * 50, ((displayHeight / 2) - DEFAULT_HEIGHT) / 2);
                switch (i) {
                    case 1:
                        x += displayWidth / 2;
                        break;
                    case 2:
                        y += displayHeight / 2;
                        break;
                    case 3:
                        x += displayWidth / 2;
                        y += displayHeight / 2;
                        break;
                }
                windows[i].stopAnimate();
                windows[i].startAnimate(x, y, 1000);
            }
            this.mInAimation = true;
            this.mHandler.removeMessages(0);
            this.mHandler.sendEmptyMessageDelayed(0, 1000);
        }
    }

    public void stack() {
        int size = this.windowList.size();
        if (size >= 1) {
            Window[] windows = new Window[size];
            this.windowList.toArray(windows);
            int x = (displayWidth - DEFAULT_WIDTH) / 2;
            int y = (displayHeight - (DEFAULT_HEIGHT / 2)) / 2;
            for (int i = 0; i < size; i++) {
                int endX = Math.min((stackMargin * i) + x, displayWidth);
                int endY = Math.max(y - (stackMargin * i), this.density * 60);
                windows[i].stopAnimate();
                windows[i].originalParams.width = DEFAULT_WIDTH;
                windows[i].originalParams.height = DEFAULT_HEIGHT;
                windows[i].startAnimate(endX, endY, 1000);
            }
            this.mInAimation = true;
            this.mHandler.removeMessages(0);
            this.mHandler.sendEmptyMessageDelayed(0, 1000);
        }
    }

    public void cancelDimBehind() {
        if (this.windowList.size() >= 1) {
            Iterator<Window> it = this.windowList.iterator();
            while (it.hasNext()) {
                it.next().cancelDimBehind();
            }
        }
    }

    public void dimBehind() {
        if (this.windowList.size() >= 1) {
            Iterator<Window> it = this.windowList.iterator();
            while (it.hasNext()) {
                it.next().dimBehind();
            }
        }
    }

    public void enableListeners() {
        if (this.mOrientationLisner == null) {
            this.mOrientationLisner = new MyOrientationEventListener(mContext);
            this.mOrientationLisner.enable();
        }
        if (this.mSuspendReceiver == null) {
            this.mSuspendReceiver = new SuspendReceiver();
            IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            mContext.getApplicationContext().registerReceiver(this.mSuspendReceiver, filter);
        }
        this.mAudioManager = (AudioManager) mContext.getSystemService("audio");
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        displayHeight = metrics.heightPixels;
        displayWidth = metrics.widthPixels;
        this.density = (int) metrics.density;
    }

    public void disableListeners() {
        if (this.mOrientationLisner != null) {
            this.mOrientationLisner.disable();
            this.mOrientationLisner = null;
        }
        if (this.mSuspendReceiver != null) {
            mContext.getApplicationContext().unregisterReceiver(this.mSuspendReceiver);
            this.mSuspendReceiver = null;
        }
        ((AudioManager) mContext.getSystemService("audio")).abandonAudioFocus((AudioManager.OnAudioFocusChangeListener) null);
    }

    public void updateFullScreenWindow() {
        Iterator<Window> it = this.windowList.iterator();
        while (it.hasNext()) {
            Window curWindow = it.next();
            curWindow.invalidate();
            curWindow.requestLayout();
            if (curWindow.isFullScreen) {
                curWindow.edit().fullScreen().commit();
                VideoSurfaceTextureView vv = getFlowVideo(curWindow.id);
                if (vv != null) {
                    vv.setZoomMode(0);
                }
            }
        }
    }

    class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        public void onOrientationChanged(int orientation) {
            DisplayMetrics metrics = FloatVideoManager.mContext.getResources().getDisplayMetrics();
            if (FloatVideoManager.displayWidth != metrics.widthPixels) {
                FloatVideoManager.displayHeight = metrics.heightPixels;
                FloatVideoManager.displayWidth = metrics.widthPixels;
                FloatVideoManager.this.updateFullScreenWindow();
            }
        }
    }

    class SuspendReceiver extends BroadcastReceiver {
        SuspendReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Log.d(FloatVideoManager.TAG, " suspend receiver action " + intent.getAction());
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                FloatVideoManager.this.pauseAllVideos();
            } else if (action.equals("android.intent.action.SCREEN_ON")) {
                FloatVideoManager.this.delayedResumeVideos();
            }
        }
    }

    /* access modifiers changed from: private */
    public void delayedResumeVideos() {
        if (delayedResumeVideosThread == null || !delayedResumeVideosThread.isAlive()) {
            delayedResumeVideosThread = new Thread(new Runnable() {
                public void run() {
                    KeyguardManager km = (KeyguardManager) FloatVideoManager.mContext.getSystemService("keyguard");
                    while (km.isKeyguardLocked()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!FloatVideoManager.this.mAudioFocusLoss) {
                        FloatVideoManager.this.mHandler.sendEmptyMessageDelayed(1, 200);
                    }
                }
            });
            delayedResumeVideosThread.start();
            return;
        }
        Log.v(TAG, "Thread is running");
    }

    public void requestAudioFocus() {
        this.mAudioFocusLoss = false;
        this.mAudioManager.requestAudioFocus(this, 3, 1);
    }

    public void onAudioFocusChange(int focusChange) {
        Log.i(TAG, "onAudioFocusChange " + focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pauseAllVideos();
                if (this.mFullscreenId >= 0) {
                    FloatWindowService.originalScreen(mContext, FloatVideoService.class, this.mFullscreenId);
                    return;
                }
                return;
            case AudioManager.AUDIOFOCUS_LOSS:
                pauseAllVideos();
                this.mAudioFocusLoss = true;
                return;
            case AudioManager.AUDIOFOCUS_GAIN:
                startAllVideos();
                return;
            default:
                return;
        }
    }
}
