package com.klinker.android.talon.Adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.klinker.android.talon.R;

import java.util.ArrayList;

public class MainDrawerArrayAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final ArrayList<String> text;
    public SharedPreferences sharedPrefs;
    public static int current = 0;

    static class ViewHolder {
        public TextView name;
        public ImageView icon;
    }

    public MainDrawerArrayAdapter(Context context, ArrayList<String> text) {
        super(context, 0);
        this.context = (Activity) context;
        this.text = text;
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public int getCount() {
        return text.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        String settingName = text.get(position);

        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.drawer_list_item, null);

            ViewHolder viewHolder = new ViewHolder();

            viewHolder.name = (TextView) rowView.findViewById(R.id.title);
            viewHolder.icon = (ImageView) rowView.findViewById(R.id.icon);

            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        holder.name.setText(settingName);

        if (text.get(position).equals(context.getResources().getString(R.string.timeline))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.timelineItem});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        } else if (text.get(position).equals(context.getResources().getString(R.string.mentions))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.mentionItem});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        } else if (text.get(position).equals(context.getResources().getString(R.string.direct_messages))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.directMessageItem});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        }

        if (current == position) {
            holder.icon.setColorFilter(context.getResources().getColor(R.color.app_color));
            holder.name.setTextColor(context.getResources().getColor(R.color.app_color));
        } else {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.textColor});
            int resource = a.getResourceId(0, 0);

            holder.icon.setColorFilter(context.getResources().getColor(resource));
            holder.name.setTextColor(context.getResources().getColor(resource));
        }

        return rowView;
    }
}