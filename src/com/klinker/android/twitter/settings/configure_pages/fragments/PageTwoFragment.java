package com.klinker.android.twitter.settings.configure_pages.fragments;

import com.klinker.android.twitter.settings.AppSettings;

public class PageTwoFragment extends ChooserFragment {

    public static int type = AppSettings.PAGE_TYPE_NONE;
    public static long listId = 0;
    public static String listName = "";

    public PageTwoFragment() {
        type = AppSettings.PAGE_TYPE_NONE;
        listId = 0;
        listName = "";
    }

    protected void setType(int type) {
        this.type = type;
    }
    protected void setId(long id) {
        this.listId = id;
    }
    protected void setListName(String listName) {
        this.listName = listName;
    }
}
