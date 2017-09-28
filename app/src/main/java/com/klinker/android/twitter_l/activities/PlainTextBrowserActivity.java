package com.klinker.android.twitter_l.activities;
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

import android.app.Activity;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefTextView;
import com.klinker.android.twitter_l.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PlainTextBrowserActivity extends BrowserActivity {

    FontPrefTextView webText;
    ScrollView scrollView;
    LinearLayout spinner;

    @Override
    public void setUpLayout() {
        setContentView(R.layout.mobilized_fragment);
        webText = (FontPrefTextView) findViewById(R.id.webpage_text);
        scrollView = (ScrollView) findViewById(R.id.scrollview);
        spinner = (LinearLayout) findViewById(R.id.spinner);

        View statusBar = findViewById(R.id.kitkat_status_bar);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            statusBar.setVisibility(View.VISIBLE);

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) statusBar.getLayoutParams();
            params.height = Utils.getStatusBarHeight(this);
            statusBar.setLayoutParams(params);

            LinearLayout.LayoutParams linear = (LinearLayout.LayoutParams) scrollView.getLayoutParams();
            linear.topMargin = Utils.getStatusBarHeight(this) + Utils.getActionBarHeight(this);
            scrollView.setLayoutParams(linear);
        }

        getTextFromSite();
    }

    public void getTextFromSite() {
        TimeoutThread getText = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Document doc = Jsoup.connect(url).get();

                    String text = "";
                    String title = doc.title();

                    if(doc != null) {
                        Elements paragraphs = doc.getElementsByTag("p");

                        if (paragraphs.hasText()) {
                            for (int i = 0; i < paragraphs.size(); i++) {
                                Element s = paragraphs.get(i);
                                if (!s.html().contains("<![CDATA")) {
                                    text += paragraphs.get(i).html().replaceAll("<br/>", "") + "<br/><br/>";
                                }
                            }
                        }
                    }

                    final String article =
                            "<strong><big>" + title + "</big></strong>" +
                                    "<br/><br/>" +
                                    text.replaceAll("<img.+?>", "") +
                                    "<br/>"; // one space at the bottom to make it look nicer

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webText.setText("\n" + Html.fromHtml(article));
                            webText.setMovementMethod(LinkMovementMethod.getInstance());
                            webText.setTextSize(settings.textSize);

                            spinner.setVisibility(View.GONE);
                            scrollView.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webText.setText(getResources().getString(R.string.error_loading_page));
                        }
                    });
                }
            }
        });

        getText.setPriority(8);
        getText.start();
    }
}
