package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import com.klinker.android.twitter_l.adapters.AutoCompleteHashtagAdapter;
import com.klinker.android.twitter_l.adapters.AutoCompletePeopleAdapter;
import com.klinker.android.twitter_l.adapters.AutoCompleteUserArrayAdapter;
import com.klinker.android.twitter_l.adapters.UserListMembersArrayAdapter;
import com.klinker.android.twitter_l.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HashtagDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Twitter;
import twitter4j.User;

public class UserAutoCompleteHelper {

    public interface Callback {
        void onUserSelected(User selectedUser);
    }

    private Activity context;
    private Handler handler;
    private ListPopupWindow userAutoComplete;
    private ListPopupWindow hashtagAutoComplete;
    private AutoCompleteHelper autoCompleter;
    private EditText textView;
    private Callback callback;

    private AutoCompletePeopleAdapter adapter;

    private List<User> users = new ArrayList<>();

    public static UserAutoCompleteHelper applyTo(Activity activity, EditText tv) {
        UserAutoCompleteHelper helper = new UserAutoCompleteHelper(activity);
        helper.on(tv);

        return helper;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private UserAutoCompleteHelper(Activity activity) {
        this.handler = new Handler();
        this.context = activity;
        this.autoCompleter = new AutoCompleteHelper();

        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        userAutoComplete = new ListPopupWindow(context);
        userAutoComplete.setHeight(Utils.toDP(200, context));
        userAutoComplete.setWidth((int)(width * .75));
        userAutoComplete.setPromptPosition(ListPopupWindow.POSITION_PROMPT_BELOW);

        hashtagAutoComplete = new ListPopupWindow(context);
        hashtagAutoComplete.setHeight(Utils.toDP(200, context));
        hashtagAutoComplete.setWidth((int)(width * .75));
        hashtagAutoComplete.setPromptPosition(ListPopupWindow.POSITION_PROMPT_ABOVE);
    }

    private ListPopupWindow on(final EditText textView) {
        this.textView = textView;
        userAutoComplete.setAnchorView(textView);
        hashtagAutoComplete.setAnchorView(textView);

        hashtagAutoComplete.setAdapter(new AutoCompleteHashtagAdapter(context,
                HashtagDataSource.getInstance(context).getCursor(""), textView));

        textView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override public void afterTextChanged(Editable editable) {
                String searchText = textView.getText().toString();
                int position = textView.getSelectionStart() - 1;

                if (position < 0 || position > searchText.length() - 1) {
                    return;
                }

                try {
                    if (searchText.charAt(position) == '@') {
                        userAutoComplete.show();
                        hashtagAutoComplete.dismiss();
                    } else if (searchText.charAt(position) == ' ') {
                        userAutoComplete.dismiss();
                        hashtagAutoComplete.dismiss();
                    } else if (userAutoComplete.isShowing()) {
                        String adapterText = "";

                        int localPosition = position;
                        
                        do {
                            adapterText = searchText.charAt(localPosition--) + adapterText;
                        } while (localPosition >= 0 && searchText.charAt(localPosition) != '@');

                        adapterText = adapterText.replace("@", "");
                        search(adapterText);
                    }

                    if (searchText.charAt(position) == '#') {
                        hashtagAutoComplete.show();
                        userAutoComplete.dismiss();
                    } else if (searchText.charAt(position) == ' ') {
                        hashtagAutoComplete.dismiss();
                        userAutoComplete.dismiss();
                    } else if (hashtagAutoComplete.isShowing()) {
                        String adapterText = "";

                        int localPosition = position;
                        
                        do {
                            adapterText = searchText.charAt(localPosition--) + adapterText;
                        } while (localPosition >= 0 && searchText.charAt(localPosition) != '#');

                        adapterText = adapterText.replace("#", "");
                        hashtagAutoComplete.setAdapter(new AutoCompleteHashtagAdapter(context,
                                HashtagDataSource.getInstance(context).getCursor(adapterText), textView));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("text: " + searchText + ", position index: " + position);
//                    // there is no text
//                    try {
//                        userAutoComplete.dismiss();
//                    } catch (Exception x) {
//                        // something went really wrong I guess haha
//                    }
//
//                    try {
//                        hashtagAutoComplete.dismiss();
//                    } catch (Exception x) {
//                        // something went really wrong I guess haha
//                    }
                }
            }
        });

        if (!AppSettings.getInstance(context).followersOnlyAutoComplete) {
            userAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    userAutoComplete.dismiss();
                    autoCompleter.completeTweet(textView, users.get(i).getScreenName(), '@');

                    if (callback != null) {
                        callback.onUserSelected(users.get(i));
                    }
                }
            });
        }

        hashtagAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                hashtagAutoComplete.dismiss();
            }
        });

        return userAutoComplete;
    }

    public ListPopupWindow getAutoCompletePopup() {
        return userAutoComplete;
    }

    private void search(final String screenName) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AppSettings settings = AppSettings.getInstance(context);
                        if (settings.followersOnlyAutoComplete) {
                            if (adapter != null) {
                                try {
                                    adapter.getCursor().close();
                                } catch (Exception e) {

                                }
                            }

                            final Cursor cursor = FollowersDataSource.getInstance(context).getCursor(settings.currentAccount, screenName);
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter = new AutoCompletePeopleAdapter(context, cursor, textView);
                                    userAutoComplete.setAdapter(adapter);
                                }
                            });
                        } else {
                            Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));

                            try {
                                users = twitter.searchUsers("@" + screenName, 0);
                            } catch (Exception e) {
                            }

                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    userAutoComplete.setAdapter(new AutoCompleteUserArrayAdapter(context, users));
                                }
                            });
                        }
                    }
                }).start();
            }
        }, 150);

    }
}
