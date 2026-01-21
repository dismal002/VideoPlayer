package com.softwinner.fireplayer.ui;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import com.softwinner.fireplayer.util.Utils;
import java.io.File;

public class VideoThumbsTextureView {
    private static final String TAG = "VideoThumbsTextureView";
    private Context mContext;
    /* access modifiers changed from: private */
    public TextrueViewListener mListener;
    /* access modifiers changed from: private */
    public String mPath;
    /* access modifiers changed from: private */
    public int mPostion;
    private MySurfaceTextureListener mSurfaceTextureListener;
    /* access modifiers changed from: private */
    public TextureView mTextureView;
    /* access modifiers changed from: private */
    public String vPath;

    public interface TextrueViewListener {
        void onTextureViewAvailable(int i);

        void onTextureViewDestory(int i);
    }

    public void setListener(TextrueViewListener listener) {
        this.mListener = listener;
    }

    public VideoThumbsTextureView(Context context, String path, int pos) {
        this.mContext = context;
        this.mPath = path;
        this.mPostion = pos;
    }

    public synchronized void attachTextureView(FrameLayout fl) {
        if (this.mPath == null || !new File(this.mPath).exists()) {
            Log.e(TAG, "MicroStream is not created!!");
        } else {
            this.mTextureView = new TextureView(this.mContext);
            this.mSurfaceTextureListener = new MySurfaceTextureListener();
            this.mTextureView.setSurfaceTextureListener(this.mSurfaceTextureListener);
            if (fl != null) {
                fl.addView(this.mTextureView);
            }
        }
    }

    class MySurfaceTextureListener implements TextureView.SurfaceTextureListener {
        MediaPlayer mMediaPlayer;
        /* access modifiers changed from: private */
        public boolean mPreparing = false;
        boolean mToPlay = false;

        MySurfaceTextureListener() {
        }

        public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
            VideoThumbsTextureView.this.mListener.onTextureViewAvailable(VideoThumbsTextureView.this.mPostion);
            startPlay();
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
            stopPlay();
            VideoThumbsTextureView.this.mListener.onTextureViewDestory(VideoThumbsTextureView.this.mPostion);
            return true;
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
        }

        public void onSurfaceTextureUpdated(SurfaceTexture st) {
        }

        private void startPlay() {
            this.mToPlay = true;
            if (!this.mPreparing) {
                prepareAsync();
            }
        }

        private void stopPlay() {
            this.mToPlay = false;
            if (!this.mPreparing) {
                release();
            }
        }

        private void prepareAsync() {
            try {
                this.mMediaPlayer = new MediaPlayer();
                this.mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        MySurfaceTextureListener.this.mPreparing = false;
                        if (MySurfaceTextureListener.this.mToPlay) {
                            mp.start();
                        } else {
                            MySurfaceTextureListener.this.release();
                        }
                    }
                });
                this.mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        mp.start();
                    }
                });
                this.mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Log.e(VideoThumbsTextureView.TAG, "onError()..mUrl = " + VideoThumbsTextureView.this.vPath + ", what = " + what + ",extra = " + extra);
                        MySurfaceTextureListener.this.release();
                        return true;
                    }
                });
                Surface surface = new Surface(VideoThumbsTextureView.this.mTextureView.getSurfaceTexture());
                this.mMediaPlayer.setSurface(surface);
                surface.release();
                this.mMediaPlayer.setDataSource(Utils.convertMediaPath(VideoThumbsTextureView.this.mPath));
                this.mMediaPlayer.prepareAsync();
                this.mPreparing = true;
            } catch (Exception ex) {
                Log.e(VideoThumbsTextureView.TAG, "Unable to open source: " + VideoThumbsTextureView.this.mPath, ex);
                release();
            }
        }

        /* access modifiers changed from: private */
        public void release() {
            if (this.mMediaPlayer != null) {
                this.mMediaPlayer.reset();
                this.mMediaPlayer.release();
                this.mMediaPlayer = null;
            }
        }
    }
}
