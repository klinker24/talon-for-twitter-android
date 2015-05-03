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
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.*;
import android.widget.*;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.TweetView;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.manipulations.MultiplePicsPopup;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import de.hdodenhof.circleimageview.CircleImageView;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class TimeLineCursorAdapter extends CursorAdapter {

    public Map<Long, Status> quotedTweets = new HashMap<>();
    public Set<String> muffledUsers = new HashSet<String>();
    public Cursor cursor;
    public AppSettings settings;
    public Context context;
    public final LayoutInflater inflater;
    private boolean isDM = false;
    protected SharedPreferences sharedPrefs;
    private int cancelButton;
    private int border;
    private boolean secondAcc = false;

    protected Handler[] mHandlers;
    protected int currHandler;

    public boolean hasKeyboard = false;

    public int layout;
    private XmlResourceParser addonLayout = null;
    public Resources res;
    protected BitmapLruCache mCache;

    private ColorDrawable transparent;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    public boolean isHomeTimeline;

    public int contentHeight = 0;
    public int headerMultiplier = 0;
    public Expandable expander;

    public static MultiplePicsPopup multPics;
    public boolean hasConvo = false;

    public boolean hasExpandedTweet = false;

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

    public BitmapLruCache getCache() {
        return App.getInstance(context).getBitmapCache();
    }

    public void init() {
        init(true);
    }
    public void init(boolean cont) {
        settings = AppSettings.getInstance(context);

        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.cancelButton});
        cancelButton = a.getResourceId(0, 0);
        a.recycle();

        layout = R.layout.tweet;

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

        if (!settings.bottomPictures) {
            holder.image = (NetworkedCacheableImageView) v.findViewById(R.id.image);
            holder.playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button);
            holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder);
        } else {
            holder.image = (NetworkedCacheableImageView) v.findViewById(R.id.image_bellow);
            holder.playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button_bellow);
            holder.imageHolder = (FrameLayout) v.findViewById(R.id.picture_holder_bellow);
        }

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.screenTV.setTextSize(settings.textSize - 2);
        holder.name.setTextSize(settings.textSize + 4);
        holder.muffledName.setTextSize(settings.textSize);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);

        //holder.profilePic.setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.image.setClipToOutline(true);
        }

        holder.rootView = v;

        v.setTag(holder);

        return v;
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
                    boolean displayPic = !holder.picUrl.equals("") && !holder.picUrl.contains("youtube") && !(hasGif);
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
                    context.startActivity(viewTweet);
                }
            });

            if (expander != null && ! MainActivity.isPopup) {
                holder.background.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if (holder.expandArea.getVisibility() == View.GONE) {
                            if (!tweetText.contains("vine.co/v/")) {
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
                    boolean displayPic = !holder.picUrl.equals("") && !holder.picUrl.contains("youtube") && !(hasGif);
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
                            if (!tweetText.contains("vine.co/v/")) {
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

        holder.tweet.setText(tweetText);

        boolean picture = false;

        if((settings.inlinePics || isDM) && holder.picUrl != null) {
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

                if (holder.picUrl.contains("youtube") || (holder.gifUrl != null && !android.text.TextUtils.isEmpty(holder.gifUrl))) {
                    if (holder.playButton.getVisibility() == View.GONE) {
                        holder.playButton.setVisibility(View.VISIBLE);
                    }

                    final String fRetweeter = retweeter;

                    holder.imageHolder.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String link;

                            boolean hasGif = holder.gifUrl != null && !holder.gifUrl.isEmpty();
                            boolean displayPic = !holder.picUrl.equals("") && !holder.picUrl.contains("youtube") && !(hasGif);
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
                            viewTweet.putExtra("animated_gif", holder.gifUrl);

                            if (isHomeTimeline) {
                                sharedPrefs.edit()
                                        .putLong("current_position_" + settings.currentAccount, holder.tweetId)
                                        .commit();
                            }

                            /*ActivityOptions options = ActivityOptions
                                    .makeSceneTransitionAnimation(((Activity) context), displayPic ? holder.image : holder.profilePic, "image");*/

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

                            if (holder.picUrl.contains(" ") && !MainActivity.isPopup) {
                                multPics = new MultiplePicsPopup(context, context.getResources().getBoolean(R.bool.isTablet), holder.picUrl);
                                multPics.setFullScreen();
                                multPics.setExpansionPointForAnim(view);
                                multPics.show();
                            } else {
                                Intent photo = new Intent(context, PhotoViewerActivity.class).putExtra("url", holder.picUrl);
                                photo.putExtra("shared_trans", true);

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    ActivityOptions options = ActivityOptions
                                            .makeSceneTransitionAnimation(((Activity)context), holder.image, "image");

                                    context.startActivity(photo, options.toBundle());
                                } else {
                                    context.startActivity(photo);
                                }

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

                    if (otherUrl.contains("/status/")) {
                        loadEmbeddedTweet(holder, otherUrl);
                    }
                }
            }
        }, 400);
        currHandler++;

        if (currHandler == 10) {
            currHandler = 0;
        }

        if (otherUrl.contains("/status/")) {
            holder.embeddedTweet.setVisibility(View.VISIBLE);
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
    public static Interpolator ANIMATION_INTERPOLATOR;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ANIMATION_INTERPOLATOR = new PathInterpolator(.1f,.1f,.2f,1f);
        } else {
            ANIMATION_INTERPOLATOR = new DecelerateInterpolator();
        }
    }

    public void addExpansion(final ViewHolder holder, int position, final String screenname, String users, final String[] otherLinks, final String webpage, final long tweetId, String[] hashtags) {

        hasExpandedTweet = true;

        final String text = holder.tweet.getText().toString();
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
                                v.hideImage(true);

                                holder.embeddedTweet.addView(v.getView());
                            }
                        });
                    }
                }
            }
        }).start();
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

                    if (!url.contains(" ")) {
                        Bitmap b;
                        if (url.contains("ton.twitter.com")) {
                            // it is a direct message picture
                            TwitterDMPicHelper helper = new TwitterDMPicHelper();
                            b = helper.getDMPicture(url, Utils.getTwitter(context, AppSettings.getInstance(context)), context);
                        } else {

                            // The bitmap isn't cached so download from the web
                            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                            InputStream is = new BufferedInputStream(conn.getInputStream());

                            b = decodeSampledBitmapFromResourceMemOpt(is, 1000, 1000);

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
