package com.klinker.android.twitter.ui.tweet_viewer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import uk.co.senab.bitmapcache.BitmapLruCache;

/**
 * This fragment is not used anymore. It was combined with the ConversationFragment for unified
 * conversations in the tweet viewer.
 */
public class DiscussionFragment extends Fragment {
    private Context context;
    private View layout;
    private AppSettings settings;
    private long tweetId;
    private String screenname;

    public DiscussionFragment(AppSettings settings, long tweetId, String screenname) {
        this.settings = settings;
        this.tweetId = tweetId;
        this.screenname = screenname;
    }

    public DiscussionFragment() {
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

        layout = inflater.inflate(R.layout.conversation_fragment, null, false);
        final AsyncListView replyList = (AsyncListView) layout.findViewById(R.id.listView);
        final LinearLayout progressSpinner = (LinearLayout) layout.findViewById(R.id.list_progress);
        final HoloTextView none = (HoloTextView) layout.findViewById(R.id.no_conversation);
        none.setText(getResources().getString(R.string.no_replies));

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        replyList.setItemManager(builder.build());

        getReplies(replyList, tweetId, progressSpinner, none);

        return layout;
    }

    public Query query;

    public void getReplies(final ListView listView, final long tweetId, final LinearLayout progressBar, final HoloTextView none) {

        Thread getReplies = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<twitter4j.Status> all = null;
                Twitter twitter = Utils.getTwitter(context, settings);
                try {
                    twitter4j.Status status = twitter.showStatus(tweetId);

                    boolean isRetweet = status.isRetweet();

                    if (isRetweet) {
                        status = status.getRetweetedStatus();
                    }

                    long id = status.getId();
                    String screenname = status.getUser().getScreenName();

                    Log.v("talon_retweet", screenname + " " + id);

                    query = new Query("@" + screenname +
                            " since_id:" + id);

                    try {
                        query.setCount(100);
                    } catch (Exception e) {
                        // enlarge buffer error?
                        query.setCount(30);
                    }

                    QueryResult result = twitter.search(query);

                    all = new ArrayList<twitter4j.Status>();

                    do {
                        List<twitter4j.Status> tweets = result.getTweets();

                        for(twitter4j.Status tweet : tweets){
                            if (tweet.getInReplyToStatusId() == id) {
                                all.add(tweet);
                            }
                        }

                        query = result.nextQuery();

                        if (query != null)
                            result = twitter.search(query);

                    } while (query != null);

                } catch (Exception e) {
                    e.printStackTrace();
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                }

                final ArrayList<Status> fAll = all;

                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        try {
                            if (fAll.size() > 0) {

                                ArrayList<twitter4j.Status> reversed = new ArrayList<twitter4j.Status>();
                                for (int i = fAll.size() - 1; i >= 0; i--) {
                                    reversed.add(fAll.get(i));
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
        });

        getReplies.setPriority(8);
        getReplies.start();

    }
}
