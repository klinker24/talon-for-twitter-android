package com.klinker.android.twitter_l.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@RequiresApi(Build.VERSION_CODES.Q)
object IoUtilKotlin {

    fun saveVideoAndroid11(context: Context, videoUrl: String): Uri {
        val relativeLocation = Environment.DIRECTORY_DOWNLOADS
        val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Video-" + Date().time + if (videoUrl.contains(".m3u8")) ".m3u8" else ".mp4")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)

        val uri = context.contentResolver?.insert(contentUri, contentValues)
        val outputStream = context.contentResolver?.openOutputStream(uri!!)

        val u = URL(videoUrl)
        val `is` = u.openStream()
        val huc = u.openConnection() as HttpURLConnection //to know the size of video
        val size = huc.contentLength

        val buffer = ByteArray(1024)
        var len1 = 0
        if (`is` != null) {
            while (`is`.read(buffer).also { len1 = it } > 0) {
                outputStream?.write(buffer, 0, len1)
            }
        }

        outputStream?.close()
        try {
            `is`?.close()
        } catch (ioe: IOException) {
            // just going to ignore this one
        }

        return uri!!
    }

    fun saveGiphyAndroid11(context: Context, videoUrl: String): Uri {
        val relativeLocation = Environment.DIRECTORY_DOWNLOADS
        val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Giphy-" + Date().time + ".gif")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)

        val uri = context.contentResolver?.insert(contentUri, contentValues)
        val outputStream = context.contentResolver?.openOutputStream(uri!!)

        val u = URL(videoUrl)
        val `is` = u.openStream()
        val huc = u.openConnection() as HttpURLConnection //to know the size of video
        val size = huc.contentLength

        val buffer = ByteArray(1024)
        var len1 = 0
        if (`is` != null) {
            while (`is`.read(buffer).also { len1 = it } > 0) {
                outputStream?.write(buffer, 0, len1)
            }
        }

        outputStream?.close()
        try {
            `is`?.close()
        } catch (ioe: IOException) {
            // just going to ignore this one
        }

        return uri!!
    }
}