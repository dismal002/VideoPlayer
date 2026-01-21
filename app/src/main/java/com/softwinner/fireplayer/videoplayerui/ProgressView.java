package com.softwinner.fireplayer.videoplayerui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.softwinner.fireplayer.R;

public class ProgressView extends View {
    private Paint mBackgroundPaint;
    private int mMax;
    private int mProgress;
    private Paint mProgressPaint;

    public ProgressView(Context context) {
        super(context);
        init();
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.mBackgroundPaint = new Paint();
        this.mBackgroundPaint.setColor(getContext().getResources().getColor(R.color.sound_background));
        this.mProgressPaint = new Paint();
        this.mProgressPaint.setColor(getContext().getResources().getColor(R.color.sound_level));
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int height = (h / this.mMax) * (this.mMax - this.mProgress);
        canvas.drawRect(0.0f, 0.0f, (float) w, (float) h, this.mBackgroundPaint);
        canvas.drawRect(1.0f, (float) (height + 1), (float) (w - 1), (float) (h - 1), this.mProgressPaint);
    }

    public void changeVolume(int VolumeLevel, int mMaxLevel) {
        this.mProgress = VolumeLevel;
        this.mMax = mMaxLevel;
        postInvalidate();
    }

    public void changeBrightness(int BrightnessLevel, int mMaxLevel) {
        this.mProgress = BrightnessLevel;
        this.mMax = mMaxLevel;
        postInvalidate();
    }
}
