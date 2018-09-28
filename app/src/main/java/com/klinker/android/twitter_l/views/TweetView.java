package com.klinker.android.twitter_l.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.halilibo.bettervideoplayer.BetterVideoPlayer;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.peekview.PeekViewActivity;
import com.klinker.android.peekview.builder.Peek;
import com.klinker.android.peekview.builder.PeekViewOptions;
import com.klinker.android.peekview.callback.OnPeek;
import com.klinker.android.peekview.callback.SimpleOnPeek;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.media_viewer.image.ImageViewerActivity;
import com.klinker.android.twitter_l.data.WebPreview;
import com.klinker.android.twitter_l.listeners.MultipleImageTouchListener;
import com.klinker.android.twitter_l.utils.BetterVideoCallbackWrapper;
import com.klinker.android.twitter_l.utils.ReplyUtils;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.views.badges.GifBadge;
import com.klinker.android.twitter_l.views.peeks.ProfilePeek;
import com.klinker.android.twitter_l.views.popups.QuickActionsPopup;
import com.klinker.android.twitter_l.views.badges.VideoBadge;
import com.klinker.android.twitter_l.activities.media_viewer.VideoViewerActivity;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.activities.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.VideoMatcherUtil;
import com.klinker.android.twitter_l.utils.text.TextUtils;
import com.klinker.android.twitter_l.utils.text.TouchableMovementMethod;

import org.jetbrains.annotations.NotNull;

