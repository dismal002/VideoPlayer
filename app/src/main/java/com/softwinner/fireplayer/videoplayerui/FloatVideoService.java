package com.softwinner.fireplayer.videoplayerui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.floatwindow.FloatWindowFlags;
import com.softwinner.fireplayer.floatwindow.FloatWindowService;
import com.softwinner.fireplayer.floatwindow.Window;
import com.softwinner.fireplayer.provider.LocalMediaProviderContract;
import com.softwinner.fireplayer.ui.AppConfig;
import com.softwinner.fireplayer.ui.FourKMainActivity;
import com.softwinner.fireplayer.ui.PresentationScreenMonitor;
import com.softwinner.fireplayer.ui.VideoPlayerActivity;
import java.util.ArrayList;
import java.util.List;

public class FloatVideoService extends FloatWindowService implements PresentationScreenMonitor.PresentationScreenMonitorListener {
    public static final String ACTION_PLAY_NETURI = "softwinner.intent.action.PLAY_NETURI";
    public static final String ACTION_SWITCH_MODE = "softwinner.intent.action.SWITCH_MOD";
    private static final String TAG = "FloatVideoService";
    private FloatVideoManager mFloatVideoManager;
    private PresentationScreenMonitor mPresentationScreenMonitor;

    public String getAppName() {
        return TAG;
    }

    public int getAppIcon() {
        return 17301555;
    }

    public String getTitle(int id) {
        return this.mFloatVideoManager.getFlowVideo(id).getVideoName();
    }

    public void createAndAttachView(int id, Intent intent, FrameLayout frame) {
        this.mFloatVideoManager.addFlowVideo(new VideoSurfaceTextureView(this, ((LayoutInflater) getSystemService("layout_inflater")).inflate(R.layout.video_surfacetexture_view, frame, true), id, intent, 0), id);
    }

    public FloatWindowService.FloatLayoutParams getParams(int id, Window window) {
        return new FloatWindowService.FloatLayoutParams(this, id, window.defaulwidth, window.defaulheight, Integer.MIN_VALUE, Integer.MIN_VALUE, window.defaulwidth, window.defaulheight);
    }

    public int getFlags(int id) {
        return FloatWindowFlags.FLAG_BODY_MOVE_ENABLE | FloatWindowFlags.FLAG_WINDOW_HIDE_ENABLE | FloatWindowFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE | FloatWindowFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE | FloatWindowFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TOUCH | FloatWindowFlags.FLAG_WINDOW_FOCUS_INDICATOR_DISABLE;
    }

    public String getPersistentNotificationTitle(int id) {
        return String.valueOf(getAppName()) + " Running";
    }

    public String getPersistentNotificationMessage(int id) {
        return "Click to add a new " + getAppName();
    }

    public Intent getPersistentNotificationIntent(int id) {
        return FloatWindowService.getShowIntent(this, getClass(), getUniqueId(), (String) null);
    }

    public int getHiddenIcon() {
        return 17301569;
    }

    public String getHiddenNotificationTitle(int id) {
        return String.valueOf(getAppName()) + " Hidden";
    }

    public String getHiddenNotificationMessage(int id) {
        return "Click to restore #" + id;
    }

    public Intent getHiddenNotificationIntent(int id) {
        return FloatWindowService.getShowIntent(this, getClass(), id, this.mFloatVideoManager.getFlowVideo(id).getVideoName());
    }

    public Animation getShowAnimation(int id) {
        if (isExistingId(id)) {
            return AnimationUtils.loadAnimation(this, 17432578);
        }
        return super.getShowAnimation(id);
    }

    public Animation getHideAnimation(int id) {
        return AnimationUtils.loadAnimation(this, 17432579);
    }

    public List<FloatWindowService.DropDownListItem> getDropDownItems(int id) {
        List<FloatWindowService.DropDownListItem> items = new ArrayList<>();
        items.add(new FloatWindowService.DropDownListItem(17301568, "About", new Runnable() {
            public void run() {
                Toast.makeText(FloatVideoService.this, String.valueOf(FloatVideoService.this.getAppName()) + " is a demonstration of StandOut.", 0).show();
            }
        }));
        items.add(new FloatWindowService.DropDownListItem(17301577, "Settings", new Runnable() {
            public void run() {
                Toast.makeText(FloatVideoService.this, "There are no settings.", 0).show();
            }
        }));
        return items;
    }

    public void onCreate() {
        super.onCreate();
        this.mFloatVideoManager = FloatVideoManager.getInstance(getApplicationContext());
        this.mFloatVideoManager.setService(this);
        this.mFloatVideoManager.enableListeners();
        this.mPresentationScreenMonitor = PresentationScreenMonitor.getInstance(getApplicationContext());
    }

