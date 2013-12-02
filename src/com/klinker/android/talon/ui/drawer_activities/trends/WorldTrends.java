package com.klinker.android.talon.ui.drawer_activities.trends;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.TrendsArrayAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;

import twitter4j.Trend;
import twitter4j.Twitter;

/**
 * Created by luke on 11/29/13.
 */
public class WorldTrends extends Fragment {

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
        settings = new AppSettings(context);

        layout = inflater.inflate(R.layout.text_list_view, null);

        listView = (AsyncListView) layout.findViewById(R.id.listView);

        new GetTrends().execute();

        return layout;
    }

    class GetTrends extends AsyncTask<String, Void, ArrayList<String>> {

        protected ArrayList<String> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                twitter4j.Trends trends = twitter.getPlaceTrends(1);
                ArrayList<String> currentTrends = new ArrayList<String>();

                for(Trend t: trends.getTrends()){
                    String name = t.getName();
                    currentTrends.add(name);
                }

                return currentTrends;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(ArrayList<String> trends) {

            listView.setAdapter(new TrendsArrayAdapter(context, trends));
            listView.setVisibility(View.VISIBLE);

            LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }
}
