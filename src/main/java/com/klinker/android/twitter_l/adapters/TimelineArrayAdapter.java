package com.klinker.android.twitter_l.adapters;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.*;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.manipulations.MultiplePicsPopup;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.ui.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoViewerActivity;
import com.klinker.android.twitter_l.utils.*;
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
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;

import twitter4j.Status;
import twitter4j.User;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class TimelineArrayAdapter extends ArrayAdapter<Status> {

    public boolean openFirst = false;

    public static final int NORMAL = 0;
    public static final int RETWEET = 1;
    public static final int FAVORITE = 2;

    public Context context;
    public ArrayList<Status> statuses;
    public LayoutInflater inflater;
    public AppSettings settings;
    public int border;

    public static final String REGEX = "(http|ftp|https):\\/\\/([\\w\\-_]+(?:(?:\\.[\\w\\-_]+)+))([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?";
    public static Pattern pattern = Pattern.compile(REGEX);

    public boolean hasKeyboard = false;
    public boolean isProfile = false;

    public int layout;
    public XmlResourceParser addonLayout = null;
    public Resources res;
    public BitmapLruCache mCache;

    public ColorDrawable transparent;

    public Handler[] mHandler;
    public Handler emojiHandler;
    public int currHandler = 0;

    public int type;
    public String username;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    public int contentHeight = 0;
    public int headerMultiplier = 0;
    public Expandable expander;

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
        public TextView screenTV;
        public FrameLayout imageHolder;
        public View rootView;

        public long tweetId;
        public boolean isFavorited;
        public String screenName;
        public String picUrl;
        public String retweeterName;

        public boolean preventNextClick = false;
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, boolean openFirst, Expandable expander) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;

        this.username = "";
        this.openFirst = openFirst;

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        this.expander = expander;

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, Expandable expander) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;

        this.username = "";

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        this.expander = expander;

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, int type, Expandable expander) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = type;
        this.username = "";

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        this.expander = expander;

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, String username, Expandable expander) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;
        this.username = username;

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        this.expander = expander;

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

        setUpLayout();
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

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        this.expander = expander;

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = AppSettings.getInstance(context);

        this.type = NORMAL;

        this.username = "";

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        this.expander = expander;

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

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

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        this.expander = expander;

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

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

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }

        this.expander = expander;

        if (context.getResources().getBoolean(R.bool.isTablet) ||
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            headerMultiplier = -25;
        }

        setUpLayout();
    }

    public void setUpLayout() {
        mHandler = new Handler[4];

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        layout = R.layout.tweet;

        TypedArray b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        border = b.getResourceId(0, 0);
        b.recycle();

        mCache = App.getInstance(context).getBitmapCache();

        transparent = new ColorDrawable(android.R.color.transparent);

        mHandler = new Handler[10];
        for (int i = 0; i < 10; i++) {
            mHandler[i] = new Handler();
        }
        currHandler = 0;
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

        if (!settings.bottomPictures) {
            holder.playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button);
            holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);
            holder.image = (NetworkedCacheableImageView) v.findViewById(R.id.image);
        } else {
            holder.playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button_bellow);
            holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder_bellow);
            holder.image = (NetworkedCacheableImageView) v.findViewById(R.id.image_bellow);
        }

        holder.profilePic.setClipToOutline(true);
        holder.image.setClipToOutline(true);

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.name.setTextSize(settings.textSize + 4);
        holder.screenTV.setTextSize(settings.textSize - 2);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);

        holder.rootView = v;

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Status status, final int position) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (holder.expandArea.getVisibility() == View.VISIBLE) {
            removeExpansion(holder, false);
        }

        Status thisStatus;

        String retweeter;
        final long time = status.getCreatedAt().getTime();
        long originalTime = 0;

        if (status.isRetweet()) {
            retweeter = status.getUser().getScreenName();

            thisStatus = status.getRetweetedStatus();
            originalTime = thisStatus.getCreatedAt().getTime();
        } else {
            retweeter = "";

            thisStatus = status;
        }

        final long fOriginalTime = originalTime;

        User user = thisStatus.getUser();

        holder.tweetId = thisStatus.getId();
        final long id = holder.tweetId;
        final String profilePic = user.getOriginalProfileImageURL();
        String tweetTexts = thisStatus.getText();
        final String name = user.getName();
        final String screenname = user.getScreenName();

        String[] html = TweetLinkUtils.getLinksInStatus(thisStatus);
        final String tweetText = html[0];
        final String picUrl = html[1];
        holder.picUrl = picUrl;
        final String otherUrl = html[2];
        final String hashtags = html[3];
        final String users = html[4];

        if(settings.reverseClickActions || expander == null) {
            final String fRetweeter = retweeter;
            holder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.preventNextClick) {
                        holder.preventNextClick = false;
                        return;
                    }

                    String link;

                    boolean displayPic = !holder.picUrl.equals("") && !holder.picUrl.contains("youtube");
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

                    context.startActivity(viewTweet);
                }
            });

            if (expander != null) {
                holder.background.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if (holder.expandArea.getVisibility() == View.GONE) {
                            addExpansion(holder, position, screenname, users, otherUrl.split("  "), holder.picUrl, id, hashtags.split("  "));
                        } else {
                            removeExpansion(holder, true);
                        }

                        return true;
                    }
                });
            }

        } else {
            final String fRetweeter = retweeter;
            holder.background.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    String link;

                    boolean displayPic = !holder.picUrl.equals("") && !holder.picUrl.contains("youtube");
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
                            addExpansion(holder, position, screenname, users, otherUrl.split("  "), holder.picUrl, id, hashtags.split("  "));
                        } else {
                            removeExpansion(holder, true);
                        }
                    }
                });
            }
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

        if (holder.screenTV.getVisibility() == View.GONE) {
            holder.screenTV.setVisibility(View.VISIBLE);
        }

        holder.screenTV.setText("@" + screenname);
        holder.name.setText(name);


        if (!settings.absoluteDate) {
            holder.time.setText(Utils.getTimeAgo(time, context));
        } else {
            Date date = new Date(time);
            holder.time.setText(timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date));
        }

        holder.tweet.setText(tweetText);

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
                if (holder.picUrl.contains("youtube")) {

                    if (holder.playButton.getVisibility() == View.GONE) {
                        holder.playButton.setVisibility(View.VISIBLE);
                    }

                    final String fRetweeter = retweeter;

                    holder.image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String link;

                            boolean displayPic = !holder.picUrl.equals("") && !holder.picUrl.contains("youtube");
                            if (displayPic) {
                                link = holder.picUrl;
                            } else {
                                link = otherUrl.split("  ")[0];
                            }

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
                            viewTweet.putExtra("clicked_youtube", true);

                            context.startActivity(viewTweet);
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
                                MultiplePicsPopup popup = new MultiplePicsPopup(context, context.getResources().getBoolean(R.bool.isTablet), holder.picUrl);
                                popup.setFullScreen();
                                popup.setExpansionPointForAnim(view);
                                popup.show();
                            } else {
                                Intent photo = new Intent(context, PhotoViewerActivity.class).putExtra("url", holder.picUrl);
                                photo.putExtra("shared_trans", true);

                                ActivityOptions options = ActivityOptions
                                        .makeSceneTransitionAnimation(((Activity)context), holder.image, "image");

                                context.startActivity(photo, options.toBundle());
                            }
                        }
                    });
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

        if (picture) {
            CacheableBitmapDrawable wrapper = mCache.getFromMemoryCache(holder.picUrl);
            if (wrapper != null) {
                holder.image.setImageDrawable(wrapper);
                picture = false;
            }
        }

        final boolean hasPicture = picture;
        mHandler[currHandler].removeCallbacksAndMessages(null);
        /*mHandler[currHandler].postDelayed(new Runnable() {
            @Override
            public void run() {*/
                //if (holder.tweetId == id) {
                    if (hasPicture) {
                        loadImage(context, holder, holder.picUrl, mCache, id);
                    }

                    if (settings.useEmoji && (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || EmojiUtils.ios)) {
                        String text = holder.tweet.getText().toString();
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
                //}
            /*}
        }, 400);*/
        currHandler++;

        if (currHandler == 10) {
            currHandler = 0;
        }

        if (openFirst && position == 0) {
            holder.background.performClick();
            ((Activity)context).finish();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();

            holder.profilePic.setImageDrawable(context.getResources().getDrawable(border));
            if (holder.imageHolder.getVisibility() == View.VISIBLE) {
                holder.imageHolder.setVisibility(View.GONE);
            }
        }

        bindView(v, statuses.get(position), position);

        return v;
    }

    public void removeExpansion(final ViewHolder holder, boolean anim) {

        ObjectAnimator translationXAnimator = ObjectAnimator.ofFloat(holder.imageHolder, View.TRANSLATION_X, holder.imageHolder.getTranslationX(), 0f);
        translationXAnimator.setDuration(anim ? ANIMATION_DURATION : 0);
        translationXAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(translationXAnimator);

        ObjectAnimator translationYAnimator = ObjectAnimator.ofFloat(holder.background, View.TRANSLATION_Y, holder.background.getTranslationY(), 0f);
        translationYAnimator.setDuration(anim ? ANIMATION_DURATION : 0);
        translationYAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(translationYAnimator);

        int padding = (int) context.getResources().getDimension(R.dimen.header_side_padding);
        ValueAnimator widthAnimator = ValueAnimator.ofInt(holder.imageHolder.getWidth(), holder.rootView.getWidth() - (4 * padding));
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
                }
            }, ANIMATION_DURATION);
        } else {
            holder.expandArea.setVisibility(View.GONE);
        }

        if (anim) {
            // if they are just scrolling away from it, there is no reason to put them back at
            // their original position, so we don't call the expander method.
            expander.expandViewClosed((int) holder.rootView.getY());
        } else {
            expander.expandViewClosed(-1);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                holder.expandArea.removeAllViews();
            }
        }, ANIMATION_DURATION);

    }

    protected void startAnimation(Animator animator) {
        animator.start();
    }

    public static final int ANIMATION_DURATION = 200;
    public static final Interpolator ANIMATION_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    public void addExpansion(final ViewHolder holder, int position, final String screenname, String users, final String[] otherLinks, final String webpage, final long tweetId, String[] hashtags) {
        final String text = holder.tweet.getText().toString();
        String extraNames = "";
        String replyStuff = "";

        if (text.contains("@")) {
            for (String s : users.split("  ")) {
                if (!s.equals(settings.myScreenName) && !extraNames.contains(s) && !s.equals(screenname)) {
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

        if (!screenname.equals(settings.myScreenName)) {
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
        ObjectAnimator translationXAnimator = ObjectAnimator.ofFloat(holder.imageHolder, View.TRANSLATION_X, 0f, -1 * (holder.imageHolder.getX() + headerPadding * 2));
        translationXAnimator.setDuration(ANIMATION_DURATION);
        translationXAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
        startAnimation(translationXAnimator);

        if (holder.imageHolder.getVisibility() == View.VISIBLE) {
            int topPadding = (int) context.getResources().getDimension(R.dimen.header_top_padding);
            ObjectAnimator translationYAnimator = ObjectAnimator.ofFloat(holder.background, View.TRANSLATION_Y, 0f, -1 * topPadding - 5);
            translationYAnimator.setDuration(ANIMATION_DURATION);
            translationYAnimator.setInterpolator(ANIMATION_INTERPOLATOR);
            startAnimation(translationYAnimator);
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

        final int headerHeight = (int) (contentHeight * .5);
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

        final int distance;// = (int) context.getResources().getDimension(R.dimen.expansion_size);;
        if (holder.imageHolder.getVisibility() == View.VISIBLE) {
            distance = contentHeight - headerHeight;
        } else {
            distance = contentHeight;
        }
        ValueAnimator heightAnimatorContent = ValueAnimator.ofInt(0, distance);
        heightAnimatorContent.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = holder.expandArea.getLayoutParams();
                layoutParams.height = val;
                holder.expandArea.setLayoutParams(layoutParams);
            }
        });
        final String s = replyStuff;
        heightAnimatorContent.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                holder.expandArea.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                holder.expandArea.setMinimumHeight(distance);
                holder.expandArea.getLayoutParams().height = distance;
                holder.expandArea.invalidate();

                ExpansionViewHelper helper = new ExpansionViewHelper(context, holder.tweetId);
                helper.setBackground(holder.background);
                helper.setWebLink(otherLinks);
                helper.setReplyDetails("@" + screenname + ": " + text, s);
                View root = helper.getExpansion();

                ObjectAnimator alpha = ObjectAnimator.ofFloat(root, View.ALPHA, 0f, 1f);
                alpha.setDuration(ANIMATION_DURATION);
                alpha.setInterpolator(ANIMATION_INTERPOLATOR);
                startAnimation(alpha);

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

        expander.expandViewOpen((int) holder.rootView.getY() + headerPadding * headerMultiplier, position, holder.background, null);

    }

    // used to place images on the timeline
    public static ImageUrlAsyncTask mCurrentTask;

    public void loadImage(Context context, final ViewHolder holder, final String url, BitmapLruCache mCache, final long tweetId) {
        // First check whether there's already a task running, if so cancel it
        /*if (null != mCurrentTask) {
            mCurrentTask.cancel(true);
        }*/

        if (url == null) {
            return;
        }

        BitmapDrawable wrapper = mCache.getFromMemoryCache(url);

        if (null != wrapper && holder.image.getVisibility() != View.GONE) {
            // The cache has it, so just display it
            holder.image.setImageDrawable(wrapper);Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);

            holder.image.startAnimation(fadeInAnimation);
        } else {
            // Memory Cache doesn't have the URL, do threaded request...
            holder.image.setImageDrawable(null);

            mCurrentTask = new ImageUrlAsyncTask(context, holder, mCache, tweetId);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    SDK11.executeOnThreadPool(mCurrentTask, url);
                } else {
                    mCurrentTask.execute(url);
                }
            } catch (RejectedExecutionException e) {
                // This shouldn't happen, but might.
            }

        }
    }

    private static class ImageUrlAsyncTask
            extends AsyncTask<String, Void, CacheableBitmapDrawable> {

        private BitmapLruCache mCache;
        private Context context;
        private ViewHolder holder;
        private long id;

        ImageUrlAsyncTask(Context context, ViewHolder holder, BitmapLruCache cache, long tweetId) {
            this.context = context;
            mCache = cache;
            this.holder = holder;
            this.id = tweetId;
        }

        @Override
        protected CacheableBitmapDrawable doInBackground(String... params) {
            try {
                if (holder.tweetId != id) {
                    return null;
                }
                String url = params[0];

                if (url.contains("twitpic")) {
                    try {
                        URL address = new URL(url);
                        HttpURLConnection connection = (HttpURLConnection) address.openConnection(Proxy.NO_PROXY);
                        connection.setConnectTimeout(1000);
                        connection.setInstanceFollowRedirects(false);
                        connection.setReadTimeout(1000);
                        connection.connect();
                        String expandedURL = connection.getHeaderField("Location");
                        if(expandedURL != null) {
                            url = expandedURL;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (url.contains("p.twipple.jp")) {

                }

                // Now we're not on the main thread we can check all caches
                CacheableBitmapDrawable result;

                try {
                    result = mCache.get(url, null);
                } catch (Exception e) {
                    result = null;
                }

                if (null == result) {

                    if (!url.contains(" ")) {
                        // The bitmap isn't cached so download from the web
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        InputStream is = new BufferedInputStream(conn.getInputStream());

                        Bitmap b = decodeSampledBitmapFromResourceMemOpt(is, 1000, 1000);

                        try {
                            is.close();
                        } catch (Exception e) {

                        }
                        try {
                            conn.disconnect();
                        } catch (Exception e) {

                        }

                        // Add to cache
                        try {
                            result = mCache.put(url, b);
                        } catch (Exception e) {
                            result = null;
                        }
                    } else {
                        // there are multiple pictures... uh oh
                        String[] pics = url.split(" ");
                        Bitmap[] bitmaps = new Bitmap[pics.length];

                        // need to download all of them, then combine them
                        for (int i = 0; i < pics.length; i++) {
                            String s = pics[i];

                            // The bitmap isn't cached so download from the web
                            HttpURLConnection conn = (HttpURLConnection) new URL(s).openConnection();
                            InputStream is = new BufferedInputStream(conn.getInputStream());

                            Bitmap b = decodeSampledBitmapFromResourceMemOpt(is, 1000, 1000);

                            try {
                                is.close();
                            } catch (Exception e) {

                            }
                            try {
                                conn.disconnect();
                            } catch (Exception e) {

                            }

                            // Add to cache
                            try {
                                mCache.put(s, b);

                                // throw it into our bitmap array for later
                                bitmaps[i] = b;
                            } catch (Exception e) {
                                result = null;
                            }
                        }

                        // now that we have all of them, we need to put them together
                        Bitmap combined = ImageUtils.combineBitmaps(context, bitmaps);

                        try {
                            result = mCache.put(url, combined);
                        } catch (Exception e) {

                        }
                    }

                }

                return result;

            } catch (IOException e) {
                Log.e("ImageUrlAsyncTask", e.toString());
            } catch (OutOfMemoryError e) {
                Log.v("ImageUrlAsyncTask", "Out of memory error here");
            }

            return null;
        }

        public Bitmap decodeSampledBitmapFromResourceMemOpt(
                InputStream inputStream, int reqWidth, int reqHeight) {

            byte[] byteArr = new byte[0];
            byte[] buffer = new byte[1024];
            int len;
            int count = 0;

            try {
                while ((len = inputStream.read(buffer)) > -1) {
                    if (len != 0) {
                        if (count + len > byteArr.length) {
                            byte[] newbuf = new byte[(count + len) * 2];
                            System.arraycopy(byteArr, 0, newbuf, 0, count);
                            byteArr = newbuf;
                        }

                        System.arraycopy(buffer, 0, byteArr, count, len);
                        count += len;
                    }
                }

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(byteArr, 0, count, options);

                options.inSampleSize = calculateInSampleSize(options, reqWidth,
                        reqHeight);
                options.inPurgeable = true;
                options.inInputShareable = true;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            } catch (Exception e) {
                e.printStackTrace();

                return null;
            }
        }

        public static int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = opt.outHeight;
            final int width = opt.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > reqHeight
                        && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }

        @Override
        protected void onPostExecute(CacheableBitmapDrawable result) {
            super.onPostExecute(result);

            try {
                if (result != null && holder.tweetId == id) {
                    holder.image.setImageDrawable(result);
                    Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_fast);

                    if (holder.tweetId == id) {
                        holder.image.startAnimation(fadeInAnimation);
                    }
                }

            } catch (Exception e) {

            }
        }
    }
}