import twitter4j.Status;
import twitter4j.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class TweetView {

    public interface TweetLoaded {
        public void onLoaded(Status status);
    }

    private static final int MAX_EMBEDDED_TWEETS = 2;

    public static final Pattern embeddedTweetPattern = Pattern.compile("\\stwitter.com/");
    public static final Pattern twitterMomentPattern = Pattern.compile("\\stwitter.com/i/moments");
    public static final Pattern twitterExpandedTweetPattern = Pattern.compile("\\stwitter.com/i/web");

    public static boolean isEmbeddedTweet(String text) {
        return TweetView.embeddedTweetPattern.matcher(text).find() &&
                !TweetView.twitterMomentPattern.matcher(text).find() &&
                !TweetView.twitterExpandedTweetPattern.matcher(text).find();
    }

    Context context;
    AppSettings settings;
    TweetLoaded loadedCallback;

    public Status status;
    String currentUser = null;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    // tweet data
    long tweetId;
    String name;
    String screenName;
    String profilePicUrl;
    String tweetWithReplyHandles;
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
    long videoDuration;
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
    TextView replies;
    View backgroundLayout;
    ImageView playButton;
    TextView screenTV;
    FrameLayout imageHolder;
    ImageView isAConvo;
    ViewGroup embeddedTweet;
    View quickActions;
    WebPreviewCard webPreviewCard;

    int embeddedTweets = 0;

    boolean displayProfilePicture = true;
    boolean smallerMargins = false;

    public TweetView setUseSmallerMargins(boolean smaller) {
        this.smallerMargins = smaller;
        return this;
    }

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

    public void setCurrentUser(String s) {
        currentUser = s;
    }

    public void setData(Status status) {

        longTime = status.getCreatedAt().getTime();

        if (!settings.absoluteDate) {
            time = Utils.getTimeAgo(status.getCreatedAt().getTime(), context, false);
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

        if (loadedCallback != null) {
            loadedCallback.onLoaded(this.status);
        }

        User user = status.getUser();

        tweetId = status.getId();
        try {
            profilePicUrl = user.getOriginalProfileImageURL();
        } catch (Exception e) {
            profilePicUrl = user.getProfileImageURL();
        }
        tweet = status.getText();
        name = user.getName();
        screenName = user.getScreenName();

        String[] html = TweetLinkUtils.getLinksInStatus(status);
        tweet = html[0];
        imageUrl = html[1];
        otherUrl = html[2];
        hashtags = html[3];
        users = html[4];

        TweetLinkUtils.TweetMediaInformation info = TweetLinkUtils.getGIFUrl(status, otherUrl);
        gifUrl = info.url;
        videoDuration = info.duration;

        isConvo = status.getInReplyToStatusId() != -1;

        numLikes = status.getFavoriteCount();
        numRetweets = status.getRetweetCount();
    }

    private View tweetView = null;
    public View getView() {
        if (tweetView == null) {
            tweetView = createTweet();

            if (smallerMargins) {
                View header = tweetView.findViewById(R.id.tweet_header);
                if (header == null) {
                    tweetView.findViewById(R.id.background).setPadding(0,Utils.toDP(6, context),0, Utils.toDP(6, context));
                } else {
                    tweetView.findViewById(R.id.background).setPadding(0,0,0, Utils.toDP(6, context));
                    header.setPadding(0,Utils.toDP(6, context), 0,0);
                }
            }
            setComponents(tweetView);
            bindData();
            setupImage();
        }

        return tweetView;
    }

    protected void setupImage() {
        if (smallImage && shouldShowImage() && !settings.condensedTweets()) {
            ViewGroup.LayoutParams params = imageHolder.getLayoutParams();
            params.height = Utils.toDP(settings.cropImagesOnTimeline ? 148 : 248, context);

            imageHolder.setLayoutParams(params);
        }
    }

    protected View createTweet() {
        int layout = R.layout.tweet;
        if (settings.revampedTweets()) {
            //layout = R.layout.tweet_quoted_revamped;
        } else if (settings.condensedTweets()) {
            layout = R.layout.tweet_condensed;
        }

        return ((Activity) context).getLayoutInflater().inflate(layout, null, false);
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
        replies = (TextView) v.findViewById(R.id.reply_to);
        backgroundLayout = v.findViewById(R.id.background);
        playButton = (ImageView) v.findViewById(R.id.play_button);
        screenTV = (TextView) v.findViewById(R.id.screenname);
        isAConvo = (ImageView) v.findViewById(R.id.is_a_conversation);
        embeddedTweet = (ViewGroup) v.findViewById(R.id.embedded_tweet_card);
        quickActions = v.findViewById(R.id.quick_actions);
        webPreviewCard = (WebPreviewCard) v.findViewById(R.id.web_preview_card);

        imageIv = (ImageView) v.findViewById(R.id.image);
        playButton = (ImageView) v.findViewById(R.id.play_button);
        imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageIv.setClipToOutline(true);
        }

        setupFontSizes();
        setupProfilePicture();

        embeddedTweetMinHeight = Utils.toDP(140, context);
        embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);

    }

    protected void setupProfilePicture() {
        if (profilePicIv != null) {
            if (!displayProfilePicture || !settings.showProfilePictures) {
                profilePicIv.setVisibility(View.GONE);
            } else if (profilePicIv.getVisibility() != View.VISIBLE) {
                profilePicIv.setVisibility(View.VISIBLE);
            }
        }
    }

    protected void setupFontSizes() {
        // sets up the font sizes
        tweetTv.setTextSize(settings.textSize);
        screenTV.setTextSize(settings.textSize - (settings.condensedTweets() ? 1 : 2));
        nameTv.setTextSize(settings.textSize + (settings.condensedTweets() ? 1 : 4));
        timeTv.setTextSize(settings.textSize - 3);
        retweeterTv.setTextSize(settings.textSize - 3);
        replies.setTextSize(settings.textSize - 2);
    }

    protected void bindData() {
        if (quickActions != null) {
            quickActions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    QuickActionsPopup popup = new QuickActionsPopup(context, tweetId, screenName, tweetWithReplyHandles);
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
                viewTweet.putExtra("tweet", tweetWithReplyHandles);
                viewTweet.putExtra("retweeter", retweeter);
                viewTweet.putExtra("webpage", link);
                viewTweet.putExtra("other_links", otherUrl);
                viewTweet.putExtra("picture", displayPic);
                viewTweet.putExtra("tweetid", tweetId);
                viewTweet.putExtra("proPic", profilePicUrl);
                viewTweet.putExtra("users", users);
                viewTweet.putExtra("hashtags", hashtags);
                viewTweet.putExtra("animated_gif", gifUrl);
                viewTweet.putExtra("conversation", isConvo);
                viewTweet.putExtra("video_duration", videoDuration);

                TweetActivity.applyDragDismissBundle(context, viewTweet);
                context.startActivity(viewTweet);
            }
        });

        if ((currentUser == null || !screenName.equals(currentUser)) && profilePicIv != null) {
            profilePicIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ProfilePager.start(context, name, screenName, profilePicUrl);
                }
            });
        }

        if (settings.usePeek && profilePicIv != null) {
            ProfilePeek.create(context, profilePicIv, screenName);
        }

        if (screenTV.getVisibility() == View.GONE) {
            screenTV.setVisibility(View.VISIBLE);
        }

        screenTV.setText("@" + screenName);
        nameTv.setText(name);
        timeTv.setText(time);

        tweetWithReplyHandles = tweet;

        final String replyUsers = ReplyUtils.getReplyingToHandles(tweet);
        if (isConvo && settings.compressReplies && replyUsers != null && !replyUsers.isEmpty()) {
            tweet = tweet.replace(replyUsers, "");

            final String replyToText = context.getString(R.string.reply_to);
            final String othersText = context.getString(R.string.others);

            if (ReplyUtils.showMultipleReplyNames(replyUsers)) {
                replies.setText(replyToText + " " + replyUsers);
                TextUtils.linkifyText(context, replies, backgroundLayout, true, "", false);
            } else {
                final String firstPerson = replyUsers.split(" ")[0];
                replies.setText(replyToText + " " + firstPerson + " & " + othersText);

                Link others = new Link(othersText)
                        .setUnderlined(false)
                        .setTextColor(settings.themeColors.accentColor)
                        .setOnClickListener(new Link.OnClickListener() {
                            @Override
                            public void onClick(String clickedText) {
                                final String[] repliesSplit = replyUsers.split(" ");
                                new AlertDialog.Builder(context).setItems(replyUsers.split(" "), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ProfilePager.start(context, repliesSplit[which]);
                                    }
                                }).show();
                            }
                        });
                Link first = new Link(firstPerson)
                        .setUnderlined(false)
                        .setTextColor(settings.themeColors.accentColor)
                        .setOnClickListener(new Link.OnClickListener() {
                            @Override
                            public void onClick(String clickedText) {
                                ProfilePager.start(context, firstPerson);
                            }
                        });

                LinkBuilder.on(replies).addLink(others).addLink(first).build();
            }

            if (replies.getVisibility() != View.VISIBLE) {
                replies.setVisibility(View.VISIBLE);
            }
        } else if (replies.getVisibility() != View.GONE) {
            replies.setVisibility(View.GONE);
        }

        boolean replace = false;
        boolean embeddedTweetFound = embeddedTweets < MAX_EMBEDDED_TWEETS ? isEmbeddedTweet(tweet) : false;
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

        handleRetweeter();

        if (isConvo && isAConvo != null) {
            isAConvo.setVisibility(View.VISIBLE);
        }

        boolean picture = false;

        MultipleImageTouchListener imageTouchListener = new MultipleImageTouchListener(imageUrl);
        imageIv.setOnTouchListener(imageTouchListener);


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
                        playButton.setImageDrawable(new VideoBadge(context, videoDuration));

                        if (!imageUrl.contains("youtube")) {
                            layoutRes = R.layout.peek_video;
                        }
                    }

                    if (context instanceof PeekViewActivity && settings.usePeek) {
                        if (layoutRes != 0) {
                            Peek.into(layoutRes, new OnPeek() {
                                private BetterVideoPlayer videoView;

                                @Override
                                public void shown() {}

                                @Override
                                public void onInflated(View rootView) {
                                    videoView = (BetterVideoPlayer) rootView.findViewById(R.id.video);
                                    videoView.setSource(Uri.parse(gifUrl.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4")));
                                    videoView.setCallback(new BetterVideoCallbackWrapper() {
                                        @Override
                                        public void onCompletion(BetterVideoPlayer player) {
                                            if (VideoMatcherUtil.isTwitterGifLink(gifUrl)) {
                                                videoView.seekTo(0);
                                                videoView.start();
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void dismissed() {
                                    videoView.release();
                                }
                            }).with(options).applyTo((PeekViewActivity) context, imageHolder);
                        }
                    }

                    imageHolder.setOnClickListener(new View.OnClickListener() {
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

                    imageHolder.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int imagePosition = imageTouchListener.getImageTouchPosition();
                            ImageViewerActivity.Companion.startActivity(context, tweetId, imageIv, imagePosition, imageUrl.split(" "));
                        }
                    });

                    if (context instanceof PeekViewActivity && settings.usePeek) {
                        PeekViewOptions options = new PeekViewOptions();
                        options.setFullScreenPeek(true);
                        options.setBackgroundDim(1f);

                        Peek.into(R.layout.peek_image, new SimpleOnPeek() {
                            @Override
                            public void onInflated(View rootView) {
                                try {
                                    int imagePosition = imageTouchListener.getImageTouchPosition();
                                    Glide.with(context).load(imageUrl.split(" ")[imagePosition])
                                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                            .into((ImageView) rootView.findViewById(R.id.image));
                                } catch (IllegalArgumentException e) {
                                }
                            }
                        }).with(options).applyTo((PeekViewActivity) context, imageHolder);
                    }
                }

                if (imageHolder.getVisibility() == View.GONE) {
                    imageHolder.setVisibility(View.VISIBLE);
                }
            }
        }

        if (picture) {
            try {
                glide(imageUrl, imageIv, settings.cropImagesOnTimeline);
            } catch (Exception e) { }
        }

        glide(profilePicUrl, profilePicIv, true);

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

        if (webPreviewCard != null) {
            if (embeddedTweetFound || picture || !settings.webPreviews) {
                if (webPreviewCard.getVisibility() == View.VISIBLE) {
                    webPreviewCard.setVisibility(View.GONE);
                }
            } else if (otherUrl != null && otherUrl.length() > 0) {
                String link = otherUrl.split(" ")[0];

                if (WebPreviewCard.Companion.ignoreLink(link)) {
                    if (webPreviewCard.getVisibility() == View.VISIBLE) {
                        webPreviewCard.setVisibility(View.GONE);
                    }
                } else {
                    if (webPreviewCard.getVisibility() == View.GONE) {
                        webPreviewCard.setVisibility(View.VISIBLE);
                    }

                    webPreviewCard.loadLink(otherUrl.split(" ")[0], new WebPreviewCard.OnLoad() {
                        @Override public void onLinkLoaded(@NotNull String link, @NotNull WebPreview preview) { }
                    });
                }
            } else {
                if (webPreviewCard.getVisibility() == View.VISIBLE) {
                    webPreviewCard.setVisibility(View.GONE);
                }
            }
        }

        TextUtils.linkifyText(context, tweetTv, backgroundLayout, true, otherUrl, false);
        TextUtils.linkifyText(context, retweeterTv, backgroundLayout, true, "", false);

        if (TweetView.isEmbeddedTweet(tweet) && !otherUrl.contains("/photo/") &&
                embeddedTweet.getChildCount() == 0) {
            loadEmbeddedTweet(otherUrl);
        }
    }

    protected void handleRetweeter() {
        if (retweeter != null) {
            retweeterTv.setText(retweetText);
            retweeterTv.setVisibility(View.VISIBLE);
        }
    }

    public void loadEmbeddedTweet(final String otherUrls) {

        if (embeddedTweets >= MAX_EMBEDDED_TWEETS) {
            return;
        }

        embeddedTweet.setVisibility(View.VISIBLE);
        embeddedTweet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Long embeddedId = 0l;
                for (String u : otherUrls.split(" ")) {
                    if (u.contains("/status/") && !u.contains("/i/web/") && !otherUrl.contains("/photo/")) {
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

    public TweetView setTweetLoadedCallback(TweetLoaded callback) {
        this.loadedCallback = callback;
        return this;
    }

    protected boolean shouldShowImage() {
        return true;
    }

    private void glide(String url, ImageView target, boolean cropImage) {
        if (target != null) {
            try {
                if (cropImage) {
                    Glide.with(context).load(url)
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(target);
                } else {
                    target.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    Glide.with(context).load(url).fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(target);
                }
            } catch (Exception e) {
                // load after activity is destroyed
            }
        }
    }
}
