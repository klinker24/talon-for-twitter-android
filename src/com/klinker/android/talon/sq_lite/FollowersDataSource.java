package com.klinker.android.talon.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import twitter4j.User;

public class FollowersDataSource {

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
        dbHelper.close();
    }

    public void createUser(User user, int account) {
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

        database.insert(FollowersSQLiteHelper.TABLE_HOME, null, values);
    }

    public void deleteUser(long userId) {
        long id = userId;

        database.delete(FollowersSQLiteHelper.TABLE_HOME, FollowersSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public void deleteAllUsers(int account) {
        database.delete(FollowersSQLiteHelper.TABLE_HOME,
                FollowersSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
    }

    public Cursor getCursor(int account, String name) {
        Cursor cursor = database.query(FollowersSQLiteHelper.TABLE_HOME,
                allColumns, FollowersSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " +
                    FollowersSQLiteHelper.COLUMN_NAME + " LIKE '%" + name + "%'" + " OR " +
                    FollowersSQLiteHelper.COLUMN_SCREEN_NAME + " LIKE '%" + name + "%'",
                null,
                null,
                null,
                FollowersSQLiteHelper.COLUMN_NAME + " DESC");

        return cursor;
    }
}
