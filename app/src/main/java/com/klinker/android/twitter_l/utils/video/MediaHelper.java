package com.klinker.android.twitter_l.utils.video;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

public class MediaHelper {

    public static int GetDuration( Uri uri ) {
        return GetMediaMetadataRetrieverPropertyInteger( uri, MediaMetadataRetriever.METADATA_KEY_DURATION, 0 );
    }

    public static int GetMediaMetadataRetrieverPropertyInteger( Uri uri, int key, int defaultValue ) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource( uri.toString() );
        String value = retriever.extractMetadata( key );

        if ( value == null ) {
            return defaultValue;
        }
        return Integer.parseInt( value );

    }

}