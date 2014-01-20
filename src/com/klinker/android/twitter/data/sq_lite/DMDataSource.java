package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.klinker.android.twitter.utils.HtmlUtils;

import twitter4j.DirectMessage;
import twitter4j.MediaEntity;

public class DMDataSource {

    // Database fields
    private SQLiteDatabase database;
    private DMSQLiteHelper dbHelper;
    public String[] allColumns = {DMSQLiteHelper.COLUMN_ID, DMSQLiteHelper.COLUMN_TWEET_ID, DMSQLiteHelper.COLUMN_ACCOUNT, DMSQLiteHelper.COLUMN_TYPE,
            DMSQLiteHelper.COLUMN_TEXT, DMSQLiteHelper.COLUMN_NAME, DMSQLiteHelper.COLUMN_PRO_PIC,
            DMSQLiteHelper.COLUMN_SCREEN_NAME, DMSQLiteHelper.COLUMN_TIME, DMSQLiteHelper.COLUMN_PIC_URL, DMSQLiteHelper.COLUMN_RETWEETER,
            DMSQLiteHelper.COLUMN_URL, HomeSQLiteHelper.COLUMN_USERS, HomeSQLiteHelper.COLUMN_HASHTAGS, DMSQLiteHelper.COLUMN_EXTRA_ONE, DMSQLiteHelper.COLUMN_EXTRA_TWO };

    public DMDataSource(Context context) {
        dbHelper = new DMSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void createDirectMessage(DirectMessage status, int account) {
        ContentValues values = new ContentValues();
        long time = status.getCreatedAt().getTime();

        values.put(DMSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(DMSQLiteHelper.COLUMN_TEXT, HtmlUtils.getHtmlStatus(status)[0]);
        values.put(DMSQLiteHelper.COLUMN_TWEET_ID, status.getId());
        values.put(DMSQLiteHelper.COLUMN_NAME, status.getSender().getName());
        values.put(DMSQLiteHelper.COLUMN_PRO_PIC, status.getSender().getBiggerProfileImageURL());
        values.put(DMSQLiteHelper.COLUMN_SCREEN_NAME, status.getSender().getScreenName());
        values.put(DMSQLiteHelper.COLUMN_TIME, time);
        values.put(DMSQLiteHelper.COLUMN_RETWEETER, status.getRecipientScreenName());
        values.put(DMSQLiteHelper.COLUMN_EXTRA_ONE, status.getRecipient().getBiggerProfileImageURL());
        values.put(DMSQLiteHelper.COLUMN_EXTRA_TWO, status.getRecipient().getName());

        MediaEntity[] entities = status.getMediaEntities();

        if (entities.length > 0) {
            values.put(DMSQLiteHelper.COLUMN_PIC_URL, entities[0].getMediaURL());
        }
        database.insert(DMSQLiteHelper.TABLE_DM, null, values);
    }

    public void deleteTweet(long tweetId) {
        long id = tweetId;
        database.delete(DMSQLiteHelper.TABLE_DM, DMSQLiteHelper.COLUMN_TWEET_ID
                + " = " + id, null);
    }

    public void deleteAllTweets(int account) {
        database.delete(DMSQLiteHelper.TABLE_DM, DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
    }

    public Cursor getCursor(int account) {
        Cursor cursor = database.query(true, DMSQLiteHelper.TABLE_DM,
                allColumns, DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC", null);

        return cursor;
    }

    public String getNewestName(int account) {

        Cursor cursor = getCursor(account);
        String name = "";

        try {
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_SCREEN_NAME));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return name;
    }

    public String getNewestMessage(int account) {

        Cursor cursor = getCursor(account);
        String message = "";

        try {
            if (cursor.moveToFirst()) {
                message = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_TEXT));
            }
        } catch (Exception e) {

        }

        cursor.close();

        return message;
    }

    public void deleteDups(int account) {
        database.execSQL("DELETE FROM " + DMSQLiteHelper.TABLE_DM + " WHERE _id NOT IN (SELECT MIN(_id) FROM " + DMSQLiteHelper.TABLE_DM + " GROUP BY " + DMSQLiteHelper.COLUMN_TWEET_ID + ") AND " + DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account);
    }


}
