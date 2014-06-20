package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;


public class HashtagDataSource {

    // provides access to the database
    public static HashtagDataSource dataSource = null;

    /**
     * This is used so that we don't have to open and close the database on different threads or fragments
     * every time. This will facilitate it between all of them to avoid Illegal State Exceptions.
     */
    public static HashtagDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        if (dataSource == null ||
                dataSource.getDatabase() == null ||
                !dataSource.getDatabase().isOpen()) {

            dataSource = new HashtagDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    private SQLiteDatabase database;
    private HashtagSQLiteHelper dbHelper;
    public String[] allColumns = { HashtagSQLiteHelper.COLUMN_ID, HashtagSQLiteHelper.COLUMN_TAG };

    public HashtagDataSource(Context context) {
        dbHelper = new HashtagSQLiteHelper(context);
    }

    public void open() throws SQLException {

        try {
            database = dbHelper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            close();
        }
    }

    public void close() {
        try {
            dbHelper.close();
        } catch (Exception e) {

        }

        database = null;
        dataSource = null;
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public HashtagSQLiteHelper getHelper() {
        return dbHelper;
    }

    public synchronized void createTag(String tag) {
        ContentValues values = new ContentValues();

        values.put(HashtagSQLiteHelper.COLUMN_TAG, tag);

        if (database == null || !database.isOpen()) {
            open();
        }

        try {
            database.insert(HashtagSQLiteHelper.TABLE_HASHTAGS, null, values);
        } catch (Exception e) {
            // already exist. primary key must be unique
        }
    }

    public synchronized void deleteTag(String tag) {
        try {
            database.delete(HashtagSQLiteHelper.TABLE_HASHTAGS, HashtagSQLiteHelper.COLUMN_TAG
                    + " = " + tag, null);
        } catch (Exception e) {
            open();
            database.delete(HashtagSQLiteHelper.TABLE_HASHTAGS, HashtagSQLiteHelper.COLUMN_TAG
                    + " = " + tag, null);
        }
    }

    public synchronized void deleteAllTags(int account) {

        try {
            database.delete(HashtagSQLiteHelper.TABLE_HASHTAGS, null, null);
        } catch (Exception e) {
            open();
            database.delete(HashtagSQLiteHelper.TABLE_HASHTAGS, null, null);
        }
    }

    public synchronized Cursor getCursor(String tag) {

        Cursor cursor;
        try {
            cursor = database.query(HashtagSQLiteHelper.TABLE_HASHTAGS,
                    allColumns,
                    HashtagSQLiteHelper.COLUMN_TAG + " LIKE '%" + tag + "%'",
                    null, null, null, null);
        } catch (Exception e) {
            open();
            cursor = database.query(HashtagSQLiteHelper.TABLE_HASHTAGS,
                    allColumns,
                    HashtagSQLiteHelper.COLUMN_TAG + " LIKE '%" + tag + "%'",
                    null, null, null, null);
        }

        return cursor;
    }
}

