/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.twitter_l.utils.text;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;

import android.widget.Toast;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.Link;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter_l.activities.media_viewer.VideoViewerActivity;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.BrowserActivity;
import com.klinker.android.twitter_l.activities.PlainTextBrowserActivity;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.activities.drawer_activities.discover.trends.SearchedTrendsActivity;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.utils.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.WebIntentBuilder;

import twitter4j.Twitter;
import twitter4j.User;

import java.util.HashSet;
import java.util.Set;

public class TouchableSpan extends ClickableSpan {

    public TouchableSpan(Context context, Link value, boolean extBrowser) {
        mContext = context;
        mValue = value.getShort();
        full = value.getLong();
        this.extBrowser = extBrowser;

        settings = AppSettings.getInstance(context);

        mThemeColor = settings.themeColors.accentColor;
        mColorString = Color.argb(70, Color.red(settings.themeColors.accentColor),
                Color.green(settings.themeColors.accentColor),
                Color.blue(settings.themeColors.accentColor));

        // getconnectionstatus() is true if on mobile data, false otherwise
        mobilizedBrowser = settings.alwaysMobilize || (settings.mobilizeOnData && Utils.getConnectionStatus(context));

        fromLauncher = false;
    }

    private AppSettings settings;
    public final Context mContext;
    private final String mValue;
    private final String full;
    private int mThemeColor;
    private int mColorString;
    private boolean extBrowser;
    private boolean mobilizedBrowser;
    private boolean fromLauncher;

