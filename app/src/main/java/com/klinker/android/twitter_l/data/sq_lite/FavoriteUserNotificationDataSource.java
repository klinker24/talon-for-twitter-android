package com.klinker.android.twitter_l.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class FavoriteUserNotificationDataSource {

    // provides access to the database
    public static FavoriteUserNotificationDataSource dataSource = null;

    /**
     * This is used so that we don't have to open and close the database on different threads or fragments
     * every time. This will facilitate it between all of them to avoid Illegal State Exceptions.
     */
    public static FavoriteUserNotificationDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        if (dataSource == null ||
                dataSource.getDatabase() == null ||
                !dataSource.getDatabase().isOpen()) {

            dataSource = new FavoriteUserNotificationDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    private SQLiteDatabase database;
    private FavoriteUserNotificationSQLiteHelper dbHelper;
    public String[] allColumns = { FavoriteUserNotificationSQLiteHelper.COLUMN_ID, FavoriteUserNotificationSQLiteHelper.COLUMN_TWEET_ID };

    public FavoriteUserNotificationDataSource(Context context) {
        dbHelper = new FavoriteUserNotificationSQLiteHelper(context);
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

    public FavoriteUserNotificationSQLiteHelper getHelper() {
        return dbHelper;
    }

    public synchronized void storeShowedNotification(long tweetId) {
        ContentValues values = new ContentValues();

        values.put(FavoriteUserNotificationSQLiteHelper.COLUMN_TWEET_ID, tweetId);

        if (database == null || !database.isOpen()) {
            open();
        }

        try {
            database.insert(FavoriteUserNotificationSQLiteHelper.TABLE, null, values);
        } catch (Exception e) {

        }
    }

    public synchronized boolean hasShownNotification(long tweetId) {

        Cursor cursor;
        try {
            cursor = database.query(FavoriteUserNotificationSQLiteHelper.TABLE,
                    allColumns,
                    FavoriteUserNotificationSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId,
                    null, null, null, null);
        } catch (Exception e) {
            open();
            cursor = database.query(FavoriteUserNotificationSQLiteHelper.TABLE,
                    allColumns,
                    FavoriteUserNotificationSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId,
                    null, null, null, null);
        }

        try {
            if (cursor.moveToFirst()) {
                return cursor.getCount() > 0;
            } else {
                return false;
            }
        } catch (Throwable t) {
            return true;
        }
    }

    public synchronized void trimDatabase(int trimSize) {
        Cursor cursor = database.query(FavoriteUserNotificationSQLiteHelper.TABLE,
                allColumns, null, null, null, null, null);
        if (cursor.getCount() > trimSize) {
            if (cursor.moveToPosition(cursor.getCount() - trimSize)) {
                try {
                    database.delete(
                            HomeSQLiteHelper.TABLE_HOME,
                            FavoriteUserNotificationSQLiteHelper.COLUMN_ID + " < " +
                                    cursor.getLong(cursor.getColumnIndex(FavoriteUserNotificationSQLiteHelper.COLUMN_ID)),
                            null);
                } catch (Exception e) {
                    open();
                    database.delete(
                            HomeSQLiteHelper.TABLE_HOME,
                            FavoriteUserNotificationSQLiteHelper.COLUMN_ID + " < " +
                                    cursor.getLong(cursor.getColumnIndex(FavoriteUserNotificationSQLiteHelper.COLUMN_ID)),
                            null);
                }
            }
        }

        try {
            cursor.close();
        } catch (Exception e) {

        }
    }
}

