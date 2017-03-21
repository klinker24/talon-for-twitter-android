package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.compose.ComposeActivity;
import com.klinker.android.twitter_l.activities.compose.ComposeSecAccActivity;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteTweetsDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.util.ArrayList;

import twitter4j.HashtagEntity;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.UserMentionEntity;

public class TweetButtonUtils {

    private ExpansionViewHelper.TweetLoaded tweetLoaded;

    private Status status;
    private Context context;
    private AppSettings settings;

    private TextView tweetCounts;
    private TextView tweetVia;
    private ImageButton retweetButton;
    private ImageButton likeButton;

    private boolean secondAcc;
    private String replyText;

    public TweetButtonUtils(Context context) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);
    }

    public void setIsSecondAcc(boolean secondAcc) {
        this.secondAcc = secondAcc;
    }

    public void setUpButtons(Status s, View countsRoot, View buttonsRoot, boolean showOverflow) {
        if (s.isRetweet()) {
            s = s.getRetweetedStatus();
        }

        this.status = s;
        this.replyText = generateReplyText();

        tweetCounts = (TextView) countsRoot.findViewById(R.id.tweet_counts);
        tweetVia = (TextView) countsRoot.findViewById(R.id.tweet_source);
        likeButton = (ImageButton) buttonsRoot.findViewById(R.id.like_button);
        retweetButton = (ImageButton) buttonsRoot.findViewById(R.id.retweet_button);
        final ImageButton composeButton = (ImageButton) buttonsRoot.findViewById(R.id.compose_button);
        final ImageButton shareButton = (ImageButton) buttonsRoot.findViewById(R.id.share_button);
        final ImageButton quoteButton = (ImageButton) buttonsRoot.findViewById(R.id.quote_button);
        final ImageButton overflowButton = (ImageButton) buttonsRoot.findViewById(R.id.overflow_button);

        if (!settings.darkTheme) {
            likeButton.setColorFilter(Color.BLACK);
            retweetButton.setColorFilter(Color.BLACK);
            shareButton.setColorFilter(Color.BLACK);
            composeButton.setColorFilter(Color.BLACK);
            overflowButton.setColorFilter(Color.BLACK);
            quoteButton.setColorFilter(Color.BLACK);
        }

        if (showOverflow) {
            overflowButton.setVisibility(View.VISIBLE);
        }

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (status.isFavorited() || !settings.crossAccActions) {
                    favoriteStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
                } else if (settings.crossAccActions) {
                    // dialog for favoriting
                    String[] options = new String[3];

                    options[0] = "@" + settings.myScreenName;
                    options[1] = "@" + settings.secondScreenName;
                    options[2] = context.getString(R.string.both_accounts);

                    new AlertDialog.Builder(context)
                            .setItems(options, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int item) {
                                    favoriteStatus(item + 1);
                                }
                            })
                            .create().show();
                }
            }
        });

        retweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (status.isRetweetedByMe() || !settings.crossAccActions) {
                    retweetStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
                } else {
                    // dialog for favoriting
                    String[] options = new String[3];

                    options[0] = "@" + settings.myScreenName;
                    options[1] = "@" + settings.secondScreenName;
                    options[2] = context.getString(R.string.both_accounts);

                    new AlertDialog.Builder(context)
                            .setItems(options, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int item) {
                                    retweetStatus(item + 1);
                                }
                            })
                            .create().show();
                }
            }
        });

        retweetButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.remove_retweet))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new RemoveRetweet().execute();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create()
                        .show();
                return false;
            }
        });

        quoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = restoreLinks(status.getText());

                switch (AppSettings.getInstance(context).quoteStyle) {
                    case AppSettings.QUOTE_STYLE_TWITTER:
                        text = " " + "https://twitter.com/" + status.getUser().getScreenName() + "/status/" + status.getId();
                        break;
                    case AppSettings.QUOTE_STYLE_TALON:
                        text = restoreLinks(text);
                        text = "\"@" + status.getUser().getScreenName() + ": " + text + "\" ";
                        break;
                    case AppSettings.QUOTE_STYLE_RT:
                        text = restoreLinks(text);
                        text = " RT @" + status.getUser().getScreenName() + ": " + text;
                        break;
                    case AppSettings.QUOTE_STYLE_VIA:
                        text = restoreLinks(text);
                        text = text + " via @" + status.getUser().getScreenName();
                }

                Intent quote;
                if (!secondAcc) {
                    quote = new Intent(context, ComposeActivity.class);
                } else {
                    quote = new Intent(context, ComposeSecAccActivity.class);
                }
                quote.putExtra("user", text);
                quote.putExtra("id", status.getId());
                quote.putExtra("reply_to_text", "@" + status.getUser().getScreenName() + ": " + status.getText());

                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                        v.getMeasuredWidth(), v.getMeasuredHeight());
                quote.putExtra("already_animated", true);

                context.startActivity(quote, opts.toBundle());
            }
        });

        composeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent compose;
                if (!secondAcc) {
                    compose = new Intent(context, ComposeActivity.class);
                } else {
                    compose = new Intent(context, ComposeSecAccActivity.class);
                }
                compose.putExtra("user", replyText);
                compose.putExtra("id", status.getId());
                compose.putExtra("reply_to_text", "@" + status.getUser().getScreenName() + ": " + status.getText());

                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                        v.getMeasuredWidth(), v.getMeasuredHeight());
                compose.putExtra("already_animated", true);

                context.startActivity(compose, opts.toBundle());
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String screenName = status.getUser().getScreenName();
                String text = "https://twitter.com/" + screenName + "/status/" + status.getId() + "\n\n" + restoreLinks(status.getText());
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT, "Tweet from @" + screenName);
                share.putExtra(Intent.EXTRA_TEXT, text);

                context.startActivity(Intent.createChooser(share, "Share with:"));
            }
        });

        if (status.getUser().isProtected()) {
            retweetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(context, R.string.protected_account_retweet, Toast.LENGTH_SHORT).show();
                }
            });

            quoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(context, R.string.protected_account_quote, Toast.LENGTH_SHORT).show();
                }
            });
        }

        updateTweetCounts(status);
    }


    private final int TYPE_ACC_ONE = 1;
    private final int TYPE_ACC_TWO = 2;

    private void updateTweetCounts(twitter4j.Status status) {
        if (status.isRetweet()) {
            status = status.getRetweetedStatus();
        }

        this.status = status;

        AppSettings settings = AppSettings.getInstance(context);

        String retweets = status.getRetweetCount() == 1 ? context.getString(R.string.retweet).toLowerCase() : context.getString(R.string.new_retweets);
        String likes = status.getFavoriteCount() == 1 ? context.getString(R.string.favorite).toLowerCase() : context.getString(R.string.new_favorites);
        String tweetCount = status.getFavoriteCount() + " <b>" + likes + "</b>  " +
                (!status.getUser().isProtected() ? status.getRetweetCount() + " <b>" + retweets + "</b> " : "");
        tweetCounts.setText(Html.fromHtml(tweetCount));

        if (status.isRetweetedByMe()) {
            retweetButton.setImageResource(R.drawable.ic_action_repeat_dark);
            retweetButton.setColorFilter(settings.themeColors.accentColor, PorterDuff.Mode.MULTIPLY);
        } else {
            if(!settings.darkTheme) {
                retweetButton.setImageResource(R.drawable.ic_action_repeat_light);
                retweetButton.setColorFilter(Color.BLACK);
            } else {
                retweetButton.clearColorFilter();
            }

        }

        if (status.isFavorited()) {
            likeButton.setImageResource(R.drawable.ic_heart_dark);
            likeButton.setColorFilter(settings.themeColors.accentColor, PorterDuff.Mode.MULTIPLY);
        } else {
            if(!settings.darkTheme) {
                likeButton.setImageResource(R.drawable.ic_heart_light);
                likeButton.setColorFilter(Color.BLACK);
            } else {
                likeButton.clearColorFilter();
            }
        }

        String via = context.getResources().getString(R.string.via) + " <b>" + android.text.Html.fromHtml(status.getSource()).toString() + "</b>";
        tweetVia.setText(Html.fromHtml(via));
    }



    public void favoriteStatus(final int type) {
        final long id = status.getId();
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = null;
                    Twitter secTwitter = null;
                    if (type == TYPE_ACC_ONE) {
                        twitter = Utils.getTwitter(context, settings);
                    } else if (type == TYPE_ACC_TWO) {
                        secTwitter = Utils.getSecondTwitter(context);
                    } else {
                        twitter = Utils.getTwitter(context, settings);
                        secTwitter = Utils.getSecondTwitter(context);
                    }

                    if (status.isFavorited() && twitter != null) {
                        twitter.destroyFavorite(id);
                        try {
                            FavoriteTweetsDataSource.getInstance(context).deleteTweet(id);
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_FAVORITES"));
                        } catch (Exception e) { }
                    } else if (twitter != null) {
                        try {
                            twitter.createFavorite(id);
                        } catch (TwitterException e) {
                            // already been favorited by this account
                        }
                    }

                    if (secTwitter != null) {
                        try {
                            secTwitter.createFavorite(id);
                        } catch (Exception e) {

                        }
                    }

                    final Status status = twitter.showStatus(TweetButtonUtils.this.status.getId());

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateTweetCounts(status);
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public void retweetStatus(final int type) {
        final long id = status.getId();
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // if they have a protected account, we want to still be able to retweet their retweets
                    long idToRetweet = id;
                    if (status != null && status.isRetweet()) {
                        idToRetweet = status.getRetweetedStatus().getId();
                    }

                    Twitter twitter = null;
                    Twitter secTwitter = null;
                    if (type == TYPE_ACC_ONE) {
                        twitter = Utils.getTwitter(context, settings);
                    } else if (type == TYPE_ACC_TWO) {
                        secTwitter = Utils.getSecondTwitter(context);
                    } else {
                        twitter = Utils.getTwitter(context, settings);
                        secTwitter = Utils.getSecondTwitter(context);
                    }

                    if (status.isRetweetedByMe() && twitter != null) {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new RemoveRetweet().execute();
                            }
                        });
                    } else if (twitter != null) {
                        try {
                            twitter.retweetStatus(idToRetweet);
                        } catch (TwitterException e) {

                        }
                    }

                    if (secTwitter != null) {
                        secTwitter.retweetStatus(idToRetweet);
                    }

                    final twitter4j.Status status = twitter.showStatus(TweetButtonUtils.this.status.getId());

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateTweetCounts(status);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String generateReplyText() {
        String text = status.getText();
        String screenName = status.getUser().getScreenName();
        String extraNames = "";

        String screenNameToUse;

        if (secondAcc) {
            screenNameToUse = settings.secondScreenName;
        } else {
            screenNameToUse = settings.myScreenName;
        }

        if (text.contains("@")) {
            for (UserMentionEntity user  : status.getUserMentionEntities()) {
                String s = user.getScreenName();
                if (!s.equals(screenNameToUse) && !extraNames.contains(s)  && !s.equals(screenName)) {
                    extraNames += "@" + s + " ";
                }
            }
        }

        String replyStuff = "";
        if (!screenName.equals(screenNameToUse)) {
            replyStuff = "@" + screenName + " " + extraNames;
        } else {
            replyStuff = extraNames;
        }

        if (settings.autoInsertHashtags && text.contains("#")) {
            for (HashtagEntity entity : status.getHashtagEntities()) {
                replyStuff += "#" + entity.getText() + " ";
            }
        }

        return replyStuff;
    }

    String restoreLinks(String text) {
        String imageUrl = TweetLinkUtils.getLinksInStatus(status)[1];
        int urlEntitiesSize = status.getURLEntities().length;
        int length = imageUrl != null && !imageUrl.isEmpty() ? urlEntitiesSize + 1 : urlEntitiesSize;

        String[] otherLinks = new String[length];
        for (int i = 0; i < otherLinks.length; i++) {
            if (i < urlEntitiesSize - 1) {
                otherLinks[i] = status.getURLEntities()[i].getExpandedURL();
            } else {
                otherLinks[i] = imageUrl;
            }
        }

        String webLink = null;

        ArrayList<String> webpages = new ArrayList<>();
        if (otherLinks.length > 0 && !otherLinks[0].equals("")) {
            for (String s : otherLinks) {
                if (!s.contains("youtu")) {
                    if (!s.contains("pic.twitt")) {
                        webpages.add(s);
                    }
                }
            }

            if (webpages.size() >= 1) {
                webLink = webpages.get(0);
            } else {
                webLink = null;
            }

        }

        String full = text;

        String[] split = text.split("\\s");
        String[] otherLink = new String[otherLinks.length];

        for (int i = 0; i < otherLinks.length; i++) {
            otherLink[i] = "" + otherLinks[i];
        }

        for (String s : otherLink) {
            Log.v("talon_links", ":" + s + ":");
        }

        boolean changed = false;
        int otherIndex = 0;

        if (otherLink.length > 0) {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];

                //if (Patterns.WEB_URL.matcher(s).find()) { // we know the link is cut off
                if (Patterns.WEB_URL.matcher(s).find()) { // we know the link is cut off
                    String f = s.replace("...", "").replace("http", "");

                    f = stripTrailingPeriods(f);

                    try {
                        if (otherIndex < otherLinks.length) {
                            if (otherLink[otherIndex].substring(otherLink[otherIndex].length() - 1, otherLink[otherIndex].length()).equals("/")) {
                                otherLink[otherIndex] = otherLink[otherIndex].substring(0, otherLink[otherIndex].length() - 1);
                            }
                            f = otherLink[otherIndex].replace("http://", "").replace("https://", "").replace("www.", "");
                            otherLink[otherIndex] = "";
                            otherIndex++;

                            changed = true;
                        }
                    } catch (Exception e) {

                    }

                    if (changed) {
                        split[i] = f;
                    } else {
                        split[i] = s;
                    }
                } else {
                    split[i] = s;
                }

            }
        }

        if (webLink != null && !webLink.equals("")) {
            for (int i = split.length - 1; i >= 0; i--) {
                String s = split[i];
                if (Patterns.WEB_URL.matcher(s).find()) {
                    String replace = otherLinks[otherLinks.length - 1];
                    if (replace.replace(" ", "").equals("")) {
                        replace = webLink;
                    }
                    split[i] = replace;
                    changed = true;
                    break;
                }
            }
        }

        if(changed) {
            full = "";
            for (String p : split) {
                full += p + " ";
            }

            full = full.substring(0, full.length() - 1);
        }

        return full.replaceAll("  ", " ");
    }

    private static String stripTrailingPeriods(String url) {
        try {
            if (url.substring(url.length() - 1, url.length()).equals(".")) {
                return stripTrailingPeriods(url.substring(0, url.length() - 1));
            } else {
                return url;
            }
        } catch (Exception e) {
            return url;
        }
    }

    private Twitter getTwitter() {
        if (secondAcc) {
            return Utils.getSecondTwitter(context);
        } else {
            return Utils.getTwitter(context, AppSettings.getInstance(context));
        }
    }

    private class RemoveRetweet extends AsyncTask<String, Void, Boolean> {

        protected void onPreExecute() {
            Toast.makeText(context, context.getResources().getString(R.string.removing_retweet), Toast.LENGTH_SHORT).show();
        }

        protected Boolean doInBackground(String... urls) {
            try {
                AppSettings settings = AppSettings.getInstance(context);
                Twitter twitter =  getTwitter();
                ResponseList<twitter4j.Status> retweets = twitter.getUserTimeline(settings.myId, new Paging(1, 100));
                for (twitter4j.Status retweet : retweets) {
                    if(retweet.isRetweet() && retweet.getRetweetedStatus().getId() == status.getId())
                        twitter.destroyStatus(retweet.getId());
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean deleted) {
            if (deleted) {
                retweetButton.clearColorFilter();
            }

            try {
                if (deleted) {
                    Toast.makeText(context, context.getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // user has gone away from the window
            }
        }
    }
}
