package com.klinker.android.twitter_l.ui.compose;

import android.content.Intent;
import android.util.Log;
import android.widget.ImageView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.utils.ImageUtils;

public class ComposeSecAccActivity extends ComposeActivity {

    public void setUpReplyText() {

        useAccOne = false;
        useAccTwo = true;

        ImageView pic = (ImageView) findViewById(R.id.profile_pic);
        HoloTextView currentName = (HoloTextView) findViewById(R.id.current_name);
        ImageUtils.loadImage(context, pic, settings.secondProfilePicUrl, App.getInstance(context).getBitmapCache());
        currentName.setText("@" + settings.secondScreenName);

        // for failed notification
        if (!sharedPrefs.getString("draft", "").equals("")) {
            reply.setText(sharedPrefs.getString("draft", ""));
            reply.setSelection(reply.getText().length());
        }

        String to = getIntent().getStringExtra("user") + (isDM ? "" : " ");

        if ((!to.equals("null ") && !isDM) || (isDM && !to.equals("null"))) {
            if(!isDM) {
                Log.v("username_for_noti", "to place: " + to);
                reply.setText(to);
                reply.setSelection(reply.getText().toString().length());
            } else {
                contactEntry.setText(to);
                reply.requestFocus();
            }

            sharedPrefs.edit().putString("draft", "").commit();
        }

        notiId = getIntent().getLongExtra("id", 0);
        replyText = getIntent().getStringExtra("reply_to_text");

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        }
    }
}