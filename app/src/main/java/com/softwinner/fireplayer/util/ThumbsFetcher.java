package com.softwinner.fireplayer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import com.softwinner.fireplayer.provider.LocalMediaInfo;
import java.io.File;

public class ThumbsFetcher extends ThumbsWorker {
    private static final int DEFAULT_MICRO_HEIGHT = 144;
    private static final int DEFAULT_MICRO_WIDTH = 256;
    private static final String TAG = "ThumbsFetcher";

    public ThumbsFetcher(Context context, int imageWidth, int imageHeight) {
        super(context);
    }

    public ThumbsFetcher(Context context) {
        super(context);
    }

    public Bitmap processThumbVideo(Object data) {
        LocalMediaInfo mediaInfo = (LocalMediaInfo) data;
        String path = mediaInfo.mPath;
        if (!new File(Utils.convertMediaPathUser(path)).exists()) {
            Log.w(TAG, "get thumb pic error: file not exist! path=" + path);
            return null;
        }
        String path2 = Utils.convertMediaPath(path);
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        try {
            metadataRetriever.setDataSource(Utils.convertMediaPath(mediaInfo.mPath));
            Bitmap stream = metadataRetriever.getFrameAtTime(20000000, 3);
            if (stream == null || stream.getAllocationByteCount() == 0) {
                Log.e(TAG, "get micro stream error!!");
                return null;
            }
            metadataRetriever.release();
            return stream;
        } catch (Exception e) {
            Log.e(TAG, "processThumbVideo Exception!");
            e.printStackTrace();
            return null;
        } finally {
            metadataRetriever.release();
        }
    }

    private Bitmap processThumbPicImp(LocalMediaInfo mediaInfo) {
        String path = mediaInfo.mPath;
        if (!new File(Utils.convertMediaPathUser(path)).exists()) {
            Log.w(TAG, "get thumb pic error: file not exist! path=" + path);
            return null;
        }
        String path2 = Utils.convertMediaPath(path);
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        try {
            metadataRetriever.setDataSource(path2);
            if (mediaInfo.mMediaInfoGetted != 1) {
                String duration = metadataRetriever.extractMetadata(9);
                String vw = metadataRetriever.extractMetadata(18);
                String vh = metadataRetriever.extractMetadata(19);
                if (duration != null) {
                    mediaInfo.mDuration = Integer.valueOf(duration).intValue();
                    mediaInfo.mMediaInfoGetted = 1;
                }
                if (vw != null) {
                    mediaInfo.mWidth = Integer.valueOf(vw).intValue();
                }
                if (vh != null) {
                    mediaInfo.mHeight = Integer.valueOf(vh).intValue();
                }
            }
            Bitmap originBmp = metadataRetriever.getFrameAtTime(20000000, 0);
            if (originBmp != null) {
                Bitmap createScaledBitmap = Bitmap.createScaledBitmap(originBmp, 256, DEFAULT_MICRO_HEIGHT, true);
                originBmp.recycle();
                return createScaledBitmap;
            }
            Log.e(TAG, "get thumb pic error! path=" + path2);
            metadataRetriever.release();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "processThumbPic Exception!");
            e.printStackTrace();
            return null;
        } finally {
            metadataRetriever.release();
        }
    }

    /* access modifiers changed from: protected */
    public Bitmap processThumbPic(Object data) {
        return processThumbPicImp((LocalMediaInfo) data);
    }
}
