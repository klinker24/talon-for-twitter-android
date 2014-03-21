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

package com.klinker.android.twitter.utils.text;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.Link;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.BrowserActivity;
import com.klinker.android.twitter.ui.drawer_activities.discover.trends.SearchedTrendsActivity;
import com.klinker.android.twitter.ui.profile_viewer.ProfilePager;

public class LongClickableSpan extends ClickableSpan {

    public LongClickableSpan(Context context, Link value) {
        mContext = context;
        mValue = value.getShort();
        full = value.getLong();

        Log.v("talon_clickable_string", "short: " + mValue);
        Log.v("talon_clickable_string", "long: " + full);

        settings = AppSettings.getInstance(context);

        if (settings.addonTheme) {
            mThemeColor = settings.accentInt;
            mColorString = Color.parseColor("#44" + settings.accentColor);
        } else {
            mThemeColor = context.getResources().getColor(R.color.app_color);
            mColorString = context.getResources().getColor(R.color.pressed_app_color);
        }
    }

    private AppSettings settings;
    private final Context mContext;
    private final String mValue;
    private final String full;
    private int mThemeColor;
    private int mColorString;

    @Override
    public void onClick(View widget) {
        Log.v("talon_link", mValue);
        if (Patterns.WEB_URL.matcher(mValue).find()) {
            // open the in-app browser or the regular browser
            Log.v("talon_link", "web");
            if (mValue.contains("play.google.com")) {
                // open to the play store
                String data = full.replace("http://", "").replace("https://", "").replace("\"", "");
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(
                        Uri.parse("http://" + data)
                );
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                String data = full.replace("http://", "").replace("https://", "").replace("\"", "");

                Uri weburi = Uri.parse("http://" + data);
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                launchBrowser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(launchBrowser);
            }
        } else if (Regex.HASHTAG_PATTERN.matcher(mValue).find()) {
            Log.v("talon_link", "hashtag");
            // found a hashtag, so open the hashtag search
            Intent search = new Intent(mContext, SearchedTrendsActivity.class);
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, full);
            mContext.startActivity(search);
        } else if (Regex.MENTION_PATTERN.matcher(mValue).find()) {
            Log.v("talon_link", "mention");
            Intent user = new Intent(mContext, ProfilePager.class);
            user.putExtra("screenname", full.replace("@", "").replaceAll(" ", ""));
            user.putExtra("proPic", "");
            mContext.startActivity(user);
        }

        /*highlight = false;
        updateDrawState(unhighlighed);*/
    }

    //public TextPaint unhighlighed = null;
    //public boolean colorSet = false;

    @Override
    public void updateDrawState(TextPaint ds) {

        //super.updateDrawState(ds);

        ds.setUnderlineText(false);
        ds.setColor(mThemeColor);

        /*if (unhighlighed == null) {
            unhighlighed = ds;
        }

        if (highlight && !colorSet) {
            Log.v("talon_spans", "adding color");
            ds.bgColor = mColorString;
            colorSet = true;
        } else if (!highlight) {
            Log.v("talon_spans", "taking away color");
            ds = unhighlighed;
            colorSet = false;
        }*/
    }

    /*public boolean highlight = false;
    public View widget = null;*/

    /*public void setHighlighted(boolean highlighted, View widget) {
        Log.v("talon_spans", "highlight: " + highlighted);
        this.widget = widget;
        this.highlight = highlighted;
        updateDrawState(unhighlighed);
        widget.invalidate();
    }*/
    /*public boolean highlighted = false;
    public void setPressed(boolean isSelected, View view) {
        highlighted = isSelected;
        updateDrawState(unhighlighed);
        view.invalidate();
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);

        ds.setUnderlineText(false);
        ds.setColor(mThemeColor);

        if (unhighlighed == null) {
            unhighlighed = ds;
        }

        if (highlighted) {
            ds.bgColor = mColorString;
        } else {
            ds = unhighlighed;
        }
    }

    public void onLongClick(View v) {
        // do nothing on a long click
    }*/
}