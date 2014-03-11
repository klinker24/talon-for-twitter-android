package com.klinker.android.twitter.ui.drawer_activities.discover.people;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.CategoriesArrayAdapter;
import com.klinker.android.twitter.adapters.TrendsArrayAdapter;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import twitter4j.Category;
import twitter4j.GeoLocation;
import twitter4j.ResponseList;
import twitter4j.Trend;
import twitter4j.Twitter;
import twitter4j.UserList;
import twitter4j.api.SuggestedUsersResources;


public class CategoryFragment extends Fragment {

    private Context context;
    private SharedPreferences sharedPrefs;
    private AppSettings settings;

    private AsyncListView listView;
    private View layout;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = AppSettings.getInstance(context);

        layout = inflater.inflate(R.layout.trends_list_view, null);

        listView = (AsyncListView) layout.findViewById(R.id.listView);

        if (DrawerActivity.translucent) {
            if (Utils.hasNavBar(context)) {
                View footer = new View(context);
                footer.setOnClickListener(null);
                footer.setOnLongClickListener(null);
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
                footer.setLayoutParams(params);
                listView.addFooterView(footer);
                listView.setFooterDividersEnabled(false);
            }
        }

        getSuggestions();

        return layout;
    }

    public void getSuggestions() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, DrawerActivity.settings);

                    int i = 0;

                    final ResponseList<Category> categories = twitter.getSuggestedUserCategories();

                    Collections.sort(categories, new Comparator<Category>() {
                        public int compare(Category result1, Category result2) {
                            return result1.getName().compareTo(result2.getName());
                        }
                    });

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setAdapter(new CategoriesArrayAdapter(context, categories));
                            listView.setVisibility(View.VISIBLE);

                            LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
                            spinner.setVisibility(View.GONE);
                        }
                    });
                } catch (Throwable e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Couldn't find your location!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

}