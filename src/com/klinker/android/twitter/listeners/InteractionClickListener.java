package com.klinker.android.twitter.listeners;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.MainDrawerArrayAdapter;
import com.klinker.android.twitter.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.UserProfileActivity;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.ui.widgets.NotificationDrawerLayout;

public class InteractionClickListener implements AdapterView.OnItemClickListener {

    private Context context;
    private NotificationDrawerLayout drawer;
    private ViewPager viewPager;
    private boolean noWait;
    private boolean extraPages;

    private SharedPreferences sharedPreferences;

    public InteractionClickListener(Context context, NotificationDrawerLayout drawer, ViewPager viewPager, boolean extraPages) {
        this.context = context;
        this.drawer = drawer;
        this.viewPager = viewPager;
        this.extraPages = extraPages;
        this.noWait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ||
                context.getResources().getBoolean(R.bool.isTablet);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        HoloTextView title = (HoloTextView) view.findViewById(R.id.title);
        String mTitle = title.getText().toString();

        if(mTitle.contains(context.getResources().getString(R.string.mentioned_by))) { // this is a mention
            if (MainDrawerArrayAdapter.current < 3) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            drawer.closeDrawer(Gravity.END);
                        } catch (Exception e) {
                            // landscape mode
                        }
                    }
                }, noWait ? 0 : 300);

                viewPager.setCurrentItem((extraPages ? 3 : 1), true);
            } else {
                final int pos = i;
                try {
                    drawer.closeDrawer(Gravity.END);
                } catch (Exception e) {
                    // landscape mode
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent.putExtra("page_to_open", (extraPages ? 3 : 1));
                        intent.putExtra("from_drawer", true);

                        sharedPreferences.edit().putBoolean("should_refresh", false).commit();

                        if (!noWait) {
                            try {
                                Thread.sleep(400);
                            } catch (Exception e) {

                            }
                        }

                        try {
                            context.startActivity(intent);
                            ((Activity)context).overridePendingTransition(0,0);
                            ((Activity)context).finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }).start();
            }
        } else if (mTitle.contains(context.getResources().getString(R.string.retweeted))) { // it is a retweet
            // open up the dialog with the users that retweeted it
        } else if (mTitle.contains(context.getResources().getString(R.string.favorited))) { // it is a favorite
            // open the dialog with the users that favorited it
        } else if (mTitle.contains(context.getResources().getString(R.string.followed))) { // someone new followed you
            // a new follower, open up the followers profile
            String username = mTitle.substring(mTitle.indexOf("@"), mTitle.length());

            Intent user = new Intent(context, UserProfileActivity.class);
            user.putExtra("screenname", username);
            user.putExtra("proPic", "");
            context.startActivity(user);
        }

        // mark it read in the sql database
        InteractionsDataSource data = new InteractionsDataSource(context);
        data.open();
        data.markRead(sharedPreferences.getInt("current_account", 1), i);
        data.close();

        // tell the system to refresh the notifications when the user opens the drawer again
        sharedPreferences.edit().putBoolean("new_notification", true).commit();
    }
}
