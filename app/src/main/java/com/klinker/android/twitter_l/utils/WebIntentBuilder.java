package com.klinker.android.twitter_l.utils;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsIntent;

import com.klinker.android.twitter_l.APIKeys;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.BrowserActivity;

import java.util.Random;

import xyz.klinker.android.article.ArticleIntent;

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

    private static boolean JUST_RAN = false;

    private static final String PLAY_STORE = "play.google.com";
    private static final String YOUTUBE = "youtu";
    private static final String TWITTER = "twitter.com";
    private static final String PERISCOPE = "periscope.tv";
    private static final String MEERKAT = "mkr.tv";
    private static final String NEOGAF = "neogaf.com";
    private static final String FACEBOOK = "facebook.com";
    private static final String PIXIV = "pixiv.net";

    private static final String[] ALWAYS_EXTERNAL = new String[] {
            PLAY_STORE,
            YOUTUBE,
            TWITTER,
            PERISCOPE,
            MEERKAT,
            NEOGAF,
            FACEBOOK,
            PIXIV
    };

    private Context context;
    private AppSettings settings;

    private String webpage;
    private boolean forceExternal;

    private Intent intent;
    private CustomTabsIntent customTab;
    private ArticleIntent articleIntent;

    public WebIntentBuilder(Context context) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);
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

        if (forceExternal || shouldAlwaysForceExternal(webpage) || settings.browserSelection.equals("external")) {
            // request the external browser
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webpage));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (settings.browserSelection.equals("article")) {
            articleIntent = new ArticleIntent.Builder(context, APIKeys.ARTICLE_API_KEY)
                    .setToolbarColor(settings.themeColors.primaryColor)
                    .setAccentColor(settings.themeColors.accentColor)
                    .setTheme(settings.darkTheme ? ArticleIntent.THEME_DARK : ArticleIntent.THEME_LIGHT)
                    .setTextSize(settings.textSize)
                    .build();
        } else if (settings.browserSelection.equals("custom_tab")) {
            // add the share action
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            String extraText = webpage;
            shareIntent.putExtra(Intent.EXTRA_TEXT, extraText);
            shareIntent.setType("text/plain");
            Random random = new Random();
            PendingIntent pendingIntent = PendingIntent.getActivity(context, random.nextInt(Integer.MAX_VALUE), shareIntent, 0);

            customTab = new CustomTabsIntent.Builder(null)
                    .setShowTitle(true)
                    .setActionButton(((BitmapDrawable) context.getResources().getDrawable(R.drawable.ic_action_share_material)).getBitmap(), "Share", pendingIntent)
                    .setToolbarColor(settings.themeColors.primaryColor)
                    .build();
        } else {
            // fallback to in app browser
            intent = new Intent(context, BrowserActivity.class);
            intent.putExtra("url", webpage);
            intent.setFlags(0);
        }

        return this;
    }

    public void start() {
        if (customTab != null) {
            customTab.launchUrl(context, Uri.parse(webpage));
        } else if (articleIntent != null) {
            if (!JUST_RAN) {
                articleIntent.launchUrl(context, Uri.parse(webpage));
            }

            WebIntentBuilder.JUST_RAN = true;
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { }

                WebIntentBuilder.JUST_RAN = false;
            }).start();
        } else {
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean shouldAlwaysForceExternal(String url) {
        for (String s : ALWAYS_EXTERNAL)
            if (url.contains(s))
                return true;

        return false;
    }
}
