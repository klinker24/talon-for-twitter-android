package com.klinker.android.talon.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.ArrayListLoader;
import com.klinker.android.talon.adapters.PeopleArrayAdapter;
import com.klinker.android.talon.adapters.TimelineArrayAdapter;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.utils.Utils;
import com.squareup.picasso.Picasso;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.*;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends Activity {

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
    private boolean isFollowingSet = false;

    private ItemManager.Builder builder;

    private long currentFollowers = -1;
    private long currentFollowing = -1;
    private int refreshes = 0;
    private ArrayList<User> friends;
    private ArrayList<User> following;
    private boolean canRefresh = true;

    private NetworkedCacheableImageView background;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        settings = new AppSettings(context);

        if (settings.advanceWindowed) {
            setUpWindow();
        }

        setUpTheme();
        getFromIntent();

        inflater = LayoutInflater.from(context);

        setContentView(R.layout.main_fragments);

        AsyncListView listView = (AsyncListView) findViewById(R.id.listView);
        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        final View header = inflater.inflate(R.layout.user_profile_header, null);

        listView.addHeaderView(header);
        listView.setAdapter(new TimelineArrayAdapter(context, new ArrayList<Status>(0)));

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
        actionBar.setTitle(name);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        final ImageView profilePic = (ImageView) findViewById(R.id.profile_pic);

        tweetsBtn = (Button) findViewById(R.id.tweets);
        followersBtn = (Button) findViewById(R.id.followers);
        followingBtn = (Button) findViewById(R.id.following);

        if(!proPic.equals("")) {
            Picasso.with(context)
                    .load(proPic)
                    .transform(new CircleTransform())
                    .into(profilePic);
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

        background = (NetworkedCacheableImageView) findViewById(R.id.background_image);
        final TextView statement = (TextView) findViewById(R.id.user_statement);
        final TextView screenname = (TextView) findViewById(R.id.username);
        final AsyncListView listView = (AsyncListView) findViewById(R.id.listView);

        statement.setTextSize(settings.textSize);
        screenname.setTextSize(settings.textSize);

        //new GetData(tweetId, numTweets, numFollowers, numFollowing, statement, listView, background).execute();
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

                    listView.setAdapter(new TimelineArrayAdapter(context, new ArrayList<Status>(0)));

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
            }
        });
    }

    class GetData extends AsyncTask<String, Void, User> {

        private long tweetId;
        private TextView numTweets;
        private TextView numFollowers;
        private TextView numFollowing;
        private AsyncListView listView;
        private TextView statement;

        public GetData(long tweetId, TextView numTweets, TextView numFollowers, TextView numFollowing, TextView statement, AsyncListView listView) {
            this.tweetId = tweetId;
            this.numFollowers = numFollowers;
            this.numFollowing = numFollowing;
            this.numTweets = numTweets;
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

                background.loadImage(user.getProfileBannerURL(), false, null);

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
                Twitter twitter =  Utils.getTwitter(context);

                String otherUserName = thisUser.getScreenName();
                Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);

                isFollowing = friendship.isSourceFollowingTarget();
                isBlocking = friendship.isSourceBlockingTarget();
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
        private AsyncListView listView1;
        private boolean shouldIncrement;

        public GetFollowers(User user, AsyncListView listView, boolean inc) {
            this.user = user;
            this.listView1 = listView;
            this.shouldIncrement = inc;
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView1.setItemManager(null);
                        listView1.setAdapter(people);

                        if (shouldIncrement) {
                            refreshes++;
                        }
                        
                        listView1.smoothScrollToPosition(refreshes * 20);

                    }
                });
            }

            String url = thisUser.getProfileBannerMobileURL();
            background.loadImage(url == null ? "" : url, false, null);
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

        protected ArrayList<twitter4j.User> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                PagableResponseList<User> friendsPaging = twitter.getFriendsList(user.getId(), currentFollowing);

                for (int i = 0; i < friendsPaging.size(); i++) {
                    following.add(friendsPaging.get(i));
                    Log.v("friends_list", friendsPaging.get(i).getName());
                }

                currentFollowers = friendsPaging.getNextCursor();

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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setItemManager(null);
                        listView.setAdapter(people);

                        if (shouldIncrement) {
                            refreshes++;
                        }

                        listView.smoothScrollToPosition(refreshes * 20);

                    }
                });
            }

            String url = thisUser.getProfileBannerMobileURL();
            background.loadImage(url == null ? "" : url, false, null);
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
                final TimelineArrayAdapter adapter = new TimelineArrayAdapter(context, statuses, screenName);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setItemManager(builder.build());
                        listView.setAdapter(adapter);
                    }
                });
            }

            String url = thisUser.getProfileBannerMobileURL();
            background.loadImage(url == null ? "" : url, false, null);
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
        final int MENU_BLOCK = 3;
        final int MENU_UNBLOCK = 4;
        final int MENU_ADD_LIST = 5;
        final int MENU_DM = 6;
        final int MENU_CHANGE_PICTURE = 7;
        final int MENU_CHANGE_BIO = 8;

        if (isMyProfile) {
            menu.getItem(MENU_TWEET).setVisible(false);
            menu.getItem(MENU_FOLLOW).setVisible(false);
            menu.getItem(MENU_UNFOLLOW).setVisible(false);
            menu.getItem(MENU_BLOCK).setVisible(false);
            menu.getItem(MENU_UNBLOCK).setVisible(false);
            menu.getItem(MENU_ADD_LIST).setVisible(false);
            menu.getItem(MENU_DM).setVisible(false);
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
            } else {
                menu.getItem(MENU_FOLLOW).setVisible(false);
                menu.getItem(MENU_UNFOLLOW).setVisible(false);
                menu.getItem(MENU_BLOCK).setVisible(false);
                menu.getItem(MENU_UNBLOCK).setVisible(false);
            }

            menu.getItem(MENU_CHANGE_BIO).setVisible(false);
            menu.getItem(MENU_CHANGE_PICTURE).setVisible(false);
        }

        // todo - take this out when they get added
        menu.getItem(MENU_CHANGE_BIO).setVisible(false);
        menu.getItem(MENU_CHANGE_PICTURE).setVisible(false);

        return true;
    }

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

            case R.id.menu_block:
                new BlockUser().execute();
                return true;

            case R.id.menu_unblock:
                new BlockUser().execute();
                return true;

            case R.id.menu_add_to_list:
                //TODO - get the lists working here
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
                // todo - be able to select and upload a picture
                return true;

            case  R.id.menu_change_bio:
                // TODO - open dialog to change the bio text
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
