package com.klinker.android.roar.SQLite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

public class HomeDataSource {

    // Database fields
    private SQLiteDatabase database;
    private HomeSQLiteHelper dbHelper;
    public String[] allColumns = {HomeSQLiteHelper.COLUMN_ID,
            HomeSQLiteHelper.COLUMN_TEXT, HomeSQLiteHelper.COLUMN_NAME, HomeSQLiteHelper.COLUMN_PRO_PIC,
            HomeSQLiteHelper.COLUMN_SCREEN_NAME, HomeSQLiteHelper.COLUMN_TIME, HomeSQLiteHelper.COLUMN_RETWEET_COUNT,
            HomeSQLiteHelper.COLUMN_FAVORITE_COUNT};

    public HomeDataSource(Context context) {
        dbHelper = new HomeSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void createTweet(Status status) {
        ContentValues values = new ContentValues();
        values.put(HomeSQLiteHelper.COLUMN_TEXT, status.getText());
        values.put(HomeSQLiteHelper.COLUMN_ID, status.getId());
        values.put(HomeSQLiteHelper.COLUMN_NAME, status.getUser().getName());
        values.put(HomeSQLiteHelper.COLUMN_PRO_PIC, status.getUser().getBiggerProfileImageURL());
        values.put(HomeSQLiteHelper.COLUMN_SCREEN_NAME, status.getUser().getScreenName());
        values.put(HomeSQLiteHelper.COLUMN_TIME, status.getCreatedAt().getTime());
        values.put(HomeSQLiteHelper.COLUMN_FAVORITE_COUNT, status.getFavoriteCount());
        values.put(HomeSQLiteHelper.COLUMN_RETWEET_COUNT, status.getRetweetCount());
        database.insert(HomeSQLiteHelper.TABLE_HOME, null, values);
    }

    public void deleteTweet(Tweet tweet) {
        long id = tweet.getId();
        System.out.println("Comment deleted with id: " + id);
        database.delete(HomeSQLiteHelper.TABLE_HOME, HomeSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public List<Tweet> getAllTweets() {
        List<Tweet> tweets = new ArrayList<Tweet>();

        Cursor cursor = getCursor();

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
        database.delete(HomeSQLiteHelper.TABLE_HOME, null, null);
    }

    public Cursor getCursor() {
        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, null, null, null, null, null);

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
