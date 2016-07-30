package com.klinker.android.twitter_l.views.popups;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.views.widgets.PopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Twitter;

public class FollowMePopup extends PopupLayout {

    Context context;
    public FollowMePopup(Context context) {
        super(context);
        this.context = context;

        setTitle(getResources().getString(R.string.follow_progress));
    }

    View root;
    ImageView talonIv;
    ImageView lukeIv;
    ImageView googleIv;

    @Override
    public View setMainLayout() {
        root = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.follow_me_popup, null, false);

        LinearLayout talon = (LinearLayout) root.findViewById(R.id.talon_area);
        talonIv = (ImageView) root.findViewById(R.id.talon_picture);

        LinearLayout luke = (LinearLayout) root.findViewById(R.id.luke_area);
        lukeIv = (ImageView) root.findViewById(R.id.luke_picture);

        LinearLayout google = (LinearLayout) root.findViewById(R.id.google_plus_area);
        googleIv = (ImageView) root.findViewById(R.id.google_picture);

        Glide.with(getContext())
                .load("https://pbs.twimg.com/profile_images/496279971094986753/9NVnIz-m.png").into(talonIv);
        Glide.with(getContext())
                .load("https://pbs.twimg.com/profile_images/553283282431770624/Xv_h0aeM.jpeg").into(lukeIv);
        Glide.with(getContext())
                .load("https://developers.google.com/+/images/branding/g+128.png").into(googleIv);

        talon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new FollowMe().execute("TalonAndroid");

                view.setAlpha(.4f);
                view.setEnabled(false);
            }
        });

        luke.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new FollowMe().execute("lukeklinker");

                view.setAlpha(.4f);
                view.setEnabled(false);
            }
        });

        google.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://google.com/+LukeKlinker")));

                view.setAlpha(.4f);
                view.setEnabled(false);
            }
        });

        return root;
    }

    class FollowMe extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... urls) {
            Twitter twit = Utils.getTwitter(getContext(), AppSettings.getInstance(getContext()));

            try {
                twit.createFriendship(urls[0]);
            } catch (Exception x) {
                x.printStackTrace();
            }

            return null;
        }
    }

    @Override
    public void show() {
        super.show();

        Glide.with(getContext())
                .load("https://pbs.twimg.com/profile_images/496279971094986753/9NVnIz-m.png").into(talonIv);
        Glide.with(getContext())
                .load("https://pbs.twimg.com/profile_images/553283282431770624/Xv_h0aeM.jpeg").into(lukeIv);
        Glide.with(getContext())
                .load("https://developers.google.com/+/images/branding/g+128.png").into(googleIv);
    }
}
