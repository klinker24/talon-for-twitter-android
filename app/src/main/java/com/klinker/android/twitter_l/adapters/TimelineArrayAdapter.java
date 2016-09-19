package com.klinker.android.twitter_l.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.afollestad.easyvideoplayer.EasyVideoPlayer;
import com.bumptech.glide.Glide;
import com.klinker.android.peekview.PeekViewActivity;
import com.klinker.android.peekview.builder.Peek;
import com.klinker.android.peekview.builder.PeekViewOptions;
import com.klinker.android.peekview.callback.OnPeek;
import com.klinker.android.peekview.callback.SimpleOnPeek;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.views.TweetView;
import com.klinker.android.twitter_l.views.badges.GifBadge;
import com.klinker.android.twitter_l.views.peeks.ProfilePeek;
import com.klinker.android.twitter_l.views.popups.QuickActionsPopup;
import com.klinker.android.twitter_l.views.badges.VideoBadge;
import com.klinker.android.twitter_l.activities.media_viewer.PhotoPagerActivity;
import com.klinker.android.twitter_l.activities.media_viewer.VideoViewerActivity;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.activities.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.activities.media_viewer.PhotoViewerActivity;
import com.klinker.android.twitter_l.utils.*;
import com.klinker.android.twitter_l.utils.text.TextUtils;
import com.klinker.android.twitter_l.utils.text.TouchableMovementMethod;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import twitter4j.Status;
import twitter4j.User;

public class TimelineArrayAdapter extends ArrayAdapter<Status> {

    public Map<Long, Status> quotedTweets = new HashMap();

    public boolean openFirst = false;

    public static final int NORMAL = 0;
    public static final int RETWEET = 1;
    public static final int FAVORITE = 2;

    public int embeddedTweetMinHeight = 0;

    public Context context;
    public List<Status> statuses;
    public LayoutInflater inflater;
    public AppSettings settings;

    public int layout;
    public Resources res;

    public ColorDrawable transparent;

    public Handler[] mHandler;
    public int currHandler = 0;

    public int type;
    public String username;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    private int normalPictures;
    private int smallPictures;

    private boolean canUseQuickActions = true;

    public void setCanUseQuickActions(boolean bool) {
        canUseQuickActions = bool;
    }

    public static class ViewHolder {
        public TextView name;
        public ImageView profilePic;
        public TextView tweet;
        public TextView time;
        public TextView retweeter;
        public LinearLayout expandArea;
        public ImageView image;
        public LinearLayout background;
        public ImageView playButton;
        public ImageView isAConversation;
        public TextView screenTV;
        public FrameLayout imageHolder;
        public View rootView;
        public CardView embeddedTweet;
        public View quickActions;

        public long tweetId;
        public boolean isFavorited;
        public String screenName;
        public String picUrl;
        public String retweeterName;
        public String animatedGif;

        public boolean preventNextClick = false;
    }

    private List<Status> removeMutes(List<Status> statuses) {
        AppSettings settings = AppSettings.getInstance(context);
        SharedPreferences sharedPrefs = settings.sharedPrefs;
        String mutedUsers = sharedPrefs.getString("muted_users", "");
        String mutedHashtags = sharedPrefs.getString("muted_hashtags", "");

        for (int i = 0; i < statuses.size(); i++) {
            if (mutedUsers.contains(statuses.get(i).getUser().getScreenName())) {
                statuses.remove(i);
                i--;
            } else if (statuses.get(i).isRetweet() &&
                    mutedUsers.contains(statuses.get(i).getRetweetedStatus().getUser().getScreenName())) {
                statuses.remove(i);
                i--;
            } else if (!isEmpty(mutedHashtags)) {
                for (String hashTag : mutedHashtags.split(" ")) {
                    if (statuses.get(i).getText().contains(hashTag)) {
                        statuses.remove(i);
                        i--;
                        break;
                    }
                }
            }
        }

        return statuses;
    }

    private boolean isEmpty(String s) {
        return android.text.TextUtils.isEmpty(s.replace(" ", ""));
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, boolean openFirst) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;

