package com.klinker.android.twitter.widget.launcher_fragment;

import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.klinker.android.launcher.api.BaseLauncherPage;
import com.klinker.android.launcher.api.ResourceHelper;
import com.klinker.android.twitter.adapters.LauncherListLoader;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter.manipulations.widgets.swipe_refresh_layout.FullScreenSwipeRefreshLayout;
import com.klinker.android.twitter.manipulations.widgets.swipe_refresh_layout.SwipeProgressBar;
import com.klinker.android.twitter.services.CatchupPull;
import com.klinker.android.twitter.services.MentionsRefreshService;
import com.klinker.android.twitter.services.TimelineRefreshService;
import com.klinker.android.twitter.services.WidgetRefreshService;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
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
import java.util.HashSet;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class LauncherFragment extends com.klinker.android.twitter.ui.main_fragments.home_fragments.HomeFragment
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

    @Override
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

    @Override
    public void resetTimeline(boolean spinner) {
        // don't need this because of the load manager
        //context.getLoaderManager().restartLoader(0, null, this);
    }
    
    @Override
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

    @Override
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

    @Override
    public boolean isLauncher() {
        return true;
    }

    public boolean scrolled = false;

    @Override
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
                                    showToastBar(firstVisibleItem + " " + fromTop, jumpToTop, 400, false, toTopListener);
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

    @Override
    public void getStrings() {
        fromTop = resHelper.getString("from_top");
        jumpToTop = resHelper.getString("jump_to_top");
        allRead = resHelper.getString("all_read");
        toMentions = resHelper.getString("mentions");
    }

    @Override
    public void setStrings() {
        resHelper = new ResourceHelper(getActivity(), "com.klinker.android.twitter");
        sNewTweet = resHelper.getString("new_tweet");
        sNewTweets = resHelper.getString("new_tweets");
        sNoNewTweets = resHelper.getString("no_new_tweets");
        sNewMention = resHelper.getString("new_mention");
        sNewMentions = resHelper.getString("new_mentions");
    }

    public boolean logoutVisible = false;

    @Override
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

    @Override
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

                markReadForLoad();

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

    public boolean isHiding = false;

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

    public void setBuilder() {
        LauncherListLoader loader = new LauncherListLoader(mCache, context, false);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
        builder.setThreadPoolSize(2);

        listView.setItemManager(builder.build());
    }

    @Override
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

    @Override
    public void markRead(int currentAccount, Context context, long id) {
        try {
            HomeContentProvider.updateCurrent(currentAccount, context, id);
        } catch (Throwable t) {

        }
    }
}
