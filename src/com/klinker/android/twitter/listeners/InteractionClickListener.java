package com.klinker.android.twitter.listeners;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
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
import com.klinker.android.twitter.ui.BrowserActivity;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.UserProfileActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.ui.drawer_activities.trends.SearchedTrendsActivity;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.ui.widgets.NotificationDrawerLayout;

public class InteractionClickListener implements AdapterView.OnItemClickListener {

    private Context context;
    private NotificationDrawerLayout drawer;
    private ViewPager viewPager;
    private boolean extraPages;

    private SharedPreferences sharedPreferences;

    public InteractionClickListener(Context context, NotificationDrawerLayout drawer, ViewPager viewPager, boolean extraPages) {
        this.context = context;
        this.drawer = drawer;
        this.viewPager = viewPager;
        this.extraPages = extraPages;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        HoloTextView title = (HoloTextView) view.findViewById(R.id.title);
        String mTitle = title.getText().toString();

        // get the datasource ready to read/write
        InteractionsDataSource data = new InteractionsDataSource(context);
        data.open();

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
                }, 300);

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

                        try {
                            Thread.sleep(400);
                        } catch (Exception e) {

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
        } else if (mTitle.contains(context.getResources().getString(R.string.retweeted)) ||
                mTitle.contains(context.getResources().getString(R.string.favorited)) ||
                mTitle.contains(context.getResources().getString(R.string.new_favorites)) ||
                mTitle.contains(context.getResources().getString(R.string.new_retweets))) { // it is a retweet or favorite

            try {
                drawer.closeDrawer(Gravity.END);
            } catch (Exception e) {
                // landscape mode
            }

            // open up the dialog with the users that retweeted it

            final String[] fItems = data.getUsers(sharedPreferences.getInt("current_account", 1),
                    i,
                    DrawerActivity.oldInteractions.getText().toString().equals(context.getResources().getString(R.string.old_interactions))).split(" ");

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setItems(fItems, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    String touched = fItems[item];

                    Intent user = new Intent(context, UserProfileActivity.class);
                    user.putExtra("screenname", touched.replace("@", "").replace(" ", ""));
                    user.putExtra("proPic", "");
                    context.startActivity(user);

                    dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else if (mTitle.contains(context.getResources().getString(R.string.followed))) { // someone new followed you

            try {
                drawer.closeDrawer(Gravity.END);
            } catch (Exception e) {
                // landscape mode
            }

            // a new follower, open up the followers profile
            String username = mTitle.substring(mTitle.indexOf("@") + 1, mTitle.indexOf(" "));

            Intent user = new Intent(context, UserProfileActivity.class);
            user.putExtra("screenname", username);
            user.putExtra("proPic", "");
            context.startActivity(user);
        } else if (mTitle.contains(context.getResources().getString(R.string.tweeted))) {
            try {
                drawer.closeDrawer(Gravity.END);
            } catch (Exception e) {
                // landscape mode
            }

            // a new follower, open up the followers profile
            String username = mTitle.substring(mTitle.indexOf("@") + 1, mTitle.indexOf(" "));

            Intent user = new Intent(context, UserProfileActivity.class);
            user.putExtra("screenname", username);
            user.putExtra("proPic", "");
            context.startActivity(user);
        }

        // mark it read in the sql database
        data.markRead(sharedPreferences.getInt("current_account", 1), i);
        data.close();

        // tell the system to refresh the notifications when the user opens the drawer again
        sharedPreferences.edit().putBoolean("new_notification", true).commit();
    }
}
