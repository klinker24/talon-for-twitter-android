package com.klinker.android.talon.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import twitter4j.User;

public class FavoriteUsersDataSource {

    private SQLiteDatabase database;
    private FavoriteUsersSQLiteHelper dbHelper;
    public String[] allColumns = {HomeSQLiteHelper.COLUMN_ID, HomeSQLiteHelper.COLUMN_NAME, HomeSQLiteHelper.COLUMN_PRO_PIC,
            HomeSQLiteHelper.COLUMN_SCREEN_NAME };

    public FavoriteUsersDataSource(Context context) {
        dbHelper = new FavoriteUsersSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void createUser(User user) {
        ContentValues values = new ContentValues();

        long id = user.getId();
        String screenName = user.getScreenName();
        String name = user.getName();
        String proPicUrl = user.getBiggerProfileImageURL();

        values.put(FavoriteUsersSQLiteHelper.COLUMN_ID, id);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_NAME, name);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_PRO_PIC, proPicUrl);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_SCREEN_NAME, screenName);

        database.insert(FavoriteUsersSQLiteHelper.TABLE_HOME, null, values);
    }

    public void deleteUser(long userId) {
        long id = userId;

        database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME, HomeSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }


    public void deleteAllUsers() {
        database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME, null, null);
    }

    public Cursor getCursor() {
        Cursor cursor = database.query(FavoriteUsersSQLiteHelper.TABLE_HOME,
                allColumns, null, null, null, null, null);

        return cursor;
    }
}
