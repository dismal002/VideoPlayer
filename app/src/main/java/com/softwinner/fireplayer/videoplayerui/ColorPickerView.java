package com.softwinner.fireplayer.videoplayerui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {
    private RectF colorBarRect;
    private float colorHeight;
    private float colorWidth;
    private boolean inSelected = false;
    private final int[] mColors;
    private OnColorChangedListener mListener;
    private Paint mPaint;
    private Paint mSeletedPaint;
    private RectF selectedRect;
    private float viewWidth;

    public interface OnColorChangedListener {
        void colorChanged(int i);
    }

    public ColorPickerView(Context context, OnColorChangedListener l, int color, float heitht) {
        super(context);
        this.mListener = l;
        this.mColors = new int[]{-1, -12868894, -1079593, -944640, -5210041, -16777216};
        this.colorHeight = heitht;
        this.colorWidth = (this.colorHeight * 3.0f) / 2.0f;
        this.viewWidth = this.colorWidth * ((float) this.mColors.length);
        Shader s = new LinearGradient(this.colorWidth, this.colorHeight / 2.0f, this.viewWidth - this.colorWidth, this.colorHeight / 20.0f, this.mColors, (float[]) null, Shader.TileMode.CLAMP);
        this.mPaint = new Paint(1);
        this.mPaint.setShader(s);
        this.mPaint.setStrokeWidth(3.0f);
        this.mSeletedPaint = new Paint(1);
        this.mSeletedPaint.setColor(color);
        this.mSeletedPaint.setStrokeWidth(3.0f);
        this.selectedRect = new RectF(1.5f, 1.5f, this.colorHeight - 1.5f, this.colorHeight - 1.5f);
        this.colorBarRect = new RectF(this.colorWidth + 1.5f, 1.5f, this.viewWidth - 1.5f, this.colorHeight - 1.5f);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        canvas.drawRect(this.selectedRect, this.mSeletedPaint);
        canvas.drawRect(this.colorBarRect, this.mPaint);
        if (this.inSelected) {
            int c = this.mSeletedPaint.getColor();
            this.mSeletedPaint.setStyle(Paint.Style.STROKE);
            this.mSeletedPaint.setColor(-16257800);
            canvas.drawRect(new RectF(1.5f, 1.5f, this.colorHeight - 1.5f, this.colorHeight - 1.5f), this.mSeletedPaint);
            this.mSeletedPaint.setColor(c);
            this.mSeletedPaint.setStyle(Paint.Style.FILL);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (x > this.colorHeight || y > this.colorHeight) {
        }
        switch (event.getAction()) {
            case 2:
                if (this.colorBarRect.contains(x, y)) {
                    float unit = (x - this.colorWidth) / (this.viewWidth - this.colorWidth);
                    int c = interpColor(this.mColors, unit);
                    Log.d("yang", " ------move in color bar -----------unit " + unit + " color " + c);
                    this.mSeletedPaint.setColor(c);
                    this.mListener.colorChanged(this.mSeletedPaint.getColor());
                    invalidate();
                    break;
                }
                break;
        }
        return true;
    }

    private int ave(int s, int d, float p) {
        return Math.round(((float) (d - s)) * p) + s;
    }

    private int interpColor(int[] colors, float unit) {
        if (unit <= 0.0f) {
            return colors[0];
        }
        if (unit >= 1.0f) {
            return colors[colors.length - 1];
        }
        float p = unit * ((float) (colors.length - 1));
        int i = (int) p;
        float p2 = p - ((float) i);
        int c0 = colors[i];
        int c1 = colors[i + 1];
        return Color.argb(ave(Color.alpha(c0), Color.alpha(c1), p2), ave(Color.red(c0), Color.red(c1), p2), ave(Color.green(c0), Color.green(c1), p2), ave(Color.blue(c0), Color.blue(c1), p2));
    }
}
