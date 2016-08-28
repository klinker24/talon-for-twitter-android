package com.klinker.android.twitter_l.utils;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.sq_lite.*;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

public class IOUtils {

    public static Uri saveImage(Bitmap finalBitmap, String d, Context context) {
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

        return Uri.fromFile(file);
    }

    public static final Uri saveVideo(String videoUrl) throws Exception {

        File myDir = new File(Environment.getExternalStorageDirectory() + "/Talon");
        myDir.mkdirs();

        final File file = new File(Environment.getExternalStorageDirectory(), "Talon/Video-" + (new Date()).getTime() + (videoUrl.contains(".m3u8") ? ".m3u8" : ".mp4"));
        if (!file.createNewFile()) {
            throw new RuntimeException("Cannot download surfaceView - error creating file");
        }

        URL u = null;
        InputStream is = null;

        u = new URL(videoUrl);
        is = u.openStream();
        HttpURLConnection huc = (HttpURLConnection)u.openConnection(); //to know the size of video
        int size = huc.getContentLength();

        if(huc != null) {

            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len1 = 0;
            if(is != null) {
                while ((len1 = is.read(buffer)) > 0) {
                    fos.write(buffer,0, len1);
                }
            }
            if(fos != null) {
                fos.close();
            }
        }
        try {
            if(is != null) {
                is.close();
            }
        } catch (IOException ioe) {
            // just going to ignore this one
        }

        return Uri.fromFile(file);
    }

    public static final Uri saveGiffy(Context context, String videoUrl) throws Exception {

        final File directory = new File(Environment.getExternalStorageDirectory() + "/Talon/");
        if (!directory.exists()) {
            directory.mkdir();
        }

        final File file = new File(Environment.getExternalStorageDirectory() + "/Talon/", "giphy.gif");
        if (!file.createNewFile()) {
            // file already exists
        }

        URL url = new URL(videoUrl);
        URLConnection connection = url.openConnection();
        connection.setReadTimeout(5000);
        connection.setConnectTimeout(30000);

        InputStream is = connection.getInputStream();
        BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
        FileOutputStream outStream = new FileOutputStream(file);

        byte[] buffer = new byte[1024 * 5];
        int len;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }

        outStream.flush();
        outStream.close();
        inStream.close();

        return Uri.fromFile(file);
    }

    public static String getPath(Uri uri, Context context) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        String filePath = null;

        try {
            Cursor cursor = context.getContentResolver().query(
                    uri, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
            filePath = uri.getPath();
        }

        if (filePath == null) {
            filePath = uri.getPath();
            Log.v("talon_file_path", filePath);
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
            SharedPreferences.Editor prefEdit = AppSettings.getSharedPreferences(context)
                    .edit();

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
                } else if (v instanceof Set) {
                    prefEdit.putStringSet(key, ((Set<String>) v));
                }
            }

            prefEdit.apply();

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
            SharedPreferences pref = AppSettings.getSharedPreferences(context);


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
            AppSettings settings = AppSettings.getInstance(context);
            SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

            InteractionsDataSource interactions = InteractionsDataSource.getInstance(context);
            interactions.trimDatabase(account, 100);

            HomeDataSource home = HomeDataSource.getInstance(context);
            home.deleteDups(settings.currentAccount);
            home.trimDatabase(account, settings.timelineSize);

            FavoriteUserNotificationDataSource favoriteUserNotificationDataSource =
                    FavoriteUserNotificationDataSource.getInstance(context);
            favoriteUserNotificationDataSource.trimDatabase(100);

            // trimming the lists
            ListDataSource lists = ListDataSource.getInstance(context);
            for (int accountIndex = 0; accountIndex < 2; accountIndex++) {
                for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
                    long listId = sharedPrefs.getLong("account_" + accountIndex + "_list_" + (i + 1) + "_long", 0);
                    lists.deleteDups(listId);
                    lists.trimDatabase(listId, settings.listSize);
                }
            }

            MentionsDataSource mentions = MentionsDataSource.getInstance(context);
            mentions.deleteDups(account);
            mentions.trimDatabase(account, settings.mentionsSize);

            DMDataSource dm = DMDataSource.getInstance(context);
            dm.deleteDups(account);
            dm.trimDatabase(account, settings.dmSize);

            HashtagDataSource hashtag = HashtagDataSource.getInstance(context);
            hashtag.trimDatabase(500);

            ActivityDataSource activity = ActivityDataSource.getInstance(context);
            activity.trimDatabase(account, 200);

            FavoriteTweetsDataSource favtweets = FavoriteTweetsDataSource.getInstance(context);
            favtweets.deleteDups(settings.currentAccount);
            favtweets.trimDatabase(account, 400);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
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
            if (fileList != null) {
                for (int i = 0; i < fileList.length; i++) {

                    if (fileList[i].isDirectory())
                        dirlist.push(fileList[i]);
                    else
                        result += fileList[i].length();
                }
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

    public byte[] convertToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}
