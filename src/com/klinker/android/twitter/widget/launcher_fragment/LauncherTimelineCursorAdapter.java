package com.klinker.android.twitter.widget.launcher_fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.launcher.api.ResourceHelper;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter.manipulations.widgets.NetworkedCacheableImageView;

public class LauncherTimelineCursorAdapter extends TimeLineCursorAdapter {

    private ResourceHelper helper;

    public LauncherTimelineCursorAdapter(Context context, Cursor cursor, boolean isDM, boolean isHomeTimeline) {
        super(context, cursor, isDM, isHomeTimeline);
        helper = new ResourceHelper(context, "com.klinker.android.twitter");
    }

    public LauncherTimelineCursorAdapter(Context context, Cursor cursor, boolean isDM) {
        super(context, cursor, isDM);
        helper = new ResourceHelper(context, "com.klinker.android.twitter");
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();
        if (settings.addonTheme) {
            try {
                Context viewContext = null;

                if (res == null) {
                    res = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                }

                try {
                    viewContext = context.createPackageContext(settings.addonThemePackage, Context.CONTEXT_IGNORE_SECURITY);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (res != null && viewContext != null) {
                    int id = res.getIdentifier("tweet", "layout", settings.addonThemePackage);
                    v = LayoutInflater.from(viewContext).inflate(res.getLayout(id), null);

                    holder.name = (TextView) v.findViewById(res.getIdentifier("name", "id", settings.addonThemePackage));
                    holder.screenTV = (TextView) v.findViewById(res.getIdentifier("screenname", "id", settings.addonThemePackage));
                    holder.profilePic = (ImageView) v.findViewById(res.getIdentifier("profile_pic", "id", settings.addonThemePackage));
                    holder.time = (TextView) v.findViewById(res.getIdentifier("time", "id", settings.addonThemePackage));
                    holder.tweet = (TextView) v.findViewById(res.getIdentifier("tweet", "id", settings.addonThemePackage));
                    holder.reply = (EditText) v.findViewById(res.getIdentifier("reply", "id", settings.addonThemePackage));
                    holder.favorite = (ImageButton) v.findViewById(res.getIdentifier("favorite", "id", settings.addonThemePackage));
                    holder.retweet = (ImageButton) v.findViewById(res.getIdentifier("retweet", "id", settings.addonThemePackage));
                    holder.favCount = (TextView) v.findViewById(res.getIdentifier("fav_count", "id", settings.addonThemePackage));
                    holder.retweetCount = (TextView) v.findViewById(res.getIdentifier("retweet_count", "id", settings.addonThemePackage));
                    holder.expandArea = (LinearLayout) v.findViewById(res.getIdentifier("expansion", "id", settings.addonThemePackage));
                    holder.replyButton = (ImageButton) v.findViewById(res.getIdentifier("reply_button", "id", settings.addonThemePackage));
                    holder.image = (ImageView) v.findViewById(res.getIdentifier("image", "id", settings.addonThemePackage));
                    holder.retweeter = (TextView) v.findViewById(res.getIdentifier("retweeter", "id", settings.addonThemePackage));
                    holder.background = (LinearLayout) v.findViewById(res.getIdentifier("background", "id", settings.addonThemePackage));
                    holder.charRemaining = (TextView) v.findViewById(res.getIdentifier("char_remaining", "id", settings.addonThemePackage));
                    holder.playButton = (ImageView) v.findViewById(res.getIdentifier("play_button", "id", settings.addonThemePackage));
                    try {
                        holder.quoteButton = (ImageButton) v.findViewById(res.getIdentifier("quote_button", "id", settings.addonThemePackage));
                        holder.shareButton = (ImageButton) v.findViewById(res.getIdentifier("share_button", "id", settings.addonThemePackage));
                    } catch (Exception e) {
                        // they don't exist because the theme was made before they were added
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
                v = helper.getLayout("launcher_frag_tweet_dark");//inflater.inflate(layout, viewGroup, false);

                holder.name = (TextView) v.findViewById(helper.getId("name"));
                holder.screenTV = (TextView) v.findViewById(helper.getId("screenname"));
                holder.profilePic = (ImageView) v.findViewById(helper.getId("profile_pic"));
                holder.time = (TextView) v.findViewById(helper.getId("time"));
                holder.tweet = (TextView) v.findViewById(helper.getId("tweet"));
                holder.reply = (EditText) v.findViewById(helper.getId("reply"));
                holder.favorite = (ImageButton) v.findViewById(helper.getId("favorite"));
                holder.retweet = (ImageButton) v.findViewById(helper.getId("retweet"));
                holder.favCount = (TextView) v.findViewById(helper.getId("fav_count"));
                holder.retweetCount = (TextView) v.findViewById(helper.getId("retweet_count"));
                holder.expandArea = (LinearLayout) v.findViewById(helper.getId("expansion"));
                holder.replyButton = (ImageButton) v.findViewById(helper.getId("reply_button"));
                holder.image = (ImageView) v.findViewById(helper.getId("image"));
                holder.retweeter = (TextView) v.findViewById(helper.getId("retweeter"));
                holder.background = (LinearLayout) v.findViewById(helper.getId("background"));
                holder.charRemaining = (TextView) v.findViewById(helper.getId("char_remaining"));
                holder.playButton = (ImageView) v.findViewById(helper.getId("play_button"));
                try {
                    holder.quoteButton = (ImageButton) v.findViewById(helper.getId("quote_button"));
                    holder.shareButton = (ImageButton) v.findViewById(helper.getId("share_button"));
                } catch (Exception x) {
                    // theme was made before they were added
                }

            }
        } else {
            v = helper.getLayout("launcher_frag_tweet_dark");//inflater.inflate(layout, viewGroup, false);

            holder.name = (TextView) v.findViewById(helper.getId("name"));
            holder.screenTV = (TextView) v.findViewById(helper.getId("screenname"));
            holder.profilePic = (ImageView) v.findViewById(helper.getId("profile_pic"));
            holder.time = (TextView) v.findViewById(helper.getId("time"));
            holder.tweet = (TextView) v.findViewById(helper.getId("tweet"));
            holder.reply = (EditText) v.findViewById(helper.getId("reply"));
            holder.favorite = (ImageButton) v.findViewById(helper.getId("favorite"));
            holder.retweet = (ImageButton) v.findViewById(helper.getId("retweet"));
            holder.favCount = (TextView) v.findViewById(helper.getId("fav_count"));
            holder.retweetCount = (TextView) v.findViewById(helper.getId("retweet_count"));
            holder.expandArea = (LinearLayout) v.findViewById(helper.getId("expansion"));
            holder.replyButton = (ImageButton) v.findViewById(helper.getId("reply_button"));
            holder.image = (ImageView) v.findViewById(helper.getId("image"));
            holder.retweeter = (TextView) v.findViewById(helper.getId("retweeter"));
            holder.background = (LinearLayout) v.findViewById(helper.getId("background"));
            holder.charRemaining = (TextView) v.findViewById(helper.getId("char_remaining"));
            holder.playButton = (ImageView) v.findViewById(helper.getId("play_button"));
            try {
                holder.quoteButton = (ImageButton) v.findViewById(helper.getId("quote_button"));
                holder.shareButton = (ImageButton) v.findViewById(helper.getId("share_button"));
            } catch (Exception x) {
                // theme was made before they were added
            }
        }

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.screenTV.setTextSize(settings.textSize - 2);
        holder.name.setTextSize(settings.textSize + 4);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);
        holder.favCount.setTextSize(settings.textSize + 1);
        holder.retweetCount.setTextSize(settings.textSize + 1);
        holder.reply.setTextSize(settings.textSize);

        v.setTag(holder);

        return v;
    }
}
