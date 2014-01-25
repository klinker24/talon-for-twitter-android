package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
    private int timelineSize;
    private boolean noRetweets;
    private SharedPreferences sharedPreferences;
    public static String[] allColumns = { HomeSQLiteHelper.COLUMN_ID, HomeSQLiteHelper.COLUMN_TWEET_ID, HomeSQLiteHelper.COLUMN_ACCOUNT, HomeSQLiteHelper.COLUMN_TYPE,
            HomeSQLiteHelper.COLUMN_TEXT, HomeSQLiteHelper.COLUMN_NAME, HomeSQLiteHelper.COLUMN_PRO_PIC,
            HomeSQLiteHelper.COLUMN_SCREEN_NAME, HomeSQLiteHelper.COLUMN_TIME, HomeSQLiteHelper.COLUMN_PIC_URL,
            HomeSQLiteHelper.COLUMN_RETWEETER, HomeSQLiteHelper.COLUMN_URL, HomeSQLiteHelper.COLUMN_USERS, HomeSQLiteHelper.COLUMN_HASHTAGS };

    public HomeDataSource(Context context) {
        dbHelper = new HomeSQLiteHelper(context);
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        timelineSize = Integer.parseInt(sharedPreferences.getString("timeline_size", "1000"));
        noRetweets = sharedPreferences.getBoolean("ignore_retweets", false);
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

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        if (cursor.getCount() > timelineSize) {
            cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                    allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC", (cursor.getCount() - timelineSize) + "," + timelineSize);
        }

        return cursor;
    }

    public Cursor getWidgetCursor(int account) {

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

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " DESC");

        return cursor;
    }

    public Cursor getUnreadCursor(int account) {

        String users = sharedPreferences.getString("muted_users", "");
        String hashtags = sharedPreferences.getString("muted_hashtags", "");
        String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + HomeSQLiteHelper.COLUMN_UNREAD + " = ?";

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

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, new String[] {account + "", "1"}, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        return cursor;
    }

    public Cursor getPicsCursor(int account) {

        String users = sharedPreferences.getString("muted_users", "");
        String hashtags = sharedPreferences.getString("muted_hashtags", "");
        String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + HomeSQLiteHelper.COLUMN_PIC_URL + " LIKE '%ht%'";

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

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        if (cursor.getCount() > timelineSize) {
            cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                    allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC", (cursor.getCount() - timelineSize) + "," + timelineSize);
        }

        return cursor;
    }

    public Cursor getLinksCursor(int account) {

        String users = sharedPreferences.getString("muted_users", "");
        String hashtags = sharedPreferences.getString("muted_hashtags", "");
        String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + HomeSQLiteHelper.COLUMN_URL + " LIKE '%ht%'";

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

        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        if (cursor.getCount() > timelineSize) {
            cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                    allColumns, where, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " DESC", (cursor.getCount() - timelineSize) + "," + timelineSize);
        }

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

    public void markUnread(int account, long wantid) {
        Cursor cursor = getCursor(account);

        ContentValues cv = new ContentValues();
        cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 1);

        if (cursor.moveToFirst()) {
            boolean mark = false;
            do {
                long tweetId = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));

                if (wantid == tweetId) {
                    mark = true;
                }
                if (mark) {
                    database.update(HomeSQLiteHelper.TABLE_HOME, cv, HomeSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {tweetId + ""});
                }
            } while (cursor.moveToNext());
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

    public void markUnreadFilling(int account) {
        ContentValues cv = new ContentValues();
        cv.put(HomeSQLiteHelper.COLUMN_UNREAD, 1);

        // first get the unread cursor to find the first id to mark unread
        Cursor unread = getUnreadCursor(account);

        if (unread.moveToFirst()) {
            // this is the long for the first unread tweet in the list
            long id = unread.getLong(unread.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));

            Cursor full = getCursor(account);
            if (full.moveToFirst()) {
                boolean startUnreads = false;
                do {
                    long thisId = full.getLong(full.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));

                    if (thisId == id) {
                        startUnreads = true;
                    }

                    if (startUnreads) {
                        database.update(HomeSQLiteHelper.TABLE_HOME, cv, HomeSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {thisId + ""});
                    }
                } while (full.moveToNext());
            }
            full.close();
        }

        unread.close();
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

    public boolean tweetExists(long tweetId, int account) {
        Cursor cursor = database.query(HomeSQLiteHelper.TABLE_HOME,
                allColumns, HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account + " AND " + HomeSQLiteHelper.COLUMN_TWEET_ID + " = " + tweetId, null, null, null, HomeSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        return cursor.getCount() > 0;
    }

    public void deleteDups(int account) {
        database.execSQL("DELETE FROM " + HomeSQLiteHelper.TABLE_HOME + " WHERE _id NOT IN (SELECT MIN(_id) FROM " + HomeSQLiteHelper.TABLE_HOME + " GROUP BY " + HomeSQLiteHelper.COLUMN_TWEET_ID + ") AND " + HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + account);
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
        return pos;
    }
}
