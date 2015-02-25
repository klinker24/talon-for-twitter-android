package com.klinker.android.twitter_l.ui.main_fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.CursorListLoader;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.FullScreenSwipeRefreshLayout;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.SwipeProgressBar;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.utils.Expandable;
import com.klinker.android.twitter_l.utils.ExpansionViewHelper;
import com.klinker.android.twitter_l.utils.SystemBarVisibility;
import com.klinker.android.twitter_l.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import twitter4j.Twitter;
import uk.co.senab.bitmapcache.BitmapLruCache;

public abstract class MainFragment extends Fragment implements Expandable {

    protected Twitter twitter;

    protected AsyncListView listView;
    protected CursorAdapter cursorAdapter;
    protected View toastBar;
    protected TextView toastDescription;
    protected TextView toastButton;
    protected MaterialSwipeRefreshLayout refreshLayout;
    protected LinearLayout spinner;

    public static BitmapLruCache mCache;

    protected SharedPreferences sharedPrefs;
    protected Activity context;

    protected android.support.v7.app.ActionBar actionBar;
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
            hideToastBar(300);
        }
    };

    public BroadcastReceiver jumpTopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toTop();
            hideToastBar(300);
        }
    };
    public BroadcastReceiver showToast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (listView.getFirstVisiblePosition() > 3) {
                //showToastBar(listView.getFirstVisiblePosition() + " " + fromTop, jumpToTop, 300, false, toTopListener);
            }
        }
    };
    public BroadcastReceiver hideToast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //hideToastBar(300);
        }
    };

    public AppSettings settings;
    
    public void setAppSettings() {
        settings = AppSettings.getInstance(context);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.TOP_TIMELINE");
        context.registerReceiver(jumpTopReceiver, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.SHOW_TOAST");
        context.registerReceiver(showToast, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.HIDE_TOAST");
        context.registerReceiver(hideToast, filter);
    }

    @Override
    public void onPause() {

        context.unregisterReceiver(jumpTopReceiver);
        context.unregisterReceiver(showToast);
        context.unregisterReceiver(hideToast);

        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        actionBar = ((ActionBarActivity)context).getSupportActionBar();
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
        isHome = false;
    }

    public View getLayout(LayoutInflater inflater) {
        return inflater.inflate(R.layout.main_fragments, null);
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

    public boolean isLauncher() {
        return false;
    }

    public void getCache() {
        mCache = App.getInstance(context).getBitmapCache();
    }

    public void getStrings() {
        fromTop = getResources().getString(R.string.tweets);
        jumpToTop = getResources().getString(R.string.to_top);
        allRead = getResources().getString(R.string.all_read);
        toMentions = getResources().getString(R.string.mentions);
    }

    protected void setSpinner(View layout) {
        spinner = (LinearLayout) layout.findViewById(R.id.spinner);
    }

    public void setViews(View layout) {

        listView = (AsyncListView) layout.findViewById(R.id.listView);
        setSpinner(layout);

        refreshLayout = (MaterialSwipeRefreshLayout) layout.findViewById(R.id.swipe_refresh_layout);
        refreshLayout.setOnRefreshListener(new MaterialSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshStarted();
            }
        });
        int size = Utils.getActionBarHeight(context) + (landscape || MainActivity.isPopup ? 0 : Utils.getStatusBarHeight(context));

        if (!getResources().getBoolean(R.bool.isTablet) && !landscape) {
            refreshLayout.setProgressViewOffset(false, 0, size + toDP(25));
        }

        refreshLayout.setColorSchemeColors(settings.themeColors.accentColor, settings.themeColors.primaryColor);
        refreshLayout.setBarVisibilityWatcher(barVisibility);

        setUpHeaders();

    }

    boolean moveActionBar = true;
    public void setUpListScroll() {
        final boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        if (isTablet || landscape) {
            moveActionBar = false;
        }

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            int oldFirstVisibleItem = 0;
            int oldTop = 0;
            Handler top;

            @Override
            public void onScrollStateChanged(final AbsListView absListView, final int i) {
                if (top == null) {
                    top = new Handler();
                }
                top.removeCallbacksAndMessages(null);
                top.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (i == SCROLL_STATE_IDLE && absListView.getFirstVisiblePosition() == 0 && canUseScrollStuff) {
                            showStatusBar();
                            MainActivity.sendHandler.post(MainActivity.showSend);
                        } else if (absListView.getFirstVisiblePosition() == 0 && !toTopPressed) {
                            MainActivity.sendHandler.post(MainActivity.showSend);
                        }
                    }
                }, 350);

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int onScreen, int total) {
                if (!canUseScrollStuff) {

                    if (expansionHelper != null) {
                        expansionHelper.hidePopups();
                    }

                    return;
                }

                View view = absListView.getChildAt(0);
                int top = (view == null) ? 0 : view.getTop();

                if (!settings.topDown) {
                    if (firstVisibleItem == oldFirstVisibleItem) {
                        if (top > oldTop) {
                            // scrolling up
                            scrollUp();
                        } else if (top < oldTop) {
                            // scrolling down
                            scrollDown();
                        }
                    } else {
                        if (firstVisibleItem < oldFirstVisibleItem) {
                            // scrolling up
                            scrollUp();
                        } else {
                            // scrolling down
                            scrollDown();
                        }
                    }
                } else {
                    if (firstVisibleItem == 0) {
                        hideToastBar(300);
                    } else {
                        if (firstVisibleItem == oldFirstVisibleItem) {
                            if (top > oldTop) {
                                // scrolling up
                                scrollDown();
                            } else if (top < oldTop) {
                                // scrolling down
                                scrollUp();
                            }
                        } else {
                            if (firstVisibleItem < oldFirstVisibleItem) {
                                // scrolling up
                                scrollDown();
                            } else {
                                // scrolling down
                                scrollUp();
                            }
                        }
                    }
                }

                oldTop = top;
                oldFirstVisibleItem = firstVisibleItem;
            }
        });
    }

    public void setUpHeaders() {
        if (!getResources().getBoolean(R.bool.isTablet) && !landscape) {
            View viewHeader = context.getLayoutInflater().inflate(R.layout.ab_header, null);
            listView.addHeaderView(viewHeader, null, false);
            listView.setHeaderDividersEnabled(false);
        }

        if (DrawerActivity.translucent) {
            if (Utils.hasNavBar(context)) {
                View footer = new View(context);
                footer.setOnClickListener(null);
                footer.setOnLongClickListener(null);
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
                footer.setLayoutParams(params);
                listView.addFooterView(footer);
                listView.setFooterDividersEnabled(false);
            }

            if (!MainActivity.isPopup && !getResources().getBoolean(R.bool.isTablet) && !landscape) {
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
    }

    public void setBuilder() {

    }

    public boolean toTopPressed = false;
    public void toTop() {
        canUseScrollStuff = false;
        toTopPressed = true;
        showStatusBar();
        hideToastBar(300);

        try {
            if (listView.getFirstVisiblePosition() > 40) {
                listView.setSelection(0);
            } else {
                listView.smoothScrollToPosition(0);
            }
        } catch (Exception e) {
            listView.smoothScrollToPosition(0);
        }

        if (expansionHandler == null) {
            expansionHandler = new Handler();
        }

        expansionHandler.removeCallbacksAndMessages(null);
        expansionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                canUseScrollStuff = true;
            }
        }, 3000);
    }

    public void onRefreshStarted() {
        //mPullToRefreshLayout.setRefreshing(true);
        getCursorAdapter(false);
    }

    public abstract void getCursorAdapter(boolean showSpinner);

    public int toDP(int px) {
        try {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
        } catch (Exception e) {
            return px;
        }
    }

    public Handler upHandler;
    public Handler downHandler;

    public boolean emptyUpHandler = true;
    public boolean emptyDownHandler = true;

    public void scrollUp() {
        if (upHandler == null) {
            upHandler = new Handler();
            downHandler = new Handler();
        }

        downHandler.removeCallbacksAndMessages(null);
        emptyDownHandler = true;

        if (emptyUpHandler) {
            emptyUpHandler = false;
            upHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (moveActionBar) {
                        hideStatusBar();
                    }

                    MainActivity.sendHandler.removeCallbacks(null);
                    MainActivity.sendHandler.post(MainActivity.hideSend);

                    hideToastBar(300);
                }
            }, 300);
        }

    }

    public void scrollDown() {
        if (upHandler == null) {
            upHandler = new Handler();
            downHandler = new Handler();
        }

        upHandler.removeCallbacksAndMessages(null);
        emptyUpHandler = true;

        if (emptyDownHandler) {
            emptyDownHandler = false;
            downHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (moveActionBar) {
                        showStatusBar();
                    }

                    MainActivity.sendHandler.removeCallbacks(null);
                    MainActivity.sendHandler.post(MainActivity.showSend);

                    int first = listView.getFirstVisiblePosition();
                    if (first > 3) {
                        showToastBar(first + " " + fromTop, jumpToTop, 300, false, toTopListener);
                    }
                }
            }, 300);
        }

        toastDescription.setText(listView.getFirstVisiblePosition() + " " + fromTop);
    }

    SystemBarVisibility barVisibility;

    public void addSystemBarVisibility(SystemBarVisibility watcher) {
        this.barVisibility = watcher;
    }

    public boolean statusIsShown = true;
    public boolean ignoreVisibilityChange = false;
    public Handler visibilityChange =  null;
    public Runnable change = new Runnable() {
        @Override
        public void run() {
            ignoreVisibilityChange = false;
        }
    };

    public void showStatusBar() {
        if (visibilityChange == null) {
            visibilityChange = new Handler();
        }
        if (!statusIsShown && !ignoreVisibilityChange && barVisibility != null) {
            statusIsShown = true;
            ignoreVisibilityChange = true;
            visibilityChange.postDelayed(change, 250);
            barVisibility.showBars();
        }
    }

    public void hideStatusBar() {
        if (visibilityChange == null) {
            visibilityChange = new Handler();
        }

        if (!refreshLayout.isRefreshing() && statusIsShown && !ignoreVisibilityChange && barVisibility != null) {
            statusIsShown = false;
            ignoreVisibilityChange = true;
            visibilityChange.postDelayed(change, 250);
            barVisibility.hideBars();
        }
}

    public void setUpToastBar(View view) {
        toastBar = view.findViewById(R.id.toastBar);
        toastDescription = (TextView) view.findViewById(R.id.toastDescription);
        toastButton = (TextView) view.findViewById(R.id.toastButton);

        toastButton.setTextColor(settings.themeColors.accentColorLight);
    }

    public Handler removeToastHandler;
    public void showToastBar(String description, String buttonText, final long length, final boolean quit, View.OnClickListener listener) {
        if (removeToastHandler == null) {
            removeToastHandler = new Handler();
        } else {
            removeToastHandler.removeCallbacksAndMessages(null);
        }

        if (!settings.useSnackbar) {
            return;
        }

        toastDescription.setText(description);
        toastButton.setText(buttonText);
        toastButton.setOnClickListener(listener);

        isToastShowing = true;

        if (toastBar.getVisibility() != View.VISIBLE) {
            toastBar.setVisibility(View.VISIBLE);

            Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (quit) {
                        infoBar = true;
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (quit) {
                        removeToastHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideToastBar(length);
                                infoBar = false;
                            }
                        }, 3000);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            anim.setDuration(length);
            toastBar.startAnimation(anim);
        }
    }

    public void hideToastBar(long length) {
        if (!isToastShowing || !settings.useSnackbar) {
            return;
        }

        if (removeToastHandler == null) {
            removeToastHandler.removeCallbacksAndMessages(null);
        }

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isToastShowing = false;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                toastBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(length);
        toastBar.startAnimation(anim);
    }

    public void updateToastText(String text) {
        toastDescription.setText(text);
    }

    public boolean allowBackPress() {

        if (expansionHelper != null) {
            if (expansionHelper.hidePopups()) {
                return false;
            }
        }

        if (background != null) {
            if (!settings.bottomPictures) {
                background.performClick();
            } else {
                background.performLongClick();
            }
            return false;
        }

        return true;
    }

    private int expandedDistanceFromTop = 0;
    protected boolean canUseScrollStuff = true;
    private Handler expansionHandler;
    private View background;
    private ExpansionViewHelper expansionHelper;

    @Override
    public void expandViewOpen(int distanceFromTop, int position, View root, ExpansionViewHelper helper) {
        if (landscape) {
            distanceFromTop = distanceFromTop + Utils.getStatusBarHeight(context);
        }

        if (expansionHandler == null) {
            expansionHandler = new Handler();
        }
        expansionHandler.removeCallbacks(null);

        background = root;
        expansionHelper = helper;

        canUseScrollStuff = false;
        expandedDistanceFromTop = distanceFromTop;

        MainActivity.sendHandler.removeCallbacks(null);
        MainActivity.sendHandler.post(MainActivity.hideSend);

        hideToastBar(300);

        if (getResources().getBoolean(R.bool.isTablet) || landscape) {
            listView.smoothScrollBy(distanceFromTop - Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context), TimeLineCursorAdapter.ANIMATION_DURATION);
        } else {
            listView.smoothScrollBy(distanceFromTop, TimeLineCursorAdapter.ANIMATION_DURATION);
            hideStatusBar();
        }
    }

    @Override
    public void expandViewClosed(int currentDistanceFromTop) {
        if (expansionHandler == null) {
            expansionHandler = new Handler();
        }

        background = null;
        expansionHelper = null;

        expansionHandler.removeCallbacks(null);
        expansionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                canUseScrollStuff = true;
            }
        }, currentDistanceFromTop == -1 ? 0 : 500);

        MainActivity.sendHandler.removeCallbacks(null);
        MainActivity.sendHandler.post(MainActivity.showSend);

        if (listView.getFirstVisiblePosition() < 5) {
            showStatusBar();
        }

        if (currentDistanceFromTop != -1) {
            listView.smoothScrollBy(
                    -1 * expandedDistanceFromTop + currentDistanceFromTop -
                            (getResources().getBoolean(R.bool.isTablet) ? Utils.getActionBarHeight(context) : 0),
                    TimeLineCursorAdapter.ANIMATION_DURATION
            );
        }
    }
}