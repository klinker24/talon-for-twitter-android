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
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.BrowserActivity;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.ui.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.ui.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.manipulations.PhotoViewerDialog;
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
import java.util.Date;
import java.util.concurrent.RejectedExecutionException;

import twitter4j.DirectMessage;
import twitter4j.MediaEntity;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class TimeLineCursorAdapter extends CursorAdapter {

    public Cursor cursor;
    public AppSettings settings;
    public Context context;
    public final LayoutInflater inflater;
    private boolean isDM = false;
    private SharedPreferences sharedPrefs;
    private int cancelButton;
    private int border;

    private Handler[] mHandlers;
    private int currHandler;

    public boolean hasKeyboard = false;

    public int layout;
    private XmlResourceParser addonLayout = null;
    public Resources res;
    private BitmapLruCache mCache;

    private ColorDrawable transparent;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    public boolean isHomeTimeline;

    public int contentHeight = 0;
    public int headerMultiplier = 0;
    public Expandable expander;

    public static class ViewHolder {
        public TextView name;
        public TextView screenTV;
        public ImageView profilePic;
        public TextView tweet;
        public TextView time;
        public TextView retweeter;
        public LinearLayout expandArea;
        public ImageView image;
        public LinearLayout background;
        public ImageView playButton;
        public FrameLayout imageHolder;
        public View rootView;

        public long tweetId;
        public boolean isFavorited;
        public String proPicUrl;
        public String screenName;
        public String picUrl;
        public String retweeterName;

        public boolean preventNextClick = false;
    }

    public BitmapLruCache getCache() {
        return App.getInstance(context).getBitmapCache();
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, boolean isHomeTimeline, Expandable expander) {
        super(context, cursor, 0);

        this.isHomeTimeline = isHomeTimeline;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        settings = AppSettings.getInstance(context);

        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.cancelButton});
        cancelButton = a.getResourceId(0, 0);
        a.recycle();

        layout = R.layout.tweet_full_screen;

        TypedArray b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        border = b.getResourceId(0, 0);
        b.recycle();

        mCache = getCache();

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        transparent = new ColorDrawable(android.R.color.transparent);

        mHandlers = new Handler[10];
        for (int i = 0; i < 10; i++) {
            mHandlers[i] = new Handler();
        }

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
    }
    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, boolean isHomeTimeline) {
        super(context, cursor, 0);

        this.isHomeTimeline = isHomeTimeline;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        settings = AppSettings.getInstance(context);

        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.cancelButton});
        cancelButton = a.getResourceId(0, 0);
        a.recycle();

        layout = R.layout.tweet_full_screen;

        TypedArray b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        border = b.getResourceId(0, 0);
        b.recycle();

        mCache = getCache();

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        transparent = new ColorDrawable(android.R.color.transparent);

        mHandlers = new Handler[10];
        for (int i = 0; i < 10; i++) {
            mHandlers[i] = new Handler();
        }

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            // we need to take off the size of the action bar and status bar
            contentHeight -= Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context);
        }
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM, Expandable expander) {
        super(context, cursor, 0);

        this.isHomeTimeline = false;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        settings = AppSettings.getInstance(context);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.cancelButton});
        cancelButton = a.getResourceId(0, 0);
        a.recycle();

        layout = R.layout.tweet_full_screen;

        TypedArray b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        border = b.getResourceId(0, 0);
        b.recycle();

        mCache = getCache();

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        transparent = new ColorDrawable(android.R.color.transparent);

        mHandlers = new Handler[10];
        for (int i = 0; i < 10; i++) {
            mHandlers[i] = new Handler();
        }

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;

        this.expander = expander;
    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM) {
        super(context, cursor, 0);

        this.isHomeTimeline = false;

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;
        
        settings = AppSettings.getInstance(context);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.cancelButton});
        cancelButton = a.getResourceId(0, 0);
        a.recycle();

        layout = R.layout.tweet_full_screen;

        TypedArray b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        border = b.getResourceId(0, 0);
        b.recycle();

        mCache = getCache();

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        transparent = new ColorDrawable(android.R.color.transparent);

        mHandlers = new Handler[10];
        for (int i = 0; i < 10; i++) {
            mHandlers[i] = new Handler();
        }

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        contentHeight = size.y;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.screenTV = (TextView) v.findViewById(R.id.screenname);
        holder.profilePic = (ImageView) v.findViewById(R.id.profile_pic);
        holder.time = (TextView) v.findViewById(R.id.time);
        holder.tweet = (TextView) v.findViewById(R.id.tweet);
        holder.expandArea = (LinearLayout) v.findViewById(R.id.expansion);
        holder.image = (NetworkedCacheableImageView) v.findViewById(R.id.image);
        holder.retweeter = (TextView) v.findViewById(R.id.retweeter);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button);
        holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.screenTV.setTextSize(settings.textSize - 2);
        holder.name.setTextSize(settings.textSize + 4);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);

        holder.profilePic.setClipToOutline(true);
        holder.image.setClipToOutline(true);

        holder.rootView = v;

        v.setTag(holder);

        return v;
    }

    @Override
    public void bindView(final View view, Context mContext, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        if (holder.expandArea.getHeight() != 0) {
            removeExpansion(holder, false);
        }

        final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
        holder.tweetId = id;
        final String profilePic = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC));
        holder.proPicUrl = profilePic;
        String tweetTexts = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT));
        final String name = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME));
        final String screenname = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));
        final String picUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL));
        holder.picUrl = picUrl;
        final long longTime = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME));
        final String otherUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_URL));
        final String users = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_USERS));
        final String hashtags = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_HASHTAGS));

        final int position = cursor.getPosition();

        String retweeter;
        try {
            retweeter = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER));
        } catch (Exception e) {
            retweeter = "";
        }

        final String tweetText = tweetTexts;

        if(!settings.reverseClickActions) {
            final String fRetweeter = retweeter;
            if (!isDM) {
                View.OnLongClickListener click = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
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

                        if (isHomeTimeline) {
                            sharedPrefs.edit()
                                    .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                    .commit();
                        }

                        context.startActivity(viewTweet);

                        return true;
                    }
                };
                holder.background.setOnLongClickListener(click);
            }

            if (!isDM) {
                View.OnClickListener click = new View.OnClickListener() {
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
                            removeKeyboard(holder);
                        }
                    }
                };
                holder.background.setOnClickListener(click);
            }
        } else {
            final String fRetweeter = retweeter;
            if (!isDM) {
                View.OnClickListener click = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (holder.preventNextClick) {
                            holder.preventNextClick = false;
                            return;
                        }
                        String link = "";

                        boolean displayPic = !holder.picUrl.equals("") && !holder.picUrl.contains("youtube");
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
                        viewTweet.putExtra("picture", displayPic);
                        viewTweet.putExtra("other_links", otherUrl);
                        viewTweet.putExtra("tweetid", holder.tweetId);
                        viewTweet.putExtra("proPic", profilePic);
                        viewTweet.putExtra("users", users);
                        viewTweet.putExtra("hashtags", hashtags);

                        if (isHomeTimeline) {
                            sharedPrefs.edit()
                                    .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                    .commit();
                        }

                        /*ActivityOptions options = ActivityOptions
                                .makeSceneTransitionAnimation((Activity) context,
                                        Pair.create((View) holder.profilePic, "profile_picture"),
                                        Pair.create((View)holder.name, "person_name"),
                                        Pair.create((View)holder.screenTV, "person_handle"),
                                        Pair.create((View)holder.image, "large_image"));*/

                        context.startActivity(viewTweet/*, options.toBundle()*/);
                        //((Activity) context).getFragmentManager().executePendingTransactions();
                    }
                };
                holder.background.setOnClickListener(click);
            }

            if (!isDM) {
                View.OnLongClickListener click = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {

                        if (holder.expandArea.getVisibility() != View.VISIBLE) {
                            addExpansion(holder, position, screenname, users, otherUrl.split("  "), holder.picUrl, id, hashtags.split("  "));
                        } else {
                            removeExpansion(holder, true);
                            removeKeyboard(holder);
                        }

                        return true;
                    }
                };

                holder.background.setOnLongClickListener(click);
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
                viewProfile.putExtra("proPic", profilePic);
                viewProfile.putExtra("tweetid", holder.tweetId);
                viewProfile.putExtra("retweet", holder.retweeter.getVisibility() == View.VISIBLE);
                viewProfile.putExtra("long_click", false);

                if (isHomeTimeline) {
                    sharedPrefs.edit()
                            .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                            .commit();
                }

                context.startActivity(viewProfile);
            }
        });


        if (holder.screenTV.getVisibility() == View.GONE) {
            holder.screenTV.setVisibility(View.VISIBLE);
        }
        holder.screenTV.setText("@" + screenname);
        holder.name.setText(name);

        if (!settings.absoluteDate) {
            holder.time.setText(Utils.getTimeAgo(longTime, context));
        } else {
            Date date = new Date(longTime);
            holder.time.setText(timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date));
        }

        holder.tweet.setText(tweetText);

        boolean picture = false;

        if(settings.inlinePics && holder.picUrl != null) {
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

                if (holder.picUrl.contains("youtube")) {
                    if (holder.playButton.getVisibility() == View.GONE) {
                        holder.playButton.setVisibility(View.VISIBLE);
                    }

                    final String fRetweeter = retweeter;

                    holder.imageHolder.setOnClickListener(new View.OnClickListener() {
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
                            viewTweet.putExtra("clicked_youtube", true);

                            if (isHomeTimeline) {
                                sharedPrefs.edit()
                                        .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                        .commit();
                            }

                            context.startActivity(viewTweet);
                        }
                    });

                    holder.image.setImageDrawable(null);

                    picture = true;


                } else {
                    if (holder.playButton.getVisibility() == View.VISIBLE) {
                        holder.playButton.setVisibility(View.GONE);
                    }

                    holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            if (isHomeTimeline) {
                                sharedPrefs.edit()
                                        .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                        .commit();
                            }

                            Intent viewImage = new Intent(context, PhotoViewerDialog.class);
                            viewImage.putExtra("url", holder.picUrl);

                            context.startActivity(viewImage);
                        }
                    });

                    holder.image.setImageDrawable(null);

                    picture = true;
                }
            }
        }


        if (retweeter.length() > 0 && !isDM) {
            String text = context.getResources().getString(R.string.retweeter);
            //holder.retweeter.setText(settings.displayScreenName ? text + retweeter : text.substring(0, text.length() - 2) + " " + name);
            holder.retweeter.setText(text + retweeter);
            holder.retweeterName = retweeter;
            holder.retweeter.setVisibility(View.VISIBLE);
        } else if (holder.retweeter.getVisibility() == View.VISIBLE) {
            holder.retweeter.setVisibility(View.GONE);
        }

        if (picture) {
            CacheableBitmapDrawable wrapper = mCache.getFromMemoryCache(holder.picUrl);
            if (wrapper != null) {
                holder.image.setImageDrawable(wrapper);
                picture = false;
            }
        }

        CacheableBitmapDrawable wrapper2 = mCache.getFromMemoryCache(holder.proPicUrl);

        final boolean gotProPic;
        if (wrapper2 == null) {
            gotProPic = false;
            if (holder.profilePic.getDrawable() != null) {
                holder.profilePic.setImageDrawable(null);
            }
        } else {
            gotProPic = true;
            holder.profilePic.setImageDrawable(wrapper2);
        }

        final boolean hasPicture = picture;
        mHandlers[currHandler].removeCallbacksAndMessages(null);
        mHandlers[currHandler].postDelayed(new Runnable() {
            @Override
            public void run() {
                if (holder.tweetId == id) {
                    if (hasPicture) {
                        loadImage(context, holder, holder.picUrl, mCache, id);
                    }

                    if (!gotProPic) {
                        loadProPic(context, holder, holder.proPicUrl, mCache, id);
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

                }
            }
        }, 400);
        currHandler++;

        if (currHandler == 10) {
            currHandler = 0;
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
    }

    protected void startAnimation(Animator animator) {
        animator.start();
    }

    public static final int ANIMATION_DURATION = 200;
    public static final Interpolator ANIMATION_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    public void addExpansion(final ViewHolder holder, int position, String screenname, String users, final String[] otherLinks, final String webpage, final long tweetId, String[] hashtags) {
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

        final int distance;
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

                //startArticleLoader(holder, holder.position, new ArticleItem().fillFromCursor(data));
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

        expander.expandViewOpen((int) holder.rootView.getY() + headerPadding * headerMultiplier, position);

        /*if (isDM) {
            holder.retweet.setVisibility(View.GONE);
            holder.retweetCount.setVisibility(View.GONE);
            holder.favCount.setVisibility(View.GONE);
            holder.favorite.setVisibility(View.GONE);
        } else {
            holder.retweet.setVisibility(View.VISIBLE);
            holder.retweetCount.setVisibility(View.VISIBLE);
            holder.favCount.setVisibility(View.VISIBLE);
            holder.favorite.setVisibility(View.VISIBLE);
        }

        try {
            holder.replyButton.setVisibility(View.GONE);
        } catch (Exception e) {

        }
        try {
            holder.charRemaining.setVisibility(View.GONE);
        } catch (Exception e) {

        }

        holder.screenName = screenname;

        // used to find the other names on a tweet... could be optimized i guess, but only run when button is pressed
        if (!isDM) {
            String text = holder.tweet.getText().toString();
            String extraNames = "";

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
                holder.reply.setText("@" + screenname + " " + extraNames);
            } else {
                holder.reply.setText(extraNames);
            }

            if (settings.autoInsertHashtags && hashtags != null) {
                for (String s : hashtags) {
                    if (!s.equals("")) {
                        holder.reply.append("#" + s + " ");
                    }
                }
            }
        }

        holder.reply.setSelection(holder.reply.getText().length());

        if (holder.favCount.getText().toString().length() <= 2) {
            holder.favCount.setText(" -");
            holder.retweetCount.setText(" -");
        }

        //ExpansionAnimation expandAni = new ExpansionAnimation(holder.expandArea, 450);
        holder.expandArea.setVisibility(View.VISIBLE);//startAnimation(expandAni);

        if (holder.favCount.getText().toString().equals(" -")) {
            getCounts(holder, tweetId);
        }

        holder.favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FavoriteStatus(holder, holder.tweetId).execute();
            }
        });

        holder.retweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new RetweetStatus(holder, holder.tweetId).execute();
            }
        });

        holder.retweet.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.remove_retweet))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new RemoveRetweet(holder.tweetId).execute();
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

            class RemoveRetweet extends AsyncTask<String, Void, Boolean> {

                private long tweetId;

                public RemoveRetweet(long tweetId) {
                    this.tweetId = tweetId;
                }

                protected void onPreExecute() {
                    holder.retweet.clearColorFilter();

                    Toast.makeText(context, context.getResources().getString(R.string.removing_retweet), Toast.LENGTH_SHORT).show();
                }

                protected Boolean doInBackground(String... urls) {
                    try {
                        Twitter twitter =  Utils.getTwitter(context, settings);
                        ResponseList<twitter4j.Status> retweets = twitter.getRetweets(tweetId);
                        for (twitter4j.Status retweet : retweets) {
                            if(retweet.getUser().getId() == settings.myId)
                                twitter.destroyStatus(retweet.getId());
                        }
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                protected void onPostExecute(Boolean deleted) {
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
        });

        holder.reply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeExpansionWithAnimation(holder);

                Intent compose = new Intent(context, ComposeActivity.class);
                String string = holder.reply.getText().toString();
                try{
                    compose.putExtra("user", string.substring(0, string.length() - 1));
                } catch (Exception e) {

                }
                compose.putExtra("id", holder.tweetId);
                compose.putExtra("reply_to_text", "@" + holder.screenName + ": " + holder.tweet.getText().toString());

                if (isHomeTimeline) {
                    sharedPrefs.edit()
                            .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                            .commit();
                }

                context.startActivity(compose);
            }
        });

        holder.reply.requestFocus();
        removeKeyboard(holder);

        // this isn't going to run anymore, but just in case i put it back i guess
        if (holder.replyButton != null) {
            holder.replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new ReplyToStatus(holder, holder.tweetId).execute();
                }
            });

            holder.charRemaining.setText(140 - holder.reply.getText().length() + "");

            holder.reply.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    hasKeyboard = b;
                }
            });

            holder.reply.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    holder.charRemaining.setText(140 - holder.reply.getText().length() + "");
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });

        }

        final String name = screenname;

        try {
            holder.shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent=new Intent(android.content.Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    String text = holder.tweet.getText().toString();

                    text = restoreLinks(text);

                    text = "@" + name + ": " + text;

                    Log.v("talon_sharing", "text: " + text);

                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    intent.putExtra(Intent.EXTRA_TEXT, text);

                    if (isHomeTimeline) {
                        sharedPrefs.edit()
                                .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                .commit();
                    }

                    context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.menu_share)));
                }

                public String restoreLinks(String text) {
                    String full = text;

                    String[] split = text.split("\\s");
                    String[] otherLink = new String[otherLinks.length];

                    for (int i = 0; i < otherLinks.length; i++) {
                        otherLink[i] = "" + otherLinks[i];
                    }


                    boolean changed = false;

                    if (otherLink.length > 0) {
                        for (int i = 0; i < split.length; i++) {
                            String s = split[i];

                            if (Patterns.WEB_URL.matcher(s).find()) { // we know the link is cut off
                                String f = s.replace("...", "").replace("http", "");

                                for (int x = 0; x < otherLink.length; x++) {
                                    if (otherLink[x].contains(f)) {
                                        changed = true;
                                        // for some reason it wouldn't match the last "/" on a url and it was stopping it from opening
                                        if (otherLink[x].substring(otherLink[x].length() - 1, otherLink[x].length()).equals("/")) {
                                            otherLink[x] = otherLink[x].substring(0, otherLink[x].length() - 1);
                                        }
                                        f = otherLink[x];
                                        otherLink[x] = "";
                                        break;
                                    }
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

                    if (!webpage.equals("")) {
                        for (int i = 0; i < split.length; i++) {
                            String s = split[i];
                            s = s.replace("...", "");

                            if (Patterns.WEB_URL.matcher(s).find() && (s.startsWith("t.co/") || s.contains("twitter.com/"))) { // we know the link is cut off
                                String replace = otherLinks[otherLinks.length - 1];
                                if (replace.replace(" ", "").equals("")) {
                                    replace = webpage;
                                }
                                split[i] = replace;
                                changed = true;
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

                    return full;
                }
            });


            holder.quoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent=new Intent(context, ComposeActivity.class);
                    intent.setType("text/plain");
                    String text = holder.tweet.getText().toString();

                    text = TweetLinkUtils.removeColorHtml(text, settings);
                    text = restoreLinks(text);

                    if (!settings.preferRT) {
                        text = "\"@" + name + ": " + text + "\" ";
                    } else {
                        text = " RT @" + name + ": " + text;
                    }
                    intent.putExtra("user", text);
                    intent.putExtra("id", tweetId);

                    if (isHomeTimeline) {
                        sharedPrefs.edit()
                                .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                .commit();
                    }

                    context.startActivity(intent);
                }

                public String restoreLinks(String text) {
                    String full = text;

                    String[] split = text.split("\\s");
                    String[] otherLink = new String[otherLinks.length];

                    for (int i = 0; i < otherLinks.length; i++) {
                        otherLink[i] = "" + otherLinks[i];
                    }


                    boolean changed = false;

                    if (otherLink.length > 0) {
                        for (int i = 0; i < split.length; i++) {
                            String s = split[i];

                            if (Patterns.WEB_URL.matcher(s).find()) { // we know the link is cut off
                                String f = s.replace("...", "").replace("http", "");

                                for (int x = 0; x < otherLink.length; x++) {
                                    if (otherLink[x].contains(f)) {
                                        changed = true;
                                        // for some reason it wouldn't match the last "/" on a url and it was stopping it from opening
                                        if (otherLink[x].substring(otherLink[x].length() - 1, otherLink[x].length()).equals("/")) {
                                            otherLink[x] = otherLink[x].substring(0, otherLink[x].length() - 1);
                                        }
                                        f = otherLink[x];
                                        otherLink[x] = "";
                                        break;
                                    }
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

                    if (!webpage.equals("")) {
                        for (int i = 0; i < split.length; i++) {
                            String s = split[i];
                            s = s.replace("...", "");

                            if (Patterns.WEB_URL.matcher(s).find() && (s.startsWith("t.co/") || s.contains("twitter.com/"))) { // we know the link is cut off
                                String replace = otherLinks[otherLinks.length - 1];
                                if (replace.replace(" ", "").equals("")) {
                                    replace = webpage;
                                }
                                split[i] = replace;
                                changed = true;
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

                    return full;
                }
            });
        } catch (Exception e) {
            // theme made before these were implemented
        }
        if (settings.addonTheme) {
            try {
                Resources resourceAddon = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                int back = resourceAddon.getIdentifier("reply_entry_background", "drawable", settings.addonThemePackage);
                holder.reply.setBackgroundDrawable(resourceAddon.getDrawable(back));
            } catch (Exception e) {
                // theme does not include a reply entry box
            }
        }*/
    }

    public void removeKeyboard(ViewHolder holder) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(holder.reply.getWindowToken(), 0);
    }

    class DeleteTweet extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = Utils.getTwitter(context, settings);

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

    public void getFavoriteCount(final ViewHolder holder, final long tweetId) {

        Thread getCount = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);
                    final Status status;
                    if (holder.retweeter.getVisibility() != View.GONE) {
                        status = twitter.showStatus(holder.tweetId).getRetweetedStatus();
                    } else {
                        status = twitter.showStatus(tweetId);
                    }

                    if (status != null && holder.tweetId == tweetId) {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //holder.favCount.setText(" " + status.getFavoriteCount());

                                if (status.isFavorited()) {
                                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.favoritedButton});
                                    int resource = a.getResourceId(0, 0);
                                    a.recycle();

                                    if (!settings.addonTheme) {
                                        //holder.favorite.setColorFilter(context.getResources().getColor(R.color.app_color));
                                    } else {
                                        //holder.favorite.setColorFilter(settings.accentInt);
                                    }

                                    //holder.favorite.setImageDrawable(context.getResources().getDrawable(resource));
                                    holder.isFavorited = true;
                                } else {
                                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.notFavoritedButton});
                                    int resource = a.getResourceId(0, 0);
                                    a.recycle();

                                    //holder.favorite.setImageDrawable(context.getResources().getDrawable(resource));
                                    holder.isFavorited = false;

                                    //holder.favorite.clearColorFilter();
                                }
                            }
                        });
                    }

                } catch (Exception e) {

                }
            }
        });

        getCount.setPriority(7);
        getCount.start();
    }

    public void getCounts(final ViewHolder holder, final long tweetId) {

        Thread getCount = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);
                    final Status status;
                    if (holder.retweeter.getVisibility() != View.GONE) {
                        status = twitter.showStatus(holder.tweetId).getRetweetedStatus();
                    } else {
                        status = twitter.showStatus(tweetId);
                    }

                    if (status != null && holder.tweetId == tweetId) {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //holder.favCount.setText(" " + status.getFavoriteCount());
                                //holder.retweetCount.setText(" " + status.getRetweetCount());

                                if (status.isFavorited()) {
                                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.favoritedButton});
                                    int resource = a.getResourceId(0, 0);
                                    a.recycle();

                                    if (!settings.addonTheme) {
                                        //holder.favorite.setColorFilter(context.getResources().getColor(R.color.app_color));
                                    } else {
                                        //holder.favorite.setColorFilter(settings.accentInt);
                                    }

                                    //holder.favorite.setImageDrawable(context.getResources().getDrawable(resource));
                                    holder.isFavorited = true;
                                } else {
                                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.notFavoritedButton});
                                    int resource = a.getResourceId(0, 0);
                                    a.recycle();

                                    //holder.favorite.setImageDrawable(context.getResources().getDrawable(resource));
                                    holder.isFavorited = false;

                                    //holder.favorite.clearColorFilter();
                                }

                                if (status.isRetweetedByMe()) {
                                    if (!settings.addonTheme) {
                                        //holder.retweet.setColorFilter(context.getResources().getColor(R.color.app_color));
                                    } else {
                                        //holder.retweet.setColorFilter(settings.accentInt);
                                    }
                                } else {
                                    //holder.retweet.clearColorFilter();
                                }
                            }
                        });
                    }

                } catch (Exception e) {

                }
            }
        });

        getCount.setPriority(7);
        getCount.start();
    }

    public void getRetweetCount(final ViewHolder holder, final long tweetId) {

        Thread getRetweetCount = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);
                    twitter4j.Status status = twitter.showStatus(holder.tweetId);
                    final boolean retweetedByMe = status.isRetweetedByMe();
                    final String count = "" + status.getRetweetCount();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (tweetId == holder.tweetId) {
                                if (retweetedByMe) {
                                    if (!settings.addonTheme) {
                                        //holder.retweet.setColorFilter(context.getResources().getColor(R.color.app_color));
                                    } else {
                                        //holder.retweet.setColorFilter(settings.accentInt);
                                    }
                                } else {
                                    //holder.retweet.clearColorFilter();
                                }
                                if (count != null) {
                                    //holder.retweetCount.setText(" " + count);
                                }
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        getRetweetCount.setPriority(7);
        getRetweetCount.start();
    }

    class FavoriteStatus extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public FavoriteStatus(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
        }

        protected void onPreExecute() {
            if (!holder.isFavorited) {
                Toast.makeText(context, context.getResources().getString(R.string.favoriting_status), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.removing_favorite), Toast.LENGTH_SHORT).show();
            }
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
                if (holder.isFavorited) {
                    twitter.destroyFavorite(tweetId);
                } else {
                    twitter.createFavorite(tweetId);
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            Toast.makeText(context, context.getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
            getFavoriteCount(holder, tweetId);
        }
    }

    class RetweetStatus extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public RetweetStatus(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
        }

        protected void onPreExecute() {
            Toast.makeText(context, context.getResources().getString(R.string.retweeting_status), Toast.LENGTH_SHORT).show();
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
                twitter.retweetStatus(tweetId);
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            Toast.makeText(context, context.getResources().getString(R.string.retweet_success), Toast.LENGTH_SHORT).show();
            getRetweetCount(holder, tweetId);
        }
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

    public void loadProPic(Context context, final ViewHolder holder, final String url, BitmapLruCache mCache, final long tweetId) {
        // First check whether there's already a task running, if so cancel it
        /*if (null != mCurrentTask) {
            mCurrentTask.cancel(true);
        }*/

        if (url == null) {
            return;
        }

        // Memory Cache doesn't have the URL, do threaded request...
        if (holder.profilePic.getDrawable() != null) {
            holder.profilePic.setImageDrawable(null);
        }

        mCurrentTask = new ImageUrlAsyncTask(context, holder, mCache, tweetId, holder.profilePic);

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

    private static class ImageUrlAsyncTask
            extends AsyncTask<String, Void, CacheableBitmapDrawable> {

        private BitmapLruCache mCache;
        private Context context;
        private ViewHolder holder;
        private long id;
        private ImageView iv;

        ImageUrlAsyncTask(Context context, ViewHolder holder, BitmapLruCache cache, long tweetId) {
            this.context = context;
            mCache = cache;
            this.holder = holder;
            this.id = tweetId;
            this.iv = null;
        }

        ImageUrlAsyncTask(Context context, ViewHolder holder, BitmapLruCache cache, long tweetId, ImageView iv) {
            this.context = context;
            mCache = cache;
            this.holder = holder;
            this.id = tweetId;
            this.iv = iv;
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
                }

                // Now we're not on the main thread we can check all caches
                CacheableBitmapDrawable result;

                result = mCache.get(url, null);

                if (null == result) {

                    Bitmap b;
                    if (url.contains("ton.twitter.com")) {
                        // it is a direct message picture
                        TwitterDMPicHelper helper = new TwitterDMPicHelper();
                        b = helper.getDMPicture(url, Utils.getTwitter(context, AppSettings.getInstance(context)));
                    } else {

                        // The bitmap isn't cached so download from the web
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        InputStream is = new BufferedInputStream(conn.getInputStream());

                        b = decodeSampledBitmapFromResourceMemOpt(is, 500, 500);

                        try {
                            is.close();
                        } catch (Exception e) {

                        }
                        try {
                            conn.disconnect();
                        } catch (Exception e) {

                        }
                    }

                    try {
                        result = mCache.put(url, b);
                    } catch (Exception e) {
                        result = null;
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
                    if (iv == null) {
                        holder.image.setImageDrawable(result);
                        Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_fast);

                        holder.image.startAnimation(fadeInAnimation);
                    } else {
                        iv.setImageDrawable(result);
                        Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_fast);

                        iv.startAnimation(fadeInAnimation);
                    }
                }

            } catch (Exception e) {

            }
        }
    }
}
