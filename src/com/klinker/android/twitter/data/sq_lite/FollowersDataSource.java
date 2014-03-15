package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import twitter4j.User;

public class FollowersDataSource {

    // provides access to the database
    public static FollowersDataSource dataSource = null;

    /*

    This is used so that we don't have to open and close the database on different threads or fragments
    every time. This will facilitate it between all of them to avoid Illegal State Exceptions.

     */
    public static FollowersDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        if (dataSource == null ||
                dataSource.getDatabase() == null ||
                !dataSource.getDatabase().isOpen()) {

            dataSource = new FollowersDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    private SQLiteDatabase database;
    private FollowersSQLiteHelper dbHelper;
    public String[] allColumns = { FollowersSQLiteHelper.COLUMN_ID, FollowersSQLiteHelper.COLUMN_ACCOUNT,
            FollowersSQLiteHelper.COLUMN_NAME, FollowersSQLiteHelper.COLUMN_PRO_PIC,
            FollowersSQLiteHelper.COLUMN_SCREEN_NAME };

    public FollowersDataSource(Context context) {
        dbHelper = new FollowersSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
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

    public FollowersSQLiteHelper getHelper() {
        return dbHelper;
    }

    public synchronized void createUser(User user, int account) {
        ContentValues values = new ContentValues();

        long id = user.getId();
        String screenName = user.getScreenName();
        String name = user.getName();
        String proPicUrl = user.getBiggerProfileImageURL();

        values.put(FollowersSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(FollowersSQLiteHelper.COLUMN_ID, id);
        values.put(FollowersSQLiteHelper.COLUMN_NAME, name);
        values.put(FollowersSQLiteHelper.COLUMN_PRO_PIC, proPicUrl);
        values.put(FollowersSQLiteHelper.COLUMN_SCREEN_NAME, screenName);

        if (database == null || !database.isOpen()) {
            open();
        }

        try {
            database.insert(FollowersSQLiteHelper.TABLE_HOME, null, values);
        } catch (Exception e) {
            // already exist. primary key must be unique
        }
    }

    public synchronized void deleteUser(long userId) {
        long id = userId;

        try {
            database.delete(FollowersSQLiteHelper.TABLE_HOME, FollowersSQLiteHelper.COLUMN_ID
                    + " = " + id, null);
        } catch (Exception e) {
            open();
            database.delete(FollowersSQLiteHelper.TABLE_HOME, FollowersSQLiteHelper.COLUMN_ID
                    + " = " + id, null);
        }
    }

    public synchronized void deleteAllUsers(int account) {

        try {
            database.delete(FollowersSQLiteHelper.TABLE_HOME,
                    FollowersSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
        } catch (Exception e) {
            open();
            database.delete(FollowersSQLiteHelper.TABLE_HOME,
                    FollowersSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
        }
    }

    public synchronized Cursor getCursor(int account, String name) {

        Cursor cursor;
        try {
            cursor = database.query(FollowersSQLiteHelper.TABLE_HOME,
                    allColumns, FollowersSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " +
                    FollowersSQLiteHelper.COLUMN_NAME + " LIKE '%" + name + "%'" + " OR " +
                    FollowersSQLiteHelper.COLUMN_SCREEN_NAME + " LIKE '%" + name + "%'",
                    null,
                    null,
                    null,
                    FollowersSQLiteHelper.COLUMN_NAME + " DESC");
        } catch (Exception e) {
            open();
            cursor = database.query(FollowersSQLiteHelper.TABLE_HOME,
                    allColumns, FollowersSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " +
                    FollowersSQLiteHelper.COLUMN_NAME + " LIKE '%" + name + "%'" + " OR " +
                    FollowersSQLiteHelper.COLUMN_SCREEN_NAME + " LIKE '%" + name + "%'",
                    null,
                    null,
                    null,
                    FollowersSQLiteHelper.COLUMN_NAME + " DESC");
        }

        return cursor;
    }
}
