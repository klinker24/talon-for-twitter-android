package com.klinker.android.twitter_l.data.sq_lite;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        if (dataSource == null ||
                dataSource.getDatabase() == null ||
                !dataSource.getDatabase().isOpen()) {

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

        try {
            database = dbHelper.getWritableDatabase();
        } catch (Exception e) {
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

    public FavoriteUsersSQLiteHelper getHelper() {
        return dbHelper;
    }

    public synchronized void createUser(User user, int account) {
        ContentValues values = new ContentValues();

        long id = user.getId();
        String screenName = user.getScreenName();
        String name = user.getName();
        String proPicUrl = user.getOriginalProfileImageURL();

        values.put(FavoriteUsersSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_ID, id);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_NAME, name);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_PRO_PIC, proPicUrl);
        values.put(FavoriteUsersSQLiteHelper.COLUMN_SCREEN_NAME, screenName);

        try {
            database.insert(FavoriteUsersSQLiteHelper.TABLE_HOME, null, values);
        } catch (Exception e) {
            open();
            database.insert(FavoriteUsersSQLiteHelper.TABLE_HOME, null, values);
        }
    }

    public synchronized void deleteUser(long userId) {
        long id = userId;

        try {
            database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME, FavoriteUsersSQLiteHelper.COLUMN_ID
                    + " = " + id, null);
        } catch (Exception e) {
            open();
            database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME, FavoriteUsersSQLiteHelper.COLUMN_ID
                    + " = " + id, null);
        }
    }

    public synchronized void deleteAllUsers() {

        try {
            database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME, null, null);
        } catch (Exception e) {
            open();
            database.delete(FavoriteUsersSQLiteHelper.TABLE_HOME, null, null);
        }
    }

    public synchronized Cursor getCursor() {

        Cursor cursor;
        try {
            cursor = database.query(FavoriteUsersSQLiteHelper.TABLE_HOME,
                    allColumns, null, null, null, null, null);
        } catch (Exception e) {
            open();
            cursor = database.query(FavoriteUsersSQLiteHelper.TABLE_HOME,
                    allColumns, null, null, null, null, null);
        }

        return cursor;
    }

    public String getNames() {
        String names = "";

        Cursor cursor = getCursor();

        if (cursor.moveToFirst()) {
            do {
                names += cursor.getString(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_SCREEN_NAME)) + "  ";
            } while (cursor.moveToNext());
        }

        cursor.close();

        return names;
    }

    public boolean isFavUser(String username) {
        Cursor check = getCursor();

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
