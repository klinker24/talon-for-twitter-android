package com.klinker.android.twitter_l.ui.tweet_viewer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.QuoteUtil;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;

public class QuotersFragment extends Fragment {

    private static final String ARG_SCREENNAME = "arg_screenname";
    private static final String ARG_TWEET_ID = "arg_tweet_id";

    public static QuotersFragment getInstance(String screenname, long tweetId) {
        QuotersFragment fragment = new QuotersFragment();

        Bundle args = new Bundle();
        args.putString(ARG_SCREENNAME, screenname);
        args.putLong(ARG_TWEET_ID, tweetId);

        fragment.setArguments(args);
        return fragment;
    }

    private String screenname;
    private long tweetId;

    private ListView listView;
    private LinearLayout spinner;
    private LinearLayout noContent;
    private HoloTextView noContentText;

    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, null);

        context = getActivity();
        screenname = getArguments().getString(ARG_SCREENNAME);
        tweetId = getArguments().getLong(ARG_TWEET_ID);

        View layout = inflater.inflate(R.layout.ptr_list_layout, null);

        listView = (ListView) layout.findViewById(R.id.listView);
        spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
        noContent = (LinearLayout) layout.findViewById(R.id.no_content);
        noContentText = (HoloTextView) layout.findViewById(R.id.no_retweeters_text);

        noContentText.setText(getActivity().getResources().getString(R.string.no_quotes));

        startSearch();

        return layout;
    }

    private void startSearch() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));
                    Query query = new Query(QuoteUtil.getSearchString(screenname, tweetId));

                    query.setCount(100);
                    query.setSinceId(tweetId);

                    QueryResult result = twitter.search(query);
                    final List<Status> statuses = QuoteUtil.stripNoQuotes(result.getTweets());

                    if (getActivity() == null) {
                        return;
                    }

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (statuses.size() > 0) {
                                TimelineArrayAdapter adapter = new TimelineArrayAdapter(context, statuses);

                                listView.setAdapter(adapter);
                                listView.setVisibility(View.VISIBLE);
                            } else {
                                noContent.setVisibility(View.VISIBLE);
                            }

                            spinner.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();

                    if (getActivity() == null) {
                        return;
                    }

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            noContent.setVisibility(View.VISIBLE);
                            spinner.setVisibility(View.GONE);
                        }
                    });

                }
            }
        }).start();
    }

}
