package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import twitter4j.User;

public class FavoriteUsersDataSource {

    // provides access to the database
    public static FavoriteUsersDataSource dataSource = null;

    /*

    This is used so that we don't have to open and close the database on different threads or fragments
    every time. This will facilitate it between all of them to avoid Illegal State Exceptions.

     */
    public static FavoriteUsersDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        try {
            if (dataSource == null || !dataSource.getDatabase().isOpen()) {
                dataSource = new FavoriteUsersDataSource(context); // create the database
                dataSource.open(); // open the database
            }
        } catch (Exception e) {
            dataSource = new FavoriteUsersDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    private SQLiteDatabase database;
    private FavoriteUsersSQLiteHelper dbHelper;
    public String[] allColumns = { FavoriteUsersSQLiteHelper.COLUMN_ID, FavoriteUsersSQLiteHelper.COLUMN_ACCOUNT,
            FavoriteUsersSQLiteHelper.COLUMN_NAME, FavoriteUsersSQLiteHelper.COLUMN_PRO_PIC,
            FavoriteUsersSQLiteHelper.COLUMN_SCREEN_NAME };

    public FavoriteUsersDataSource(Context context) {
        dbHelper = new FavoriteUsersSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public FavoriteUsersSQLiteHelper getHelper() {
        return dbHelper;
    }

    public void createUser(User user, int account) {
        ContentValues values = new ContentValues();

        long id = user.getId();
        String screenName = user.getScreenName();
        String name = user.getName();
        String proPicUrl = user.getBiggerProfileImageURL();

        values.put(FavoriteUsersSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_ID, id);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_NAME, name);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_PRO_PIC, proPicUrl);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_SCREEN_NAME, screenName);

        if (database == null) {
            open();
        } else if (!database.isOpen() || database.isDbLockedByOtherThreads()) {
            open();
        }

        database.insert(FavoriteUsersSQLiteHelper.TABLE_HOME, null, values);
    }

    public void deleteUser(long userId) {
        long id = userId;

        if (database == null) {
            open();
        } else if (!database.isOpen() || database.isDbLockedByOtherThreads()) {
            open();
        }

        database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME, FavoriteUsersSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public void deleteAllUsers(int account) {
        if (database == null) {
            open();
        } else if (!database.isOpen() || database.isDbLockedByOtherThreads()) {
            open();
        }

        database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME,
                FavoriteUsersSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
    }

    public Cursor getCursor(int account) {
        if (database == null) {
            open();
        } else if (!database.isOpen() || database.isDbLockedByOtherThreads()) {
            open();
        }

        Cursor cursor = database.query(FavoriteUsersSQLiteHelper.TABLE_HOME,
                allColumns, FavoriteUsersSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null, null, null, null);

        return cursor;
    }

    public boolean isFavUser(int account, String username) {
        Cursor check = getCursor(account);

        if (check.moveToFirst()) {
            do {
                if(check.getString(check.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_SCREEN_NAME)).equals(username)) {
                    check.close();
                    return true;
                }
            } while (check.moveToNext());
        }

        check.close();
        return false;
    }
}
