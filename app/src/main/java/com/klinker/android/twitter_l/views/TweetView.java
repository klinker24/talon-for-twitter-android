package com.klinker.android.twitter_l.views;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.easyvideoplayer.EasyVideoPlayer;
import com.bumptech.glide.Glide;
import com.klinker.android.peekview.PeekViewActivity;
import com.klinker.android.peekview.builder.Peek;
import com.klinker.android.peekview.builder.PeekViewOptions;
import com.klinker.android.peekview.callback.OnPeek;
import com.klinker.android.peekview.callback.SimpleOnPeek;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.TimeoutThread;
import com.klinker.android.twitter_l.views.badges.GifBadge;
import com.klinker.android.twitter_l.views.peeks.ProfilePeek;
import com.klinker.android.twitter_l.views.popups.QuickActionsPopup;
import com.klinker.android.twitter_l.views.badges.VideoBadge;
import com.klinker.android.twitter_l.activities.media_viewer.PhotoPagerActivity;
import com.klinker.android.twitter_l.activities.media_viewer.PhotoViewerActivity;
import com.klinker.android.twitter_l.activities.media_viewer.VideoViewerActivity;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.activities.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.utils.EasyVideoCallbackWrapper;
import com.klinker.android.twitter_l.utils.EmojiUtils;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.VideoMatcherUtil;
import com.klinker.android.twitter_l.utils.text.TextUtils;
import com.klinker.android.twitter_l.utils.text.TouchableMovementMethod;

