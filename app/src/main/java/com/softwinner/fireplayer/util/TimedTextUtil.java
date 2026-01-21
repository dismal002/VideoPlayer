package com.softwinner.fireplayer.util;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.TimedText;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.List;

public class TimedTextUtil {
    private static final String TAG = "TimedTextUtil";

    public static List<?> AWExtend_getStyleList(TimedText tt) {
        try {
            Method m = tt.getClass().getMethod("AWExtend_getStyleList");
            return (List<?>) m.invoke(tt);
        } catch (Exception e) {
            Log.w(TAG, "AWExtend_getStyleList not found");
            return null;
        }
    }

    public static Rect AWExtend_getTextScreenBounds(TimedText tt) {
        try {
            Method m = tt.getClass().getMethod("AWExtend_getTextScreenBounds");
            return (Rect) m.invoke(tt);
        } catch (Exception e) {
            Log.w(TAG, "AWExtend_getTextScreenBounds not found");
            return null;
        }
    }

    public static int AWExtend_getSubDispPos(TimedText tt) {
        try {
            Method m = tt.getClass().getMethod("AWExtend_getSubDispPos");
            return (Integer) m.invoke(tt);
        } catch (Exception e) {
            Log.w(TAG, "AWExtend_getSubDispPos not found");
            return 0;
        }
    }

    public static int AWExtend_getBitmapSubtitleFlag(TimedText tt) {
        try {
            Method m = tt.getClass().getMethod("AWExtend_getBitmapSubtitleFlag");
            return (Integer) m.invoke(tt);
        } catch (Exception e) {
            Log.w(TAG, "AWExtend_getBitmapSubtitleFlag not found");
            return 0;
        }
    }

    public static Bitmap AWExtend_getBitmap(TimedText tt) {
        try {
            Method m = tt.getClass().getMethod("AWExtend_getBitmap");
            return (Bitmap) m.invoke(tt);
        } catch (Exception e) {
            Log.w(TAG, "AWExtend_getBitmap not found");
            return null;
        }
    }

    public static int AWExtend_getHideSubFlag(TimedText tt) {
        try {
            Method m = tt.getClass().getMethod("AWExtend_getHideSubFlag");
            return (Integer) m.invoke(tt);
        } catch (Exception e) {
            Log.w(TAG, "AWExtend_getHideSubFlag not found");
            return 0;
        }
    }
}
