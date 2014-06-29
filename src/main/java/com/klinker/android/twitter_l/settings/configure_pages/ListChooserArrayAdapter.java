package com.klinker.android.twitter_l.settings.configure_pages;

import android.content.Context;
import android.view.View;

import com.klinker.android.twitter_l.adapters.ListsArrayAdapter;

import twitter4j.ResponseList;
import twitter4j.UserList;


public class ListChooserArrayAdapter extends ListsArrayAdapter {

    private Context context;

    public ListChooserArrayAdapter(Context context, ResponseList<UserList> lists) {
        super(context, lists);
        this.context = context;
    }

    @Override
    public void bindView(final View view, Context mContext, final UserList list) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String name = list.getName();
        final String id = list.getId() + "";

        holder.text.setText(name);
    }
}
