package com.klinker.android.twitter.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter.data.sq_lite.InteractionsSQLiteHelper;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.settings.AppSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

public class IOUtils {

    public static void saveImage(Bitmap finalBitmap, String d, Context context) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/Talon");
        myDir.mkdirs();
        String fname = d + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        //context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Toast.makeText(context, context.getResources().getString(R.string.save_image), Toast.LENGTH_SHORT).show();
    }

    public static String getPath(Uri uri, Context context) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        String filePath;

        try {
            Cursor cursor = context.getContentResolver().query(
                    uri, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        } catch (Exception e) {
            filePath = uri.getPath();
        }

        return filePath;
    }

    public static boolean loadSharedPreferencesFromFile(File src, Context context) {
        boolean res = false;
        ObjectInputStream input = null;

        try {
            if (!src.getParentFile().exists()) {
                src.getParentFile().mkdirs();
                src.createNewFile();
            }

            input = new ObjectInputStream(new FileInputStream(src));
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            prefEdit.clear();

            @SuppressWarnings("unchecked")
            Map<String, ?> entries = (Map<String, ?>) input.readObject();

            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) {
                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                } else if (v instanceof Float) {
                    prefEdit.putFloat(key, ((Float) v).floatValue());
                } else if (v instanceof Integer) {
                    prefEdit.putInt(key, ((Integer) v).intValue());
                } else if (v instanceof Long) {
                    prefEdit.putLong(key, ((Long) v).longValue());
                } else if (v instanceof String) {
                    prefEdit.putString(key, ((String) v));
                }
            }

            prefEdit.commit();

            res = true;
        } catch (Exception e) {

        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Exception e) {

            }
        }

        return res;
    }

    public static boolean saveSharedPreferencesToFile(File dst, Context context) {
        boolean res = false;
        ObjectOutputStream output = null;

        try {
            if (!dst.getParentFile().exists()) {
                dst.getParentFile().mkdirs();
                dst.createNewFile();
            }

            output = new ObjectOutputStream(new FileOutputStream(dst));
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

            output.writeObject(pref.getAll());

            res = true;
        } catch (Exception e) {

        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (Exception e) {

            }
        }

        return res;
    }

    public static String readChangelog(Context context) {
        String ret = "";
        try {
            AssetManager assetManager = context.getAssets();
            Scanner in = new Scanner(assetManager.open("changelog.txt"));

            while (in.hasNextLine()) {
                ret += in.nextLine() + "\n";
            }
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }

        return ret;
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {

        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    public static boolean trimDatabase(Context context, int account) {
        try {
            AppSettings settings = new AppSettings(context);
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

            InteractionsDataSource interactions = new InteractionsDataSource(context);
            interactions.open();
            Cursor inters = interactions.getCursor(account);

            if (inters.getCount() > 50) {
                if (inters.moveToPosition(inters.getCount() - 50)) {
                    do {
                        interactions.deleteInteraction(inters.getLong(inters.getColumnIndex(InteractionsSQLiteHelper.COLUMN_ID)));
                    } while (inters.moveToPrevious());
                }
            }

            interactions.close();
            inters.close();

            HomeDataSource home = new HomeDataSource(context);
            home.open();
            Cursor timeline = home.getCursor(account);

            Log.v("trimming", "timeline size: " + timeline.getCount());
            Log.v("trimming", "timeline settings size: " + settings.timelineSize);
            if (timeline.getCount() > settings.timelineSize) {

                if(timeline.moveToPosition(timeline.getCount() - settings.timelineSize)) {
                    Log.v("trimming", "in the trim section");
                    do {
                        home.deleteTweet(timeline.getLong(timeline.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)));
                    } while (timeline.moveToPrevious());
                }
            }

            home.close();

            MentionsDataSource mentions = new MentionsDataSource(context);
            mentions.open();
            timeline = mentions.getCursor(account);

            Log.v("trimming", "mentions size: " + timeline.getCount());
            Log.v("trimming", "mentions settings size: " + settings.mentionsSize);
            if (timeline.getCount() > settings.mentionsSize) {

                if(timeline.moveToPosition(timeline.getCount() - settings.mentionsSize)) {
                    do {
                        mentions.deleteTweet(timeline.getLong(timeline.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)));
                    } while (timeline.moveToPrevious());
                }
            }

            mentions.close();

            DMDataSource dm = new DMDataSource(context);
            dm.open();
            timeline = dm.getCursor(account);

            Log.v("trimming", "dm size: " + timeline.getCount());
            Log.v("trimming", "dm settings size: " + settings.dmSize);

            if (timeline.getCount() > settings.dmSize) {

                if(timeline.moveToPosition(timeline.getCount() - settings.dmSize)) {
                    do {
                        dm.deleteTweet(timeline.getLong(timeline.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)));
                    } while (timeline.moveToPrevious());
                }
            }

            timeline.close();
            dm.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static long dirSize(File dir) {
        long result = 0;

        Stack<File> dirlist= new Stack<File>();
        dirlist.clear();

        dirlist.push(dir);

        while(!dirlist.isEmpty())
        {
            File dirCurrent = dirlist.pop();

            File[] fileList = dirCurrent.listFiles();
            for (int i = 0; i < fileList.length; i++) {

                if(fileList[i].isDirectory())
                    dirlist.push(fileList[i]);
                else
                    result += fileList[i].length();
            }
        }

        return result;
    }

    public static String readAsset(Context context, String assetTitle) {
        String ret = "";
        try {
            AssetManager assetManager = context.getAssets();
            Scanner in = new Scanner(assetManager.open(assetTitle));

            while (in.hasNextLine()) {
                ret += in.nextLine() + "\n";
            }
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }

        return ret;
    }
}
