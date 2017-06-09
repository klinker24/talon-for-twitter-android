package com.klinker.android.twitter_l.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.redirects.RedirectToMyAccount;
import com.klinker.android.twitter_l.utils.redirects.RedirectToSecondAccount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

            List<ShortcutInfo> shortcuts = new ArrayList<>();

            Intent firstAccount = new Intent(context, RedirectToMyAccount.class);
            firstAccount.setAction(Intent.ACTION_VIEW);
            shortcuts.add(new ShortcutInfo.Builder(context, settings.myScreenName)
                    .setIntent(firstAccount)
                    .setRank(0)
                    .setShortLabel(settings.myName)
                    .setIcon(getIcon(context, settings.myProfilePicUrl))
                    .build());

            if (settings.numberOfAccounts == 2) {
                Intent secondAccount = new Intent(context, RedirectToSecondAccount.class);
                secondAccount.setAction(Intent.ACTION_VIEW);
                shortcuts.add(new ShortcutInfo.Builder(context, settings.secondScreenName)
                        .setIntent(secondAccount)
                        .setRank(0)
                        .setShortLabel(settings.secondName)
                        .setIcon(getIcon(context, settings.secondProfilePicUrl))
                        .build());
            }

            manager.setDynamicShortcuts(shortcuts);
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
