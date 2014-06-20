package com.klinker.android.twitter.data.sq_lite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by luke on 6/19/14.
 */
public class HashtagSQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_HASHTAGS = "hashtags";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TAG = "name";

    private static final String DATABASE_NAME = "hashtags.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_HASHTAGS + "(" + COLUMN_ID
            + " integer primary key, " +  COLUMN_TAG
            + " text hashtag); ";

    public HashtagSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(HomeSQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HASHTAGS);
        onCreate(db);
    }
}