package com.klinker.android.twitter.settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.klinker.android.twitter.R;

import java.util.ArrayList;

public class DrawerArrayAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final ArrayList<String> text;
    public SharedPreferences sharedPrefs;
    public static int current = 0;

    static class ViewHolder {
        public TextView name;
        public ImageView icon;
    }

    public DrawerArrayAdapter(Activity context, ArrayList<String> text) {
        super(context, R.layout.drawer_list_item);
        this.context = context;
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

        if (text.get(position).equals(context.getResources().getString(R.string.theme_settings))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.themeSettings});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        } else if (text.get(position).equals(context.getResources().getString(R.string.sync_settings))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.syncSettings});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        } else if (text.get(position).equals(context.getResources().getString(R.string.advanced_settings))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.advancedSettings});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        } else if (text.get(position).equals(context.getResources().getString(R.string.get_help_settings))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.getHelp});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        } else if (text.get(position).equals(context.getResources().getString(R.string.whats_new))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.whatsNew});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        } else if (text.get(position).equals(context.getResources().getString(R.string.other_apps))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.otherApps});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        } else if (text.get(position).equals(context.getResources().getString(R.string.rate_it))) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.rateIt});
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