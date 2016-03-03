/*
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */

package com.klinker.android.simple_videoview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.io.IOException;

/**
 * VideoView implementation that simplifies things, fixes aspect ratio, and allows 
 * you to specify whether or not you want to overtake the system audio.
 */
public class SimpleVideoView extends LinearLayout {

    private MediaPlayer mediaPlayer;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private View progressBar;

    private boolean loop = false;
    private boolean stopSystemAudio = false;
    private boolean muted = false;

    private Uri videoUri = null;

    /**
     * Default constructor
     * @param context context for the activity
     */
    public SimpleVideoView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor for XML layout
     * @param context activity context
     * @param attrs xml attributes
     */
    public SimpleVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SimpleVideoView, 0, 0);
        loop = a.getBoolean(R.styleable.SimpleVideoView_loop, false);
        stopSystemAudio = a.getBoolean(R.styleable.SimpleVideoView_stopSystemAudio, false);
        muted = a.getBoolean(R.styleable.SimpleVideoView_muted, false);
        a.recycle();

        init();
    }

    /**
     * Initialize the layout for the SimpleVideoView.
     */
    private void init() {
        // add a progress spinner
        progressBar = LayoutInflater.from(getContext()).inflate(R.layout.progress_bar, null, false);
        addView(progressBar);

        setGravity(Gravity.CENTER);
    }

    /**
     * Add the SurfaceView to the layout.
     */
    private void addSurfaceView() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        // initialize the media player
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                progressBar.setVisibility(View.GONE);

                scalePlayer();

                if (stopSystemAudio) {
                    AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                    am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                }

                if (muted) {
                    mediaPlayer.setVolume(0, 0);
                }

                mediaPlayer.setDisplay(surfaceHolder);
                mediaPlayer.setLooping(loop);
                mediaPlayer.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // Some devices (Samsung) don't respond to the MediaPlayer#setLooping value
                // for whatever reason. So this manually restarts it.
                if (loop) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                }
            }
        });

        LinearLayout.LayoutParams surfaceViewParams =
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        surfaceView = new SurfaceView(getContext());
        surfaceView.setLayoutParams(surfaceViewParams);

        addView(surfaceView);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) { }
            @Override public void surfaceDestroyed(SurfaceHolder surfaceHolder) { }
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    mediaPlayer.setDataSource(getContext(), videoUri);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Adjust the size of the player so it fits on the screen.
     */
    private void scalePlayer() {
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;

        float screenProportion = (float) getWidth() / (float) getHeight();
        ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();

        if (videoProportion > screenProportion) {
            lp.width = getWidth();
            lp.height = (int) ((float) getWidth() / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) getHeight());
            lp.height = getHeight();
        }


        surfaceView.setLayoutParams(lp);
    }

    /**
     * Load the video into the player and initialize the layouts
     *
     * @param videoUrl String url to the video
     */
    public void start(String videoUrl) {
        start(Uri.parse(videoUrl));
    }

    /**
     * Load the video into the player and initialize the layouts.
     *
     * @param videoUri uri to the video.
     */
    public void start(Uri videoUri) {
        this.videoUri = videoUri;

        // we will not load the surface view or anything else until we are given a video.
        // That way, if, say, you wanted to add the simple video view on a list or something,
        // it won't be as intensive. ( == Better performance.)
        addSurfaceView();
    }

    /**
     * Start video playback. Called automatically with the SimpleVideoPlayer#start method
     */
    public void play() {
        if (!mediaPlayer.isPlaying())
            mediaPlayer.start();
    }

    /**
     * Pause video playback
     */
    public void pause() {
        if (mediaPlayer.isPlaying())
            mediaPlayer.pause();
    }

    /**
     * Release the video to stop playback immediately.
     *
     * Should be called when you are leaving the playback activity
     */
    public void release() {
        removeAllViews();

        try {
            mediaPlayer.release();
        } catch (Exception e) { }
    }

    /**
     * Whether you want the video to loop or not
     *
     * @param shouldLoop
     */
    public void setShouldLoop(boolean shouldLoop) {
        this.loop = shouldLoop;
    }

    /**
     * Whether you want the app to stop the currently playing audio when you start the video
     *
     * @param stopSystemAudio
     */
    public void setStopSystemAudio(boolean stopSystemAudio) {
        this.stopSystemAudio = stopSystemAudio;
    }

    /**
     * Get whether or not the video is playing
     *
     * @return true if the video is playing, false otherwise
     */
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
}