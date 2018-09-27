package com.klinker.android.twitter_l.services;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.annotation.RequiresApi;

import com.klinker.android.twitter_l.activities.compose.LauncherCompose;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ComposeTileService extends TileService {

    @Override
    public void onClick() {
        Intent compose = new Intent(this, LauncherCompose.class);
        startActivityAndCollapse(compose);
    }

    @Override
    public void onStartListening() {
        getQsTile().setState(Tile.STATE_ACTIVE);
    }
}
