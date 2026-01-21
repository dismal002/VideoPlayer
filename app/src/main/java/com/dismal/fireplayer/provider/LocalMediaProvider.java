package com.dismal.fireplayer.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

public class LocalMediaProvider extends ContentProvider {
    private static final String BIGINT_TYPE = "BIGINT";
    private static final String CREATE_MEDIAINFO_TABLE_SQL = "CREATE TABLE IF NOT EXISTS mediadata ( _id INTEGER PRIMARY KEY ,hashcode INTEGER ,path TEXT ,dir TEXT ,name TEXT ,filesize BIGINT ,type INTEGER ,duration INTEGER ,width INTEGER ,height INTEGER ,bookmark TEXT ,activevideo INTEGER ,mediainfogetted INTEGER ,favorite INTEGER ,latestplaytime INTEGER ,date_modified INTEGER)";
    public static final int CUSTOM_UPDATE0_INSERT = 4;
    public static final int FOLDER_DISTINCT_QUERY = 3;
    public static final int IMAGE_URL_QUERY = 1;
    private static final String INTEGER_TYPE = "INTEGER";
    public static final int INVALID_URI = -1;
    private static final String PRIMARY_KEY_TYPE = "INTEGER PRIMARY KEY";
    public static final String TAG = "LocalMediaProvider";
    private static final String TEXT_TYPE = "TEXT";
    public static final int URL_DATE_QUERY = 2;
    private static final SparseArray<String> sMimeTypes = new SparseArray<>();
    private static final UriMatcher sUriMatcher = new UriMatcher(0);
    private SQLiteOpenHelper mHelper;

    static {
        sUriMatcher.addURI(LocalMediaProviderContract.AUTHORITY, LocalMediaProviderContract.MEDIAINFO_TABLE_NAME, 1);
        sUriMatcher.addURI(LocalMediaProviderContract.AUTHORITY, LocalMediaProviderContract.FOLDER_PATH_NAME, 3);
        sUriMatcher.addURI(LocalMediaProviderContract.AUTHORITY, LocalMediaProviderContract.CUSTOM_UPDATE_NAME, 4);
        sMimeTypes.put(1, "vnd.android.cursor.dir/vnd.com.dismal.fireplayer.mediadata");
    }

    public void close() {
        this.mHelper.close();
    }

    private class DataProviderHelper extends SQLiteOpenHelper {
        DataProviderHelper(Context context) {
            super(context, LocalMediaProviderContract.DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 5);
        }

        private void dropTables(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS mediadata");
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(LocalMediaProvider.CREATE_MEDIAINFO_TABLE_SQL);
            Log.d(LocalMediaProvider.TAG, "CREATE_MEDIAINFO_TABLE_SQL");
        }

        public void onUpgrade(SQLiteDatabase db, int version1, int version2) {
            Log.w(DataProviderHelper.class.getName(), "Upgrading database from version " + version1 + " to " + version2
                    + ", which will destroy all the existing data");
            dropTables(db);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int version1, int version2) {
            Log.w(DataProviderHelper.class.getName(), "Downgrading database from version " + version1 + " to "
                    + version2 + ", which will destroy all the existing data");
            dropTables(db);
            onCreate(db);
        }
    }

    public boolean onCreate() {
        this.mHelper = new DataProviderHelper(getContext());
        Log.d(TAG, "LocalMediaProvider onCreate");
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = this.mHelper.getReadableDatabase();
        switch (sUriMatcher.match(uri)) {
            case -1:
                throw new IllegalArgumentException("Query -- Invalid URI:" + uri);
            case 1:
                Cursor returnCursor = db.query(LocalMediaProviderContract.MEDIAINFO_TABLE_NAME, projection, selection,
                        selectionArgs, (String) null, (String) null, sortOrder);
                returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return returnCursor;
            case 3:
                return db.query(true, LocalMediaProviderContract.MEDIAINFO_TABLE_NAME, projection, selection,
                        selectionArgs, LocalMediaProviderContract.DIR_COLUMN, (String) null, "UPPER(dir)",
                        (String) null);
            default:
                return null;
        }
    }

    public String getType(Uri uri) {
        return sMimeTypes.get(sUriMatcher.match(uri));
    }

    public Uri insert(Uri uri, ContentValues values) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                return Uri.withAppendedPath(uri, Long.toString(this.mHelper.getWritableDatabase()
                        .insert(LocalMediaProviderContract.MEDIAINFO_TABLE_NAME, (String) null, values)));
            default:
                throw new IllegalArgumentException("Insert: Invalid URI" + uri);
        }
    }

    public int bulkInsert(Uri uri, ContentValues[] insertValuesArray) {
        switch (sUriMatcher.match(uri)) {
            case -1:
                throw new IllegalArgumentException("Bulk insert -- Invalid URI:" + uri);
            case 1:
                SQLiteDatabase localSQLiteDatabase = this.mHelper.getWritableDatabase();
                localSQLiteDatabase.beginTransaction();
                for (ContentValues insert : insertValuesArray) {
                    localSQLiteDatabase.insert(LocalMediaProviderContract.MEDIAINFO_TABLE_NAME, (String) null, insert);
                }
                localSQLiteDatabase.setTransactionSuccessful();
                localSQLiteDatabase.endTransaction();
                getContext().getContentResolver().notifyChange(uri, (ContentObserver) null);
                return insertValuesArray.length;
            case 2:
                return super.bulkInsert(uri, insertValuesArray);
            case 4:
                SQLiteDatabase localSQLiteDatabase2 = this.mHelper.getWritableDatabase();
                localSQLiteDatabase2.beginTransaction();
                for (ContentValues cv : insertValuesArray) {
                    String[] strArr = { cv.getAsString(LocalMediaProviderContract.HASHCODE_COLUMN) };
                    Log.v(TAG,
                            "debug delete row ="
                                    + ((long) localSQLiteDatabase2.delete(
                                            LocalMediaProviderContract.MEDIAINFO_TABLE_NAME, "hashcode =? ", strArr))
                                    + " name:" + cv.getAsString(LocalMediaProviderContract.PATH_COLUMN));
                    Log.v(TAG, "debug insert row =" + localSQLiteDatabase2
                            .insert(LocalMediaProviderContract.MEDIAINFO_TABLE_NAME, (String) null, cv));
                }
                localSQLiteDatabase2.setTransactionSuccessful();
                localSQLiteDatabase2.endTransaction();
                getContext().getContentResolver().notifyChange(uri, (ContentObserver) null);
                return insertValuesArray.length;
            default:
                return -1;
        }
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case 1:
                if (selectionArgs.length <= 2) {
                    return db.delete(LocalMediaProviderContract.MEDIAINFO_TABLE_NAME, selection, selectionArgs);
                }
                db.beginTransaction();
                int num = selectionArgs.length;
                for (int i = 0; i < num; i++) {
                    db.delete(LocalMediaProviderContract.MEDIAINFO_TABLE_NAME, selection,
                            new String[] { selectionArgs[i] });
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                return num;
            default:
                return 0;
        }
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                int rows = this.mHelper.getWritableDatabase().update(LocalMediaProviderContract.MEDIAINFO_TABLE_NAME,
                        values, selection, selectionArgs);
                if (rows == 0) {
                    return 0;
                }
                getContext().getContentResolver().notifyChange(uri, (ContentObserver) null);
                return rows;
            default:
                return -1;
        }
    }
}
