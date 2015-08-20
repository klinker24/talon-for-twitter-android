/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.twitter_l.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.wearable.view.GridViewPager;
import android.view.View;
import android.widget.TextView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapter.TweetGridPagerAdapter;
import com.klinker.android.twitter_l.transaction.KeyProperties;
import com.klinker.android.twitter_l.view.CircularProgressBar;

import java.util.List;

public class WearActivity extends WearTransactionActivity {

    private static final String TAG = "WearActivity";

    private GridViewPager viewPager;
    private TweetGridPagerAdapter adapter;
    private CircularProgressBar progressBar;
    private TextView emptyView;

    private int primaryColor;
    private int accentColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wear);
        viewPager = (GridViewPager) findViewById(R.id.article_pager);
        adapter = new TweetGridPagerAdapter(this);
        viewPager.setAdapter(adapter);

        progressBar = (CircularProgressBar) findViewById(R.id.progress_bar);
        emptyView = (TextView) findViewById(R.id.empty_view);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        accentColor = sharedPreferences.getInt(KeyProperties.KEY_ACCENT_COLOR, getResources().getColor(R.color.orange_accent_color));
        primaryColor = sharedPreferences.getInt(KeyProperties.KEY_PRIMARY_COLOR, getResources().getColor(R.color.orange_primary_color));
        progressBar.setColor(accentColor);
        viewPager.setBackgroundColor(primaryColor);

        viewPager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {
            }

            @Override
            public void onPageSelected(int row, int col) {
                try {
                    sendReadStatus(getIds().get(row - 2));
                } catch (Exception e) { }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    @Override
    public void updateDisplay() {
        progressBar.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        adapter = new TweetGridPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (adapter.getRowCount() > 2)
                    viewPager.setCurrentItem(adapter.getRowCount() - 3,0, adapter.getRowCount() > 20 ? false : true);
                else
                    viewPager.setCurrentItem(adapter.getRowCount() - 2,0, true);
            }
        }, 500);
    }

    private static final int COMPOSE_REQUEST_CODE = 101;
    private static final int REPLY_REQUEST_CODE = 102;

    public void startComposeRequest() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        startActivityForResult(intent, COMPOSE_REQUEST_CODE);
    }

    private String replyingToScreenname = "";
    private long replyingToTweetId = 0l;
    public void startReplyRequest(String screenname, long tweetId) {
        this.replyingToScreenname = screenname;
        this.replyingToTweetId = tweetId;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        startActivityForResult(intent, REPLY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == COMPOSE_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            sendComposeRequest(spokenText);
        } else if (requestCode == REPLY_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            sendReplyRequest(replyingToTweetId, replyingToScreenname, spokenText);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}
