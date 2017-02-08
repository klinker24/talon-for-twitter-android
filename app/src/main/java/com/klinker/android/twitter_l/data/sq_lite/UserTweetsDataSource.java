package com.klinker.android.twitter_l.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;

import java.util.List;

import twitter4j.Status;

public class UserTweetsDataSource {

    // provides access to the database
    public static UserTweetsDataSource dataSource = null;

    /*

    This is used so that we don't have to open and close the database on different threads or fragments
    every time. This will facilitate it between all of them to avoid Illegal State Exceptions.

     */
    public static UserTweetsDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        if (dataSource == null ||
                dataSource.getDatabase() == null ||
                !dataSource.getDatabase().isOpen()) {

            dataSource = new UserTweetsDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    // Database fields
    private SQLiteDatabase database;
    private UserTweetsSQLiteHelper dbHelper;
    private Context context;
    private int timelineSize;
    private boolean noRetweets;
    private SharedPreferences sharedPreferences;
    public static String[] allColumns = { UserTweetsSQLiteHelper.COLUMN_ID,
            UserTweetsSQLiteHelper.COLUMN_TWEET_ID,
            UserTweetsSQLiteHelper.COLUMN_ACCOUNT,
            UserTweetsSQLiteHelper.COLUMN_TEXT,
            UserTweetsSQLiteHelper.COLUMN_NAME,
            UserTweetsSQLiteHelper.COLUMN_PRO_PIC,
            UserTweetsSQLiteHelper.COLUMN_SCREEN_NAME,
            UserTweetsSQLiteHelper.COLUMN_TIME,
            UserTweetsSQLiteHelper.COLUMN_PIC_URL,
            UserTweetsSQLiteHelper.COLUMN_RETWEETER,
            UserTweetsSQLiteHelper.COLUMN_URL,
            UserTweetsSQLiteHelper.COLUMN_USERS,
            UserTweetsSQLiteHelper.COLUMN_HASHTAGS,
            UserTweetsSQLiteHelper.COLUMN_ANIMATED_GIF
    };

    private UserTweetsDataSource(Context context) {
        dbHelper = new UserTweetsSQLiteHelper(context);
        this.context = context;
        sharedPreferences = AppSettings.getSharedPreferences(context);

        timelineSize = Integer.parseInt(sharedPreferences.getString("timeline_size", "1000"));
        noRetweets = sharedPreferences.getBoolean("ignore_retweets", false);
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

    public UserTweetsSQLiteHelper getHelper() {
        return dbHelper;
    }

    public synchronized void createTweet(Status status, long userId) {
        ContentValues values = new ContentValues();
        String originalName = "";
        long time = status.getCreatedAt().getTime();
        long id = status.getId();

        if(status.isRetweet()) {
            originalName = status.getUser().getScreenName();
            status = status.getRetweetedStatus();
        }

        String[] html = TweetLinkUtils.getLinksInStatus(status);
        String text = html[0];
        String media = html[1];
        String url = html[2];
        String hashtags = html[3];
        String users = html[4];

        if (media.contains("/tweet_video/")) {
            media = media.replace("tweet_video", "tweet_video_thumb").replace(".mp4", ".png").replace(".m3u8", ".png");;
        }

        values.put(UserTweetsSQLiteHelper.COLUMN_TEXT, text);
        values.put(UserTweetsSQLiteHelper.COLUMN_TWEET_ID, id);
        values.put(UserTweetsSQLiteHelper.COLUMN_NAME, status.getUser().getName());
        values.put(UserTweetsSQLiteHelper.COLUMN_PRO_PIC, status.getUser().getOriginalProfileImageURL());
        values.put(UserTweetsSQLiteHelper.COLUMN_SCREEN_NAME, status.getUser().getScreenName());
        values.put(UserTweetsSQLiteHelper.COLUMN_TIME, time);
        values.put(UserTweetsSQLiteHelper.COLUMN_RETWEETER, originalName);
        values.put(UserTweetsSQLiteHelper.COLUMN_PIC_URL, media);
        values.put(UserTweetsSQLiteHelper.COLUMN_URL, url);
        values.put(UserTweetsSQLiteHelper.COLUMN_USERS, users);
        values.put(UserTweetsSQLiteHelper.COLUMN_HASHTAGS, hashtags);
        values.put(UserTweetsSQLiteHelper.COLUMN_USER_ID, userId);
        values.put(UserTweetsSQLiteHelper.COLUMN_ANIMATED_GIF, TweetLinkUtils.getGIFUrl(status, url));

        try {
            database.insert(UserTweetsSQLiteHelper.TABLE_HOME, null, values);
        } catch (Exception e) {
            open();
            database.insert(UserTweetsSQLiteHelper.TABLE_HOME, null, values);
        }
    }

    public int insertTweets(List<Status> statuses, long userId) {

        ContentValues[] valueses = new ContentValues[statuses.size()];

        for (int i = 0; i < statuses.size(); i++) {
            Status status = statuses.get(i);

            ContentValues values = new ContentValues();
            String originalName = "";
            long time = status.getCreatedAt().getTime();
            long id = status.getId();

            if(status.isRetweet()) {
                originalName = status.getUser().getScreenName();
                status = status.getRetweetedStatus();
            }

            String[] html = TweetLinkUtils.getLinksInStatus(status);
            String text = html[0];
            String media = html[1];
            String url = html[2];
            String hashtags = html[3];
            String users = html[4];

            if (media.contains("/tweet_video/")) {
                media = media.replace("tweet_video", "tweet_video_thumb").replace(".mp4", ".png").replace(".m3u8", ".png");;
            }

            values.put(UserTweetsSQLiteHelper.COLUMN_TEXT, text);
            values.put(UserTweetsSQLiteHelper.COLUMN_TWEET_ID, id);
            values.put(UserTweetsSQLiteHelper.COLUMN_NAME, status.getUser().getName());
            values.put(UserTweetsSQLiteHelper.COLUMN_PRO_PIC, status.getUser().getOriginalProfileImageURL());
            values.put(UserTweetsSQLiteHelper.COLUMN_SCREEN_NAME, status.getUser().getScreenName());
            values.put(UserTweetsSQLiteHelper.COLUMN_TIME, time);
            values.put(UserTweetsSQLiteHelper.COLUMN_RETWEETER, originalName);
            values.put(UserTweetsSQLiteHelper.COLUMN_PIC_URL, media);
            values.put(UserTweetsSQLiteHelper.COLUMN_URL, url);
            values.put(UserTweetsSQLiteHelper.COLUMN_USERS, users);
            values.put(UserTweetsSQLiteHelper.COLUMN_HASHTAGS, hashtags);
            values.put(UserTweetsSQLiteHelper.COLUMN_USER_ID, userId);
            values.put(UserTweetsSQLiteHelper.COLUMN_ANIMATED_GIF, TweetLinkUtils.getGIFUrl(status, url));

            valueses[i] = values;
        }

        return insertMultiple(valueses);
    }

    private synchronized int insertMultiple(ContentValues[] allValues) {
        int rowsAdded = 0;
        long rowId;
        ContentValues values;

        if (database == null || !database.isOpen()) {
            open();
        }

        try {
            database.beginTransaction();

            for (ContentValues initialValues : allValues) {
                values = initialValues == null ? new ContentValues() : new ContentValues(initialValues);
                try {
                    rowId = database.insert(UserTweetsSQLiteHelper.TABLE_HOME, null, values);
                } catch (IllegalStateException e) {
                    return rowsAdded;
                }
                if (rowId > 0)
                    rowsAdded++;
            }

            database.setTransactionSuccessful();
        } catch (NullPointerException e)  {
            e.printStackTrace();
            return rowsAdded;
        } catch (SQLiteDatabaseLockedException e) {
            e.printStackTrace();
            return rowsAdded;
        } catch (IllegalStateException e) {
            // caught setting up the transaction I guess, shouldn't ever happen now.
            e.printStackTrace();
            return rowsAdded;
        } finally {
            try {
                database.endTransaction();
            } catch (Exception e) {
                // shouldn't happen unless it gets caught above from an illegal state
            }
        }

        return rowsAdded;
    }

    public synchronized void deleteTweet(long tweetId) {
        long id = tweetId;

        try {
            database.delete(UserTweetsSQLiteHelper.TABLE_HOME, UserTweetsSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        } catch (Exception e) {
            open();
            database.delete(UserTweetsSQLiteHelper.TABLE_HOME, UserTweetsSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        }
    }

    public synchronized void deleteAllTweets(long userId) {

        try {
            database.delete(UserTweetsSQLiteHelper.TABLE_HOME,
                    UserTweetsSQLiteHelper.COLUMN_USER_ID + " = " + userId, null);
        } catch (Exception e) {
            open();
            database.delete(UserTweetsSQLiteHelper.TABLE_HOME,
                    UserTweetsSQLiteHelper.COLUMN_USER_ID + " = " + userId, null);
        }
    }

    public synchronized Cursor getCursor(long userId) {

        String users = sharedPreferences.getString("muted_users", "");
        String rts = sharedPreferences.getString("muted_rts", "");
        String hashtags = sharedPreferences.getString("muted_hashtags", "");
        String expressions = sharedPreferences.getString("muted_regex", "");

        expressions = expressions.replaceAll("'", "''");

        String where = UserTweetsSQLiteHelper.COLUMN_USER_ID + " = " + userId;

        if (!users.equals("")) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + UserTweetsSQLiteHelper.COLUMN_RETWEETER + " NOT LIKE '" + s + "'";
            }
        }

        if (!hashtags.equals("")) {
            String[] split = hashtags.split(" ");
            for (String s : split) {
                where += " AND " + UserTweetsSQLiteHelper.COLUMN_HASHTAGS + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (!expressions.equals("")) {
            String[] split = expressions.split("   ");
            for (String s : split) {
                where += " AND " + UserTweetsSQLiteHelper.COLUMN_TEXT + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (noRetweets) {
            where += " AND " + UserTweetsSQLiteHelper.COLUMN_RETWEETER + " = '' OR " + UserTweetsSQLiteHelper.COLUMN_RETWEETER + " is NULL";
        } else if (!rts.equals("")) {
            String[] split = rts.split(" ");
            for (String s : split) {
                where += " AND " + HomeSQLiteHelper.COLUMN_RETWEETER + " NOT LIKE '" + s + "'";
            }
        }

        Cursor cursor;
        String sql = "SELECT COUNT(*) FROM " + UserTweetsSQLiteHelper.TABLE_HOME + " WHERE " + where;
        SQLiteStatement statement;
        try {
            statement = database.compileStatement(sql);
        } catch (Exception e) {
            open();
            statement = database.compileStatement(sql);
        }
        long count;
        try {
            count = statement.simpleQueryForLong();
        } catch (Exception e) {
            open();
            count = statement.simpleQueryForLong();
        }
        Log.v("talon_database", "list database has " + count + " entries");
        int maxListSize = AppSettings.getInstance(context).listSize;
        if (count > maxListSize) {
            try {
                cursor = database.query(UserTweetsSQLiteHelper.TABLE_HOME,
                        allColumns,
                        where,
                        null,
                        null,
                        null,
                        UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC",
                        (count - maxListSize) + "," + maxListSize);
            } catch (Exception e) {
                open();
                cursor = database.query(UserTweetsSQLiteHelper.TABLE_HOME,
                        allColumns,
                        where,
                        null,
                        null,
                        null,
                        UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC",
                        (count - maxListSize) + "," + maxListSize);
            }
        } else {
            try {
                cursor = database.query(UserTweetsSQLiteHelper.TABLE_HOME,
                        allColumns,
                        where,
                        null,
                        null,
                        null,
                        UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
            } catch (Exception e) {
                open();
                cursor = database.query(UserTweetsSQLiteHelper.TABLE_HOME,
                        allColumns,
                        where,
                        null,
                        null,
                        null,
                        UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
            }
        }

        return cursor;
    }

    public synchronized Cursor getTrimmingCursor(long userId) {

        String where = UserTweetsSQLiteHelper.COLUMN_USER_ID + " = " + userId;

        Cursor cursor;

        try {
            cursor = database.query(UserTweetsSQLiteHelper.TABLE_HOME,
                    allColumns,
                    where,
                    null,
                    null,
                    null,
                    UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(UserTweetsSQLiteHelper.TABLE_HOME,
                    allColumns,
                    where,
                    null,
                    null,
                    null,
                    UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public synchronized Cursor getWholeCursor() {

        Cursor cursor;
        try {
            cursor = database.query(UserTweetsSQLiteHelper.TABLE_HOME,
                    allColumns, null, null, null, null, UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            open();
            cursor = database.query(UserTweetsSQLiteHelper.TABLE_HOME,
                    allColumns, null, null, null, null, UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public synchronized long[] getLastIds(long listId) {
        long id[] = new long[] {0,0,0,0,0};

        Cursor cursor;
        try {
            cursor = getCursor(listId);
        } catch (Exception e) {
            return id;
        }

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

    public synchronized void deleteDups(long userId) {

        try {
            database.execSQL("DELETE FROM " + UserTweetsSQLiteHelper.TABLE_HOME +
                    " WHERE _id NOT IN (SELECT MIN(_id) FROM " + UserTweetsSQLiteHelper.TABLE_HOME +
                    " GROUP BY " + UserTweetsSQLiteHelper.COLUMN_TWEET_ID + ") AND " + UserTweetsSQLiteHelper.COLUMN_USER_ID + " = " + userId);
        } catch (Exception e) {
            open();
            database.execSQL("DELETE FROM " + UserTweetsSQLiteHelper.TABLE_HOME +
                    " WHERE _id NOT IN (SELECT MIN(_id) FROM " + UserTweetsSQLiteHelper.TABLE_HOME +
                    " GROUP BY " + UserTweetsSQLiteHelper.COLUMN_TWEET_ID + ") AND " + UserTweetsSQLiteHelper.COLUMN_USER_ID + " = " + userId);
        }
    }

    public synchronized void removeHTML(long tweetId, String text) {
        ContentValues cv = new ContentValues();
        cv.put(UserTweetsSQLiteHelper.COLUMN_TEXT, text);

        if (database == null || !database.isOpen()) {
            open();
        }

        try {
            database.update(UserTweetsSQLiteHelper.TABLE_HOME, cv, UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {tweetId + ""});
        } catch (Exception e) {
            close();
            open();
            database.update(UserTweetsSQLiteHelper.TABLE_HOME, cv, UserTweetsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {tweetId + ""});
        }
    }

    public synchronized void trimDatabase(long userId, int trimSize) {
        Cursor cursor = getTrimmingCursor(userId);
        if (cursor.getCount() > trimSize) {
            if (cursor.moveToPosition(cursor.getCount() - trimSize)) {
                try {
                    database.delete(
                            UserTweetsSQLiteHelper.TABLE_HOME,
                            UserTweetsSQLiteHelper.COLUMN_USER_ID + " = " + userId + " AND " +
                                    UserTweetsSQLiteHelper.COLUMN_ID + " < " + cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ID)),
                            null);
                } catch (Exception e) {
                    open();
                    database.delete(
                            UserTweetsSQLiteHelper.TABLE_HOME,
                            UserTweetsSQLiteHelper.COLUMN_USER_ID + " = " + userId + " AND " +
                                    UserTweetsSQLiteHelper.COLUMN_ID + " < " + cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ID)),
                            null);
                }
            }
        }

        try {
            cursor.close();
        } catch (Exception e) {

        }
    }
}
