package com.klinker.android.twitter_l.data;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Html;
import android.text.Spannable;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.MultiplePicsPopup;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoViewerActivity;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.ui.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.utils.EmojiUtils;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.text.TextUtils;
import com.klinker.android.twitter_l.utils.text.TouchableMovementMethod;

import twitter4j.Status;
import twitter4j.User;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TweetView {

    Context context;
    AppSettings settings;

    Status status;
    String currentUser = null;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    // tweet data
    long tweetId;
    String name;
    String screenName;
    String profilePicUrl;
    String tweet;
    String time;
    long longTime;
    String retweetText;
    String retweeter;
    String imageUrl;
    String otherUrl;
    String hashtags;
    String users;
    String gifUrl;
    boolean isConvo = false;

    // layout components
    TextView nameTv;
    ImageView profilePicIv;
    TextView timeTv;
    TextView tweetTv;
    NetworkedCacheableImageView imageIv;
    TextView retweeterTv;
    LinearLayout backgroundLayout;
    NetworkedCacheableImageView playButton;
    TextView screenTV;
    FrameLayout imageHolder;
    ImageView isAConvo;

    public TweetView(Context context) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }
    }

    public TweetView(Context context, Status status) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        setData(status);
    }

    public void setCurrentUser(String s) {
        currentUser = s;
    }

    public void setData(Status status) {

        longTime = status.getCreatedAt().getTime();

        if (!settings.absoluteDate) {
            time = Utils.getTimeAgo(status.getCreatedAt().getTime(), context);
        } else {
            Date date = new Date(status.getCreatedAt().getTime());
            time = timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date);
        }

        if (status.isRetweet()) {
            retweeter = status.getUser().getScreenName();
            retweetText = context.getString(R.string.retweeter) + retweeter;
            this.status = status.getRetweetedStatus();
            status = status.getRetweetedStatus();
        } else {
            retweetText = null;
            retweeter = null;
            this.status = status;
        }

        User user = status.getUser();

        tweetId = status.getId();
        profilePicUrl = user.getOriginalProfileImageURL();
        tweet = status.getText();
        name = user.getName();
        screenName = user.getScreenName();

        String[] html = TweetLinkUtils.getLinksInStatus(status);
        tweet = html[0];
        imageUrl = html[1];
        otherUrl = html[2];
        hashtags = html[3];
        users = html[4];

        gifUrl = TweetLinkUtils.getGIFUrl(status, otherUrl);

        isConvo = status.getInReplyToStatusId() != -1;
    }

    public View getView() {
        View tweet = createTweet();
        setComponents(tweet);
        bindData();

        /*if (images) {
            imageHolder.setVisibility(View.VISIBLE);
        } else {
            imageHolder.setVisibility(View.GONE);
        }*/

        if (smallImage) {
            ViewGroup.LayoutParams params = imageHolder.getLayoutParams();
            params.height = Utils.toDP(100, context);

            imageHolder.setLayoutParams(params);
        }

        return tweet;
    }

    private View createTweet() {
        View tweetView = ((Activity) context).getLayoutInflater().inflate(R.layout.tweet, null, false);
        return tweetView;
    }

    //private boolean images = true;
    private boolean smallImage = false;

    /*public void hideImage(boolean hide) {
        images = !hide;
    }*/

    public void setSmallImage(boolean small) {
        smallImage = small;
    }

    private void setComponents(View v) {
        nameTv = (TextView) v.findViewById(R.id.name);
        profilePicIv = (ImageView) v.findViewById(R.id.profile_pic);
        timeTv = (TextView) v.findViewById(R.id.time);
        tweetTv = (TextView) v.findViewById(R.id.tweet);
        imageIv = (NetworkedCacheableImageView) v.findViewById(R.id.image);
        retweeterTv = (TextView) v.findViewById(R.id.retweeter);
        backgroundLayout = (LinearLayout) v.findViewById(R.id.background);
        playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button);
        screenTV = (TextView) v.findViewById(R.id.screenname);
        imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);
        isAConvo = (ImageView) v.findViewById(R.id.is_a_conversation);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageIv.setClipToOutline(true);
        }

        // sets up the font sizes
        tweetTv.setTextSize(settings.textSize);
        nameTv.setTextSize(settings.textSize + 4);
        screenTV.setTextSize(settings.textSize - 2);
        timeTv.setTextSize(settings.textSize - 3);
        retweeterTv.setTextSize(settings.textSize - 3);
    }

    private void bindData() {
        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String link;

                boolean displayPic = !imageUrl.equals("") && !imageUrl.contains("youtube");
                if (displayPic) {
                    link = imageUrl;
                } else {
                    link = otherUrl.split("  ")[0];
                }

                Intent viewTweet = new Intent(context, TweetActivity.class);
                viewTweet.putExtra("name", name);
                viewTweet.putExtra("screenname", screenName);
                viewTweet.putExtra("time", longTime);
                viewTweet.putExtra("tweet", tweet);
                viewTweet.putExtra("retweeter", retweeter);
                viewTweet.putExtra("webpage", link);
                viewTweet.putExtra("other_links", otherUrl);
                viewTweet.putExtra("picture", displayPic);
                viewTweet.putExtra("tweetid", tweetId);
                viewTweet.putExtra("proPic", profilePicUrl);
                viewTweet.putExtra("users", users);
                viewTweet.putExtra("hashtags", hashtags);
                viewTweet.putExtra("animated_gif", gifUrl);

                viewTweet.putExtra("shared_trans", true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    profilePicIv.setTransitionName("pro_pic");
                    screenTV.setTransitionName("screen_name");
                    nameTv.setTransitionName("name");
                    tweetTv.setTransitionName("tweet");
                    ActivityOptions options = ActivityOptions
                            .makeSceneTransitionAnimation(((Activity) context),

                                    new Pair<View, String>(profilePicIv, "pro_pic"),
                                    new Pair<View, String>(screenTV, "screen_name"),
                                    new Pair<View, String>(nameTv, "name"),
                                    new Pair<View, String>(tweetTv, "tweet")
                            );

                    context.startActivity(viewTweet, options.toBundle());
                } else {
                    context.startActivity(viewTweet);
                }

            }
        });

        if (currentUser == null || !screenName.equals(currentUser)) {
            profilePicIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent viewProfile = new Intent(context, ProfilePager.class);
                    viewProfile.putExtra("name", name);
                    viewProfile.putExtra("screenname", screenName);
                    viewProfile.putExtra("proPic", profilePicUrl);
                    viewProfile.putExtra("tweetid", tweetId);
                    viewProfile.putExtra("retweet", retweeterTv.getVisibility() == View.VISIBLE);
                    viewProfile.putExtra("long_click", false);

                    context.startActivity(viewProfile);
                }
            });
        }

        if (screenTV.getVisibility() == View.GONE) {
            screenTV.setVisibility(View.VISIBLE);
        }

        screenTV.setText("@" + screenName);
        nameTv.setText(name);
        timeTv.setText(time);

        boolean replace = false;
        if (settings.inlinePics && (tweet.contains("pic.twitter.com/"))) {
            if (tweet.lastIndexOf(".") == tweet.length() - 1) {
                replace = true;
            }
        }

        try {
            tweetTv.setText(replace ?
                    tweet.substring(0, tweet.length() - (tweet.contains(" twitter.com") ? 33 : 25)) :
                    tweet);
        } catch (Exception e) {
            tweetTv.setText(tweet);
        }

        if (retweeter != null) {
            retweeterTv.setText(retweetText);
            retweeterTv.setVisibility(View.VISIBLE);
        }

        if (isConvo) {
            isAConvo.setVisibility(View.VISIBLE);
        }

        boolean picture = false;

        if(settings.inlinePics) {
            if (imageUrl.equals("")) {
                if (imageHolder.getVisibility() != View.GONE) {
                    imageHolder.setVisibility(View.GONE);
                }

                if (playButton.getVisibility() == View.VISIBLE) {
                    playButton.setVisibility(View.GONE);
                }
            } else {
                if (imageUrl.contains("youtube")) {

                    if (playButton.getVisibility() == View.GONE) {
                        playButton.setVisibility(View.VISIBLE);
                    }

                    final String fRetweeter = retweeter;

                    imageIv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String link;

                            boolean displayPic = !imageUrl.equals("") && !imageUrl.contains("youtube");
                            if (displayPic) {
                                link = imageUrl;
                            } else {
                                link = otherUrl.split("  ")[0];
                            }

                            Intent viewTweet = new Intent(context, TweetActivity.class);
                            viewTweet.putExtra("name", name);
                            viewTweet.putExtra("screenname", screenName);
                            viewTweet.putExtra("time", time);
                            viewTweet.putExtra("tweet", tweet);
                            viewTweet.putExtra("retweeter", fRetweeter);
                            viewTweet.putExtra("webpage", link);
                            viewTweet.putExtra("other_links", otherUrl);
                            viewTweet.putExtra("picture", displayPic);
                            viewTweet.putExtra("tweetid", tweetId);
                            viewTweet.putExtra("proPic", profilePicUrl);
                            viewTweet.putExtra("users", users);
                            viewTweet.putExtra("hashtags", hashtags);
                            viewTweet.putExtra("clicked_youtube", true);
                            viewTweet.putExtra("animated_gif", gifUrl);

                            viewTweet.putExtra("shared_trans", true);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                profilePicIv.setTransitionName("pro_pic");
                                screenTV.setTransitionName("screen_name");
                                nameTv.setTransitionName("name");
                                tweetTv.setTransitionName("tweet");
                                ActivityOptions options = ActivityOptions
                                        .makeSceneTransitionAnimation(((Activity) context),

                                                new Pair<View, String>(profilePicIv, "pro_pic"),
                                                new Pair<View, String>(screenTV, "screen_name"),
                                                new Pair<View, String>(nameTv, "name"),
                                                new Pair<View, String>(tweetTv, "tweet")
                                        );

                                context.startActivity(viewTweet, options.toBundle());
                            } else {
                                context.startActivity(viewTweet);
                            }
                        }
                    });

                    imageIv.setImageDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));

                    picture = true;

                } else {
                    imageIv.setImageDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));

                    picture = true;

                    if (playButton.getVisibility() == View.VISIBLE) {
                        playButton.setVisibility(View.GONE);
                    }

                    imageIv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (imageUrl.contains(" ")) {
                                MultiplePicsPopup popup = new MultiplePicsPopup(context, context.getResources().getBoolean(R.bool.isTablet), imageUrl);
                                popup.setFullScreen();
                                popup.setExpansionPointForAnim(view);
                                popup.show();
                            } else {
                                context.startActivity(new Intent(context, PhotoViewerActivity.class).putExtra("url", imageUrl));
                            }
                        }
                    });
                }

                if (imageHolder.getVisibility() == View.GONE) {
                    imageHolder.setVisibility(View.VISIBLE);
                }
            }
        }

        BitmapLruCache mCache = App.getInstance(context).getBitmapCache();
        if (picture) {
            ImageUtils.loadImage(context, imageIv, imageUrl, mCache);
        }

        ImageUtils.loadImage(context, profilePicIv, profilePicUrl, mCache);

        if (settings.useEmoji && (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || EmojiUtils.ios)) {
            if (EmojiUtils.emojiPattern.matcher(tweet).find()) {
                final Spannable span = EmojiUtils.getSmiledText(context, Html.fromHtml(tweet));
                tweetTv.setText(span);
            }
        }

        tweetTv.setSoundEffectsEnabled(false);
        tweetTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TouchableMovementMethod.touched) {
                    backgroundLayout.performClick();
                }
            }
        });


        if (retweeterTv.getVisibility() == View.VISIBLE) {
            retweeterTv.setSoundEffectsEnabled(false);
            retweeterTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!TouchableMovementMethod.touched) {
                        backgroundLayout.performClick();
                    }
                }
            });
        }

        TextUtils.linkifyText(context, tweetTv, backgroundLayout, true, otherUrl, false);
        TextUtils.linkifyText(context, retweeterTv, backgroundLayout, true, "", false);
    }
}
