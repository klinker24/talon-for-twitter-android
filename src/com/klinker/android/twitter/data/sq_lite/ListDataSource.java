package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.klinker.android.twitter.utils.HtmlUtils;

import twitter4j.Status;


public class ListDataSource {

    // Database fields
    private SQLiteDatabase database;
    private HomeSQLiteHelper dbHelper;
    private Context context;
    private int timelineSize;
    private boolean noRetweets;
    private SharedPreferences sharedPreferences;
    public static String[] allColumns = { HomeSQLiteHelper.COLUMN_ID,
            HomeSQLiteHelper.COLUMN_TWEET_ID,
            HomeSQLiteHelper.COLUMN_ACCOUNT,
            HomeSQLiteHelper.COLUMN_TYPE,
            HomeSQLiteHelper.COLUMN_TEXT,
            HomeSQLiteHelper.COLUMN_NAME,
            HomeSQLiteHelper.COLUMN_PRO_PIC,
            HomeSQLiteHelper.COLUMN_SCREEN_NAME,
            HomeSQLiteHelper.COLUMN_TIME,
            HomeSQLiteHelper.COLUMN_PIC_URL,
            HomeSQLiteHelper.COLUMN_RETWEETER,
            HomeSQLiteHelper.COLUMN_URL,
            HomeSQLiteHelper.COLUMN_USERS,
            HomeSQLiteHelper.COLUMN_HASHTAGS };

    public ListDataSource(Context context) {
        dbHelper = new HomeSQLiteHelper(context);
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        timelineSize = Integer.parseInt(sharedPreferences.getString("timeline_size", "1000"));
        noRetweets = sharedPreferences.getBoolean("ignore_retweets", false);
    }

    public void open() throws SQLException {
        try {
            database = dbHelper.getWritableDatabase();
        } catch (Exception e) {

        }
    }

    public void close() {
        dbHelper.close();
    }

    public void createTweet(Status status, int account) {
        ContentValues values = new ContentValues();
        String originalName = "";
        long time = status.getCreatedAt().getTime();
        long id = status.getId();

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

        if (database == null) {
            open();
        }

        database.insert(HomeSQLiteHelper.TABLE_HOME, null, values);
    }

    public void createTweet(Status status, int account, boolean initial) {
        ContentValues values = new ContentValues();
        String originalName = "";
        long time = status.getCreatedAt().getTime();
        long id = status.getId();

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

        if (database == null) {
            open();
        }

        database.insert(HomeSQLiteHelper.TABLE_HOME, null, values);
    }

    public void deleteTweet(long tweetId) {
        long id = tweetId;

        if (database == null) {
            open();
        }

        database.delete(HomeSQLiteHelper.TABLE_HOME, HomeSQLiteHelper.COLUMN_TWEET_ID
                + " = " + id, null);
    }

    public void deleteAllTweets(int account) {
        if (database == null) {
            open();
        }

        database.delete(HomeSQLiteHelper.TABLE_HOME,
                HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
    }

    public Cursor getCursor(int account) {

        if (database == null) {
            open();
        }

        String users = sharedPreferences.getString("muted_users", "");
        String hashtags = sharedPreferences.getString("muted_hashtags", "");
        String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account;

        if (!users.equals("")) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + HomeSQLiteHelper.COLUMN_SCREEN_NAME + " NOT LIKE '" + s + "'";
            }

            for (String s : split) {
                where += " AND " + HomeSQLiteHelper.COLUMN_RETWEETER + " NOT LIKE '" + s + "'";
            }
        }

        if (!hashtags.equals("")) {
            String[] split = hashtags.split(" ");
            for (String s : split) {
                where += " AND " + HomeSQLiteHelper.COLUMN_HASHTAGS + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (noRetweets) {
            where += " AND " + HomeSQLiteHelper.COLUMN_RETWEETER + " = '' OR " + HomeSQLiteHelper.COLUMN_RETWEETER + " is NULL";
        }

        if (database == null) {
            open();
        }

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        if (cursor.getCount() > timelineSize) {
            cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                    allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC", (cursor.getCount() - timelineSize) + "," + timelineSize);
        }

        return cursor;
    }

    public long[] getLastIds(int account) {
        long id[] = new long[5];

        Cursor cursor = getCursor(account);

        try {
            if (cursor.moveToFirst()) {
                int i = 0;
                do {
                    id[i] = cursor.getLong(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_TWEET_ID));
                } while (cursor.moveToNext() && i < 5);
            }
        } catch (Exception e) {
        }

        cursor.close();

        return id;
    }

    public int getPosition(int account, long id) {
        int pos = 0;

        Cursor cursor = getCursor(account);
        if (cursor.moveToLast()) {
            do {
                if (cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) == id) {
                    break;
                } else {
                    pos++;
                }
            } while (cursor.moveToPrevious());
        }

        cursor.close();

        return pos;
    }

}
