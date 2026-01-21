package com.softwinner.fireplayer.videoplayerui;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import com.softwinner.fireplayer.provider.LocalMediaInfo;
import com.softwinner.fireplayer.provider.LocalMediaProviderContract;
import com.softwinner.fireplayer.ui.FourKApplication;
import com.softwinner.fireplayer.util.ThumbsWorker;
import java.util.ArrayList;

public class PlayListMediaInfoAdapter extends ThumbsWorker.ThumbsWorkAdapter {
    public static final int SORT_BY_ALPHA = 1;
    public static final int SORT_BY_FOLDER = 0;
    public static final int SORT_BY_SIZE = 2;
    private static final String TAG = "LocalMediaInfoAdapter";
    private static PlayListMediaInfoAdapter mInstance = null;
    private ContentResolver mContentResolver;
    private Context mContext;
    private ArrayList<LocalMediaInfo> mMediaInfoArray = new ArrayList<>();
    private int sortMethod = 1;

    public static PlayListMediaInfoAdapter getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new PlayListMediaInfoAdapter(context);
        }
        return mInstance;
    }

    public PlayListMediaInfoAdapter(Context context) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
    }

    public int loadPlayListMediaInfo(String selection, String[] selectionArgs) {
        String[] projection = {LocalMediaProviderContract.ROW_ID, LocalMediaProviderContract.DIR_COLUMN, LocalMediaProviderContract.NAME_COLUMN, LocalMediaProviderContract.PATH_COLUMN, LocalMediaProviderContract.DURATION_COLUMN, LocalMediaProviderContract.BOOKMARK_COLUMN, LocalMediaProviderContract.MEDIAINFO_GETTED_COLUMN, LocalMediaProviderContract.HASHCODE_COLUMN};
        String sortSentence = null;
        int itemSize = 0;
        this.mMediaInfoArray.clear();
        switch (this.sortMethod & Integer.MAX_VALUE) {
            case 1:
                sortSentence = LocalMediaProviderContract.NAME_COLUMN;
                break;
            case 2:
                sortSentence = LocalMediaProviderContract.FILESIZE_COLUMN;
                break;
        }
        if ((this.sortMethod & Integer.MIN_VALUE) != 0) {
            sortSentence = String.valueOf(sortSentence) + " desc";
        }
        Cursor cursor = this.mContentResolver.query(LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, projection, "(activevideo=1) AND (" + selection + ")", selectionArgs, sortSentence);
        if (cursor != null && cursor.moveToFirst()) {
            itemSize = cursor.getCount();
            for (int i = 0; i < itemSize; i++) {
                LocalMediaInfo mediaInfo = new LocalMediaInfo();
                mediaInfo.mNum = i;
                mediaInfo.mName = cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.NAME_COLUMN));
                mediaInfo.mKey = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.HASHCODE_COLUMN));
                mediaInfo.mDuration = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.DURATION_COLUMN));
                mediaInfo.mBookmark = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.BOOKMARK_COLUMN));
                mediaInfo.mPath = cursor.getString(cursor.getColumnIndex(LocalMediaProviderContract.PATH_COLUMN));
                mediaInfo.mMediaInfoGetted = cursor.getInt(cursor.getColumnIndex(LocalMediaProviderContract.MEDIAINFO_GETTED_COLUMN));
                this.mMediaInfoArray.add(mediaInfo);
                cursor.moveToNext();
            }
            cursor.close();
        }
        FourKApplication.setPlayList(this.mMediaInfoArray);
        return itemSize;
    }

    public void clearPlayList() {
        this.mMediaInfoArray.clear();
    }

    public Object getItem(int num) {
        return this.mMediaInfoArray.get(num);
    }

    public int getSize() {
        return this.mMediaInfoArray.size();
    }

    public void setSortFlag(int mSortFlag) {
        if ((this.sortMethod & Integer.MAX_VALUE) == mSortFlag) {
            this.sortMethod ^= Integer.MIN_VALUE;
        } else {
            this.sortMethod = mSortFlag;
        }
    }
}
