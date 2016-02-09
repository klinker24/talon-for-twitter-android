package com.klinker.android.twitter_l.manipulations;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.ui.compose.ComposeSecAccActivity;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Twitter;


public class QuickActionsPopup extends PopupLayout {

    public enum Type { RETWEET, LIKE };

    Context context;

    long tweetId;
    String screenName;
    String tweetText;

    boolean secondAccount = false;

    public QuickActionsPopup(Context context, long tweetId, String screenName, String tweetText) {
        this(context, tweetId, screenName, tweetText, false);
    }

    public QuickActionsPopup(Context context, long tweetId, String screenName, String tweetText, boolean secondAccount) {
        super(context);
        this.context = context;

        this.tweetId = tweetId;
        this.screenName = screenName;
        this.tweetText = tweetText;

        this.secondAccount = secondAccount;

        setTitle(getResources().getString(R.string.quick_actions));
        setWidth(Utils.toDP(264, context));
        setHeight(Utils.toDP(106, context));
        setAnimationScale(.5f);
    }

    View root;
    ImageButton like;
    ImageButton retweet;
    ImageButton reply;
    ImageButton quote;

    @Override
    public View setMainLayout() {
        root = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.quick_actions, null, false);

        like = (ImageButton) root.findViewById(R.id.favorite_button);
        retweet = (ImageButton) root.findViewById(R.id.retweet_button);
        reply = (ImageButton) root.findViewById(R.id.reply_button);
        quote = (ImageButton) root.findViewById(R.id.quote_button);

        like.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new Action(context, Type.LIKE, tweetId).execute();
                hide();
            }
        });

        retweet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new Action(context, Type.RETWEET, tweetId).execute();
                hide();
            }
        });

        reply.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent compose;

                if (!secondAccount) {
                    compose = new Intent(context, ComposeActivity.class);
                } else {
                    compose = new Intent(context, ComposeSecAccActivity.class);
                }

                compose.putExtra("user", "@" + screenName.replace("@", ""));
                compose.putExtra("id", tweetId);
                compose.putExtra("reply_to_text", "@" + screenName + ": " + tweetText);

                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                        view.getMeasuredWidth(), view.getMeasuredHeight());
                compose.putExtra("already_animated", true);

                context.startActivity(compose, opts.toBundle());

                hide();
            }
        });

        quote.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent compose;

                if (!secondAccount) {
                    compose = new Intent(context, ComposeActivity.class);
                } else {
                    compose = new Intent(context, ComposeSecAccActivity.class);
                }

                compose.putExtra("user", " " + "https://twitter.com/" + screenName + "/status/" + tweetId);
                compose.putExtra("id", tweetId);
                compose.putExtra("reply_to_text", "@" + screenName + ": " + tweetText);

                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                        view.getMeasuredWidth(), view.getMeasuredHeight());
                compose.putExtra("already_animated", true);

                context.startActivity(compose, opts.toBundle());

                hide();
            }
        });

        return root;
    }

    class Action extends AsyncTask<String, Void, Void> {
        private Type type;
        private Context context;
        private long tweetId;

        public Action(Context context, Type type, long tweetId) {
            this.context = context;
            this.type = type;
            this.tweetId = tweetId;
        }

        @Override
        protected Void doInBackground(String... urls) {

            Twitter twit;
            if (secondAccount) {
                twit = Utils.getSecondTwitter(context);
            } else {
                twit = Utils.getTwitter(context, AppSettings.getInstance(context));
            }

            try {
                switch (type) {
                    case LIKE:
                        twit.createFavorite(tweetId);
                        break;
                    case RETWEET:
                        twit.retweetStatus(tweetId);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        public void onPostExecute(Void nothing) {
            switch (type) {
                case LIKE:
                    Toast.makeText(context, R.string.liked_status, Toast.LENGTH_SHORT).show();
                    break;
                case RETWEET:
                    Toast.makeText(context, R.string.retweet_success, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public void show() {
        super.show();
    }
}