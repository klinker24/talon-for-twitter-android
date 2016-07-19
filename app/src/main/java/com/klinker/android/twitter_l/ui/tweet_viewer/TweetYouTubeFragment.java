package com.klinker.android.twitter_l.ui.tweet_viewer;
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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.youtube.player.*;
import com.klinker.android.twitter_l.APIKeys;
import com.klinker.android.twitter_l.R;


public class TweetYouTubeFragment {

    public static YouTubePlayerFragment getInstance(final Context context, final String vidUrl) {
        YouTubePlayerFragment fragment = YouTubePlayerFragment.newInstance();
        fragment.initialize(APIKeys.YOUTUBE_API_KEY, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                Log.v("talon_video", "initializing player");
                String url = vidUrl;
                String video;

                try {
                    if (url.contains("youtube")) { // normal youtube link
                        // first get the youtube surfaceView code
                        int start = url.indexOf("v=") + 2;
                        int end;
                        if (url.substring(start).contains("&")) {
                            end = url.indexOf("&");
                            video = url.substring(start, end);
                        } else if (url.substring(start).contains("?")) {
                            end = url.indexOf("?");
                            video = url.substring(start, end);
                        } else {
                            video = url.substring(start);
                        }
                    } else { // shortened youtube link
                        // first get the youtube surfaceView code
                        int start = url.indexOf(".be/") + 4;
                        int end;
                        if (url.substring(start).contains("&")) {
                            end = url.indexOf("&");
                            video = url.substring(start, end);
                        } else if (url.substring(start).contains("?")) {
                            end = url.indexOf("?");
                            video = url.substring(start, end);
                        } else {
                            video = url.substring(start);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    video = "";
                }

                youTubePlayer.loadVideo(video, 0);
                youTubePlayer.setShowFullscreenButton(false);
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                Toast.makeText(context, R.string.error_gif, Toast.LENGTH_SHORT).show();
                ((Activity)context).finish();
            }
        });

        return fragment;
    }
}
