package com.klinker.android.twitter.widget.launcher_fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.StaleDataException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.launcher.api.BaseLauncherPage;
import com.klinker.android.launcher.api.ResourceHelper;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.CursorListLoader;
import com.klinker.android.twitter.adapters.LauncherListLoader;
import com.klinker.android.twitter.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.manipulations.widgets.swipe_refresh_layout.FullScreenSwipeRefreshLayout;
import com.klinker.android.twitter.manipulations.widgets.swipe_refresh_layout.SwipeProgressBar;
import com.klinker.android.twitter.services.CatchupPull;
import com.klinker.android.twitter.services.MentionsRefreshService;
import com.klinker.android.twitter.services.TimelineRefreshService;
import com.klinker.android.twitter.services.WidgetRefreshService;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.ui.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter.ui.setup.LoginActivity;
import com.klinker.android.twitter.utils.ImageUtils;
import com.klinker.android.twitter.utils.Utils;
import com.klinker.android.twitter.utils.api_helper.TweetMarkerHelper;
import com.klinker.android.twitter.widget.launcher_fragment.adapters.DrawerArrayAdapter;
import com.klinker.android.twitter.widget.launcher_fragment.adapters.LauncherTimelineCursorAdapter;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class LauncherFragment extends BaseLauncherPage
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public ImageView backgroundPic;
    public ImageView profilePic;
    public ListView drawerList;
    public View statusBar;
    public LinearLayout sendLayout;
    
    public boolean canSwitch = true;
    public boolean translucent;
    public int statusBarHeight = 0;

    public ResourceHelper resHelper;

    public Handler sendHandler;

    public boolean showIsRunning = false;
    public Runnable showSend = new Runnable() {
        @Override
        public void run() {
            if (sendLayout.getVisibility() == View.GONE && !showIsRunning) {
                Animation anim = resHelper.getAnimation("slide_in_left");
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        showIsRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sendLayout.setVisibility(View.VISIBLE);
                        showIsRunning = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                anim.setDuration(300);
                sendLayout.startAnimation(anim);
            }
        }
    };

    public boolean hideIsRunning = false;
    public Runnable hideSend = new Runnable() {
        @Override
        public void run() {
            if (sendLayout.getVisibility() == View.VISIBLE && !hideIsRunning) {
                Animation anim = resHelper.getAnimation("slide_out_right");
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        hideIsRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sendLayout.setVisibility(View.GONE);
                        hideIsRunning = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                anim.setDuration(300);
                sendLayout.startAnimation(anim);
            }
        }
    };

    @Override
    public BaseLauncherPage getFragment(int position) {
        LauncherFragment fragment = new LauncherFragment();
        return fragment;
    }

    @Override
    public View[] getBackground() {
        return new View[] {background, sendLayout};
    }

    public View getLayout(LayoutInflater inflater) {
        return resHelper.getLayout("launcher_frag");
    }

    public boolean openedFrag = false;

    @Override
    public void onFragmentsOpened() {
        openedFrag = true;
        showBar();
        sendHandler.post(showSend);

        if(settings.tweetmarker) {
            fetchTweetMarker();
        }
    }

    @Override
    public void onFragmentsClosed() {
        if (scrolled) {
            scrolled = false;
            markReadForLoad();
        }

        if (settings.tweetmarker) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    TweetMarkerHelper helper = new TweetMarkerHelper(currentAccount,
                            sharedPrefs.getString("twitter_screen_name_" + currentAccount, ""),
                            Utils.getTwitter(context, new AppSettings(context)), sharedPrefs);

                    long currentId = sharedPrefs.getLong("current_position_" + currentAccount, 0);
                    helper.sendCurrentId("timeline", currentId);

                }
            }).start();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        sharedPrefs = talonContext.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        if (settings.theme != AppSettings.getCurrentTheme(sharedPrefs)) {
            // night mode happened
            settings = new AppSettings(sharedPrefs, talonContext);
            switchedAccounts = true;
            setViews(layout);
            getLoaderManager().restartLoader(0, null, this);
        }

    }

    @Override
    public void onStop() {
        if (scrolled) {
            scrolled = false;
            markReadForLoad();
        }
        super.onStop();
    }

    public CursorAdapter returnAdapter(Cursor c) {
        return new LauncherTimelineCursorAdapter(talonContext, context, c, false, true);
    }

    public void resetTimeline(boolean spinner) {
        // don't need this because of the load manager
        //context.getLoaderManager().restartLoader(0, null, this);
    }

    public void setAppSettings() {
        try {
            talonContext = context.createPackageContext("com.klinker.android.twitter", Context.CONTEXT_IGNORE_SECURITY);
            sharedPrefs = talonContext.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
            settings = new AppSettings(sharedPrefs, talonContext);
        } catch (Exception e) {
            talonContext = context;
            settings = AppSettings.getInstance(context);
        }
    }

    public Context talonContext;

    public void getCache() {
        try {
            File cacheDir = new File(talonContext.getCacheDir(), "talon");
            cacheDir.mkdirs();

            BitmapLruCache.Builder builder = new BitmapLruCache.Builder();
            builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize();
            builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheDir);

            mCache = builder.build();
        } catch (Exception e) {
            mCache = App.getInstance(context).getBitmapCache();
        }
    }

    public boolean isLauncher() {
        return true;
    }

    public boolean scrolled = false;

    public void setUpListScroll() {

        sendHandler = new Handler();

        if (settings.useToast) {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {

                int mLastFirstVisibleItem = 0;

                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                    if (i == SCROLL_STATE_IDLE) {
                        sendHandler.removeCallbacks(hideSend);
                        sendHandler.postDelayed(showSend, 600);
                    } else {
                        sendHandler.removeCallbacks(showSend);
                        sendHandler.postDelayed(hideSend, 300);
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                    if (openedFrag) {
                        openedFrag = false;
                        final Intent handleScroll = new Intent("android.intent.action.MAIN");
                        handleScroll.setComponent(new ComponentName("com.klinker.android.twitter", "com.klinker.android.twitter.widget.launcher_fragment.utils.HandleScrollService"));
                        handleScroll.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        talonContext.startService(handleScroll);
                    }

                    if (!scrolled) {
                        scrolled = true;
                    }

                    if (settings.uiExtras) {
                        if (firstVisibleItem != 0) {
                            if (canSwitch) {
                                // used to show and hide the action bar
                                if (firstVisibleItem < 3) {

                                } else if (firstVisibleItem < mLastFirstVisibleItem) {
                                    showToastBar(firstVisibleItem + " " + fromTop, jumpToTop, 400, false, toTopListener, false);
                                } else if (firstVisibleItem > mLastFirstVisibleItem) {
                                    hideToastBar(400);
                                }

                                mLastFirstVisibleItem = firstVisibleItem;
                            }
                        } else {
                            if (liveUnread == 0) {
                                hideToastBar(400);
                            }
                        }
                    } else {
                        hideToastBar(400);
                    }

                    if (firstVisibleItem == 0) {
                        showBar();
                    } else {
                        hideBar();
                    }

                    // this is for when they are live streaming and get to the top of the feed, the "View" button comes up.
                    if (newTweets && firstVisibleItem == 0 && settings.liveStreaming) {
                        if (liveUnread > 0) {
                            showToastBar(liveUnread + " " + (liveUnread == 1 ? resHelper.getString("new_tweet") : resHelper.getString("new_tweets")),
                                    resHelper.getString("view"),
                                    400,
                                    false,
                                    liveStreamRefresh,
                                    true);
                        }
                    }
                }
            });
        } else {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {

                int mLastFirstVisibleItem = 0;

                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                    if (i == SCROLL_STATE_IDLE) {
                        sendHandler.removeCallbacks(hideSend);
                        sendHandler.postDelayed(showSend, 600);
                    } else {
                        sendHandler.removeCallbacks(showSend);
                        sendHandler.postDelayed(hideSend, 300);
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                    if (openedFrag) {
                        openedFrag = false;
                        final Intent handleScroll = new Intent("android.intent.action.MAIN");
                        handleScroll.setComponent(new ComponentName("com.klinker.android.twitter", "com.klinker.android.twitter.widget.launcher_fragment.utils.HandleScrollService"));
                        handleScroll.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        talonContext.startService(handleScroll);
                    }

                    if (!scrolled) {
                        scrolled = true;
                    }

                    if (newTweets && firstVisibleItem == 0 && (settings.liveStreaming)) {
                        if (liveUnread > 0) {
                            showToastBar(liveUnread + " " + (liveUnread == 1 ? resHelper.getString("new_tweet") : resHelper.getString("new_tweets")),
                                    resHelper.getString("view"),
                                    400,
                                    false,
                                    liveStreamRefresh,
                                    true);
                        } else {
                            hideToastBar(400);
                        }
                    } else {
                        hideToastBar(400);
                    }

                    if (firstVisibleItem == 0) {
                        showBar();
                    } else {
                        hideBar();
                    }

                    if (settings.uiExtras) {
                        if (firstVisibleItem != 0) {
                            if (canSwitch) {
                                mLastFirstVisibleItem = firstVisibleItem;
                            }
                        }
                    }

                }
            });
        }
    }

    public boolean barShowing = false;
    public void showBar() {
        if (barShowing || immersiveMode || Build.VERSION.SDK_INT < 18) {
            return;
        } else {
            barShowing = true;
        }
        Animation anim = resHelper.getAnimation("fade_in");
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                statusBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(500);
        statusBar.startAnimation(anim);
    }

    public void hideBar() {
        if (!barShowing || immersiveMode || Build.VERSION.SDK_INT < 18) {
            return;
        } else {
            barShowing = false;
        }

        Animation anim = resHelper.getAnimation("fade_out");
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                statusBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(500);
        statusBar.startAnimation(anim);
    }

    public void getStrings() {
        resHelper = new ResourceHelper(getActivity(), "com.klinker.android.twitter");
        fromTop = resHelper.getString("from_top");
        jumpToTop = resHelper.getString("jump_to_top");
        allRead = resHelper.getString("all_read");
        toMentions = resHelper.getString("mentions");
    }

    public void setStrings() {
        sNewTweet = resHelper.getString("new_tweet");
        sNewTweets = resHelper.getString("new_tweets");
        sNoNewTweets = resHelper.getString("no_new_tweets");
        sNewMention = resHelper.getString("new_mention");
        sNewMentions = resHelper.getString("new_mentions");
    }

    public boolean logoutVisible = false;

    public void setUpHeaders() {
        if (Build.VERSION.SDK_INT >= 19 && !switchedAccounts) {
            if (Utils.hasNavBar(context)) {
                View footer = new View(context);
                footer.setOnClickListener(null);
                footer.setOnLongClickListener(null);
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
                footer.setLayoutParams(params);
                listView.addFooterView(footer);
                listView.setFooterDividersEnabled(false);
            }

            View view = new View(context);
            view.setOnClickListener(null);
            view.setOnLongClickListener(null);
            ListView.LayoutParams params2 = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getStatusBarHeight(context));
            view.setLayoutParams(params2);
            listView.addHeaderView(view);
            listView.setHeaderDividersEnabled(false);
        }
    }

    public View background;
    public View rootLayout;

    public boolean immersiveMode = false;

    public boolean switchedAccounts = false;

    public void setViews(View layout) {

        try {
            int immersive = android.provider.Settings.System.getInt(context.getContentResolver(), "immersive_mode");

            if (immersive == 1) {
                immersiveMode = true;
            }
        } catch (Exception e) {

        }

        rootLayout = layout;

        background = layout.findViewById(resHelper.getId("frag_background"));
        if (settings.addonTheme) {
            background.setBackgroundDrawable(settings.customBackground);
        } else {
            switch (settings.theme) {
                case AppSettings.THEME_LIGHT:
                    background.setBackgroundColor(resHelper.getColor("white"));
                    break;
                case AppSettings.THEME_DARK:
                    background.setBackgroundColor(resHelper.getColor("dark_background"));
                    break;
                case AppSettings.THEME_BLACK:
                    background.setBackgroundColor(resHelper.getColor("black_background"));
                    break;
            }
        }

        cursorAdapter = null;
        if (!switchedAccounts) {
            getLoaderManager().initLoader(0, null, this);
        }

        if (!switchedAccounts) {
            LinearLayout root = (LinearLayout) layout.findViewById(resHelper.getId("swipe_layout"));
            listView = new AsyncListView(context);
            listView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            spinner = (LinearLayout) layout.findViewById(resHelper.getId("spinner"));

            refreshLayout = new FullScreenSwipeRefreshLayout(context);
            refreshLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            refreshLayout.setFullScreen(true);
            refreshLayout.setOnRefreshListener(new FullScreenSwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refreshTweetmarker = true;
                    onRefreshStarted();
                }
            });

            root.addView(refreshLayout);
            refreshLayout.addView(listView);
        }

        if (settings.addonTheme) {
            refreshLayout.setColorScheme(settings.accentInt,
                    SwipeProgressBar.COLOR2,
                    settings.accentInt,
                    SwipeProgressBar.COLOR3);
        } else {
            if (settings.theme != AppSettings.THEME_LIGHT) {
                refreshLayout.setColorScheme(resHelper.getColor("app_color"),
                        SwipeProgressBar.COLOR2,
                        resHelper.getColor("app_color"),
                        SwipeProgressBar.COLOR3);
            } else {
                refreshLayout.setColorScheme(resHelper.getColor("app_color"),
                        resHelper.getColor("light_ptr_1"),
                        resHelper.getColor("app_color"),
                        resHelper.getColor("light_ptr_2"));
            }
        }

        setUpHeaders();

        backgroundPic = (ImageView) layout.findViewById(resHelper.getId("background_image"));
        profilePic = (ImageView) layout.findViewById(resHelper.getId("profile_pic_contact"));
        drawerList = (ListView) layout.findViewById(resHelper.getId("drawer_list"));
        sendLayout = (LinearLayout) layout.findViewById(resHelper.getId("send_layout"));
        final ImageButton showMoreDrawer = (ImageButton) layout.findViewById(resHelper.getId("options"));
        final ImageView proPic2 = (ImageView) layout.findViewById(resHelper.getId("profile_pic_2"));
        final LinearLayout logoutLayout = (LinearLayout) layout.findViewById(resHelper.getId("logoutLayout"));
        ImageButton sendButton = (ImageButton) layout.findViewById(resHelper.getId("send_button"));
        ImageButton openApp = (ImageButton) layout.findViewById(resHelper.getId("open_app_button"));

        final String backgroundUrl = settings.myBackgroundUrl;
        final String profilePicUrl = settings.myProfilePicUrl;
        statusBar = layout.findViewById(resHelper.getId("activity_status_bar"));

        int statusBarHeight = Utils.getStatusBarHeight(context);

        View drawerToListDivider = layout.findViewById(resHelper.getId("drawer_to_list_divider"));
        ColorDrawable color;

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                color = new ColorDrawable(resHelper.getColor("light_text_drawer"));
                sendLayout.setBackgroundDrawable(resHelper.getDrawable("send_card"));
                sendButton.setImageDrawable(resHelper.getDrawable("ic_send_light"));
                break;
            case AppSettings.THEME_DARK:
                color = new ColorDrawable(resHelper.getColor("dark_text_drawer"));
                sendLayout.setBackgroundDrawable(resHelper.getDrawable("send_card_dark"));
                sendButton.setImageDrawable(resHelper.getDrawable("ic_send_dark"));
                break;
            default:
                color = new ColorDrawable(resHelper.getColor("dark_text_drawer"));
                sendLayout.setBackgroundDrawable(resHelper.getDrawable("send_card_black"));
                sendButton.setImageDrawable(resHelper.getDrawable("ic_send_dark"));
                break;
        }

        drawerToListDivider.setBackgroundDrawable(color);
        drawerList.setDivider(color);

        openApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                markReadForLoad();
                talonContext.startActivity(new Intent(talonContext, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent compose = new Intent("android.intent.action.MAIN");
                compose.setComponent(new ComponentName("com.klinker.android.twitter", "com.klinker.android.twitter.ui.compose.LauncherCompose"));
                compose.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                compose.putExtra("current_account", currentAccount);

                talonContext.startActivity(compose);
            }
        });

        try {
            RelativeLayout.LayoutParams statusParams = (RelativeLayout.LayoutParams) statusBar.getLayoutParams();
            statusParams.height = statusBarHeight;
            statusBar.setLayoutParams(statusParams);
        } catch (Exception e) {
            try {
                LinearLayout.LayoutParams statusParams = (LinearLayout.LayoutParams) statusBar.getLayoutParams();
                statusParams.height = statusBarHeight;
                statusBar.setLayoutParams(statusParams);
            } catch (Exception x) {
                // in the trends
            }
        }

        if (!backgroundUrl.equals("")) {
            ImageUtils.loadImage(context, backgroundPic, backgroundUrl, mCache);
        } else {
            backgroundPic.setImageDrawable(resHelper.getDrawable("default_header_background"));
        }

        try {
            if (settings.roundContactImages) {
                ImageUtils.loadCircleImage(context, profilePic, profilePicUrl, mCache);
            } else {
                ImageUtils.loadImage(context, profilePic, profilePicUrl, mCache);
            }
        } catch (Exception e) {
            // empty path again
        }

        showMoreDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!logoutVisible) {
                    Animation ranim = resHelper.getAnimation("drawer_rotate");
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    Animation anim = resHelper.getAnimation("fade_out");
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            drawerList.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    anim.setDuration(300);
                    drawerList.startAnimation(anim);

                    Animation anim2 = resHelper.getAnimation("fade_in");
                    anim2.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            logoutLayout.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    anim2.setDuration(300);
                    logoutLayout.startAnimation(anim2);

                    logoutVisible = true;
                } else {
                    Animation ranim = resHelper.getAnimation("drawer_rotate_back");
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    Animation anim = resHelper.getAnimation("fade_in");
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            drawerList.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    anim.setDuration(300);
                    drawerList.startAnimation(anim);

                    Animation anim2 = resHelper.getAnimation("fade_out");
                    anim2.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            logoutLayout.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    anim2.setDuration(300);
                    logoutLayout.startAnimation(anim2);

                    logoutVisible = false;
                }
            }
        });

        backgroundPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Intent viewProfile = new Intent("android.intent.action.MAIN");
                        viewProfile.setComponent(new ComponentName("com.klinker.android.twitter", "com.klinker.android.twitter.ui.profile_viewer.LauncherProfilePager"));
                        viewProfile.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        viewProfile.putExtra("name", settings.myName);
                        viewProfile.putExtra("screenname", settings.myScreenName);
                        viewProfile.putExtra("proPic", profilePicUrl);
                        viewProfile.putExtra("tweetid", 0);
                        viewProfile.putExtra("retweet", false);
                        viewProfile.putExtra("long_click", false);
                        viewProfile.putExtra("current_account", currentAccount);
                        viewProfile.putExtra("from_widget", true);

                        context.startActivity(viewProfile);
                    }
                }, 400);
            }
        });

        DrawerArrayAdapter adapter = new DrawerArrayAdapter(talonContext);
        drawerList.setAdapter(adapter);

        final int extraPages = adapter.getNumExtraPages();

        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == extraPages) {
                    return;
                }

                //markReadForLoad();

                final Intent popup = new Intent("android.intent.action.MAIN");
                popup.setComponent(new ComponentName("com.klinker.android.twitter", "com.klinker.android.twitter.utils.redirects.RedirectToLauncherPopup"));
                popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                popup.putExtra("current_account", currentAccount);

                if (i < extraPages) {
                    popup.putExtra("launcher_page", i);
                } else {
                    popup.putExtra("launcher_page", i-1);
                }

                context.startActivity(popup);
            }
        });

        // set up for the second account
        int count = 0; // number of accounts logged in

        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            count++;
        }

        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            count++;
        }

        final int current = sharedPrefs.getInt("current_account", 1);

        // make a second account
        if(count == 1){
            proPic2.setVisibility(View.GONE);
        } else { // switch accounts
            if (current == 1) {
                try {
                    if(settings.roundContactImages) {
                        ImageUtils.loadCircleImage(context, proPic2, sharedPrefs.getString("profile_pic_url_2", ""), mCache);
                    } else {
                        ImageUtils.loadImage(context, proPic2, sharedPrefs.getString("profile_pic_url_2", ""), mCache);
                    }
                } catch (Exception e) {

                }

                proPic2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (canSwitch) {
                            showMoreDrawer.performClick();

                            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION").putExtra("current_account", current));

                            // restart Talon pull
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    final Intent pull = new Intent("android.intent.action.MAIN");
                                    pull.setComponent(new ComponentName("com.klinker.android.twitter", "com.klinker.android.twitter.utils.redirects.StartPull"));
                                    pull.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    pull.putExtra("current_account", 2);

                                    talonContext.startActivity(pull);
                                }
                            }, 2500);

                            Toast.makeText(context, "Preparing to switch", Toast.LENGTH_SHORT).show();

                            // we want to wait a second so that the mark position broadcast will work
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(500);
                                    } catch (Exception e) {

                                    }

                                    sharedPrefs.edit().putInt("current_account", 2).commit();
                                    currentAccount = 2;
                                    sharedPrefs.edit().remove("new_notifications").remove("new_retweets").remove("new_favorites").remove("new_follows").commit();
                                    settings = new AppSettings(sharedPrefs, context);

                                    context.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            switchedAccounts = true;
                                            setViews(rootLayout);
                                            context.getLoaderManager().restartLoader(0, null, LauncherFragment.this);
                                        }
                                    });
                                }
                            }).start();

                        }
                    }
                });
            } else {
                try {
                    if(settings.roundContactImages) {
                        ImageUtils.loadCircleImage(context, proPic2, sharedPrefs.getString("profile_pic_url_1", ""), mCache);
                    } else {
                        ImageUtils.loadImage(context, proPic2, sharedPrefs.getString("profile_pic_url_1", ""), mCache);
                    }
                } catch (Exception e) {

                }
                proPic2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (canSwitch) {
                            showMoreDrawer.performClick();

                            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION").putExtra("current_account", current));

                            // restart Talon pull
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    final Intent pull = new Intent("android.intent.action.MAIN");
                                    pull.setComponent(new ComponentName("com.klinker.android.twitter", "com.klinker.android.twitter.utils.redirects.StartPull"));
                                    pull.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    pull.putExtra("current_account", 1);

                                    talonContext.startActivity(pull);
                                }
                            }, 2500);

                            Toast.makeText(context, "Preparing to switch", Toast.LENGTH_SHORT).show();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(500);
                                    } catch (Exception e) {

                                    }

                                    sharedPrefs.edit().putInt("current_account", 1).commit();
                                    currentAccount = 1;
                                    sharedPrefs.edit().remove("new_notifications").remove("new_retweets").remove("new_favorites").remove("new_follows").commit();
                                    AppSettings.invalidate();
                                    settings = new AppSettings(sharedPrefs, context);
                                    context.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            switchedAccounts = true;
                                            setViews(rootLayout);
                                            context.getLoaderManager().restartLoader(0, null, LauncherFragment.this);
                                        }
                                    });

                                }
                            }).start();
                        }
                    }
                });
            }
        }

        refreshLayout.setFullScreen(false);
        if (Build.VERSION.SDK_INT >= 19) {
            refreshLayout.setOnlyStatus(true);
        }
    }

    public void setBuilder() {
        LauncherListLoader loader = new LauncherListLoader(mCache, context, false);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
        builder.setThreadPoolSize(2);

        listView.setItemManager(builder.build());
    }

    public int insertTweets(List<Status> statuses, long[] lastId) {
        return HomeContentProvider.insertTweets(statuses, currentAccount, context);
    }

    boolean launcherRefreshing = false;

    public int doRefresh() {
        int numberNew = 0;

        // TODO: start a service to make sure talon is opened to the correct place here.
        // commit the account to shared prefs, invalidate settings, and force update on start

        if (TimelineRefreshService.isRunning || WidgetRefreshService.isRunning || CatchupPull.isRunning) {
            // quit if it is running in the background
            return 0;
        }

        long id = 1l;

        try {
            Cursor cursor = cursorAdapter.getCursor();
            if (cursor.moveToLast()) {
                id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();

                launcherRefreshing = true;
                HomeContentProvider.updateCurrent(currentAccount, context, cursor.getCount() - 1);
            }
        } catch (Exception e) {
            return 0;
        }

        if (id == 1l) {
            return 0;
        }

        try {
            context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));

            twitter = Utils.getTwitter(context, settings);

            User user = twitter.verifyCredentials();

            final List<twitter4j.Status> statuses = new ArrayList<Status>();

            boolean foundStatus = false;

            Paging paging = new Paging(1, 200);

            Log.v("talon_inserting", "since_id=" + id);
            try {
                paging.setSinceId(id);
            } catch (Exception e) {
                // 0 for some reason, so dont set one and let the database sort which should show and which shouldn't
            }

            long beforeDownload = Calendar.getInstance().getTimeInMillis();

            for (int i = 0; i < settings.maxTweetsRefresh; i++) {

                try {
                    if (!foundStatus) {
                        paging.setPage(i + 1);
                        List<Status> list = twitter.getHomeTimeline(paging);
                        statuses.addAll(list);

                        if (statuses.size() == 0 || statuses.get(statuses.size() - 1).getId() == id) {
                            Log.v("talon_inserting", "found status");
                            foundStatus = true;
                        } else {
                            Log.v("talon_inserting", "haven't found status");
                            foundStatus = false;
                        }
                    }
                } catch (Exception e) {
                    // the page doesn't exist
                    e.printStackTrace();
                    foundStatus = true;
                } catch (OutOfMemoryError o) {
                    // don't know why...
                }
            }

            long afterDownload = Calendar.getInstance().getTimeInMillis();
            Log.v("talon_inserting", "downloaded " + statuses.size() + " tweets in " + (afterDownload - beforeDownload));

            HashSet hs = new HashSet();
            hs.addAll(statuses);
            statuses.clear();
            statuses.addAll(hs);

            Log.v("talon_inserting", "tweets after hashset: " + statuses.size());

            manualRefresh = false;

            try {
                if (statuses.size() > 0) {
                    launcherRefreshing = false;
                    numberNew = insertTweets(statuses, new long[]{0, 0, 0, 0, 0});
                } else {
                    return 0;
                }
            } catch (NullPointerException e) {
                return 0;
            }

            if (numberNew > 0 && statuses.size() > 0) {
                sharedPrefs.edit().putLong("account_" + currentAccount + "_lastid", statuses.get(0).getId()).commit();
            }

            Log.v("talon_inserting", "inserted " + numberNew + " tweets in " + (Calendar.getInstance().getTimeInMillis() - afterDownload));

            unread = numberNew;
            statuses.clear();

            talonContext.startService(new Intent(talonContext, MentionsRefreshService.class));

            return numberNew;

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }

        return 0;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.v("talon_loader", "creating the loader on account: " + currentAccount);
        try {
            talonContext = context.createPackageContext("com.klinker.android.twitter", Context.CONTEXT_IGNORE_SECURITY);
            sharedPrefs = talonContext.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        } catch (Exception e) {

        }

        String[] projection = HomeDataSource.allColumns;
        CursorLoader cursorLoader = new CursorLoader(
                context,
                HomeContentProvider.CONTENT_URI,
                projection,
                null,
                new String[] {currentAccount + ""},
                null );

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.v("talon_loader", "finished loading");

        Cursor c = null;
        if (cursorAdapter != null) {
            c = cursorAdapter.getCursor();
        }

        cursorAdapter = returnAdapter(cursor);

        try {
            Log.v("talon_databases", "size of adapter cursor on home fragment: " + cursor.getCount());
        } catch (Exception e) {
            e.printStackTrace();
            HomeDataSource.dataSource = null;
            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
        }

        initial = false;

        long id = sharedPrefs.getLong("current_position_" + sharedPrefs.getInt("current_account", 1), 0l);

        int numTweets;
        if (id == 0) {
            numTweets = 0;
        } else {
            numTweets = getPosition(cursor);

            // if it would set it to the end, then we will get the position by the id instead
            if (numTweets > cursor.getCount() - 5) {
                numTweets = getPosition(cursor, id);
                if (numTweets == -1) {
                    return;
                }
            }

            sharedPrefs.edit().putBoolean("just_muted", false).commit();
        }

        final int tweets = numTweets;

        if (spinner.getVisibility() == View.VISIBLE) {
            spinner.setVisibility(View.GONE);
        }

        if (listView.getVisibility() != View.VISIBLE) {
            listView.setVisibility(View.VISIBLE);
        }

        switchedAccounts = false;

        Log.v("talon_loader", "settings adapter");
        listView.setAdapter(cursorAdapter);
        Log.v("talon_loader", "adapter set");

        if (viewPressed) {
            int size;
            size = 0;//(Build.VERSION.SDK_INT >= 19 ? Utils.getStatusBarHeight(context) : 0);
            listView.setSelectionFromTop(liveUnread + (landscape || settings.jumpingWorkaround || isLauncher() ? 1 : 2), size);
        } else if (tweets != 0) {
            unread = tweets;
            int size;
            size = 0;//(Build.VERSION.SDK_INT >= 19 ? Utils.getStatusBarHeight(context) : 0);
            listView.setSelectionFromTop(tweets + (landscape || settings.jumpingWorkaround || isLauncher() ? 1 : 2), size);
        } else {
            listView.setSelectionFromTop(0, 0);
        }

        try {
            c.close();
        } catch (Exception e) {

        }

        liveUnread = 0;
        viewPressed = false;

        if (!launcherRefreshing) {
            refreshLayout.setRefreshing(false);
        }

        isRefreshing = false;

        try {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    newTweets = false;
                }
            }, 500);
        } catch (Exception e) {
            newTweets = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        cursorAdapter.changeCursor(null);
    }

    public void markRead(int currentAccount, Context context, long id) {
        try {
            HomeContentProvider.updateCurrent(currentAccount, context, id);
        } catch (Throwable t) {

        }
    }

    protected Twitter twitter;

    protected AsyncListView listView;
    protected CursorAdapter cursorAdapter;
    protected View toastBar;
    protected TextView toastDescription;
    protected TextView toastButton;
    protected FullScreenSwipeRefreshLayout refreshLayout;
    protected LinearLayout spinner;

    public static BitmapLruCache mCache;

    protected SharedPreferences sharedPrefs;
    protected Activity context;

    protected ActionBar actionBar;
    protected int mActionBarSize;

    protected int currentAccount;

    protected boolean landscape;
    protected boolean isToastShowing = false;
    protected boolean infoBar = false;

    protected String fromTop;
    protected String jumpToTop;
    protected String toMentions;
    protected String allRead;

    protected boolean isHome = false;

    protected View.OnClickListener toTopListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            toTop();
        }
    };

    public BroadcastReceiver jumpTopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toTop();
        }
    };

    public AppSettings settings;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        actionBar = context.getActionBar();
    }

    @Override
    public void onDestroy() {
        try {
            cursorAdapter.getCursor().close();
        } catch (Exception e) {

        }
        super.onDestroy();
    }

    public void setHome() {
        isHome = true;
    }

    public int getCurrentAccount() {
        return sharedPrefs.getInt("current_account", 1);
    }

    public View layout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        setAppSettings();
        setHome();
        getCache();

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        currentAccount = getCurrentAccount();

        SharedPreferences.Editor e = sharedPrefs.edit();
        e.putInt("dm_unread_" + sharedPrefs.getInt("current_account", 1), 0);
        e.putBoolean("refresh_me", false);
        e.commit();

        getStrings();

        try{
            final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                    new int[] { android.R.attr.actionBarSize });
            mActionBarSize = (int) styledAttributes.getDimension(0, 0);
            styledAttributes.recycle();
        } catch (Exception x) {
            // a default just in case i guess...
            mActionBarSize = toDP(48);
        }

        layout = getLayout(inflater);

        setViews(layout);

        setBuilder();

        if (isHome) {
            getCursorAdapter(true);
        } else {
            // delay it a tiny bit so that the main fragment has priority
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getCursorAdapter(true);
                }
            }, 500);
        }
        setUpListScroll();
        setUpToastBar(layout);

        return layout;
    }

    public int toDP(int px) {
        try {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
        } catch (Exception e) {
            return px;
        }
    }

    public void showStatusBar() {
        DrawerActivity.statusBar.setVisibility(View.VISIBLE);
    }

    public void hideStatusBar() {
        DrawerActivity.statusBar.setVisibility(View.GONE);
    }

    public void setUpToastBar(View view) {
        toastBar = view.findViewById(R.id.toastBar);
        toastDescription = (TextView) view.findViewById(R.id.toastDescription);
        toastButton = (TextView) view.findViewById(R.id.toastButton);
        if (settings.addonTheme) {
            LinearLayout toastBackground = (LinearLayout) view.findViewById(R.id.toast_background);
            try {
                toastBackground.setBackgroundColor(Color.parseColor("#DD" + settings.accentColor));
            } catch (Exception e) {
                // they messed up the theme
            }
        }
    }

    public void showToastBar(final String description, final String buttonText, final long length, final boolean quit, final View.OnClickListener listener, boolean isLive) {
        try {
            if (!isToastShowing || isLive) {
                if (isToastShowing) {
                    if (topViewToastShowing) {
                        return;
                    }
                    infoBar = false;
                    hideToastBar(300);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            topViewToastShowing = true;
                            showToastBar(description, buttonText, length, quit, listener, false);
                        }
                    }, 350);
                } else {
                    infoBar = quit;

                    mLength = length;

                    toastDescription.setText(description);
                    toastButton.setText(buttonText);
                    toastButton.setOnClickListener(listener);

                    handler.removeCallbacks(hideToast);
                    isToastShowing = true;
                    toastBar.setVisibility(View.VISIBLE);

                    Animation anim = resHelper.getAnimation("slide_in_right");
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (quit) {
                                handler.postDelayed(hideToast, 3000);
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    anim.setDuration(length);
                    toastBar.startAnimation(anim);
                }
            } else if (!infoBar) {
                // this will change the # from top
                toastDescription.setText(description);
            }
        } catch (Exception e) {
            // fragment not attached
        }
    }

    public void hideToastBar(long length) {
        hideToastBar(length, false);
    }

    public void hideToastBar(long length, boolean force) {
        try {
            mLength = length;

            // quit if the toast isn't showing or it is an info bar, since those will hide automatically
            if (!isToastShowing || infoBar || isHiding) {
                if (force && toastBar.getVisibility() == View.VISIBLE) {

                } else {
                    return;
                }
            }

            Animation anim = resHelper.getAnimation("slide_out_right");
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    isHiding = true;
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    isToastShowing = false;
                    topViewToastShowing = false;
                    infoBar = false;
                    isHiding = false;
                    toastBar.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            anim.setDuration(length);
            toastBar.startAnimation(anim);
        } catch (Exception e) {
            // fragment not attached
        }
    }

    public static final int HOME_REFRESH_ID = 121;

    public int unread;

    public boolean initial = true;
    public boolean newTweets = false;

    private View.OnClickListener toMentionsListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int page1Type = sharedPrefs.getInt("account_" + currentAccount + "_page_1", AppSettings.PAGE_TYPE_NONE);
            int page2Type = sharedPrefs.getInt("account_" + currentAccount + "_page_2", AppSettings.PAGE_TYPE_NONE);

            int extraPages = 0;
            if (page1Type != AppSettings.PAGE_TYPE_NONE) {
                extraPages++;
            }

            if (page2Type != AppSettings.PAGE_TYPE_NONE) {
                extraPages++;
            }

            MainActivity.mViewPager.setCurrentItem(1 + extraPages, true);
            hideToastBar(400);
        }
    };

    protected View.OnClickListener liveStreamRefresh = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            newTweets = false;
            viewPressed = true;
            trueLive = true;
            manualRefresh = false;
            resetTimeline(false);
            listView.setSelectionFromTop(0, 0);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    infoBar = false;
                    hideToastBar(400);
                }
            }, 500);

            context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));
        }
    };

    public int liveUnread = 0;
    public boolean loadToTop = false;

    public BroadcastReceiver pullReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (!isLauncher()) {
                if (listView.getFirstVisiblePosition() == 0) {
                    // we want to automatically show the new one if the user is at the top of the list
                    // so we set the current position to the id of the top tweet

                    context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));

                    sharedPrefs.edit().putBoolean("refresh_me", false).commit();
                    final long id = sharedPrefs.getLong("account_" + currentAccount + "_lastid", 0l);
                    sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // sleep so that everyting loads correctly
                            try {
                                Thread.sleep(2000);
                            } catch (Exception e) {

                            }
                            HomeDataSource.getInstance(context).markPosition(currentAccount, id);
                            //HomeContentProvider.updateCurrent(currentAccount, context, id);

                            trueLive = true;
                            loadToTop = true;

                            resetTimeline(false);
                        }
                    }).start();

                } else {
                    liveUnread++;
                    sharedPrefs.edit().putBoolean("refresh_me", false).commit();
                    if (liveUnread != 0) {
                        try {
                            showToastBar(liveUnread + " " + (liveUnread == 1 ? getResources().getString(R.string.new_tweet) : getResources().getString(R.string.new_tweets)),
                                    getResources().getString(R.string.view),
                                    400,
                                    true,
                                    liveStreamRefresh,
                                    true);
                        } catch (Exception e) {
                            // fragment not attached to activity
                        }
                    }

                    newTweets = true;
                }
            }
        }
    };

    public BroadcastReceiver markRead = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            markReadForLoad();
            if (settings.tweetmarker) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        TweetMarkerHelper helper = new TweetMarkerHelper(currentAccount,
                                sharedPrefs.getString("twitter_screen_name_" + currentAccount, ""),
                                Utils.getTwitter(context, new AppSettings(context)),
                                sharedPrefs);

                        long currentId = sharedPrefs.getLong("current_position_" + currentAccount, 0l);
                        helper.sendCurrentId("timeline", currentId);

                    }
                }).start();
            }
        }
    };

    public BroadcastReceiver homeClosed = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Log.v("talon_home_frag", "home closed broadcast received on home fragment");
            if (!dontGetCursor) {
                resetTimeline(true);
            }
            dontGetCursor = false;
        }
    };

    public void getCursorAdapter(boolean showSpinner) {

        Thread getCursor = new Thread(new Runnable() {
            @Override
            public void run() {

                if (!trueLive && !initial) {
                    Log.v("talon_tweetmarker", "true live");
                    markReadForLoad();
                }

                final Cursor cursor;
                try {
                    cursor = HomeDataSource.getInstance(context).getCursor(currentAccount);
                } catch (Exception e) {
                    Log.v("talon_home_frag", "caught getting the cursor on the home timeline, sending reset home");
                    HomeDataSource.dataSource = null;
                    context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
                    return;
                }

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Cursor c = null;
                        if (cursorAdapter != null) {
                            c = cursorAdapter.getCursor();
                        }

                        cursorAdapter = returnAdapter(cursor);

                        try {
                            Log.v("talon_databases", "size of adapter cursor on home fragment: " + cursor.getCount());
                        } catch (Exception e) {
                            e.printStackTrace();
                            HomeDataSource.dataSource = null;
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
                            return;
                        }

                        initial = false;

                        long id = sharedPrefs.getLong("current_position_" + currentAccount, 0l);
                        boolean update = true;
                        int numTweets;
                        if (id == 0 || loadToTop) {
                            numTweets = 0;
                            loadToTop = false;
                        } else {
                            numTweets = getPosition(cursor);

                            // if it would set it to the end, then we will get the position by the id instead
                            if (numTweets > cursor.getCount() - 5) {
                                numTweets = getPosition(cursor, id);
                                if (numTweets == -1) {
                                    return;
                                }
                            }

                            sharedPrefs.edit().putBoolean("just_muted", false).commit();
                        }

                        final int tweets = numTweets;

                        if (spinner.getVisibility() == View.VISIBLE) {
                            spinner.setVisibility(View.GONE);
                        }

                        if (listView.getVisibility() != View.VISIBLE) {
                            listView.setVisibility(View.VISIBLE);
                        }

                        try {
                            listView.setAdapter(cursorAdapter);
                        } catch (Exception e) {
                            // happens when coming from the launcher sometimes because database has been closed
                            HomeDataSource.dataSource = null;
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
                            return;
                        }

                        if (viewPressed) {
                            int size;
                            if (!isLauncher()) {
                                size = mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
                            } else {
                                size = (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
                            }
                            listView.setSelectionFromTop(liveUnread + (MainActivity.isPopup || landscape || MainActivity.settings.jumpingWorkaround || isLauncher() ? 1 : 2), size);
                        } else if (tweets != 0) {
                            unread = tweets;
                            int size;
                            if (!isLauncher()) {
                                size = mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
                            } else {
                                size = (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
                            }
                            listView.setSelectionFromTop(tweets + (MainActivity.isPopup || landscape || MainActivity.settings.jumpingWorkaround || isLauncher() ? 1 : 2), size);
                        } else {
                            listView.setSelectionFromTop(0, 0);
                        }

                        try {
                            c.close();
                        } catch (Exception e) {

                        }

                        liveUnread = 0;
                        viewPressed = false;

                        refreshLayout.setRefreshing(false);

                        isRefreshing = false;

                        try {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    newTweets = false;
                                }
                            }, 500);
                        } catch (Exception e) {
                            newTweets = false;
                        }
                    }
                });
            }
        });

        getCursor.setPriority(8);
        getCursor.start();


    }

    public void toTop() {
        // used so the content observer doesn't change the shared pref we just put in
        trueLive = true;
        try {
            if (listView.getFirstVisiblePosition() > 40) {
                listView.setSelection(0);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideToastBar(400);
                    }
                }, 250);
            } else {
                listView.smoothScrollToPosition(0);
            }
        } catch (Exception e) {
            listView.smoothScrollToPosition(0);
        }
    }

    public boolean manualRefresh = false;
    public boolean dontGetCursor = false;

    public boolean getTweet() {

        TweetMarkerHelper helper = new TweetMarkerHelper(currentAccount,
                sharedPrefs.getString("twitter_screen_name_" + currentAccount, ""),
                Utils.getTwitter(context, new AppSettings(context)),
                sharedPrefs);

        boolean updated = helper.getLastStatus("timeline", context);

        Log.v("talon_tweetmarker", "tweetmarker status: " + updated);

        if (updated) {
            //HomeContentProvider.updateCurrent(currentAccount, context, sharedPrefs.getLong("current_position_" + currentAccount, 0l));
            trueLive = true;
            return true;
        } else {
            return false;
        }
    }

    public void fetchTweetMarker() {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                if (!isLauncher() && !actionBar.isShowing()) {
                    showStatusBar();
                    actionBar.show();
                }

                try {
                    refreshLayout.setRefreshing(true);
                } catch (Exception e) {
                    // same thing
                }
                MainActivity.canSwitch = false;
                isRefreshing = true;
            }

            @Override
            protected Boolean doInBackground(Void... params) {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                }

                return getTweet();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                hideToastBar(400);

                MainActivity.canSwitch = true;

                if (result) {
                    resetTimeline(false);
                } else {
                    refreshLayout.setRefreshing(false);
                    isRefreshing = false;
                }

            }
        }.execute();
    }

    public String sNewTweet;
    public String sNewTweets;
    public String sNoNewTweets;
    public String sNewMention;
    public String sNewMentions;

    public int numberNew;
    public boolean tweetMarkerUpdate;

    public boolean isRefreshing = false;

    public void onRefreshStarted() {
        if (isRefreshing) {
            return;
        } else {
            isRefreshing = true;
        }

        DrawerActivity.canSwitch = false;

        Thread refresh = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v("talon_fragment", "started the refresh thread");
                numberNew = doRefresh();
                Log.v("talon_fragment", "finished the doRefresh method");

                tweetMarkerUpdate = false;

                if (settings.tweetmarker && refreshTweetmarker) {
                    tweetMarkerUpdate = getTweet();
                    Log.v("talon_tweetmarker", "tweet marker update " + tweetMarkerUpdate);
                }

                refreshTweetmarker = false;

                final boolean result = numberNew > 0 || tweetMarkerUpdate;

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setStrings();

                            if (result) {
                                Log.v("talon_home_frag", "getting cursor adapter in onrefreshstarted");
                                resetTimeline(false);

                                if (unread > 0) {
                                    final CharSequence text;

                                    text = numberNew == 1 ?  numberNew + " " + sNewTweet :  numberNew + " " + sNewTweets;

                                    if (!tweetMarkerUpdate) {
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    Looper.prepare();
                                                } catch (Exception e) {
                                                    // just in case
                                                }
                                                isToastShowing = false;
                                                showToastBar(text + "", jumpToTop, 400, true, toTopListener, false);
                                            }
                                        }, 500);
                                    }
                                }
                            } else {
                                final CharSequence text = sNoNewTweets;
                                if (!settings.tweetmarker) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Looper.prepare();
                                            } catch (Exception e) {
                                                // just in case
                                            }
                                            isToastShowing = false;
                                            showToastBar(text + "", allRead, 400, true, toTopListener, false);
                                        }
                                    }, 500);
                                }

                                refreshLayout.setRefreshing(false);
                                isRefreshing = false;
                            }

                            DrawerActivity.canSwitch = true;

                            newTweets = false;
                        } catch (Exception e) {
                            DrawerActivity.canSwitch = true;

                            try {
                                refreshLayout.setRefreshing(false);
                            } catch (Exception x) {
                                // not attached to the activity i guess, don't know how or why that would be though
                            }
                            isRefreshing = false;
                        }
                    }
                });
            }
        });

        refresh.setPriority(7);
        refresh.start();
    }

    public boolean justStarted = false;
    public Handler waitOnRefresh = new Handler();
    public Runnable applyRefresh = new Runnable() {
        @Override
        public void run() {
            sharedPrefs.edit().putBoolean("should_refresh", true).commit();
        }
    };

    @Override
    public void onPause() {

        context.unregisterReceiver(pullReceiver);
        context.unregisterReceiver(markRead);
        context.unregisterReceiver(homeClosed);

        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.NEW_TWEET");
        context.registerReceiver(pullReceiver, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.RESET_HOME");
        context.registerReceiver(homeClosed, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.MARK_POSITION");
        context.registerReceiver(markRead, filter);

        if (isLauncher()) {
            return;
        }

        if (sharedPrefs.getBoolean("refresh_me", false)) { // this will restart the loader to display the new tweets
            //getLoaderManager().restartLoader(0, null, HomeFragment.this);
            Log.v("talon_home_frag", "getting cursor adapter in on resume");
            resetTimeline(true);
            sharedPrefs.edit().putBoolean("refresh_me", false).commit();
        }
    }

    public boolean refreshTweetmarker = false;

    public boolean trueLive = false;
    public boolean viewPressed = false;

    // use the cursor to find which one has "1" in current position column
    public int getPosition(Cursor cursor) {
        int pos = 0;

        try {
            if (cursor.moveToLast()) {
                String s;
                do {
                    s = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_CURRENT_POS));
                    if (s != null && !s.isEmpty()) {
                        break;
                    } else {
                        pos++;
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {
            Log.v("talon_home_frag", "caught getting position on home timeline, getting the cursor adapter again");
            e.printStackTrace();
            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
            return -1;
        }

        return pos;
    }

    // find the id from the cursor to get the position
    public int getPosition(Cursor cursor, long id) {
        int pos = 0;

        try {
            if (cursor.moveToLast()) {
                do {
                    if (cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) == id) {
                        break;
                    } else {
                        pos++;
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {
            Log.v("talon_home_frag", "caught getting position on home timeline, getting the cursor adapter again");
            e.printStackTrace();
            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
            return -1;
        }

        return pos;
    }

    public Handler handler = new Handler();
    public Runnable hideToast = new Runnable() {
        @Override
        public void run() {
            infoBar = false;
            hideToastBar(mLength);
        }
    };
    public long mLength;


    public boolean topViewToastShowing = false;

    public boolean isHiding = false;

    public void markReadForLoad() {
        try {
            final Cursor cursor = cursorAdapter.getCursor();
            final int current = listView.getFirstVisiblePosition();

            if (cursor.isClosed()) {
                return;
            }

            if (!isLauncher()) {
                HomeDataSource.getInstance(context).markAllRead(currentAccount);
            }

            if (cursor.moveToPosition(cursor.getCount() - current)) {
                Log.v("talon_marking_read", cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) + "");
                final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        markRead(currentAccount, context, id);
                    }
                }).start();
            } else {
                if (cursor.moveToLast()) {
                    final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                    sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            markRead(currentAccount, context, id);
                        }
                    }).start();
                }
            }
        } catch (IllegalStateException e) {
            // Home datasource is not open, so we manually close it to null out values and reset it
            e.printStackTrace();
            try {
                HomeDataSource.dataSource = null;
            } catch (Exception x) {

            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            // the cursoradapter is null
        } catch (StaleDataException e) {
            e.printStackTrace();
            // do nothing here i guess
        }
    }
}
