package com.klinker.android.twitter_l.views.popups;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.views.widgets.PopupLayout;


public class ConversationPopupLayout extends PopupLayout {

    ListView list;
    LinearLayout spinner;

    public ConversationPopupLayout(Context context, View main) {
        super(context);

        list = (ListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        //setTitle(getContext().getString(R.string.conversation));
        showTitle(false);
        setFullScreen();

        content.addView(main);
    }

    @Override
    public View setMainLayout() {
        return null;
    }
}
