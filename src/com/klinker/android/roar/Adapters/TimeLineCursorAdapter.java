package com.klinker.android.roar.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.klinker.android.roar.ExpansionAnimation;
import com.klinker.android.roar.R;
import com.klinker.android.roar.SQLite.HomeSQLiteHelper;
import com.klinker.android.roar.Utilities.Utils;
import twitter4j.Status;
import twitter4j.Twitter;

public class TimeLineCursorAdapter extends SimpleCursorAdapter {

    public Cursor cursor;
    public Context context;
    private final LayoutInflater inflater;

    public boolean showMore = false;

    public static class ViewHolder {
        public TextView name;
        public ImageView profilePic;
        public TextView tweet;
        public TextView time;
        public ImageButton expand;
        public EditText reply;
        public ImageButton favorite;
        public ImageButton retweet;
        public TextView favCount;
        public TextView retweetCount;
        public LinearLayout expandArea;
        public ImageButton replyButton;
        //public Bitmap tweetPic;

        public int position;
        public long tweetId;
        public boolean isFavorited;

    }

    public TimeLineCursorAdapter(Context context, Cursor cursor) {
        super(context, -1, null, new String[]{}, new int[]{}, 0);

        this.cursor = cursor;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (!cursor.moveToPosition(cursor.getCount() - 1 - position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        View v;
        final ViewHolder holder;
        if (convertView == null) {
            Log.v("listview_scrolling", "not recycled");
            v = inflater.inflate(R.layout.tweet, parent, false);

            holder = new ViewHolder();

            holder.name = (TextView) v.findViewById(R.id.name);
            holder.profilePic = (ImageView) v.findViewById(R.id.profile_pic);
            holder.time = (TextView) v.findViewById(R.id.time);
            holder.tweet = (TextView) v.findViewById(R.id.tweet);
            holder.expand = (ImageButton) v.findViewById(R.id.show_more);
            holder.reply = (EditText) v.findViewById(R.id.reply);
            holder.favorite = (ImageButton) v.findViewById(R.id.favorite);
            holder.retweet = (ImageButton) v.findViewById(R.id.retweet);
            holder.favCount = (TextView) v.findViewById(R.id.fav_count);
            holder.retweetCount = (TextView) v.findViewById(R.id.retweet_count);
            holder.expandArea = (LinearLayout) v.findViewById(R.id.expansion);
            holder.replyButton = (ImageButton) v.findViewById(R.id.reply_button);

            v.setTag(holder);
        } else {
            Log.v("listview_scrolling", "recycled");
            v = convertView;

            holder = (ViewHolder) v.getTag();

            if (!showMore) {
                removeExpansionNoAnimation(holder);
            }
        }

        holder.position = position;
        holder.tweetId = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_ID));

        String tweetText = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT));
        String name = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME));
        long date = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME));
        String screenname = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));

        holder.name.setText(name);
        holder.time.setText(Utils.getTimeAgo(date));
        holder.tweet.setText(tweetText);
        holder.reply.setText("@" + screenname + " ");
        holder.reply.setSelection(holder.reply.getText().length());

        if (holder.favCount.getText().toString().length() <= 2) {
            holder.favCount.setText("- ");
            holder.retweetCount.setText("- ");
        }

        holder.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.expandArea.getVisibility() == View.GONE) {
                    addExpansion(holder);
                    showMore = true;
                } else {
                    removeExpansionWithAnimation(holder);
                    showMore = false;
                    removeKeyboard(holder);
                }
            }
        });

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

        return v;
    }

    public void removeExpansionWithAnimation(ViewHolder holder) {
        ExpansionAnimation expandAni = new ExpansionAnimation(holder.expandArea, 250);
        holder.expandArea.startAnimation(expandAni);
    }

    public void removeExpansionNoAnimation(ViewHolder holder) {
        holder.expandArea.setVisibility(View.GONE);
    }

    public void addExpansion(ViewHolder holder) {
        ExpansionAnimation expandAni = new ExpansionAnimation(holder.expandArea, 250);
        holder.expandArea.startAnimation(expandAni);

        if (holder.favCount.getText().toString().equals("- ")) {
            new GetFavoriteCount(holder, holder.tweetId).execute();
        }

        if (holder.retweetCount.getText().toString().equals("- ")) {
            new GetRetweetCount(holder, holder.tweetId).execute();
        }
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
                return twitter.showStatus(tweetId);
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(twitter4j.Status status) {
            if (status != null) {
                holder.favCount.setText("- " + status.getFavoriteCount());

                if (status.isFavorited()) {
                    holder.favorite.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_important));
                    holder.isFavorited = true;
                } else {
                    holder.favorite.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_not_important));
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
}
