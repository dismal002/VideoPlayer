package com.dismal.fireplayer.ui;

import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.dismal.fireplayer.R;
import com.dismal.fireplayer.provider.LocalMediaProviderContract;
import com.dismal.fireplayer.ui.MediaPlayerEventInterface;
import com.dismal.fireplayer.videoplayerui.VideoSurfaceTextureView;

public class RemotePresentation extends Presentation {
    private static String TAG = "RemotePresentation";
    private int mLayerStack;
    private MediaPlayerEventInterface.MeidaPlayerEventListener mMeidaPlayerEventListener;
    private String mPath;
    private View mRootView;
    private VideoSurfaceTextureView mVideoView;

    public RemotePresentation(Context context, Display display, String path) {
        super(context, display);
        this.mPath = path;
        this.mLayerStack = com.dismal.fireplayer.util.ReflectionUtil.getLayerStack(display);
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_surfacetexture_view);
        this.mRootView = findViewById(R.id.video_container);
        this.mRootView.setSystemUiVisibility(1792);
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.buttonBrightness = 0.0f;
        winParams.flags |= 1024;
        win.setAttributes(winParams);
        win.setBackgroundDrawable((Drawable) null);
        Log.v(TAG, "onCreate()");
        Intent i = new Intent(getContext(), VideoPlayerActivity.class);
        i.putExtra(LocalMediaProviderContract.PATH_COLUMN, this.mPath);
        i.putExtra("boot-mode", "internal");
        this.mVideoView = new VideoSurfaceTextureView(getContext(), this.mRootView, -1, i, 2);
        if (this.mMeidaPlayerEventListener != null) {
            this.mVideoView.setMeidaPlayerEventListener(this.mMeidaPlayerEventListener);
        }
        this.mVideoView.resume();
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        super.onStop();
        this.mVideoView.release();
        this.mVideoView = null;
        Log.v(TAG, "onStop()");
    }

    public VideoSurfaceTextureView getVideoSurfaceTextureView() {
        return this.mVideoView;
    }

    public void setMeidaPlayerEventListener(MediaPlayerEventInterface.MeidaPlayerEventListener listener) {
        this.mMeidaPlayerEventListener = listener;
        if (this.mVideoView != null) {
            this.mVideoView.setMeidaPlayerEventListener(this.mMeidaPlayerEventListener);
        }
    }
}
