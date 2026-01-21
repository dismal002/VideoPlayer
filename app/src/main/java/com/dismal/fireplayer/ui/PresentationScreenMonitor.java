package com.dismal.fireplayer.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.MediaRouter;
import android.util.Log;
import android.view.Display;
import com.dismal.fireplayer.util.Utils;

@TargetApi(17)
public class PresentationScreenMonitor {
    private static final int DISABLE_DISPLAY_TYPE = 0;
    public static final int PSM_CLIENT_FLOAT_WINDOW_SERVICE = 2;
    public static final int PSM_CLIENT_MAIN_ACTITIY = 0;
    public static final int PSM_CLIENT_UNKOWN = -1;
    public static final int PSM_CLIENT_VPLAYER_ACTITIY = 1;
    public static final String TAG = "PresentationScreenMonitor";
    private static PresentationScreenMonitor mInstance;
    private int mClientType = -1;
    private final Context mContext;
    private DisplayManager mDisplayManager;
    /* access modifiers changed from: private */
    public PresentationScreenMonitorListener mListener = null;
    /* access modifiers changed from: private */
    public MediaRouter mMediaRouter;
    private final MediaRouter.SimpleCallback mMediaRouterCallback = new MediaRouter.SimpleCallback() {
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
        }

        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
        }

        public void onRoutePresentationDisplayChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(PresentationScreenMonitor.TAG, "onRoutePresentationDisplayChanged: info=" + info);
            MediaRouter.RouteInfo route = PresentationScreenMonitor.this.mMediaRouter.getSelectedRoute(2);
            Display presentationDisplay = route != null ? route.getPresentationDisplay() : null;
            if ((presentationDisplay == null
                    || com.dismal.fireplayer.util.ReflectionUtil.getDisplayType(presentationDisplay) != 0)
                    && PresentationScreenMonitor.this.mListener != null
                    && PresentationScreenMonitor.this.mPresentationEnable) {
                PresentationScreenMonitor.this.mListener.onPresentationDisplayChanged(presentationDisplay);
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mPresentationEnable;
    private RemotePresentation mRemotePresentation = null;

    public interface PresentationScreenMonitorListener {
        void onPresentationDisplayChanged(Display display);
    }

    public static PresentationScreenMonitor getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new PresentationScreenMonitor(context);
        }
        return mInstance;
    }

    private PresentationScreenMonitor(Context context) {
        this.mContext = context;
        this.mPresentationEnable = AppConfig.getInstance(this.mContext.getApplicationContext())
                .getBoolean(AppConfig.PRESENTATION_ENABLE, true);
        this.mMediaRouter = (MediaRouter) this.mContext.getSystemService("media_router");
        if (Utils.isSdkJB42OrAbove()) {
            this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        }
        this.mMediaRouter.addCallback(2, this.mMediaRouterCallback);
    }

    public void setListener(int clientType, PresentationScreenMonitorListener listener) {
        this.mClientType = clientType;
        this.mListener = listener;
    }

    public void setPresentationEnable(boolean enable) {
        this.mPresentationEnable = enable;
    }

    public int getClientType(int clientType) {
        return this.mClientType;
    }

    public Display getPresentationDisplay() {
        Display display = null;
        if (!this.mPresentationEnable || !Utils.isSdkJB42OrAbove()) {
            return null;
        }
        MediaRouter.RouteInfo route = this.mMediaRouter.getSelectedRoute(2);
        if (route != null) {
            display = route.getPresentationDisplay();
        }
        if (display == null || com.dismal.fireplayer.util.ReflectionUtil.getDisplayType(display) != 0) {
            return display;
        }
        return null;
    }

    public Display getExsitPresentationDisplay() {
        MediaRouter.RouteInfo route = this.mMediaRouter.getSelectedRoute(2);
        Display display = route != null ? route.getPresentationDisplay() : null;
        if (display == null || com.dismal.fireplayer.util.ReflectionUtil.getDisplayType(display) != 0) {
            return display;
        }
        return null;
    }

    public DisplayManager getDisplayManager() {
        return this.mDisplayManager;
    }

    public void setPresentation(RemotePresentation remotePresentation) {
        this.mRemotePresentation = remotePresentation;
    }

    public void setPresentationToNull(RemotePresentation remotePresentation) {
        if (this.mRemotePresentation == remotePresentation) {
            this.mRemotePresentation = null;
        }
    }

    public RemotePresentation getPresentation() {
        return this.mRemotePresentation;
    }

    public boolean isWIFIDisplayOn() {
        if (!Utils.isSdkJB42OrAbove()) {
            return false;
        }
        MediaRouter.RouteInfo route = this.mMediaRouter.getSelectedRoute(2);
        Display display = route != null ? route.getPresentationDisplay() : null;
        if (display == null || com.dismal.fireplayer.util.ReflectionUtil.getDisplayType(display) != 0) {
            return false;
        }
        return true;
    }
}
