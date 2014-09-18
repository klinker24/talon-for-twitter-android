package com.klinker.android.twitter_l.data;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.klinker.android.launcher.api.ResourceHelper;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;
import com.klinker.android.twitter_l.utils.Utils;
import twitter4j.Status;
import twitter4j.User;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lucasklinker on 9/18/14.
 */
public class TweetView {

    Context context;
    AppSettings settings;

    Status status;

    public java.text.DateFormat dateFormatter;
    public java.text.DateFormat timeFormatter;

    // tweet data
    long tweetId;
    String name;
    String screenName;
    String profilePicUrl;
    String tweet;
    String time;
    String retweetText;
    String imageUrl;
    String otherUrl;
    String hashtags;
    String users;

    // layout components
    TextView nameTv;
    ImageView profilePicIv;
    TextView timeTv;
    TextView tweetTv;
    NetworkedCacheableImageView imageIv;
    TextView retweeterTv;
    LinearLayout backgroundLayout;
    NetworkedCacheableImageView playButton;
    TextView screenTV;

    public TweetView(Context context) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }
    }

    public TweetView(Context context, Status status) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);

        dateFormatter = android.text.format.DateFormat.getDateFormat(context);
        timeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        if (settings.militaryTime) {
            timeFormatter = new SimpleDateFormat("kk:mm");
        }

        setData(status);
    }

    public void setData(Status status) {

        if (!settings.absoluteDate) {
            time = Utils.getTimeAgo(status.getCreatedAt().getTime(), context);
        } else {
            Date date = new Date(status.getCreatedAt().getTime());
            time = timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date);
        }

        if (status.isRetweet()) {
            retweetText = context.getString(R.string.retweeter) + "@" + status.getUser().getScreenName();
            this.status = status.getRetweetedStatus();
        }

        User user = status.getUser();

        tweetId = status.getId();
        profilePicUrl = user.getOriginalProfileImageURL();
        tweet = status.getText();
        name = user.getName();
        screenName = user.getScreenName();

        String[] html = TweetLinkUtils.getLinksInStatus(status);
        imageUrl = html[1];
        otherUrl = html[2];
        hashtags = html[3];
        users = html[4];
    }

    public View getView() {
        View tweet = createTweet();
        setComponents(tweet);
        bindData();

        return tweet;
    }

    private View createTweet() {
        View tweetView = ((Activity) context).getLayoutInflater().inflate(R.layout.tweet, null, false);
        return tweetView;
    }

    private void setComponents(View v) {
        nameTv = (TextView) v.findViewById(R.id.name);
        profilePicIv = (ImageView) v.findViewById(R.id.profile_pic);
        timeTv = (TextView) v.findViewById(R.id.time);
        tweetTv = (TextView) v.findViewById(R.id.tweet);
        imageIv = (NetworkedCacheableImageView) v.findViewById(R.id.image);
        retweeterTv = (TextView) v.findViewById(R.id.retweeter);
        backgroundLayout = (LinearLayout) v.findViewById(R.id.background);
        playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button);
        screenTV = (TextView) v.findViewById(R.id.screenname);

        profilePicIv.setClipToOutline(true);
        imageIv.setClipToOutline(true);

        // sets up the font sizes
        tweetTv.setTextSize(settings.textSize);
        nameTv.setTextSize(settings.textSize + 4);
        screenTV.setTextSize(settings.textSize - 2);
        timeTv.setTextSize(settings.textSize - 3);
        retweeterTv.setTextSize(settings.textSize - 3);
    }

    private void bindData() {

    }
}
