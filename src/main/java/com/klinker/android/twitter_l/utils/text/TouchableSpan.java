/*
 * Copyright 2013 Jacob Klinker
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

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import android.widget.Toast;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.Link;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.BrowserActivity;
import com.klinker.android.twitter_l.ui.PlainTextBrowserActivity;
import com.klinker.android.twitter_l.ui.drawer_activities.discover.trends.SearchedTrendsActivity;
import com.klinker.android.twitter_l.ui.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.utils.Utils;

public class TouchableSpan extends ClickableSpan {

    public TouchableSpan(Context context, Link value, boolean extBrowser) {
        mContext = context;
        mValue = value.getShort();
        full = value.getLong();
        this.extBrowser = extBrowser;

        settings = AppSettings.getInstance(context);

        mThemeColor = settings.themeColors.accentColor;
        mColorString = Color.argb(70, Color.red(mThemeColor), Color.green(mThemeColor), Color.blue(mThemeColor));

        // getconnectionstatus() is true if on mobile data, false otherwise
        mobilizedBrowser = settings.alwaysMobilize || (settings.mobilizeOnData && Utils.getConnectionStatus(context));

        fromLauncher = false;
    }

    public TouchableSpan(Context context, Link value, boolean extBrowser, AppSettings settings) {
        mContext = context;
        mValue = value.getShort();
        full = value.getLong();
        this.extBrowser = true;

        this.settings = settings;

        mThemeColor = settings.themeColors.accentColor;
        mColorString = Color.argb(70, Color.red(mThemeColor), Color.green(mThemeColor), Color.blue(mThemeColor));

        // getconnectionstatus() is true if on mobile data, false otherwise
        mobilizedBrowser = settings.alwaysMobilize || (settings.mobilizeOnData && Utils.getConnectionStatus(context));

        fromLauncher = true;
    }

    private AppSettings settings;
    private final Context mContext;
    private final String mValue;
    private final String full;
    private int mThemeColor;
    private int mColorString;
    private boolean extBrowser;
    private boolean mobilizedBrowser;
    private boolean fromLauncher;

    @Override
    public void onClick(View widget) {
        Log.v("talon_clickable", "clicked on the span");
        Log.v("talon_link", full);
        if (Patterns.WEB_URL.matcher(mValue).find()) {
            // open the in-app browser or the regular browser
            if (mValue.contains("play.google.com") || mValue.contains("youtu") || mValue.contains("twitter.com")) {
                // open to the play store
                String data = full.replace("http://", "").replace("https://", "").replace("\"", "");
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(
                        Uri.parse("http://" + data)
                );
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                if (extBrowser || !settings.inAppBrowser) {
                    String data = full.replace("http://", "").replace("https://", "").replace("\"", "");
                    Uri weburi = Uri.parse("http://" + data);
                    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                    launchBrowser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        mContext.startActivity(launchBrowser);
                    } catch (Exception e) {
                        Toast.makeText(mContext, "No browser found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String data = "http://" + full.replace("http://", "").replace("https://", "").replace("\"", "");
                    Intent launchBrowser = new Intent(mContext, mobilizedBrowser ? PlainTextBrowserActivity.class :BrowserActivity.class);
                    launchBrowser.putExtra("url", data);
                    mContext.startActivity(launchBrowser);
                }
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
            Intent user;
            if (!fromLauncher) {
                user = new Intent(mContext, ProfilePager.class);
            } else {
                user = new Intent("android.intent.action.MAIN");
                user.setComponent(new ComponentName("com.klinker.android.twitter",
                        "com.klinker.android.twitter.ui.profile_viewer.LauncherProfilePager"));
                user.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                user.putExtra("current_account", settings.currentAccount);
            }
            user.putExtra("screenname", full.replace("@", "").replaceAll(" ", ""));
            user.putExtra("proPic", "");
            mContext.startActivity(user);
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
}