package com.klinker.android.twitter.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.manipulations.ExpansionAnimation;
import com.klinker.android.twitter.manipulations.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.UserProfileActivity;
import com.klinker.android.twitter.ui.tweet_viewer.TweetPager;
import com.klinker.android.twitter.ui.widgets.PhotoViewerDialog;
import com.klinker.android.twitter.utils.EmojiUtils;
import com.klinker.android.twitter.utils.HtmlUtils;
import com.klinker.android.twitter.utils.ImageUtils;
import com.klinker.android.twitter.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class TimelineArrayAdapter extends ArrayAdapter<Status> {

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
    public boolean talonLayout;
    public BitmapLruCache mCache;

    public int type;
    public String username;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    public static class ViewHolder {
        public TextView name;
        public ImageView profilePic;
        public TextView tweet;
        public TextView time;
        public TextView retweeter;
        public EditText reply;
        public ImageButton favorite;
        public ImageButton retweet;
        public TextView favCount;
        public TextView retweetCount;
        public LinearLayout expandArea;
        public ImageButton replyButton;
        public ImageView image;
        public LinearLayout background;
        public ImageView playButton;
        public TextView screenTV;

        public long tweetId;
        public boolean isFavorited;
        public String screenName;

    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = new AppSettings(context);

        this.type = NORMAL;

        this.username = "";

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, int type) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = new AppSettings(context);

        this.type = type;
        this.username = "";

        setUpLayout();
    }

    public TimelineArrayAdapter(Context context, ArrayList<Status> statuses, String username) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = new AppSettings(context);

        this.type = NORMAL;
        this.username = username;

        setUpLayout();
    }

    public void setUpLayout() {
        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        talonLayout = settings.layout == AppSettings.LAYOUT_TALON;

        if (settings.addonTheme) {
            try {
                res = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                addonLayout = res.getLayout(res.getIdentifier("tweet", "layout", settings.addonThemePackage));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        layout = talonLayout ? R.layout.tweet : R.layout.tweet_hangout;

        TypedArray b;
        if (talonLayout) {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        } else {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.squareBorder});
        }
        border = b.getResourceId(0, 0);
        b.recycle();

        mCache = App.getInstance(context).getBitmapCache();
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
        if (settings.addonTheme) {
            try {
                Context viewContext = null;

                if (res == null) {
                    res = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                }

                try {
                    viewContext = context.createPackageContext(settings.addonThemePackage, Context.CONTEXT_IGNORE_SECURITY);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (res != null && viewContext != null) {
                    int id = res.getIdentifier("tweet", "layout", settings.addonThemePackage);
                    v = LayoutInflater.from(viewContext).inflate(res.getLayout(id), null);

                    holder.name = (TextView) v.findViewById(res.getIdentifier("name", "id", settings.addonThemePackage));
                    holder.profilePic = (ImageView) v.findViewById(res.getIdentifier("profile_pic", "id", settings.addonThemePackage));
                    holder.time = (TextView) v.findViewById(res.getIdentifier("time", "id", settings.addonThemePackage));
                    holder.tweet = (TextView) v.findViewById(res.getIdentifier("tweet", "id", settings.addonThemePackage));
                    holder.reply = (EditText) v.findViewById(res.getIdentifier("reply", "id", settings.addonThemePackage));
                    holder.favorite = (ImageButton) v.findViewById(res.getIdentifier("favorite", "id", settings.addonThemePackage));
                    holder.retweet = (ImageButton) v.findViewById(res.getIdentifier("retweet", "id", settings.addonThemePackage));
                    holder.favCount = (TextView) v.findViewById(res.getIdentifier("fav_count", "id", settings.addonThemePackage));
                    holder.retweetCount = (TextView) v.findViewById(res.getIdentifier("retweet_count", "id", settings.addonThemePackage));
                    holder.expandArea = (LinearLayout) v.findViewById(res.getIdentifier("expansion", "id", settings.addonThemePackage));
                    holder.replyButton = (ImageButton) v.findViewById(res.getIdentifier("reply_button", "id", settings.addonThemePackage));
                    holder.image = (ImageView) v.findViewById(res.getIdentifier("image", "id", settings.addonThemePackage));
                    holder.retweeter = (TextView) v.findViewById(res.getIdentifier("retweeter", "id", settings.addonThemePackage));
                    holder.background = (LinearLayout) v.findViewById(res.getIdentifier("background", "id", settings.addonThemePackage));
                    holder.playButton = (ImageView) v.findViewById(res.getIdentifier("play_button", "id", settings.addonThemePackage));
                    holder.screenTV = (TextView) v.findViewById(res.getIdentifier("screenname", "id", settings.addonThemePackage));
                }
            } catch (Exception e) {
                e.printStackTrace();
                v = inflater.inflate(layout, viewGroup, false);

                holder.name = (TextView) v.findViewById(R.id.name);
                holder.profilePic = (ImageView) v.findViewById(R.id.profile_pic);
                holder.time = (TextView) v.findViewById(R.id.time);
                holder.tweet = (TextView) v.findViewById(R.id.tweet);
                holder.reply = (EditText) v.findViewById(R.id.reply);
                holder.favorite = (ImageButton) v.findViewById(R.id.favorite);
                holder.retweet = (ImageButton) v.findViewById(R.id.retweet);
                holder.favCount = (TextView) v.findViewById(R.id.fav_count);
                holder.retweetCount = (TextView) v.findViewById(R.id.retweet_count);
                holder.expandArea = (LinearLayout) v.findViewById(R.id.expansion);
                holder.replyButton = (ImageButton) v.findViewById(R.id.reply_button);
                holder.image = (NetworkedCacheableImageView) v.findViewById(R.id.image);
                holder.retweeter = (TextView) v.findViewById(R.id.retweeter);
                holder.background = (LinearLayout) v.findViewById(R.id.background);
                holder.playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button);
                holder.screenTV = (TextView) v.findViewById(R.id.screenname);
            }
        } else {
            v = inflater.inflate(layout, viewGroup, false);

            holder.name = (TextView) v.findViewById(R.id.name);
            holder.profilePic = (ImageView) v.findViewById(R.id.profile_pic);
            holder.time = (TextView) v.findViewById(R.id.time);
            holder.tweet = (TextView) v.findViewById(R.id.tweet);
            holder.reply = (EditText) v.findViewById(R.id.reply);
            holder.favorite = (ImageButton) v.findViewById(R.id.favorite);
            holder.retweet = (ImageButton) v.findViewById(R.id.retweet);
            holder.favCount = (TextView) v.findViewById(R.id.fav_count);
            holder.retweetCount = (TextView) v.findViewById(R.id.retweet_count);
            holder.expandArea = (LinearLayout) v.findViewById(R.id.expansion);
            holder.replyButton = (ImageButton) v.findViewById(R.id.reply_button);
            holder.image = (NetworkedCacheableImageView) v.findViewById(R.id.image);
            holder.retweeter = (TextView) v.findViewById(R.id.retweeter);
            holder.background = (LinearLayout) v.findViewById(R.id.background);
            holder.playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button);
            holder.screenTV = (TextView) v.findViewById(R.id.screenname);
        }

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.name.setTextSize(settings.textSize + 4);
        holder.screenTV.setTextSize(settings.textSize - 2);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);
        holder.favCount.setTextSize(settings.textSize + 1);
        holder.retweetCount.setTextSize(settings.textSize + 1);
        holder.reply.setTextSize(settings.textSize);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, Status status) {
        final ViewHolder holder = (ViewHolder) view.getTag();

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
        final String profilePic = user.getBiggerProfileImageURL();
        String tweetTexts = thisStatus.getText();
        final String name = user.getName();
        final String screenname = user.getScreenName();

        String[] html = HtmlUtils.getHtmlStatus(thisStatus);
        final String tweetText = html[0];
        final String picUrl = html[1];
        final String otherUrl = html[2];
        final String hashtags = html[3];
        final String users = html[4];

        if(!settings.reverseClickActions) {
            final String fRetweeter = retweeter;
            holder.background.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    String link;

                    boolean displayPic = !picUrl.equals("") && !picUrl.contains("youtube");
                    if (displayPic) {
                        link = picUrl;
                    } else {
                        link = otherUrl.split("  ")[0];
                    }

                    Log.v("tweet_page", "clicked");
                    Intent viewTweet = new Intent(context, TweetPager.class);
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

            holder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.expandArea.getVisibility() == View.GONE) {
                        addExpansion(holder, screenname);
                    } else {
                        removeExpansionWithAnimation(holder);
                        removeKeyboard(holder);
                    }
                }
            });

        } else {
            final String fRetweeter = retweeter;
            holder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String link;

                    boolean displayPic = !picUrl.equals("") && !picUrl.contains("youtube");
                    if (displayPic) {
                        link = picUrl;
                    } else {
                        link = otherUrl.split("  ")[0];
                    }

                    Log.v("tweet_page", "clicked");
                    Intent viewTweet = new Intent(context, TweetPager.class);
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

            holder.background.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (holder.expandArea.getVisibility() == View.GONE) {
                        addExpansion(holder, screenname);
                    } else {
                        removeExpansionWithAnimation(holder);
                        removeKeyboard(holder);
                    }

                    return true;
                }
            });
        }

        if (!screenname.equals(username)) {
            holder.profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent viewProfile = new Intent(context, UserProfileActivity.class);
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

                    Intent viewProfile = new Intent(context, UserProfileActivity.class);
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
        }

        if (!settings.showBoth) {
            holder.name.setText(settings.displayScreenName ? "@" + screenname : name);
        } else {
            if (holder.screenTV.getVisibility() == View.GONE) {
                holder.screenTV.setVisibility(View.VISIBLE);
            }
            holder.name.setText(name);
            holder.screenTV.setText("@" + screenname);
        }

        if (!settings.absoluteDate) {
            holder.time.setText(Utils.getTimeAgo(time, context));
        } else {
            Date date = new Date(time);
            holder.time.setText(timeFormatter.format(date) + ", " + dateFormatter.format(date));
        }
        if (settings.addonTheme) {
            holder.tweet.setText(Html.fromHtml(tweetText.replaceAll("FF8800", settings.accentColor)));
        } else {
            holder.tweet.setText(Html.fromHtml(tweetText));
        }

        if(settings.inlinePics) {
            if (picUrl.equals("")) {
                if (holder.image.getVisibility() == View.VISIBLE) {
                    holder.image.setVisibility(View.GONE);
                }

                if (holder.playButton.getVisibility() == View.VISIBLE) {
                    holder.playButton.setVisibility(View.GONE);
                }
            } else {
                if (picUrl.contains("youtube")) {
                    //holder.image.loadImage(picUrl, false, null);
                    ImageUtils.loadImage(context, holder.image, picUrl, mCache);
                    if (holder.playButton.getVisibility() == View.GONE) {
                        holder.playButton.setVisibility(View.VISIBLE);
                    }

                    final String fRetweeter = retweeter;

                    holder.image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String link;

                            boolean displayPic = !picUrl.equals("") && !picUrl.contains("youtube");
                            if (displayPic) {
                                link = picUrl;
                            } else {
                                link = otherUrl.split("  ")[0];
                            }

                            Intent viewTweet = new Intent(context, TweetPager.class);
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
                } else {
                    //holder.image.loadImage(picUrl, false, null);
                    ImageUtils.loadImage(context, holder.image, picUrl, mCache);
                    if (holder.playButton.getVisibility() == View.VISIBLE) {
                        holder.playButton.setVisibility(View.GONE);
                    }

                    holder.image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            context.startActivity(new Intent(context, PhotoViewerDialog.class).putExtra("url", picUrl));
                        }
                    });
                }

                if (holder.image.getVisibility() == View.GONE) {
                    holder.image.setVisibility(View.VISIBLE);
                }
            }
        }

        if (type == NORMAL) {
            if (retweeter.length() > 0) {
                holder.retweeter.setText(context.getResources().getString(R.string.retweeter) + retweeter);
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

        if (settings.useEmoji && (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || EmojiUtils.ios)) {
            String text = holder.tweet.getText().toString();
            if (EmojiUtils.emojiPattern.matcher(text).find()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                        }

                        if (holder.tweetId == id) {
                            final Spannable span = EmojiUtils.getSmiledText(context, holder.tweet.getText());

                            ((Activity)context).findViewById(android.R.id.content).post(new Runnable() {
                                @Override
                                public void run() {
                                    holder.tweet.setText(span);
                                }
                            });
                        }
                    }
                }).start();
            }
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

            if (holder.expandArea.getVisibility() == View.VISIBLE && !hasKeyboard) {
                removeExpansionNoAnimation(holder);
            }

            holder.profilePic.setImageDrawable(context.getResources().getDrawable(border));
            holder.image.setVisibility(View.GONE);
        }

        bindView(v, context, statuses.get(position));

        return v;
    }

    public void removeExpansionWithAnimation(ViewHolder holder) {
        ExpansionAnimation expandAni = new ExpansionAnimation(holder.expandArea, 450);
        holder.expandArea.startAnimation(expandAni);
    }

    public void removeExpansionNoAnimation(ViewHolder holder) {

        ExpansionAnimation expandAni = new ExpansionAnimation(holder.expandArea, 10);
        holder.expandArea.startAnimation(expandAni);
    }

    public void addExpansion(final ViewHolder holder, String screenname) {

        holder.retweet.setVisibility(View.VISIBLE);
        holder.retweetCount.setVisibility(View.VISIBLE);
        holder.favCount.setVisibility(View.VISIBLE);
        holder.favorite.setVisibility(View.VISIBLE);

        holder.reply.setVisibility(View.GONE);
        holder.replyButton.setVisibility(View.GONE);

        holder.screenName = screenname;


        // used to find the other names on a tweet... could be optimized i guess, but only run when button is pressed

        String text = holder.tweet.getText().toString();
        String extraNames = "";

        if (text.contains("@")) {
            String[] split = text.split(" ");

            for (String s : split) {
                if (s.endsWith(":")) {
                    s = s.substring(0, s.length() - 1);
                }

                if (s.contains("@") && !s.contains(settings.myScreenName) && !s.contains(screenname) && s.length() > 1) {
                    extraNames += s.substring(s.indexOf("@")) + " ";
                }
            }
        }
        holder.reply.setText("@" + screenname + " " + extraNames);

        holder.reply.setSelection(holder.reply.getText().length());

        if (holder.favCount.getText().toString().length() <= 2) {
            holder.favCount.setText(" ");
            holder.retweetCount.setText(" ");
        }

        ExpansionAnimation expandAni = new ExpansionAnimation(holder.expandArea, 450);
        holder.expandArea.startAnimation(expandAni);

        if (holder.favCount.getText().toString().equals(" ")) {
            new GetFavoriteCount(holder, holder.tweetId).execute();
        }

        if (holder.retweetCount.getText().toString().equals(" ")) {
            new GetRetweetCount(holder, holder.tweetId).execute();
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

        holder.replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ReplyToStatus(holder, holder.tweetId).execute();
            }
        });

        holder.reply.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                hasKeyboard = b;
            }
        });
    }

    public void removeKeyboard(ViewHolder holder) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(holder.reply.getWindowToken(), 0);
    }

    class GetFavoriteCount extends AsyncTask<String, Void, Status> {

        private ViewHolder holder;
        private long tweetId;

        public GetFavoriteCount(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
        }

        protected twitter4j.Status doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
                if (holder.retweeter.getVisibility() != View.GONE) {
                    twitter4j.Status retweeted = twitter.showStatus(tweetId).getRetweetedStatus();
                    return retweeted;
                }
                return twitter.showStatus(tweetId);
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(twitter4j.Status status) {
            if (status != null) {
                holder.favCount.setText(" " + status.getFavoriteCount());

                if (status.isFavorited()) {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.favoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    holder.favorite.setImageDrawable(context.getResources().getDrawable(resource));
                    holder.isFavorited = true;
                } else {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.notFavoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    holder.favorite.setImageDrawable(context.getResources().getDrawable(resource));
                    holder.isFavorited = false;
                }
            }
        }
    }

    class GetRetweetCount extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public GetRetweetCount(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
                twitter4j.Status status = twitter.showStatus(tweetId);
                return "" + status.getRetweetCount();
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            if (count != null) {
                holder.retweetCount.setText(" " + count);
            }
        }
    }

    class FavoriteStatus extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public FavoriteStatus(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
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
            new GetFavoriteCount(holder, tweetId).execute();
        }
    }

    class RetweetStatus extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public RetweetStatus(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
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
            new GetRetweetCount(holder, tweetId).execute();
        }
    }

    class ReplyToStatus extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public ReplyToStatus(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);


                twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(holder.reply.getText().toString());
                reply.setInReplyToStatusId(tweetId);

                twitter.updateStatus(reply);

                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            removeExpansionWithAnimation(holder);
            removeKeyboard(holder);
        }
    }

    class GetImage extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public GetImage(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
                twitter4j.Status status = twitter.showStatus(tweetId);

                MediaEntity[] entities = status.getMediaEntities();



                return entities[0].getMediaURL();
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String url) {

        }
    }
}
