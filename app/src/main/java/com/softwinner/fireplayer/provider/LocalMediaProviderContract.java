package com.softwinner.fireplayer.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public final class LocalMediaProviderContract implements BaseColumns {
    public static final String ACTIVE_VIDEO_COLUMN = "activevideo";
    public static final String AUTHORITY = "com.softwinner.fireplayer";
    public static final String BOOKMARK_COLUMN = "bookmark";
    public static final Uri CONTENT_URI = Uri.parse("content://com.softwinner.fireplayer");
    public static final String CUSTOM_UPDATE_NAME = "custom_update";
    public static final Uri CUSTOM_UPDATE_CONTENTURI = Uri.withAppendedPath(CONTENT_URI, CUSTOM_UPDATE_NAME);
    public static final String DATABASE_NAME = "FourKPlayerDB";
    public static final int DATABASE_VERSION = 5;
    public static final String DIR_COLUMN = "dir";
    public static final String DURATION_COLUMN = "duration";
    public static final String DATE_MODIFIED_COLUMN = "date_modified";
    public static final String FAVORITE_COLUMN = "favorite";
    public static final String FILESIZE_COLUMN = "filesize";
    public static final String FOLDER_PATH_NAME = "folder";
    public static final Uri FOLDER_PATH_CONTENTURI = Uri.withAppendedPath(CONTENT_URI, FOLDER_PATH_NAME);
    public static final String HASHCODE_COLUMN = "hashcode";
    public static final String HEIGHT_COLUMN = "height";
    public static final String LATEST_PLAY_COLUMN = "latestplaytime";
    public static final String MEDIAINFO_GETTED_COLUMN = "mediainfogetted";
    public static final String MEDIAINFO_TABLE_NAME = "mediadata";
    public static final Uri MEDIAINFO_TABLE_CONTENTURI = Uri.withAppendedPath(CONTENT_URI, MEDIAINFO_TABLE_NAME);
    public static final String MIME_TYPE_ROWS = "vnd.android.cursor.dir/vnd.com.example.android.threadsample";
    public static final String MIME_TYPE_SINGLE_ROW = "vnd.android.cursor.item/vnd.com.example.android.threadsample";
    public static final String NAME_COLUMN = "name";
    public static final String PATH_COLUMN = "path";
    public static final String ROW_ID = "_id";
    public static final String SCHEME = "content";
    public static final String TYPE_COLUMN = "type";
    public static final String WIDTH_COLUMN = "width";

    private LocalMediaProviderContract() {
    }
}