    public void onDestroy() {
        this.mFloatVideoManager.disableListeners();
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, " onstartcommand action " + action);
            if (action.equals(ACTION_PLAY_NETURI) || action.equals(ACTION_SWITCH_MODE)) {
                if (this.mPresentationScreenMonitor.getPresentationDisplay() != null) {
                    startMainActivity(intent.getStringExtra(LocalMediaProviderContract.PATH_COLUMN));
                } else {
                    int id = this.mFloatVideoManager.getAvailableId();
                    if (id != 0) {
                        this.mPresentationScreenMonitor.setListener(1, this);
                        show(id, intent);
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void onReceiveData(int id, int requestCode, Bundle data, Class<? extends FloatWindowService> cls, int fromId) {
        Log.d(TAG, "Unexpected data received.");
    }

    public void onMove(int id, Window window, View view, MotionEvent event) {
        super.onMove(id, window, view, event);
    }

    public void onResize(int id, Window window, View view, MotionEvent event) {
        super.onResize(id, window, view, event);
    }

    public boolean onShow(int id, Window window) {
        this.mFloatVideoManager.onShow(id, window);
        return super.onShow(id, window);
    }

    public boolean onHide(int id, Window window) {
        this.mFloatVideoManager.onHide(id, window);
        Window cur_window = this.mFloatVideoManager.getTopWindow();
        if (cur_window != null) {
            setFrontWindow(cur_window);
        }
        return super.onHide(id, window);
    }

    public boolean onClose(int id, Window window) {
        this.mFloatVideoManager.onClose(id, window);
        Window cur_window = this.mFloatVideoManager.getTopWindow();
        if (cur_window != null) {
            setFrontWindow(cur_window);
        }
        return super.onClose(id, window);
    }

    public boolean onCloseAll() {
        return super.onCloseAll();
    }

    public boolean onBringToFront(int id, Window window) {
        this.mFloatVideoManager.onBringToFront(id, window);
        return super.onBringToFront(id, window);
    }

    public boolean onFocusChange(int id, Window window, boolean focus) {
        return super.onFocusChange(id, window, focus);
    }

    public boolean onKeyEvent(int id, Window window, KeyEvent event) {
        if (event.getAction() == 1) {
            switch (event.getKeyCode()) {
                case 4:
                    VideoSurfaceTextureView videoView = this.mFloatVideoManager.getFlowVideo(id);
                    if (videoView != null && videoView.isFullScreen()) {
                        originalScreen(id);
                    }
                    return false;
            }
        }
        return super.onKeyEvent(id, window, event);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void onLowMemory() {
        super.onLowMemory();
    }

    public boolean onFullScreen(int id, Window window) {
        return super.onFullScreen(id, window);
    }

    public boolean onUpdate(int id, Window window, FloatWindowService.FloatLayoutParams params) {
        boolean isFull = isFullScreen(params);
        VideoSurfaceTextureView videoView = this.mFloatVideoManager.getFlowVideo(id);
        if (!isFull || AppConfig.getInstance(this).getInt(AppConfig.APP_MODE, 1) < 1) {
            window.isFullScreen = isFull;
            if (videoView != null) {
                videoView.onFullScreen(isFull);
            }
            this.mFloatVideoManager.onUpdate(id, window, params);
            return super.onUpdate(id, window, params);
        } else if (videoView == null) {
            return true;
        } else {
            Log.v("fuqiang", "onUpdate");
            String path = videoView.getPathName();
            int position = videoView.getCurrentPosition();
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra(LocalMediaProviderContract.PATH_COLUMN, path);
            intent.putExtra("position", position).putExtra("boot-mode", "internal");
            intent.setFlags(268435456);
            startActivity(intent);
            FloatVideoManager.getInstance(this).closeAllWindows();
            return true;
        }
    }

    public boolean onTouchBody(int id, Window window, View view, MotionEvent event) {
        VideoSurfaceTextureView videoView = this.mFloatVideoManager.getFlowVideo(id);
        if (videoView != null) {
            videoView.getContainer().onTouchEvent(event);
        }
        return super.onTouchBody(id, window, view, event);
    }

    public boolean isFullScreen(FloatWindowService.FloatLayoutParams params) {
        boolean z;
        boolean z2;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        boolean z3 = params.width >= width;
        if (params.width == -1) {
            z = true;
        } else {
            z = false;
        }
        if (z3 || z) {
            boolean z4 = params.height >= height;
            if (params.height == -1) {
                z2 = true;
            } else {
                z2 = false;
            }
            if (z4 || z2) {
                return true;
            }
        }
        return false;
    }

    public void onPresentationDisplayChanged(Display presentationDisplay) {
        int id;
        if (presentationDisplay != null && (id = this.mFloatVideoManager.getTopWindowId()) >= 0) {
            String mPathName = this.mFloatVideoManager.getVideoFullPath(id);
            this.mFloatVideoManager.closeAllWindows();
            startMainActivity(mPathName);
        }
    }

    private void startMainActivity(String path) {
        Intent intent = new Intent(this, FourKMainActivity.class);
        intent.setAction(FourKMainActivity.FKM_ACTION_SWITCH_PRESENTATION);
        intent.setFlags(268435456);
        intent.putExtra("switchToPresentation", true);
        intent.putExtra(LocalMediaProviderContract.PATH_COLUMN, path);
        Log.v(TAG, "startMainActivity ...");
        startActivity(intent);
    }
}
