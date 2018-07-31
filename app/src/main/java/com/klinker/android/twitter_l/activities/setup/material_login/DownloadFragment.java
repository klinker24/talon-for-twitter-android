package com.klinker.android.twitter_l.activities.setup.material_login;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class DownloadFragment extends Fragment {

    private MaterialLogin activity;

    public static DownloadFragment getInstance() {
        return new DownloadFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = (MaterialLogin) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_intro_download, container, false);
    }

    public void start(final MaterialLogin.Callback callback) {
        new LoadTimeLine(callback).execute();
    }

    class LoadTimeLine extends AsyncTask<Void, Void, String> {

        private MaterialLogin.Callback callback;

        public LoadTimeLine(MaterialLogin.Callback callback) {
            this.callback = callback;
        }

        protected String doInBackground(Void... args) {

            try {
                SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(activity);

                AppSettings settings = new AppSettings(activity);
                Twitter twitter = Utils.getTwitter(activity, settings);
                User user = twitter.verifyCredentials();

                if (sharedPrefs.getInt("current_account", 1) == 1) {
                    sharedPrefs.edit().putString("twitter_users_name_1", user.getName()).apply();
                    sharedPrefs.edit().putString("twitter_screen_name_1", user.getScreenName()).apply();
                    sharedPrefs.edit().putString("twitter_background_url_1", user.getProfileBannerURL()).apply();
                    sharedPrefs.edit().putString("profile_pic_url_1", user.getOriginalProfileImageURL()).apply();
                    sharedPrefs.edit().putLong("twitter_id_1", user.getId()).apply();
                } else {
                    sharedPrefs.edit().putString("twitter_users_name_2", user.getName()).apply();
                    sharedPrefs.edit().putString("twitter_screen_name_2", user.getScreenName()).apply();
                    sharedPrefs.edit().putString("twitter_background_url_2", user.getProfileBannerURL()).apply();
                    sharedPrefs.edit().putString("profile_pic_url_2", user.getOriginalProfileImageURL()).apply();
                    sharedPrefs.edit().putLong("twitter_id_2", user.getId()).apply();
                }

                // syncs 200 timeline tweets with 2 pages
                Paging paging;
                paging = new Paging(2, 100);
                List<twitter4j.Status> statuses = twitter.getHomeTimeline(paging);

                HomeDataSource dataSource = HomeDataSource.getInstance(activity);

                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
                    } catch (Exception e) {
                        dataSource = HomeDataSource.getInstance(activity);
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
                    }
                }
                paging = new Paging(1, 100);
                statuses = twitter.getHomeTimeline(paging);

                if (statuses.size() > 0) {
                    sharedPrefs.edit().putLong("last_tweet_id_" + sharedPrefs.getInt("current_account", 1), statuses.get(0).getId()).apply();
                }

                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
                    } catch (Exception e) {
                        dataSource = HomeDataSource.getInstance(activity);
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
                    }
                }

                MentionsDataSource mentionsSource = MentionsDataSource.getInstance(activity);

                // syncs 100 mentions
                paging = new Paging(1, 100);
                statuses = twitter.getMentionsTimeline(paging);

                for (twitter4j.Status status : statuses) {
                    try {
                        mentionsSource.createTweet(status, sharedPrefs.getInt("current_account", 1), false);
                    } catch (Exception e) {
                        mentionsSource = MentionsDataSource.getInstance(activity);
                        mentionsSource.createTweet(status, sharedPrefs.getInt("current_account", 1), false);
                    }
                }

                // syncs 100 Direct Messages
//                DMDataSource dmSource = DMDataSource.getInstance(activity);
//
//                try {
//                    paging = new Paging(1, 100);
//
//                    List<DirectMessage> dm = twitter.getDirectMessages(paging);
//
//                    sharedPrefs.edit().putLong("last_direct_message_id_" + sharedPrefs.getInt("current_account", 1), dm.get(0).getId()).apply();
//
//                    for (DirectMessage directMessage : dm) {
//                        try {
//                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
//                        } catch (Exception e) {
//                            dmSource = DMDataSource.getInstance(activity);
//                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
//                        }
//                    }
//
//                    List<DirectMessage> sent = twitter.getSentDirectMessages();
//
//                    for (DirectMessage directMessage : sent) {
//                        try {
//                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
//                        } catch (Exception e) {
//                            dmSource = DMDataSource.getInstance(activity);
//                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
//                        }
//                    }
//
//                } catch (Exception e) {
//                    // they have no direct messages
//                    e.printStackTrace();
//                }

                FollowersDataSource followers = FollowersDataSource.getInstance(activity);

                try {
                    int currentAccount = sharedPrefs.getInt("current_account", 1);
                    PagableResponseList<User> friendsPaging = twitter.getFriendsList(user.getId(), -1, 200);

                    for (User friend : friendsPaging) {
                        followers.createUser(friend, currentAccount);
                    }

                    long nextCursor = friendsPaging.getNextCursor();

                    final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(activity,
                            MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

                    while (nextCursor != -1) {
                        friendsPaging = twitter.getFriendsList(user.getId(), nextCursor, 200);

                        for (User friend : friendsPaging) {
                            followers.createUser(friend, currentAccount);

                            // insert them into the suggestion search provider
                            suggestions.saveRecentQuery(
                                    "@" + friend.getScreenName(),
                                    null);
                        }

                        nextCursor = friendsPaging.getNextCursor();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
                e.printStackTrace();

            }
            return null;
        }

        protected void onPostExecute(String none) {
            this.callback.onDone();
        }
    }
}
