package com.klinker.android.twitter.ui.compose;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class WidgetCompose extends ComposeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reply.setText("");
    }

    @Override
    public void setUpLayout() {
        super.setUpLayout();

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(0,0);
                startActivity(new Intent(context, ComposeActivity.class)
                        .putExtra("start_attach", true)
                        .putExtra("user", reply.getText().toString())
                        .putExtra("reply_to_text", replyText));
            }
        });
    }
}