        this.username = "";
        this.openFirst = openFirst;

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, List<Status> statuses) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;

        this.username = "";

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, int type) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = type;
        this.username = "";

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, String username) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;
        this.username = username;

        setUpLayout();
    }

    public void setUpLayout() {

        statuses = removeMutes(statuses);

        normalPictures = (int) context.getResources().getDimension(R.dimen.header_condensed_height);
        smallPictures = Utils.toDP(120, context);

        mHandler = new Handler[4];

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        if (settings.picturesType != AppSettings.CONDENSED_TWEETS) {
            layout = R.layout.tweet;
        } else {
            layout = R.layout.tweet_condensed;
        }

        transparent = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));

        mHandler = new Handler[10];
        for (int i = 0; i < 10; i++) {
            mHandler[i] = new Handler();
        }
        currHandler = 0;

        embeddedTweetMinHeight = Utils.toDP(140, context);
    }

    @Override
    public int getCount() {
        return statuses.size();
    }

    @Override
    public Status getItem(int position) {
        return statuses.get(position);
    }

    public View newView(ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.profilePic = (ImageView) v.findViewById(R.id.profile_pic);
        holder.time = (TextView) v.findViewById(R.id.time);
        holder.tweet = (TextView) v.findViewById(R.id.tweet);
        holder.expandArea = (LinearLayout) v.findViewById(R.id.expansion);
        holder.retweeter = (TextView) v.findViewById(R.id.retweeter);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.screenTV = (TextView) v.findViewById(R.id.screenname);
        holder.isAConversation = (ImageView) v.findViewById(R.id.is_a_conversation);
        holder.embeddedTweet = (CardView) v.findViewById(R.id.embedded_tweet_card);
        holder.quickActions = v.findViewById(R.id.quick_actions);

        holder.playButton = (ImageView) v.findViewById(R.id.play_button);
        holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);
        holder.image = (ImageView) v.findViewById(R.id.image);

        //surfaceView.profilePic.setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.image.setClipToOutline(true);
        }

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.name.setTextSize(settings.textSize + 4);
        holder.screenTV.setTextSize(settings.textSize - 2);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);

        // some things we just don't need to configure every time
        holder.tweet.setSoundEffectsEnabled(false);
        holder.tweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TouchableMovementMethod.touched) {
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

        holder.retweeter.setSoundEffectsEnabled(false);
        holder.retweeter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TouchableMovementMethod.touched) {
                    holder.background.performClick();
                }
            }
        });

        holder.retweeter.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!TouchableMovementMethod.touched) {
                    holder.background.performLongClick();
                    holder.preventNextClick = true;
                }
                return false;
            }
        });

        if (settings.picturesType == AppSettings.PICTURES_SMALL &&
                holder.imageHolder.getHeight() != smallPictures) {
            ViewGroup.LayoutParams params = holder.imageHolder.getLayoutParams();
            params.height = smallPictures;
            holder.imageHolder.setLayoutParams(params);
        }

        holder.rootView = v;

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Status status, final int position) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (holder.embeddedTweet.getChildCount() > 0 || holder.embeddedTweet.getVisibility() == View.VISIBLE) {
            holder.embeddedTweet.removeAllViews();
            holder.embeddedTweet.setVisibility(View.GONE);
            holder.embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);
        }

        Status thisStatus;

        String retweeter;
        final long time = status.getCreatedAt().getTime();

        if (status.isRetweet()) {
            retweeter = status.getUser().getScreenName();
            thisStatus = status.getRetweetedStatus();
        } else {
            retweeter = "";
            thisStatus = status;
        }

        User user = thisStatus.getUser();

        holder.tweetId = thisStatus.getId();
        final long id = holder.tweetId;
        final String profilePic = user.getOriginalProfileImageURL();
        final String name = user.getName();
        final String screenname = user.getScreenName();

        String[] html = TweetLinkUtils.getLinksInStatus(thisStatus);
        final String tweetText = html[0];
        final String picUrl = html[1];
        holder.picUrl = picUrl;
        final String otherUrl = html[2];
        final String hashtags = html[3];
        final String users = html[4];

        holder.animatedGif = TweetLinkUtils.getGIFUrl(status, otherUrl);

        final boolean inAConversation = thisStatus.getInReplyToStatusId() != -1;

        if (inAConversation) {
            if (holder.isAConversation.getVisibility() != View.VISIBLE) {
                holder.isAConversation.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.isAConversation.getVisibility() != View.GONE) {
                holder.isAConversation.setVisibility(View.GONE);
            }
        }

        if (canUseQuickActions) {
            holder.quickActions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    QuickActionsPopup popup = new QuickActionsPopup(context, holder.tweetId, screenname, tweetText);
                    popup.setExpansionPointForAnim(holder.quickActions);
                    popup.setOnTopOfView(holder.quickActions);
                    popup.show();
                }
            });
        }

        final String fRetweeter = retweeter;
        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.preventNextClick) {
                    holder.preventNextClick = false;
                    return;
                }

                String link;

                boolean hasGif = holder.animatedGif != null && !holder.animatedGif.isEmpty();
                boolean displayPic = !holder.picUrl.equals("");
                if (displayPic) {
                    link = holder.picUrl;
                } else {
                    link = otherUrl.split("  ")[0];
                }

                Log.v("tweet_page", "clicked");
                Intent viewTweet = new Intent(context, TweetActivity.class);
                viewTweet.putExtra("name", name);
                viewTweet.putExtra("screenname", screenname);
                viewTweet.putExtra("time", time);
                viewTweet.putExtra("tweet", tweetText);
                viewTweet.putExtra("retweeter", fRetweeter);
                viewTweet.putExtra("webpage", link);
                viewTweet.putExtra("other_links", otherUrl);
                viewTweet.putExtra("picture", displayPic);
                viewTweet.putExtra("tweetid", holder.tweetId);
                viewTweet.putExtra("proPic", profilePic);
                viewTweet.putExtra("users", users);
                viewTweet.putExtra("hashtags", hashtags);
                viewTweet.putExtra("animated_gif", holder.animatedGif);

                viewTweet.putExtra("shared_trans", true);

                if (!openFirst)
                    viewTweet = addDimensForExpansion(viewTweet, holder.rootView);

                context.startActivity(viewTweet);
            }
        });

        if (context instanceof PeekViewActivity && settings.usePeek) {
            PeekViewOptions options = new PeekViewOptions()
                    .setAbsoluteWidth(225)
                    .setAbsoluteHeight(257);

            Peek.into(R.layout.peek_profile, new ProfilePeek(screenname))
                    .with(options)
                    .applyTo((PeekViewActivity) context, holder.profilePic);
        }

        if (!screenname.equals(username)) {
            holder.profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent viewProfile = new Intent(context, ProfilePager.class);
                    viewProfile.putExtra("name", name);
                    viewProfile.putExtra("screenname", screenname);
                    viewProfile.putExtra("proPic", profilePic);
                    viewProfile.putExtra("tweetid", holder.tweetId);
                    viewProfile.putExtra("retweet", holder.retweeter.getVisibility() == View.VISIBLE);
                    viewProfile.putExtra("long_click", false);

                    viewProfile = addDimensForExpansion(viewProfile, holder.profilePic);

                    context.startActivity(viewProfile);
                }
            });

            holder.profilePic.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {
                    Intent viewProfile = new Intent(context, ProfilePager.class);
                    viewProfile.putExtra("name", name);
                    viewProfile.putExtra("screenname", screenname);
                    viewProfile.putExtra("proPic", profilePic);
                    viewProfile.putExtra("tweetid", holder.tweetId);
                    viewProfile.putExtra("retweet", holder.retweeter.getVisibility() == View.VISIBLE);
                    viewProfile.putExtra("long_click", true);

                    viewProfile = addDimensForExpansion(viewProfile, holder.profilePic);

                    context.startActivity(viewProfile);
                    return false;
                }
            });
        } else {
            // need to clear the click listener so it isn't left over from another profile
            holder.profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            holder.profilePic.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {
                    return true;
                }
            });
        }

        holder.screenTV.setText("@" + screenname);
        holder.name.setText(name);

        if (!settings.absoluteDate) {
            holder.time.setText(Utils.getTimeAgo(time, context));
        } else {
            Date date = new Date(time);
            holder.time.setText(timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date));
        }

        boolean removeLastCharacters = false;
        boolean embeddedTweetFound = false;

        if (settings.inlinePics && tweetText.contains("pic.twitter.com/")) {
            if (tweetText.lastIndexOf(".") == tweetText.length() - 1) {
                removeLastCharacters = true;
            }
        } else if (settings.inlinePics && TweetView.isEmbeddedTweet(tweetText)) {
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

        if(settings.inlinePics) {
            if (holder.picUrl.equals("")) {
                if (holder.imageHolder.getVisibility() != View.GONE) {
                    holder.imageHolder.setVisibility(View.GONE);
                }

                if (holder.playButton.getVisibility() == View.VISIBLE) {
                    holder.playButton.setVisibility(View.GONE);
                }
            } else {
                if (holder.imageHolder.getVisibility() == View.GONE) {
                    holder.imageHolder.setVisibility(View.VISIBLE);
                }

                if (holder.picUrl.contains("youtube") || (holder.animatedGif != null && !android.text.TextUtils.isEmpty(holder.animatedGif))) {

                    if (holder.playButton.getVisibility() == View.GONE) {
                        holder.playButton.setVisibility(View.VISIBLE);
                    }

                    PeekViewOptions options = new PeekViewOptions();
                    options.setFullScreenPeek(true);
                    options.setBackgroundDim(1f);

                    int layoutRes = 0;
                    if (VideoMatcherUtil.isTwitterGifLink(holder.animatedGif)) {
                        holder.playButton.setImageDrawable(new GifBadge(context));
                        layoutRes = R.layout.peek_gif;
                    } else {
                        holder.playButton.setImageDrawable(new VideoBadge(context));

                        if (!holder.picUrl.contains("youtu")) {
                            layoutRes = R.layout.peek_video;
                        }
                    }

                    if (context instanceof PeekViewActivity && settings.usePeek) {
                        if (layoutRes != 0) {
                            Peek.into(layoutRes, new OnPeek() {
                                private EasyVideoPlayer videoView;

                                @Override public void shown() { }

                                @Override
                                public void onInflated(View rootView) {
                                    videoView = (EasyVideoPlayer) rootView.findViewById(R.id.video);
                                    videoView.setSource(Uri.parse(holder.animatedGif.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4")));
                                    videoView.setCallback(new EasyVideoCallbackWrapper());
                                }

                                @Override
                                public void dismissed() {
                                    videoView.release();
                                }
                            }).with(options).applyTo((PeekViewActivity) context, holder.image);
                        } else {
                            Peek.clear(holder.image);
                        }
                    }

                    holder.image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            VideoViewerActivity.startActivity(context, id, holder.animatedGif, otherUrl);
                        }
                    });

                    holder.image.setImageDrawable(transparent);

                    picture = true;

                } else {

                    holder.image.setImageDrawable(transparent);

                    picture = true;

                    if (holder.playButton.getVisibility() == View.VISIBLE) {
                        holder.playButton.setVisibility(View.GONE);
                    }

                    holder.image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (holder.picUrl.contains(" ")) {
                                PhotoPagerActivity.startActivity(context, id, holder.picUrl, 0);
                            } else {
                                PhotoViewerActivity.startActivity(context, id, holder.picUrl, holder.image);
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
                                Glide.with(context).load(holder.picUrl.split(" ")[0]).into((ImageView) rootView.findViewById(R.id.image));
                            }
                        }).with(options).applyTo((PeekViewActivity) context, holder.image);
                    }
                }

                if (holder.imageHolder.getVisibility() == View.GONE) {
                    holder.imageHolder.setVisibility(View.VISIBLE);
                }
            }
        }

        if (type == NORMAL) {
            if (retweeter.length() > 0) {
                holder.retweeter.setText(context.getResources().getString(R.string.retweeter) + retweeter);
                holder.retweeterName = retweeter;
                holder.retweeter.setVisibility(View.VISIBLE);
            } else if (holder.retweeter.getVisibility() == View.VISIBLE) {
                holder.retweeter.setVisibility(View.GONE);
            }
        } else if (type == RETWEET) {

            int count = status.getRetweetCount();

            if (count > 1) {
                holder.retweeter.setText(status.getRetweetCount() + " " + context.getResources().getString(R.string.retweets_lower));
                holder.retweeter.setVisibility(View.VISIBLE);
            } else if (count == 1) {
                holder.retweeter.setText(status.getRetweetCount() + " " + context.getResources().getString(R.string.retweet_lower));
                holder.retweeter.setVisibility(View.VISIBLE);
            }
        }

        holder.profilePic.setImageDrawable(null);
        Glide.with(context).load(profilePic).placeholder(null).into(holder.profilePic);

        holder.image.setImageDrawable(null);
        if (picture) {
            if (settings.picturesType != AppSettings.CONDENSED_TWEETS) {
                Glide.with(context).load(holder.picUrl).placeholder(null).into(holder.image);
            } else {
                Glide.with(context).load(holder.picUrl).fitCenter().placeholder(null).into(holder.image);
            }
        }

        mHandler[currHandler].removeCallbacksAndMessages(null);
        mHandler[currHandler].postDelayed(new Runnable() {
            @Override
            public void run() {
                if (holder.tweetId == id) {
                    TextUtils.linkifyText(context, holder.retweeter, holder.background, true, "", false);

                    if (TweetView.isEmbeddedTweet(otherUrl) && !otherUrl.contains("/photo/") &&
                            holder.embeddedTweet.getChildCount() == 0) {
                        loadEmbeddedTweet(holder, otherUrl);
                    }
                }
            }
        }, 400);
        currHandler++;

        if (currHandler == 10) {
            currHandler = 0;
        }

        // links are a problem on the array adapter popups... so lets do them immediately
        TextUtils.linkifyText(context, holder.tweet, holder.background, true, otherUrl, false);

        if (TweetView.isEmbeddedTweet(otherUrl) && !otherUrl.contains("/photo/")) {
            holder.embeddedTweet.setVisibility(View.VISIBLE);
            tryImmediateEmbeddedLoad(holder, otherUrl);
        }

        if (openFirst && position == 0) {
            holder.background.performClick();
            ((Activity)context).finish();
        }
    }

    private void tryImmediateEmbeddedLoad(final ViewHolder holder, String otherUrl) {
        Long embeddedId = 0l;
        for (String u : otherUrl.split(" ")) {
            if (TweetView.isEmbeddedTweet(u) && !otherUrl.contains("/photo/")) {
                embeddedId = TweetLinkUtils.getTweetIdFromLink(u);
                break;
            }
        }

        if (embeddedId != 0l && quotedTweets.containsKey(embeddedId)) {
            Status status = quotedTweets.get(embeddedId);
            TweetView v = new TweetView(context, status);
            v.setDisplayProfilePicture(settings.picturesType != AppSettings.CONDENSED_TWEETS);
            v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
            v.setSmallImage(true);

            holder.embeddedTweet.removeAllViews();
            holder.embeddedTweet.addView(v.getView());
            holder.embeddedTweet.setMinimumHeight(0);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;
        }

        bindView(v, statuses.get(position), position);

        return v;
    }

    public void loadEmbeddedTweet(final ViewHolder holder, final String otherUrls) {

        holder.embeddedTweet.setVisibility(View.VISIBLE);

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Long embeddedId = 0l;
                for (String u : otherUrls.split(" ")) {
                    if (TweetView.isEmbeddedTweet(u) && !u.contains("/photo/")) {
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
                            status = Utils.getTwitter(context, settings).showStatus(embeddedId);
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
                                v.setDisplayProfilePicture(settings.picturesType != AppSettings.CONDENSED_TWEETS);
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
}