import twitter4j.Status;
import twitter4j.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class TweetView {

    private static final int MAX_EMBEDDED_TWEETS = 2;

    public static final Pattern embeddedTweetPattern = Pattern.compile("\\stwitter.com/");;
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
    int embeddedTweetMinHeight = 0;

    int numLikes;
    int numRetweets;

    // layout components
    View root;
    TextView nameTv;
    ImageView profilePicIv;
    TextView timeTv;
    TextView tweetTv;
    ImageView imageIv;
    TextView retweeterTv;
    LinearLayout backgroundLayout;
    ImageView playButton;
    TextView screenTV;
    FrameLayout imageHolder;
    ImageView isAConvo;
    CardView embeddedTweet;
    View quickActions;

    int embeddedTweets = 0;

    boolean inReplyToSection = false;
    boolean displayProfilePicture = true;

    public void setDisplayProfilePicture(boolean displayProfilePicture) {
        this.displayProfilePicture = displayProfilePicture;
    }

    public TweetView(Context context) {
        this(context, 0);
    }

    public TweetView(Context context, int embedded) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        embeddedTweets = embedded + 1;

        Log.v("embedded_tweets", embeddedTweets + "");
    }

    public TweetView(Context context, Status status) {
        this(context, status, 0);
    }

    public TweetView(Context context, Status status, int embedded) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        this.embeddedTweets = embedded + 1;

        setData(status);
        Log.v("embedded_tweets", embeddedTweets + "");
    }

    public TweetView setInReplyToSection(boolean inSection) {
        this.inReplyToSection = inSection;
        return this;
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

        numLikes = status.getFavoriteCount();
        numRetweets = status.getRetweetCount();
    }

    public View getView() {
        View tweet = createTweet();
        setComponents(tweet);
        bindData();

        if (smallImage && shouldShowImage()) {
            ViewGroup.LayoutParams params = imageHolder.getLayoutParams();
            params.height = Utils.toDP(100, context);

            imageHolder.setLayoutParams(params);
        }

        return tweet;
    }

    protected View createTweet() {
        if (inReplyToSection) {
            View tweetView = ((Activity) context).getLayoutInflater().inflate(R.layout.tweet_in_reply_to_section, null, false);
            //tweetView.findViewById(R.id.tweet_link).setBackgroundColor(AppSettings.getInstance(context).themeColors.primaryColor);
            return tweetView;
        } else {
            View tweetView = ((Activity) context).getLayoutInflater().inflate(
                    settings.picturesType != AppSettings.CONDENSED_TWEETS ? R.layout.tweet : R.layout.tweet_condensed,
                    null, false);
            return tweetView;
        }
    }

    //private boolean images = true;
    private boolean smallImage = false;

    public void setSmallImage(boolean small) {
        smallImage = small;
    }

    protected void setComponents(View v) {
        root = v.findViewById(R.id.root);
        nameTv = (TextView) v.findViewById(R.id.name);
        profilePicIv = (ImageView) v.findViewById(R.id.profile_pic);
        timeTv = (TextView) v.findViewById(R.id.time);
        tweetTv = (TextView) v.findViewById(R.id.tweet);
        retweeterTv = (TextView) v.findViewById(R.id.retweeter);
        backgroundLayout = (LinearLayout) v.findViewById(R.id.background);
        playButton = (ImageView) v.findViewById(R.id.play_button);
        screenTV = (TextView) v.findViewById(R.id.screenname);
        isAConvo = (ImageView) v.findViewById(R.id.is_a_conversation);
        embeddedTweet = (CardView) v.findViewById(R.id.embedded_tweet_card);
        quickActions = v.findViewById(R.id.quick_actions);

        imageIv = (ImageView) v.findViewById(R.id.image);
        playButton = (ImageView) v.findViewById(R.id.play_button);
        imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageIv.setClipToOutline(true);
        }

        // sets up the font sizes
        tweetTv.setTextSize(settings.textSize);
        nameTv.setTextSize(settings.textSize + 4);
        screenTV.setTextSize(settings.textSize - 2);
        timeTv.setTextSize(settings.textSize - 3);
        retweeterTv.setTextSize(settings.textSize - 3);

        embeddedTweetMinHeight = Utils.toDP(140, context);
        embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);

        if (!displayProfilePicture) {
            profilePicIv.setVisibility(View.GONE);
        } else if (profilePicIv.getVisibility() != View.VISIBLE) {
            profilePicIv.setVisibility(View.VISIBLE);
        }
    }

    protected void bindData() {
        if (quickActions != null) {
            quickActions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    QuickActionsPopup popup = new QuickActionsPopup(context, tweetId, screenName, tweet);
                    popup.setExpansionPointForAnim(quickActions);
                    popup.setOnTopOfView(quickActions);
                    popup.show();
                }
            });
        }

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String link;

                boolean displayPic = !imageUrl.equals("");
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

                viewTweet = addDimensForExpansion(viewTweet, backgroundLayout);

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

                    context.startActivity(viewTweet/*, options.toBundle()*/);
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

                    viewProfile = addDimensForExpansion(viewProfile, profilePicIv);

                    context.startActivity(viewProfile);
                }
            });
        }

        if (context instanceof PeekViewActivity && settings.usePeek) {
            PeekViewOptions options = new PeekViewOptions()
                    .setAbsoluteWidth(225)
                    .setAbsoluteHeight(257);

            Peek.into(R.layout.peek_profile, new ProfilePeek(screenName))
                    .with(options)
                    .applyTo((PeekViewActivity) context, profilePicIv);
        }

        if (screenTV.getVisibility() == View.GONE) {
            screenTV.setVisibility(View.VISIBLE);
        }

        screenTV.setText("@" + screenName);
        nameTv.setText(name);
        timeTv.setText(time);

        boolean replace = false;
        boolean embeddedTweetFound = embeddedTweets < MAX_EMBEDDED_TWEETS ? embeddedTweetPattern.matcher(tweet).find() : false;
        if (settings.inlinePics && (tweet.contains("pic.twitter.com/")) || embeddedTweetFound) {
            if (tweet.lastIndexOf(".") == tweet.length() - 1) {
                replace = true;
            }
        }

        try {
            tweetTv.setText(replace ?
                    tweet.substring(0, tweet.length() - (embeddedTweetFound ? 33 : 25)) :
                    tweet);
        } catch (Exception e) {
            tweetTv.setText(tweet);
        }

        if (retweeter != null) {
            retweeterTv.setText(retweetText);
            retweeterTv.setVisibility(View.VISIBLE);
        }

        if (isConvo && isAConvo != null) {
            isAConvo.setVisibility(View.VISIBLE);
        }

        boolean picture = false;

        if(settings.inlinePics && shouldShowImage()) {
            if (imageUrl.equals("")) {
                // no image
                if (imageHolder.getVisibility() != View.GONE) {
                    imageHolder.setVisibility(View.GONE);
                }

                if (playButton.getVisibility() == View.VISIBLE) {
                    playButton.setVisibility(View.GONE);
                }
            } else {
                // there is an image

                if (imageUrl.contains("youtube") || (gifUrl != null && !android.text.TextUtils.isEmpty(gifUrl))) {
                    // youtube or twitter video/gif

                    if (playButton.getVisibility() == View.GONE) {
                        playButton.setVisibility(View.VISIBLE);
                    }

                    PeekViewOptions options = new PeekViewOptions();
                    options.setFullScreenPeek(true);
                    options.setBackgroundDim(1f);

                    int layoutRes = 0;
                    if (VideoMatcherUtil.isTwitterGifLink(gifUrl)) {
                        playButton.setImageDrawable(new GifBadge(context));
                        layoutRes = R.layout.peek_gif;
                    } else {
                        playButton.setImageDrawable(new VideoBadge(context));

                        if (!imageUrl.contains("youtube")) {
                            layoutRes = R.layout.peek_video;
                        }
                    }

                    if (context instanceof PeekViewActivity && settings.usePeek) {
                        if (layoutRes != 0) {
                            Peek.into(layoutRes, new OnPeek() {
                                private EasyVideoPlayer videoView;

                                @Override
                                public void shown() {}

                                @Override
                                public void onInflated(View rootView) {
                                    videoView = (EasyVideoPlayer) rootView.findViewById(R.id.video);
                                    videoView.setSource(Uri.parse(gifUrl.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4")));
                                    videoView.setCallback(new EasyVideoCallbackWrapper());
                                }

                                @Override
                                public void dismissed() {
                                    videoView.release();
                                }
                            }).with(options).applyTo((PeekViewActivity) context, imageIv);
                        }
                    }

                    imageIv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            VideoViewerActivity.startActivity(context, tweetId, gifUrl, otherUrl);
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
                                PhotoPagerActivity.startActivity(context, tweetId, imageUrl, 0);
                            } else {
                                PhotoViewerActivity.startActivity(context, tweetId, imageUrl, imageIv);
                            }
                        }
                    });

                    if (context instanceof PeekViewActivity && settings.usePeek) {
                        PeekViewOptions options = new PeekViewOptions();
                        options.setFullScreenPeek(true);
                        options.setBackgroundDim(1f);

                        Peek.into(R.layout.peek_image, new SimpleOnPeek() {
                            @Override
                            public void onInflated(View rootView) {
                                Glide.with(context).load(imageUrl.split(" ")[0]).into((ImageView) rootView.findViewById(R.id.image));
                            }
                        }).with(options).applyTo((PeekViewActivity) context, imageIv);
                    }
                }

                if (imageHolder.getVisibility() == View.GONE) {
                    imageHolder.setVisibility(View.VISIBLE);
                }
            }
        }

        if (picture) {
            try {
                glide(imageUrl, imageIv);
            } catch (Exception e) { }
        }

        glide(profilePicUrl, profilePicIv);

        /*if (settings.useEmoji && (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || EmojiUtils.ios)) {
            if (EmojiUtils.emojiPattern.matcher(tweet).find()) {
                final Spannable span = EmojiUtils.getSmiledText(context, Html.fromHtml(tweet));
                tweetTv.setText(span);
            }
        }*/

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

        if (otherUrl != null && otherUrl.contains("/status/") && !otherUrl.contains("/photo/") &&
                embeddedTweet.getChildCount() == 0) {
            loadEmbeddedTweet(otherUrl);
        }
    }

    public void loadEmbeddedTweet(final String otherUrls) {

        if (embeddedTweets >= MAX_EMBEDDED_TWEETS) {
            return;
        }

        embeddedTweet.setVisibility(View.VISIBLE);

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Long embeddedId = 0l;
                for (String u : otherUrls.split(" ")) {
                    if (u.contains("/status/") && !otherUrl.contains("/photo/")) {
                        embeddedId = TweetLinkUtils.getTweetIdFromLink(u);
                        break;
                    }
                }

                if (embeddedId != 0l) {
                    Status status = null;

                    try {
                        status = Utils.getTwitter(context, settings).showStatus(embeddedId);
                    } catch (Exception e) {
                        status = null;
                    }

                    final Status embedded = status;

                    if (status != null) {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TweetView v = new TweetView(context, embedded, embeddedTweets);

                                v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
                                v.setSmallImage(true);

                                embeddedTweet.removeAllViews();
                                embeddedTweet.addView(v.getView());

                                embeddedTweet.setMinimumHeight(0);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private Intent addDimensForExpansion(Intent i, View view) {
        i.putExtra(TweetActivity.USE_EXPANSION, true);

        int location[] = new int[2];
        view.getLocationOnScreen(location);

        i.putExtra(TweetActivity.EXPANSION_DIMEN_LEFT_OFFSET, location[0]);
        i.putExtra(TweetActivity.EXPANSION_DIMEN_TOP_OFFSET, location[1]);
        i.putExtra(TweetActivity.EXPANSION_DIMEN_HEIGHT, view.getHeight());
        i.putExtra(TweetActivity.EXPANSION_DIMEN_WIDTH, view.getWidth());

        return i;
    }

    protected boolean shouldShowImage() {
        return true;
    }

    private void glide(String url, ImageView target) {
        try {
            Glide.with(context).load(url).into(target);
        } catch (Exception e) {
            // load after activity is destroyed
        }
    }
}
