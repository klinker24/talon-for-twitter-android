package com.klinker.android.twitter.settings.configure_pages.fragments;


import com.klinker.android.twitter.settings.AppSettings;

public class PageTwoFragment extends ChooserFragment {

    public static int type = AppSettings.PAGE_TYPE_NONE;
    public static int listId = 0;
    public static String listName = "";

    protected void setType(int type) {
        this.type = type;
    }
    protected void setId(int id) {
        this.listId = id;
    }
    protected void setListName(String listName) {
        this.listName = listName;
    }
}
