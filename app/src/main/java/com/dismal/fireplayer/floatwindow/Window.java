package com.dismal.fireplayer.floatwindow;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.dismal.fireplayer.R;
import com.dismal.fireplayer.floatwindow.FloatWindowService;
import com.dismal.fireplayer.videoplayerui.FloatVideoManager;
import com.dismal.fireplayer.videoplayerui.VideoSurfaceTextureView;
import java.util.LinkedList;
import java.util.Queue;

public class Window extends FrameLayout implements ValueAnimator.AnimatorUpdateListener {
    public static int DEFAULT_FLOATWINDOW_WIDTH = 0;
    static final String TAG = "Window";
    public static final int VISIBILITY_GONE = 0;
    public static final int VISIBILITY_TRANSITION = 2;
    public static final int VISIBILITY_VISIBLE = 1;
    private ValueAnimator anim;
    public Class<? extends FloatWindowService> cls;
    public Bundle data;
    public int defaulheight;
    public int defaulwidth;
    private int distanceX;
    private int distanceY;
    public int flags;
    public boolean focused;
    public int id;
    public boolean isAnimating;
    public boolean isFullScreen;
    /* access modifiers changed from: private */
    public final FloatWindowService mContext;
    private LayoutInflater mLayoutInflater;
    private DisplayMetrics mMetrics;
    /* access modifiers changed from: private */
    public int mOldHeight;
    /* access modifiers changed from: private */
    public int mOldWidth;
    private MyOrientationEventListener mOrientationLisner;
    public FloatWindowService.FloatLayoutParams originalParams;
    private int[] preLayout;
    private int startX;
    private int startY;
    public TouchInfo touchInfo;
    public int visibility;

    public Window(Context context) {
        super(context);
        this.isFullScreen = false;
        this.preLayout = new int[4];
        this.isAnimating = false;
        this.mOldWidth = -1;
        this.mOldHeight = -1;
        this.mContext = null;
    }

