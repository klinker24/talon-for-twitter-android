package com.klinker.android.twitter_l.manipulations;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.klinker.android.peekview.callback.SimpleOnPeek;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.User;

public class ProfilePeek extends SimpleOnPeek {

    private String profileScreenName;

    private ImageView profilePicture;
    private ImageView bannerImage;
    private ImageView verified;
    private TextView realName;
    private TextView screenName;
    private TextView description;
    private TextView location;
    private TextView followerCount;
    private TextView friendCount;
    private TextView tweetCount;

    public ProfilePeek(String screenName) {
        this.profileScreenName = screenName;
    }

    @Override
    public void onInflated(View rootView) {
        profilePicture = (ImageView) rootView.findViewById(R.id.profile_pic);
        bannerImage = (ImageView) rootView.findViewById(R.id.banner);
        verified = (ImageView) rootView.findViewById(R.id.verified);
        realName = (TextView) rootView.findViewById(R.id.real_name);
        screenName = (TextView) rootView.findViewById(R.id.screen_name);
        location = (TextView) rootView.findViewById(R.id.location);
        description = (TextView) rootView.findViewById(R.id.description);
        followerCount = (TextView) rootView.findViewById(R.id.followers_count);
        friendCount = (TextView) rootView.findViewById(R.id.following_count);
        tweetCount = (TextView) rootView.findViewById(R.id.tweet_count);

        final Activity activity = (Activity) rootView.getContext();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final User user = Utils.getTwitter(activity, AppSettings.getInstance(activity)).showUser(profileScreenName);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(activity).load(user.getOriginalProfileImageURL()).into(profilePicture);
                            Glide.with(activity).load(user.getProfileBannerURL()).into(bannerImage);

                            realName.setText(user.getName());
                            screenName.setText("@" + user.getScreenName());

                            location.setText(user.getLocation());
                            description.setText(user.getDescription());

                            followerCount.setText(
                                    user.getFollowersCount() < 1000 ?
                                            "" + user.getFollowersCount() :
                                            Utils.coolFormat(user.getFollowersCount(), 0));

                            friendCount.setText(
                                    user.getFriendsCount() < 1000 ?
                                            "" + user.getFriendsCount() :
                                            Utils.coolFormat(user.getFriendsCount(), 0));

                            tweetCount.setText(
                                    user.getStatusesCount() < 1000 ?
                                            "" + user.getStatusesCount() :
                                            Utils.coolFormat(user.getStatusesCount(), 0));

                            if (user.getLocation().isEmpty()) {
                                location.setVisibility(View.GONE);
                            }

                            if (user.isVerified()) {
                                verified.setVisibility(View.VISIBLE);
                                verified.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
                            }
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }
}
