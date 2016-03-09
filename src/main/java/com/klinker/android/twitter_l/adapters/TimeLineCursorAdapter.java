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
    public Map<Long, Status> quotedTweets = new HashMap<>();

    public Set<String> muffledUsers = new HashSet<String>();
    public Cursor cursor;
    public AppSettings settings;
    public Context context;
    public final LayoutInflater inflater;
    private boolean isDM = false;
    protected SharedPreferences sharedPrefs;
    private boolean secondAcc = false;

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

    private int smallPictures;

    public static MultiplePicsPopup multPics;
    public boolean hasConvo = false;

    public boolean hasExpandedTweet = false;

    private Handler videoHandler;

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
        public View noMediaPreviewText;
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
        videoHandler = new Handler();

        settings = AppSettings.getInstance(context);

        smallPictures = Utils.toDP(120, context);

        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        layout = R.layout.tweet;

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

        if (!settings.bottomPictures) {
            holder.image = (ImageView) v.findViewById(R.id.image);
            holder.playButton = (ImageView) v.findViewById(R.id.play_button);
            holder.noMediaPreviewText = v.findViewById(R.id.no_media_preview);
            holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);
            holder.videoView = (SimpleVideoView) v.findViewById(R.id.video_view);
        } else {
            holder.image = (ImageView) v.findViewById(R.id.image_bellow);
            holder.playButton = (ImageView) v.findViewById(R.id.play_button_bellow);
            holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder_bellow);
            holder.noMediaPreviewText = v.findViewById(R.id.no_media_preview_below);
            holder.videoView = (SimpleVideoView) v.findViewById(R.id.video_view_below);
        }

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.screenTV.setTextSize(settings.textSize - 2);
        holder.name.setTextSize(settings.textSize + 4);
        holder.muffledName.setTextSize(settings.textSize);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);

        //surfaceView.profilePic.setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.image.setClipToOutline(true);
        }

        holder.rootView = v;

        v.setTag(holder);

        return v;
    }

    private List<Video> videos = new ArrayList<>();

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.image.setTransitionName(null);
            holder.name.setTransitionName(null);
            holder.screenTV.setTransitionName(null);
            holder.profilePic.setTransitionName(null);
            holder.tweet.setTransitionName(null);
        }

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
        if (!isDM && (muffledUsers.contains(screenname) ||
                (retweeter != null && !android.text.TextUtils.isEmpty(retweeter) && muffledUsers.contains(retweeter)))) {
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

        holder.quickActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QuickActionsPopup popup = new QuickActionsPopup(context, holder.tweetId, screenname, tweetTexts, secondAcc);
                popup.setExpansionPointForAnim(holder.quickActions);
                popup.setOnTopOfView(holder.quickActions);
                popup.show();
            }
        });

        if((settings.reverseClickActions || expander == null || MainActivity.isPopup || settings.bottomPictures || muffled) && !isDM) {
            final String fRetweeter = retweeter;
            holder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.preventNextClick) {
                        holder.preventNextClick = false;
                        return;
                    }

                    String link;

                    boolean hasGif = holder.gifUrl != null && !holder.gifUrl.isEmpty();
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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !muffled) {
                        holder.profilePic.setTransitionName("pro_pic");
                        holder.screenTV.setTransitionName("screen_name");
                        holder.name.setTransitionName("name");
                        holder.tweet.setTransitionName("tweet");

                        if (holder.imageHolder.getVisibility() == View.VISIBLE &&
                                holder.playButton.getVisibility() != View.VISIBLE) {
                            ActivityOptions options = ActivityOptions
                                    .makeSceneTransitionAnimation(((Activity) context),

                                            new Pair<View, String>(holder.profilePic, "pro_pic"),
                                            new Pair<View, String>(holder.screenTV, "screen_name"),
                                            new Pair<View, String>(holder.name, "name"),
                                            new Pair<View, String>(holder.tweet, "tweet"),
                                            new Pair<View, String>(holder.image, "image")
                                    );

                            context.startActivity(viewTweet/*, options.toBundle()*/);
                        } else {
                            ActivityOptions options = ActivityOptions
                                    .makeSceneTransitionAnimation(((Activity) context),

                                            new Pair<View, String>(holder.profilePic, "pro_pic"),
                                            new Pair<View, String>(holder.screenTV, "screen_name"),
                                            new Pair<View, String>(holder.name, "name"),
                                            new Pair<View, String>(holder.tweet, "tweet")
                                    );

                            context.startActivity(viewTweet/*, options.toBundle()*/);
                        }
                    } else {
                        context.startActivity(viewTweet);
                    }
                }
            });

            if (expander != null && ! MainActivity.isPopup) {
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

        } else if (!isDM) {
            final String fRetweeter = retweeter;
            holder.background.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    String link;

                    boolean hasGif = holder.gifUrl != null && !holder.gifUrl.isEmpty();
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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !muffled) {
                        holder.profilePic.setTransitionName("pro_pic");
                        holder.screenTV.setTransitionName("screen_name");
                        holder.name.setTransitionName("name");
                        holder.tweet.setTransitionName("tweet");

                        if (holder.imageHolder.getVisibility() == View.VISIBLE &&
                                holder.playButton.getVisibility() != View.VISIBLE) {
                            ActivityOptions options = ActivityOptions
                                    .makeSceneTransitionAnimation(((Activity) context),

                                            new Pair<View, String>(holder.profilePic, "pro_pic"),
                                            new Pair<View, String>(holder.screenTV, "screen_name"),
                                            new Pair<View, String>(holder.name, "name"),
                                            new Pair<View, String>(holder.tweet, "tweet"),
                                            new Pair<View, String>(holder.image, "image")
                                    );

                            context.startActivity(viewTweet/*, options.toBundle()*/);
                        } else {
                            ActivityOptions options = ActivityOptions
                                    .makeSceneTransitionAnimation(((Activity) context),

                                            new Pair<View, String>(holder.profilePic, "pro_pic"),
                                            new Pair<View, String>(holder.screenTV, "screen_name"),
                                            new Pair<View, String>(holder.name, "name"),
                                            new Pair<View, String>(holder.tweet, "tweet")
                                    );

                            context.startActivity(viewTweet/*, options.toBundle()*/);
                        }
                    } else {
                        context.startActivity(viewTweet);
                    }

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

        if (isDM) {
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

            if (otherUrl != null && !otherUrl.equals("")) {
                holder.tweet.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browser = new Intent(context, BrowserActivity.class);
                        browser.putExtra("url", otherUrl);

                        context.startActivity(browser);
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
                            .commit();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    holder.profilePic.getVisibility() == View.VISIBLE) {

                    /*surfaceView.profilePic.setTransitionName("pro_pic");
                    ActivityOptions options = ActivityOptions
                            .makeSceneTransitionAnimation(((Activity) context),
                                    new Pair<View, String>(surfaceView.profilePic, "pro_pic")
                            );

                    context.startActivity(viewProfile, options.toBundle());*/
                    context.startActivity(viewProfile);
                } else {
                    context.startActivity(viewProfile);
                }

            }
        });


        if (holder.screenTV.getVisibility() == View.GONE) {
            holder.screenTV.setVisibility(View.VISIBLE);
        }
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

        boolean replace = false;
        boolean embeddedTweetFound = TweetView.embeddedTweetPattern.matcher(tweetText).find();
        if (settings.inlinePics && (tweetText.contains("pic.twitter.com/") || embeddedTweetFound)) {
            if (tweetText.lastIndexOf(".") == tweetText.length() - 1) {
                replace = true;
            }
        }

        try {
            holder.tweet.setText(replace ?
                    tweetText.substring(0, tweetText.length() - (embeddedTweetFound ? 33 : 25)) :
                    tweetText);
        } catch (Exception e) {
            holder.tweet.setText(tweetText);
        }

        boolean picture = false;

        if (holder.videoView.getVisibility() == View.VISIBLE) {
            holder.videoView.setVisibility(View.GONE);
        }

        boolean playVideo = false;
        boolean containsThirdPartyVideo = VideoMatcherUtil.containsThirdPartyVideo(tweetTexts);
        if((settings.inlinePics || isDM) && (holder.picUrl != null || containsThirdPartyVideo)) {
            if (holder.picUrl != null && holder.picUrl.equals("") && !containsThirdPartyVideo) {
                if (holder.imageHolder.getVisibility() != View.GONE) {
                    holder.imageHolder.setVisibility(View.GONE);
                }

                if (holder.playButton.getVisibility() == View.VISIBLE) {
                    holder.playButton.setVisibility(View.GONE);
                }

                if (holder.noMediaPreviewText.getVisibility() == View.VISIBLE) {
                    holder.noMediaPreviewText.setVisibility(View.GONE);
                }
            } else {
                if (holder.imageHolder.getVisibility() == View.GONE) {
                    holder.imageHolder.setVisibility(View.VISIBLE);
                }

                if (settings.picturesType == AppSettings.PICTURES_SMALL &&
                        holder.imageHolder.getHeight() != smallPictures) {
                    ViewGroup.LayoutParams params = holder.imageHolder.getLayoutParams();
                    params.height = smallPictures;
                    holder.imageHolder.setLayoutParams(params);
                }

                if (!isDM && (holder.picUrl.contains("youtube") || (holder.gifUrl != null && !android.text.TextUtils.isEmpty(holder.gifUrl)))) {
                    if (holder.playButton.getVisibility() == View.GONE) {
                        holder.playButton.setVisibility(View.VISIBLE);
                    }

                    if (holder.noMediaPreviewText.getVisibility() == View.VISIBLE) {
                        holder.noMediaPreviewText.setVisibility(View.GONE);
                    }

                    if (VideoMatcherUtil.isTwitterGifLink(holder.gifUrl))
                        holder.playButton.setImageDrawable(new GifBadge(context));
                    else
                        holder.playButton.setImageDrawable(new VideoBadge(context));

                    holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            VideoViewerActivity.startActivity(context, id, holder.gifUrl, otherUrl);
                        }
                    });

                    if (holder.gifUrl.contains(".mp4")) {
                        videos.add(new Video(holder.videoView, holder.tweetId, holder.gifUrl));
                    }

                    holder.image.setImageDrawable(null);

                    picture = true;
                } else if (containsThirdPartyVideo) {
                    if (holder.playButton.getVisibility() == View.GONE) {
                        holder.playButton.setVisibility(View.VISIBLE);
                    }

                    if (holder.noMediaPreviewText.getVisibility() == View.GONE) {
                        holder.noMediaPreviewText.setVisibility(View.VISIBLE);
                    }

                    String vid = null;
                    for (String s : otherUrl.split("  ")) {
                        if (VideoMatcherUtil.containsThirdPartyVideo(s))
                            vid = s;
                    }

                    final String fVid = vid;

                    if (VideoMatcherUtil.isTwitterGifLink(vid))
                        holder.playButton.setImageDrawable(new GifBadge(context));
                    else
                        holder.playButton.setImageDrawable(new VideoBadge(context));

                    holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (fVid != null)
                                VideoViewerActivity.startActivity(context, id, fVid, otherUrl);
                        }
                    });

                    holder.image.setImageDrawable(new ColorDrawable(Color.BLACK));

                    picture = false;
                } else {
                    if (holder.playButton.getVisibility() == View.VISIBLE) {
                        holder.playButton.setVisibility(View.GONE);
                    }

                    if (holder.noMediaPreviewText.getVisibility() == View.VISIBLE) {
                        holder.noMediaPreviewText.setVisibility(View.GONE);
                    }

                    holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            if (isHomeTimeline) {
                                sharedPrefs.edit()
                                        .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                        .commit();
                            }

                            if (holder.picUrl.contains(" ") && !MainActivity.isPopup) {
                                /*multPics = new MultiplePicsPopup(context, surfaceView.picUrl);
                                multPics.setFullScreen();
                                multPics.setExpansionPointForAnim(view);
                                multPics.show();*/

                                PhotoPagerActivity.startActivity(context, id, holder.picUrl, 0);

                            } else {
                                PhotoViewerActivity.startActivity(context, id, holder.picUrl, holder.image);
                            }
                        }
                    });

                    holder.image.setImageDrawable(null);

                    picture = true;
                }
            }
        }


        if (retweeter.length() > 0 && !isDM) {
            String text = context.getResources().getString(R.string.retweeter);
            //surfaceView.retweeter.setText(settings.displayScreenName ? text + retweeter : text.substring(0, text.length() - 2) + " " + name);
            holder.retweeter.setText(text + retweeter);
            holder.retweeterName = retweeter;
            holder.retweeter.setVisibility(View.VISIBLE);
        } else if (holder.retweeter.getVisibility() == View.VISIBLE) {
            holder.retweeter.setVisibility(View.GONE);
        }

        if (picture) {
            //if (settings.preCacheImages){
                //Glide.with(context).load(holder.picUrl).diskCacheStrategy(DiskCacheStrategy.SOURCE).into(holder.image);
            //} else {
                Glide.with(context).load(holder.picUrl).into(holder.image);
            //}
        }

        //if (settings.preCacheImages) {
            //Glide.with(context).load(holder.proPicUrl).diskCacheStrategy(DiskCacheStrategy.SOURCE).into(holder.profilePic);
        //} else {
            Glide.with(context).load(holder.proPicUrl).into(holder.profilePic);
        //}

        mHandlers[currHandler].removeCallbacksAndMessages(null);
        mHandlers[currHandler].postDelayed(new Runnable() {
            @Override
            public void run() {
                if (holder.tweetId == id) {

                    if (settings.useEmoji && (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || EmojiUtils.ios)) {
                        String text = tweetText;
                        if (EmojiUtils.emojiPattern.matcher(text).find()) {
                            final Spannable span = EmojiUtils.getSmiledText(context, Html.fromHtml(tweetText));
                            holder.tweet.setText(span);
                        }
                    }

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

                    if (holder.retweeter.getVisibility() == View.VISIBLE) {
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
                    }

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

    private void tryImmediateEmbeddedLoad(final ViewHolder holder, String otherUrl) {
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

            final ViewHolder holder = (ViewHolder) v.getTag();

            holder.profilePic.setImageDrawable(transparent);
            if (holder.imageHolder.getVisibility() == View.VISIBLE) {
                holder.imageHolder.setVisibility(View.GONE);
            }
        }

        bindView(v, context, cursor);

        return v;
    }

    public void removeExpansion(final ViewHolder holder, boolean anim) {

        if (holder.expandHelper != null) {
            holder.expandHelper.stop();
        }

        ObjectAnimator translationXAnimator = ObjectAnimator.ofFloat(holder.imageHolder, View.TRANSLATION_X, holder.imageHolder.getTranslationX(), 0f);
        translationXAnimator.setDuration(anim ? ANIMATION_DURATION : 0);
        translationXAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(translationXAnimator);

        ObjectAnimator translationYAnimator = ObjectAnimator.ofFloat(holder.background, View.TRANSLATION_Y, holder.background.getTranslationY(), 0f);
        translationYAnimator.setDuration(anim ? ANIMATION_DURATION : 0);
        translationYAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(translationYAnimator);

        int padding = (int) context.getResources().getDimension(R.dimen.header_side_padding);
        ValueAnimator widthAnimator = ValueAnimator.ofInt(holder.imageHolder.getWidth(), holder.rootView.getWidth() - (2 * padding));
        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = holder.imageHolder.getLayoutParams();
                layoutParams.width = val;
                holder.imageHolder.setLayoutParams(layoutParams);
            }
        });
        widthAnimator.setDuration(anim ? ANIMATION_DURATION : 0);
        widthAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(widthAnimator);

        int condensedHeight = (int) context.getResources().getDimension(R.dimen.header_condensed_height);
        ValueAnimator heightAnimatorHeader = ValueAnimator.ofInt(holder.imageHolder.getHeight(), condensedHeight);
        heightAnimatorHeader.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = holder.imageHolder.getLayoutParams();
                layoutParams.height = val;
                holder.imageHolder.setLayoutParams(layoutParams);
            }
        });
        heightAnimatorHeader.setDuration(anim ? ANIMATION_DURATION : 0);
        heightAnimatorHeader.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(heightAnimatorHeader);

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

        ObjectAnimator translationXAnimator = ObjectAnimator.ofFloat(holder.imageHolder, View.TRANSLATION_X, 0f, -1 * (holder.imageHolder.getX() + headerPadding * 2));
        translationXAnimator.setDuration(ANIMATION_DURATION);
        translationXAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(translationXAnimator);

        if (!settings.bottomPictures) {
            if (holder.imageHolder.getVisibility() == View.VISIBLE) {
                int topPadding = (int) context.getResources().getDimension(R.dimen.header_top_padding);
                ObjectAnimator translationYAnimator = ObjectAnimator.ofFloat(holder.background, View.TRANSLATION_Y, 0f, -1 * topPadding - 5);
                translationYAnimator.setDuration(ANIMATION_DURATION);
                translationYAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
                startAnimation(translationYAnimator);

                ObjectAnimator translationYAnimatorExpansion = ObjectAnimator.ofFloat(holder.expandArea, View.TRANSLATION_Y, 0f, -1 * topPadding - 5);
                translationYAnimatorExpansion.setDuration(ANIMATION_DURATION);
                translationYAnimatorExpansion.setInterpolator(ANIMATION_INTERPOLATOR);
                startAnimation(translationYAnimatorExpansion);
            } else {
                int topPadding = (int) context.getResources().getDimension(R.dimen.header_top_padding);
                ObjectAnimator translationYAnimator = ObjectAnimator.ofFloat(holder.background, View.TRANSLATION_Y, 0f, topPadding + 10);
                translationYAnimator.setDuration(ANIMATION_DURATION);
                translationYAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
                startAnimation(translationYAnimator);
            }
        }

        ValueAnimator widthAnimator = ValueAnimator.ofInt(holder.imageHolder.getWidth(), holder.rootView.getWidth() + headerPadding * 4);
        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = holder.imageHolder.getLayoutParams();
                layoutParams.width = val;
                holder.imageHolder.setLayoutParams(layoutParams);
            }
        });
        widthAnimator.setDuration(ANIMATION_DURATION);
        widthAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(widthAnimator);

        final int headerHeight = (int) context.getResources().getDimension(R.dimen.header_expanded_height);//(int) (contentHeight * .5);
        ValueAnimator heightAnimatorHeader = ValueAnimator.ofInt(holder.imageHolder.getHeight(), headerHeight);
        heightAnimatorHeader.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = holder.imageHolder.getLayoutParams();
                layoutParams.height = val;
                holder.imageHolder.setLayoutParams(layoutParams);
            }
        });
        heightAnimatorHeader.setDuration(ANIMATION_DURATION);
        heightAnimatorHeader.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(heightAnimatorHeader);

        int d;
        if (holder.imageHolder.getVisibility() == View.VISIBLE) {
            d = contentHeight - headerHeight - headerPadding - holder.tweet.getHeight() - holder.profilePic.getHeight();
        } else {
            d = contentHeight - headerPadding - holder.tweet.getHeight() - holder.profilePic.getHeight();
        }

        if (holder.embeddedTweet.getVisibility() == View.VISIBLE) {
            d -= holder.embeddedTweet.getHeight();
        }

        final int distance = d;

        ValueAnimator heightAnimatorContent = ValueAnimator.ofInt(0, d);
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

                holder.expandArea.setMinimumHeight(distance);
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
            if (!activityPaused && videoView != null) {
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
