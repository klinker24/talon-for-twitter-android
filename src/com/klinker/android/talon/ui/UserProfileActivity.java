package com.klinker.android.talon.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.ArrayListLoader;
import com.klinker.android.talon.adapters.PeopleArrayAdapter;
import com.klinker.android.talon.adapters.ProfilesArrayAdapter;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.talon.sq_lite.FollowersDataSource;
import com.klinker.android.talon.ui.widgets.HoloEditText;
import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.utils.IOUtils;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.UserList;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class UserProfileActivity extends Activity {

    private SharedPreferences sharedPrefs;

    private static final int BTN_TWEET = 0;
    private static final int BTN_FOLLOWERS = 1;
    private static final int BTN_FOLLOWING = 2;

    private int current = BTN_TWEET;

    private Context context;
    private AppSettings settings;
    private ActionBar actionBar;

    private User thisUser;

    private Button tweetsBtn;
    private Button followersBtn;
    private Button followingBtn;

    private String name;
    private String screenName;
    private String proPic;
    private long tweetId;
    private boolean isRetweet;
    private LayoutInflater inflater;

    private boolean isBlocking;
    private boolean isFollowing;
    private boolean isFavorite;
    private boolean isFollowingSet = false;

    private ItemManager.Builder builder;

    private long currentFollowers = -1;
    private long currentFollowing = -1;
    private int refreshes = 0;
    private ArrayList<User> friends;
    private ArrayList<User> following;
    private boolean canRefresh = true;

    private NetworkedCacheableImageView background;
    private ProgressBar spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        settings = new AppSettings(context);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if ((settings.advanceWindowed && !getIntent().getBooleanExtra("long_click", false)) ||
                !settings.advanceWindowed && getIntent().getBooleanExtra("long_click", false)) {
            setUpWindow();
        }

        setUpTheme();
        getFromIntent();

        inflater = LayoutInflater.from(context);

        setContentView(R.layout.profiles_list);

        AsyncListView listView = (AsyncListView) findViewById(R.id.listView);
        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        final View header = inflater.inflate(R.layout.user_profile_header, null);

        listView.addHeaderView(header);
        listView.setAdapter(new ProfilesArrayAdapter(context, new ArrayList<Status>(0)));

        friends = new ArrayList<User>();
        following = new ArrayList<User>();

        setUpUI();
    }

    public void setUpTheme() {

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight_Popup);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark_Popup);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack_Popup);
                break;
        }

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }

    public void setUpWindow() {

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .75f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .8));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

    }

    private boolean isMyProfile = false;

    public void getFromIntent() {
        Intent from = getIntent();

        name = from.getStringExtra("name");
        screenName = from.getStringExtra("screenname");
        proPic = from.getStringExtra("proPic");
        tweetId = from.getLongExtra("tweetid", 0);
        isRetweet = from.getBooleanExtra("retweet", false);

        if (screenName.equals(settings.myScreenName)) {
            isMyProfile = true;
        }
    }

    public void setUpUI() {
        spinner = (ProgressBar) findViewById(R.id.progress_bar);
        spinner.setVisibility(View.VISIBLE);

        actionBar.setTitle(name);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        tweetsBtn = (Button) findViewById(R.id.tweets);
        followersBtn = (Button) findViewById(R.id.followers);
        followingBtn = (Button) findViewById(R.id.following);

        background = (NetworkedCacheableImageView) findViewById(R.id.background_image);
        final TextView statement = (TextView) findViewById(R.id.user_statement);
        final TextView screenname = (TextView) findViewById(R.id.username);
        final AsyncListView listView = (AsyncListView) findViewById(R.id.listView);
        //final RelativeLayout header = (RelativeLayout) findViewById(R.id.header);
        final NetworkedCacheableImageView header = (NetworkedCacheableImageView) findViewById(R.id.background_image);

        statement.setTextSize(settings.textSize);
        screenname.setTextSize(settings.textSize);

        new GetData(tweetId, null, null, null, statement, listView).execute();

        screenname.setText("@" + screenName);

        tweetsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (current != BTN_TWEET) {
                    current = BTN_TWEET;
                    currentFollowing = -1;
                    currentFollowers = -1;
                    refreshes = 0;

                    listView.setAdapter(new ProfilesArrayAdapter(context, new ArrayList<Status>(0)));

                    new GetTimeline(thisUser, listView).execute();
                }
            }
        });

        followersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (current != BTN_FOLLOWERS) {
                    current = BTN_FOLLOWERS;
                    currentFollowers = -1;
                    friends = new ArrayList<User>();
                    refreshes = 0;

                    listView.setItemManager(null);
                    listView.setAdapter(new PeopleArrayAdapter(context, friends));

                    new GetFollowers(thisUser, listView, false).execute();
                }
            }
        });

        followingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (current != BTN_FOLLOWING) {
                    current = BTN_FOLLOWING;
                    currentFollowing = -1;
                    following = new ArrayList<User>();
                    refreshes = 0;

                    listView.setItemManager(null);
                    listView.setAdapter(new PeopleArrayAdapter(context, following));

                    new GetFollowing(thisUser, listView, false).execute();
                }
            }
        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;
                if(lastItem == totalItemCount) {
                    // Last item is fully visible.
                    if (current == BTN_FOLLOWING && canRefresh) {
                        new GetFollowing(thisUser, listView, true).execute();
                    } else if (current == BTN_FOLLOWERS && canRefresh) {
                        new GetFollowers(thisUser, listView, true).execute();
                    }

                    canRefresh = false;

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            canRefresh = true;
                        }
                    }, 4000);
                }

                if(visibleItemCount == 0) return;
                if(firstVisibleItem != 0) return;

                header.setTranslationY(-listView.getChildAt(0).getTop() / 2);
            }
        });
    }

    class GetData extends AsyncTask<String, Void, User> {
        private AsyncListView listView;
        private TextView statement;

        public GetData(long tweetId, TextView numTweets, TextView numFollowers, TextView numFollowing, TextView statement, AsyncListView listView) {

            this.listView = listView;
            this.statement = statement;
        }

        protected twitter4j.User doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                if (!isMyProfile) {
                    User user = twitter.showUser(screenName);

                    return user;
                } else {
                    return twitter.showUser(settings.myScreenName);
                }
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(twitter4j.User user) {
            if (user != null) {

                thisUser = user;

                new GetTimeline(user, listView).execute();
                new GetActionBarInfo(user).execute();

                String state = user.getDescription();
                if (state.equals("")) {
                    statement.setText(getResources().getString(R.string.no_description));
                } else {
                    statement.setText(state);
                }

                tweetsBtn.setText(getResources().getString(R.string.tweets) + "\n" + "(" + thisUser.getStatusesCount() + ")");
                followersBtn.setText(getResources().getString(R.string.followers) + "\n" + "(" + thisUser.getFollowersCount() + ")");
                followingBtn.setText(getResources().getString(R.string.following) + "\n" + "(" + thisUser.getFriendsCount() + ")");
            }
        }
    }


    class GetActionBarInfo extends AsyncTask<String, Void, Void> {

        private User user;
        public GetActionBarInfo(User user) {
            this.user = user;
        }

        protected Void doInBackground(String... urls) {
            try {

                int currentAccount = sharedPrefs.getInt("current_account", 1);
                Twitter twitter =  Utils.getTwitter(context);

                String otherUserName = thisUser.getScreenName();
                Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);

                isFollowing = friendship.isSourceFollowingTarget();
                isBlocking = friendship.isSourceBlockingTarget();
                isFavorite = sharedPrefs.getString("favorite_user_names_" + currentAccount, "").contains(otherUserName);
                isFollowingSet = true;

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Void none) {
            invalidateOptionsMenu();
        }
    }

    class GetFollowers extends AsyncTask<String, Void, ArrayList<twitter4j.User>> {

        private User user;
        private AsyncListView listView;
        private boolean shouldIncrement;

        public GetFollowers(User user, AsyncListView listView, boolean inc) {
            this.user = user;
            this.listView = listView;
            this.shouldIncrement = inc;
        }

        protected void onPreExecute() {
            spinner.setVisibility(View.VISIBLE);
        }

        protected ArrayList<twitter4j.User> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                PagableResponseList<User> friendsPaging = twitter.getFollowersList(user.getId(), currentFollowers);

                for (int i = 0; i < friendsPaging.size(); i++) {
                    friends.add(friendsPaging.get(i));
                    Log.v("friends_list", friendsPaging.get(i).getName());
                }

                currentFollowers = friendsPaging.getNextCursor();

                Log.v("friends_list", friends.size() + "");

                return friends;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<twitter4j.User> users) {
            if (users != null) {
                final PeopleArrayAdapter people = new PeopleArrayAdapter(context, users);
                final int firstVisible = listView.getFirstVisiblePosition();
                listView.setItemManager(null);

                if (shouldIncrement) {
                    listView.setAdapter(people);
                    refreshes++;
                } else {
                    listView.setAdapter(people);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        listView.setSelection(firstVisible);
                    }
                }, 100);

                listView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                    @Override
                    public boolean onPreDraw() {
                        if(listView.getFirstVisiblePosition() == firstVisible) {
                            listView.getViewTreeObserver().removeOnPreDrawListener(this);
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                });
            }

            final NetworkedCacheableImageView profilePic = (NetworkedCacheableImageView) findViewById(R.id.profile_pic);

            profilePic.loadImage(thisUser.getOriginalProfileImageURL(), true, null, NetworkedCacheableImageView.CIRCLE);

            String url = user.getProfileBannerRetinaURL();
            if (url != null) {
                background.loadImage(url, false, null);
            }

            spinner.setVisibility(View.GONE);
        }
    }

    class GetFollowing extends AsyncTask<String, Void, ArrayList<twitter4j.User>> {

        private User user;
        private AsyncListView listView;
        private boolean shouldIncrement;

        public GetFollowing(User user, AsyncListView listViews, boolean inc) {
            this.user = user;
            this.listView = listViews;
            this.shouldIncrement = inc;
        }

        protected void onPreExecute() {
            spinner.setVisibility(View.VISIBLE);
        }

        protected ArrayList<twitter4j.User> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                PagableResponseList<User> friendsPaging = twitter.getFriendsList(user.getId(), currentFollowing);

                for (int i = 0; i < friendsPaging.size(); i++) {
                    following.add(friendsPaging.get(i));
                    Log.v("friends_list", friendsPaging.get(i).getName());
                }

                currentFollowing = friendsPaging.getNextCursor();

                Log.v("friends_list", friends.size() + "");

                return following;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<twitter4j.User> users) {
            if (users != null) {
                final PeopleArrayAdapter people = new PeopleArrayAdapter(context, users);
                final int firstVisible = listView.getFirstVisiblePosition();
                listView.setItemManager(null);

                if (shouldIncrement) {
                    listView.setAdapter(people);
                    refreshes++;
                } else {
                    listView.setAdapter(people);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        listView.setSelection(firstVisible);
                    }
                }, 100);

                listView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                    @Override
                    public boolean onPreDraw() {
                        if(listView.getFirstVisiblePosition() == firstVisible) {
                            listView.getViewTreeObserver().removeOnPreDrawListener(this);
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                });

            }

            final NetworkedCacheableImageView profilePic = (NetworkedCacheableImageView) findViewById(R.id.profile_pic);

            profilePic.loadImage(thisUser.getBiggerProfileImageURL(), true, null, NetworkedCacheableImageView.CIRCLE);

            String url = user.getProfileBannerURL();
            if (url != null) {
                background.loadImage(user.getProfileBannerURL(), false, null);
            }

            spinner.setVisibility(View.GONE);
        }
    }

    class GetTimeline extends AsyncTask<String, Void, ArrayList<twitter4j.Status>> {

        private User user;
        private AsyncListView listView;
        private TextView statement;

        public GetTimeline(User user, AsyncListView listView) {
            this.user = user;
            this.listView = listView;
        }

        protected void onPreExecute() {
            spinner.setVisibility(View.VISIBLE);
        }

        protected ArrayList<twitter4j.Status> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                List<twitter4j.Status> statuses = twitter.getUserTimeline(user.getId(), new Paging(1, 100));

                ArrayList<twitter4j.Status> all = new ArrayList<twitter4j.Status>();

                for (twitter4j.Status s : statuses) {
                    all.add(s);
                }

                return all;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<twitter4j.Status> statuses) {
            if (statuses != null) {
                final ProfilesArrayAdapter adapter = new ProfilesArrayAdapter(context, statuses, screenName);
                listView.setItemManager(builder.build());
                listView.setAdapter(adapter);
            }

            final NetworkedCacheableImageView profilePic = (NetworkedCacheableImageView) findViewById(R.id.profile_pic);

            profilePic.loadImage(thisUser.getBiggerProfileImageURL(), true, null, NetworkedCacheableImageView.CIRCLE);

            String url = user.getProfileBannerURL();
            if (url != null) {
                background.loadImage(user.getProfileBannerURL(), false, null);
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            actionBar.setIcon(profilePic.getDrawable());
                        }
                    });
                }
            }, 1000);

            actionBar.setTitle(thisUser.getName());

            spinner.setVisibility(View.GONE);
        }
    }

    class FollowUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                if (thisUser != null) {
                    Twitter twitter =  Utils.getTwitter(context);

                    String otherUserName = thisUser.getScreenName();

                    Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);

                    boolean isFollowing = friendship.isSourceFollowingTarget();

                    if (isFollowing) {
                        twitter.destroyFriendship(otherUserName);
                        return false;
                    } else {
                        twitter.createFriendship(otherUserName);

                        FollowersDataSource data = new FollowersDataSource(context);
                        data.open();
                        data.createUser(thisUser, sharedPrefs.getInt("current_account", 1));
                        data.close();

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

            new GetActionBarInfo(thisUser).execute();
        }
    }

    class BlockUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                if (thisUser != null) {
                    Twitter twitter =  Utils.getTwitter(context);

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

            new GetActionBarInfo(thisUser).execute();
        }
    }

    class FavoriteUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                int currentAccount = sharedPrefs.getInt("current_account", 1);
                if (thisUser != null) {
                    if (isFavorite) {
                        // destroy favorite
                        FavoriteUsersDataSource data = new FavoriteUsersDataSource(context);
                        data.open();
                        data.deleteUser(thisUser.getId());
                        data.close();

                        String favs = sharedPrefs.getString("favorite_user_names_" + currentAccount, "");
                        favs = favs.replaceAll(thisUser.getScreenName() + " ", "");
                        sharedPrefs.edit().putString("favorite_user_names_" + currentAccount, favs).commit();

                        return false;

                    } else {
                        FavoriteUsersDataSource data = new FavoriteUsersDataSource(context);
                        data.open();
                        data.createUser(thisUser, currentAccount);
                        data.close();

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

            new GetActionBarInfo(thisUser).execute();
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
            } else {
                menu.getItem(MENU_FOLLOW).setVisible(false);
                menu.getItem(MENU_UNFOLLOW).setVisible(false);
                menu.getItem(MENU_FAVORITE).setVisible(false);
                menu.getItem(MENU_UNFAVORITE).setVisible(false);
                menu.getItem(MENU_BLOCK).setVisible(false);
                menu.getItem(MENU_UNBLOCK).setVisible(false);
            }

            menu.getItem(MENU_CHANGE_BIO).setVisible(false);
            menu.getItem(MENU_CHANGE_BANNER).setVisible(false);
            menu.getItem(MENU_CHANGE_PICTURE).setVisible(false);
        }

        return true;
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
                Intent proPickerIntent = new Intent(Intent.ACTION_PICK);
                proPickerIntent.setType("image/*");
                startActivityForResult(proPickerIntent, SELECT_PRO_PIC);
                return true;

            case R.id.menu_change_banner:
                Intent bannerPickerIntent = new Intent(Intent.ACTION_PICK);
                bannerPickerIntent.setType("image/*");
                startActivityForResult(bannerPickerIntent, SELECT_BANNER);
                return true;

            case  R.id.menu_change_bio:
                updateProfile();
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
                Twitter twitter =  Utils.getTwitter(context);

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
                    Uri selectedImage = imageReturnedIntent.getData();
                    String filePath = IOUtils.getPath(selectedImage, context);

                    Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

                    String root = Environment.getExternalStorageDirectory().toString();
                    File myDir = new File(root + "/Talon");
                    myDir.mkdirs();

                    File file = new File(myDir, "profile.jpg");
                    if (file.exists()) file.delete();
                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        yourSelectedImage.compress(Bitmap.CompressFormat.JPEG, 35, out);
                        out.flush();
                        out.close();

                        new UpdateProPic(file).execute();

                    } catch (Exception e) {
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                break;
            case SELECT_BANNER:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String filePath = IOUtils.getPath(selectedImage, context);

                    Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

                    String root = Environment.getExternalStorageDirectory().toString();
                    File myDir = new File(root + "/Talon");
                    myDir.mkdirs();

                    File file = new File(myDir, "banner.jpg");
                    if (file.exists()) file.delete();
                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        yourSelectedImage.compress(Bitmap.CompressFormat.JPEG, 40, out);
                        out.flush();
                        out.close();

                        new UpdateBanner(file).execute();

                    } catch (Exception e) {
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
        }
    }

    class UpdateBanner extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        private File out;

        public UpdateBanner(File out) {
            this.out = out;
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
                Twitter twitter =  Utils.getTwitter(context);

                twitter.updateProfileBanner(out);

                String profileURL = thisUser.getProfileBannerURL();
                sharedPrefs.edit().putString("twitter_background_url_" + sharedPrefs.getInt("current_account", 1), profileURL).commit();

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean uploaded) {

            pDialog.dismiss();

            if (uploaded) {
                Toast.makeText(context, getResources().getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    class UpdateProPic extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        private File out;

        public UpdateProPic(File out) {
            this.out = out;
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
                Twitter twitter =  Utils.getTwitter(context);

                User user = twitter.updateProfileImage(out);
                String profileURL = user.getBiggerProfileImageURL();
                sharedPrefs.edit().putString("profile_pic_url_" + sharedPrefs.getInt("current_account", 1), profileURL).commit();

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean uploaded) {

            pDialog.dismiss();

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
                Twitter twitter =  Utils.getTwitter(context);

                ResponseList<UserList> lists = twitter.getUserLists(settings.myScreenName);

                return lists;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(final ResponseList<UserList> lists) {

            ArrayList<String> names = new ArrayList<String>();
            for(UserList l : lists) {
                names.add(l.getName());
            }

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
        }
    }

    class AddToList extends AsyncTask<String, Void, Boolean> {

        int listId;
        long userId;

        public AddToList(int listId, long userId) {
            this.listId = listId;
            this.userId = userId;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

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
