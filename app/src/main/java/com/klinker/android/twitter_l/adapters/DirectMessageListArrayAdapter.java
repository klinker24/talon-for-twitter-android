package com.klinker.android.twitter_l.adapters;
/*
 * Copyright 2013 Luke Klinker
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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.DirectMessage;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.DMSQLiteHelper;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.DirectMessageConversation;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;

import twitter4j.Twitter;
import twitter4j.User;

public class DirectMessageListArrayAdapter extends ArrayAdapter<User> {

    public Context context;

    public ArrayList<DirectMessage> messages;

    public LayoutInflater inflater;
    public AppSettings settings;

    public int layout;
    public Resources res;

    public static class ViewHolder {
        public TextView name;
        public TextView text;
        public ImageView picture;
        public LinearLayout background;
    }

    public DirectMessageListArrayAdapter(Context context, ArrayList<DirectMessage> messages) {
        super(context, R.layout.person);

        this.context = context;
        this.messages = messages;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

        setUpLayout();

    }

    public void setUpLayout() {
        layout = R.layout.person;
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    public View newView(ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.text = (TextView) v.findViewById(R.id.screen_name);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (ImageView) v.findViewById(R.id.profile_pic);

        holder.text.setSingleLine(true);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, final DirectMessage dm) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.name.setText(settings.displayScreenName ? "@" + dm.getScreenname() : dm.getName());
        String tweetText = dm.getMessage();
        if (tweetText.contains("<font")) {
            if (settings.addonTheme) {
                holder.text.setText(Html.fromHtml(tweetText.replaceAll("FF8800", settings.accentColor).replaceAll("\n", "<br/>")));
            } else {
                holder.text.setText(Html.fromHtml(tweetText.replaceAll("\n", "<br/>")));
            }
        } else {
            holder.text.setText(tweetText);
        }

        Glide.with(context).load(dm.getPicture()).into(holder.picture);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewConv = new Intent(context, DirectMessageConversation.class);
                viewConv.putExtra("screenname", dm.getScreenname());
                viewConv.putExtra("name", dm.getName());

                context.startActivity(viewConv);
            }
        });

        holder.background.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new DeleteConv(context, dm.getScreenname()).execute();
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

                builder.setTitle(R.string.delete_conversation);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
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

            holder.picture.setImageDrawable(null);
        }

        bindView(v, context, messages.get(position));

        return v;
    }

    class DeleteConv extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        Context context;
        SharedPreferences sharedPrefs;
        String name;

        public DeleteConv(Context context, String name) {
            this.context = context;
            sharedPrefs = AppSettings.getSharedPreferences(context);
            this.name = name;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage(context.getResources().getString(R.string.deleting_messages) + "...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {

            DMDataSource data = DMDataSource.getInstance(context);

            try {
                Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));

                Cursor cursor = data.getConvCursor(name, settings.currentAccount);

                if (cursor.moveToFirst()) {
                    do {
                        long id = cursor.getLong(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_TWEET_ID));
                        data.deleteTweet(id);
                        try {
                            twitter.destroyDirectMessageEvent(id);
                        } catch (Exception x) {
                            // it doesn't actually exist on the twitter side
                        }
                    } while (cursor.moveToNext());
                }

                data.deleteDups(settings.currentAccount);

                return true;

            } catch (Exception e) {
                // they have no direct messages
                return true;
            }


        }

        protected void onPostExecute(Boolean deleted) {
            try {
                pDialog.dismiss();
                Toast.makeText(context, context.getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
            } catch (IllegalStateException e) {
                // view not attached
            } catch (IllegalArgumentException e) {

            }

            context.sendBroadcast(new Intent("com.klinker.android.twitter.UPDATE_DM"));
            sharedPrefs.edit().putLong("last_direct_message_id_" + settings.currentAccount, 0).apply();
        }
    }
}