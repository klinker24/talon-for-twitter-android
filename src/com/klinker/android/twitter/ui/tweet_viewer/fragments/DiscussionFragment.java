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
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.bitmapcache.BitmapLruCache;

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

        layout = inflater.inflate(R.layout.conversation_fragment, null);
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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new GetReplies(replyList, tweetId, progressSpinner, none).execute();
            }
        }, 1500);

        return layout;
    }

    class GetReplies extends AsyncTask<String, Void, ArrayList<Status>> {

        private ListView listView;
        private long tweetId;
        private LinearLayout progressSpinner;
        private HoloTextView none;

        public GetReplies(ListView listView, long tweetId, LinearLayout progressBar, HoloTextView none) {
            this.listView = listView;
            this.tweetId = tweetId;
            this.progressSpinner = progressBar;
            this.none = none;
        }

        protected ArrayList<twitter4j.Status> doInBackground(String... urls) {
            Twitter twitter = Utils.getTwitter(context, settings);
            try {
                Query query = new Query("@" + screenname + " since_id:" + tweetId);
                try {
                    query.setCount(100);
                } catch ( Exception e) {
                    // enlarge buffer error?
                    query.setCount(30);
                }
                QueryResult result=twitter.search(query);

                ArrayList<twitter4j.Status> all = new ArrayList<twitter4j.Status>();

                do{
                    List<twitter4j.Status> tweets = result.getTweets();
                    for(twitter4j.Status tweet: tweets){
                        if (tweet.getInReplyToStatusId() == tweetId) {
                            all.add(tweet);
                        }
                    }
                    query=result.nextQuery();
                    if(query!=null)
                        result=twitter.search(query);
                }while(query!=null);

                return all;

            } catch (TwitterException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<twitter4j.Status> replies) {
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
    }
}
