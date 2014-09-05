package com.klinker.android.twitter_l.manipulations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ArrayListLoader;
import com.klinker.android.twitter_l.adapters.PeopleArrayAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.User;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.util.ArrayList;

public class RetweetersPopupLayout extends PopupLayout {

    private AsyncListView listView;
    private LinearLayout spinner;
    private LinearLayout noContent;

    public RetweetersPopupLayout(Context context) {
        super(context);
    }

    public RetweetersPopupLayout(Context context, boolean windowed) {
        super(context, windowed);
    }

    @Override
    public View setMainLayout() {
        View retweets = LayoutInflater.from(getContext()).inflate(R.layout.list_view_activity, null, false);
        spinner = (LinearLayout) retweets.findViewById(R.id.list_progress);
        noContent = (LinearLayout) retweets.findViewById(R.id.no_content);
        listView = (AsyncListView) retweets.findViewById(R.id.listView);

        BitmapLruCache cache = App.getInstance(getContext()).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, getContext());

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
        builder.setThreadPoolSize(2);

        listView.setItemManager(builder.build());

        return retweets;
    }

    public void setData(ArrayList<User> users) {
        if (users != null) {
            listView.setAdapter(new PeopleArrayAdapter(getContext(), users));
        }

        if (users.size() == 0) {
            listView.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
            noContent.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.GONE);
            noContent.setVisibility(View.GONE);
        }
    }
}
