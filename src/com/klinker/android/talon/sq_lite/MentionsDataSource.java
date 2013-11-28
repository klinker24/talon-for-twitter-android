package com.klinker.android.talon.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.klinker.android.talon.utils.Tweet;

import java.util.ArrayList;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.MediaEntity;
import twitter4j.Status;

public class MentionsDataSource {

    // Database fields
    private SQLiteDatabase database;
    private MentionsSQLiteHelper dbHelper;
    public String[] allColumns = {MentionsSQLiteHelper.COLUMN_ID, MentionsSQLiteHelper.COLUMN_ACCOUNT, MentionsSQLiteHelper.COLUMN_TYPE,
            MentionsSQLiteHelper.COLUMN_TEXT, MentionsSQLiteHelper.COLUMN_NAME, MentionsSQLiteHelper.COLUMN_PRO_PIC,
            MentionsSQLiteHelper.COLUMN_SCREEN_NAME, MentionsSQLiteHelper.COLUMN_TIME, MentionsSQLiteHelper.COLUMN_PIC_URL,
            MentionsSQLiteHelper.COLUMN_RETWEETER };

    public MentionsDataSource(Context context) {
        dbHelper = new MentionsSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void createTweet(Status status, int account) {
        ContentValues values = new ContentValues();
        String originalName = "";
        long id = status.getId();
        long time = status.getCreatedAt().getTime();

        values.put(MentionsSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(MentionsSQLiteHelper.COLUMN_TEXT, status.getText());
        values.put(MentionsSQLiteHelper.COLUMN_ID, id);
        values.put(MentionsSQLiteHelper.COLUMN_NAME, status.getUser().getName());
        values.put(MentionsSQLiteHelper.COLUMN_PRO_PIC, status.getUser().getBiggerProfileImageURL());
        values.put(MentionsSQLiteHelper.COLUMN_SCREEN_NAME, status.getUser().getScreenName());
        values.put(MentionsSQLiteHelper.COLUMN_TIME, time);
        values.put(MentionsSQLiteHelper.COLUMN_RETWEETER, originalName);

        MediaEntity[] entities = status.getMediaEntities();

        if (entities.length > 0) {
            values.put(MentionsSQLiteHelper.COLUMN_PIC_URL, entities[0].getMediaURL());
        }

        database.insert(MentionsSQLiteHelper.TABLE_MENTIONS, null, values);
    }

    public void deleteTweet(long tweetId) {
        long id = tweetId;
        database.delete(MentionsSQLiteHelper.TABLE_MENTIONS, MentionsSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public List<Tweet> getAllTweets(int account) {
        List<Tweet> tweets = new ArrayList<Tweet>();

        Cursor cursor = getCursor(account);

        cursor.moveToLast();
        while (!cursor.isBeforeFirst()) {
            Tweet tweet = cursorToTweet(cursor);
            tweets.add(tweet);
            cursor.moveToPrevious();
        }
        // make sure to close the cursor
        cursor.close();
        return tweets;
    }

    public void deleteAllTweets() {
        database.delete(MentionsSQLiteHelper.TABLE_MENTIONS, null, null);
    }

    public Cursor getCursor(int account) {
        Cursor cursor = database.query(MentionsSQLiteHelper.TABLE_MENTIONS,
                allColumns, MentionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null, null, null, null);

        return cursor;
    }

    private Tweet cursorToTweet(Cursor cursor) {
        Tweet tweet = new Tweet();
        tweet.setId(cursor.getLong(0));
        tweet.setTweet(cursor.getString(1));
        tweet.setName(cursor.getString(2));
        return tweet;
    }
}
