package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.preference.PreferenceManager;
import android.util.Log;

import com.klinker.android.twitter.utils.TweetLinkUtils;

import twitter4j.Status;


public class ListDataSource {

    // provides access to the database
    public static ListDataSource dataSource = null;

    /*

    This is used so that we don't have to open and close the database on different threads or fragments
    every time. This will facilitate it between all of them to avoid Illegal State Exceptions.

     */
    public static ListDataSource getInstance(Context context) {

        // if the datasource isn't open or it the object is null
        try {
            if (dataSource == null || !dataSource.getDatabase().isOpen()) {
                dataSource = new ListDataSource(context); // create the database
                dataSource.open(); // open the database
            }
        } catch (Exception e) {
            dataSource = new ListDataSource(context); // create the database
            dataSource.open(); // open the database
        }

        return dataSource;
    }

    // Database fields
    private SQLiteDatabase database;
    private ListSQLiteHelper dbHelper;
    private Context context;
    private int timelineSize;
    private boolean noRetweets;
    private SharedPreferences sharedPreferences;
    public static String[] allColumns = { ListSQLiteHelper.COLUMN_ID,
            ListSQLiteHelper.COLUMN_TWEET_ID,
            ListSQLiteHelper.COLUMN_ACCOUNT,
            ListSQLiteHelper.COLUMN_TEXT,
            ListSQLiteHelper.COLUMN_NAME,
            ListSQLiteHelper.COLUMN_PRO_PIC,
            ListSQLiteHelper.COLUMN_SCREEN_NAME,
            ListSQLiteHelper.COLUMN_TIME,
            ListSQLiteHelper.COLUMN_PIC_URL,
            ListSQLiteHelper.COLUMN_RETWEETER,
            ListSQLiteHelper.COLUMN_URL,
            ListSQLiteHelper.COLUMN_USERS,
            ListSQLiteHelper.COLUMN_HASHTAGS };

