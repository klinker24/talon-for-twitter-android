package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;

import com.klinker.android.twitter.utils.HtmlUtils;

import twitter4j.Status;

public class HomeDataSource {

    // Database fields
    private SQLiteDatabase database;
    private HomeSQLiteHelper dbHelper;
    private Context context;
    public static String[] allColumns = { HomeSQLiteHelper.COLUMN_ID, HomeSQLiteHelper.COLUMN_TWEET_ID, HomeSQLiteHelper.COLUMN_ACCOUNT, HomeSQLiteHelper.COLUMN_TYPE,
            HomeSQLiteHelper.COLUMN_TEXT, HomeSQLiteHelper.COLUMN_NAME, HomeSQLiteHelper.COLUMN_PRO_PIC,
            HomeSQLiteHelper.COLUMN_SCREEN_NAME, HomeSQLiteHelper.COLUMN_TIME, HomeSQLiteHelper.COLUMN_PIC_URL,
            HomeSQLiteHelper.COLUMN_RETWEETER, HomeSQLiteHelper.COLUMN_URL, HomeSQLiteHelper.COLUMN_USERS, HomeSQLiteHelper.COLUMN_HASHTAGS };

    public HomeDataSource(Context context) {
        dbHelper = new HomeSQLiteHelper(context);
        this.context = context;
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

        if(status.isRetweet()) {
            originalName = status.getUser().getScreenName();
            status = status.getRetweetedStatus();
        }

        String[] html = HtmlUtils.getHtmlStatus(status);
        String text = html[0];
        String media = html[1];
        String url = html[2];
        String hashtags = html[3];
        String users = html[4];

        values.put(HomeSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(HomeSQLiteHelper.COLUMN_TEXT, text);
        values.put(HomeSQLiteHelper.COLUMN_TWEET_ID, id);
        values.put(HomeSQLiteHelper.COLUMN_NAME, status.getUser().getName());
        values.put(HomeSQLiteHelper.COLUMN_PRO_PIC, status.getUser().getBiggerProfileImageURL());
        values.put(HomeSQLiteHelper.COLUMN_SCREEN_NAME, status.getUser().getScreenName());
        values.put(HomeSQLiteHelper.COLUMN_TIME, time);
        values.put(HomeSQLiteHelper.COLUMN_RETWEETER, originalName);
        values.put(HomeSQLiteHelper.COLUMN_UNREAD, 1);
        values.put(HomeSQLiteHelper.COLUMN_PIC_URL, media);
        values.put(HomeSQLiteHelper.COLUMN_URL, url);
        values.put(HomeSQLiteHelper.COLUMN_USERS, users);
        values.put(HomeSQLiteHelper.COLUMN_HASHTAGS, hashtags);

        database.insert(HomeSQLiteHelper.TABLE_HOME, null, values);
    }

    public void createTweet(Status status, int account, boolean initial) {
        ContentValues values = new ContentValues();
        String originalName = "";
        long id = status.getId();
        long time = status.getCreatedAt().getTime();

        if(status.isRetweet()) {
            originalName = status.getUser().getScreenName();
            status = status.getRetweetedStatus();
        }

        String[] html = HtmlUtils.getHtmlStatus(status);
        String text = html[0];
        String media = html[1];
        String url = html[2];
        String hashtags = html[3];
        String users = html[4];

        values.put(HomeSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(HomeSQLiteHelper.COLUMN_TEXT, text);
        values.put(HomeSQLiteHelper.COLUMN_TWEET_ID, id);
        values.put(HomeSQLiteHelper.COLUMN_NAME, status.getUser().getName());
        values.put(HomeSQLiteHelper.COLUMN_PRO_PIC, status.getUser().getBiggerProfileImageURL());
        values.put(HomeSQLiteHelper.COLUMN_SCREEN_NAME, status.getUser().getScreenName());
        values.put(HomeSQLiteHelper.COLUMN_TIME, time);
        values.put(HomeSQLiteHelper.COLUMN_RETWEETER, originalName);
        values.put(HomeSQLiteHelper.COLUMN_UNREAD, 0);
        values.put(HomeSQLiteHelper.COLUMN_PIC_URL, media);
        values.put(HomeSQLiteHelper.COLUMN_URL, url);
        values.put(HomeSQLiteHelper.COLUMN_USERS, users);
        values.put(HomeSQLiteHelper.COLUMN_HASHTAGS, hashtags);

        database.insert(HomeSQLiteHelper.TABLE_HOME, null, values);
    }

    public void deleteTweet(long tweetId) {
        long id = tweetId;

        database.delete(HomeSQLiteHelper.TABLE_HOME, HomeSQLiteHelper.COLUMN_TWEET_ID
                + " = " + id, null);
    }

    public void deleteAllTweets(int account) {
        database.delete(HomeSQLiteHelper.TABLE_HOME,
                HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
    }

    public Cursor getCursor(int account) {

        String users = PreferenceManager.getDefaultSharedPreferences(context).getString("muted_users", "");
        String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        if (!users.equals("")) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + HomeSQLiteHelper.COLUMN_SCREEN_NAME + " NOT LIKE '" + s + "'";
            }
        }

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        return cursor;
    }

    public Cursor getWidgetCursor(int account) {

        String users = PreferenceManager.getDefaultSharedPreferences(context).getString("muted_users", "");
        String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        if (!users.equals("")) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + HomeSQLiteHelper.COLUMN_SCREEN_NAME + " NOT LIKE '" + s + "'";
            }
        }

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " DESC");

        return cursor;
    }

    public Cursor getUnreadCursor(int account) {

        String users = PreferenceManager.getDefaultSharedPreferences(context).getString("muted_users", "");
        String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + HomeSQLiteHelper.COLUMN_UNREAD + " = ?";

        if (!users.equals("")) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + HomeSQLiteHelper.COLUMN_SCREEN_NAME + " NOT LIKE '" + s + "'";
            }
        }

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, new String[] {account + "", "1"}, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        return cursor;
    }

    public Cursor getPicsCursor(int account) {

        String users = PreferenceManager.getDefaultSharedPreferences(context).getString("muted_users", "");
        String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + HomeSQLiteHelper.COLUMN_PIC_URL + " LIKE '%ht%'";

        if (!users.equals("")) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + HomeSQLiteHelper.COLUMN_SCREEN_NAME + " NOT LIKE '" + s + "'";
            }
        }

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        return cursor;
    }

    public Cursor getLinksCursor(int account) {

        String users = PreferenceManager.getDefaultSharedPreferences(context).getString("muted_users", "");
        String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + HomeSQLiteHelper.COLUMN_URL + " LIKE '%ht%'";

        if (!users.equals("")) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + HomeSQLiteHelper.COLUMN_SCREEN_NAME + " NOT LIKE '" + s + "'";
            }
        }

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        return cursor;
    }

    public int getUnreadCount(int account) {

        Cursor cursor = getUnreadCursor(account);

        int count = cursor.getCount();

        cursor.close();

        return count;
    }

    public void markRead(int account, int position) {
        Cursor cursor = getUnreadCursor(account);

        if (cursor.moveToPosition(position)) {
            long tweetId = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));

            ContentValues cv = new ContentValues();
            cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 0);

            database.update(HomeSQLiteHelper.TABLE_HOME, cv, HomeSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {tweetId + ""});
        }

        cursor.close();
    }

    public void markMultipleRead(int current, int account) {

        Cursor cursor = getUnreadCursor(account);

        try {
            if (cursor.moveToPosition(current)) {
                do {
                    long tweetId = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));

                    ContentValues cv = new ContentValues();
                    cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 0);

                    database.update(HomeSQLiteHelper.TABLE_HOME, cv, HomeSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {tweetId + ""});

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            // there is nothing in the unread array
        }

        cursor.close();
    }

    public void markMultipleRead(String text, int account) {

        Cursor c = getUnreadCursor(account); // first is the oldest

        ContentValues cv = new ContentValues();
        cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 0);

        boolean dontMark = true;
        if (c.moveToLast()) {
            do {
                String thisText = c.getString(c.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT));
                Log.v("talon_pull_mark_read", thisText);
                if (text.equals(thisText)) {
                    dontMark = false;
                }

                if (!dontMark) {
                    long tweetId = c.getLong(c.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                    database.update(HomeSQLiteHelper.TABLE_HOME, cv, HomeSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {tweetId + ""});
                }
            } while (c.moveToPrevious());
        }

        c.close();
    }

    public void markAllRead(int account) {

        ContentValues cv = new ContentValues();
        cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 0);

        database.update(HomeSQLiteHelper.TABLE_HOME, cv, HomeSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + HomeSQLiteHelper.COLUMN_UNREAD + " = ?", new String[] {account + "", "1"});
    }

    public long getLastId(int account) {
        long id = 0;

        Cursor cursor = getCursor(account);

        try {
            if (cursor.moveToLast()) {
                id = cursor.getLong(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_TWEET_ID));
            }
        } catch (Exception e) {
        }

        cursor.close();

        return id;
    }

    public boolean tweetExists(long tweetId, int account) {
        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + HomeSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        return cursor.getCount() > 0;
    }
}
