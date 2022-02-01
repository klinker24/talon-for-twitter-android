package com.klinker.android.twitter_l.activities.tweet_viewer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.halilibo.bvpkotlin.BetterVideoPlayer;
import com.halilibo.bvpkotlin.VideoCallback;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.media_viewer.image.DragController;
import com.klinker.android.twitter_l.activities.media_viewer.image.OnSwipeListener;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.VideoMatcherUtil;
import com.potyvideo.library.AndExoPlayerView;
import com.potyvideo.library.globalEnums.EnumMute;

import java.util.HashMap;


public class VideoFragment extends Fragment {

    public static VideoFragment getInstance(String url) {
        Bundle args = new Bundle();
        args.putString("url", url);

        VideoFragment fragment = new VideoFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public Context context;
    public String tweetUrl;
    public String videoUrl;

    private View layout;

    public AndExoPlayerView videoView;

    private DragController dragController;
    private GestureDetectorCompat gestureDetector;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        tweetUrl = getArguments().getString("url");

        layout = inflater.inflate(R.layout.gif_player, null, false);
        videoView = (AndExoPlayerView) layout.findViewById(R.id.player);
        dragController = new DragController((AppCompatActivity) getActivity(), videoView);

        if (VideoMatcherUtil.isTwitterGifLink(tweetUrl)) {
            videoView.setShowControllers(false);
            videoView.setMute(EnumMute.MUTE);
        } else {
            videoView.setShowControllers(true);
        }

        getGif();

        gestureDetector = new GestureDetectorCompat(getActivity(), new OnSwipeListener() {
            @Override
            public boolean onSwipe(Direction direction) {
                return direction == Direction.UP || direction == Direction.DOWN;
            }
        });

        return layout;
    }

    private void getGif() {
        new TimeoutThread(() -> {
            videoUrl = tweetUrl;

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {
                if (videoUrl != null) {
                    videoView.setSource(videoUrl, new HashMap<>());
                    videoView.startPlayer();
                }
            });

        }).start();
    }

    public String getLoadedVideoLink() {
        return videoUrl;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Make sure the player stops playing if the user presses the home button.
        videoView.stopPlayer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        videoView.releasePlayer();
    }

    public boolean isGif() {
        return VideoMatcherUtil.isTwitterGifLink(videoUrl);
    }
}
