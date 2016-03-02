package com.klinker.android.twitter_l.utils;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.BrowserActivity;
import com.klinker.android.twitter_l.ui.PlainTextBrowserActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to handle URLs.
 *
 * Will redirect some to the external browser automatically (play store, youtube, twitter, periscope, meerkat)
 * Others will attempt to load a custom tab
 * If the tab fails, then it will open up the in app browser intent.
 *
 * This does not do anything with warming up the custom intent or starting a session.
 * It will simply display the page.
 */
public class WebIntentBuilder {

    private static final String PLAY_STORE = "play.google.com";
    private static final String YOUTUBE = "youtu";
    private static final String TWITTER = "twitter.com";
    private static final String PERISCOPE = "periscope.tv";
    private static final String MEERKAT = "mkr.tv";

    private static final String[] ALWAYS_EXTERNAL = new String[] {
            PLAY_STORE,
            YOUTUBE,
            TWITTER,
            PERISCOPE,
            MEERKAT
    };

    private Context context;
    private AppSettings settings;
    private boolean mobilizedBrowser;

    private Intent intent;
    private String webpage;
    private boolean forceExternal;

    private boolean tryCustomTabs = false;

    public WebIntentBuilder(Context context) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);
        this.mobilizedBrowser = settings.alwaysMobilize ||
                (settings.mobilizeOnData && Utils.getConnectionStatus(context));
    }

    public WebIntentBuilder setUrl(String url) {
        this.webpage = url;
        return this;
    }

    public WebIntentBuilder setShouldForceExternal(boolean forceExternal) {
        this.forceExternal = forceExternal;
        return this;
    }

    public WebIntentBuilder build() {
        if (webpage == null) {
            throw new RuntimeException("URL cannot be null.");
        }

        if (forceExternal || !settings.inAppBrowser || shouldAlwaysForceExternal(webpage)) {
            // request the external browser
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webpage));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (!mobilizedBrowser) {
            tryCustomTabs = true;

            intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName(CHROME_PACKAGE, "org.chromium.chrome.browser.customtabs.CustomTabActivity"));
            intent.setData(Uri.parse(webpage));

            // request a chrome custom tab
            Bundle extras = new Bundle();
            extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
            intent.putExtras(extras);

            intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, settings.themeColors.primaryColor);

            // add the share action
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            String extraText = webpage;
            shareIntent.putExtra(Intent.EXTRA_TEXT, extraText);
            shareIntent.setType("text/plain");
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, Intent.createChooser(shareIntent, "Share to:"), 0);

            ArrayList menuItemBundleList = new ArrayList<>();

            Bundle menuItem = new Bundle();
            menuItem.putString(KEY_CUSTOM_TABS_MENU_TITLE, context.getString(R.string.share_link));
            menuItem.putParcelable(KEY_CUSTOM_TABS_PENDING_INTENT, pendingIntent);
            menuItemBundleList.add(menuItem);

            intent.putParcelableArrayListExtra(EXTRA_CUSTOM_TABS_MENU_ITEMS, menuItemBundleList);

        } else {
            // fallback to in app browser
            intent = new Intent(context, mobilizedBrowser ?
                    PlainTextBrowserActivity.class : BrowserActivity.class);
            intent.putExtra("url", webpage);
            intent.setFlags(0);
        }

        return this;
    }

    public void start() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (tryCustomTabs) {
            final UseTabs tabs = isChromeInstalled();
            if (tabs.chromeInstalled && tabs.tabsExported) {
                context.startActivity(intent);
            } else {
                if (sharedPreferences.getBoolean("shown_disclaimer_for_custom_tabs_4", false)) {
                    fallbackToInternal(sharedPreferences);
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.custom_tab_title)
                            .setMessage(R.string.custom_tab_message)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sharedPreferences.edit().putBoolean("shown_disclaimer_for_custom_tabs_4", true).commit();
                                    sharedPreferences.edit().putBoolean("is_chrome_default", true).commit();
                                    fallbackToInternal(sharedPreferences);
                                }
                            })
                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sharedPreferences.edit().putBoolean("shown_disclaimer_for_custom_tabs_4", true).commit();
                                    sharedPreferences.edit().putBoolean("is_chrome_default", false).commit();
                                    fallbackToInternal(sharedPreferences);
                                }
                            })
                            .setNeutralButton(R.string.learn_more, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse("http://android-developers.blogspot.com/2015/09/chrome-custom-tabs-smooth-transition.html"));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                }
                            })
                            .create().show();
                }

            }
        } else {
            context.startActivity(intent);
        }
    }

    private void fallbackToInternal(SharedPreferences sharedPreferences) {
        if (sharedPreferences.getBoolean("is_chrome_default", false)) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webpage));

            // request a chrome custom tab
            Bundle extras = new Bundle();
            extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
            intent.putExtras(extras);

            intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, settings.themeColors.primaryColor);

            context.startActivity(intent);
        } else {
            intent = new Intent(context, mobilizedBrowser ?
                    PlainTextBrowserActivity.class : BrowserActivity.class);
            intent.putExtra("url", webpage);
            intent.setFlags(0);

            context.startActivity(intent);
        }
    }

    private UseTabs isChromeInstalled() {
        PackageManager pm = context.getPackageManager();

        UseTabs tabs = new UseTabs();
        try {
            ActivityInfo activityInfo = intent.resolveActivityInfo(pm, intent.getFlags());
            if (!activityInfo.exported) {
                Log.v("talon_intent", "activity not exported");
                tabs.tabsExported = false;
            }
        } catch (Exception e) {
            Log.v("talon_intent", "activity not found");
            tabs.chromeInstalled = false;
        }

        return tabs;
    }

    private boolean shouldAlwaysForceExternal(String url) {
        for (String s : ALWAYS_EXTERNAL)
            if (url.contains(s))
                return true;

        return false;
    }

    /**
     * Chrome Custom Tab Extras
     */

    private static final String CHROME_PACKAGE = "com.android.chrome";

    // REQUIRED. Must use an extra bundle with this. Even if the contense is null.
    private static final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";

    // Optional. specify an integer color
    private static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

    // Optional. Use an ArrayList for specifying menu related params. There
    // should be a separate Bundle for each custom menu item.
    public static final String EXTRA_CUSTOM_TABS_MENU_ITEMS = "android.support.customtabs.extra.MENU_ITEMS";
    public static final String KEY_CUSTOM_TABS_MENU_TITLE = "android.support.customtabs.customaction.MENU_ITEM_TITLE";


    // Optional. Key that specifies the PendingIntent to launch when the action button
    // or menu item was tapped. Chrome will be calling PendingIntent#send() on
    // taps after adding the url as data. The client app can call Intent#getDataString() to get the url.
    public static final String KEY_CUSTOM_TABS_PENDING_INTENT = "android.support.customtabs.customaction.PENDING_INTENT";

    // Optional. Use a bundle for parameters if an the action button is specified.
    public static final String EXTRA_CUSTOM_TABS_ACTION_BUTTON_BUNDLE = "android.support.customtabs.extra.ACTION_BUNDLE_BUTTON";

    private static class UseTabs {
        public boolean chromeInstalled;
        public boolean tabsExported;

        public UseTabs() {
            chromeInstalled = true;
            tabsExported = true;
        }
    }
}
