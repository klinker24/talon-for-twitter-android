package com.klinker.android.twitter.ui.tweet_viewer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter.settings.AppSettings;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;

/**
 * Created by luke on 4/1/14.
 */
public class MobilizedFragment extends Fragment {
    private View layout;
    private ArrayList<String> webpages;
    private HoloTextView webText;

    public Context context;

    public MobilizedFragment(AppSettings settings, ArrayList<String> webpages) {
        this.webpages = webpages;
    }

    public MobilizedFragment() {
        this.webpages = new ArrayList<String>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        context = getActivity();

        layout = inflater.inflate(R.layout.mobilized_fragment, null, false);
        webText = (HoloTextView) layout.findViewById(R.id.webpage_text);

        getTextFromSite();

        return layout;
    }

    public boolean isStopped = false;

    @Override
    public void onStop() {
        isStopped = true;
        super.onStop();
    }

    public void getTextFromSite() {
        Thread getText = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(webpages.get(0));
                    Document doc = Jsoup.connect(webpages.get(0)).get();

                    String text = "";
                    String title = doc.title();

                    if(doc != null) {
                        Elements paragraphs = doc.getElementsByTag("p");

                        if (paragraphs.hasText()) {
                            text = paragraphs.html();
                        }
                    }

                    final String article = title + "<br/><br/>" + text;

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webText.setText(article);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webText.setText("Error getting webpage.");
                        }
                    });
                }
            }
        });

        getText.setPriority(7);
        getText.start();
    }
}