    public Window(final FloatWindowService context, final int id2, Intent intent) {
        super(context);
        FrameLayout body;
        View content;
        this.isFullScreen = false;
        this.preLayout = new int[4];
        this.isAnimating = false;
        this.mOldWidth = -1;
        this.mOldHeight = -1;
        context.setTheme(context.getThemeStyle());
        com.dismal.fireplayer.util.ReflectionUtil.makeOptionalFitsSystemWindows(this);
        this.mContext = context;
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mMetrics = this.mContext.getResources().getDisplayMetrics();
        DEFAULT_FLOATWINDOW_WIDTH = (Math.max(this.mMetrics.heightPixels, this.mMetrics.widthPixels) * 2) / 5;
        int videowidth = intent.getIntExtra("videowidth", 0);
        int videoheight = intent.getIntExtra("videoheight", 0);
        this.defaulwidth = DEFAULT_FLOATWINDOW_WIDTH;
        if (videowidth == 0) {
            videoheight = 3;
            videowidth = 4;
        }
        this.defaulheight = (DEFAULT_FLOATWINDOW_WIDTH * videoheight) / videowidth;
        this.cls = context.getClass();
        this.id = id2;
        this.originalParams = context.getParams(id2, this);
        this.flags = context.getFlags(id2);
        this.touchInfo = new TouchInfo();
        this.touchInfo.ratio = ((float) this.originalParams.width) / ((float) this.originalParams.height);
        this.data = new Bundle();
        if (Utils.isSet(this.flags, FloatWindowFlags.FLAG_DECORATION_SYSTEM)) {
            View content2 = getSystemDecorations();
            body = (FrameLayout) content2.findViewById(R.id.body);
            content = content2;
        } else {
            View content3 = new FrameLayout(context);
            content3.setLayoutParams(new FrameLayout.LayoutParams(-2, -2));
            content3.setId(R.id.content);
            body = (FrameLayout) content3;
            content = content3;
        }
        addView(content);
        body.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                boolean consumed;
                if (!Window.this.isFullScreen && !(consumed = context.onTouchHandleMove(id2, Window.this, v, event))) {
                    if (context.onTouchBody(id2, Window.this, v, event) || consumed) {
                    }
                }
                return true;
            }
        });
        context.createAndAttachView(id2, intent, body);
        TextView title = (TextView) content.findViewById(R.id.title);
        if (title != null) {
            title.setText(this.mContext.getTitle(id2));
        }
        if (body.getChildCount() == 0) {
            throw new RuntimeException("You must attach your view to the given frame in createAndAttachView()");
        }
        if (!Utils.isSet(this.flags, FloatWindowFlags.FLAG_FIX_COMPATIBILITY_ALL_DISABLE)) {
            fixCompatibility(body);
        }
        if (!Utils.isSet(this.flags, FloatWindowFlags.FLAG_ADD_FUNCTIONALITY_ALL_DISABLE)) {
            addFunctionality(body);
        }
        setTag(body.getTag());
        if (this.mOrientationLisner != null) {
            this.mOrientationLisner.disable();
            this.mOrientationLisner = null;
        }
        if (this.mOrientationLisner == null) {
            this.mOrientationLisner = new MyOrientationEventListener(this.mContext);
            this.mOrientationLisner.enable();
        }
    }

    public int[] getPreLayout() {
        return this.preLayout;
    }

    public void setPreLayout(FloatWindowService.FloatLayoutParams param) {
        if (!this.isFullScreen) {
            this.preLayout[0] = param.x;
            this.preLayout[1] = param.y;
            this.preLayout[2] = param.width;
            this.preLayout[3] = param.height;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        FloatWindowService.FloatLayoutParams params = getLayoutParams();
        if (event.getAction() == 0 && this.mContext.getFocusedWindow() != this) {
            this.mContext.focus(this.id);
        }
        if (event.getPointerCount() < 2 || !Utils.isSet(this.flags, FloatWindowFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE)
                || event.getActionMasked() != MotionEvent.ACTION_POINTER_DOWN || this.isFullScreen) {
            return false;
        }
        VideoSurfaceTextureView vv = FloatVideoManager.getInstance(this.mContext).getFlowVideo(this.id);
        if (vv != null) {
            vv.hideControls();
        }
        this.touchInfo.scale = 1.0d;
        this.touchInfo.dist = -1.0d;
        this.touchInfo.firstWidth = (double) params.width;
        this.touchInfo.firstHeight = (double) params.height;
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case 4:
                if (this.mContext.getFocusedWindow() == this) {
                    this.mContext.unfocus(this);
                }
                this.mContext.onTouchBody(this.id, this, this, event);
                break;
        }
        if (event.getPointerCount() < 2 || !Utils.isSet(this.flags, FloatWindowFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE)) {
            return true;
        }
        double dist = Math.sqrt(Math.pow((double) (event.getX(0) - event.getX(1)), 2.0d)
                + Math.pow((double) (event.getY(0) - event.getY(1)), 2.0d));
        switch (event.getActionMasked()) {
            case 2:
                if (this.touchInfo.dist == -1.0d) {
                    this.touchInfo.dist = dist;
                }
                this.touchInfo.scale *= dist / this.touchInfo.dist;
                this.touchInfo.dist = dist;
                int width = (int) (this.touchInfo.firstWidth * this.touchInfo.scale);
                int height = (int) (this.touchInfo.firstHeight * this.touchInfo.scale);
                if (width > (this.mMetrics.widthPixels * 9) / 10 && height > (this.mMetrics.heightPixels * 9) / 10) {
                    return true;
                }
                if (Math.abs(getLayoutParams().width - width) <= 10
                        && Math.abs(getLayoutParams().height - height) <= 10) {
                    return true;
                }
                edit().setAnchorPoint(0.0f, 0.0f).setSize(width, height).commit();
                return true;
            default:
                return true;
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "Window " + this.id + " key event code " + event.getKeyCode());
        if (this.mContext.onKeyEvent(this.id, this, event)) {
            Log.d(TAG, "Window " + this.id + " key event " + event + " cancelled by implementation.");
            return false;
        }
        if (event.getAction() == 1) {
            switch (event.getKeyCode()) {
                case 4:
                    this.mContext.unfocus(this);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public boolean onFocus(boolean focus) {
        boolean z;
        if (Utils.isSet(this.flags, FloatWindowFlags.FLAG_WINDOW_FOCUSABLE_DISABLE) || focus == this.focused) {
            return false;
        }
        this.focused = focus;
        if (this.mContext.onFocusChange(this.id, this, focus)) {
            Log.d(TAG, "Window " + this.id + " focus change " + (focus ? "(true)" : "(false)")
                    + " cancelled by implementation.");
            if (focus) {
                z = false;
            } else {
                z = true;
            }
            this.focused = z;
            return false;
        }
        if (!Utils.isSet(this.flags, FloatWindowFlags.FLAG_WINDOW_FOCUS_INDICATOR_DISABLE)) {
            Log.d(TAG, " --------set focus boder -----------");
            View content = findViewById(R.id.content);
            if (focus) {
                content.setBackgroundResource(R.drawable.float_bg);
            } else if (Utils.isSet(this.flags, FloatWindowFlags.FLAG_DECORATION_SYSTEM)) {
                content.setBackgroundResource(R.drawable.border);
            } else {
                content.setBackgroundResource(0);
            }
        }
        FloatWindowService.FloatLayoutParams params = getLayoutParams();
        params.setFocusFlag(focus);
        params.alpha = 1.0f;
        this.mContext.updateViewLayout(this.id, params);
        if (focus) {
            this.mContext.setFocusedWindow(this);
        } else if (this.mContext.getFocusedWindow() == this) {
            this.mContext.setFocusedWindow((Window) null);
        }
        return true;
    }

    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params instanceof FloatWindowService.FloatLayoutParams) {
            super.setLayoutParams(params);
            return;
        }
        throw new IllegalArgumentException(TAG + this.id + ": LayoutParams must be an instance of FloatLayoutParams.");
    }

    public Editor edit() {
        return new Editor();
    }

    public FloatWindowService.FloatLayoutParams getLayoutParams() {
        FloatWindowService.FloatLayoutParams params = (FloatWindowService.FloatLayoutParams) super.getLayoutParams();
        if (params == null) {
            return this.originalParams;
        }
        return params;
    }

    private View getSystemDecorations() {
        View decorations = this.mLayoutInflater.inflate(R.layout.system_window_decorators, (ViewGroup) null);
        final ImageView icon = (ImageView) decorations.findViewById(R.id.window_icon);
        icon.setImageResource(this.mContext.getAppIcon());
        icon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PopupWindow dropDown = Window.this.mContext.getDropDown(Window.this.id);
                if (dropDown != null) {
                    dropDown.showAsDropDown(icon);
                }
            }
        });
        View hide = decorations.findViewById(R.id.hide);
        hide.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Window.this.mContext.hide(Window.this.id);
            }
        });
        hide.setVisibility(8);
        View close = decorations.findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Window.this.mContext.close(Window.this.id);
            }
        });
        View titlebar = decorations.findViewById(R.id.titlebar);
        titlebar.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return Window.this.mContext.onTouchHandleMove(Window.this.id, Window.this, v, event);
            }
        });
        View corner = decorations.findViewById(R.id.corner);
        corner.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return Window.this.mContext.onTouchHandleResize(Window.this.id, Window.this, v, event);
            }
        });
        if (Utils.isSet(this.flags, FloatWindowFlags.FLAG_WINDOW_HIDE_ENABLE)) {
            hide.setVisibility(0);
        }
        if (Utils.isSet(this.flags, FloatWindowFlags.FLAG_DECORATION_CLOSE_DISABLE)) {
            close.setVisibility(8);
        }
        if (Utils.isSet(this.flags, FloatWindowFlags.FLAG_DECORATION_MOVE_DISABLE)) {
            titlebar.setOnTouchListener((View.OnTouchListener) null);
        }
        if (Utils.isSet(this.flags, FloatWindowFlags.FLAG_DECORATION_RESIZE_DISABLE)) {
            corner.setVisibility(8);
        }
        return decorations;
    }

    public void addFunctionality(View root) {
        if (!Utils.isSet(this.flags, FloatWindowFlags.FLAG_ADD_FUNCTIONALITY_RESIZE_DISABLE)) {
            View corner = root.findViewById(R.id.corner);
            if (corner != null) {
                corner.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        return Window.this.mContext.onTouchHandleResize(Window.this.id, Window.this, v, event);
                    }
                });
            }
        }
        if (!Utils.isSet(this.flags, FloatWindowFlags.FLAG_ADD_FUNCTIONALITY_DROP_DOWN_DISABLE)) {
            final View icon = root.findViewById(R.id.window_icon);
            if (icon != null) {
                icon.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        PopupWindow dropDown = Window.this.mContext.getDropDown(Window.this.id);
                        if (dropDown != null) {
                            dropDown.showAsDropDown(icon);
                        }
                    }
                });
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void fixCompatibility(View root) {
        Queue<View> queue = new LinkedList<>();
        queue.add(root);
        while (true) {
            View view = queue.poll();
            if (view != null) {
                if (view instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) view;
                    for (int i = 0; i < group.getChildCount(); i++) {
                        queue.add(group.getChildAt(i));
                    }
                }
            } else {
                return;
            }
        }
    }

    public class Editor {
        public static final int UNCHANGED = Integer.MIN_VALUE;
        float anchorX = 0.0f;
        float anchorY = 0.0f;
        int displayHeight;
        int displayWidth;
        FloatWindowService.FloatLayoutParams mParams;

        public Editor() {
            this.mParams = Window.this.getLayoutParams();
            DisplayMetrics metrics = Window.this.mContext.getResources().getDisplayMetrics();
            this.displayWidth = metrics.widthPixels;
            this.displayHeight = metrics.heightPixels;
        }

        public Editor setAnchorPoint(float x, float y) {
            if (x < 0.0f || x > 1.0f || y < 0.0f || y > 1.0f) {
                throw new IllegalArgumentException("Anchor point must be between 0 and 1, inclusive.");
            }
            this.anchorX = x;
            this.anchorY = y;
            return this;
        }

        public Editor setSize(int width, int height) {
            return setSize(width, height, false);
        }

        private Editor setSize(int width, int height, boolean skip) {
            if (this.mParams != null) {
                if (this.anchorX < 0.0f || this.anchorX > 1.0f || this.anchorY < 0.0f || this.anchorY > 1.0f) {
                    throw new IllegalStateException("Anchor point must be between 0 and 1, inclusive.");
                }
                int lastWidth = this.mParams.width;
                int lastHeight = this.mParams.height;
                if (width != Integer.MIN_VALUE) {
                    this.mParams.width = width;
                }
                if (height != Integer.MIN_VALUE) {
                    this.mParams.height = height;
                }
                int maxWidth = this.mParams.maxWidth;
                int maxHeight = this.mParams.maxHeight;
                if (Utils.isSet(Window.this.flags, FloatWindowFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE)) {
                    maxWidth = Math.min(maxWidth, this.displayWidth);
                    maxHeight = Math.min(maxHeight, this.displayHeight);
                }
                this.mParams.width = Math.min(Math.max(this.mParams.width, this.mParams.minWidth), maxWidth);
                this.mParams.height = Math.min(Math.max(this.mParams.height, this.mParams.minHeight), maxHeight);
                int ratioWidth = (int) (((float) this.mParams.height) * Window.this.touchInfo.ratio);
                int ratioHeight = (int) (((float) this.mParams.width) / Window.this.touchInfo.ratio);
                if (ratioHeight < this.mParams.minHeight || ratioHeight > this.mParams.maxHeight) {
                    this.mParams.width = ratioWidth;
                } else {
                    this.mParams.height = ratioHeight;
                }
                if (!skip) {
                    setPosition((int) (((float) this.mParams.x) + (((float) lastWidth) * this.anchorX)),
                            (int) (((float) this.mParams.y) + (((float) lastHeight) * this.anchorY)));
                }
            }
            return this;
        }

        public Editor setPosition(int x, int y) {
            return setPosition(x, y, false);
        }

        public Editor fullScreen() {
            if (this.mParams != null) {
                Window.this.setPreLayout(this.mParams);
                this.mParams.width = -1;
                this.mParams.height = -1;
                Window.this.setSystemUiVisibility(4);
                setPosition(0, 0);
            }
            return this;
        }

        private Editor setPosition(int x, int y, boolean skip) {
            if (this.mParams != null) {
                if (this.anchorX < 0.0f || this.anchorX > 1.0f || this.anchorY < 0.0f || this.anchorY > 1.0f) {
                    throw new IllegalStateException("Anchor point must be between 0 and 1, inclusive.");
                }
                if (x != Integer.MIN_VALUE) {
                    this.mParams.x = (int) (((float) x) - (((float) this.mParams.width) * this.anchorX));
                }
                if (y != Integer.MIN_VALUE) {
                    this.mParams.y = (int) (((float) y) - (((float) this.mParams.height) * this.anchorY));
                }
                if (Utils.isSet(Window.this.flags, FloatWindowFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE)) {
                    if (this.mParams.gravity != 51) {
                        throw new IllegalStateException("The window " + Window.this.id
                                + " gravity must be TOP|LEFT if FLAG_WINDOW_EDGE_LIMITS_ENABLE or FLAG_WINDOW_EDGE_TILE_ENABLE is set.");
                    }
                    this.mParams.x = Math.min(Math.max(this.mParams.x, 0), this.displayWidth - this.mParams.width);
                    this.mParams.y = Math.min(Math.max(this.mParams.y, 0), this.displayHeight - this.mParams.height);
                }
            }
            return this;
        }

        public void commit() {
            if (this.mParams != null) {
                Window.this.mContext.updateViewLayout(Window.this.id, this.mParams);
                this.mParams = null;
            }
        }
    }

    public void startAnimate(int endX, int endY, int duration) {
        if (getLayoutParams().width != 0 && getLayoutParams().height != 0 && !this.isAnimating) {
            this.isAnimating = true;
            this.startX = getLayoutParams().x;
            this.startY = getLayoutParams().y;
            this.distanceX = endX - this.startX;
            this.distanceY = endY - this.startY;
            this.anim = ValueAnimator.ofFloat(new float[] { 0.0f, 1.0f });
            this.anim.setDuration((long) duration);
            this.anim.addUpdateListener(this);
            this.anim.setInterpolator(new DecelerateInterpolator(3.0f));
            this.anim.start();
        }
    }

    public void stopAnimate() {
        if (this.isAnimating) {
            this.anim.cancel();
            this.isAnimating = false;
        }
    }

    public void onAnimationUpdate(ValueAnimator anim2) {
        float value = ((Float) anim2.getAnimatedValue()).floatValue();
        if (value == 1.0f) {
            this.isAnimating = false;
        }
        getLayoutParams().x = this.startX + Math.round(((float) this.distanceX) * value);
        getLayoutParams().y = this.startY + Math.round(((float) this.distanceY) * value);
        this.mContext.updateViewLayout(this.id, this.originalParams);
    }

    public void cancelDimBehind() {
        getLayoutParams().flags &= -3;
        this.mContext.updateViewLayout(this.id, this.originalParams);
    }

    public void dimBehind() {
        getLayoutParams().flags |= 2;
        this.mContext.updateViewLayout(this.id, this.originalParams);
    }

    public void setFormat(int format) {
        getLayoutParams().format = format;
        this.mContext.updateViewLayout(this.id, this.originalParams);
    }

    class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        public void onOrientationChanged(int orientation) {
            DisplayMetrics metrics = Window.this.mContext.getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int a = Window.this.originalParams.width;
            int b = Window.this.originalParams.height;
            if (width != Window.this.mOldWidth || height != Window.this.mOldHeight) {
                Window.this.mOldWidth = width;
                Window.this.mOldHeight = height;
                if (width < a) {
                    Log.d("fuqiang", "screenwidth < videowidth");
                    Window.this.originalParams.width = width;
                    Window.this.originalParams.height = (b * width) / a;
                    Window.this.mContext.updateViewLayout(Window.this.id, Window.this.originalParams);
                }
                if (height < b) {
                    Log.d("fuqiang", "screenheight < videoheight");
                    Window.this.originalParams.width = (a * height) / b;
                    Window.this.originalParams.height = height;
                    Window.this.mContext.updateViewLayout(Window.this.id, Window.this.originalParams);
                }
            }
        }
    }
}
