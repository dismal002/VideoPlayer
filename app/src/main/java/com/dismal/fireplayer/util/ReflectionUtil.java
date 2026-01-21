package com.dismal.fireplayer.util;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.Display;
import java.lang.reflect.Method;

public class ReflectionUtil {
    private static final String TAG = "ReflectionUtil";

    public static String getSubCharset(MediaPlayer mp) {
        try {
            Method m = mp.getClass().getMethod("getSubCharset");
            return (String) m.invoke(mp);
        } catch (Exception e) {
            Log.w(TAG, "getSubCharset not found");
            return null;
        }
    }

    public static int setSubDelay(MediaPlayer mp, int time) {
        try {
            Method m = mp.getClass().getMethod("setSubDelay", int.class);
            return (Integer) m.invoke(mp, time);
        } catch (Exception e) {
            Log.w(TAG, "setSubDelay not found");
            return -1;
        }
    }

    public static int getDisplayType(Display display) {
        try {
            Method m = display.getClass().getMethod("getType");
            return (Integer) m.invoke(display);
        } catch (Exception e) {
            Log.w(TAG, "Display.getType not found");
            return -1;
        }
    }

    public static int getLayerStack(Display display) {
        try {
            Method m = display.getClass().getMethod("getLayerStack");
            return (Integer) m.invoke(display);
        } catch (Exception e) {
            Log.w(TAG, "Display.getLayerStack not found");
            return -1;
        }
    }

    public static void makeOptionalFitsSystemWindows(Object obj) {
        try {
            Method m = obj.getClass().getMethod("makeOptionalFitsSystemWindows");
            m.invoke(obj);
        } catch (Exception e) {
            Log.w(TAG, "makeOptionalFitsSystemWindows not found on " + obj.getClass().getName());
        }
    }
}
