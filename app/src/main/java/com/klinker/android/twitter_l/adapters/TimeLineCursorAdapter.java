package com.klinker.android.twitter_l.adapters;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityManagerCompat;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.*;
import android.widget.*;

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
import com.klinker.android.simple_videoview.SimpleVideoView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.views.QuotedTweetView;
import com.klinker.android.twitter_l.views.TweetView;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.views.badges.GifBadge;
import com.klinker.android.twitter_l.views.peeks.ProfilePeek;
import com.klinker.android.twitter_l.views.popups.QuickActionsPopup;
import com.klinker.android.twitter_l.views.badges.VideoBadge;
import com.klinker.android.twitter_l.activities.media_viewer.PhotoPagerActivity;
import com.klinker.android.twitter_l.activities.media_viewer.VideoViewerActivity;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;
import twitter4j.Status;
import twitter4j.Twitter;

public class TimeLineCursorAdapter extends CursorAdapter {

    public Map<Long, Status> quotedTweets = new HashMap();

    public Set<String> muffledUsers = new HashSet<String>();
    public Cursor cursor;
    public AppSettings settings;
    public Context context;
    public LayoutInflater inflater;
    public boolean isDM = false;
    protected SharedPreferences sharedPrefs;
    public boolean secondAcc = false;

    private String othersText;
    private String replyToText;

    public int layout;
    public Resources res;

    private ColorDrawable transparent;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    public boolean isHomeTimeline;

    public int contentHeight = 0;
    public int headerMultiplier = 0;

    protected Handler[] mHandlers;
    protected int currHandler;

    private int normalPictures;
    private int smallPictures;

    public boolean hasConvo = false;

    public boolean hasExpandedTweet = false;

    private Handler videoHandler;

    private boolean isDataSaver = false;

    private boolean duelPanel;

    int embeddedTweetMinHeight;

    public static class ViewHolder {
        public TextView name;
        public TextView muffledName;
        public TextView screenTV;
        public ImageView profilePic;
        public TextView tweet;
        public TextView time;
        public TextView retweeter;
        public TextView replies;
        public LinearLayout expandArea;
        public ImageView image;
        public LinearLayout background;
        public ImageView playButton;
        public ImageView isAConversation;
        public FrameLayout imageHolder;
        public View rootView;
        public CardView embeddedTweet;
        public View quickActions;
        public SimpleVideoView videoView;
        public LinearLayout conversationArea;

        public long tweetId;
        public boolean isFavorited;
        public String proPicUrl;
        public String screenName;
        public String picUrl;
        public String retweeterName;
        public String gifUrl = "";

        public boolean preventNextClick = false;
    }

    // This is need for the case that the activity is paused while the handler is counting down
    // for the video playback still. If that happens, we definitely don't want to start the video.
    private boolean activityPaused = false;
    public void activityPaused(boolean paused) {
        activityPaused = paused;
    }

