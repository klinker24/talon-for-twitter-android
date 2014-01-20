package com.klinker.android.twitter.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.DirectMessage;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.UserProfileActivity;
import com.klinker.android.twitter.utils.ImageUtils;

import java.util.ArrayList;

import twitter4j.User;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class DirectMessageListArrayAdapter extends ArrayAdapter<User> {

    public Context context;

    public ArrayList<DirectMessage> messages;

    public LayoutInflater inflater;
    public AppSettings settings;

    public int layout;
    public XmlResourceParser addonLayout = null;
    public Resources res;
    public boolean talonLayout;
    public BitmapLruCache mCache;
    public int border;

    public static class ViewHolder {
        public TextView name;
        public TextView text;
        public ImageView picture;
        public LinearLayout background;
    }

    public DirectMessageListArrayAdapter(Context context, ArrayList<DirectMessage> messages) {
        super(context, R.layout.tweet);

        this.context = context;
        this.messages = messages;

        settings = new AppSettings(context);
        inflater = LayoutInflater.from(context);

        setUpLayout();

    }

    public void setUpLayout() {
        talonLayout = settings.layout == AppSettings.LAYOUT_TALON;

        if (settings.addonTheme) {
            try {
                res = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                addonLayout = res.getLayout(res.getIdentifier("person", "layout", settings.addonThemePackage));
            } catch (Exception e) {
                e.printStackTrace();
                layout = talonLayout ? R.layout.person : R.layout.person_hangouts;
            }
        } else {
            layout = talonLayout ? R.layout.person : R.layout.person_hangouts;
        }

        TypedArray b;
        if (talonLayout) {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        } else {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.squareBorder});
        }
        border = b.getResourceId(0, 0);
        b.recycle();

        mCache = App.getInstance(context).getBitmapCache();
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    public View newView(ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();
        if (settings.addonTheme) {
            try {
                Context viewContext = null;

                if (res == null) {
                    res = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                }

                try {
                    viewContext = context.createPackageContext(settings.addonThemePackage, Context.CONTEXT_IGNORE_SECURITY);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (res != null && viewContext != null) {
                    int id = res.getIdentifier("person", "layout", settings.addonThemePackage);
                    v = LayoutInflater.from(viewContext).inflate(res.getLayout(id), null);


                    holder.name = (TextView) v.findViewById(res.getIdentifier("name", "id", settings.addonThemePackage));
                    holder.text = (TextView) v.findViewById(res.getIdentifier("screen_name", "id", settings.addonThemePackage));
                    holder.background = (LinearLayout) v.findViewById(res.getIdentifier("background", "id", settings.addonThemePackage));
                    holder.picture = (ImageView) v.findViewById(res.getIdentifier("profile_pic", "id", settings.addonThemePackage));
                }
            } catch (Exception e) {
                e.printStackTrace();
                v = inflater.inflate(layout, viewGroup, false);

                holder.name = (TextView) v.findViewById(R.id.name);
                holder.text = (TextView) v.findViewById(R.id.screen_name);
                holder.background = (LinearLayout) v.findViewById(R.id.background);
                holder.picture = (ImageView) v.findViewById(R.id.profile_pic);
            }
        } else {
            v = inflater.inflate(layout, viewGroup, false);

            holder.name = (TextView) v.findViewById(R.id.name);
            holder.text = (TextView) v.findViewById(R.id.screen_name);
            holder.background = (LinearLayout) v.findViewById(R.id.background);
            holder.picture = (ImageView) v.findViewById(R.id.profile_pic);
        }

        // sets up the font sizes
        holder.name.setTextSize(settings.textSize + 4);
        holder.text.setTextSize(settings.textSize);
        holder.text.setSingleLine(true);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, final DirectMessage dm) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.name.setText(settings.displayScreenName ? "@" + dm.getScreenname() : dm.getName());
        holder.text.setText(dm.getMessage());

        //holder.picture.loadImage(user.getBiggerProfileImageURL(), true, null, NetworkedCacheableImageView.CIRCLE);
        if(settings.roundContactImages) {
            ImageUtils.loadCircleImage(context, holder.picture, dm.getPicture(), mCache);
        } else {
            ImageUtils.loadImage(context, holder.picture, dm.getPicture(), mCache);
        }

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewProfile = new Intent(context, UserProfileActivity.class);
                viewProfile.putExtra("name", dm.getName());
                viewProfile.putExtra("screenname", dm.getScreenname());
                viewProfile.putExtra("proPic", dm.getPicture());
                //viewProfile.putExtra("tweetid", holder.tweetId);
                viewProfile.putExtra("retweet", false);

                context.startActivity(viewProfile);
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

            holder.picture.setImageDrawable(context.getResources().getDrawable(border));
        }

        bindView(v, context, messages.get(position));

        return v;
    }
}