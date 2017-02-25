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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;

import twitter4j.Twitter;
import twitter4j.User;

public class UserListMembersArrayAdapter extends PeopleArrayAdapter {

    private long listId;

    public UserListMembersArrayAdapter(Context context, ArrayList<User> users, long listId) {
        super(context, users);
        this.listId = listId;
    }

    @Override
    public void bindView(final View view, int position, final User user) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final long id = user.getId();
        holder.userId = id;

        holder.name.setText(user.getName());
        holder.screenName.setText("@" + user.getScreenName());

        final String url = user.getOriginalProfileImageURL();

        Glide.with(context).load(url).into(holder.picture);

        holder.picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProfilePager.start(context, user);
            }
        });

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.remove_user))
                        .setMessage(context.getResources().getString(R.string.remove) + " " + user.getName() + " " + context.getResources().getString(R.string.from_list) + "?")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    new RemoveUser().execute(user.getId() + "");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    class RemoveUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                twitter.destroyUserListMember(listId, Long.parseLong(urls[0]));

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean removed) {
            if (removed) {
                Toast.makeText(context, R.string.removed_user, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
