package com.klinker.android.twitter_l.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.redirects.RedirectToMyAccount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class DynamicShortcutUtils {

    private Context context;
    private ShortcutManager manager;

    @SuppressWarnings("WrongConstant")
    public DynamicShortcutUtils(Context context) {
        this.context = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            manager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        }
    }

    public void buildProfileShortcut() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && manager != null) {
            final AppSettings settings = AppSettings.getInstance(context);

            Intent messenger = new Intent(context, RedirectToMyAccount.class);
            messenger.setAction(Intent.ACTION_VIEW);

            ShortcutInfo info = new ShortcutInfo.Builder(context, settings.myScreenName)
                    .setIntent(messenger)
                    .setRank(0)
                    .setShortLabel(settings.myName)
                    .setIcon(getIcon(context, settings.myProfilePicUrl))
                    .build();


            manager.setDynamicShortcuts(Arrays.asList(info));
        }
    }

    private Icon getIcon(Context context, String url) throws InterruptedException, ExecutionException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Bitmap image = Glide.with(context).load(url).asBitmap()
                    .into(-1,-1).get();

            if (image != null) {
                return createIcon(image);
            } else {
                Bitmap color = Bitmap.createBitmap(Utils.toDP(48, context), Utils.toDP(48, context), Bitmap.Config.ARGB_8888);
                color.eraseColor(AppSettings.getInstance(context).themeColors.primaryColor);
                color = ImageUtils.getCircleBitmap(color);

                return createIcon(color);
            }
        } else {
            return null;
        }
    }

    private Icon createIcon(Bitmap bitmap) {
        if (Utils.isAndroidO()) {
            return Icon.createWithAdaptiveBitmap(bitmap);
        } else {
            return Icon.createWithBitmap(bitmap);
        }
    }
}
