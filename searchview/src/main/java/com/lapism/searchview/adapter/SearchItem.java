package com.lapism.searchview.adapter;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.lapism.searchview.R;


public class SearchItem implements Parcelable {

    public static final Parcelable.Creator<SearchItem> CREATOR = new Parcelable.Creator<SearchItem>() {
        public SearchItem createFromParcel(Parcel source) {
            return new SearchItem(source);
        }

        public SearchItem[] newArray(int size) {
            return new SearchItem[size];
        }
    };
    private int icon;
    private CharSequence text;

    public SearchItem() {
    }

    public SearchItem(CharSequence text) {
        this(text, R.drawable.search_ic_search_black_24dp);
    }

    public SearchItem(CharSequence text, int icon) {
        this.icon = icon;
        this.text = text;
    }

    private SearchItem(Parcel in) {
        this.icon = in.readInt();
        this.text = in.readParcelable(CharSequence.class.getClassLoader());
    }

    public int get_icon() {
        return this.icon;
    }

    public void set_icon(int icon) {
        this.icon = icon;
    }

    public CharSequence get_text() {
        return this.text;
    }

    public void set_text(CharSequence text) {
        this.text = text;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.icon);
        TextUtils.writeToParcel(this.text, dest, flags); // dest.writeValue(this.text);
    }

}