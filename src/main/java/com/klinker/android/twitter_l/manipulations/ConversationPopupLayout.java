package com.klinker.android.twitter_l.manipulations;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;
import org.lucasr.smoothie.AsyncListView;


public class ConversationPopupLayout extends PopupLayout {

    AsyncListView list;
    LinearLayout spinner;

    public ConversationPopupLayout(Context context, View main, boolean windowed) {
        super(context, windowed);

        list = (AsyncListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        setTitle(getContext().getString(R.string.conversation));
        setFullScreen();

        content.addView(main);
    }

    public ConversationPopupLayout(Context context, View main) {
        super(context);

        list = (AsyncListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        setTitle(getContext().getString(R.string.conversation));
        setFullScreen();

        content.addView(main);
    }

    @Override
    public View setMainLayout() {
        return null;
    }
}
