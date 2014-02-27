package com.klinker.android.twitter.ui.profile_viewer;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ProfilePagerAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter.services.TalonPullNotificationService;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.compose.ComposeActivity;
import com.klinker.android.twitter.ui.compose.ComposeDMActivity;
import com.klinker.android.twitter.manipulations.widgets.HoloEditText;
import com.klinker.android.twitter.utils.IOUtils;
import com.klinker.android.twitter.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

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
    private boolean isFollowingSet = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.activity_slide_up, R.anim.activity_slide_down);

        mCache = App.getInstance(this).getBitmapCache();
        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        settings = AppSettings.getInstance(this);

        if ((settings.advanceWindowed && !getIntent().getBooleanExtra("long_click", false)) ||
                !settings.advanceWindowed && getIntent().getBooleanExtra("long_click", false)) {
            setUpWindow();
        }

        setUpTheme();

        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        getFromIntent();

        setContentView(R.layout.tweet_pager);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        ProfilePagerAdapter mPagerAdapter = new ProfilePagerAdapter(getFragmentManager(), context, name, screenName, proPic, tweetId, isRetweet, isMyProfile);
        pager.setAdapter(mPagerAdapter);
        pager.setOffscreenPageLimit(3);

        if (settings.addonTheme) {
            PagerTitleStrip strip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
            strip.setBackgroundColor(settings.accentInt);
        }

        Utils.setActionBar(context);

    }

    public void setUpTheme() {

        Utils.setUpPopupTheme(context, settings);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        if (settings.addonTheme) {
            getWindow().getDecorView().setBackgroundColor(settings.backgroundColor);
        }
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

        getUser();
    }

    public User thisUser;

    public void getUser() {
        Thread getUser = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);
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
                }

                new GetActionBarInfo().execute();
            }
        });

        getUser.setPriority(Thread.MAX_PRIORITY);
        getUser.start();
    }

    class GetActionBarInfo extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... urls) {
            if (isMyProfile) {
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
            } else {
                menu.getItem(MENU_FOLLOW).setVisible(false);
                menu.getItem(MENU_UNFOLLOW).setVisible(false);
                menu.getItem(MENU_FAVORITE).setVisible(false);
                menu.getItem(MENU_UNFAVORITE).setVisible(false);
                menu.getItem(MENU_BLOCK).setVisible(false);
                menu.getItem(MENU_UNBLOCK).setVisible(false);
                menu.getItem(MENU_MUTE).setVisible(false);
                menu.getItem(MENU_UNMUTE).setVisible(false);
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
        overridePendingTransition(R.anim.activity_slide_up, R.anim.activity_slide_down);
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

            case R.id.menu_mute:
                String current = sharedPrefs.getString("muted_users", "");
                sharedPrefs.edit().putString("muted_users", current + screenName.replaceAll(" ", "").replaceAll("@", "") + " ").commit();
                sharedPrefs.edit().putBoolean("refresh_me", true).commit();
                finish();
                return true;

            case R.id.menu_unmute:
                String muted = sharedPrefs.getString("muted_users", "");
                muted = muted.replace(screenName + " ", "");
                sharedPrefs.edit().putString("muted_users", muted).commit();
                sharedPrefs.edit().putBoolean("refresh_me", true).commit();
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

                        new UpdateProPic(getContentResolver().openInputStream(selectedImage)).execute();

                    } catch (Exception e) {
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                break;
            case SELECT_BANNER:
                if(resultCode == RESULT_OK){
                    try {
                        Uri selectedImage = imageReturnedIntent.getData();
                        new UpdateBanner(getContentResolver().openInputStream(selectedImage)).execute();

                    } catch (FileNotFoundException e) {
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
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

    class UpdateBanner extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        private InputStream stream;

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

                twitter.updateProfileBanner(stream);

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
        private InputStream stream;

        public UpdateProPic(InputStream stream) {
            this.stream = stream;
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

                User user = twitter.updateProfileImage(stream);
                String profileURL = user.getBiggerProfileImageURL();
                sharedPrefs.edit().putString("profile_pic_url_" + sharedPrefs.getInt("current_account", 1), profileURL).commit();

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
                Twitter twitter =  Utils.getTwitter(context, settings);

                ResponseList<UserList> lists = twitter.getUserLists(settings.myScreenName);

                return lists;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(final ResponseList<UserList> lists) {

            if (lists != null) {
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
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
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
