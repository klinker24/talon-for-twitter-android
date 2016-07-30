package com.klinker.android.twitter_l.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.PeopleArrayAdapter;
import com.klinker.android.twitter_l.views.widgets.PopupLayout;
import twitter4j.User;

import java.util.List;

public class RetweetersPopupLayout extends PopupLayout {

    private ListView listView;
    private LinearLayout spinner;
    private LinearLayout noContent;
    protected TextView noContentText;

    public RetweetersPopupLayout(Context context) {
        super(context);
    }

    @Override
    public View setMainLayout() {
        View retweets = LayoutInflater.from(getContext()).inflate(R.layout.list_view_activity, (ViewGroup) getRootView(), false);
        spinner = (LinearLayout) retweets.findViewById(R.id.list_progress);
        noContent = (LinearLayout) retweets.findViewById(R.id.no_content);
        noContentText = (TextView) retweets.findViewById(R.id.no_content_text);
        listView = (ListView) retweets.findViewById(R.id.listView);

        setUserWindowTitle();

        return retweets;
    }

    public void setUserWindowTitle() {

    }

    public void setData(List<User> users) {
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
