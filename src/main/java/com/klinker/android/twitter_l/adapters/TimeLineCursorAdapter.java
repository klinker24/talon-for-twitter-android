package com.klinker.android.twitter_l.adapters;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.*;
import android.widget.*;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.klinker.android.simple_videoview.SimpleVideoView;
import com.klinker.android.twitter_l.BuildConfig;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.TweetView;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.manipulations.GifBadge;
import com.klinker.android.twitter_l.manipulations.MultiplePicsPopup;
import com.klinker.android.twitter_l.manipulations.QuickActionsPopup;
import com.klinker.android.twitter_l.manipulations.VideoBadge;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoPagerActivity;
import com.klinker.android.twitter_l.manipulations.photo_viewer.VideoViewerActivity;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.BrowserActivity;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.ui.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.ui.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoViewerActivity;
import com.klinker.android.twitter_l.utils.*;
import com.klinker.android.twitter_l.utils.api_helper.TwitterDMPicHelper;
import com.klinker.android.twitter_l.utils.text.TextUtils;
import com.klinker.android.twitter_l.utils.text.TouchableMovementMethod;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import de.hdodenhof.circleimageview.CircleImageView;
import lombok.Getter;
import lombok.Setter;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TimeLineCursorAdapter extends CursorAdapter {

    @Getter @Setter
    public Map<Long, Status> quotedTweets = new HashMap();

    public Set<String> muffledUsers = new HashSet<String>();
    public Cursor cursor;
    public AppSettings settings;
    public Context context;
    public LayoutInflater inflater;
    public boolean isDM = false;
    protected SharedPreferences sharedPrefs;
    public boolean secondAcc = false;

    protected Handler[] mHandlers;
    protected int currHandler;

    public int layout;
    public Resources res;

    private ColorDrawable transparent;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    public boolean isHomeTimeline;

    public int embeddedTweetMinHeight = 0;

    public int contentHeight = 0;
    public int headerMultiplier = 0;
    public Expandable expander;

    private int normalPictures;
    private int smallPictures;

    public boolean hasConvo = false;

    public boolean hasExpandedTweet = false;

    private Handler videoHandler;

    private boolean isDataSaver = false;

    public static class ViewHolder {
        public TextView name;
        public TextView muffledName;
        public TextView screenTV;
        public ImageView profilePic;
        public TextView tweet;
        public TextView time;
        public TextView retweeter;
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

        public long tweetId;
        public boolean isFavorited;
        public String proPicUrl;
        public String screenName;
        public String picUrl;
        public String retweeterName;
        public String gifUrl = "";

        public boolean preventNextClick = false;

        public ExpansionViewHelper expandHelper;
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
        new Thread(new Runnable() {
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

        settings = AppSettings.getInstance(context);

        normalPictures = (int) context.getResources().getDimension(R.dimen.header_condensed_height);
        smallPictures = Utils.toDP(120, context);

        sharedPrefs = AppSettings.getSharedPreferences(context);


        if (settings.picturesType != AppSettings.CONDENSED_TWEETS) {
            layout = R.layout.tweet;
        } else {
            layout = R.layout.tweet_condensed;
        }

        dateFormatter = android.text.format.DateFormat.getMediumDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        transparent = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));

        mHandlers = new Handler[10];
        for (int i = 0; i < 10; i++) {
            mHandlers[i] = new Handler();
        }

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

        embeddedTweetMinHeight = Utils.toDP(140, context);
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, boolean isHomeTimeline, Expandable expander) {
        super(context, cursor, 0);

        this.isHomeTimeline = isHomeTimeline;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;
        this.expander = expander;

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
        this.expander = expander;
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
        this.expander = expander;

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
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.isAConversation = (ImageView) v.findViewById(R.id.is_a_conversation);
        holder.embeddedTweet = (CardView) v.findViewById(R.id.embedded_tweet_card);
        holder.quickActions = v.findViewById(R.id.quick_actions);
        holder.image = (ImageView) v.findViewById(R.id.image);
        holder.playButton = (ImageView) v.findViewById(R.id.play_button);
        holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);
        holder.videoView = (SimpleVideoView) v.findViewById(R.id.video_view);

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.screenTV.setTextSize(settings.textSize - 2);
        holder.name.setTextSize(settings.textSize + 4);
        holder.muffledName.setTextSize(settings.textSize);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);

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

    public void resetVideoHandler() {
        videoHandler.removeCallbacksAndMessages(null);
    }

    public void releaseVideo() {
        for (Video v : videos) {
            v.releaseVideo();
        }
        videos.clear();
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
            holder.embeddedTweet.setMinimumHeight(embeddedTweetMinHeight);
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

        final int position = cursor.getPosition();

        String retweeter;
        try {
            retweeter = cursor.getString(RETWEETER_COL);
        } catch (Exception e) {
            retweeter = "";
        }

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

        if (settings.reverseClickActions) {
            holder.quickActions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    QuickActionsPopup popup = new QuickActionsPopup(context, holder.tweetId, screenname, tweetTexts, secondAcc);
                    popup.setExpansionPointForAnim(holder.quickActions);
                    popup.setOnTopOfView(holder.quickActions);
                    popup.show();
                }
            });
        }

        if(settings.reverseClickActions || expander == null || MainActivity.isPopup || muffled) {
            final String fRetweeter = retweeter;
            holder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.preventNextClick) {
                        holder.preventNextClick = false;
                        return;
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
                    viewTweet.putExtra("tweet", tweetText);
                    viewTweet.putExtra("retweeter", fRetweeter);
                    viewTweet.putExtra("webpage", link);
                    viewTweet.putExtra("other_links", otherUrl);
                    viewTweet.putExtra("picture", displayPic);
                    viewTweet.putExtra("tweetid", holder.tweetId);
                    viewTweet.putExtra("proPic", profilePic);
                    viewTweet.putExtra("users", users);
                    viewTweet.putExtra("hashtags", hashtags);
                    viewTweet.putExtra("animated_gif", holder.gifUrl);

                    if (secondAcc) {
                        String text = context.getString(R.string.using_second_account).replace("%s", "@" + settings.secondScreenName);
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                        viewTweet.putExtra("second_account", true);
                    }

                    viewTweet.putExtra("shared_trans", true);
                    viewTweet = addDimensForExpansion(viewTweet, holder.rootView);

                    context.startActivity(viewTweet);
                }
            });

            if (expander != null && !MainActivity.isPopup) {
                holder.background.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if (holder.expandArea.getVisibility() == View.GONE) {
                            if (!(VideoMatcherUtil.containsThirdPartyVideo(tweetTexts))) {
                                addExpansion(holder, position, screenname, users, otherUrl.split("  "), holder.picUrl, id, hashtags.split("  "));
                            } else {
                                holder.background.performClick();
                            }
                        } else {
                            removeExpansion(holder, true);
                        }

                        return true;
                    }
                });
            }

        } else  {
            final String fRetweeter = retweeter;

            holder.background.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
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
                    viewTweet.putExtra("tweet", tweetText);
                    viewTweet.putExtra("retweeter", fRetweeter);
                    viewTweet.putExtra("webpage", link);
                    viewTweet.putExtra("other_links", otherUrl);
                    viewTweet.putExtra("picture", displayPic);
                    viewTweet.putExtra("tweetid", holder.tweetId);
                    viewTweet.putExtra("proPic", profilePic);
                    viewTweet.putExtra("users", users);
                    viewTweet.putExtra("hashtags", hashtags);
                    viewTweet.putExtra("animated_gif", holder.gifUrl);

                    if (secondAcc) {
                        String text = context.getString(R.string.using_second_account).replace("%s", "@" + settings.secondScreenName);
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                        viewTweet.putExtra("second_account", true);
                    }

                    viewTweet.putExtra("shared_trans", true);
                    viewTweet = addDimensForExpansion(viewTweet, holder.rootView);

                    context.startActivity(viewTweet);

                    return true;
                }
            });

            if (expander != null) {
                holder.background.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (holder.preventNextClick) {
                            holder.preventNextClick = false;
                            return;
                        }

                        if (holder.expandArea.getVisibility() == View.GONE) {
                            if (!VideoMatcherUtil.containsThirdPartyVideo(tweetTexts)) {
                                addExpansion(holder, position, screenname, users, otherUrl.split("  "), holder.picUrl, id, hashtags.split("  "));
                            } else {
                                holder.background.performLongClick();
                            }
                        } else {
                            removeExpansion(holder, true);
                        }
                    }
                });
            }
        }

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

                viewProfile = addDimensForExpansion(viewProfile, holder.profilePic);

                context.startActivity(viewProfile);
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
                } else {
                    holder.playButton.setImageDrawable(new VideoBadge(context));
                }

                holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        VideoViewerActivity.startActivity(context, id, holder.gifUrl, otherUrl);
                    }
                });

                if (holder.gifUrl.contains(".mp4") || holder.gifUrl.contains(".m3u8")) {
                    videos.add(new Video(holder.videoView, holder.tweetId, holder.gifUrl));
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
                    }
                });

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

        mHandlers[currHandler].removeCallbacksAndMessages(null);
        mHandlers[currHandler].postDelayed(new Runnable() {
            @Override
            public void run() {
                if (holder.tweetId == id) {
                    TextUtils.linkifyText(context, holder.tweet, holder.background, true, otherUrl, false);
                    TextUtils.linkifyText(context, holder.retweeter, holder.background, true, "", false);

                    if (otherUrl != null && otherUrl.contains("/status/") &&
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

        if (otherUrl != null && otherUrl.contains("/status/")) {
            holder.embeddedTweet.setVisibility(View.VISIBLE);
            tryImmediateEmbeddedLoad(holder, otherUrl);
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

    public void removeExpansion(final ViewHolder holder, boolean anim) {

        if (holder.expandHelper != null) {
            holder.expandHelper.stop();
        }

        final int sixteen = Utils.toDP(16, context);
        final int eight = Utils.toDP(8, context);
        ValueAnimator paddingTopAnimator = ValueAnimator.ofInt(holder.background.getPaddingTop(), 0);
        paddingTopAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                holder.background.setPadding(0, val + (settings.picturesType == AppSettings.CONDENSED_TWEETS ? eight : 0), 0, sixteen);
            }
        });
        paddingTopAnimator.setDuration(ANIMATION_DURATION / 2);
        paddingTopAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(paddingTopAnimator);

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

            // if they are just scrolling away from it, there is no reason to put them back at
            // their original position, so we don't call the expander method.
            expander.expandViewClosed((int) holder.rootView.getY());
        } else {
            holder.expandArea.setVisibility(View.GONE);
            expander.expandViewClosed(-1);
            hasExpandedTweet = false;
        }

    }

    protected void startAnimation(Animator animator) {
        animator.start();
    }

    public static final int ANIMATION_DURATION = 300;
    public static Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

    public void addExpansion(final ViewHolder holder, int position, final String screenname, String users, final String[] otherLinks, final String webpage, final long tweetId, String[] hashtags) {

        hasExpandedTweet = true;

        String str = holder.tweet.getText().toString();
        try {
            if (!str.contains(webpage.substring(0, 18))) {
                str = str + " " + webpage;
            }
        } catch (Exception e) {

        }

        final String text = str;
        String extraNames = "";
        String replyStuff = "";

        String screenNameToUse;

        if (secondAcc) {
            screenNameToUse = settings.secondScreenName;
        } else {
            screenNameToUse = settings.myScreenName;
        }

        if (text.contains("@")) {
            for (String s : users.split("  ")) {
                if (!s.equals(screenNameToUse) && !extraNames.contains(s) && !s.equals(screenname)) {
                    extraNames += "@" + s + " ";
                }
            }
        }

        try {
            if (holder.retweeter.getVisibility() == View.VISIBLE && !extraNames.contains(holder.retweeterName)) {
                extraNames += "@" + holder.retweeterName + " ";
            }
        } catch (NullPointerException e) {

        }

        if (!screenname.equals(screenNameToUse)) {
            replyStuff = "@" + screenname + " " + extraNames;
        } else {
            replyStuff = extraNames;
        }

        if (settings.autoInsertHashtags && hashtags != null) {
            for (String s : hashtags) {
                if (!s.equals("")) {
                    replyStuff += "#" + s + " ";
                }
            }
        }

        int headerPadding = (int)context.getResources().getDimension(R.dimen.header_holder_padding);

        final ExpansionViewHelper helper = new ExpansionViewHelper(context, tweetId);
        helper.setSecondAcc(secondAcc);
        helper.setBackground(holder.background);
        helper.setWebLink(otherLinks);
        helper.setReplyDetails("@" + screenname + ": " + text, replyStuff);
        helper.setUser(screenname);
        helper.setText(text);
        helper.setUpOverflow();
        helper.showEmbedded(false);
        holder.expandHelper = helper;

        if (secondAcc) {
            String t = context.getString(R.string.using_second_account).replace("%s", "@" + settings.secondScreenName);
            Toast.makeText(context, t, Toast.LENGTH_SHORT).show();
        }

        expander.expandViewOpen((int) holder.rootView.getY() + headerPadding * headerMultiplier, position, holder.background, helper);

        int topPadding = Utils.getStatusBarHeight(context);
        if (settings.staticUi || context.getResources().getBoolean(R.bool.duel_panel)) {
            // the app bar doesn't move, so we should use it for the top padding too
            topPadding += Utils.getActionBarHeight(context);
        }

        final int sixteen = Utils.toDP(16, context);
        final int eight = Utils.toDP(8, context);
        ValueAnimator paddingTopAnimator = ValueAnimator.ofInt(0, topPadding);
        paddingTopAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                holder.background.setPadding(0, val + (settings.picturesType == AppSettings.CONDENSED_TWEETS ? eight : 0), 0, sixteen);
            }
        });
        paddingTopAnimator.setDuration(ANIMATION_DURATION / 2);
        paddingTopAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(paddingTopAnimator);

        final int expansionSize = contentHeight - holder.background.getHeight();

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
            @Override
            public void onAnimationStart(Animator animation) {
                holder.expandArea.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                if (holder.expandArea.getChildCount() > 0) {
                    holder.expandArea.removeAllViews();
                }

                holder.expandArea.setMinimumHeight(expansionSize);
                holder.expandArea.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.expandArea.invalidate();

                View root = helper.getExpansion();

                helper.startFlowAnimation();

                holder.expandArea.addView(root);

            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
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

    private class Video {
        public String url;
        public long tweetId;
        public SimpleVideoView videoView;

        public Video(SimpleVideoView videoView, long tweetId, String url) {
            this.videoView = videoView;
            this.tweetId = tweetId;
            this.url = url;
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
}
