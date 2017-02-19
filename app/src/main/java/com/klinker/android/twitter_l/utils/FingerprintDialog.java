package com.klinker.android.twitter_l.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.widget.Toast;

import com.github.ajalt.reprint.core.AuthenticationFailureReason;
import com.github.ajalt.reprint.core.AuthenticationListener;
import com.github.ajalt.reprint.core.Reprint;
import com.klinker.android.twitter_l.R;

@TargetApi(Build.VERSION_CODES.M)
public class FingerprintDialog implements AuthenticationListener {

    private Activity context;
    private AlertDialog dialog;

    public FingerprintDialog(Activity context) {
        this.context = context;
    }

    public void show() {
        dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.authenticate)
                .setView(R.layout.dialog_fingerprint)
                .setCancelable(false)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.finish();
                    }
                }).show();

        Reprint.authenticate(this);
    }

    @Override
    public void onSuccess(int moduleTag) {
        dialog.dismiss();
        Reprint.cancelAuthentication();
    }

    @Override
    public void onFailure(AuthenticationFailureReason failureReason, boolean fatal, CharSequence errorMessage, int moduleTag, int errorCode) {
        if (fatal) {
            dialog.dismiss();
            context.finish();
        } else {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
}
