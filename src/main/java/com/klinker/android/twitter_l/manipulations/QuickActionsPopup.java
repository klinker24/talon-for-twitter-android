package com.klinker.android.twitter_l.manipulations;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Twitter;


public class QuickActionsPopup extends PopupLayout {

    public enum Type { RETWEET, LIKE };

    Context context;
    long tweetId;

    public QuickActionsPopup(Context context, long tweetId) {
        super(context);
        this.context = context;
        this.tweetId = tweetId;

        setTitle(getResources().getString(R.string.quick_actions));
        setWidth(Utils.toDP(216, context));
        setHeight(Utils.toDP(90, context));
        setAnimationScale(.5f);
    }

    View root;
    ImageView like;
    ImageView retweet;
    ImageView reply;

    @Override
    public View setMainLayout() {
        root = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.quick_actions, null, false);

        like = (ImageView) root.findViewById(R.id.favorite_button);
        retweet = (ImageView) root.findViewById(R.id.retweet_button);
        reply = (ImageView) root.findViewById(R.id.reply_button);

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
            Twitter twit = Utils.getTwitter(context, AppSettings.getInstance(context));

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
                    Toast.makeText(context, R.string.favorited, Toast.LENGTH_SHORT).show();
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