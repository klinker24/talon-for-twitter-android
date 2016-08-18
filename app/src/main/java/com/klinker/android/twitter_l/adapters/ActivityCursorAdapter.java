package com.klinker.android.twitter_l.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.view.View;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.data.sq_lite.ActivityDataSource;
import com.klinker.android.twitter_l.data.sq_lite.ActivitySQLiteHelper;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.activities.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.utils.EmojiUtils;
import com.klinker.android.twitter_l.utils.text.TextUtils;
import com.klinker.android.twitter_l.utils.text.TouchableMovementMethod;

import java.util.Date;

public class ActivityCursorAdapter extends TimeLineCursorAdapter {

    public ActivityCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, false);
    }

    private int TYPE_COL;
    private int TITLE_COL;

    @Override
    public void init(boolean cont) {
        super.init(cont);

        TYPE_COL = cursor.getColumnIndex(ActivitySQLiteHelper.COLUMN_TYPE);
        TITLE_COL = cursor.getColumnIndex(ActivitySQLiteHelper.COLUMN_TITLE);
    }

    @Override
    public void bindView(final View view, Context mContext, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String title = cursor.getString(TITLE_COL);
        final long id = cursor.getLong(TWEET_COL);
        holder.tweetId = id;
        final String profilePic = cursor.getString(PRO_PIC_COL);
        holder.proPicUrl = profilePic;
        final String tweetText = cursor.getString(TEXT_COL);
        final String name = cursor.getString(NAME_COL);
        final String screenname = cursor.getString(SCREEN_NAME_COL);
        final String picUrl = cursor.getString(PIC_COL);
        holder.picUrl = picUrl;
        final long longTime = cursor.getLong(TIME_COL);
        final String otherUrl = cursor.getString(URL_COL);
        final String users = cursor.getString(USER_COL);
        final String hashtags = cursor.getString(HASHTAG_COL);
        holder.gifUrl = cursor.getString(GIF_COL);
        int type = cursor.getInt(TYPE_COL);

        String retweeter;
        try {
            retweeter = cursor.getString(RETWEETER_COL);
        } catch (Exception e) {
            retweeter = "";
        }

        if (retweeter == null) {
            retweeter = "";
        }

        Date date = new Date(longTime);
        holder.screenTV.setText(timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date));

        holder.name.setSingleLine(true);

        switch (type) {
            case ActivityDataSource.TYPE_NEW_FOLLOWER:
                holder.background.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String[] userArray = users.split(" ");

                        if (userArray.length == 1) {
                            Intent viewProfile = new Intent(context, ProfilePager.class);
                            viewProfile.putExtra("screenname", userArray[0].replace("@", "").replace(" ", ""));

                            context.startActivity(viewProfile);
                        } else {
                            displayUserDialog(userArray);
                        }
                    }
                });
                holder.profilePic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.background.performClick();
                    }
                });
                break;
            case ActivityDataSource.TYPE_FAVORITES:
            case ActivityDataSource.TYPE_RETWEETS:
                final String fRetweeter = retweeter;
                holder.background.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (holder.preventNextClick) {
                            holder.preventNextClick = false;
                            return;
                        }
                        String link = "";

                        boolean displayPic = holder.picUrl != null && !holder.picUrl.equals("") && !holder.picUrl.contains("youtu");
                        if (displayPic) {
                            link = holder.picUrl;
                        } else {
                            link = otherUrl.split("  ")[0];
                        }

                        String text = tweetText;
                        String[] split = tweetText.split(" ");
                        if (split.length > 2 && split[1].endsWith(":")) {
                            text = "";
                            for (int i = 2; i < split.length; i++) {
                                text += split[i] + " ";
                            }
                        }

                        Intent viewTweet = new Intent(context, TweetActivity.class);
                        viewTweet.putExtra("name", settings.myName);
                        viewTweet.putExtra("screenname", settings.myScreenName);
                        viewTweet.putExtra("time", longTime);
                        viewTweet.putExtra("tweet", text);
                        viewTweet.putExtra("retweeter", fRetweeter);
                        viewTweet.putExtra("webpage", link);
                        viewTweet.putExtra("picture", displayPic);
                        viewTweet.putExtra("other_links", otherUrl);
                        viewTweet.putExtra("tweetid", holder.tweetId);
                        viewTweet.putExtra("proPic", settings.myProfilePicUrl);
                        viewTweet.putExtra("users", users);
                        viewTweet.putExtra("hashtags", hashtags);
                        viewTweet.putExtra("animated_gif", holder.gifUrl);

                        context.startActivity(viewTweet);
                    }
                });

                holder.profilePic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        holder.background.performClick();
                    }
                });

                break;
            case ActivityDataSource.TYPE_MENTION:
                final String finalRetweeter = retweeter;
                holder.background.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (holder.preventNextClick) {
                            holder.preventNextClick = false;
                            return;
                        }
                        String link = "";

                        boolean displayPic = holder.picUrl != null && !holder.picUrl.equals("") && !holder.picUrl.contains("youtube");
                        if (displayPic) {
                            link = holder.picUrl;
                        } else {
                            link = otherUrl.split("  ")[0];
                        }

                        Intent viewTweet = new Intent(context, TweetActivity.class);
                        viewTweet.putExtra("name", name);
                        viewTweet.putExtra("screenname", screenname);
                        viewTweet.putExtra("time", longTime);
                        viewTweet.putExtra("tweet", tweetText);
                        viewTweet.putExtra("retweeter", finalRetweeter);
                        viewTweet.putExtra("webpage", link);
                        viewTweet.putExtra("picture", displayPic);
                        viewTweet.putExtra("other_links", otherUrl);
                        viewTweet.putExtra("tweetid", holder.tweetId);
                        viewTweet.putExtra("proPic", profilePic);
                        viewTweet.putExtra("users", users);
                        viewTweet.putExtra("hashtags", hashtags);
                        viewTweet.putExtra("animated_gif", holder.gifUrl);

                        context.startActivity(viewTweet);
                    }
                });

                holder.profilePic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent viewProfile = new Intent(context, ProfilePager.class);
                        viewProfile.putExtra("screenname", screenname);

                        context.startActivity(viewProfile);
                    }
                });

                break;
        }

        holder.name.setText(title);
        holder.tweet.setText(tweetText);

        Glide.with(context).load(holder.proPicUrl).into(holder.profilePic);

        mHandlers[currHandler].postDelayed(new Runnable() {
            @Override
            public void run() {
                if (holder.tweetId == id) {
                    holder.tweet.setSoundEffectsEnabled(false);
                    holder.tweet.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!TouchableMovementMethod.touched) {
                                // we need to manually set the background for click feedback because the spannable
                                // absorbs the click on the background
                                if (!holder.preventNextClick) {
                                    holder.background.getBackground().setState(new int[]{android.R.attr.state_pressed});
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            holder.background.getBackground().setState(new int[]{android.R.attr.state_empty});
                                        }
                                    }, 25);
                                }

                                holder.background.performClick();
                            }
                        }
                    });

                    holder.tweet.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            if (!TouchableMovementMethod.touched) {
                                holder.background.performLongClick();
                                holder.preventNextClick = true;
                            }
                            return false;
                        }
                    });

                    TextUtils.linkifyText(context, holder.tweet, holder.background, true, otherUrl, false);
                    TextUtils.linkifyText(context, holder.retweeter, holder.background, true, "", false);

                }
            }
        }, 400);
        currHandler++;

        if (currHandler == 10) {
            currHandler = 0;
        }
    }

    public void displayUserDialog(final String[] users) {
        new AlertDialog.Builder(context)
                .setItems(users, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String s = users[i];

                        Intent viewProfile = new Intent(context, ProfilePager.class);
                        viewProfile.putExtra("screenname", s.replace("@", "").replace(" ", ""));

                        context.startActivity(viewProfile);
                    }
                })
                .create()
                .show();
    }
}