    public void init() {
        init(true);
    }
    public void init(boolean cont) {
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    ConnectivityManager connMgr = (ConnectivityManager)
                            context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connMgr.isActiveNetworkMetered() &&
                            (connMgr.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED ||
                                    connMgr.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED)) {
                        isDataSaver = true;
                    }
                }
            }
        }).start();

        videoHandler = new Handler();

        othersText = context.getString(R.string.others);
        replyToText = context.getString(R.string.reply_to);
        settings = AppSettings.getInstance(context);
        embeddedTweetMinHeight = settings.condensedTweets() ? Utils.toDP(70, context) : Utils.toDP(140, context);

        normalPictures = (int) context.getResources().getDimension(R.dimen.header_condensed_height);
        smallPictures = Utils.toDP(120, context);

        sharedPrefs = AppSettings.getSharedPreferences(context);

        duelPanel = AppSettings.dualPanels(context);

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !ActivityManagerCompat.isLowRamDevice(am)) {
            mHandlers = null;
        } else {
            mHandlers = new Handler[10];
            for (int i = 0; i < 10; i++) {
                mHandlers[i] = new Handler();
            }
        }

        if (!settings.condensedTweets()) {
            layout = R.layout.tweet;
        } else {
            layout = R.layout.tweet_condensed;
        }

        dateFormatter = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            dateFormatter = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        Locale locale = context.getResources().getConfiguration().locale;
        if (locale != null && !locale.getLanguage().equals("en")) {
            dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        }

        transparent = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (cont && context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

        for (String s : cursor.getColumnNames()) {
            if (s.equals(HomeSQLiteHelper.COLUMN_CONVERSATION)) {
                hasConvo = true;
            }
        }

        muffledUsers = sharedPrefs.getStringSet("muffled_users", new HashSet<String>());

        TWEET_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID);
        PRO_PIC_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC);
        TEXT_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT);
        NAME_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME);
        SCREEN_NAME_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME);
        PIC_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL);
        TIME_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME);
        URL_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_URL);
        USER_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_USERS);
        HASHTAG_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_HASHTAGS);
        GIF_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ANIMATED_GIF);
        RETWEETER_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER);

        if (hasConvo) {
            CONVO_COL = cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_CONVERSATION);
        } else {
            CONVO_COL = -1;
        }
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, boolean isHomeTimeline, Expandable expander) {
        super(context, cursor, 0);

        this.isHomeTimeline = isHomeTimeline;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        init();
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, boolean isHomeTimeline) {
        super(context, cursor, 0);

        this.isHomeTimeline = isHomeTimeline;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        init();
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, Expandable expander, boolean secondAcc) {
        super(context, cursor, 0);

        this.isHomeTimeline = false;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = false;
        this.secondAcc = secondAcc;

        init();
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, Expandable expander) {
        super(context, cursor, 0);

        this.isHomeTimeline = false;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        init();
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM) {
        super(context, cursor, 0);

        this.isHomeTimeline = false;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;
        
        init(false);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.muffledName = (TextView) v.findViewById(R.id.muffled_name);
        holder.screenTV = (TextView) v.findViewById(R.id.screenname);
        holder.profilePic = (CircleImageView) v.findViewById(R.id.profile_pic);
        holder.time = (TextView) v.findViewById(R.id.time);
        holder.tweet = (TextView) v.findViewById(R.id.tweet);
        holder.expandArea = (LinearLayout) v.findViewById(R.id.expansion);
        holder.retweeter = (TextView) v.findViewById(R.id.retweeter);
        holder.replies = (TextView) v.findViewById(R.id.reply_to);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.isAConversation = (ImageView) v.findViewById(R.id.is_a_conversation);
        holder.embeddedTweet = (CardView) v.findViewById(R.id.embedded_tweet_card);
        holder.quickActions = v.findViewById(R.id.quick_actions);
        holder.image = (ImageView) v.findViewById(R.id.image);
        holder.playButton = (ImageView) v.findViewById(R.id.play_button);
        holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);
        holder.videoView = (SimpleVideoView) v.findViewById(R.id.video_view);
        holder.conversationArea = (LinearLayout) v.findViewById(R.id.conversation_area);

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.screenTV.setTextSize(settings.textSize - 2);
        holder.name.setTextSize(settings.textSize + 4);
        holder.muffledName.setTextSize(settings.textSize);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);
        holder.replies.setTextSize(settings.textSize - 2);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.image.setClipToOutline(true);
        }

        // some things we just don't need to configure every time
        holder.muffledName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.background.performClick();
            }
        });

        holder.expandArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.background.performClick();
            }
        });

        holder.expandArea.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                holder.background.performLongClick();
                return false;
            }
        });

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

        if (settings.detailedQuotes) {
            holder.embeddedTweet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);
        }

        holder.rootView = v;

        v.setTag(holder);

        return v;
    }

    private List<Video> videos = new ArrayList();

    public void playCurrentVideo() {
        for (Video v : videos) {
            v.playCurrentVideo();
        }
    }

    public void stopOnScroll() {
        for (Video v : videos) {
            v.stopOnScroll();
        }
    }

    public void stopOnScroll(int firstVisible, int lastVisible) {
        for (Video v : videos) {
            Log.v("talon_video", "video position: " + v.positionOnTimeline + ", first: " + firstVisible + ", last: " + lastVisible);
            if (v.positionOnTimeline > lastVisible || v.positionOnTimeline < firstVisible) {
                v.stopOnScroll();
            }
        }
    }

    public void resetVideoHandler() {
        videoHandler.removeCallbacksAndMessages(null);
    }

    protected int TWEET_COL;
    protected int PRO_PIC_COL;
    protected int TEXT_COL;
    protected int NAME_COL;
    protected int SCREEN_NAME_COL;
    protected int PIC_COL;
    protected int TIME_COL;
    protected int URL_COL;
    protected int USER_COL;
    protected int HASHTAG_COL;
    protected int GIF_COL;
    protected int CONVO_COL;
    protected int RETWEETER_COL;

    @Override
    public void bindView(final View view, Context mContext, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (holder.expandArea.getVisibility() != View.GONE) {
            removeExpansion(holder, false);
        }

        if (holder.embeddedTweet.getChildCount() > 0 || holder.embeddedTweet.getVisibility() == View.VISIBLE) {
            holder.embeddedTweet.removeAllViews();
            holder.embeddedTweet.setVisibility(View.GONE);

            if (settings.detailedQuotes) {
                holder.embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);
            }
        }

        if (holder.conversationArea.getChildCount() > 0) {
            holder.conversationArea.removeAllViews();

            ViewGroup.LayoutParams params = holder.conversationArea.getLayoutParams();
            params.height = 0;
            holder.conversationArea.setLayoutParams(params);
        }

        for (int i = 0; i < videos.size(); i++) {
            if (holder.tweetId == videos.get(i).tweetId) {
                // recycling the playing videos layout since it is off the screen
                videos.get(i).releaseVideo();
                videos.remove(i);
                i--;
            }
        }

        final long id = cursor.getLong(TWEET_COL);
        holder.tweetId = id;
        final String profilePic = cursor.getString(PRO_PIC_COL);
        holder.proPicUrl = profilePic;
        String tweetTexts = cursor.getString(TEXT_COL);
        final String name = cursor.getString(NAME_COL);
        final String screenname = cursor.getString(SCREEN_NAME_COL);
        final String picUrl = cursor.getString(PIC_COL);
        holder.picUrl = picUrl;
        final long longTime = cursor.getLong(TIME_COL);
        final String otherUrl = cursor.getString(URL_COL);
        final String users = cursor.getString(USER_COL);
        final String hashtags = cursor.getString(HASHTAG_COL);

        holder.gifUrl = cursor.getString(GIF_COL);

        final boolean inAConversation;
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

        final String tweetWithReplyHandles = tweetTexts;

        final String replies = ReplyUtils.getReplyingToHandles(tweetTexts);
        if (inAConversation && settings.compressReplies && replies != null && !replies.isEmpty()) {
            tweetTexts = tweetTexts.replace(replies, "");

            if (ReplyUtils.showMultipleReplyNames(replies)) {
                holder.replies.setText(replyToText + " " + replies);
                TextUtils.linkifyText(context, holder.replies, holder.background, true, "", false);
            } else {
                final String firstPerson = replies.split(" ")[0];
                holder.replies.setText(replyToText + " " + firstPerson + " & " + othersText);

                Link others = new Link(othersText)
                        .setUnderlined(false)
                        .setTextColor(settings.themeColors.accentColor)
                        .setOnClickListener(new Link.OnClickListener() {
                            @Override
                            public void onClick(String clickedText) {
                                final String[] repliesSplit = replies.split(" ");
                                new AlertDialog.Builder(context).setItems(replies.split(" "), new DialogInterface.OnClickListener() {
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

                LinkBuilder.on(holder.replies).addLink(others).addLink(first).build();
            }

            if (holder.replies.getVisibility() != View.VISIBLE) {
                holder.replies.setVisibility(View.VISIBLE);
            }
        } else if (holder.replies.getVisibility() != View.GONE) {
            holder.replies.setVisibility(View.GONE);
        }

        String retweeter;
        try {
            retweeter = cursor.getString(RETWEETER_COL);
        } catch (Exception e) {
            retweeter = "";
        }
        final String fRetweeter = retweeter;

        final String tweetText = tweetTexts;

        final boolean muffled;
        if (muffledUsers.contains(screenname) ||
                (retweeter != null && !android.text.TextUtils.isEmpty(retweeter) && muffledUsers.contains(retweeter))) {
            if (holder.background.getVisibility() != View.GONE) {
                holder.background.setVisibility(View.GONE);
                holder.muffledName.setVisibility(View.VISIBLE);
            }
            muffled = true;
        } else {
            if (holder.background.getVisibility() != View.VISIBLE) {
                holder.background.setVisibility(View.VISIBLE);
                holder.muffledName.setVisibility(View.GONE);
            }
            muffled = false;
        }

        holder.quickActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QuickActionsPopup popup = new QuickActionsPopup(context, holder.tweetId, screenname, fRetweeter, tweetWithReplyHandles, secondAcc);
                popup.setExpansionPointForAnim(holder.quickActions);
                popup.setOnTopOfView(holder.quickActions);
                popup.show();
            }
        });

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.preventNextClick) {
                    holder.preventNextClick = false;
                    return;
                }

                if (holder.expandArea.getVisibility() != View.GONE) {
                    removeExpansion(holder, true);
                }

                String link;
                boolean displayPic = !holder.picUrl.equals("");
                if (displayPic) {
                    link = holder.picUrl;
                } else {
                    link = otherUrl.split("  ")[0];
                }

                Intent viewTweet = new Intent(context, TweetActivity.class);
                viewTweet.putExtra("name", name);
                viewTweet.putExtra("screenname", screenname);
                viewTweet.putExtra("time", longTime);
                viewTweet.putExtra("tweet", tweetWithReplyHandles);
                viewTweet.putExtra("retweeter", fRetweeter);
                viewTweet.putExtra("webpage", link);
                viewTweet.putExtra("other_links", otherUrl);
                viewTweet.putExtra("picture", displayPic);
                viewTweet.putExtra("tweetid", holder.tweetId);
                viewTweet.putExtra("proPic", profilePic);
                viewTweet.putExtra("users", users);
                viewTweet.putExtra("hashtags", hashtags);
                viewTweet.putExtra("animated_gif", holder.gifUrl);
                viewTweet.putExtra("conversation", inAConversation);

                if (secondAcc) {
                    String text = context.getString(R.string.using_second_account).replace("%s", "@" + settings.secondScreenName);
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                    viewTweet.putExtra("second_account", true);
                }

                TweetActivity.applyDragDismissBundle(context, viewTweet);

                context.startActivity(viewTweet);
            }
        });

        holder.background.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (holder.expandArea.getVisibility() == View.GONE) {
                    addExpansion(holder, id);
                } else {
                    removeExpansion(holder, true);
                }

                return true;
            }
        });

        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isHomeTimeline) {
                    sharedPrefs.edit()
                            .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                            .apply();
                }

                ProfilePager.start(context, name, screenname, holder.proPicUrl);
            }
        });

        holder.screenTV.setText("@" + screenname);
        holder.name.setText(name);

        if (muffled) {
            String t = "<b>@" + screenname + "</b>: " + tweetText;
            holder.muffledName.setText(Html.fromHtml(t));
        }

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
        boolean picturePeek = false;
        int videoPeekLayout = -1;

        if (holder.videoView.getVisibility() == View.VISIBLE) {
            holder.videoView.setVisibility(View.GONE);
        }

        if(settings.inlinePics && holder.picUrl != null && !holder.picUrl.equals("")) {
            // there is a picture in the tweet

            if (holder.imageHolder.getVisibility() == View.GONE) {
                holder.imageHolder.setVisibility(View.VISIBLE);
            }

            if (holder.picUrl.contains("youtube") || (holder.gifUrl != null && !android.text.TextUtils.isEmpty(holder.gifUrl))) {
                // video tag on the picture

                if (holder.playButton.getVisibility() == View.GONE) {
                    holder.playButton.setVisibility(View.VISIBLE);
                }

                if (VideoMatcherUtil.isTwitterGifLink(holder.gifUrl)) {
                    holder.playButton.setImageDrawable(new GifBadge(context));
                    videoPeekLayout = R.layout.peek_gif;
                } else {
                    holder.playButton.setImageDrawable(new VideoBadge(context));

                    if (!holder.picUrl.contains("youtube")) {
                        videoPeekLayout = R.layout.peek_video;
                    }
                }

                holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        VideoViewerActivity.startActivity(context, id, holder.gifUrl, otherUrl);
                    }
                });

                if (holder.gifUrl.contains(".mp4") || holder.gifUrl.contains(".m3u8")) {
                    videos.add(new Video(holder.videoView, holder.tweetId, holder.gifUrl, cursor.getPosition()));
                }

                picture = true;
            } else {
                // no video tag, just the picture
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

                        holder.imageHolder.setOnClickListener(null);
                        final View.OnClickListener thisListener = this;
                        holder.imageHolder.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                holder.imageHolder.setOnClickListener(thisListener);
                            }
                        }, 300);
                    }
                });

                picturePeek = true;
                picture = true;
            }
        } else {
            if (holder.imageHolder.getVisibility() != View.GONE) {
                holder.imageHolder.setVisibility(View.GONE);
            }

            if (holder.playButton.getVisibility() == View.VISIBLE) {
                holder.playButton.setVisibility(View.GONE);
            }
        }


        if (retweeter.length() > 0 && !isDM) {
            String text = context.getResources().getString(R.string.retweeter);
            holder.retweeter.setText(text + retweeter);
            holder.retweeterName = retweeter;

            if (holder.retweeter.getVisibility() != View.VISIBLE) {
                holder.retweeter.setVisibility(View.VISIBLE);
            }
        } else if (holder.retweeter.getVisibility() == View.VISIBLE) {
            holder.retweeter.setVisibility(View.GONE);
        }

        if (picture) {
            if (!settings.condensedTweets()) {
                if (settings.cropImagesOnTimeline) {
                    Glide.with(context).load(holder.picUrl).centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .placeholder(null).into(holder.image);
                } else {
                    holder.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    Glide.with(context).load(holder.picUrl).fitCenter()
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .placeholder(null).into(holder.image);
                }
            } else {
                Glide.with(context).load(holder.picUrl).fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .placeholder(null).into(holder.image);
            }
        }

        if (settings.showProfilePictures) {
            Glide.with(context).load(holder.proPicUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .placeholder(null).into(holder.profilePic);
        } else if (holder.profilePic.getVisibility() != View.GONE) {
            holder.profilePic.setVisibility(View.GONE);
            holder.name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.profilePic.performClick();
                }
            });
        }

        if (mHandlers != null) {
            final boolean picturePeekF = picturePeek;
            final int videoPeekF = videoPeekLayout;

            mHandlers[currHandler].removeCallbacksAndMessages(null);
            mHandlers[currHandler].postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (holder.tweetId == id) {
                        TextUtils.linkifyText(context, holder.tweet, holder.background, true, otherUrl, false);
                        TextUtils.linkifyText(context, holder.retweeter, holder.background, true, "", false);

                        if (TweetView.isEmbeddedTweet(tweetText) &&
                                holder.embeddedTweet.getChildCount() == 0) {
                            loadEmbeddedTweet(holder, otherUrl);
                        }

                        if (settings.usePeek) {
                            ProfilePeek.create(context, holder.profilePic, screenname);

                            if (picturePeekF) {
                                if (context instanceof PeekViewActivity) {
                                    PeekViewOptions options = new PeekViewOptions();
                                    options.setFullScreenPeek(true);
                                    options.setBackgroundDim(1f);

                                    Peek.into(R.layout.peek_image, new SimpleOnPeek() {
                                        @Override
                                        public void onInflated(View rootView) {
                                            Glide.with(context).load(holder.picUrl.split(" ")[0])
                                                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                                    .into((ImageView) rootView.findViewById(R.id.image));
                                        }
                                    }).with(options).applyTo((PeekViewActivity) context, holder.imageHolder);
                                }
                            } else if (videoPeekF != -1) {
                                if (context instanceof PeekViewActivity) {
                                    if (videoPeekF != 0 && !holder.gifUrl.contains("youtu")) {

                                        PeekViewOptions options = new PeekViewOptions();
                                        options.setFullScreenPeek(true);
                                        options.setBackgroundDim(1f);

                                        Peek.into(videoPeekF, new OnPeek() {
                                            private BetterVideoPlayer videoView;

                                            @Override
                                            public void shown() {
                                            }

                                            @Override
                                            public void onInflated(View rootView) {
                                                videoView = (BetterVideoPlayer) rootView.findViewById(R.id.video);
                                                videoView.setSource(Uri.parse(holder.gifUrl.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4")));
                                                videoView.setCallback(new BetterVideoCallbackWrapper());
                                            }

                                            @Override
                                            public void dismissed() {
                                                videoView.release();
                                            }
                                        }).with(options).applyTo((PeekViewActivity) context, holder.imageHolder);
                                    } else {
                                        Peek.clear(holder.imageHolder);
                                    }
                                }
                            } else {
                                Peek.clear(holder.imageHolder);
                            }
                        }
                    }
                }
            }, 400);
            currHandler++;

            if (currHandler == 10) {
                currHandler = 0;
            }

            if (TweetView.isEmbeddedTweet(tweetText)) {
                holder.embeddedTweet.setVisibility(View.VISIBLE);
                tryImmediateEmbeddedLoad(holder, otherUrl);
            }
        } else {
            TextUtils.linkifyText(context, holder.tweet, holder.background, true, otherUrl, false);
            TextUtils.linkifyText(context, holder.retweeter, holder.background, true, "", false);

            if (settings.usePeek) {
                ProfilePeek.create(context, holder.profilePic, screenname);

                if (picturePeek) {
                    if (context instanceof PeekViewActivity) {
                        PeekViewOptions options = new PeekViewOptions();
                        options.setFullScreenPeek(true);
                        options.setBackgroundDim(1f);

                        Peek.into(R.layout.peek_image, new SimpleOnPeek() {
                            @Override
                            public void onInflated(View rootView) {
                                Glide.with(context).load(holder.picUrl.split(" ")[0])
                                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                        .into((ImageView) rootView.findViewById(R.id.image));
                            }
                        }).with(options).applyTo((PeekViewActivity) context, holder.imageHolder);
                    }
                } else if (videoPeekLayout != -1) {
                    if (context instanceof PeekViewActivity) {
                        if (videoPeekLayout != 0 && !holder.gifUrl.contains("youtu")) {

                            PeekViewOptions options = new PeekViewOptions();
                            options.setFullScreenPeek(true);
                            options.setBackgroundDim(1f);

                            Peek.into(videoPeekLayout, new OnPeek() {
                                private BetterVideoPlayer videoView;

                                @Override
                                public void shown() {
                                }

                                @Override
                                public void onInflated(View rootView) {
                                    videoView = (BetterVideoPlayer) rootView.findViewById(R.id.video);
                                    videoView.setSource(Uri.parse(holder.gifUrl.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4")));
                                    videoView.setCallback(new BetterVideoCallbackWrapper());
                                }

                                @Override
                                public void dismissed() {
                                    videoView.release();
                                }
                            }).with(options).applyTo((PeekViewActivity) context, holder.imageHolder);
                        } else {
                            Peek.clear(holder.imageHolder);
                        }
                    }
                } else {
                    Peek.clear(holder.imageHolder);
                }
            }

            if (TweetView.isEmbeddedTweet(tweetText)) {
                holder.embeddedTweet.setVisibility(View.VISIBLE);
                if (!tryImmediateEmbeddedLoad(holder, otherUrl)) {
                    loadEmbeddedTweet(holder, otherUrl);
                }
            }
        }
    }

    private boolean tryImmediateEmbeddedLoad(final ViewHolder holder, String otherUrl) {
        Long embeddedId = 0l;
        for (String u : otherUrl.split(" ")) {
            if (u.contains("/status/") && !u.contains("/i/web/")) {
                embeddedId = TweetLinkUtils.getTweetIdFromLink(u);
                break;
            }
        }

        if (embeddedId != 0l && quotedTweets.containsKey(embeddedId)) {
            Status status = quotedTweets.get(embeddedId);
            TweetView v = QuotedTweetView.create(context, status);
            v.setDisplayProfilePicture(!settings.condensedTweets());
            v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
            v.setSmallImage(true);

            holder.embeddedTweet.removeAllViews();
            holder.embeddedTweet.addView(v.getView());

            if (settings.detailedQuotes) {
                holder.embeddedTweet.setMinimumHeight(0);
            }

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

    public void removeExpansion(final ViewHolder holder, boolean anim) {
        ValueAnimator heightAnimatorContent = ValueAnimator.ofInt(holder.expandArea.getHeight(), 0);
        heightAnimatorContent.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = holder.expandArea.getLayoutParams();
                layoutParams.height = val;
                holder.expandArea.setLayoutParams(layoutParams);
            }
        });
        heightAnimatorContent.setDuration(anim ? ANIMATION_DURATION : 0);
        heightAnimatorContent.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(heightAnimatorContent);

        if (anim) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    holder.expandArea.setVisibility(View.GONE);
                    holder.expandArea.removeAllViews();
                    hasExpandedTweet = false;
                }
            }, ANIMATION_DURATION);
        } else {
            holder.expandArea.setVisibility(View.GONE);
            hasExpandedTweet = false;
        }

    }

    protected void startAnimation(Animator animator) {
        animator.start();
    }

    public static final int ANIMATION_DURATION = 100;
    public static Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

    public void addExpansion(final ViewHolder holder, final long tweetId) {
        hasExpandedTweet = true;

        final View buttons = LayoutInflater.from(holder.background.getContext()).inflate(R.layout.tweet_expansion_buttons, null, false);
        final View counts = LayoutInflater.from(holder.background.getContext()).inflate(R.layout.tweet_expansion_counts, null, false);
        buttons.setPadding(0, Utils.toDP(12, context), 0, Utils.toDP(12, context));

        if (settings.darkTheme) {
            buttons.findViewById(R.id.compose_button).setAlpha(.75f);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = getTwitter();
                    Status s = twitter.showStatus(tweetId);
                    final Status status = s.isRetweet() ? s.getRetweetedStatus() : s;

                    counts.post(new Runnable() {
                        @Override
                        public void run() {
                            TweetButtonUtils utils = new TweetButtonUtils(context);
                            utils.setUpButtons(status, counts, buttons, false);
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();

        final int expansionSize = Utils.toDP(64, context);
        ValueAnimator heightAnimatorContent = ValueAnimator.ofInt(0, expansionSize);
        heightAnimatorContent.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = holder.expandArea.getLayoutParams();
                layoutParams.height = val;
                holder.expandArea.setLayoutParams(layoutParams);
            }
        });
        heightAnimatorContent.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationCancel(Animator animation) { }
            @Override public void onAnimationRepeat(Animator animation) { }
            @Override public void onAnimationStart(Animator animation) {
                holder.expandArea.setVisibility(View.VISIBLE);
            }
            @Override public void onAnimationEnd(Animator animation) {
                if (holder.expandArea.getChildCount() > 0) {
                    holder.expandArea.removeAllViews();
                }

                holder.expandArea.setMinimumHeight(expansionSize);
                holder.expandArea.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.expandArea.invalidate();

                holder.expandArea.addView(counts);
                holder.expandArea.addView(buttons);
            }
        });

        heightAnimatorContent.setDuration(ANIMATION_DURATION);
        heightAnimatorContent.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(heightAnimatorContent);

    }

    public Twitter getTwitter() {
        if (secondAcc) {
            return Utils.getSecondTwitter(context);
        } else {
            return Utils.getTwitter(context, settings);
        }
    }

    public void loadEmbeddedTweet(final ViewHolder holder, final String otherUrls) {

        holder.embeddedTweet.setVisibility(View.VISIBLE);

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Long embeddedId = 0l;
                for (String u : otherUrls.split(" ")) {
                    if (u.contains("/status/") && !u.contains("/i/web/")) {
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
                                TweetView v = QuotedTweetView.create(context, embedded);
                                v.setDisplayProfilePicture(!settings.condensedTweets());
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

    private class Video {
        public int positionOnTimeline;
        public String url;
        public long tweetId;
        public SimpleVideoView videoView;

        public Video(SimpleVideoView videoView, long tweetId, String url, int positionOnTimeline) {
            this.videoView = videoView;
            this.tweetId = tweetId;
            this.url = url;
            this.positionOnTimeline = getCount() - positionOnTimeline + 1;
        }

        private boolean isPlaying = false;
        public void playCurrentVideo() {
            if (!activityPaused && videoView != null && !isDataSaver) {
                videoHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!activityPaused && videoView != null && !Video.this.isPlaying) {
                            if (settings.autoplay == AppSettings.AUTOPLAY_ALWAYS ||
                                    (settings.autoplay == AppSettings.AUTOPLAY_WIFI && !Utils.getConnectionStatus(context))) {
                                if (videoView.getVisibility() != View.VISIBLE) {
                                    videoView.setVisibility(View.VISIBLE);
                                }
                                videoView.start(url);
                                Video.this.isPlaying = true;
                            }
                        }
                    }
                }, 500);
            }
        }

        public void stopOnScroll() {
            if (videoView != null && Video.this.isPlaying) {
                videoView.release();
                videoView.setVisibility(View.GONE);
            }
            resetVideoHandler();
            Video.this.isPlaying = false;
        }

        public void releaseVideo() {
            if (videoView != null) {
                videoView.setVisibility(View.GONE);
                videoView.release();
                tweetId = -1;
                videoView = null;
                url = null;
            }
        }
    }


    public Map<Long, Status> getQuotedTweets() {
        return quotedTweets;
    }

    public void setQuotedTweets(Map<Long, Status> quotedTweets) {
        this.quotedTweets = quotedTweets;
    }
}
