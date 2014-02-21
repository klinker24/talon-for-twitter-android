package com.klinker.android.twitter.ui.profile_viewer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.PeopleArrayAdapter;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter.services.TalonPullNotificationService;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.compose.ComposeActivity;
import com.klinker.android.twitter.ui.compose.ComposeDMActivity;
import com.klinker.android.twitter.ui.widgets.HoloEditText;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.ui.widgets.PhotoViewerDialog;
import com.klinker.android.twitter.utils.IOUtils;
import com.klinker.android.twitter.utils.ImageUtils;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
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

public class ProfileFragment extends Fragment {

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
    private boolean isMyProfile;

    private LayoutInflater inflater;

    private boolean isBlocking;
    private boolean isFollowing;
    private boolean isFavorite;
    private boolean isMuted;
    private boolean isFollowingSet = false;

    private ItemManager.Builder builder;

    private long currentFollowers = -1;
    private long currentFollowing = -1;
    private int refreshes = 0;
    private ArrayList<User> followers;
    private ArrayList<User> following;
    private boolean canRefresh = true;

    private ImageView background;
    private ImageView profilePicture;
    private ProgressBar spinner;

    public BitmapLruCache mCache;

    public View layout;

    public ProfileFragment(String name, String screenName, String proPic, long tweetId, boolean isRetweet, boolean isMyProfile) {
        this.name = name;
        this.screenName = screenName;
        this.proPic = proPic;
        this.tweetId = tweetId;
        this.isRetweet = isRetweet;
        this.isMyProfile = isMyProfile;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);


        mCache = App.getInstance(context).getBitmapCache();

        settings = new AppSettings(context);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        inflater = LayoutInflater.from(context);

        layout = inflater.inflate(R.layout.conversation_fragment, null);

        AsyncListView listView = (AsyncListView) layout.findViewById(R.id.listView);
        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        View header;
        boolean fromAddon = false;

