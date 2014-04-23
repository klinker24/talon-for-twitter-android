package com.klinker.android.twitter.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.ui.MainActivity;

import java.io.File;

/**
 * Created by luke on 4/22/14.
 */
public class UpdateUtils {

    public static void updateToGlobalPrefs(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Settings Update")
                .setMessage("Talon has to update your settings preferences to prepare for some new things. This will override any old settings backups.")
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new WriteGlobalSharedPrefs(context).execute();
                    }
                })
                .create()
                .show();
    }

    static class WriteGlobalSharedPrefs extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        Context context;

        public WriteGlobalSharedPrefs(Context context) {
            this.context = context;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Saving...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {
            File des = new File(Environment.getExternalStorageDirectory() + "/Talon/backup.prefs");
            IOUtils.saveSharedPreferencesToFile(des, context);
            IOUtils.loadSharedPreferencesFromFile(des, context);

            return true;
        }

        protected void onPostExecute(Boolean deleted) {
            try {
                pDialog.dismiss();
                Toast.makeText(context, "Save Complete", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // not attached
            }

            SharedPreferences sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
            sharedPrefs.edit().putBoolean("version_2_2_7_1", false).commit();

            ((Activity)context).finish();
            context.startActivity(new Intent(context, MainActivity.class));
            ((Activity)context).overridePendingTransition(0,0);
        }
    }
}
