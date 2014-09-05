package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.util.ArrayList;

import twitter4j.User;

/**
 * Created by luke on 1/2/14.
 */
public class FAQArrayAdapter extends ArrayAdapter<User> {

    private Context context;

    private ArrayList<String[]> text;

    private LayoutInflater inflater;
    private AppSettings settings;

    public static class ViewHolder {
        public TextView text;
    }

    public FAQArrayAdapter(Context context, ArrayList<String[]> text) {
        super(context, R.layout.tweet);

        this.context = context;
        this.text = text;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

    }

    @Override
    public int getCount() {
        return text.size();
    }

    public View newView(ViewGroup viewGroup) {
        View v = inflater.inflate(R.layout.text, viewGroup, false);;
        final ViewHolder holder = new ViewHolder();

        holder.text = (TextView) v.findViewById(R.id.text);
        holder.text.setTextSize(20);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, final String[] faq) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.text.setText(faq[0]); // name of the faq
        holder.text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(faq[1]))); // open the link
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
        }

        bindView(v, context, text.get(position));

        return v;
    }
}