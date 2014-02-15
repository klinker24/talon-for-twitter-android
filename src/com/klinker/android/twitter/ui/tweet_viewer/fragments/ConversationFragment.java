package com.klinker.android.twitter.ui.tweet_viewer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.AutoCompetePeopleAdapter;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class ConversationFragment extends Fragment {
    private Context context;
    private View layout;
    private AppSettings settings;
    private long tweetId;

    public ConversationFragment(AppSettings settings, long tweetId) {
        this.settings = settings;
        this.tweetId = tweetId;
    }

    public ConversationFragment() {
        this.settings = null;
        this.tweetId = 0;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        layout = inflater.inflate(R.layout.conversation_fragment, null);
        final AsyncListView replyList = (AsyncListView) layout.findViewById(R.id.listView);
        final LinearLayout progressSpinner = (LinearLayout) layout.findViewById(R.id.list_progress);
        final HoloTextView none = (HoloTextView) layout.findViewById(R.id.no_conversation);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        replyList.setItemManager(builder.build());

        getReplies(replyList, tweetId, progressSpinner, none);

        return layout;
    }

    public void getReplies(final ListView listView, final long tweetId, final LinearLayout progressSpinner, final HoloTextView none) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Twitter twitter = Utils.getTwitter(context, settings);
                final ArrayList<twitter4j.Status> replies = new ArrayList<twitter4j.Status>();
                try {
                    twitter4j.Status status = twitter.showStatus(tweetId);
                    twitter4j.Status replyStatus = twitter.showStatus(status.getInReplyToStatusId());

                    try {
                        while(!replyStatus.getText().equals("")) {
                            replies.add(replyStatus);
                            Log.v("reply_status", replyStatus.getText());

                            replyStatus = twitter.showStatus(replyStatus.getInReplyToStatusId());
                        }
                    } catch (Exception e) {
                        // the list of replies has ended, but we dont want to go to null
                    }

                } catch (TwitterException e) {
                    e.printStackTrace();
                }

                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressSpinner.setVisibility(View.GONE);

                        try {
                            if (replies.size() > 0) {

                                ArrayList<twitter4j.Status> reversed = new ArrayList<twitter4j.Status>();
                                for (int i = replies.size() - 1; i >= 0; i--) {
                                    reversed.add(replies.get(i));
                                }

                                listView.setAdapter(new TimelineArrayAdapter(context, reversed));
                                listView.setVisibility(View.VISIBLE);
                            } else {
                                none.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            // none and it got the null object
                            listView.setVisibility(View.GONE);
                            none.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }).start();
    }
}
