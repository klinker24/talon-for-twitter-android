package com.klinker.android.twitter_l.adapters;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import android.widget.TextView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainDrawerArrayAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final ArrayList<String> text = new ArrayList<String>();
    public SharedPreferences sharedPrefs;
    public static int current = 0;
    public int textSize;

    public List<Long> listIds = new ArrayList<Long>(); // 0 is the furthest to the left
    public List<Long> userIds = new ArrayList<Long>(); // 0 is the furthest to the left
    public List<Integer> pageTypes = new ArrayList<Integer>();
    public List<String> pageNames = new ArrayList<String>();
    public List<String> searchPages = new ArrayList<String>();
    public List<String> searchNames = new ArrayList<String>();

    public Set<String> shownItems;

    static class ViewHolder {
        public TextView name;
        public ImageView icon;
    }

    public static String[] getItems(Context context1) {
        String[] items = new String[] {
                context1.getResources().getString(R.string.discover),
                context1.getResources().getString(R.string.lists),
                context1.getResources().getString(R.string.favorite_users),
                context1.getResources().getString(R.string.retweets),
                context1.getResources().getString(R.string.favorite_tweets),
                context1.getResources().getString(R.string.saved_searches) };

        return items;
    }

    public MainDrawerArrayAdapter(Context context) {
        super(context, 0);
        this.context = (Activity) context;
        this.sharedPrefs = AppSettings.getSharedPreferences(context);

        textSize = 15;

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String listIdentifier = "account_" + currentAccount + "_list_" + (i + 1) + "_long";
            String userIdentifier = "account_" + currentAccount + "_user_tweets_" + (i + 1) + "_long";
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            String nameIdentifier = "account_" + currentAccount + "_name_" + (i + 1);
            String searchIdentifier = "account_" + currentAccount + "_search_" + (i + 1);

            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type != AppSettings.PAGE_TYPE_NONE) {
                pageTypes.add(type);
                listIds.add(sharedPrefs.getLong(listIdentifier, 0l));
                userIds.add(sharedPrefs.getLong(userIdentifier, 0l));
                pageNames.add(sharedPrefs.getString(nameIdentifier, ""));
                searchNames.add(sharedPrefs.getString(searchIdentifier, ""));
            }
        }

        for (int i = 0; i < pageTypes.size(); i++) {
            switch (pageTypes.get(i)) {
                case AppSettings.PAGE_TYPE_SECOND_MENTIONS:
                    text.add(AppSettings.getInstance(context).secondScreenName);
                    break;
                case AppSettings.PAGE_TYPE_SAVED_SEARCH:
                    text.add(searchNames.get(i));
                    searchPages.add(pageNames.get(i));
                    break;
                case AppSettings.PAGE_TYPE_LIST:
                case AppSettings.PAGE_TYPE_USER_TWEETS:
                    text.add(pageNames.get(i));
                    break;
                default:
                    text.add(getName(pageTypes.get(i)));
                    break;
            }
        }

        for (String s : getItems(context)) {
            text.add(s);
            pageTypes.add(-1);
        }

        shownItems = sharedPrefs.getStringSet("drawer_elements_shown_" + currentAccount, new HashSet<String>());

        for (int i = text.size() - 1; i >= 0; i--) {
            if (!shownItems.contains(i + "")) {
                text.remove(i);
                pageTypes.remove(i);
            }
        }
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

        try {
            int iconRes = -1;

            int pageType = pageTypes.get(position);
            String pageName = text.get(position);

            if (pageType == AppSettings.PAGE_TYPE_HOME) {
                iconRes = R.attr.timelineItem;
            } else if (pageType == AppSettings.PAGE_TYPE_MENTIONS || pageType == AppSettings.PAGE_TYPE_SECOND_MENTIONS) {
                iconRes = R.attr.mentionItem;
            } else if (pageType == AppSettings.PAGE_TYPE_DMS) {
                iconRes = R.attr.directMessageItem;
            } else if (pageName.equals(context.getResources().getString(R.string.retweets))) {
                iconRes = R.attr.retweetButton;
            } else if (pageName.equals(context.getResources().getString(R.string.favorite_tweets)) || pageType == AppSettings.PAGE_TYPE_SAVED_TWEETS) {
                iconRes = R.attr.heart_button;
            } else if (pageName.equals(context.getResources().getString(R.string.favorite_users)) || pageType == AppSettings.PAGE_TYPE_FAV_USERS || pageType == AppSettings.PAGE_TYPE_USER_TWEETS) {
                iconRes = R.attr.favUser;
            } else if (pageName.equals(context.getResources().getString(R.string.discover)) || pageName.equals(context.getString(R.string.world_trends)) || pageName.equals(context.getString(R.string.local_trends))) {
                iconRes = R.attr.drawerTrends;
            } else if (text.get(position).equals(context.getResources().getString(R.string.lists)) || pageType == AppSettings.PAGE_TYPE_LIST) {
                iconRes = R.attr.listIcon;
            } else if (pageName.equals(context.getResources().getString(R.string.saved_searches)) || pageTypes.get(position) == AppSettings.PAGE_TYPE_SAVED_SEARCH) {
                iconRes = R.attr.search_icon;
            } else if (pageType == AppSettings.PAGE_TYPE_LINKS) {
                iconRes = R.attr.links;
            } else if (pageType == AppSettings.PAGE_TYPE_PICS) {
                iconRes = R.attr.picturePlaceholder;
            } else if (pageType == AppSettings.PAGE_TYPE_ACTIVITY) {
                iconRes = R.attr.notification_button;
            } else {
                iconRes = R.attr.favUser;
            }

            if (pageType == AppSettings.PAGE_TYPE_ACTIVITY) {
                if (AppSettings.getInstance(context).darkTheme) {
                    holder.icon.setAlpha(.8f);
                } else {
                    holder.icon.setAlpha(.6f);
                }
            } else {
                holder.icon.setAlpha(1.0f);
            }

            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{iconRes});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.icon.setImageResource(resource);
        } catch (Exception e) {

        }

        if (highlightedCurrent == position) {
            holder.icon.setColorFilter(DrawerActivity.settings.themeColors.accentColor);
            holder.name.setTextColor(DrawerActivity.settings.themeColors.accentColor);

            rowView.setBackgroundColor(Color.parseColor("#09000000"));
        } else {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.textColor});
            int resource = a.getResourceId(0, 0);

            holder.icon.setColorFilter(context.getResources().getColor(resource));
            holder.name.setTextColor(context.getResources().getColor(resource));

            rowView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        return rowView;
    }

    public static int highlightedCurrent;
    public static void setCurrent(Context context, int i) {
        current = i;
        highlightedCurrent = i;

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        Set<String> shownItems = sharedPrefs.getStringSet("drawer_elements_shown_" + currentAccount, new HashSet<String>());
        for (int index = 0; index <= highlightedCurrent; index++) {
            if (!shownItems.contains(index + "")) {
                highlightedCurrent--;
            }
        }
    }

    public String getName(int type) {
        switch (type) {
            case AppSettings.PAGE_TYPE_HOME:
                return context.getResources().getString(R.string.timeline);
            case AppSettings.PAGE_TYPE_MENTIONS:
                return context.getResources().getString(R.string.mentions);
            case AppSettings.PAGE_TYPE_DMS:
                return context.getResources().getString(R.string.direct_messages);
            case AppSettings.PAGE_TYPE_WORLD_TRENDS:
                return context.getResources().getString(R.string.world_trends);
            case AppSettings.PAGE_TYPE_LOCAL_TRENDS:
                return context.getString(R.string.local_trends);
            case AppSettings.PAGE_TYPE_ACTIVITY:
                return context.getString(R.string.activity);
            case AppSettings.PAGE_TYPE_FAVORITE_STATUS:
                return context.getString(R.string.favorite_tweets);
            case AppSettings.PAGE_TYPE_LINKS:
                return context.getResources().getString(R.string.links);
            case AppSettings.PAGE_TYPE_PICS:
                return context.getResources().getString(R.string.pictures);
            case AppSettings.PAGE_TYPE_FAV_USERS:
                return context.getString(R.string.favorite_users_tweets);
            case AppSettings.PAGE_TYPE_SAVED_TWEETS:
                return context.getString(R.string.saved_tweets);
        }

        return null;
    }
}