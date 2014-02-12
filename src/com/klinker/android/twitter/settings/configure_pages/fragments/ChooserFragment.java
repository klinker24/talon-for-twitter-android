package com.klinker.android.twitter.settings.configure_pages.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.widgets.HoloTextView;


public abstract class ChooserFragment extends Fragment {

    protected Context context;
    protected ActionBar actionBar;
    protected HoloTextView current;

    public static int type = AppSettings.PAGE_TYPE_NONE;
    public static int listId = 0;
    public static String listName = "";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        actionBar = activity.getActionBar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View layout = inflater.inflate(R.layout.main_fragments, null);

        current = (HoloTextView) layout.findViewById(R.id.current);
        current.setText(getResources().getString(R.string.current) + ": \n" + getResources().getString(R.string.dont_use));

        Button dontUse = (Button) layout.findViewById(R.id.dont_use);
        dontUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                current.setText(getResources().getString(R.string.current) + ": \n" + getResources().getString(R.string.dont_use));
                type = AppSettings.PAGE_TYPE_NONE;
            }
        });

        Button pics = (Button) layout.findViewById(R.id.use_pics);
        pics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                current.setText(getResources().getString(R.string.current) + ": \n" + getResources().getString(R.string.picture_page));
                type = AppSettings.PAGE_TYPE_PICS;
            }
        });

        Button links = (Button) layout.findViewById(R.id.use_links);
        links.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                current.setText(getResources().getString(R.string.current) + ": \n" + getResources().getString(R.string.link_page));
                type = AppSettings.PAGE_TYPE_LINKS;
            }
        });

        Button list = (Button) layout.findViewById(R.id.use_list);
        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //current.setText(getResources().getString(R.string.current) + ": \n" + getResources().getString(R.string.list_page));
            }
        });


        return layout;
    }

    public static int RESULT_OK = 2;
    public static int RESULT_CANCELED = 3;
    public static int REQUEST_LIST = 1;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_LIST) {

            if(resultCode == RESULT_OK) {
                listId = data.getIntExtra("listId", 0);
                listName = data.getStringExtra("listName");
                current.setText(getResources().getString(R.string.current) + ": \n" + getResources().getString(R.string.list_page));
            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result
                current.setText(getResources().getString(R.string.current) + ": \n" + getResources().getString(R.string.dont_use));
            }
        }
    }

    protected abstract int getPage();
}
