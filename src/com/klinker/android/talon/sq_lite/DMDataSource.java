package com.klinker.android.talon.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.klinker.android.talon.utils.Tweet;

import java.util.ArrayList;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.MediaEntity;

public class DMDataSource {

    // Database fields
    private SQLiteDatabase database;
    private DMSQLiteHelper dbHelper;
    public String[] allColumns = {DMSQLiteHelper.COLUMN_ID, DMSQLiteHelper.COLUMN_ACCOUNT, DMSQLiteHelper.COLUMN_TYPE,
            DMSQLiteHelper.COLUMN_TEXT, DMSQLiteHelper.COLUMN_NAME, DMSQLiteHelper.COLUMN_PRO_PIC,
            DMSQLiteHelper.COLUMN_SCREEN_NAME, DMSQLiteHelper.COLUMN_TIME, DMSQLiteHelper.COLUMN_PIC_URL, DMSQLiteHelper.COLUMN_RETWEETER };

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
        values.put(DMSQLiteHelper.COLUMN_TEXT, status.getText());
        values.put(DMSQLiteHelper.COLUMN_ID, status.getId());
        values.put(DMSQLiteHelper.COLUMN_NAME, status.getSender().getName());
        values.put(DMSQLiteHelper.COLUMN_PRO_PIC, status.getSender().getBiggerProfileImageURL());
        values.put(DMSQLiteHelper.COLUMN_SCREEN_NAME, status.getSender().getScreenName());
        values.put(DMSQLiteHelper.COLUMN_TIME, time);
        values.put(DMSQLiteHelper.COLUMN_RETWEETER, status.getRecipientScreenName());

        MediaEntity[] entities = status.getMediaEntities();

        if (entities.length > 0) {
            values.put(DMSQLiteHelper.COLUMN_PIC_URL, entities[0].getMediaURL());
        }
        database.insert(DMSQLiteHelper.TABLE_DM, null, values);
    }

    public void deleteTweet(long tweetId) {
        long id = tweetId;
        database.delete(DMSQLiteHelper.TABLE_DM, DMSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public void deleteAllTweets(int account) {
        database.delete(DMSQLiteHelper.TABLE_DM, DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
    }

    public Cursor getCursor(int account) {
        Cursor cursor = database.query(DMSQLiteHelper.TABLE_DM,
                allColumns, DMSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null, null, null, null);

        return cursor;
    }
}
