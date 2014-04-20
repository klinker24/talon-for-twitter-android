package com.klinker.android.twitter.widget.launcher_fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.klinker.android.launcher.api.ResourceHelper;
import com.klinker.android.twitter.adapters.MainDrawerArrayAdapter;
import com.klinker.android.twitter.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.manipulations.widgets.swipe_refresh_layout.FullScreenSwipeRefreshLayout;
import com.klinker.android.twitter.manipulations.widgets.swipe_refresh_layout.SwipeProgressBar;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.ui.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter.ui.profile_viewer.ProfilePager;
import com.klinker.android.twitter.ui.setup.LoginActivity;
import com.klinker.android.twitter.utils.ImageUtils;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;
import java.util.Arrays;


public class LauncherFragment extends HomeFragment {

    public ImageView backgroundPic;
    public ImageView profilePic;
    public ListView drawerList;
    public View statusBar;

    public ResourceHelper resHelper;

    @Override
    public View getLayout(LayoutInflater inflater) {
        return resHelper.getLayout("launcher_frag");
    }

    public CursorAdapter returnAdapter(Cursor c) {
        return new LauncherTimelineCursorAdapter(context, c, false, true);
    }

    @Override
    public void getCache() {
        mCache = com.klinker.android.twitter.data.App.getInstance(context).getBitmapCache();
    }
    
