package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.drawer_activities.discover.trends.SearchedTrendsActivity;
import com.klinker.android.twitter_l.ui.search.SearchPager;
import com.lapism.searchview.adapter.SearchAdapter;
import com.lapism.searchview.adapter.SearchItem;
import com.lapism.searchview.view.SearchCodes;

import java.util.ArrayList;
import java.util.List;

public class SearchUtils {

    private Activity activity;

    private com.lapism.searchview.view.SearchView mSearchView;
    private List<SearchItem> mSuggestionsList = new ArrayList<>();

    public SearchUtils(Activity activity) {
        this.activity = activity;
    }

    public void setUpSearch() {
        mSearchView = (com.lapism.searchview.view.SearchView) activity.findViewById(R.id.searchView);

        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ||
                correctActivity() ||
                activity.getResources().getBoolean(R.bool.isTablet))
            mSearchView.setTranslationY(Utils.getStatusBarHeight(activity));

        mSearchView.setTheme(AppSettings.getInstance(activity).darkTheme ? SearchCodes.THEME_DARK : SearchCodes.THEME_LIGHT);

        mSearchView.setOnQueryTextListener(new com.lapism.searchview.view.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.hide(false);
                startSearchIntent(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        List<SearchItem> mResultsList = new ArrayList<>();
        SearchAdapter mSearchAdapter = new SearchAdapter(activity, mResultsList, mSuggestionsList,
                AppSettings.getInstance(activity).darkTheme ? SearchCodes.THEME_DARK : SearchCodes.THEME_LIGHT);
        mSearchAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mSearchView.hide(false);
                TextView textView = (TextView) view.findViewById(R.id.textView_item_text);
                CharSequence text = textView.getText();
                startSearchIntent(text + "");
            }
        });

        mSearchView.setAdapter(mSearchAdapter);
    }

    private void startSearchIntent(String query) {
        if (activity instanceof SearchPager) {
            Intent search = new Intent();
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, query);
            ((SearchPager) activity).handleIntent(search);

            Intent broadcast = new Intent("com.klinker.android.twitter.NEW_SEARCH");
            broadcast.putExtra("query", query);
            activity.sendBroadcast(broadcast);
        } else if (activity instanceof SearchedTrendsActivity) {
            Intent search = new Intent();
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, query);
            ((SearchedTrendsActivity) activity).handleIntent(search);
        } else {
            Intent search = new Intent(activity, SearchPager.class);
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, query);
            activity.startActivity(search);
        }
    }

    // returns true if it is one of the activities that always needs to translate the search view down.
    private boolean correctActivity() {
        return activity instanceof SearchPager;
    }

    public boolean isShowing() {
        return mSearchView.isShown();
    }

    public void hideSearch(boolean animated) {
        mSearchView.hide(animated);
    }

    public void showSearchView() {
        MySuggestionsProvider provider = new MySuggestionsProvider();

        mSuggestionsList.clear();
        mSuggestionsList.addAll(provider.getAllSuggestions(activity));
        mSearchView.show(true);
    }
}

