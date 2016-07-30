package com.klinker.android.twitter_l.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.TweetView;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.activities.media_viewer.PhotoPagerActivity;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.activities.media_viewer.PhotoViewerActivity;
import com.klinker.android.twitter_l.utils.*;
import com.klinker.android.twitter_l.utils.text.TextUtils;

import java.util.Date;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class DMCursorAdapter extends TimeLineCursorAdapter {

    public DMCursorAdapter(Context context, Cursor cursor, boolean isDM) {
        super(context, cursor, isDM);
    }

    @Override
    public void bindView(final View view, Context mContext, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (holder.embeddedTweet.getChildCount() > 0 || holder.embeddedTweet.getVisibility() == View.VISIBLE) {
            holder.embeddedTweet.removeAllViews();
            holder.embeddedTweet.setVisibility(View.GONE);
            holder.embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);
        }

        final long id = cursor.getLong(TWEET_COL);
        holder.tweetId = id;
        final String profilePic = cursor.getString(PRO_PIC_COL);
        holder.proPicUrl = profilePic;
        final String tweetTexts = cursor.getString(TEXT_COL);
        final String name = cursor.getString(NAME_COL);
        final String screenname = cursor.getString(SCREEN_NAME_COL);
        final String picUrl = cursor.getString(PIC_COL);
        holder.picUrl = picUrl;
        final long longTime = cursor.getLong(TIME_COL);
        final String otherUrl = cursor.getString(URL_COL);
        final String users = cursor.getString(USER_COL);
        final String hashtags = cursor.getString(HASHTAG_COL);

        holder.gifUrl = cursor.getString(GIF_COL);

        boolean inAConversation;
        if (hasConvo) {
            inAConversation = cursor.getInt(CONVO_COL) == 1;
        } else {
            inAConversation = false;
        }

        if (inAConversation) {
            if (holder.isAConversation.getVisibility() != View.VISIBLE) {
                holder.isAConversation.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.isAConversation.getVisibility() != View.GONE) {
                holder.isAConversation.setVisibility(View.GONE);
            }
        }

        final String tweetText = tweetTexts;

        holder.background.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new DeleteTweet().execute("" + holder.tweetId);
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

                builder.setTitle(R.string.delete_direct_message);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });


        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewProfile = new Intent(context, ProfilePager.class);
                viewProfile.putExtra("name", name);
                viewProfile.putExtra("screenname", screenname);
                viewProfile.putExtra("proPic", holder.proPicUrl);
                viewProfile.putExtra("tweetid", holder.tweetId);
                viewProfile.putExtra("retweet", holder.retweeter.getVisibility() == View.VISIBLE);
                viewProfile.putExtra("long_click", false);

                if (isHomeTimeline) {
                    sharedPrefs.edit()
                            .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                            .apply();
                }

                context.startActivity(viewProfile);
            }
        });

        holder.screenTV.setText("@" + screenname);
        holder.name.setText(name);

        if (!settings.absoluteDate) {
            holder.time.setText(Utils.getTimeAgo(longTime, context));
        } else {
            Date date = new Date(longTime);
            holder.time.setText(timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date));
        }

        boolean removeLastCharacters = false;
        boolean embeddedTweetFound = false;

        if (settings.inlinePics && tweetText.contains("pic.twitter.com/")) {
            if (tweetText.lastIndexOf(".") == tweetText.length() - 1) {
                removeLastCharacters = true;
            }
        } else if (settings.inlinePics && TweetView.embeddedTweetPattern.matcher(tweetText).find()) {
            embeddedTweetFound = true;

            if (tweetText.lastIndexOf(".") == tweetText.length() - 1) {
                removeLastCharacters = true;
            }
        }

        try {
            holder.tweet.setText(removeLastCharacters ?
                    tweetText.substring(0, tweetText.length() - (embeddedTweetFound ? 33 : 25)) :
                    tweetText);
        } catch (Exception e) {
            holder.tweet.setText(tweetText);
        }

        boolean picture = false;

        if(holder.picUrl != null && !holder.picUrl.equals("")) {

            if (holder.imageHolder.getVisibility() == View.GONE) {
                holder.imageHolder.setVisibility(View.VISIBLE);
            }


            if (holder.playButton.getVisibility() == View.VISIBLE) {
                holder.playButton.setVisibility(View.GONE);
            }

            holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (isHomeTimeline) {
                        sharedPrefs.edit()
                                .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                .apply();
                    }

                    if (holder.picUrl.contains(" ") && !MainActivity.isPopup) {
                        PhotoPagerActivity.startActivity(context, id, holder.picUrl, 0);
                    } else {
                        PhotoViewerActivity.startActivity(context, id, holder.picUrl, holder.image);
                    }
                }
            });

            picture = true;
        } else {
            if (holder.imageHolder.getVisibility() != View.GONE) {
                holder.imageHolder.setVisibility(View.GONE);
            }

            if (holder.playButton.getVisibility() == View.VISIBLE) {
                holder.playButton.setVisibility(View.GONE);
            }
        }

        if (holder.retweeter.getVisibility() == View.VISIBLE) {
            holder.retweeter.setVisibility(View.GONE);
        }

        if (picture) {
            if (settings.picturesType != AppSettings.CONDENSED_TWEETS) {
                if (settings.preCacheImages){
                    Glide.with(context).load(holder.picUrl).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(null).into(holder.image);
                } else {
                    Glide.with(context).load(holder.picUrl).centerCrop().placeholder(null).into(holder.image);
                }
            } else {
                if (settings.preCacheImages){
                    Glide.with(context).load(holder.picUrl).fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(null).into(holder.image);
                } else {
                    Glide.with(context).load(holder.picUrl).fitCenter().placeholder(null).into(holder.image);
                }
            }
        }

        if (settings.preCacheImages) {
            Glide.with(context).load(holder.proPicUrl).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(null).into(holder.profilePic);
        } else {
            Glide.with(context).load(holder.proPicUrl).placeholder(null).into(holder.profilePic);
        }

        TextUtils.linkifyText(context, holder.tweet, holder.background, true, otherUrl, false);

        if (otherUrl != null && otherUrl.contains("/status/")) {
            holder.embeddedTweet.setVisibility(View.VISIBLE);
            if (!tryImmediateEmbeddedLoad(holder, otherUrl)) {
                loadEmbeddedTweet(holder, otherUrl);
            }
        }
    }

    private boolean tryImmediateEmbeddedLoad(final ViewHolder holder, String otherUrl) {
        Long embeddedId = 0l;
        for (String u : otherUrl.split(" ")) {
            if (u.contains("/status/")) {
                embeddedId = TweetLinkUtils.getTweetIdFromLink(u);
                break;
            }
        }

        if (embeddedId != 0l && quotedTweets.containsKey(embeddedId)) {
            Status status = quotedTweets.get(embeddedId);
            TweetView v = new TweetView(context, status);
            v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
            v.setSmallImage(true);

            holder.embeddedTweet.removeAllViews();
            holder.embeddedTweet.addView(v.getView());
            holder.embeddedTweet.setMinimumHeight(0);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        try {
            if (!cursor.moveToPosition(cursor.getCount() - 1 - position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
        } catch (Exception e) {
            ((Activity)context).recreate();
            return null;
        }

        View v;
        if (convertView == null) {
            v = newView(context, cursor, parent);
        } else {
            v = convertView;
        }

        bindView(v, context, cursor);

        return v;
    }

    public Twitter getTwitter() {
        if (secondAcc) {
            return Utils.getSecondTwitter(context);
        } else {
            return Utils.getTwitter(context, settings);
        }
    }

    class DeleteTweet extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = getTwitter();

            try {
                long tweetId = Long.parseLong(urls[0]);

                DMDataSource source = DMDataSource.getInstance(context);
                source.deleteTweet(tweetId);

                twitter.destroyDirectMessage(tweetId);

                return true;
            } catch (TwitterException e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean deleted) {
            if (deleted) {
                Toast.makeText(context, context.getResources().getString(R.string.deleted_tweet), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void loadEmbeddedTweet(final ViewHolder holder, final String otherUrls) {

        holder.embeddedTweet.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Long embeddedId = 0l;
                for (String u : otherUrls.split(" ")) {
                    if (u.contains("/status/")) {
                        embeddedId = TweetLinkUtils.getTweetIdFromLink(u);
                        break;
                    }
                }

                if (embeddedId != 0l) {
                    Status status = null;
                    if (quotedTweets.containsKey(embeddedId)) {
                        status = quotedTweets.get(embeddedId);
                    } else {
                        try {
                            status = getTwitter().showStatus(embeddedId);
                            quotedTweets.put(embeddedId, status);
                        } catch (Exception e) {

                        }
                    }

                    final Status embedded = status;

                    if (status != null) {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TweetView v = new TweetView(context, embedded);
                                v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
                                v.setSmallImage(true);

                                holder.embeddedTweet.removeAllViews();
                                holder.embeddedTweet.addView(v.getView());

                                holder.embeddedTweet.setMinimumHeight(0);
                            }
                        });
                    }
                }
            }
        }).start();
    }
}
