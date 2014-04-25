package com.klinker.android.twitter.widget.launcher_fragment.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.klinker.android.launcher.api.ResourceHelper;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;

import java.util.ArrayList;

/**
 * Created by luke on 4/19/14.
 */
public class DrawerArrayAdapter extends ArrayAdapter<String> {

    private static final int TYPE_PIC = 1;
    private static final int TYPE_LINK = 2;
    private static final int TYPE_LIST = 3;
    private static final int TYPE_FAV_USERS = 4;
    private static final int TYPE_HOME = 5;
    private static final int TYPE_MENTION = 6;
    private static final int TYPE_DM = 7;

    private Context context;
    private ArrayList<Integer> types = new ArrayList<Integer>();
    public SharedPreferences sharedPrefs;
    public ResourceHelper helper;
    public AppSettings settings;

    private String color;

    // list stuff
    public long list1Id; // furthest left list
    public long list2Id; // list next to the timeline
    public int page1Type;
    public int page2Type;
    public String page1Name;
    public String page2Name;

    public int numExtraPages = 0;

    static class ViewHolder {
        public ImageView icon;
    }

    public DrawerArrayAdapter(Context context) {
        super(context, 0);
        this.context = context;
        this.sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        helper = new ResourceHelper(context, "com.klinker.android.twitter");

        settings = new AppSettings(sharedPrefs, context);

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        // List ID's
        list1Id = sharedPrefs.getLong("account_" + currentAccount + "_list_1_long", 0l);
        list2Id = sharedPrefs.getLong("account_" + currentAccount + "_list_2_long", 0l);
        page1Type = sharedPrefs.getInt("account_" + currentAccount + "_page_1", AppSettings.PAGE_TYPE_NONE);
        page2Type = sharedPrefs.getInt("account_" + currentAccount + "_page_2", AppSettings.PAGE_TYPE_NONE);
        page1Name = sharedPrefs.getString("account_" + currentAccount + "_name_1", "");
        page2Name = sharedPrefs.getString("account_" + currentAccount + "_name_2", "");

        if (page1Type != AppSettings.PAGE_TYPE_NONE) {
            numExtraPages++;
            types.add(page1Type);
        }

        if (page2Type != AppSettings.PAGE_TYPE_NONE) {
            numExtraPages++;
            types.add(page2Type);
        }

        types.add(TYPE_HOME);
        types.add(TYPE_MENTION);
        types.add(TYPE_DM);

        if (settings.theme == AppSettings.THEME_LIGHT) {
            color = "light";
        } else {
            color = "dark";
        }
    }

    public int getNumExtraPages() {
        return numExtraPages;
    }

    @Override
    public int getCount() {
        return types.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        if (rowView == null) {
            rowView = helper.getLayout("launcher_drawer_item_" + color);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) rowView.findViewById(helper.getId("icon"));
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        switch (types.get(position)) {
            case TYPE_PIC:
                holder.icon.setImageDrawable(helper.getDrawable("ic_action_picture_" + color));
                break;
            case TYPE_LINK:
                holder.icon.setImageDrawable(helper.getDrawable("trends_" + color));
                break;
            case TYPE_LIST:
                holder.icon.setImageDrawable(helper.getDrawable("list_" + color));
                break;
            case TYPE_FAV_USERS:
                holder.icon.setImageDrawable(helper.getDrawable("drawer_user_" + color));
                break;
            case TYPE_HOME:
                holder.icon.setImageDrawable(helper.getDrawable("timeline_" + color));
                break;
            case TYPE_MENTION:
                holder.icon.setImageDrawable(helper.getDrawable("mentions_" + color));
                break;
            case TYPE_DM:
                holder.icon.setImageDrawable(helper.getDrawable("ic_action_reply_" + color));
                break;
        }

        if (TYPE_HOME == types.get(position)) {
            if (!settings.addonTheme) {
                holder.icon.setColorFilter(helper.getColor("app_color"));
            } else {
                holder.icon.setColorFilter(settings.accentInt);
            }
        } else {
            holder.icon.setColorFilter(helper.getColor(color + "_text"));
        }

        return rowView;
    }
}