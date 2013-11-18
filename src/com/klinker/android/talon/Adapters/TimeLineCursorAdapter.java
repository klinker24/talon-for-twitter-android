package com.klinker.android.talon.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.klinker.android.talon.ExpansionAnimation;
import com.klinker.android.talon.R;
import com.klinker.android.talon.SQLite.HomeSQLiteHelper;
import com.klinker.android.talon.UI.TweetActivity;
import com.klinker.android.talon.UI.UserProfileActivity;
import com.klinker.android.talon.Utilities.AppSettings;
import com.klinker.android.talon.Utilities.Utils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import twitter4j.DirectMessage;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.Twitter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeLineCursorAdapter extends CursorAdapter {

    public Cursor cursor;
    public Context context;
    private final LayoutInflater inflater;
    private boolean isDM = false;
    private SharedPreferences sharedPrefs;
    private int cancelButton;

    public AppSettings settings;

    private static final String REGEX = "(http|ftp|https):\\/\\/([\\w\\-_]+(?:(?:\\.[\\w\\-_]+)+))([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?";
    private static Pattern pattern = Pattern.compile(REGEX);

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
        //public Bitmap tweetPic;

        public long tweetId;
        public boolean isFavorited;
        public String screenName;

    }

    public TimeLineCursorAdapter(Context context, Cursor cursor, boolean isDM) {
        super(context, cursor, 0);

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.isDM = isDM;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.cancelButton});
        cancelButton = a.getResourceId(0, 0);
        a.recycle();

        settings = new AppSettings(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v;
        final ViewHolder holder;
        v = inflater.inflate(R.layout.tweet, viewGroup, false);

        holder = new ViewHolder();

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
        holder.image = (ImageView) v.findViewById(R.id.image);
        holder.retweeter = (TextView) v.findViewById(R.id.retweeter);
        holder.background = (LinearLayout) v.findViewById(R.id.background);

        v.setTag(holder);
        return v;
    }

    @Override
    public void bindView(final View view, Context mContext, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.tweetId = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ID));
        final String profilePic = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC));
        final String tweetText = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT));
        final String name = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME));
        final String screenname = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));
        final String picUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL));

        String retweeter;
        try {
            retweeter = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER));
        } catch (Exception e) {
            retweeter = "";
        }

        final String fRetweeter = retweeter;
        if (!isDM) {
            holder.background.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Log.v("tweet_page", "clicked");
                    Intent viewTweet = new Intent(context, TweetActivity.class);
                    viewTweet.putExtra("name", name);
                    viewTweet.putExtra("screenname", screenname);
                    viewTweet.putExtra("time", holder.time.getText().toString());
                    viewTweet.putExtra("tweet", tweetText);
                    viewTweet.putExtra("retweeter", fRetweeter);
                    viewTweet.putExtra("webpage", picUrl);
                    viewTweet.putExtra("picture", holder.image.getVisibility() == View.VISIBLE);
                    viewTweet.putExtra("tweetid", holder.tweetId);
                    viewTweet.putExtra("proPic", profilePic);

                    context.startActivity(viewTweet);

                    return true;
                }
            });
        }

        if (!isDM || (isDM  && !screenname.equals(sharedPrefs.getString("twitter_screen_name", "")))) {
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
        }

        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewProfile = new Intent(context, UserProfileActivity.class);
                viewProfile.putExtra("name", name);
                viewProfile.putExtra("screenname", screenname);
                viewProfile.putExtra("proPic", profilePic);
                viewProfile.putExtra("tweetid", holder.tweetId);
                viewProfile.putExtra("retweet", holder.retweeter.getVisibility() == View.VISIBLE);

                context.startActivity(viewProfile);
            }
        });

        holder.name.setText(name);
        holder.time.setText(Utils.getTimeAgo(cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME))));
        holder.tweet.setText(tweetText);

        Matcher matcher = pattern.matcher(tweetText);

        if (matcher.find()) {
            final RequestCreator rc = Picasso.with(context)
                    .load(picUrl)
                    .error(cancelButton);
            rc.into(holder.image);

        }

        if (retweeter.length() > 0 && !isDM) {
            holder.retweeter.setText("retweeted by @" + retweeter);
            holder.retweeter.setVisibility(View.VISIBLE);
        } else if (isDM && screenname.equals(sharedPrefs.getString("twitter_screen_name", ""))) {
            holder.retweeter.setText("reply to @" + retweeter);
            holder.retweeter.setVisibility(View.VISIBLE);
        } else if (holder.retweeter.getVisibility() == View.VISIBLE) {
            holder.retweeter.setVisibility(View.GONE);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (!cursor.moveToPosition(cursor.getCount() - 1 - position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        View v;
        if (convertView == null) {
            Log.v("listview_scrolling", "not recycled");

            v = newView(context, cursor, parent);

        } else {
            Log.v("listview_scrolling", "recycled");
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();

            if (holder.expandArea.getVisibility() == View.VISIBLE) {
                removeExpansionNoAnimation(holder);
            }

            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.profilePic.setImageDrawable(context.getResources().getDrawable(resource));
            holder.image.setVisibility(View.GONE);
        }

        bindView(v, context, cursor);

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
        if (isDM) {
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

        if (holder.name.getText().toString().contains(settings.myName)) {
            holder.reply.setVisibility(View.GONE);
            holder.replyButton.setVisibility(View.GONE);
        }

        holder.screenName = screenname;


        // used to find the other names on a tweet... could be optimized i guess, but only run when button is pressed
        if (!isDM) {
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
        }

        holder.reply.setSelection(holder.reply.getText().length());

        if (holder.favCount.getText().toString().length() <= 2) {
            holder.favCount.setText("- ");
            holder.retweetCount.setText("- ");
        }

        ExpansionAnimation expandAni = new ExpansionAnimation(holder.expandArea, 450);
        holder.expandArea.startAnimation(expandAni);

        if (holder.favCount.getText().toString().equals("- ")) {
            new GetFavoriteCount(holder, holder.tweetId).execute();
        }

        if (holder.retweetCount.getText().toString().equals("- ")) {
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
                Twitter twitter =  Utils.getTwitter(context);
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
                holder.favCount.setText("- " + status.getFavoriteCount());

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
                Twitter twitter =  Utils.getTwitter(context);
                twitter4j.Status status = twitter.showStatus(tweetId);
                return "" + status.getRetweetCount();
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            if (count != null) {
                holder.retweetCount.setText("- " + count);
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
                Twitter twitter =  Utils.getTwitter(context);
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
                Twitter twitter =  Utils.getTwitter(context);
                twitter.retweetStatus(tweetId);
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
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
                Twitter twitter =  Utils.getTwitter(context);

                if (!isDM) {
                    twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(holder.reply.getText().toString());
                    reply.setInReplyToStatusId(tweetId);

                    twitter.updateStatus(reply);
                } else {
                    String screenName = holder.screenName;
                    String message = holder.reply.getText().toString();
                    DirectMessage dm = twitter.sendDirectMessage(screenName, message);
                }

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
                Twitter twitter =  Utils.getTwitter(context);
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
