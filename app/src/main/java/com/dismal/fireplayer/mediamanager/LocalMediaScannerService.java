package com.dismal.fireplayer.mediamanager;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import com.dismal.fireplayer.constant.Constants;
import com.dismal.fireplayer.provider.LocalMediaProviderContract;
import com.dismal.fireplayer.util.Utils;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Vector;

public class LocalMediaScannerService extends IntentService {
    public static final String EXTRA_DEVPATH = "extra_dev_path";
    public static final String EXTRA_SCANTYPE = "scan_type";
    private static final String TAG = "LocalMediaScannerService";
    /* access modifiers changed from: private */
    public static LocalMediaScanner mLocalMediaScanner;
    private ArrayList<String> deletedRecordersArray = new ArrayList<>();

    public LocalMediaScannerService() {
        super(TAG);
    }

    /* access modifiers changed from: private */
    public void scanFile(String path) {
        File f = new File(path);
        int hashCode = (String.valueOf(path) + f.length()).hashCode();
        ContentValues cv = new ContentValues();
        cv.put(LocalMediaProviderContract.PATH_COLUMN, path);
        cv.put(LocalMediaProviderContract.DIR_COLUMN, f.getParent());
        cv.put(LocalMediaProviderContract.NAME_COLUMN, f.getName());
        cv.put(LocalMediaProviderContract.FILESIZE_COLUMN, Long.valueOf(f.length()));
        cv.put(LocalMediaProviderContract.HASHCODE_COLUMN, Integer.valueOf(hashCode));
        cv.put(LocalMediaProviderContract.ACTIVE_VIDEO_COLUMN, 1);
        cv.put(LocalMediaProviderContract.DATE_MODIFIED_COLUMN, f.lastModified() / 1000);
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        try {
            metadataRetriever.setDataSource(Utils.convertMediaPath(path));
            String duration = metadataRetriever.extractMetadata(9);
            String vw = metadataRetriever.extractMetadata(18);
            String vh = metadataRetriever.extractMetadata(19);
            cv.put(LocalMediaProviderContract.MEDIAINFO_GETTED_COLUMN, 1);
            if (duration != null) {
                cv.put(LocalMediaProviderContract.DURATION_COLUMN, Integer.valueOf(duration));
            }
            if (vw != null) {
                cv.put(LocalMediaProviderContract.WIDTH_COLUMN, Integer.valueOf(vw));
            }
            if (vh != null) {
                cv.put(LocalMediaProviderContract.HEIGHT_COLUMN, Integer.valueOf(vh));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            metadataRetriever.release();
        }
        getContentResolver().insert(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, cv);
    }

    /* access modifiers changed from: private */
    public void scanDev(String devPath, Vector<ContentValues> addedRecorderValues, String escapePath) {
        SparseArray<String> mOriginalRecordersArray = new SparseArray<>();
        String[] projection = { LocalMediaProviderContract.ROW_ID, LocalMediaProviderContract.HASHCODE_COLUMN };
        Cursor cursor = getContentResolver().query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, projection,
                "dir like ? ", new String[] { String.valueOf(devPath) + "%" }, (String) null);
        if (cursor != null && cursor.moveToFirst()) {
            int tmpCount = cursor.getCount();
            for (int i = 0; i < tmpCount; i++) {
                mOriginalRecordersArray.put(
                        cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.HASHCODE_COLUMN)),
                        (String) null);
                cursor.moveToNext();
            }
            cursor.close();
        }
        mLocalMediaScanner.scan(devPath, mOriginalRecordersArray, addedRecorderValues, escapePath);
        SparseArray<String> exsitRecordersArray = mLocalMediaScanner.getExsitRecordersArray();
        this.deletedRecordersArray.clear();
        int tmpCount2 = mOriginalRecordersArray.size();
        for (int i2 = 0; i2 < tmpCount2; i2++) {
            int key = mOriginalRecordersArray.keyAt(i2);
            if (exsitRecordersArray.indexOfKey(key) < 0) {
                this.deletedRecordersArray.add(String.valueOf(key));
            }
        }
        for (int i3 = 0; i3 < this.deletedRecordersArray.size(); i3++) {
            getContentResolver().delete(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, "hashcode = ? ",
                    new String[] { this.deletedRecordersArray.get(i3) });
        }
    }

    /* access modifiers changed from: package-private */
    public void extractOtherMediaInfos(Vector<ContentValues> addedRecorderValues) {
        int size = addedRecorderValues.size();
        Log.v(TAG, "extractOtherMediaInfos size=" + size);
        for (int i = 0; i < size; i++) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            try {
                ContentValues cv = addedRecorderValues.get(i);
                String path = cv.getAsString(LocalMediaProviderContract.PATH_COLUMN);
                Log.v(TAG, "getMediaInfo path = " + path);
                metadataRetriever.setDataSource(Utils.convertMediaPath(path));
                String duration = metadataRetriever.extractMetadata(9);
                String vw = metadataRetriever.extractMetadata(18);
                String vh = metadataRetriever.extractMetadata(19);
                cv.put(LocalMediaProviderContract.MEDIAINFO_GETTED_COLUMN, 1);
                if (duration != null) {
                    cv.put(LocalMediaProviderContract.DURATION_COLUMN, Integer.valueOf(duration));
                }
                if (vw != null) {
                    cv.put(LocalMediaProviderContract.WIDTH_COLUMN, Integer.valueOf(vw));
                }
                if (vh != null) {
                    cv.put(LocalMediaProviderContract.HEIGHT_COLUMN, Integer.valueOf(vh));
                }
                metadataRetriever.release();
            } catch (Exception e) {
                e.printStackTrace();
                metadataRetriever.release();
            } catch (Throwable th) {
                metadataRetriever.release();
                throw th;
            }
        }
    }

    /* access modifiers changed from: private */
    public void sendMediaScanBroadcast(String action, Uri uri) {
        Intent localIntent = new Intent();
        localIntent.setAction(action);
        localIntent.addCategory("android.intent.category.DEFAULT");
        sendBroadcast(localIntent);
    }

    /* access modifiers changed from: protected */
    public void onHandleIntent(Intent intent) {
        new Thread(new MediaScanRunnable(intent)).run();
    }

    private class MediaScanRunnable implements Runnable {
        private Intent intent;

        MediaScanRunnable(Intent i) {
            this.intent = i;
        }

        public void run() {
            String action = this.intent.getAction();
            String devPath = this.intent.getStringExtra(LocalMediaScannerService.EXTRA_DEVPATH);
            if (Boolean.valueOf(this.intent.getBooleanExtra(LocalMediaScannerService.EXTRA_SCANTYPE, false))
                    .booleanValue() && new File(devPath).isFile()) {
                if (devPath.toLowerCase(Locale.US).matches(
                        "^.*?\\.(avi|wmv|rmvb|mkv|m4v|mov|mpg|rm|flv|pmp|vob|asf|3gp|mpeg|ram|divx|m4p|m4b|mp4|f4v|3gpp|3g2|3gpp2|webm|ts|tp|m2ts|3dv|3dm|m1v)$")) {
                    LocalMediaScannerService.this.scanFile(devPath);
                } else {
                    return;
                }
            }
            Log.v(LocalMediaScannerService.TAG, ">>>> onHandleIntent" + action + " devPath = " + devPath);
            Vector<ContentValues> addedRecorderValues = new Vector<>(200);
            LocalMediaScannerService.mLocalMediaScanner = LocalMediaScanner
                    .getInstance(LocalMediaScannerService.this.getApplicationContext());
            Uri uri = Uri.parse("file://" + devPath);
            if (!action.equals(Constants.ACTION_MEDIASCAN)) {
                LocalMediaScannerService.this.sendMediaScanBroadcast(Constants.BROADCAST_ACTION_MEDIASCAN_START, uri);
            }
            if (action.equals("android.intent.action.MEDIA_UNMOUNTED")) {
                ContentValues cv = new ContentValues();
                cv.put(LocalMediaProviderContract.ACTIVE_VIDEO_COLUMN, 0);
                LocalMediaScannerService.this.getContentResolver().update(
                        LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, cv, "dir like ? ",
                        new String[] { String.valueOf(devPath) + "%" });
            } else if (action.equals(MediaScannerReceiver.INTENT_ACTION_DELETE_FILE)) {
                ContentValues cv2 = new ContentValues();
                cv2.put(LocalMediaProviderContract.ACTIVE_VIDEO_COLUMN, 0);
                LocalMediaScannerService.this.getContentResolver().update(
                        LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, cv2, "dir = ? ",
                        new String[] { devPath });
                LocalMediaScannerService.this.getContentResolver().delete(
                        LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, "path = ?", new String[] { devPath });
            } else {
                ContentValues cv3 = new ContentValues();
                cv3.put(LocalMediaProviderContract.ACTIVE_VIDEO_COLUMN, 1);
                LocalMediaScannerService.this.getContentResolver().update(
                        LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, cv3, "dir like ? ",
                        new String[] { "/%" });
                // Use StorageService to get all mounted storage volumes dynamically
                StorageService storageService = StorageService.getInstance(LocalMediaScannerService.this);
                ArrayList<String> mountedRoots = storageService.getMountedRootList();
                if (mountedRoots != null && mountedRoots.size() > 0) {
                    // Scan all mounted storage volumes
                    for (String rootPath : mountedRoots) {
                        File rootFile = new File(rootPath);
                        if (rootFile.exists() && rootFile.canRead()) {
                            LocalMediaScannerService.this.scanDev(rootPath, addedRecorderValues, (String) null);
                        }
                    }
                } else {
                    // Fallback to standard external storage
                    String externalStorage = Environment.getExternalStorageDirectory().getPath();
                    if (externalStorage != null) {
                        LocalMediaScannerService.this.scanDev(externalStorage, addedRecorderValues, (String) null);
                    }
                    // Try common storage paths
                    String[] commonPaths = { "/storage/sdcard1", "/storage/usbhost1", "/storage/extSdCard" };
                    for (String path : commonPaths) {
                        File pathFile = new File(path);
                        if (pathFile.exists() && pathFile.canRead()) {
                            LocalMediaScannerService.this.scanDev(path, addedRecorderValues, (String) null);
                        }
                    }
                }
                LocalMediaScannerService.this.getContentResolver().bulkInsert(
                        LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI,
                        (ContentValues[]) addedRecorderValues.toArray(new ContentValues[addedRecorderValues.size()]));
            }
            LocalMediaScannerService.this.sendMediaScanBroadcast(Constants.BROADCAST_ACTION_MEDIASCAN_FINISHED, uri);
        }
    }
}
