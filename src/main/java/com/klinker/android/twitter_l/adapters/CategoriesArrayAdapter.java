package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.drawer_activities.discover.people.PeopleSearch;

import twitter4j.Category;
import twitter4j.ResponseList;
import twitter4j.User;

/**
 * Created by luke on 2/23/14.
 */
public class CategoriesArrayAdapter extends ArrayAdapter<User> {

    protected Context context;

    private ResponseList<Category> categories;

    private LayoutInflater inflater;
    private AppSettings settings;

    public static class ViewHolder {
        public TextView text;
    }

    public CategoriesArrayAdapter(Context context, ResponseList<Category> categories) {
        super(context, R.layout.tweet);

        this.context = context;
        this.categories = categories;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

    }

    @Override
    public int getCount() {
        try {
            return categories.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public View newView(ViewGroup viewGroup) {
        View v;
        final ViewHolder holder;

        v = inflater.inflate(R.layout.text, viewGroup, false);

        holder = new ViewHolder();

        holder.text = (TextView) v.findViewById(R.id.text);

        // sets up the font sizes
        holder.text.setTextSize(24);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, final Category category) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.text.setText(category.getName());

        holder.text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent search = new Intent(context, PeopleSearch.class);
                search.putExtra("slug", category.getSlug());
                context.startActivity(search);
            }
        });

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();
        }

        bindView(v, context, categories.get(position));

        return v;
    }
}
