package com.klinker.android.twitter_l.ui.setup.material_login;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.services.DirectMessageRefreshService;
import com.klinker.android.twitter_l.services.MentionsRefreshService;
import com.klinker.android.twitter_l.services.TimelineRefreshService;
import com.klinker.android.twitter_l.services.TrimDataService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.ui.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter_l.ui.main_fragments.other_fragments.DMFragment;
import com.klinker.android.twitter_l.ui.main_fragments.other_fragments.MentionsFragment;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.Date;

import twitter4j.Twitter;


public class MaterialLogin extends MaterialLVLActivity {

    // CHANGE THIS TO UPDATE THE KEY VERSION
    public static final int KEY_VERSION = 2;

    public interface Callback {
        void onDone();
    }

    private ViewPager pager;
    private ImageView nextButton;

    @Override
    public void init(Bundle bundle) {
        super.init(bundle);

        SharedPreferences sharedPrefs = AppSettings.getInstance(this).sharedPrefs;

        int currAccount = sharedPrefs.getInt("current_account", 1);
        sharedPrefs.edit().putInt("key_version_" + currAccount, KEY_VERSION).commit();

        addSlides();

        setVibrate(true);
        setVibrateIntensity(30);
        setOffScreenPageLimit(4);

        pager = (ViewPager) findViewById(R.id.view_pager);
        nextButton = (ImageView) findViewById(R.id.next);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    // hacky to disable paging
                    pager.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            return true;
                        }
                    });

                    nextButton.setVisibility(View.INVISIBLE);

                    loginFragment.start(new Callback() {
                        @Override
                        public void onDone() {
                            nextButton.performClick();
                        }
                    });
                } else if (position == 2) {

                    nextButton.setVisibility(View.INVISIBLE);

                    downloadFragment.start(new Callback() {
                        @Override
                        public void onDone() {
                            nextButton.performClick();
                        }
                    });
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onDonePressed() {
        followUsers();
        startTimeline();
    }

    private ImageFragment welcomeFragment;
    private LoginFragment loginFragment;
    private DownloadFragment downloadFragment;
    private ImageFragment finishedFragment;

    private void addSlides() {
        welcomeFragment = ImageFragment.newInstance(getString(R.string.first_welcome), getString(R.string.first_info), "https://pbs.twimg.com/profile_images/496279971094986753/9NVnIz-m.png", Color.parseColor("#5C6BC0"));
        loginFragment = LoginFragment.getInstance();
        downloadFragment = DownloadFragment.getInstance();
        finishedFragment = ImageFragment.newInstance(getString(R.string.third_welcome), getString(R.string.follow_me_description), "https://g.twimg.com/Twitter_logo_white.png", Color.parseColor("#4CAF50"));

        addSlide(welcomeFragment);
        addSlide(loginFragment);
        addSlide(downloadFragment);
        addSlide(finishedFragment);
    }

    @Override
    public void finish() {
        SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        sharedPrefs.edit().putBoolean("version_3_2", false).commit();

        super.finish();
    }
    @Override
    public void onBackPressed() {
        // we don't want them to back out of the activity
        if (pager.getCurrentItem() == 3) {
            // final page
            startTimeline();
        }
    }

    public void restartLogin() {
        new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.login_error))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent restart = new Intent(MaterialLogin.this, MaterialLogin.class);
                        finish();
                        AppSettings.invalidate();
                        startActivity(restart);
                    }
                })
                .create()
                .show();
    }

    private void startTimeline() {
        AppSettings settings = AppSettings.getInstance(this);
        Context context = this;

        if (settings.timelineRefresh != 0) { // user only wants manual
            TimelineRefreshService.scheduleRefresh(context);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long now = new Date().getTime();
            long alarm = now + settings.mentionsRefresh;

            PendingIntent pendingIntent2 = PendingIntent.getService(context, MentionsFragment.MENTIONS_REFRESH_ID, new Intent(context, MentionsRefreshService.class), 0);

            am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.mentionsRefresh, pendingIntent2);

            alarm = now + settings.dmRefresh;

            PendingIntent pendingIntent3 = PendingIntent.getService(context, DMFragment.DM_REFRESH_ID, new Intent(context, DirectMessageRefreshService.class), 0);
            am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.dmRefresh, pendingIntent3);
        }

        // set up the autotrim
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long now = new Date().getTime();
        long alarm = now + AlarmManager.INTERVAL_DAY;
        Log.v("alarm_date", "auto trim " + new Date(alarm).toString());
        PendingIntent pendingIntent = PendingIntent.getService(context, 161, new Intent(context, TrimDataService.class), 0);
        am.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);

        finish();

        Intent timeline = new Intent(context, MainActivity.class);
        timeline.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        timeline.putExtra("tutorial", true);
        AppSettings.invalidate();
        startActivity(timeline);
    }

    private void followUsers() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Twitter twitter = Utils.getTwitter(MaterialLogin.this, null);

                try { twitter.createFriendship("TalonAndroid"); } catch (Exception e) { }
                //try { twitter.createFriendship("lukeklinker"); } catch (Exception e) { }
                try { twitter.createFriendship("KlinkerApps"); } catch (Exception e) { }
            }
        }).start();
    }
}
