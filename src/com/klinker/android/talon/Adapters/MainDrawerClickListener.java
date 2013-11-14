package com.klinker.android.talon.Adapters;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import com.klinker.android.talon.R;
import com.klinker.android.talon.UI.DMTimeline;
import com.klinker.android.talon.UI.HomeTimeline;
import com.klinker.android.talon.UI.MentionsTimeline;

import java.util.logging.Handler;

/**
 * Created with IntelliJ IDEA.
 * User: luke
 * Date: 11/13/13
 * Time: 6:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class MainDrawerClickListener implements AdapterView.OnItemClickListener {

    private Context context;
    private DrawerLayout drawer;

    public MainDrawerClickListener(Context context, DrawerLayout drawer) {
        this.context = context;
        this.drawer = drawer;
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        drawer.closeDrawer(Gravity.START);

        final int pos = i;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = null;

                if (pos == 0) {
                    intent = new Intent(context, HomeTimeline.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                } else if (pos == 1) {
                    intent = new Intent(context, MentionsTimeline.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                } else if (pos == 2) {
                    intent = new Intent(context, DMTimeline.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                }

                try {
                    Thread.sleep(400);
                } catch (Exception e) {

                }
                try {
                    context.startActivity(intent);
                } catch (Exception e) {

                }

            }
        }).start();

    }
}
