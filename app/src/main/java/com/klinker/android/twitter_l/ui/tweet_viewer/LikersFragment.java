package com.klinker.android.twitter_l.ui.tweet_viewer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.PeopleArrayAdapter;
import com.klinker.android.twitter_l.views.widgets.FontPrefTextView;
import com.klinker.android.twitter_l.utils.FavoriterUtils;

import java.util.List;

import twitter4j.User;

public class LikersFragment extends Fragment {

    private static final String ARG_TWEET_ID = "arg_tweet_id";

    public static LikersFragment getInstance(long tweetId) {
        LikersFragment fragment = new LikersFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_TWEET_ID, tweetId);

        fragment.setArguments(args);
        return fragment;
    }

    private long tweetId;

    private ListView listView;
    private LinearLayout spinner;
    private LinearLayout noContent;
    private FontPrefTextView noContentText;

    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, null);

        context = getActivity();
        tweetId = getArguments().getLong(ARG_TWEET_ID);

        View layout = inflater.inflate(R.layout.no_ptr_list_layout, null);

        listView = (ListView) layout.findViewById(R.id.listView);
        spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
        noContent = (LinearLayout) layout.findViewById(R.id.no_content);
        noContentText = (FontPrefTextView) layout.findViewById(R.id.no_retweeters_text);

        noContentText.setText(getActivity().getResources().getString(R.string.no_favorites));

        startSearch();

        return layout;
    }

    private void startSearch() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FavoriterUtils utils = new FavoriterUtils();
                    final List<User> users = utils.getFavoriters(getActivity(), tweetId);

                    if (getActivity() == null) {
                        return;
                    }

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (users.size() > 0 && getActivity() != null) {
                                listView.setAdapter(new PeopleArrayAdapter(getActivity(), users));
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