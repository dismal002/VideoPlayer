package com.dismal.fireplayer.floatwindow;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.dismal.fireplayer.R;
import com.dismal.fireplayer.provider.LocalMediaProviderContract;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class FloatWindowService extends Service {
    public static final String ACTION_CLOSE = "CLOSE";
    public static final String ACTION_CLOSE_ALL = "CLOSE_ALL";
    public static final String ACTION_FULL_SCREEN = "FULL_SCREEN";
    public static final String ACTION_HIDE = "HIDE";
    public static final String ACTION_ORIGINAL_SCREEN = "ORIGINAL_SCREEN";
    public static final String ACTION_RESTORE = "RESTORE";
    public static final String ACTION_SEND_DATA = "SEND_DATA";
    public static final String ACTION_SHOW = "SHOW";
    public static final int DEFAULT_ID = 0;
    public static final int DISREGARD_ID = -2;
    public static final int ONGOING_NOTIFICATION_ID = -1;
    static final String TAG = "FloatWindowService";
    static Window sFocusedWindow = null;
    static Window sFrontWindow = null;
    static WindowCache sWindowCache = new WindowCache();
    LayoutInflater mLayoutInflater;
    private NotificationManager mNotificationManager;
    WindowManager mWindowManager;
    /* access modifiers changed from: private */
    public boolean startedForeground;

    public abstract void createAndAttachView(int i, Intent intent, FrameLayout frameLayout);

    public abstract int getAppIcon();

    public abstract String getAppName();

    public abstract FloatLayoutParams getParams(int i, Window window);

    public static void show(Context context, Class<? extends FloatWindowService> cls, int id, String external) {
        context.startService(getShowIntent(context, cls, id, external));
    }

    public static void hide(Context context, Class<? extends FloatWindowService> cls, int id) {
        context.startService(getHideIntent(context, cls, id));
    }

    public static void close(Context context, Class<? extends FloatWindowService> cls, int id) {
        context.startService(getCloseIntent(context, cls, id));
    }

    public static void fullScreen(Context context, Class<? extends FloatWindowService> cls, int id) {
        context.startService(getFullScreenIntent(context, cls, id));
    }

    public static void originalScreen(Context context, Class<? extends FloatWindowService> cls, int id) {
        context.startService(getOriginalScreenIntent(context, cls, id));
    }

    public static void closeAll(Context context, Class<? extends FloatWindowService> cls) {
        context.startService(getCloseAllIntent(context, cls));
    }

    public static void sendData(Context context, Class<? extends FloatWindowService> toCls, int toId, int requestCode,
            Bundle data, Class<? extends FloatWindowService> fromCls, int fromId) {
        context.startService(getSendDataIntent(context, toCls, toId, requestCode, data, fromCls, fromId));
    }

    public static Intent getShowIntent(Context context, Class<? extends FloatWindowService> cls, int id, String path) {
        boolean cached = sWindowCache.isCached(id, cls);
        return new Intent(context, cls).putExtra("id", id).putExtra(LocalMediaProviderContract.PATH_COLUMN, path)
                .setAction(cached ? ACTION_RESTORE : ACTION_SHOW)
                .setData(cached ? Uri.parse("standout://" + cls + '/' + id) : null);
    }

    public static Intent getHideIntent(Context context, Class<? extends FloatWindowService> cls, int id) {
        return new Intent(context, cls).putExtra("id", id).setAction(ACTION_HIDE);
    }

    public static Intent getCloseIntent(Context context, Class<? extends FloatWindowService> cls, int id) {
        return new Intent(context, cls).putExtra("id", id).setAction(ACTION_CLOSE);
    }

    public static Intent getFullScreenIntent(Context context, Class<? extends FloatWindowService> cls, int id) {
        return new Intent(context, cls).putExtra("id", id).setAction(ACTION_FULL_SCREEN);
    }

    public static Intent getOriginalScreenIntent(Context context, Class<? extends FloatWindowService> cls, int id) {
        return new Intent(context, cls).putExtra("id", id).setAction(ACTION_ORIGINAL_SCREEN);
    }

    public static Intent getCloseAllIntent(Context context, Class<? extends FloatWindowService> cls) {
        return new Intent(context, cls).setAction(ACTION_CLOSE_ALL);
    }

    public static Intent getSendDataIntent(Context context, Class<? extends FloatWindowService> toCls, int toId,
            int requestCode, Bundle data, Class<? extends FloatWindowService> fromCls, int fromId) {
        return new Intent(context, toCls).putExtra("id", toId).putExtra("requestCode", requestCode)
                .putExtra("com.dismal.standout.data", data).putExtra("com.dismal.standout.fromCls", fromCls)
                .putExtra("fromId", fromId).setAction(ACTION_SEND_DATA);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        this.mWindowManager = (WindowManager) getSystemService("window");
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
        this.mLayoutInflater = (LayoutInflater) getSystemService("layout_inflater");
        this.startedForeground = false;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            String action = intent.getAction();
            int id = intent.getIntExtra("id", 0);
            if (ACTION_SHOW.equals(action) || ACTION_RESTORE.equals(action)) {
                show(id, intent);
                return 2;
            } else if (ACTION_HIDE.equals(action)) {
                hide(id);
                return 2;
            } else if (ACTION_CLOSE.equals(action)) {
                close(id);
                return 2;
            } else if (ACTION_FULL_SCREEN.equals(action)) {
                fullscreen(id);
                return 2;
            } else if (ACTION_ORIGINAL_SCREEN.equals(action)) {
                originalScreen(id);
                return 2;
            } else if (ACTION_CLOSE_ALL.equals(action)) {
                closeAll();
                return 2;
            } else if (!ACTION_SEND_DATA.equals(action)) {
                return 2;
            } else {
                if (isExistingId(id) || id == -2) {
                    onReceiveData(id, intent.getIntExtra("requestCode", 0),
                            intent.getBundleExtra("com.dismal.standout.data"),
                            (Class) intent.getSerializableExtra("com.dismal.standout.fromCls"),
                            intent.getIntExtra("fromId", 0));
                    return 2;
                }
                Log.w(TAG,
                        "Failed to send data to non-existant window. Make sure toId is either an existing window's id, or is DISREGARD_ID.");
                return 2;
            }
        } else {
            Log.w(TAG, "Tried to onStartCommand() with a null intent.");
            return 2;
        }
    }

    public void onDestroy() {
        super.onDestroy();
        closeAll();
    }

    public int getFlags(int id) {
        return 0;
    }

    public String getTitle(int id) {
        return getAppName();
    }

    public int getIcon(int id) {
        return getAppIcon();
    }

    public String getPersistentNotificationTitle(int id) {
        return String.valueOf(getAppName()) + " Running";
    }

    public String getPersistentNotificationMessage(int id) {
        return "";
    }

    public Intent getPersistentNotificationIntent(int id) {
        return null;
    }

    public int getHiddenIcon() {
        return getAppIcon();
    }

    public String getHiddenNotificationTitle(int id) {
        return String.valueOf(getAppName()) + " Hidden";
    }

    public String getHiddenNotificationMessage(int id) {
        return "";
    }

    public Intent getHiddenNotificationIntent(int id) {
        return null;
    }

    public Notification getPersistentNotification(int id) {
        int icon = getAppIcon();
        long when = System.currentTimeMillis();
        Context c = getApplicationContext();
        String contentTitle = getPersistentNotificationTitle(id);
        String contentText = getPersistentNotificationMessage(id);
        String tickerText = String.format("%s: %s", new Object[] { contentTitle, contentText });
        Intent notificationIntent = getPersistentNotificationIntent(id);
        PendingIntent contentIntent = null;
        if (notificationIntent != null) {
            contentIntent = PendingIntent.getService(this, 0, notificationIntent, 134217728);
        }
        Notification notification = new Notification.Builder(c)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setWhen(when)
                .setTicker(tickerText)
                .setContentIntent(contentIntent)
                .getNotification();
        return notification;
    }

    public Notification getHiddenNotification(int id) {
        int icon = getHiddenIcon();
        long when = System.currentTimeMillis();
        Context c = getApplicationContext();
        String contentTitle = getHiddenNotificationTitle(id);
        String contentText = getHiddenNotificationMessage(id);
        String tickerText = String.format("%s: %s", new Object[] { contentTitle, contentText });
        Intent notificationIntent = getHiddenNotificationIntent(id);
        PendingIntent contentIntent = null;
        if (notificationIntent != null) {
            contentIntent = PendingIntent.getService(this, 0, notificationIntent, 134217728);
        }
        Notification notification = new Notification.Builder(c)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setWhen(when)
                .setTicker(tickerText)
                .setContentIntent(contentIntent)
                .getNotification();
        return notification;
    }

    public Animation getShowAnimation(int id) {
        return AnimationUtils.loadAnimation(this, 17432576);
    }

    public Animation getHideAnimation(int id) {
        return AnimationUtils.loadAnimation(this, 17432577);
    }

    public Animation getCloseAnimation(int id) {
        return AnimationUtils.loadAnimation(this, 17432577);
    }

    public int getThemeStyle() {
        return 0;
    }

    public PopupWindow getDropDown(int id) {
        List<DropDownListItem> items;
        List<DropDownListItem> dropDownListItems = getDropDownItems(id);
        if (dropDownListItems != null) {
            items = dropDownListItems;
        } else {
            items = new ArrayList<>();
        }
        items.add(new DropDownListItem(17301560, "Quit " + getAppName(), new Runnable() {
            public void run() {
                FloatWindowService.this.closeAll();
            }
        }));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(1);
        final PopupWindow dropDown = new PopupWindow(list, -2, -2, true);
        for (final DropDownListItem item : items) {
            ViewGroup listItem = (ViewGroup) this.mLayoutInflater.inflate(R.layout.drop_down_list_item,
                    (ViewGroup) null);
            list.addView(listItem);
            ((ImageView) listItem.findViewById(R.id.icon)).setImageResource(item.icon);
            ((TextView) listItem.findViewById(R.id.description)).setText(item.description);
            listItem.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    item.action.run();
                    dropDown.dismiss();
                }
            });
        }
        dropDown.setBackgroundDrawable(getResources().getDrawable(17301530));
        return dropDown;
    }

    public List<DropDownListItem> getDropDownItems(int id) {
        return null;
    }

    public boolean onTouchBody(int id, Window window, View view, MotionEvent event) {
        return false;
    }

    public void onMove(int id, Window window, View view, MotionEvent event) {
    }

    public void onResize(int id, Window window, View view, MotionEvent event) {
    }

    public boolean onShow(int id, Window window) {
        return false;
    }

    public boolean onFullScreen(int id, Window window) {
        return false;
    }

    public boolean onHide(int id, Window window) {
        return false;
    }

    public boolean onClose(int id, Window window) {
        return false;
    }

    public boolean onCloseAll() {
        return false;
    }

    public void onReceiveData(int id, int requestCode, Bundle data, Class<? extends FloatWindowService> cls,
            int fromId) {
    }

    public boolean onUpdate(int id, Window window, FloatLayoutParams params) {
        return false;
    }

    public boolean onBringToFront(int id, Window window) {
        return false;
    }

    public boolean onFocusChange(int id, Window window, boolean focus) {
        return false;
    }

    public boolean onKeyEvent(int id, Window window, KeyEvent event) {
        return false;
    }

    public final synchronized Window show(int id, Intent intent) {
        Window window;
        Window cachedWindow = getWindow(id);
        if (cachedWindow != null) {
            Log.d(TAG, "Window0 " + id);
            window = cachedWindow;
        } else {
            Log.d(TAG, "Window1 " + id);
            window = new Window(this, id, intent);
        }
        Log.d(TAG, "Window2 " + id);
        if (window.visibility != 1) {
            if (onShow(id, window)) {
                Log.d(TAG, "Window " + id + " show cancelled by implementation.");
                window = null;
            } else {
                window.visibility = 1;
                Animation animation = getShowAnimation(id);
                try {
                    this.mWindowManager.addView(window, window.getLayoutParams());
                    if (animation != null) {
                        window.getChildAt(0).startAnimation(animation);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                sWindowCache.putCache(id, getClass(), window);
                sFrontWindow = window;
                focus(id);
            }
        }
        return window;
    }

    public final synchronized void fullscreen(int id) {
        Window window = getWindow(id);
        if (window != null) {
            if (window.visibility == 1) {
                if (onFullScreen(id, window)) {
                    Log.d(TAG, "Window " + id + " show cancelled by implementation.");
                } else {
                    window.edit().fullScreen().commit();
                }
            }
        }
    }

    public final synchronized void originalScreen(int id) {
        boolean z;
        boolean z2 = true;
        synchronized (this) {
            Window window = getWindow(id);
            if (window != null) {
                if (window.visibility == 1) {
                    FloatLayoutParams p = window.getLayoutParams();
                    int[] pre = window.getPreLayout();
                    p.x = pre[0];
                    p.y = pre[1];
                    p.width = pre[2];
                    p.height = pre[3];
                    window.setSystemUiVisibility(window.getSystemUiVisibility() & -5);
                    if (p.width == 0) {
                        z = true;
                    } else {
                        z = false;
                    }
                    if (p.width != getResources().getDisplayMetrics().widthPixels) {
                        z2 = false;
                    }
                    if (z2 || z) {
                        p = getParams(id, window);
                        window.originalParams = p;
                        window.setLayoutParams(p);
                    }
                    updateViewLayout(id, p);
                }
            }
        }
    }

    public final synchronized void hide(int id) {
        final Window window = getWindow(id);
        if (window != null) {
            if (window.visibility != 0) {
                if (onHide(id, window)) {
                    Log.w(TAG, "Window " + id + " hide cancelled by implementation.");
                } else if (Utils.isSet(window.flags, FloatWindowFlags.FLAG_WINDOW_HIDE_ENABLE)) {
                    window.visibility = 2;
                    Notification notification = getHiddenNotification(id);
                    Animation animation = getHideAnimation(id);
                    if (animation != null) {
                        try {
                            animation.setAnimationListener(new Animation.AnimationListener() {
                                public void onAnimationStart(Animation animation) {
                                }

                                public void onAnimationRepeat(Animation animation) {
                                }

                                public void onAnimationEnd(Animation animation) {
                                    FloatWindowService.this.mWindowManager.removeView(window);
                                    window.visibility = 0;
                                }
                            });
                            window.getChildAt(0).startAnimation(animation);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        this.mWindowManager.removeView(window);
                    }
                    notification.flags = notification.flags | 32 | 16;
                    this.mNotificationManager.notify(getClass().hashCode() + id, notification);
                } else {
                    close(id);
                }
            }
        }
    }

    public final synchronized void close(final int id) {
        final Window window = getWindow(id);
        if (window != null) {
            if (window.visibility != 2) {
                Log.d(TAG, "Windows close id2 = " + id);
                if (onClose(id, window)) {
                    Log.w(TAG, "Window " + id + " close cancelled by implementation.");
                } else {
                    this.mNotificationManager.cancel(getClass().hashCode() + id);
                    unfocus(window);
                    window.visibility = 2;
                    Animation animation = getCloseAnimation(id);
                    if (animation != null) {
                        try {
                            animation.setAnimationListener(new Animation.AnimationListener() {
                                public void onAnimationStart(Animation animation) {
                                }

                                public void onAnimationRepeat(Animation animation) {
                                }

                                public void onAnimationEnd(Animation animation) {
                                    FloatWindowService.this.mWindowManager.removeView(window);
                                    window.visibility = 0;
                                    FloatWindowService.sWindowCache.removeCache(id, FloatWindowService.this.getClass());
                                    if (FloatWindowService.this.getExistingIds().size() == 0) {
                                        FloatWindowService.this.startedForeground = false;
                                        FloatWindowService.this.stopForeground(true);
                                    }
                                }
                            });
                            window.getChildAt(0).startAnimation(animation);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        this.mWindowManager.removeView(window);
                        sWindowCache.removeCache(id, getClass());
                        if (sWindowCache.getCacheSize(getClass()) == 0) {
                            this.startedForeground = false;
                            stopForeground(true);
                        }
                    }
                }
            }
        }
        return;
    }

    public final synchronized void closeAll() {
        if (onCloseAll()) {
            Log.w(TAG, "Windows close all cancelled by implementation.");
        } else {
            LinkedList<Integer> ids = new LinkedList<>();
            for (Integer intValue : getExistingIds()) {
                int id = intValue.intValue();
                Log.d(TAG, "Windows close all id0 = " + id);
                ids.add(Integer.valueOf(id));
            }
            Iterator it = ids.iterator();
            while (it.hasNext()) {
                int id2 = ((Integer) it.next()).intValue();
                Log.d(TAG, "Windows close all id1 = " + id2);
                close(id2);
            }
        }
    }

    public final void sendData(int fromId, Class<? extends FloatWindowService> toCls, int toId, int requestCode,
            Bundle data) {
        sendData(this, toCls, toId, requestCode, data, getClass(), fromId);
    }

    public final synchronized boolean bringToFront(int id) {
        boolean z = false;
        synchronized (this) {
            Window window = getWindow(id);
            if (window != null) {
                if (!(window.visibility == 0 || window.visibility == 2 || getFrontWindow() == window)) {
                    Log.d(TAG, "bringToFront id1 = " + id);
                    if (onBringToFront(id, window)) {
                        Log.w(TAG, "Window " + id + " bring to front cancelled by implementation.");
                    } else {
                        FloatLayoutParams params = window.getLayoutParams();
                        Log.d(TAG, "bringToFront id2 = " + id);
                        try {
                            this.mWindowManager.updateViewLayout(window, params);
                            Log.d(TAG, "bringToFront id3 = " + id);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        sFrontWindow = window;
                        Log.d(TAG, "bringToFront id4 = " + id);
                        z = true;
                    }
                }
            }
        }
        return z;
    }

    public final synchronized boolean focus(int id) {
        boolean z = false;
        synchronized (this) {
            Window window = getWindow(id);
            if (window == null) {
                Log.d(TAG, "Tried to focus(" + id + ") a null window.");
            } else if (!Utils.isSet(window.flags, FloatWindowFlags.FLAG_WINDOW_FOCUSABLE_DISABLE)) {
                if (sFocusedWindow != null) {
                    unfocus(sFocusedWindow);
                }
                z = window.onFocus(true);
            }
        }
        return z;
    }

    public final synchronized boolean unfocus(int id) {
        return unfocus(getWindow(id));
    }

    public final int getUniqueId() {
        int unique = 0;
        for (Integer intValue : getExistingIds()) {
            unique = Math.max(unique, intValue.intValue() + 1);
        }
        return unique;
    }

    public final boolean isExistingId(int id) {
        return sWindowCache.isCached(id, getClass());
    }

    public final Set<Integer> getExistingIds() {
        return sWindowCache.getCacheIds(getClass());
    }

    public final Window getWindow(int id) {
        return sWindowCache.getCache(id, getClass());
    }

    public final Window getFocusedWindow() {
        return sFocusedWindow;
    }

    public final void setFocusedWindow(Window window) {
        sFocusedWindow = window;
    }

    public final Window getFrontWindow() {
        return sFrontWindow;
    }

    public final void setFrontWindow(Window window) {
        sFrontWindow = window;
    }

    public final void setTitle(int id, String text) {
        Window window = getWindow(id);
        if (window != null) {
            View title = window.findViewById(R.id.title);
            if (title instanceof TextView) {
                ((TextView) title).setText(text);
            }
        }
    }

    public final void setIcon(int id, int drawableRes) {
        Window window = getWindow(id);
        if (window != null) {
            View icon = window.findViewById(R.id.window_icon);
            if (icon instanceof ImageView) {
                ((ImageView) icon).setImageResource(drawableRes);
            }
        }
    }

    public boolean onTouchHandleMove(int id, Window window, View view, MotionEvent event) {
        boolean tap = true;
        boolean consumed = false;
        FloatLayoutParams params = window.getLayoutParams();
        int totalDeltaX = window.touchInfo.lastX - window.touchInfo.firstX;
        int totalDeltaY = window.touchInfo.lastY - window.touchInfo.firstY;
        switch (event.getAction()) {
            case 0:
                Log.v("fuqiang", "MotionEvent.ACTION_DOWN");
                window.touchInfo.lastX = (int) event.getRawX();
                window.touchInfo.lastY = (int) event.getRawY();
                window.touchInfo.firstX = window.touchInfo.lastX;
                window.touchInfo.firstY = window.touchInfo.lastY;
                if (Utils.isSet(window.flags, FloatWindowFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TOUCH)) {
                    consumed = bringToFront(id);
                    break;
                }
                break;
            case 1:
                Log.v("fuqiang", "MotionEvent.ACTION_UP");
                window.touchInfo.moving = false;
                if (event.getPointerCount() == 1) {
                    if (Math.abs(totalDeltaX) >= params.threshold || Math.abs(totalDeltaY) >= params.threshold) {
                        tap = false;
                    }
                    if (tap && Utils.isSet(window.flags, FloatWindowFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP)) {
                        consumed = bringToFront(id);
                        break;
                    }
                }
                break;
            case 2:
                Log.v("fuqiang", "MotionEvent.ACTION_MOVE");
                int deltaX = ((int) event.getRawX()) - window.touchInfo.lastX;
                int deltaY = ((int) event.getRawY()) - window.touchInfo.lastY;
                window.touchInfo.lastX = (int) event.getRawX();
                window.touchInfo.lastY = (int) event.getRawY();
                if (window.touchInfo.moving || Math.abs(totalDeltaX) >= params.threshold
                        || Math.abs(totalDeltaY) >= params.threshold) {
                    window.touchInfo.moving = true;
                    if (Utils.isSet(window.flags, FloatWindowFlags.FLAG_BODY_MOVE_ENABLE)) {
                        if (event.getPointerCount() == 1) {
                            params.x += deltaX;
                            params.y += deltaY;
                        }
                        window.edit().setPosition(params.x, params.y).commit();
                        consumed = false;
                        break;
                    }
                }
                break;
        }
        onMove(id, window, view, event);
        return consumed;
    }

    public boolean onTouchHandleResize(int id, Window window, View view, MotionEvent event) {
        FloatLayoutParams params = window.getLayoutParams();
        switch (event.getAction()) {
            case 0:
                window.touchInfo.lastX = (int) event.getRawX();
                window.touchInfo.lastY = (int) event.getRawY();
                window.touchInfo.firstX = window.touchInfo.lastX;
                window.touchInfo.firstY = window.touchInfo.lastY;
                break;
            case 2:
                int deltaX = ((int) event.getRawX()) - window.touchInfo.lastX;
                int deltaY = ((int) event.getRawY()) - window.touchInfo.lastY;
                params.width += deltaX;
                params.height += deltaY;
                if (params.width >= params.minWidth && params.width <= params.maxWidth) {
                    window.touchInfo.lastX = (int) event.getRawX();
                }
                if (params.height >= params.minHeight && params.height <= params.maxHeight) {
                    window.touchInfo.lastY = (int) event.getRawY();
                }
                window.edit().setSize(params.width, params.height).commit();
                break;
        }
        onResize(id, window, view, event);
        return true;
    }

    public synchronized boolean unfocus(Window window) {
        boolean z = false;
        synchronized (this) {
            if (window != null) {
                z = window.onFocus(false);
            }
        }
        return z;
    }

    public void updateViewLayout(int id, FloatLayoutParams params) {
        Window window = getWindow(id);
        Log.v("fuqiang", "updateViewLayout");
        if (window == null) {
            Log.d(TAG, "Tried to updateViewLayout(" + id + ") a null window");
        } else if (window.visibility != 0 && window.visibility != 2) {
            if (onUpdate(id, window, params)) {
                Log.w(TAG, "Window " + id + " update cancelled by implementation.");
            } else if (params.width != 0 && params.height != 0) {
                try {
                    window.setLayoutParams(params);
                    this.mWindowManager.updateViewLayout(window, params);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public class FloatLayoutParams extends WindowManager.LayoutParams {
        public static final int AUTO_POSITION = -2147483647;
        public static final int BOTTOM = Integer.MAX_VALUE;
        public static final int CENTER = Integer.MIN_VALUE;
        public static final int LEFT = 0;
        public static final int RIGHT = Integer.MAX_VALUE;
        public static final int TOP = 0;
        public int maxHeight;
        public int maxWidth;
        public int minHeight;
        public int minWidth;
        public int threshold;

        public FloatLayoutParams(int id) {
            super(200, 200, android.os.Build.VERSION.SDK_INT >= 26 ? 2038 : 2002, 17039392, 1);
            int windowFlags = FloatWindowService.this.getFlags(id);
            setFocusFlag(false);
            if (!Utils.isSet(windowFlags, FloatWindowFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE)) {
                this.flags |= 512;
            }
            this.alpha = 1.0f;
            this.x = getX(id, this.width);
            this.y = getY(id, this.height);
            this.gravity = 51;
            this.threshold = 10;
            this.minHeight = 0;
            this.minWidth = 0;
            this.maxHeight = Integer.MAX_VALUE;
            this.maxWidth = Integer.MAX_VALUE;
        }

        public FloatLayoutParams(FloatWindowService floatWindowService, int id, int w, int h) {
            this(id);
            this.width = w;
            this.height = h;
        }

        public FloatLayoutParams(FloatWindowService floatWindowService, int id, int w, int h, int xpos, int ypos) {
            this(floatWindowService, id, w, h);
            if (xpos != -2147483647) {
                this.x = xpos;
            }
            if (ypos != -2147483647) {
                this.y = ypos;
            }
            Display display = floatWindowService.mWindowManager.getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            if (this.x == Integer.MAX_VALUE) {
                this.x = width - w;
            } else if (this.x == Integer.MIN_VALUE) {
                this.x = (width - w) / 2;
            }
            if (this.y == Integer.MAX_VALUE) {
                this.y = height - h;
            } else if (this.y == Integer.MIN_VALUE) {
                this.y = (height - (h / 2)) / 2;
            }
        }

        public FloatLayoutParams(FloatWindowService floatWindowService, int id, int w, int h, int xpos, int ypos,
                int minWidth2, int minHeight2) {
            this(floatWindowService, id, w, h, xpos, ypos);
            this.minWidth = minWidth2;
            this.minHeight = minHeight2;
        }

        public FloatLayoutParams(FloatWindowService floatWindowService, int id, int w, int h, int xpos, int ypos,
                int minWidth2, int minHeight2, int threshold2) {
            this(floatWindowService, id, w, h, xpos, ypos, minWidth2, minHeight2);
            this.threshold = threshold2;
        }

        private int getX(int id, int width) {
            return ((FloatWindowService.sWindowCache.size() * 100) + (id * 100))
                    % (FloatWindowService.this.mWindowManager.getDefaultDisplay().getWidth() - width);
        }

        private int getY(int id, int height) {
            Display display = FloatWindowService.this.mWindowManager.getDefaultDisplay();
            return ((FloatWindowService.sWindowCache.size() * 100)
                    + (this.x + (((id * 100) * 200) / (display.getWidth() - this.width))))
                    % (display.getHeight() - height);
        }

        public void setFocusFlag(boolean focused) {
            if (focused) {
                this.flags ^= 8;
            } else {
                this.flags |= 8;
            }
        }
    }

    protected class DropDownListItem {
        public Runnable action;
        public String description;
        public int icon;

        public DropDownListItem(int icon2, String description2, Runnable action2) {
            this.icon = icon2;
            this.description = description2;
            this.action = action2;
        }

        public String toString() {
            return this.description;
        }
    }
}
