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
import android.view.*;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.ArrayListLoader;
import com.klinker.android.talon.adapters.TimelineArrayAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.utilities.App;
import com.klinker.android.talon.utilities.Utils;
import com.manuelpeinado.fadingactionbar.FadingActionBarHelper;
import com.squareup.picasso.Picasso;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.*;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends Activity {

    private Context context;
    private AppSettings settings;
    private ActionBar actionBar;

    private User thisUser;

    private String name;
    private String screenName;
    private String proPic;
    private long tweetId;
    private boolean isRetweet;
    private LayoutInflater inflater;

    private boolean isBlocking;
    private boolean isFollowing;
    private boolean isFollowingSet = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        settings = new AppSettings(context);

        setUpWindow();
        setUpTheme();
        getFromIntent();

        FadingActionBarHelper helper;

        inflater = LayoutInflater.from(context);

        /*if (settings.theme == 0) {
            helper = new FadingActionBarHelper()
                    .actionBarBackground(R.drawable.ab_solid_light_holo)
                    .headerLayout(R.layout.user_profile_header)
                    .contentLayout(R.layout.user_profile_list);
        } else {
            helper = new FadingActionBarHelper()
                    .actionBarBackground(R.drawable.ab_solid_dark)
                    .headerLayout(R.layout.user_profile_header)
                    .contentLayout(R.layout.user_profile_list);
        }*/

        //setContentView(helper.createView(this));

        setContentView(R.layout.main_fragments);

        AsyncListView listView = (AsyncListView) findViewById(R.id.listView);
        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        final View header = inflater.inflate(R.layout.user_profile_header, null);

        listView.addHeaderView(header);
        listView.setAdapter(new TimelineArrayAdapter(context, new ArrayList<Status>(0)));

        //helper.initActionBar(this);

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

        final ImageView background = (ImageView) findViewById(R.id.background_image);
        //final TextView numTweets = (TextView) findViewById(R.id.num_tweets);
        //final TextView numFollowers = (TextView) findViewById(R.id.num_followers);
        //final TextView numFollowing = (TextView) findViewById(R.id.num_following);
        final TextView statement = (TextView) findViewById(R.id.user_statement);
        final TextView screenname = (TextView) findViewById(R.id.username);
        final ListView listView = (ListView) findViewById(R.id.listView);

        //new GetData(tweetId, numTweets, numFollowers, numFollowing, statement, listView, background).execute();
        new GetData(tweetId, null, null, null, statement, listView, background).execute();

        screenname.setText("@" + screenName);
    }

    class GetData extends AsyncTask<String, Void, User> {

        private long tweetId;
        private TextView numTweets;
        private TextView numFollowers;
        private TextView numFollowing;
        private ListView listView;
        private ImageView background;
        private TextView statement;

        public GetData(long tweetId, TextView numTweets, TextView numFollowers, TextView numFollowing, TextView statement, ListView listView, ImageView background) {
            this.tweetId = tweetId;
            this.numFollowers = numFollowers;
            this.numFollowing = numFollowing;
            this.numTweets = numTweets;
            this.listView = listView;
            this.background = background;
            this.statement = statement;
        }

        protected twitter4j.User doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                if (!isMyProfile) {



                    return twitter.showUser(screenName);
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

                Picasso.with(context)
                        .load(user.getProfileBannerURL())
                        .into(background);

                new GetTimeline(user, listView).execute();
                new GetActionBarInfo(user).execute();
                //new GetFollowers(user, listView, numFollowers).execute();
                //new GetFollowing(user, listView, numFollowing).execute();
                //new GetUserStatement(user, numTweets, statement);

                statement.setText(user.getDescription());
                //try { numFollowing.setText(user.getFollowersCount()); } catch (Exception e) { }
                //try { numFollowing.setText(user.getFriendsCount()); } catch (Exception e) { }
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

    class GetFollowers extends AsyncTask<String, Void, ArrayList<twitter4j.Status>> {

        private User user;
        private ListView listView;
        private ImageView background;
        private TextView statement;

        public GetFollowers(User user, ListView listView, TextView numFollowers) {
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
                final TimelineArrayAdapter adapter = new TimelineArrayAdapter(context, statuses);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(adapter);
                    }
                });
            }
        }
    }

    class GetTimeline extends AsyncTask<String, Void, ArrayList<twitter4j.Status>> {

        private User user;
        private ListView listView;
        private ImageView background;
        private TextView statement;

        public GetTimeline(User user, ListView listView) {
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
                final TimelineArrayAdapter adapter = new TimelineArrayAdapter(context, statuses);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(adapter);
                    }
                });
            }
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
                // todo - compose new DM activity
                //Intent compose = new Intent(context, ComposeActivity.class);
                //startActivity(compose);
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
