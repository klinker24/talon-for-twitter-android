package com.klinker.android.twitter_l.ui.profile_viewer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import com.jakewharton.disklrucache.Util;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ProfilePagerAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.manipulations.widgets.NotifyScrollView;
import com.klinker.android.twitter_l.services.TalonPullNotificationService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.ui.compose.ComposeDMActivity;
import com.klinker.android.twitter_l.manipulations.widgets.HoloEditText;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.klinker.android.twitter_l.utils.text.TextUtils;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.UserList;
import uk.co.senab.bitmapcache.BitmapLruCache;


public class ProfilePager extends Activity {

    private Context context;
    private AppSettings settings;
    private ActionBar actionBar;
    private BitmapLruCache mCache;
    private SharedPreferences sharedPrefs;

    private boolean isBlocking;
    private boolean isFollowing;
    private boolean isFavorite;
    private boolean isMuted;
    private boolean isRTMuted;
    private boolean isFollowingSet = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        mCache = App.getInstance(this).getBitmapCache();
        context = this;
        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        settings = AppSettings.getInstance(this);

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        setUpTheme();
        getFromIntent();
        setContentView(R.layout.user_profile);
        setUpContent();
        setUpInsets();
        getUser();
    }

    public NetworkedCacheableImageView background;
    public NetworkedCacheableImageView profilePic;
    public TextView followerCount;
    public TextView followingCount;
    public TextView description;
    public TextView location;
    public TextView website;
    public NetworkedCacheableImageView[] friends = new NetworkedCacheableImageView[3];
    public NetworkedCacheableImageView[] followers = new NetworkedCacheableImageView[3];
    public View profileCounts;

    public void setUpContent() {
        // first get all the views we need
        background = (NetworkedCacheableImageView) findViewById(R.id.background_image);
        profilePic = (NetworkedCacheableImageView) findViewById(R.id.profile_pic);

        followerCount = (TextView) findViewById(R.id.followers_number);
        followingCount = (TextView) findViewById(R.id.following_number);
        description = (TextView) findViewById(R.id.user_description);
        location = (TextView) findViewById(R.id.user_location);
        website = (TextView) findViewById(R.id.user_webpage);
        profileCounts = findViewById(R.id.profile_counts);

        friends[0] = (NetworkedCacheableImageView) findViewById(R.id.friend_1);
        friends[1] = (NetworkedCacheableImageView) findViewById(R.id.friend_2);
        friends[2] = (NetworkedCacheableImageView) findViewById(R.id.friend_3);

        followers[0] = (NetworkedCacheableImageView) findViewById(R.id.follower_1);
        followers[1] = (NetworkedCacheableImageView) findViewById(R.id.follower_2);
        followers[2] = (NetworkedCacheableImageView) findViewById(R.id.follower_3);

        for (int i = 0; i < 3; i++) {
            friends[i].setClipToOutline(true);
            followers[i].setClipToOutline(true);
        }

        // set up the margin on the profile card so it is under the action bar and status bar
        int abHeight = Utils.getActionBarHeight(context);
        int sbHeight = Utils.getStatusBarHeight(context);
        int navHeight = Utils.getNavBarHeight(context);

        CardView headerCard = (CardView) findViewById(R.id.header_card);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) headerCard.getLayoutParams();
        params.topMargin = abHeight + sbHeight + Utils.toDP(86, context);
        headerCard.setLayoutParams(params);
    }

    private int offsetSize = 0;
    public void setUpInsets() {
        final View insetsBackground = findViewById(R.id.actionbar_and_status_bar);

        ViewGroup.LayoutParams statusParams = insetsBackground.getLayoutParams();
        statusParams.height = Utils.getActionBarHeight(this) + Utils.getStatusBarHeight(this);
        insetsBackground.setLayoutParams(statusParams);
        insetsBackground.setAlpha(0);

        final int abHeight = Utils.getActionBarHeight(context);
        final int sbHeight = Utils.getStatusBarHeight(context);
        final View header = findViewById(R.id.background_image);

        View status = findViewById(R.id.status_bar);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) status.getLayoutParams();
        params.height = sbHeight;
        status.setLayoutParams(params);

        View blackStatus = findViewById(R.id.blacker_status_bar);
        RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) blackStatus.getLayoutParams();
        param.height = sbHeight;
        blackStatus.setLayoutParams(param);

        View action = findViewById(R.id.actionbar_bar);
        params = (LinearLayout.LayoutParams) action.getLayoutParams();
        params.height = abHeight;
        action.setLayoutParams(params);

        if (settings.theme == AppSettings.THEME_DARK) {
            action.setBackgroundResource(R.color.darker_primary);
            status.setBackgroundResource(R.color.darkest_primary);
        }

        insetsBackground.setAlpha(0f);
        final NotifyScrollView scroll = (NotifyScrollView) findViewById(R.id.notify_scroll_view);
        scroll.setOnScrollChangedListener(new NotifyScrollView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                if (t > offsetSize) {
                    background.setTranslationY(-1f * (t - offsetSize));
                }
                if (t < offsetSize - 5 && t > offsetSize && (oldt - t)  < 5) {
                    background.setTranslationY(0f);
                }
                final int headerHeight = header.getHeight() - abHeight;
                final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
                insetsBackground.setAlpha(ratio);
            }
        });

        if (Utils.hasNavBar(context)) {
            View v = findViewById(R.id.nav_bar_seperator);
            v.setVisibility(View.VISIBLE);
            params = (LinearLayout.LayoutParams) v.getLayoutParams();
            params.height = Utils.getNavBarHeight(context);
            v.setLayoutParams(params);
        } else {
            findViewById(R.id.nav_bar_seperator).setVisibility(View.GONE);
        }
    }

    public void setUpTheme() {

        Utils.setUpTweetTheme(context, settings);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        android:actionBar.setTitle("");
        actionBar.setIcon(null);
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
    }

    private boolean isMyProfile = false;
    private String name;
    private String screenName;
    private String proPic;
    private long tweetId;
    private boolean isRetweet;

    public void getFromIntent() {
        Intent from = getIntent();

        name = from.getStringExtra("name");
        screenName = from.getStringExtra("screenname");
        proPic = from.getStringExtra("profilePicture");
        tweetId = from.getLongExtra("tweetid", 0l);
        isRetweet = from.getBooleanExtra("retweet", false);

        if (screenName.equalsIgnoreCase(settings.myScreenName)) {
            isMyProfile = true;
        }
    }

    public void setProfileCard(User user) {
        String color = user.getProfileBackgroundColor();
        String backgroundImage = user.getProfileBannerIPadRetinaURL();

        int brightness = ImageUtils.getBrightness(color);
        Log.v("talon_profile_color", "brightness: " + brightness);

        if (brightness < 240) {
            int color1 = Color.parseColor("#" + color);
            profileCounts.setBackgroundColor(color1);

            if (brightness < 210) {
                findViewById(R.id.status_bar).setBackgroundColor(color1);
                findViewById(R.id.actionbar_bar).setBackgroundColor(color1);
            }

            if (brightness > 128) {
                followerCount.setTextColor(getResources().getColor(R.color.light_text));
                followingCount.setTextColor(getResources().getColor(R.color.light_text));
            }
        }

        if (backgroundImage != null) {
            background.loadImage(backgroundImage, true, null);
        } else {
            background.setImageDrawable(getDrawable(R.drawable.default_header_background));
        }

        profilePic.loadImage(user.getOriginalProfileImageURL(), true, null);

        String des = user.getDescription();
        String loc = user.getLocation();
        String web = user.getURL();

        if (des != null && !des.equals("")) {
            description.setText(des);
        } else {
            description.setVisibility(View.GONE);
        }
        if (loc != null && !loc.equals("")) {
            location.setText(loc);
        } else {
            location.setVisibility(View.GONE);
        }
        if (web != null && !web.equals("")) {
            website.setText(web);
        } else {
            website.setVisibility(View.GONE);
        }

        followingCount.setText(getString(R.string.following) + ": " + user.getFriendsCount());
        followerCount.setText(getString(R.string.followers) + ": " + user.getFollowersCount());

        TextUtils.linkifyText(context, description, null, true, "", false);
        TextUtils.linkifyText(context, website, null, true, "", false);

        showCard(findViewById(R.id.header_card));
    }

    private View spinner;
    private void showCard(final View v) {
        if (spinner == null) {
            spinner = findViewById(R.id.spinner);
        }
        if (spinner.getVisibility() == View.VISIBLE) {
            Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (spinner.getVisibility() != View.GONE) {
                        spinner.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            anim.setDuration(250);
            spinner.startAnimation(anim);
        }

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_card_up);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (v.getVisibility() != View.VISIBLE) {
                    v.setVisibility(View.VISIBLE);
                }

                if (offsetSize == 0) {
                    offsetSize = ((LinearLayout) findViewById(R.id.lower_card)).getHeight() + Utils.toDP(50, context);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        anim.setStartOffset(150);
        anim.setDuration(300);
        v.startAnimation(anim);
    }

    public User thisUser;

    public void getUser() {
        Thread getUser = new Thread(new Runnable() {
            @Override
            public void run() {

                Twitter twitter =  Utils.getTwitter(context, settings);

                try {
                    thisUser = twitter.showUser(screenName);
                } catch (Exception e) {
                    thisUser = null;
                }

                if (thisUser != null) {
                    try {
                        FollowersDataSource.getInstance(context).createUser(thisUser, sharedPrefs.getInt("current_account", 1));

                    } catch (Exception e) {
                        // the user already exists. don't know if this is more efficient than querying the db or not.
                    }

                    final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context,
                            MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
                    suggestions.saveRecentQuery("@" + thisUser.getScreenName(), null);
                }

                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setProfileCard(thisUser);
                    }
                });

                new GetActionBarInfo().execute();

                // start the other actions now that we are done finding the user
                getFollowers(twitter);
                getFriends(twitter);

            }
        });

        getUser.setPriority(Thread.MAX_PRIORITY);
        getUser.start();
    }

    private void getFollowers(Twitter twitter) {
        try {
            final List<User> followers = twitter.getFollowersList(thisUser.getId(), -1, 3);

            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setFollowers(followers);
                }
            });
        } catch (Exception e) {

        }
    }

    private void setFollowers(List<User> followers) {
        switch (followers.size()) {
            case 0:
                for(int i = 0; i < 3; i++)
                    this.followers[i].setVisibility(View.GONE);
                break;
            case 1:
                for(int i = 0; i < 2; i++)
                    this.followers[i].setVisibility(View.GONE);
                this.followers[2].loadImage(followers.get(0).getBiggerProfileImageURL(), false, null);
                break;
            case 2:
                for(int i = 0; i < 1; i++)
                    this.followers[i].setVisibility(View.GONE);
                this.followers[1].loadImage(followers.get(0).getBiggerProfileImageURL(), false, null);
                this.followers[2].loadImage(followers.get(1).getBiggerProfileImageURL(), false, null);
                break;
            case 3:
                this.followers[0].loadImage(followers.get(0).getBiggerProfileImageURL(), false, null);
                this.followers[1].loadImage(followers.get(1).getBiggerProfileImageURL(), false, null);
                this.followers[2].loadImage(followers.get(2).getBiggerProfileImageURL(), false, null);
                break;
        }
    }

    private void getFriends(Twitter twitter) {
        try {
            final List<User> friends = twitter.getFriendsList(thisUser.getId(), -1, 3);

            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setFriends(friends);
                }
            });
        } catch (Exception e) {

        }
    }

    private void setFriends(List<User> friends) {
        switch (friends.size()) {
            case 0:
                for(int i = 0; i < 3; i++) // 0, 1, and 2 are gone
                    this.friends[i].setVisibility(View.GONE);
                break;
            case 1:
                for(int i = 0; i < 2; i++) // 0 and 1 are gone
                    this.friends[i].setVisibility(View.GONE);
                this.friends[2].loadImage(friends.get(0).getBiggerProfileImageURL(), false, null);
                break;
            case 2:
                this.friends[0].setVisibility(View.GONE);
                this.friends[1].loadImage(friends.get(0).getBiggerProfileImageURL(), false, null);
                this.friends[2].loadImage(friends.get(1).getBiggerProfileImageURL(), false, null);
                break;
            case 3:
                this.friends[0].loadImage(friends.get(0).getBiggerProfileImageURL(), false, null);
                this.friends[1].loadImage(friends.get(1).getBiggerProfileImageURL(), false, null);
                this.friends[2].loadImage(friends.get(2).getBiggerProfileImageURL(), false, null);
                break;
        }
    }

    class GetActionBarInfo extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... urls) {
            if (isMyProfile) {
                if (thisUser != null) {
                    // put in the banner and profile pic to shared prefs
                    sharedPrefs.edit().putString("profile_pic_url_" + sharedPrefs.getInt("current_account", 1), thisUser.getOriginalProfileImageURL()).commit();
                    sharedPrefs.edit().putString("twitter_background_url_" + sharedPrefs.getInt("current_account", 1), thisUser.getProfileBannerURL()).commit();
                }
                return null;
            } else {
                try {
                    int currentAccount = sharedPrefs.getInt("current_account", 1);
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    String otherUserName = screenName;
                    Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);

                    isFollowing = friendship.isSourceFollowingTarget();
                    isBlocking = friendship.isSourceBlockingTarget();
                    isMuted = sharedPrefs.getString("muted_users", "").contains(screenName);
                    isRTMuted = sharedPrefs.getString("muted_rts", "").contains(screenName);
                    isFavorite = FavoriteUsersDataSource.getInstance(context).isFavUser(currentAccount, otherUserName);
                    isFollowingSet = true;

                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        protected void onPostExecute(Void none) {
            if (thisUser != null) {
                actionBar.setTitle(thisUser.getName());
            }
            invalidateOptionsMenu();
        }
    }

    class FollowUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                if (thisUser != null) {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    String otherUserName = thisUser.getScreenName();

                    Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);

                    boolean isFollowing = friendship.isSourceFollowingTarget();

                    if (isFollowing) {
                        twitter.destroyFriendship(otherUserName);
                        return false;
                    } else {
                        twitter.createFriendship(otherUserName);

                        FollowersDataSource.getInstance(context).createUser(thisUser, sharedPrefs.getInt("current_account", 1));

                        return true;
                    }
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Boolean created) {
            // add a toast - now following or unfollowed
            // true = followed
            // false = unfollowed
            if (created != null) {
                if (created) {
                    Toast.makeText(context, getResources().getString(R.string.followed_user), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, getResources().getString(R.string.unfollowed_user), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }

            if(settings.liveStreaming) {
                context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                context.startService(new Intent(context, TalonPullNotificationService.class));
            }

            new GetActionBarInfo().execute();
        }
    }

    class BlockUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                if (thisUser != null) {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    String otherUserName = thisUser.getScreenName();

                    Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);

                    boolean isBlocking = friendship.isSourceBlockingTarget();

                    if (isBlocking) {
                        twitter.destroyBlock(otherUserName);
                        return false;
                    } else {
                        twitter.createBlock(otherUserName);
                        return true;
                    }
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Boolean isBlocked) {
            // true = followed
            // false = unfollowed
            if (isBlocked != null) {
                if (isBlocked) {
                    Toast.makeText(context, getResources().getString(R.string.blocked_user), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, getResources().getString(R.string.unblocked_user), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }

            new GetActionBarInfo().execute();
        }
    }

    class FavoriteUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                int currentAccount = sharedPrefs.getInt("current_account", 1);
                if (thisUser != null) {
                    if (isFavorite) {
                        // destroy favorite
                        FavoriteUsersDataSource.getInstance(context).deleteUser(thisUser.getId());

                        String favs = sharedPrefs.getString("favorite_user_names_" + currentAccount, "");
                        favs = favs.replaceAll(thisUser.getScreenName() + " ", "");
                        sharedPrefs.edit().putString("favorite_user_names_" + currentAccount, favs).commit();

                        return false;

                    } else {
                        FavoriteUsersDataSource.getInstance(context).createUser(thisUser, currentAccount);

                        sharedPrefs.edit().putString("favorite_user_names_" + currentAccount, sharedPrefs.getString("favorite_user_names_" + currentAccount, "") + thisUser.getScreenName() + " ").commit();

                        return true;
                    }
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Boolean isFavorited) {
            // true = followed
            // false = unfollowed
            if (isFavorited != null) {
                if (isFavorited) {
                    Toast.makeText(context, getResources().getString(R.string.favorite_user), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, getResources().getString(R.string.unfavorite_user), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }

            new GetActionBarInfo().execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.profile_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        final int MENU_TWEET = 0;
        final int MENU_FOLLOW = 1;
        final int MENU_UNFOLLOW = 2;
        final int MENU_FAVORITE = 3;
        final int MENU_UNFAVORITE = 4;
        final int MENU_BLOCK = 5;
        final int MENU_UNBLOCK = 6;
        final int MENU_ADD_LIST = 7;
        final int MENU_DM = 8;
        final int MENU_CHANGE_PICTURE = 9;
        final int MENU_CHANGE_BANNER = 10;
        final int MENU_CHANGE_BIO = 11;
        final int MENU_MUTE = 12;
        final int MENU_UNMUTE = 13;
        final int MENU_MUTE_RT = 14;
        final int MENU_UNMUTE_RT = 15;

        if (isMyProfile) {
            menu.getItem(MENU_TWEET).setVisible(false);
            menu.getItem(MENU_FOLLOW).setVisible(false);
            menu.getItem(MENU_UNFOLLOW).setVisible(false);
            menu.getItem(MENU_BLOCK).setVisible(false);
            menu.getItem(MENU_UNBLOCK).setVisible(false);
            menu.getItem(MENU_ADD_LIST).setVisible(false);
            menu.getItem(MENU_DM).setVisible(false);
            menu.getItem(MENU_FAVORITE).setVisible(false);
            menu.getItem(MENU_UNFAVORITE).setVisible(false);
            menu.getItem(MENU_MUTE).setVisible(false);
            menu.getItem(MENU_UNMUTE).setVisible(false);
            menu.getItem(MENU_MUTE_RT).setVisible(false);
            menu.getItem(MENU_UNMUTE_RT).setVisible(false);
        } else {
            if (isFollowingSet) {
                if (isFollowing) {
                    menu.getItem(MENU_FOLLOW).setVisible(false);
                } else {
                    menu.getItem(MENU_UNFOLLOW).setVisible(false);
                }

                if (isBlocking) {
                    menu.getItem(MENU_BLOCK).setVisible(false);
                } else {
                    menu.getItem(MENU_UNBLOCK).setVisible(false);
                }

                if (isFavorite) {
                    menu.getItem(MENU_FAVORITE).setVisible(false);
                } else {
                    menu.getItem(MENU_UNFAVORITE).setVisible(false);
                }

                if (isMuted) {
                    menu.getItem(MENU_MUTE).setVisible(false);
                } else {
                    menu.getItem(MENU_UNMUTE).setVisible(false);
                }

                if (isRTMuted) {
                    menu.getItem(MENU_MUTE_RT).setVisible(false);
                } else {
                    menu.getItem(MENU_UNMUTE_RT).setVisible(false);
                }
            } else {
                menu.getItem(MENU_FOLLOW).setVisible(false);
                menu.getItem(MENU_UNFOLLOW).setVisible(false);
                menu.getItem(MENU_FAVORITE).setVisible(false);
                menu.getItem(MENU_UNFAVORITE).setVisible(false);
                menu.getItem(MENU_BLOCK).setVisible(false);
                menu.getItem(MENU_UNBLOCK).setVisible(false);
                menu.getItem(MENU_MUTE).setVisible(false);
                menu.getItem(MENU_UNMUTE).setVisible(false);
                menu.getItem(MENU_MUTE_RT).setVisible(false);
                menu.getItem(MENU_UNMUTE_RT).setVisible(false);
            }

            menu.getItem(MENU_CHANGE_BIO).setVisible(false);
            menu.getItem(MENU_CHANGE_BANNER).setVisible(false);
            menu.getItem(MENU_CHANGE_PICTURE).setVisible(false);
        }

        return true;
    }

    @Override
    public void finish() {
        super.finish();
        try {
            if (isMyProfile) {
                AppSettings.invalidate();
            }
        } catch (Exception e) {

        }
    }

    private final int SELECT_PRO_PIC = 57;
    private final int SELECT_BANNER = 58;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_follow:
                new FollowUser().execute();
                return true;

            case R.id.menu_unfollow:
                new FollowUser().execute();
                return true;

            case R.id.menu_favorite:
                new FavoriteUser().execute();
                return true;

            case R.id.menu_unfavorite:
                new FavoriteUser().execute();
                return true;

            case R.id.menu_block:
                new BlockUser().execute();
                sharedPrefs.edit().putBoolean("just_muted", true).commit();
                return true;

            case R.id.menu_unblock:
                new BlockUser().execute();
                return true;

            case R.id.menu_add_to_list:
                new GetLists().execute();
                return true;

            case R.id.menu_tweet:
                Intent compose = new Intent(context, ComposeActivity.class);
                compose.putExtra("user", "@" + screenName);
                startActivity(compose);
                return true;

            case R.id.menu_dm:
                Intent dm = new Intent(context, ComposeDMActivity.class);
                dm.putExtra("screenname", screenName);
                startActivity(dm);
                return true;

            case R.id.menu_change_picture:
                Intent photoPickerIntent = new Intent();
                photoPickerIntent.setType("image/*");
                photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                try {
                    startActivityForResult(Intent.createChooser(photoPickerIntent,
                            "Select Picture"), SELECT_PRO_PIC);
                } catch (Throwable t) {
                    // no app to preform this..? hmm, tell them that I guess
                    Toast.makeText(context, "No app available to select pictures!", Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.menu_change_banner:
                Intent bannerPickerIntent = new Intent();
                bannerPickerIntent.setType("image/*");
                bannerPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                try {
                    startActivityForResult(Intent.createChooser(bannerPickerIntent,
                            "Select Picture"), SELECT_BANNER);
                } catch (Throwable t) {
                    // no app to preform this..? hmm, tell them that I guess
                    Toast.makeText(context, "No app available to select pictures!", Toast.LENGTH_SHORT).show();
                }
                return true;

            case  R.id.menu_change_bio:
                updateProfile();
                return true;

            case R.id.menu_mute:
                String current = sharedPrefs.getString("muted_users", "");
                sharedPrefs.edit().putString("muted_users", current + screenName.replaceAll(" ", "").replaceAll("@", "") + " ").commit();
                sharedPrefs.edit().putBoolean("refresh_me", true).commit();
                sharedPrefs.edit().putBoolean("just_muted", true).commit();
                finish();
                return true;

            case R.id.menu_unmute:
                String muted = sharedPrefs.getString("muted_users", "");
                muted = muted.replace(screenName + " ", "");
                sharedPrefs.edit().putString("muted_users", muted).commit();
                sharedPrefs.edit().putBoolean("refresh_me", true).commit();
                sharedPrefs.edit().putBoolean("just_muted", true).commit();
                finish();
                return true;

            case R.id.menu_mute_rt:
                String muted_rts = sharedPrefs.getString("muted_rts", "");
                sharedPrefs.edit().putString("muted_rts", muted_rts + screenName.replaceAll(" ", "").replaceAll("@", "") + " ").commit();
                sharedPrefs.edit().putBoolean("refresh_me", true).commit();
                sharedPrefs.edit().putBoolean("just_muted", true).commit();
                finish();
                return true;

            case R.id.menu_unmute_rt:
                String curr_muted = sharedPrefs.getString("muted_rts", "");
                curr_muted = curr_muted.replace(screenName + " ", "");
                sharedPrefs.edit().putString("muted_rts", curr_muted).commit();
                sharedPrefs.edit().putBoolean("refresh_me", true).commit();
                sharedPrefs.edit().putBoolean("just_muted", true).commit();
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateProfile() {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.change_profile_info_dialog);
        dialog.setTitle(getResources().getString(R.string.change_profile_info) + ":");

        final HoloEditText name = (HoloEditText) dialog.findViewById(R.id.name);
        final HoloEditText url = (HoloEditText) dialog.findViewById(R.id.url);
        final HoloEditText location = (HoloEditText) dialog.findViewById(R.id.location);
        final HoloEditText description = (HoloEditText) dialog.findViewById(R.id.description);

        Button cancel = (Button) dialog.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button change = (Button) dialog.findViewById(R.id.change);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean ok = true;
                String nameS = null;
                String urlS = null;
                String locationS = null;
                String descriptionS = null;

                if(name.getText().length() <= 20 && ok) {
                    if (name.getText().length() > 0){
                        nameS = name.getText().toString();
                        sharedPrefs.edit().putString("twitter_users_name_" + sharedPrefs.getInt("current_account", 1), nameS).commit();
                    }
                } else {
                    ok = false;
                    Toast.makeText(context, getResources().getString(R.string.name_char_length), Toast.LENGTH_SHORT).show();
                }

                if(url.getText().length() <= 100 && ok) {
                    if (url.getText().length() > 0){
                        urlS = url.getText().toString();
                    }
                } else {
                    ok = false;
                    Toast.makeText(context, getResources().getString(R.string.url_char_length), Toast.LENGTH_SHORT).show();
                }

                if(location.getText().length() <= 30 && ok) {
                    if (location.getText().length() > 0){
                        locationS = location.getText().toString();
                    }
                } else {
                    ok = false;
                    Toast.makeText(context, getResources().getString(R.string.location_char_length), Toast.LENGTH_SHORT).show();
                }

                if(description.getText().length() <= 160 && ok) {
                    if (description.getText().length() > 0){
                        descriptionS = description.getText().toString();
                    }
                } else {
                    ok = false;
                    Toast.makeText(context, getResources().getString(R.string.description_char_length), Toast.LENGTH_SHORT).show();
                }

                if (ok) {
                    new UpdateInfo(nameS, urlS, locationS, descriptionS).execute();
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    class UpdateInfo extends AsyncTask<String, Void, Boolean> {

        String name;
        String url;
        String location;
        String description;

        public UpdateInfo(String name, String url, String location, String description) {
            this.name = name;
            this.url = url;
            this.location = location;
            this.description = description;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                twitter.updateProfile(name, url, location, description);

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean added) {
            if (added) {
                Toast.makeText(context, getResources().getString(R.string.updated_profile), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PRO_PIC:
                if(resultCode == RESULT_OK){
                    try {
                        Uri selectedImage = imageReturnedIntent.getData();

                        new UpdateProPic(selectedImage).execute();

                    } catch (Exception e) {
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                break;
            case SELECT_BANNER:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    new UpdateBanner(selectedImage).execute();
                }
        }
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


    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Log.v("talon_composing_image", "rotation: " + orientation);

        try{
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return bitmap;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(-90);
                    break;
                default:
                    return bitmap;
            }
            try {
                Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return bmRotated;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private Bitmap getBitmapToSend(Uri uri) throws FileNotFoundException, IOException {
        InputStream input = getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > 500) ? (originalSize / 500) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true;//optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;
        input = this.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);

        ExifInterface exif = new ExifInterface(IOUtils.getPath(uri, context));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        input.close();

        return rotateBitmap(bitmap, orientation);
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    class UpdateBanner extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        private Uri image = null;
        private InputStream stream = null;

        public UpdateBanner(Uri image) {
            this.image = image;
        }

        public UpdateBanner(InputStream stream) {
            this.stream = stream;
        }

        protected void onPreExecute() {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.updating_banner_pic) + "...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                if (stream == null) {
                    //create a file to write bitmap data
                    File outputDir = context.getCacheDir(); // context being the Activity pointer
                    File f = File.createTempFile("compose", "picture", outputDir);

                    Bitmap bitmap = getBitmapToSend(image);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    byte[] bitmapdata = bos.toByteArray();

                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();

                    twitter.updateProfileBanner(f);
                } else {
                    twitter.updateProfileBanner(stream);
                }

                String profileURL = thisUser.getProfileBannerURL();
                sharedPrefs.edit().putString("twitter_background_url_" + sharedPrefs.getInt("current_account", 1), profileURL).commit();

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean uploaded) {

            try {
                stream.close();
            } catch (Exception e) {

            }

            try {
                pDialog.dismiss();
            } catch (Exception e) {

            }

            if (uploaded) {
                Toast.makeText(context, getResources().getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    class UpdateProPic extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        private InputStream stream = null;
        private Uri image;

        public UpdateProPic(InputStream stream) {
            this.stream = stream;
        }

        public UpdateProPic(Uri image) {
            this.image = image;
        }

        protected void onPreExecute() {

            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.updating_pro_pic) + "...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                User user;

                if (stream != null) {
                    user = twitter.updateProfileImage(stream);
                } else {
                    //create a file to write bitmap data
                    File outputDir = context.getCacheDir(); // context being the Activity pointer
                    File f = File.createTempFile("compose", "picture", outputDir);

                    Bitmap bitmap = getBitmapToSend(image);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    byte[] bitmapdata = bos.toByteArray();

                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();

                    user = twitter.updateProfileImage(f);
                }


                String profileURL = user.getOriginalProfileImageURL();
                sharedPrefs.edit().putString("profile_pic_url_" + sharedPrefs.getInt("current_account", 1), profileURL).commit();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean uploaded) {

            try {
                stream.close();
            } catch (Exception e) {

            }

            try {
                pDialog.dismiss();
            } catch (Exception e) {

            }

            if (uploaded) {
                Toast.makeText(context, getResources().getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    class GetLists extends AsyncTask<String, Void, ResponseList<UserList>> {

        ProgressDialog pDialog;

        protected void onPreExecute() {

            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.finding_lists));
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected ResponseList<UserList> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                ResponseList<UserList> lists = twitter.getUserLists(settings.myScreenName);

                return lists;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(final ResponseList<UserList> lists) {

            if (lists != null) {
                Collections.sort(lists, new Comparator<UserList>() {
                    public int compare(UserList result1, UserList result2) {
                        return result1.getName().compareTo(result2.getName());
                    }
                });

                ArrayList<String> names = new ArrayList<String>();
                for(UserList l : lists) {
                    names.add(l.getName());
                }

                try {
                    pDialog.dismiss();

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setItems(names.toArray(new CharSequence[lists.size()]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new AddToList(lists.get(i).getId(), thisUser.getId()).execute();
                        }
                    });
                    builder.setTitle(getResources().getString(R.string.choose_list) + ":");
                    builder.create();
                    builder.show();

                } catch (Exception e) {
                    // closed the window
                }


            } else {
                Toast.makeText(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    class AddToList extends AsyncTask<String, Void, Boolean> {

        long listId;
        long userId;

        public AddToList(long listId, long userId) {
            this.listId = listId;
            this.userId = userId;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                twitter.createUserListMember(listId, userId);

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean added) {
            if (added) {
                Toast.makeText(context, getResources().getString(R.string.added_to_list), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
