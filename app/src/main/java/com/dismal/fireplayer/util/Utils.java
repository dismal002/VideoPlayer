package com.dismal.fireplayer.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.dismal.fireplayer.ui.AppConfig;
import com.dismal.fireplayer.util.ThumbsCache;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static final String IMAGE_CACHE_DIR = "thumbs";
    public static final int IO_BUFFER_SIZE = 8192;
    public static final String TAG = "Utils";
    public static final int THUMBS_DISKCACHE_SIZE = 20971520;
    public static final int THUMBS_DISKCACHE_SIZE_MAX = 104857600;

    private Utils() {
    }

    public static void disableConnectionReuseIfNecessary() {
        if (hasHttpConnectionBug()) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    @SuppressLint({"NewApi"})
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= 12) {
            return bitmap.getByteCount();
        }
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    @SuppressLint({"NewApi"})
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= 9) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    @SuppressLint({"NewApi"})
    public static File getExternalCacheDir(Context context) {
        if (hasExternalCacheDir()) {
            return context.getExternalCacheDir();
        }
        return new File(String.valueOf(Environment.getExternalStorageDirectory().getPath()) + ("/Android/data/" + context.getPackageName() + "/cache/"));
    }

    @SuppressLint({"NewApi"})
    public static long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= 9) {
            return path.getUsableSpace();
        }
        StatFs stats = new StatFs(path.getPath());
        return ((long) stats.getBlockSize()) * ((long) stats.getAvailableBlocks());
    }

    public static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService("activity")).getMemoryClass();
    }

    public static boolean hasHttpConnectionBug() {
        return Build.VERSION.SDK_INT < 8;
    }

    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= 8;
    }

    public static boolean hasActionBar() {
        return Build.VERSION.SDK_INT >= 11;
    }

    public static boolean isVersionBelowAndroid4_2() {
        return Build.VERSION.SDK_INT <= 16;
    }

    public static void makeToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static void fail(String message, Object... args) {
        if (args.length != 0) {
            message = String.format(message, args);
        }
        throw new AssertionError(message);
    }

    public static <T> T checkNotNull(T object) {
        if (object != null) {
            return object;
        }
        throw new NullPointerException();
    }

    public static boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static String stringForTime(long millis) {
        int totalSeconds = ((int) millis) / 1000;
        return String.format("%d:%02d:%02d", new Object[]{Integer.valueOf(totalSeconds / 3600), Integer.valueOf((totalSeconds / 60) % 60), Integer.valueOf(totalSeconds % 60)}).toString();
    }

    public static String stringForSize(long size) {
        int sizeMB = (int) (((10 * size) / 1024) / 1024);
        if (sizeMB < 10240) {
            return String.format("%.1fM", new Object[]{Float.valueOf(((float) sizeMB) / 10.0f)}).toString();
        }
        return String.format("%.1fG", new Object[]{Float.valueOf(((float) sizeMB) / 10240.0f)}).toString();
    }

    public static boolean checkMD5(String md5, File updateFile) {
        if (md5 == null || md5.equals("") || updateFile == null) {
            Log.e(TAG, "MD5 String NULL or UpdateFile NULL");
            return false;
        }
        String calculatedDigest = calculateMD5(updateFile);
        if (calculatedDigest == null) {
            Log.e(TAG, "calculatedDigest NULL");
            return false;
        }
        Log.v(TAG, "c digest: " + calculatedDigest);
        Log.v(TAG, "p digest: " + md5);
        return calculatedDigest.equalsIgnoreCase(md5);
    }

    public static String calculateMD5(File updateFile) {
        String output = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try {
                InputStream is = new FileInputStream(updateFile);
                byte[] buffer = new byte[8192];
                while (true) {
                    try {
                        int read = is.read(buffer);
                        if (read <= 0) {
                            break;
                        }
                        digest.update(buffer, 0, read);
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to process file for MD5", e);
                    } catch (Throwable th) {
                        try {
                            is.close();
                        } catch (IOException e2) {
                            Log.e(TAG, "Exception on closing MD5 input stream", e2);
                        }
                        throw th;
                    }
                }
                output = String.format("%32s", new Object[]{new BigInteger(1, digest.digest()).toString(16)}).replace(' ', '0');
                try {
                    is.close();
                } catch (IOException e3) {
                    Log.e(TAG, "Exception on closing MD5 input stream", e3);
                }
            } catch (FileNotFoundException e4) {
                Log.e(TAG, "Exception while getting FileInputStream", e4);
            }
        } catch (NoSuchAlgorithmException e5) {
            Log.e(TAG, "Exception while getting Digest", e5);
        }
        return output;
    }

    public static String convertMediaPath(String mPath) {
        return mPath;
    }

    public static String convertMediaPathUser(String mPath) {
        return mPath;
    }

    public static ThumbsCache.ImageCacheParams getCacheParameter(Context context, int divider) {
        ThumbsCache.ImageCacheParams cacheParams = new ThumbsCache.ImageCacheParams("thumbs");
        cacheParams.memCacheSize = (1048576 * getMemoryClass(context)) / divider;
        cacheParams.diskCacheSize = THUMBS_DISKCACHE_SIZE;
        return cacheParams;
    }

    public static String getCpuHardwareInfo() {
        String hardwareInfo = null;
        try {
            BufferedReader localBufferedReader = new BufferedReader(new FileReader("/proc/cpuinfo"), 8192);
            while (true) {
                String line = localBufferedReader.readLine();
                if (line != null) {
                    if (line.contains("Hardware")) {
                        hardwareInfo = line.split("\\s+")[2];
                        break;
                    }
                } else {
                    break;
                }
            }
            localBufferedReader.close();
        } catch (IOException e) {
        }
        Log.v(TAG, "cpuinfo Hardware:" + hardwareInfo);
        return hardwareInfo;
    }

    public static Boolean isNetworkStream(String path) {
        if (path == null) {
            return false;
        }
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("rtsp://")) {
            return true;
        }
        return false;
    }

    public static int convertCustomOrientation(int customOrientation) {
        if (customOrientation == 1) {
            return 0;
        }
        if (customOrientation == 2) {
            return 8;
        }
        if (customOrientation == 3) {
            return 6;
        }
        if (customOrientation == 4) {
            return 1;
        }
        return 10;
    }

    public static long getNetworkSpeed() {
        return TrafficStats.getTotalRxBytes();
    }

    public static void printCallBackStack() {
        StackTraceElement[] stackElements = new Throwable().getStackTrace();
        if (stackElements != null) {
            System.out.println("-----------------printCallBackStack------------------");
            for (int i = 0; i < stackElements.length; i++) {
                System.out.print(">>>> " + stackElements[i].getClassName() + "/");
                System.out.print(String.valueOf(stackElements[i].getFileName()) + "/line:");
                System.out.print(String.valueOf(stackElements[i].getLineNumber()) + "/");
                System.out.println(stackElements[i].getMethodName());
            }
        }
    }

    public static Object invokeMethod(Object owner, String methodName, Object[] args) {
        Class<?> ownerClass = owner.getClass();
        Class[] methodArgs = new Class[args.length];
        int i = 0;
        for (Object obj : args) {
            if (obj instanceof Integer) {
                methodArgs[i] = Integer.TYPE;
            } else {
                methodArgs[i] = args[i].getClass();
            }
            i++;
        }
        try {
            try {
                return ownerClass.getMethod(methodName, methodArgs).invoke(owner, args);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return null;
            } catch (IllegalAccessException e2) {
                e2.printStackTrace();
                return null;
            } catch (InvocationTargetException e3) {
                e3.printStackTrace();
                return null;
            }
        } catch (NoSuchMethodException e4) {
            Log.w(TAG, "no such method:" + methodName);
            return null;
        }
    }

    public static boolean checkMethodExist(Object owner, String methodName, Object[] args) {
        Class<?> ownerClass = owner.getClass();
        if (args != null) {
            Class[] methodArgs = new Class[args.length];
            int i = 0;
            for (Object obj : args) {
                if (obj instanceof Integer) {
                    methodArgs[i] = Integer.TYPE;
                } else {
                    methodArgs[i] = args[i].getClass();
                }
                i++;
            }
            try {
                Method method = ownerClass.getMethod(methodName, methodArgs);
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "no such method:" + methodName);
                return false;
            }
        } else {
            try {
                Method method2 = ownerClass.getMethod(methodName, (Class[]) null);
            } catch (NoSuchMethodException e2) {
                Log.w(TAG, "no such method:" + methodName);
                return false;
            }
        }
        return true;
    }

    public static void customOrientationAdapter(Activity activity, String cfgKey, int default_value) {
        if (Settings.System.getInt(activity.getContentResolver(), "accelerometer_rotation", 1) == 0) {
            activity.setRequestedOrientation(2);
            return;
        }
        int customOrientation = AppConfig.getInstance(activity.getApplicationContext()).getInt(cfgKey, default_value);
        if (customOrientation != 0) {
            activity.setRequestedOrientation(convertCustomOrientation(customOrientation));
        } else if (default_value == 0) {
            activity.setRequestedOrientation(10);
        } else {
            activity.setRequestedOrientation(0);
        }
    }

    public static boolean isSdkJB42OrAbove() {
        return Build.VERSION.SDK_INT >= 17;
    }

    public static boolean isSdkSoftwinner() {
        return true;
    }
}
