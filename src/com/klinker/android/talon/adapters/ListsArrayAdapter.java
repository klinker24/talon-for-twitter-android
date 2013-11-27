package com.klinker.android.talon.adapters;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.ui.ChoosenListActivity;
import com.klinker.android.talon.ui.ViewUsers;
import com.klinker.android.talon.ui.drawer_activities.ListsActivity;
import com.klinker.android.talon.ui.drawer_activities.Search;
import com.klinker.android.talon.utils.Utils;

import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.UserList;

public class ListsArrayAdapter extends ArrayAdapter<User> {

    private Context context;

    private ResponseList<UserList> lists;

    private LayoutInflater inflater;
    private AppSettings settings;

    public static class ViewHolder {
        public TextView text;
    }

    public ListsArrayAdapter(Context context, ResponseList<UserList> lists) {
        super(context, R.layout.tweet);

        this.context = context;
        this.lists = lists;

        settings = new AppSettings(context);
        inflater = LayoutInflater.from(context);

    }

    @Override
    public int getCount() {
        return lists.size();
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

    public void bindView(final View view, Context mContext, final UserList list) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String name = list.getName();
        final String id = list.getId() + "";

        holder.text.setText(name);

        holder.text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent list = new Intent(context, ChoosenListActivity.class);
                list.putExtra("list_id", id);
                list.putExtra("list_name", name);
                list.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                context.startActivity(list);
            }
        });

        holder.text.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(R.array.lists_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final int DELETE_LIST = 0;
                        final int VIEW_USERS = 1;
                        switch (i) {
                            case DELETE_LIST:
                                new DeleteList().execute(id + "");
                                break;

                            case VIEW_USERS:
                                Intent viewUsers = new Intent(context, ViewUsers.class);
                                viewUsers.putExtra("list_id", Integer.parseInt(id));
                                viewUsers.putExtra("list_name", name);
                                context.startActivity(viewUsers);
                                break;
                        }

                    }
                });
                builder.setTitle(name);

                builder.create();
                builder.show();

                return false;
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

        bindView(v, context, lists.get(position));

        return v;
    }

    class DeleteList extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                twitter.destroyUserList(Integer.parseInt(urls[0]));

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean deleted) {

            if (deleted) {
                //make deleted toast
                // back out to see changes
            } else {
                // not deleted toast
            }

        }
    }
}