package com.klinker.android.twitter_l.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.ScheduledTweet;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ScheduledArrayAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final ArrayList<ScheduledTweet> text;

    private AppSettings settings;

    static class ViewHolder {
        public TextView tweet;
        public TextView time;
    }

    public ScheduledArrayAdapter(Activity context, ArrayList<ScheduledTweet> text) {
        super(context, R.layout.scheduled_tweet_item);
        this.context = context;
        this.text = text;
        this.settings = AppSettings.getInstance(context);
    }

    @Override
    public int getCount() {
        return text.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        Date sendDate;

        try {
            sendDate = new Date(text.get(position).time);
        } catch (Exception e) {
            sendDate = new Date(0);
        }

        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.scheduled_tweet_item, null);

            ViewHolder viewHolder = new ViewHolder();

            viewHolder.tweet = (TextView) rowView.findViewById(R.id.tweet);
            viewHolder.time = (TextView) rowView.findViewById(R.id.time);

            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        String dateString;

        if (settings.militaryTime) {
            dateString = DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN).format(sendDate);
        } else {
            dateString = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(sendDate);
        }

        if (settings.militaryTime) {
            dateString += " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN).format(sendDate);
        } else {
            dateString += " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(sendDate);
        }

        holder.time.setText(dateString);
        holder.tweet.setText(text.get(position).text);

        return rowView;
    }
}
