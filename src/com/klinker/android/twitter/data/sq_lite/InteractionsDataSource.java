package com.klinker.android.twitter.data.sq_lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.utils.HtmlUtils;

import java.util.GregorianCalendar;

import twitter4j.Status;
import twitter4j.User;

/**
 * Created by luke on 1/7/14.
 */
public class InteractionsDataSource {

    // Database fields
    private SQLiteDatabase database;
    private InteractionsSQLiteHelper dbHelper;
    public String[] allColumns = {InteractionsSQLiteHelper.COLUMN_ID, InteractionsSQLiteHelper.COLUMN_UNREAD, InteractionsSQLiteHelper.COLUMN_TWEET_ID, InteractionsSQLiteHelper.COLUMN_ACCOUNT, InteractionsSQLiteHelper.COLUMN_TYPE,
            InteractionsSQLiteHelper.COLUMN_TEXT, InteractionsSQLiteHelper.COLUMN_TITLE, InteractionsSQLiteHelper.COLUMN_PRO_PIC,
            InteractionsSQLiteHelper.COLUMN_TIME, HomeSQLiteHelper.COLUMN_USERS, HomeSQLiteHelper.COLUMN_HASHTAGS };

    public static final int TYPE_FOLLOWER = 0;
    public static final int TYPE_RETWEET = 1;
    public static final int TYPE_FAVORITE = 2;
    public static final int TYPE_MENTION = 3;

    public InteractionsDataSource(Context context) {
        dbHelper = new InteractionsSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void createMention(Context context, Status status, int account) {
        ContentValues values = new ContentValues();
        long id = status.getId();
        long time = new GregorianCalendar().getTime().getTime(); // current time
        int type = TYPE_MENTION;

        User user = status.getUser();
        String users = "@" + user.getScreenName() + " ";
        String text = status.getText();
        String title = context.getResources().getString(R.string.mentioned_by) + " @" + user.getScreenName();

        values.put(InteractionsSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(InteractionsSQLiteHelper.COLUMN_TEXT, text);
        values.put(InteractionsSQLiteHelper.COLUMN_TWEET_ID, id);
        values.put(InteractionsSQLiteHelper.COLUMN_PRO_PIC, user.getBiggerProfileImageURL());
        values.put(InteractionsSQLiteHelper.COLUMN_TIME, time);
        values.put(InteractionsSQLiteHelper.COLUMN_UNREAD, 1);
        values.put(InteractionsSQLiteHelper.COLUMN_USERS, users);
        values.put(InteractionsSQLiteHelper.COLUMN_TYPE, type);
        values.put(InteractionsSQLiteHelper.COLUMN_TITLE, title);

        database.insert(InteractionsSQLiteHelper.TABLE_INTERACTIONS, null, values);
    }

    public void createInteraction(Context context, User source, Status status, int account, int type) {
        ContentValues values = new ContentValues();
        long id = status.getId();
        long time = new GregorianCalendar().getTime().getTime(); // current time

        String users = "@" + source.getScreenName() + " ";
        
        String text;
        if (status != null) {
            text = status.getText();
        } else {
            text = "";
        }

        User user = status.getUser();
        String title = "";

        switch (type) {
            case TYPE_FAVORITE:
                title = "@" + source.getScreenName() + " " + context.getResources().getString(R.string.favorited);
                break;
            case TYPE_RETWEET:
                title = "@" + source.getScreenName() + " " + context.getResources().getString(R.string.retweeted);
                break;
            case TYPE_FOLLOWER:
                title = "@" + source.getScreenName() + " " + context.getResources().getString(R.string.followed);
                break;
        }

        values.put(InteractionsSQLiteHelper.COLUMN_ACCOUNT, account);
        values.put(InteractionsSQLiteHelper.COLUMN_TEXT, text);
        values.put(InteractionsSQLiteHelper.COLUMN_TWEET_ID, id);
        values.put(InteractionsSQLiteHelper.COLUMN_PRO_PIC, user.getBiggerProfileImageURL());
        values.put(InteractionsSQLiteHelper.COLUMN_TIME, time);
        values.put(InteractionsSQLiteHelper.COLUMN_UNREAD, 1);
        values.put(InteractionsSQLiteHelper.COLUMN_USERS, users);
        values.put(InteractionsSQLiteHelper.COLUMN_TYPE, type);
        values.put(InteractionsSQLiteHelper.COLUMN_TITLE, title);

        database.insert(InteractionsSQLiteHelper.TABLE_INTERACTIONS, null, values);
    }

    public void updateInteraction(Context context, User source, Status status, int account, int type) {

    }

    public boolean interactionExists(long tweetId, int account) {
        Cursor cursor = database.query(InteractionsSQLiteHelper.TABLE_INTERACTIONS,
                allColumns, InteractionsSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + InteractionsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[]{account + "", tweetId + ""}, null, null, InteractionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        return cursor.getCount() > 0;
    }

    public void deleteInteraction(long tweetId) {
        long id = tweetId;
        database.delete(InteractionsSQLiteHelper.TABLE_INTERACTIONS, InteractionsSQLiteHelper.COLUMN_TWEET_ID
                + " = " + id, null);
    }

    public void deleteAllInteractions(int account) {
        database.delete(InteractionsSQLiteHelper.TABLE_INTERACTIONS,
                InteractionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null);
    }

    public Cursor getCursor(int account) {
        Cursor cursor = database.query(InteractionsSQLiteHelper.TABLE_INTERACTIONS,
                allColumns, InteractionsSQLiteHelper.COLUMN_ACCOUNT + " = " + account, null, null, null, InteractionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");

        return cursor;
    }

    public Cursor getUnreadCursor(int account) {

        Cursor cursor = database.query(InteractionsSQLiteHelper.TABLE_INTERACTIONS,
                allColumns, InteractionsSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + InteractionsSQLiteHelper.COLUMN_UNREAD + " = ?", new String[]{account + "", "1"}, null, null, InteractionsSQLiteHelper.COLUMN_TWEET_ID + " ASC");

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
            long tweetId = cursor.getLong(cursor.getColumnIndex(InteractionsSQLiteHelper.COLUMN_TWEET_ID));

            ContentValues cv = new ContentValues();
            cv.put(InteractionsSQLiteHelper.COLUMN_UNREAD, 0);

            database.update(InteractionsSQLiteHelper.TABLE_INTERACTIONS, cv, InteractionsSQLiteHelper.COLUMN_TWEET_ID + " = ?", new String[] {tweetId + ""});
        }

        cursor.close();
    }

    public void markAllRead(int account) {

        ContentValues cv = new ContentValues();
        cv.put(InteractionsSQLiteHelper.COLUMN_UNREAD, 0);

        database.update(InteractionsSQLiteHelper.TABLE_INTERACTIONS, cv, InteractionsSQLiteHelper.COLUMN_ACCOUNT + " = ? AND " + InteractionsSQLiteHelper.COLUMN_UNREAD + " = ?", new String[] {account + "", "1"});
    }

}