    @Override
    public void setUpListScroll() {

        if (DrawerActivity.settings.useToast) {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {

                int mLastFirstVisibleItem = 0;

                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                    if (i == SCROLL_STATE_IDLE) {
                        MainActivity.sendHandler.removeCallbacks(MainActivity.hideSend);
                        MainActivity.sendHandler.postDelayed(MainActivity.showSend, 600);
                    } else {
                        MainActivity.sendHandler.removeCallbacks(MainActivity.showSend);
                        MainActivity.sendHandler.postDelayed(MainActivity.hideSend, 300);
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                    if (DrawerActivity.settings.uiExtras) {
                        if (firstVisibleItem != 0) {
                            if (MainActivity.canSwitch) {
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
                    if (newTweets && firstVisibleItem == 0 && DrawerActivity.settings.liveStreaming) {
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
                        MainActivity.sendHandler.removeCallbacks(MainActivity.hideSend);
                        MainActivity.sendHandler.postDelayed(MainActivity.showSend, 600);
                    } else {
                        MainActivity.sendHandler.removeCallbacks(MainActivity.showSend);
                        MainActivity.sendHandler.postDelayed(MainActivity.hideSend, 300);
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                    if (newTweets && firstVisibleItem == 0 && (DrawerActivity.settings.liveStreaming)) {
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

                    if (DrawerActivity.settings.uiExtras) {
                        if (firstVisibleItem != 0) {
                            if (MainActivity.canSwitch) {
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
        if (barShowing) {
            return;
        } else {
            barShowing = true;
        }
        Animation anim = resHelper.getAnimation("fade_in");//AnimationUtils.loadAnimation(context, R.anim.fade_in);
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
        if (!barShowing) {
            return;
        } else {
            barShowing = false;
        }
        Animation anim = resHelper.getAnimation("fade_out");//AnimationUtils.loadAnimation(context, R.anim.fade_out);
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
        sNewTweet = resHelper.getString("new_tweets");
        sNoNewTweets = resHelper.getString("no_new_tweets");
        sNewMention = resHelper.getString("new_mention");
        sNewMentions = resHelper.getString("new_mentions");
    }

    public boolean logoutVisible = false;

    @Override
    public void setUpHeaders() {
        if (Build.VERSION.SDK_INT >= 19) {
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

    @Override
    public void setViews(View layout) {
        //super.setViews(layout);

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
                onRefreshStarted();
            }
        });

        root.addView(refreshLayout);
        refreshLayout.addView(listView);

        if (DrawerActivity.settings.addonTheme) {
            refreshLayout.setColorScheme(DrawerActivity.settings.accentInt,
                    SwipeProgressBar.COLOR2,
                    DrawerActivity.settings.accentInt,
                    SwipeProgressBar.COLOR3);
        } else {
            if (DrawerActivity.settings.theme != AppSettings.THEME_LIGHT) {
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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                context.getActionBar().hide();
                hideStatusBar();
            }
        }, 1500);

        final AppSettings settings = AppSettings.getInstance(context);

        backgroundPic = (ImageView) layout.findViewById(resHelper.getId("background_image"));
        profilePic = (ImageView) layout.findViewById(resHelper.getId("profile_pic_contact"));
        drawerList = (ListView) layout.findViewById(resHelper.getId("drawer_list"));
        final ImageButton showMoreDrawer = (ImageButton) layout.findViewById(resHelper.getId("options"));
        final ImageView proPic2 = (ImageView) layout.findViewById(resHelper.getId("profile_pic_2"));
        final LinearLayout logoutLayout = (LinearLayout) layout.findViewById(resHelper.getId("logoutLayout"));

        final String backgroundUrl = settings.myBackgroundUrl;
        final String profilePicUrl = settings.myProfilePicUrl;
        statusBar = layout.findViewById(resHelper.getId("activity_status_bar"));

        int statusBarHeight = Utils.getStatusBarHeight(context);

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
            if(settings.roundContactImages) {
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
                    Animation ranim = resHelper.getAnimation("drawer_rotate");//AnimationUtils.loadAnimation(context, R.anim.drawer_rotate);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    Animation anim = resHelper.getAnimation("fade_out");//AnimationUtils.loadAnimation(context, R.anim.fade_out);
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

                    Animation anim2 = resHelper.getAnimation("fade_in");//AnimationUtils.loadAnimation(context, R.anim.fade_in);
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
                    Animation ranim = resHelper.getAnimation("drawer_rotate_back");//AnimationUtils.loadAnimation(context, R.anim.drawer_rotate_back);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    Animation anim = resHelper.getAnimation("fade_in");//AnimationUtils.loadAnimation(context, R.anim.fade_in);
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

                    Animation anim2 = resHelper.getAnimation("fade_out");//AnimationUtils.loadAnimation(context, R.anim.fade_out);
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
                        Intent viewProfile = new Intent(context, ProfilePager.class);
                        viewProfile.putExtra("name", settings.myName);
                        viewProfile.putExtra("screenname", settings.myScreenName);
                        viewProfile.putExtra("proPic", profilePicUrl);
                        viewProfile.putExtra("tweetid", 0);
                        viewProfile.putExtra("retweet", false);
                        viewProfile.putExtra("long_click", false);

                        context.startActivity(viewProfile);
                    }
                }, 400);
            }
        });

        backgroundPic.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent viewProfile = new Intent(context, ProfilePager.class);
                        viewProfile.putExtra("name", settings.myName);
                        viewProfile.putExtra("screenname", settings.myScreenName);
                        viewProfile.putExtra("proPic", profilePicUrl);
                        viewProfile.putExtra("tweetid", 0);
                        viewProfile.putExtra("retweet", false);
                        viewProfile.putExtra("long_click", true);

                        context.startActivity(viewProfile);
                    }
                }, 400);

                return false;
            }
        });

        MainDrawerArrayAdapter adapter = new MainDrawerArrayAdapter(context,
                new ArrayList<String>(Arrays.asList(MainDrawerArrayAdapter.getItems(context))));
        drawerList.setAdapter(adapter);

        if (Utils.hasNavBar(context)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
            footer.setLayoutParams(params);
            drawerList.addFooterView(footer);
            drawerList.setFooterDividersEnabled(false);
        }

        //drawerList.setOnItemClickListener(new MainDrawerClickListener(context, null, null));

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
            proPic2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (MainActivity.canSwitch) {
                        if (current == 1) {
                            sharedPrefs.edit().putInt("current_account", 2).commit();
                        } else {
                            sharedPrefs.edit().putInt("current_account", 1).commit();
                        }
                        context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                        context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION"));

                        Intent login = new Intent(context, LoginActivity.class);
                        AppSettings.invalidate();
                        startActivity(login);
                    }
                }
            });
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
                        if (MainActivity.canSwitch) {
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION").putExtra("current_account", current));

                            Toast.makeText(context, "Preparing to switch", Toast.LENGTH_SHORT).show();

                            // we want to wait a second so that the mark position broadcast will work
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {

                                    }
                                    sharedPrefs.edit().putInt("current_account", 2).commit();
                                    sharedPrefs.edit().remove("new_notifications").remove("new_retweets").remove("new_favorites").remove("new_follows").commit();
                                    AppSettings.invalidate();
                                    Intent next = new Intent(context, MainActivity.class);
                                    startActivity(next);
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
                        if (MainActivity.canSwitch) {
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION").putExtra("current_account", current));

                            Toast.makeText(context, "Preparing to switch", Toast.LENGTH_SHORT).show();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {

                                    }

                                    sharedPrefs.edit().putInt("current_account", 1).commit();
                                    sharedPrefs.edit().remove("new_notifications").remove("new_retweets").remove("new_favorites").remove("new_follows").commit();
                                    AppSettings.invalidate();
                                    Intent next = new Intent(context, MainActivity.class);
                                    startActivity(next);
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
}
