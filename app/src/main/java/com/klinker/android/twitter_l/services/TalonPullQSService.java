package com.klinker.android.twitter_l.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import com.klinker.android.twitter_l.settings.AppSettings;

public class TalonPullQSService extends TileService {

    BroadcastReceiver startPush = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getQsTile().setState(Tile.STATE_ACTIVE);
            getQsTile().updateTile();
        }
    };

    BroadcastReceiver stopPush = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getQsTile().setState(Tile.STATE_INACTIVE);
            getQsTile().updateTile();
        }
    };


    @Override
    public void onClick() {
        if (getQsTile().getState() == Tile.STATE_INACTIVE) {
            getQsTile().setState(Tile.STATE_ACTIVE);
            startPull();
        } else {
            getQsTile().setState(Tile.STATE_INACTIVE);
            stopPull();
        }

        getQsTile().updateTile();
    }

    /*@Override
    public void onStartListening() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.START_PUSH");
        registerReceiver(startPush, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.STOP_PUSH_SERVICE");
        registerReceiver(stopPush, filter);
    }

    @Override
    public void onStopListening() {
        try {
            unregisterReceiver(startPush);
        } catch (Exception e) { }
        try {
            unregisterReceiver(stopPush);
        } catch (Exception e) { }
    }*/

    @Override
    public void onTileAdded() {
        super.onTileAdded();

        if (getQsTile() != null) {
            if (AppSettings.getInstance(this).pushNotifications) {
                getQsTile().setState(Tile.STATE_ACTIVE);
            } else {
                getQsTile().setState(Tile.STATE_INACTIVE);
            }
        }

    }

    private void startPull() {
        if (!TalonPullNotificationService.isRunning) {
            SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(this);
            sharedPreferences.edit().putString("talon_pull", "2").commit();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("talon_pull", "2").commit();

            AppSettings.invalidate();

            TalonPullNotificationService.start(this);
        }
    }

    private void stopPull() {
        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(this);

        sharedPreferences.edit().putString("talon_pull", "0").commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("talon_pull", "0").commit();

        sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));

        AppSettings.invalidate();
    }
}
