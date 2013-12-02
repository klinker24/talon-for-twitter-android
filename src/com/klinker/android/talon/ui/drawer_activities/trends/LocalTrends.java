package com.klinker.android.talon.ui.drawer_activities.trends;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.TrendsArrayAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;

import twitter4j.GeoLocation;
import twitter4j.ResponseList;
import twitter4j.Trend;
import twitter4j.Twitter;


public class LocalTrends extends Fragment implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private LocationClient mLocationClient;
    private boolean connected = false;

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

        layout = inflater.inflate(R.layout.retweets_activity, null);

        listView = (AsyncListView) layout.findViewById(R.id.listView);
        listView.setDividerHeight(toDP(5));

        mLocationClient = new LocationClient(context, this, this);

        new GetTrends().execute();

        return layout;
    }

    @Override
    public void onConnected(Bundle bundle) {
        connected = true;
    }

    @Override
    public void onDisconnected() {
        connected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    public void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    class GetTrends extends AsyncTask<String, Void, ArrayList<String>> {

        protected ArrayList<String> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                while (!connected) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {

                    }
                }

                Location location = mLocationClient.getLastLocation();

                ResponseList<twitter4j.Location> locations = twitter.getClosestTrends(new GeoLocation(location.getLatitude(),location.getLongitude()));
                twitter4j.Trends trends = twitter.getPlaceTrends(locations.get(0).getWoeid());

                ArrayList<String> currentTrends = new ArrayList<String>();

                for(Trend t: trends.getTrends()){
                    String name = t.getName();
                    currentTrends.add(name);
                }

                return currentTrends;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<String> trends) {

            if (trends != null) {
                listView.setAdapter(new TrendsArrayAdapter(context, trends));
                listView.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(context, getResources().getString(R.string.no_location), Toast.LENGTH_SHORT).show();
            }

            LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

}