package com.softwinner.fireplayer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiskLruCache {
    private static final String CACHE_FILENAME_PREFIX = "cache_";
    private static final int INITIAL_CAPACITY = 32;
    private static final float LOAD_FACTOR = 0.75f;
    private static final int MAX_REMOVALS = 4;
    private static final String TAG = "DiskLruCache";
    private static final FilenameFilter cacheFileFilter = new FilenameFilter() {
        public boolean accept(File dir, String filename) {
            return filename.startsWith(DiskLruCache.CACHE_FILENAME_PREFIX);
        }
    };
    private int cacheByteSize = 0;
    private int cacheSize = 0;
    private final File mCacheDir;
    private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.JPEG;
    private int mCompressQuality = 80;
    private final Map<String, String> mLinkedHashMap = Collections
            .synchronizedMap(new LinkedHashMap(32, LOAD_FACTOR, true));
    private long maxCacheByteSize = 52428800;
    private final int maxCacheItemSize = 5000;

    public static DiskLruCache openCache(Context context, File cacheDir, long maxByteSize) {
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        if (!cacheDir.isDirectory() || !cacheDir.canWrite() || Utils.getUsableSpace(cacheDir) <= maxByteSize) {
            return null;
        }
        if (Utils.getUsableSpace(cacheDir) > 104857600) {
            maxByteSize = 104857600;
            Log.v(TAG, "resize max cache size to " + 104857600);
        } else if (Utils.getUsableSpace(cacheDir) > 52428800) {
            maxByteSize = 52428800;
            Log.v(TAG, "resize max cache size to " + 52428800);
        }
        return new DiskLruCache(cacheDir, maxByteSize);
    }

    private DiskLruCache(File cacheDir, long maxByteSize) {
        this.mCacheDir = cacheDir;
        this.maxCacheByteSize = maxByteSize;
    }

    public void put(String key, Bitmap data) {
        synchronized (this.mLinkedHashMap) {
            if (this.mLinkedHashMap.get(key) == null) {
                try {
                    String file = createFilePath(this.mCacheDir, key);
                    if (writeBitmapToFile(data, file)) {
                        put(key, file);
                        flushCache();
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Error in put: " + e.getMessage());
                } catch (IOException e2) {
                    Log.e(TAG, "Error in put: " + e2.getMessage());
                }
            }
        }
        return;
    }

    private void put(String key, String file) {
        this.mLinkedHashMap.put(key, file);
        this.cacheSize = this.mLinkedHashMap.size();
        this.cacheByteSize = (int) (((long) this.cacheByteSize) + new File(file).length());
    }

    public void put(String key, Object data) {
        synchronized (this.mLinkedHashMap) {
            if (this.mLinkedHashMap.get(key) == null) {
                String file = createFilePath(this.mCacheDir, key);
                if (writeStreamToFile(data, file)) {
                    put(key, file);
                    flushCache();
                }
            }
        }
        return;
    }

    private void flushCache() {
        int count = 0;
        while (count < 4) {
            if (this.cacheSize > 5000 || ((long) this.cacheByteSize) > this.maxCacheByteSize) {
                Map.Entry<String, String> eldestEntry = this.mLinkedHashMap.entrySet().iterator().next();
                File eldestFile = new File(eldestEntry.getValue());
                long eldestFileSize = eldestFile.length();
                this.mLinkedHashMap.remove(eldestEntry.getKey());
                eldestFile.delete();
                this.cacheSize = this.mLinkedHashMap.size();
                this.cacheByteSize = (int) (((long) this.cacheByteSize) - eldestFileSize);
                count++;
            } else {
                return;
            }
        }
    }

    public Bitmap get(String key) {
        synchronized (this.mLinkedHashMap) {
            String file = this.mLinkedHashMap.get(key);
            if (file != null) {
                Bitmap decodeFile = BitmapFactory.decodeFile(file);
                return decodeFile;
            }
            String existingFile = createFilePath(this.mCacheDir, key);
            if (!new File(existingFile).exists()) {
                return null;
            }
            put(key, existingFile);
            Bitmap decodeFile2 = BitmapFactory.decodeFile(existingFile);
            return decodeFile2;
        }
    }

    public String getPath(String key) {
        synchronized (this.mLinkedHashMap) {
            String file = this.mLinkedHashMap.get(key);
            if (file != null) {
                return file;
            }
            String existingFile = createFilePath(this.mCacheDir, key);
            if (!new File(existingFile).exists()) {
                return null;
            }
            put(key, existingFile);
            return existingFile;
        }
    }

    public boolean containsKey(String key) {
        if (this.mLinkedHashMap.containsKey(key)) {
            return true;
        }
        String existingFile = createFilePath(this.mCacheDir, key);
        if (!new File(existingFile).exists()) {
            return false;
        }
        put(key, existingFile);
        return true;
    }

    public void clearCache() {
        clearCache(this.mCacheDir);
    }

    public static void clearCache(Context context, String uniqueName) {
        clearCache(getDiskCacheDir(context, uniqueName));
    }

    private static void clearCache(File cacheDir) {
        File[] files = cacheDir.listFiles(cacheFileFilter);
        for (File delete : files) {
            delete.delete();
        }
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        File cacheFile = Environment.getExternalStorageState().equals("mounted") ? Utils.getExternalCacheDir(context)
                : context.getCacheDir();
        if (cacheFile == null) {
            cacheFile = context.getCacheDir();
        }
        return new File(String.valueOf(cacheFile.getPath()) + File.separator + uniqueName);
    }

    public static String createFilePath(File cacheDir, String key) {
        try {
            return String.valueOf(cacheDir.getAbsolutePath()) + File.separator + CACHE_FILENAME_PREFIX
                    + URLEncoder.encode(key.replace("*", ""), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "createFilePath - " + e);
            return null;
        }
    }

    public String createFilePath(String key) {
        return createFilePath(this.mCacheDir, key);
    }

    public void setCompressParams(Bitmap.CompressFormat compressFormat, int quality) {
        this.mCompressFormat = compressFormat;
        this.mCompressQuality = quality;
    }

    private boolean writeBitmapToFile(Bitmap bitmap, String file) throws IOException, FileNotFoundException {
        OutputStream out = null;
        try {
            OutputStream out2 = new BufferedOutputStream(new FileOutputStream(file), 8192);
            try {
                boolean compress = bitmap.compress(this.mCompressFormat, this.mCompressQuality, out2);
                if (out2 != null) {
                    out2.close();
                }
                return compress;
            } catch (Throwable th) {
                out = out2;
                throw th;
            }
        } catch (Throwable th2) {
            if (out != null) {
                out.close();
            }
            throw th2;
        }
    }

    private boolean writeStreamToFile(Object data, String file) {
        if (data instanceof Bitmap) {
            Bitmap bitmap = (Bitmap) data;
            BufferedOutputStream out = null;
            try {
                out = new BufferedOutputStream(new FileOutputStream(file), 8 * 1024);
                return bitmap.compress(mCompressFormat, mCompressQuality, out);
            } catch (Exception e) {
                Log.e(TAG, "Error in writeStreamToFile - " + e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }
}