    @Override
    public void onClick(View widget) {
        mContext.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION"));

        if (Patterns.WEB_URL.matcher(mValue).find()) {
            String url = "http://" + full.replace("http://", "").replace("https://", "").replace("\"", "");

            if (url.contains("/i/web/status/") || url.contains("/moments/")) {
                Intent browser = new Intent(mContext, BrowserActivity.class);
                browser.putExtra("url", url);
                mContext.startActivity(browser);
            } else if (url.contains("vine.co/v/")) {
                VideoViewerActivity.startActivity(mContext, 0l, url, "");
            } else {
                new WebIntentBuilder(mContext)
                        .setUrl(url)
                        .setShouldForceExternal(extBrowser)
                        .build().start();
            }
        } else if (Regex.HASHTAG_PATTERN.matcher(mValue).find()) {
            // found a hashtag, so open the hashtag search
            Intent search;
            if (!fromLauncher) {
                search = new Intent(mContext, SearchedTrendsActivity.class);
            } else {
                search = new Intent("android.intent.action.MAIN");
                search.setComponent(new ComponentName("com.klinker.android.twitter",
                        "com.klinker.android.twitter.ui.drawer_activities.discover.trends.LauncherSearchedTrends"));
                search.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                search.putExtra("current_account", settings.currentAccount);
            }
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, full);
            mContext.startActivity(search);
        } else if (Regex.MENTION_PATTERN.matcher(mValue).find()) {
            ProfilePager.start(mContext, full.replace("@", "").replaceAll(" ", ""));
        } else if (Regex.CASHTAG_PATTERN.matcher(mValue).find()) {
            // found a cashtag, so open the search
            Intent search;
            if (!fromLauncher) {
                search = new Intent(mContext, SearchedTrendsActivity.class);
            } else {
                search = new Intent("android.intent.action.MAIN");
                search.setComponent(new ComponentName("com.klinker.android.twitter",
                        "com.klinker.android.twitter.ui.drawer_activities.discover.trends.LauncherSearchedTrends"));
                search.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                search.putExtra("current_account", settings.currentAccount);
            }
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, full);
            mContext.startActivity(search);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TouchableMovementMethod.touched = false;
            }
        }, 500);
    }

    public void onLongClick() {
        if (Patterns.WEB_URL.matcher(mValue).find()) {
            // open external
            // open internal
            // copy link
            // share link
            longClickWeb();
        } else if (Regex.HASHTAG_PATTERN.matcher(mValue).find()) {
            // search hashtag
            // mute hashtag
            // copy hashtag
            longClickHashtag();
        } else if (Regex.MENTION_PATTERN.matcher(mValue).find()) {
            // Open profile
            // copy handle
            // search
            // favorite user
            // mute user
            // share profile
            longClickMentions();
        } else if (Regex.CASHTAG_PATTERN.matcher(mValue).find()) {
            // search cashtag
            // copy cashtag
            longClickCashtag();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TouchableMovementMethod.touched = false;
            }
        }, 500);
    }

    public boolean touched = false;

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);

        ds.setUnderlineText(false);
        ds.setColor(mThemeColor);
        ds.bgColor = touched ? mColorString : Color.TRANSPARENT;
    }

    public void setTouched(boolean isTouched) {
        touched = isTouched;
    }

    public void longClickWeb() {
        AlertDialog.Builder builder = getBuilder();

        builder.setItems(R.array.long_click_web, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0: // open external
                        String data = full.replace("http://", "").replace("https://", "").replace("\"", "");
                        Uri weburi = Uri.parse("http://" + data);
                        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                        launchBrowser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            mContext.startActivity(launchBrowser);
                        } catch (Exception e) {
                            Toast.makeText(mContext, "No browser found.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1: // open internal
                        data = "http://" + full.replace("http://", "").replace("https://", "").replace("\"", "");

                        if (data.contains("vine.co/v/")) {
                            VideoViewerActivity.startActivity(mContext, 0l, data, "");
                        } else {
                            launchBrowser = new Intent(mContext, mobilizedBrowser ? PlainTextBrowserActivity.class :BrowserActivity.class);
                            launchBrowser.putExtra("url", data);
                            mContext.startActivity(launchBrowser);
                        }

                        break;
                    case 2: // copy link
                        copy();
                        break;
                    case 3: // share link
                        share(full);
                        break;
                }
            }
        });

        builder.create().show();
    }

    public void longClickHashtag() {
        AlertDialog.Builder builder = getBuilder();

        builder.setItems(R.array.long_click_hashtag, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0: // search hashtag
                        TouchableSpan.this.onClick(null);
                        break;
                    case 1: // mute hashtag
                        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(mContext);


                        Toast.makeText(mContext, mContext.getResources().getString(R.string.muted) + " " + full, Toast.LENGTH_SHORT).show();
                        String item = full.replace("#", "") + " ";

                        String current = sharedPreferences.getString("muted_hashtags", "");
                        sharedPreferences.edit().putString("muted_hashtags", current + item).apply();
                        sharedPreferences.edit().putBoolean("refresh_me", true).apply();

                        if (mContext instanceof DrawerActivity) {
                            ((Activity)mContext).recreate();
                        }
                        break;
                    case 2: // copy hashtag
                        copy();
                        break;
                }
            }
        });

        builder.create().show();
    }

    public void longClickMentions() {
        AlertDialog.Builder builder = getBuilder();

        builder.setItems(R.array.long_click_mentions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(mContext);


                switch (i) {
                    case 0: // open profile
                        TouchableSpan.this.onClick(null);
                        break;
                    case 1: // copy handle
                        copy();
                        break;
                    case 2: // search user
                        search();
                        break;
                    case 3: // favorite user
                        new TimeoutThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Twitter twitter = Utils.getTwitter(mContext, settings);
                                    User user = twitter.showUser(full.replace("@", ""));

                                    int current = sharedPrefs.getInt("current_account", 1);

                                    FavoriteUsersDataSource.getInstance(mContext).createUser(user, current);

                                    sharedPrefs.edit().putString("favorite_user_names_" + current, sharedPrefs.getString("favorite_user_names_" + current, "") + user.getScreenName() + " ").apply();
                                } catch (Exception e) {

                                }
                            }
                        }).start();
                        break;
                    case 4: // mute user
                        String current = sharedPrefs.getString("muted_users", "");
                        sharedPrefs.edit().putString("muted_users", current + full.replaceAll(" ", "").replaceAll("@", "") + " ").apply();
                        sharedPrefs.edit().putBoolean("refresh_me", true).apply();
                        sharedPrefs.edit().putBoolean("just_muted", true).apply();
                        if (mContext instanceof DrawerActivity) {
                            ((Activity)mContext).recreate();
                        }
                        break;
                    case 5: // mute retweets
                        String muted_rts = sharedPrefs.getString("muted_rts", "");
                        sharedPrefs.edit().putString("muted_rts", muted_rts + full.replaceAll(" ", "").replaceAll("@", "") + " ").apply();
                        sharedPrefs.edit().putBoolean("refresh_me", true).apply();
                        sharedPrefs.edit().putBoolean("just_muted", true).apply();
                        if (mContext instanceof DrawerActivity) {
                            ((Activity)mContext).recreate();
                        }
                        break;
                    case 6: // muffle user
                        Set<String> muffled = sharedPrefs.getStringSet("muffled_users", new HashSet<String>());
                        String name = full.replace("@", "");

                        if (!muffled.contains(name)) {
                            muffled.add(name);
                            sharedPrefs.edit().putStringSet("muffled_users", muffled).apply();
                            sharedPrefs.edit().putBoolean("refresh_me", true).apply();
                            sharedPrefs.edit().putBoolean("just_muted", true).apply();

                            if (mContext instanceof DrawerActivity) {
                                ((Activity)mContext).recreate();
                            }
                        }
                        break;
                    case 7: // share profile
                        share("https://twitter.com/" + full.replace("@", "").replace(" ", ""));
                        break;
                }
            }
        });

        builder.create().show();
    }

    public void longClickCashtag() {
        AlertDialog.Builder builder = getBuilder();

        builder.setItems(R.array.long_click_cashtag, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0: // search cashtag
                        TouchableSpan.this.onClick(null);
                        break;
                    case 1: // copy cashtag
                        copy();
                        break;
                }
            }
        });

        builder.create().show();
    }

    private AlertDialog.Builder getBuilder() {
        String display = "";

        if (full.length() > 20) {
            display = full.substring(0, 18) + "...";
        } else {
            display = full;
        }
        return new AlertDialog.Builder(mContext)
                .setTitle(full);
    }

    private void search() {
        Intent search = new Intent(mContext, SearchedTrendsActivity.class);
        search.setAction(Intent.ACTION_SEARCH);
        search.putExtra(SearchManager.QUERY, full);
        mContext.startActivity(search);
    }

    private void copy() {
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("link", full);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(mContext, R.string.copied, Toast.LENGTH_SHORT).show();
    }

    private void share(String text) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);

        //mContext.startActivity(Intent.createChooser(share, "Share with:"));
        mContext.startActivity(share);
    }
}