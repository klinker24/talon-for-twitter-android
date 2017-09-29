package com.klinker.android.twitter_l.views.peeks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.klinker.android.peekview.PeekViewActivity;
import com.klinker.android.peekview.builder.Peek;
import com.klinker.android.peekview.builder.PeekViewOptions;
import com.klinker.android.peekview.callback.SimpleOnPeek;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.util.ColorUtils;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Relationship;
import twitter4j.Twitter;
import twitter4j.User;

public class ProfilePeek extends SimpleOnPeek {

    public static void create(Context context, View view, String screenname) {
        if (context instanceof PeekViewActivity) {
            PeekViewOptions options = new PeekViewOptions()
                    .setAbsoluteWidth(225)
                    .setAbsoluteHeight(279);

            Peek.into(R.layout.peek_profile, new ProfilePeek(screenname))
                    .with(options)
                    .applyTo((PeekViewActivity) context, view);
        }
    }

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
    private TextView followingStatus;

    private ProfilePeek(String screenName) {
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
        followingStatus = (TextView) rootView.findViewById(R.id.following_status);

        final Activity activity = (Activity) rootView.getContext();
        if (!Utils.isColorDark(AppSettings.getInstance(activity).themeColors.primaryColorDark)) {
            int color = rootView.getResources().getColor(R.color.light_text);
            location.setTextColor(color);
            description.setTextColor(color);
            followerCount.setTextColor(color);
            friendCount.setTextColor(color);
            tweetCount.setTextColor(color);
            followingStatus.setTextColor(color);

            ((TextView) rootView.findViewById(R.id.tweets_label)).setTextColor(color);
            ((TextView) rootView.findViewById(R.id.followers_label)).setTextColor(color);
            ((TextView) rootView.findViewById(R.id.following_label)).setTextColor(color);
        }

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Twitter twitter = Utils.getTwitter(activity, AppSettings.getInstance(activity));
                    final User user = twitter.showUser(profileScreenName);

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

                    final Relationship friendship = twitter.showFriendship(AppSettings.getInstance(activity).myScreenName, profileScreenName);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (friendship.isTargetFollowingSource()) {
                                followingStatus.setText(activity.getString(R.string.follows_you));
                            } else {
                                followingStatus.setText(activity.getString(R.string.not_following_you));
                            }
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }
}