    public ListDataSource(Context context) {
        dbHelper = new ListSQLiteHelper(context);
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
        try {
            dbHelper.close();
        } catch (Exception e) {

        }
        database = null;
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public ListSQLiteHelper getHelper() {
        return dbHelper;
    }

    public void createTweet(Status status, int listId) {
        ContentValues values = new ContentValues();
        String originalName = "";
        long time = status.getCreatedAt().getTime();
        long id = status.getId();

        if(status.isRetweet()) {
            originalName = status.getUser().getScreenName();
            status = status.getRetweetedStatus();
        }

        String[] html = TweetLinkUtils.getHtmlStatus(status);
        String text = html[0];
        String media = html[1];
        String url = html[2];
        String hashtags = html[3];
        String users = html[4];

        values.put(ListSQLiteHelper.COLUMN_TEXT, text);
        values.put(ListSQLiteHelper.COLUMN_TWEET_ID, id);
        values.put(ListSQLiteHelper.COLUMN_NAME, status.getUser().getName());
        values.put(ListSQLiteHelper.COLUMN_PRO_PIC, status.getUser().getBiggerProfileImageURL());
        values.put(ListSQLiteHelper.COLUMN_SCREEN_NAME, status.getUser().getScreenName());
        values.put(ListSQLiteHelper.COLUMN_TIME, time);
        values.put(ListSQLiteHelper.COLUMN_RETWEETER, originalName);
        values.put(ListSQLiteHelper.COLUMN_PIC_URL, media);
        values.put(ListSQLiteHelper.COLUMN_URL, url);
        values.put(ListSQLiteHelper.COLUMN_USERS, users);
        values.put(ListSQLiteHelper.COLUMN_HASHTAGS, hashtags);
        values.put(ListSQLiteHelper.COLUMN_LIST_ID, listId);

        try {
            database.insert(ListSQLiteHelper.TABLE_HOME, null, values);
        } catch (Exception e) {
            close();
            open();
            database.insert(ListSQLiteHelper.TABLE_HOME, null, values);
        }
    }

    public void deleteTweet(long tweetId) {
        long id = tweetId;

        try {
            database.delete(ListSQLiteHelper.TABLE_HOME, ListSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        } catch (Exception e) {
            close();
            open();
            database.delete(ListSQLiteHelper.TABLE_HOME, ListSQLiteHelper.COLUMN_TWEET_ID
                    + " = " + id, null);
        }
    }

    public void deleteAllTweets(int listNumber) {

        try {
            database.delete(ListSQLiteHelper.TABLE_HOME,
                    ListSQLiteHelper.COLUMN_LIST_ID + " = " + listNumber, null);
        } catch (Exception e) {
            close();
            open();
            database.delete(ListSQLiteHelper.TABLE_HOME,
                    ListSQLiteHelper.COLUMN_LIST_ID + " = " + listNumber, null);
        }
    }

    public Cursor getCursor(int listId) {

        String users = sharedPreferences.getString("muted_users", "");
        String hashtags = sharedPreferences.getString("muted_hashtags", "");
        String expressions = sharedPreferences.getString("muted_regex", "");
        String where = ListSQLiteHelper.COLUMN_LIST_ID + " = " + listId;

        if (!users.equals("")) {
            String[] split = users.split(" ");
            for (String s : split) {
                where += " AND " + ListSQLiteHelper.COLUMN_SCREEN_NAME + " NOT LIKE '" + s + "'";
            }

            for (String s : split) {
                where += " AND " + ListSQLiteHelper.COLUMN_RETWEETER + " NOT LIKE '" + s + "'";
            }
        }

        if (!hashtags.equals("")) {
            String[] split = hashtags.split(" ");
            for (String s : split) {
                where += " AND " + ListSQLiteHelper.COLUMN_HASHTAGS + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (!expressions.equals("")) {
            String[] split = expressions.split("   ");
            for (String s : split) {
                where += " AND " + ListSQLiteHelper.COLUMN_TEXT + " NOT LIKE " + "'%" + s + "%'";
            }
        }

        if (noRetweets) {
            where += " AND " + ListSQLiteHelper.COLUMN_RETWEETER + " = '' OR " + ListSQLiteHelper.COLUMN_RETWEETER + " is NULL";
        }

        Cursor cursor;
        try {
            String sql = "SELECT COUNT(*) FROM " + ListSQLiteHelper.TABLE_HOME + " WHERE " + where;
            SQLiteStatement statement = database.compileStatement(sql);
            long count = statement.simpleQueryForLong();
            Log.v("talon_database", "list database has " + count + " entries");
            if (count > 400) {
                cursor = database.query(ListSQLiteHelper.TABLE_HOME,
                        allColumns,
                        where,
                        null,
                        null,
                        null,
                        ListSQLiteHelper.COLUMN_TWEET_ID + " ASC",
                        (count - 400) + "," + 400);
            } else {
                cursor = database.query(ListSQLiteHelper.TABLE_HOME,
                        allColumns,
                        where,
                        null,
                        null,
                        null,
                        ListSQLiteHelper.COLUMN_TWEET_ID + " ASC");
            }
        } catch (Exception e) {
            close();
            open();
            String sql = "SELECT COUNT(*) FROM " + ListSQLiteHelper.TABLE_HOME + " WHERE " + where;
            SQLiteStatement statement = database.compileStatement(sql);
            long count = statement.simpleQueryForLong();
            Log.v("talon_database", "list database has " + count + " entries");
            if (count > 400) {
                cursor = database.query(ListSQLiteHelper.TABLE_HOME,
                        allColumns,
                        where,
                        null,
                        null,
                        null,
                        ListSQLiteHelper.COLUMN_TWEET_ID + " ASC",
                        (count - 400) + "," + 400);
            } else {
                cursor = database.query(ListSQLiteHelper.TABLE_HOME,
                        allColumns,
                        where,
                        null,
                        null,
                        null,
                        ListSQLiteHelper.COLUMN_TWEET_ID + " ASC");
            }
        }

        return cursor;
    }

    public Cursor getWholeCursor() {

        Cursor cursor;
        try {
            cursor = database.query(ListSQLiteHelper.TABLE_HOME,
                    allColumns, null, null, null, null, ListSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        } catch (Exception e) {
            close();
            open();
            cursor = database.query(ListSQLiteHelper.TABLE_HOME,
                    allColumns, null, null, null, null, ListSQLiteHelper.COLUMN_TWEET_ID + " ASC");
        }

        return cursor;
    }

    public long[] getLastIds(int listId) {
        long id[] = new long[5];

        Cursor cursor = getCursor(listId);

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

    public void deleteDups(int list) {

        try {
            database.execSQL("DELETE FROM " + ListSQLiteHelper.TABLE_HOME +
                    " WHERE _id NOT IN (SELECT MIN(_id) FROM " + ListSQLiteHelper.TABLE_HOME +
                    " GROUP BY " + ListSQLiteHelper.COLUMN_TWEET_ID + ") AND " + ListSQLiteHelper.COLUMN_LIST_ID + " = " + list);
        } catch (Exception e) {
            close();
            open();
            database.execSQL("DELETE FROM " + ListSQLiteHelper.TABLE_HOME +
                    " WHERE _id NOT IN (SELECT MIN(_id) FROM " + ListSQLiteHelper.TABLE_HOME +
                    " GROUP BY " + ListSQLiteHelper.COLUMN_TWEET_ID + ") AND " + ListSQLiteHelper.COLUMN_LIST_ID + " = " + list);
        }
    }

    public void removeHTML(long tweetId, String text) {
        ContentValues cv = new ContentValues();
        cv.put(ListSQLiteHelper.COLUMN_TEXT, text);

        database.update(ListSQLiteHelper.TABLE_HOME, cv, ListSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {tweetId + ""});
    }
}
