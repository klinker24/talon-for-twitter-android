package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import twitter4j.User;

public class FavoriteUsersDataSource {

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

        database.insert(FavoriteUsersSQLiteHelper.TABLE_HOME, null, values);
    }

    public void deleteUser(long userId) {
        long id = userId;

        database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME, FavoriteUsersSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public void deleteAllUsers(int account) {
        database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME,
                FavoriteUsersSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
    }

    public Cursor getCursor(int account) {
        Cursor cursor = database.query(FavoriteUsersSQLiteHelper.TABLE_HOME,
                allColumns, FavoriteUsersSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null, null, null, null);

        return cursor;
    }
}
