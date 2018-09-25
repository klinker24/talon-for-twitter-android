package com.klinker.android.twitter_l.activities.main_fragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.views.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.utils.Expandable;
import com.klinker.android.twitter_l.utils.ExpansionViewHelper;
import com.klinker.android.twitter_l.utils.PixelScrollDetector;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Twitter;

public abstract class MainFragment extends Fragment implements Expandable {

    protected Twitter twitter;

    protected ListView listView;
    protected TimeLineCursorAdapter cursorAdapter;
    protected View toastBar;
    protected TextView toastDescription;
    protected TextView toastButton;
    protected MaterialSwipeRefreshLayout refreshLayout;
    protected LinearLayout spinner;

    protected SharedPreferences sharedPrefs;
    protected Activity context;

    protected androidx.appcompat.app.ActionBar actionBar;
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

            if (!settings.staticUi) {
                hideToastBar(300);
            }
        }
    };

    private int thisFragmentNumber = -2;
    public BroadcastReceiver jumpTopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra("fragment_number", -1) == thisFragmentNumber)
                toTop();
        }
    };

    public BroadcastReceiver showToast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra("fragment_number", -1) == thisFragmentNumber){
                overrideSnackbarSetting = true;
                showToastBar(listView.getFirstVisiblePosition() + " " + fromTop, jumpToTop, 300, true, toTopListener);
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

    public void applyAdapter() {
        listView.setAdapter(cursorAdapter);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                playCurrentVideos();
            }
        }, 400);
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

        if (cursorAdapter != null) {
            cursorAdapter.activityPaused(false);
        }

        playCurrentVideos();

        if (sharedPrefs.getBoolean("just_muted", false)) {
            sharedPrefs.edit().putBoolean("just_muted", false).commit();
            getCursorAdapter(false);
        }
    }

    public void resetVideoHandler() {
        if (cursorAdapter != null) {
            cursorAdapter.resetVideoHandler();
        }
    }

    @Override
    public void onPause() {

        context.unregisterReceiver(jumpTopReceiver);
        context.unregisterReceiver(showToast);
        context.unregisterReceiver(hideToast);

        if (cursorAdapter != null) {
            cursorAdapter.activityPaused(true);
        }

        stopCurrentVideos();

        super.onPause();
    }

    public void playCurrentVideos() {

        if (MainActivity.mViewPager.getCurrentItem() == thisFragmentNumber) {
            try {
                cursorAdapter.playCurrentVideo();
            } catch (Exception e) {

            }
        }
    }

    public void stopCurrentVideos() {
        try {
            cursorAdapter.stopOnScroll();
        } catch (Exception e) {

        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        actionBar = ((AppCompatActivity)context).getSupportActionBar();
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

        thisFragmentNumber = getArguments().getInt("fragment_number", -2);

        sharedPrefs = AppSettings.getSharedPreferences(context);


        setAppSettings();
        setHome();

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        currentAccount = getCurrentAccount();

        SharedPreferences.Editor e = sharedPrefs.edit();
        e.putInt("dm_unread_" + sharedPrefs.getInt("current_account", 1), 0);
        e.putBoolean("refresh_me", false);
        e.apply();

        getStrings();

        try {
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

    public void getStrings() {
        fromTop = getResources().getString(R.string.tweets);
        jumpToTop = getResources().getString(R.string.to_top);
        allRead = getResources().getString(R.string.all_read);
        toMentions = getResources().getString(R.string.mentions);
    }

    protected void setSpinner(View layout) {
        spinner = (LinearLayout) layout.findViewById(R.id.spinner);
    }

    private View rootLayout;
    public void setViews(View layout) {

        rootLayout = layout;

        listView = (ListView) layout.findViewById(R.id.listView);
        setSpinner(layout);

        refreshLayout = (MaterialSwipeRefreshLayout) layout.findViewById(R.id.swipe_refresh_layout);
        refreshLayout.setOnRefreshListener(new MaterialSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshStarted();
            }
        });
        int size = Utils.getActionBarHeight(context) + (MainActivity.isPopup ? 0 : Utils.getStatusBarHeight(context));

        refreshLayout.setProgressViewOffset(false, 0, size + toDP(25));

        refreshLayout.setColorSchemeColors(settings.themeColors.accentColor, settings.themeColors.primaryColor);
        refreshLayout.setBarVisibilityWatcher((MainActivity) getActivity());

        setUpHeaders();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setMoveActionBar();
    }

    private void setMoveActionBar() {
        //boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        //if (isTablet) {
            //moveActionBar = false;
        //} else {
            //moveActionBar = true;
        //}

        moveActionBar = !AppSettings.dualPanels(context);
    }

    boolean moveActionBar = true;
    public void setUpListScroll() {

        setMoveActionBar();

        listView.setOnScrollListener(new PixelScrollDetector(new PixelScrollDetector.PixelScrollListener() {

            @Override
            public void onScrollStateChanged(final AbsListView absListView, final int i) {
                if (cursorAdapter != null) {
                    if (i == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        playCurrentVideos();
                    } else {
                        try {
                            cursorAdapter.stopOnScroll(listView.getFirstVisiblePosition(), listView.getLastVisiblePosition());
                        } catch (Exception e) {

                        }
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, float deltaY) {
                //Log.v("pixel_scrolling", "deltaY: " + deltaY);
                // negative deltaY is when we are scrolling down

                if (settings.staticUi) {
                    if (settings.useSnackbar) {
                        showToastBar(listView.getFirstVisiblePosition() + " " + fromTop, jumpToTop, 300, false, toTopListener);
                    }
                    return;
                }

                if (listView.getFirstVisiblePosition() < 2) {
                    scrollDown();
                    hideToastBar(300);
                    return;
                }

                if (Math.abs(deltaY) > 20) {
                    if (settings.topDown) {
                        if (deltaY < 0) {
                            scrollUp();
                        } else {
                            scrollDown();
                        }
                    } else {
                        if (deltaY < 0) {
                            scrollDown();
                        } else {
                            scrollUp();
                        }
                    }
                }
            }
        }));
    }

    public void setUpHeaders() {
        View viewHeader = context.getLayoutInflater().inflate(R.layout.ab_header, null);
        listView.addHeaderView(viewHeader, null, false);
        listView.setHeaderDividersEnabled(false);

        if (settings.revampedTweets()) {
            listView.setDivider(null);
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

            if (!MainActivity.isPopup) {
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

        setMoveActionBar();

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

        setMoveActionBar();

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

    public void showStatusBar() {
        if (getActivity() != null) {
            ((MainActivity)getActivity()).showBars();
        }
    }

    public void hideStatusBar() {
        if (getActivity() != null) {
            ((MainActivity)getActivity()).hideBars();
        }
    }

    public void setUpToastBar(View view) {
        toastBar = view.findViewById(R.id.toastBar);
        toastDescription = (TextView) view.findViewById(R.id.toastDescription);
        toastButton = (TextView) view.findViewById(R.id.toastButton);

        toastButton.setTextColor(settings.themeColors.accentColorLight);

        if (!Utils.hasNavBar(getActivity()) ||
                (landscape && !getResources().getBoolean(R.bool.isTablet))) {
            toastBar.setTranslationY(Utils.toDP(48, getActivity()));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toastBar.setElevation(Utils.toDP(6, getActivity()));
        }

        try {
            View toastBackground = view.findViewById(R.id.toast_background);
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;

            // fab is 20 margin right and 72 width, so max size of the toast should be screen_width - 20 - 20 - 72,
            // then - 20 more for the left margin on the background
            int maxSize = width - Utils.toDP(132, context);
            int textSize = Utils.toDP(250, context); // size needed to fit "NO NEW TWEETS"
            ViewGroup.LayoutParams params = toastBackground.getLayoutParams();
            if (maxSize < textSize) {
                params.width = maxSize;
            } else {
                params.width = textSize;
            }
        } catch (Exception e) {

        }
    }

    protected boolean overrideSnackbarSetting = false;
    public Handler removeToastHandler;
    public void showToastBar(String description, String buttonText, final long length, final boolean quit, View.OnClickListener listener) {
        if (removeToastHandler == null) {
            removeToastHandler = new Handler();
        } else if (!overrideSnackbarSetting) {
            removeToastHandler.removeCallbacksAndMessages(null);
        }

        if (!settings.useSnackbar && !overrideSnackbarSetting) {
            return;
        }

        if (description.toLowerCase().equals("0 tweets")) {
            hideToastBar(0);
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

            ObjectAnimator toastBarAlpha = ObjectAnimator.ofFloat(toastBar, View.ALPHA, 0f, 1f);
            toastBarAlpha.setDuration(length);
            toastBarAlpha.setInterpolator(anim.getInterpolator());
            toastBarAlpha.start();
        }
    }

    public void hideToastBar(long length) {
        if (!isToastShowing || (!settings.useSnackbar && !overrideSnackbarSetting)) {
            return;
        }

        overrideSnackbarSetting = false;

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

        if (cursorAdapter != null) {
            cursorAdapter.stopOnScroll();
        }

        if (expansionHelper != null) {
            if (expansionHelper.hidePopups()) {
                return false;
            }
        }

        if (background != null) {
            background.performClick();
            return false;
        }

        return true;
    }

    private int expandedDistanceFromTop = 0;
    private View background;
    private ExpansionViewHelper expansionHelper;

    @Override
    public void expandViewOpen(int distanceFromTop, int position, View root, ExpansionViewHelper helper) {
        if (landscape) {
            distanceFromTop = distanceFromTop + Utils.getStatusBarHeight(context) * 2;
        }

        background = root;
        expansionHelper = helper;

        expandedDistanceFromTop = distanceFromTop;

        if (!settings.staticUi) {
            MainActivity.sendHandler.removeCallbacks(null);
            MainActivity.sendHandler.post(MainActivity.hideSend);

            if (moveActionBar)
                hideStatusBar();
        }

        hideToastBar(300);

        listView.smoothScrollBy(distanceFromTop, TimeLineCursorAdapter.ANIMATION_DURATION);
    }

    @Override
    public void expandViewClosed(int currentDistanceFromTop) {

        background = null;
        expansionHelper = null;

        if (!settings.staticUi) {
            MainActivity.sendHandler.removeCallbacks(null);
            MainActivity.sendHandler.post(MainActivity.showSend);
            showStatusBar();
        }

        if (currentDistanceFromTop != -1) {
            listView.smoothScrollBy(
                    -1 * expandedDistanceFromTop + currentDistanceFromTop,
                    TimeLineCursorAdapter.ANIMATION_DURATION
            );
        }
    }
}