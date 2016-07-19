package com.klinker.android.twitter_l.settings;

import com.klinker.android.twitter_l.R;

/**
 * Created by luke on 6/10/16.
 */

public class PrefActivityAdvanced extends PrefActivity {

    @Override
    public PrefFragment getFragment() {
        getIntent().putExtra("title", getResources().getString(R.string.advanced_options));
        return new PrefFragmentAdvanced();
    }
}