        if(!settings.addonTheme) {
            header = inflater.inflate(R.layout.user_profile_header, null);
        } else {
            try {
                Context viewContext = null;
                Resources res = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);

                try {
                    viewContext = context.createPackageContext(settings.addonThemePackage, Context.CONTEXT_IGNORE_SECURITY);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (res != null && viewContext != null) {
                    int id = res.getIdentifier("user_profile_header", "layout", settings.addonThemePackage);
                    header = LayoutInflater.from(viewContext).inflate(res.getLayout(id), null);
                    fromAddon = true;
                } else {
                    header = inflater.inflate(R.layout.user_profile_header, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                header = inflater.inflate(R.layout.user_profile_header, null);
            }
        }

        listView.addHeaderView(header);
        listView.setAdapter(new TimelineArrayAdapter(context, new ArrayList<Status>(0)));

        followers = new ArrayList<User>();
        following = new ArrayList<User>();

        setUpUI(fromAddon, header, layout);

        return layout;
    }

    public TextView verified;

    public void setUpUI(boolean fromAddon, View listHeader, View layout) {
        TextView mstatement;
        TextView mscreenname;
        AsyncListView mlistView;
        ImageView mheader;

        if (fromAddon) {
            try {
                Resources res = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);

                spinner = (ProgressBar) listHeader.findViewById(res.getIdentifier("progress_bar", "id", settings.addonThemePackage));
                verified = (TextView) listHeader.findViewById(res.getIdentifier("verified_text", "id", settings.addonThemePackage));
                tweetsBtn = (Button) listHeader.findViewById(res.getIdentifier("tweets", "id", settings.addonThemePackage));
                followersBtn = (Button) listHeader.findViewById(res.getIdentifier("followers", "id", settings.addonThemePackage));
                followingBtn = (Button) listHeader.findViewById(res.getIdentifier("following", "id", settings.addonThemePackage));
                background = (ImageView) listHeader.findViewById(res.getIdentifier("background_image", "id", settings.addonThemePackage));
                profilePicture = (ImageView) listHeader.findViewById(res.getIdentifier("profile_pic", "id", settings.addonThemePackage));
                mstatement = (TextView) listHeader.findViewById(res.getIdentifier("user_statement", "id", settings.addonThemePackage));
                mscreenname = (TextView) listHeader.findViewById(res.getIdentifier("username", "id", settings.addonThemePackage));
                mlistView = (AsyncListView) layout.findViewById(R.id.listView);
                mheader = (ImageView) listHeader.findViewById(res.getIdentifier("background_image", "id", settings.addonThemePackage));
            } catch (Exception e) {
                spinner = (ProgressBar) listHeader.findViewById(R.id.progress_bar);
                verified = (TextView) listHeader.findViewById(R.id.verified_text);
                tweetsBtn = (Button) listHeader.findViewById(R.id.tweets);
                followersBtn = (Button) listHeader.findViewById(R.id.followers);
                followingBtn = (Button) listHeader.findViewById(R.id.following);
                background = (ImageView) listHeader.findViewById(R.id.background_image);
                profilePicture = (ImageView) listHeader.findViewById(R.id.profile_pic);
                mstatement = (TextView) listHeader.findViewById(R.id.user_statement);
                mscreenname = (TextView) listHeader.findViewById(R.id.username);
                mlistView = (AsyncListView) layout.findViewById(R.id.listView);
                mheader = (ImageView) listHeader.findViewById(R.id.background_image);
            }
        } else {
            spinner = (ProgressBar) listHeader.findViewById(R.id.progress_bar);
            verified = (TextView) listHeader.findViewById(R.id.verified_text);
            tweetsBtn = (Button) listHeader.findViewById(R.id.tweets);
            followersBtn = (Button) listHeader.findViewById(R.id.followers);
            followingBtn = (Button) listHeader.findViewById(R.id.following);
            background = (ImageView) listHeader.findViewById(R.id.background_image);
            profilePicture = (ImageView) listHeader.findViewById(R.id.profile_pic);
            mstatement = (TextView) listHeader.findViewById(R.id.user_statement);
            mscreenname = (TextView) listHeader.findViewById(R.id.username);
            mlistView = (AsyncListView) layout.findViewById(R.id.listView);
            mheader = (ImageView) listHeader.findViewById(R.id.background_image);
        }

        final TextView statement = mstatement;
        final TextView screenname = mscreenname;
        final AsyncListView listView = mlistView;
        final ImageView header = mheader;

        spinner.setVisibility(View.VISIBLE);

        actionBar.setTitle(name);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        statement.setTextSize(settings.textSize);
        screenname.setTextSize(settings.textSize + 1);

        getData(statement, listView);

        screenname.setText("@" + screenName);

        tweetsBtn.setText(getResources().getString(R.string.tweets));
        tweetsBtn.setTextSize(settings.textSize - 1);
        tweetsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (current != BTN_TWEET) {
                    current = BTN_TWEET;
                    currentFollowing = -1;
                    currentFollowers = -1;
                    refreshes = 0;

                    listView.setItemManager(builder.build());
                    listView.setAdapter(timelineAdapter);

                    getTimeline(thisUser, listView);
                }
            }
        });

        followersBtn.setText(getResources().getString(R.string.followers));
        followersBtn.setTextSize(settings.textSize - 1);
        followersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (current != BTN_FOLLOWERS) {
                    current = BTN_FOLLOWERS;

                    listView.setItemManager(null);
                    listView.setAdapter(followersAdapter);

                    getFollowers(thisUser, listView);
                }
            }
        });

        followingBtn.setText(getResources().getString(R.string.following));
        followingBtn.setTextSize(settings.textSize - 1);
        followingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (current != BTN_FOLLOWING) {
                    current = BTN_FOLLOWING;

                    listView.setItemManager(null);
                    listView.setAdapter(new PeopleArrayAdapter(context, following));

                    getFollowing(thisUser, listView);
                }
            }
        });

        background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(spinner.getVisibility() == View.GONE) {
                    startActivity(new Intent(context, PhotoViewerDialog.class).putExtra("url", thisUser.getProfileBannerURL()));
                } else {
                    // it isn't ready to be opened just yet
                }
            }
        });

        profilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (spinner.getVisibility() == View.GONE) {
                    try {
                        startActivity(new Intent(context, PhotoViewerDialog.class).putExtra("url", thisUser.getOriginalProfileImageURL()));
                    } catch (Exception e) {
                        // this user doesn't exist...
                    }
                } else {
                    // it isn't ready to be opened just yet
                }
            }
        });

        canRefresh = false;

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
                        getFollowing(thisUser, listView);
                    } else if (current == BTN_FOLLOWERS && canRefresh) {
                        getFollowers(thisUser, listView);
                    } else if (current == BTN_TWEET && canRefresh) {
                        getTimeline(thisUser, listView);
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

                if (settings.translateProfileHeader) {
                    header.setTranslationY(-listView.getChildAt(0).getTop() / 2);
                }
            }
        });
    }

    public void getURL(final TextView statement, final User user) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection;
                String url = "";
                try {
                    URL address = new URL(user.getURL());
                    connection = (HttpURLConnection) address.openConnection(Proxy.NO_PROXY);
                    connection.setConnectTimeout(1000);
                    connection.setInstanceFollowRedirects(false);
                    connection.setReadTimeout(1000);
                    connection.connect();
                    String expandedURL = connection.getHeaderField("Location");
                    if(expandedURL != null) {
                        url = expandedURL;
                    } else {
                        url = user.getURL();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    url = user.getURL();
                }

                if (url != null) {
                    final String fUrl = url;

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statement.append("\n" + fUrl);

                            if (!settings.addonTheme) {
                                linkifyText(statement);
                            }
                        }
                    });
                }

                getFollowingStatus(statement, user);


            }
        }).start();
    }

    public void getFollowingStatus(final TextView statement, final User user) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (user.getScreenName().equals(settings.myScreenName)) {
                        return;
                    }
                    final String followingStatus = Utils.getTwitter(context, settings).showFriendship(settings.myScreenName, thisUser.getScreenName()).isTargetFollowingSource() ?
                            getResources().getString(R.string.follows_you) : getResources().getString(R.string.not_following_you);

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statement.append("\n\n" + followingStatus);
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public void getData(final TextView statement, final AsyncListView listView) {

        Thread getData = new Thread(new Runnable() {
            @Override
            public void run() {
                Twitter twitter =  Utils.getTwitter(context, settings);
                try {
                    Log.v("talon_profile", "start of load time: " + Calendar.getInstance().getTimeInMillis());
                    if (!isMyProfile) {
                        thisUser = twitter.showUser(screenName);
                    } else {
                        if (settings.myId == 0) {
                            try {
                                thisUser = twitter.showUser(settings.myScreenName);
                            } catch (Exception e) {
                                // the user has changed their screen name, so look for the id
                                ((Activity)context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "You changed your username before Talon could save your ID! You will have to log out and back in once to make the correct changes!", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                            return;
                        } else {
                            thisUser = twitter.showUser(settings.myId);
                        }


                        // update the profile picture url and the background url in shared prefs
                        int currentAccount = sharedPrefs.getInt("current_account", 1);

                        SharedPreferences.Editor e = sharedPrefs.edit();
                        e.putString("twitter_users_name_" + currentAccount, thisUser.getName()).commit();
                        e.putString("twitter_screen_name_" + currentAccount, thisUser.getScreenName()).commit();
                        e.putLong("twitter_id_" + currentAccount, thisUser.getId()).commit();
                        e.putString("profile_pic_url_" + currentAccount, thisUser.getBiggerProfileImageURL());
                        e.putString("twitter_background_url_" + currentAccount, thisUser.getProfileBannerURL());
                        e.commit();
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (thisUser.isVerified()) {
                                    if (settings.addonTheme) {
                                        verified.setVisibility(View.VISIBLE);
                                        verified.setText(getResources().getString(R.string.verified));
                                    } else {
                                        verified.setVisibility(View.VISIBLE);
                                        verified.setText(getResources().getString(R.string.verified));
                                    }
                                }
                            } catch (Exception e) {
                                // their theme was created before this was implemented
                            }

                            String state = thisUser.getDescription() + "\n";
                            String loca = thisUser.getLocation();

                            if (!loca.equals("")) {
                                state += "\n" + thisUser.getLocation();
                            }

                            if (state.equals("")) {
                                statement.setText(getResources().getString(R.string.no_description));
                            } else {
                                statement.setText(state);
                            }

                            if (!settings.addonTheme) {
                                statement.setLinkTextColor(getResources().getColor(R.color.app_color));
                            } else {
                                statement.setLinkTextColor(settings.accentInt);
                            }

                            tweetsBtn.setText(getResources().getString(R.string.tweets) + "\n" + "(" + thisUser.getStatusesCount() + ")");
                            followersBtn.setText(getResources().getString(R.string.followers) + "\n" + "(" + thisUser.getFollowersCount() + ")");
                            followingBtn.setText(getResources().getString(R.string.following) + "\n" + "(" + thisUser.getFriendsCount() + ")");

                            getURL(statement, thisUser);
                            getTimeline(thisUser, listView);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Error loading profile. Check your network connection.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        getData.setPriority(Thread.MAX_PRIORITY);
        getData.start();
    }

    public PeopleArrayAdapter followersAdapter;

    public void getFollowers(final User user, final AsyncListView listView) {
        spinner.setVisibility(View.VISIBLE);
        canRefresh = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    PagableResponseList<User> friendsPaging = twitter.getFollowersList(user.getId(), currentFollowers);

                    for (int i = 0; i < friendsPaging.size(); i++) {
                        followers.add(friendsPaging.get(i));
                    }

                    currentFollowers = friendsPaging.getNextCursor();

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (followersAdapter == null) {
                                followersAdapter = new PeopleArrayAdapter(context, followers);
                                listView.setAdapter(followersAdapter);
                            } else {
                                followersAdapter.notifyDataSetChanged();
                            }

                            if(settings.roundContactImages) {
                                ImageUtils.loadCircleImage(context, profilePicture, thisUser.getBiggerProfileImageURL(), mCache);
                            } else {
                                ImageUtils.loadImage(context, profilePicture, thisUser.getBiggerProfileImageURL(), mCache);
                            }

                            String url = user.getProfileBannerURL();
                            ImageUtils.loadImage(context, background, url, mCache);

                            canRefresh = true;
                            spinner.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Couldn't load timeline! Try checking your data connection.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public PeopleArrayAdapter followingAdapter;

    public void getFollowing(final User user, final AsyncListView listView) {
        spinner.setVisibility(View.VISIBLE);
        canRefresh = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    PagableResponseList<User> friendsPaging = twitter.getFriendsList(user.getId(), currentFollowing);

                    for (int i = 0; i < friendsPaging.size(); i++) {
                        following.add(friendsPaging.get(i));
                        Log.v("friends_list", friendsPaging.get(i).getName());
                    }

                    currentFollowing = friendsPaging.getNextCursor();

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (followingAdapter == null) {
                                followingAdapter = new PeopleArrayAdapter(context, following);
                                listView.setAdapter(followingAdapter);
                            } else {
                                followingAdapter.notifyDataSetChanged();
                            }

                            if(settings.roundContactImages) {
                                ImageUtils.loadCircleImage(context, profilePicture, thisUser.getBiggerProfileImageURL(), mCache);
                            } else {
                                ImageUtils.loadImage(context, profilePicture, thisUser.getBiggerProfileImageURL(), mCache);
                            }

                            String url = user.getProfileBannerURL();
                            ImageUtils.loadImage(context, background, url, mCache);

                            canRefresh = true;
                            spinner.setVisibility(View.GONE);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Couldn't load timeline! Try checking your data connection.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public Paging timelinePaging = new Paging(1, 20);
    public ArrayList<Status> timelineStatuses = new ArrayList<Status>();
    public TimelineArrayAdapter timelineAdapter;

    public void getTimeline(final User user, final AsyncListView listView) {
        spinner.setVisibility(View.VISIBLE);
        canRefresh = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    List<twitter4j.Status> statuses = twitter.getUserTimeline(user.getId(), timelinePaging);
                    timelinePaging.setPage(timelinePaging.getPage() + 1);

                    for (twitter4j.Status s : statuses) {
                        timelineStatuses.add(s);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (timelineAdapter != null) {
                                timelineAdapter.notifyDataSetChanged();
                            } else {
                                timelineAdapter= new TimelineArrayAdapter(context, timelineStatuses, screenName);
                                listView.setItemManager(builder.build());
                                listView.setAdapter(timelineAdapter);
                            }

                            if(settings.roundContactImages) {
                                ImageUtils.loadCircleImage(context, profilePicture, thisUser.getBiggerProfileImageURL(), mCache);
                            } else {
                                ImageUtils.loadImage(context, profilePicture, thisUser.getBiggerProfileImageURL(), mCache);
                            }

                            String url = user.getProfileBannerURL();
                            ImageUtils.loadImage(context, background, url, mCache);

                            spinner.setVisibility(View.GONE);
                            canRefresh = true;
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Couldn't load timeline! Try checking your data connection.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (OutOfMemoryError x) {
                    x.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Couldn't load timeline! Try checking your data connection.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private static void linkifyText(TextView textView) {
        CharSequence text = textView.getText();
        if (Patterns.PHONE.matcher(text).find() ||
                Patterns.EMAIL_ADDRESS.matcher(text).find() ||
                Patterns.WEB_URL.matcher(text).find()) {
            Linkify.addLinks(textView, Linkify.ALL);
        }
    }
}
