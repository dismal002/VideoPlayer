package com.softwinner.fireplayer.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseIntArray;
import com.softwinner.fireplayer.constant.Constants;
import com.softwinner.fireplayer.ui.AppConfig;
import com.softwinner.fireplayer.util.ThumbsWorker;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LocalMediaInfoAdapter extends ThumbsWorker.ThumbsWorkAdapter {
    static final String EXTERNAL_VOLUME = "external";
    public static final int SORT_BY_ALPHA = 1;
    public static final int SORT_BY_FAVORITE = 4;
    public static final int SORT_BY_FOLDER = 0;
    public static final int SORT_BY_LATESTPLAY = 3;
    public static final int SORT_BY_SIZE = 2;
    public static final int SORT_BY_DATE = 5;
    private static final String TAG = "LocalMediaInfoAdapter";
    private static LocalMediaInfoAdapter mInstance = null;
    private ContentResolver mContentResolver;
    private Context mContext;
    private SparseIntArray mItemPositionMaps = new SparseIntArray();
    private ArrayList<LocalMediaInfo> mMeidaInfoArray = new ArrayList<>();

    public static LocalMediaInfoAdapter getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LocalMediaInfoAdapter(context);
        }
        return mInstance;
    }

    public LocalMediaInfoAdapter(Context context) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
    }

    public int loadFolderMedias(int sortMethod, String folder, boolean fuzzy) {
        if (folder.equals(Constants.MEDIA_ROOT_PATH)) {
            return loadAllMediaInfo(sortMethod, "dir like ?", new String[] { "/%" });
        } else if (folder.startsWith("/")) {
            if (!fuzzy) {
                return loadAllMediaInfo(sortMethod, "dir=?", new String[] { folder });
            }
            return loadAllMediaInfo(sortMethod, "dir like ?", new String[] { String.valueOf(folder) + "%" });
        } else if (folder.compareTo(Constants.PREDEF_CATEGORY[1]) == 0) {
            return loadAllMediaInfo(-2147483645, "latestplaytime>?", new String[] {
                    String.valueOf(AppConfig.getInstance((Context) null).getInt(AppConfig.RECORDED_PLAYTIME, 0)) });
        } else if (folder.compareTo(Constants.PREDEF_CATEGORY[2]) == 0) {
            return loadAllMediaInfo(sortMethod, "dir like ?", new String[] { "%DCIM/Camera%" });
        } else if (folder.compareTo(Constants.PREDEF_CATEGORY[3]) == 0) {
            return loadAllMediaInfo(-2147483644, "favorite>?", new String[] {
                    String.valueOf(AppConfig.getInstance((Context) null).getInt(AppConfig.RECORDED_FAVORITE, 0)) });
        } else {
            Log.w(TAG, "undefind folder");
            return 0;
        }
    }

    public int searchMedias(String query, boolean fuzzy) {
        if (fuzzy) {
            return loadAllMediaInfo(1, "name like ?", new String[] { "%" + query + "%" });
        }
        return loadAllMediaInfo(1, "name = ?", new String[] { query });
    }

    private void notifyMediaDeleteForScanner(String file) {
        String[] PROJECTION = { LocalMediaProviderContract.ROW_ID, "_data" };
        Uri[] mediatypes = { MediaStore.Video.Media.getContentUri(EXTERNAL_VOLUME) };
        ContentResolver cr = this.mContext.getContentResolver();
        for (int i = 0; i < mediatypes.length; i++) {
            Cursor c = cr.query(mediatypes[i], PROJECTION, (String) null, (String[]) null, (String) null);
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        try {
                            long rowId = c.getLong(0);
                            if (c.getString(1).equals(file)) {
                                Log.d(TAG, "delete row " + rowId + " in table " + mediatypes[i]);
                                cr.delete(ContentUris.withAppendedId(mediatypes[i], rowId), (String) null,
                                        (String[]) null);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "delete fail!");
                        }
                    }
                } finally {
                    c.close();
                }
            }
        }
    }

    public int deleteMedia(LocalMediaInfo mediaInfo) {
        File f = new File(mediaInfo.mPath);
        Log.v(TAG, "to delete:" + mediaInfo.mPath);
        if (!f.exists() || !f.delete()) {
            return 0;
        }
        Log.v(TAG, "delete ok");
        int num = this.mContentResolver.delete(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, "hashcode = ? ",
                new String[] { String.valueOf(mediaInfo.mKey) });
        notifyMediaDeleteForScanner(mediaInfo.mPath);
        return num;
    }

    public int loadAllMediaInfo(int sortMethod, String selection, String[] selectionArgs) {
        String[] projection = { LocalMediaProviderContract.ROW_ID, LocalMediaProviderContract.DIR_COLUMN,
                LocalMediaProviderContract.NAME_COLUMN, LocalMediaProviderContract.PATH_COLUMN,
                LocalMediaProviderContract.DURATION_COLUMN, LocalMediaProviderContract.BOOKMARK_COLUMN,
                LocalMediaProviderContract.FILESIZE_COLUMN, LocalMediaProviderContract.MEDIAINFO_GETTED_COLUMN,
                LocalMediaProviderContract.HASHCODE_COLUMN };
        String sortSentence = null;
        int itemSize = 0;
        this.mMeidaInfoArray.clear();
        this.mItemPositionMaps.clear();
        switch (Integer.MAX_VALUE & sortMethod) {
            case 1:
                sortSentence = LocalMediaProviderContract.NAME_COLUMN;
                break;
            case 2:
                sortSentence = LocalMediaProviderContract.FILESIZE_COLUMN;
                break;
            case 3:
                sortSentence = LocalMediaProviderContract.LATEST_PLAY_COLUMN;
                break;
            case 4:
                sortSentence = LocalMediaProviderContract.FAVORITE_COLUMN;
                break;
            case 5:
                sortSentence = LocalMediaProviderContract.DATE_MODIFIED_COLUMN;
                break;
        }
        if ((Integer.MIN_VALUE & sortMethod) != 0) {
            sortSentence = String.valueOf(sortSentence) + " desc";
        }
        Cursor cursor = this.mContentResolver.query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, projection,
                "(activevideo=1) AND (" + selection + ")", selectionArgs, sortSentence);
        if (cursor != null && cursor.moveToFirst()) {
            itemSize = cursor.getCount();
            for (int i = 0; i < itemSize; i++) {
                LocalMediaInfo mediaInfo = new LocalMediaInfo();
                mediaInfo.mNum = i;
                mediaInfo.mName = cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.NAME_COLUMN));
                mediaInfo.mKey = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.HASHCODE_COLUMN));
                mediaInfo.mDuration = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.DURATION_COLUMN));
                mediaInfo.mSize = cursor.getLong(cursor.getColumnIndex(LocalMediaProviderContract.FILESIZE_COLUMN));
                mediaInfo.mBookmark = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.BOOKMARK_COLUMN));
                mediaInfo.mPath = cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.PATH_COLUMN));
                mediaInfo.mMediaInfoGetted = cursor
                        .getInt(cursor.getColumnIndex(LocalMediaProviderContract.MEDIAINFO_GETTED_COLUMN));
                this.mMeidaInfoArray.add(mediaInfo);
                this.mItemPositionMaps.put(mediaInfo.mPath.hashCode(), mediaInfo.mNum);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return itemSize;
    }

    public int loadAllFolders(List<String> foldersArray) {
        int folderCount = 0;
        Cursor cursor = this.mContentResolver.query(LocalMediaProviderContract.FOLDER_PATH_CONTENTURI,
                new String[] { LocalMediaProviderContract.ROW_ID, LocalMediaProviderContract.DIR_COLUMN },
                "(activevideo=1)", (String[]) null, (String) null);
        if (cursor != null && cursor.moveToFirst()) {
            folderCount = cursor.getCount();
            for (int i = 0; i < folderCount; i++) {
                foldersArray.add(cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.DIR_COLUMN)));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return folderCount;
    }

    public void updateTempMediaInfoBookmark(String path, int bookmark) {
        LocalMediaInfo mediaInfo;
        if (this.mMeidaInfoArray.size() > 0
                && (mediaInfo = this.mMeidaInfoArray.get(this.mItemPositionMaps.get(path.hashCode()))) != null) {
            mediaInfo.mBookmark = bookmark;
        }
    }

    public LocalMediaInfo loadDetailMediaInfo(int hashcode) {
        Cursor cursor = this.mContentResolver.query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI,
                (String[]) null, "hashcode=?", new String[] { String.valueOf(hashcode) }, (String) null);
        if (cursor == null || !cursor.moveToFirst()) {
            return null;
        }
        LocalMediaInfo mediaInfo = new LocalMediaInfo();
        mediaInfo.mPath = cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.PATH_COLUMN));
        mediaInfo.mName = cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.NAME_COLUMN));
        mediaInfo.mKey = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.HASHCODE_COLUMN));
        mediaInfo.mDuration = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.DURATION_COLUMN));
        mediaInfo.mWidth = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.WIDTH_COLUMN));
        mediaInfo.mHeight = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.HEIGHT_COLUMN));
        mediaInfo.mSize = cursor.getLong(cursor.getColumnIndex(LocalMediaProviderContract.FILESIZE_COLUMN));
        cursor.close();
        return mediaInfo;
    }

    public Object getItem(int num) {
        return this.mMeidaInfoArray.get(num);
    }

    public int getSize() {
        return this.mMeidaInfoArray.size();
    }
}
