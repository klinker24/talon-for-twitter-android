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

import com.halilibo.bettervideoplayer.BetterVideoCallback;
import com.halilibo.bettervideoplayer.BetterVideoPlayer;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.media_viewer.image.DragController;
import com.klinker.android.twitter_l.activities.media_viewer.image.OnSwipeListener;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.VideoMatcherUtil;


public class VideoFragment extends Fragment implements BetterVideoCallback {

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

    public BetterVideoPlayer videoView;

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
        videoView = (BetterVideoPlayer) layout.findViewById(R.id.player);
        dragController = new DragController((AppCompatActivity) getActivity(), videoView);

        if (VideoMatcherUtil.isTwitterGifLink(tweetUrl)) {
            videoView.disableControls();
            videoView.hideControls();
        }

        if (AppSettings.getInstance(getActivity()).themeColors.primaryColor == Color.BLACK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ((ProgressBar) videoView.findViewById(R.id.seeker)).setProgressTintList(ColorStateList.valueOf(Color.WHITE));
            }
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

    public void stopPlayback() {
        try {
            videoView.stop();
        } catch (Exception e) {

        }
    }

    private void getGif() {
        new TimeoutThread(() -> {

//            if (tweetUrl.contains("vine.co")) {
//                // have to get the html from the page and parse the surfaceView from there.
//
//                videoUrl = getVineLink();
//            } else if (tweetUrl.contains("amp.twimg.com/v/")) {
//                videoUrl = getAmpTwimgLink();
//            } else if (tweetUrl.contains("snpy.tv")) {
//                videoUrl = getSnpyTvLink();
//            } else if (tweetUrl.contains("/photo/1") && tweetUrl.contains("twitter.com/")) {
//                // this is before it was added to the api.
//                // finds the surfaceView from the HTML on twitters website.
//
//                videoUrl = getGifLink();
//            } else {
                videoUrl = tweetUrl;
//            }

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {
                if (videoUrl != null) {
                    videoView.setCallback(VideoFragment.this);
                    videoView.setSource(Uri.parse(videoUrl));
                }
            });

        }).start();
    }

//    private Document getDoc() {
//        try {
//            HttpClient httpclient = new DefaultHttpClient();
//            HttpGet httpget = new HttpGet((tweetUrl.contains("http") ? "" : "https://") + tweetUrl);
//            HttpResponse response = httpclient.execute(httpget);
//            HttpEntity entity = response.getEntity();
//            InputStream is = entity.getContent();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
//            StringBuilder sb = new StringBuilder();
//            String line = null;
//            while ((line = reader.readLine()) != null)
//                sb.append(line + "\n");
//
//            String docHtml = sb.toString();
//
//            is.close();
//
//            return Jsoup.parse(docHtml);
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    private String getGifLink() {
//        try {
//            Document doc = getDoc();
//
//            if(doc != null) {
//                Elements elements = doc.getElementsByAttributeValue("class", "animated-gif");
//
//                for (Element e : elements) {
//                    for (Element x : e.getAllElements()) {
//                        if (x.nodeName().contains("source")) {
//                            return x.attr("surfaceView-src");
//                        }
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } catch (OutOfMemoryError e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    private String getVineLink() {
//        try {
//            Document doc = getDoc();
//
//            if(doc != null) {
//                Elements elements = doc.getElementsByAttributeValue("property", "twitter:player:stream");
//
//                for (Element e : elements) {
//                    return e.attr("content");
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } catch (OutOfMemoryError e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    private String getSnpyTvLink() {
//        try {
//            String location = tweetUrl;
//            HttpURLConnection connection = (HttpURLConnection) new URL(location).openConnection();
//            connection.setInstanceFollowRedirects(false);
//            while (connection.getResponseCode() / 100 == 3) {
//                location = connection.getHeaderField("location");
//                connection = (HttpURLConnection) new URL(location).openConnection();
//            }
//
//            tweetUrl = location;
//
//            Log.v("talon_gif", "tweet_url: " + tweetUrl);
//
//            Document doc = getDoc();
//
//            if(doc != null) {
//                Elements elements = doc.getElementsByAttributeValue("class", "snappy-surfaceView");
//
//                for (Element e : elements) {
//                    return e.attr("src");
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } catch (OutOfMemoryError e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    private String getAmpTwimgLink() {
//        try {
//            Document doc = getDoc();
//
//            if(doc != null) {
//                Element element = doc.getElementById("iframe");
//                return element.attr("src");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } catch (OutOfMemoryError e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }

    public String getLoadedVideoLink() {
        return videoUrl;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Make sure the player stops playing if the user presses the home button.
        videoView.pause();
    }

    // Methods for the implemented EasyVideoCallback

    @Override
    public void onStarted(BetterVideoPlayer player) {

    }

    @Override
    public void onPaused(BetterVideoPlayer player) {

    }

    @Override
    public void onPreparing(BetterVideoPlayer player) {

    }

    @Override
    public void onPrepared(BetterVideoPlayer player) {
        if (VideoMatcherUtil.isTwitterGifLink(videoUrl)) {
            //videoView.set(false);
            videoView.setHideControlsOnPlay(true);
            videoView.disableControls();
            videoView.setVolume(0,0);
        } else {
            videoView.setHideControlsOnPlay(true);
            videoView.enableControls();
        }
    }

    @Override
    public void onBuffering(int percent) {

    }

    @Override
    public void onError(BetterVideoPlayer player, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onCompletion(BetterVideoPlayer player) {
        if (VideoMatcherUtil.isTwitterGifLink(videoUrl)) {
            videoView.seekTo(0);
            videoView.start();
        } else {
            videoView.showControls();
        }
    }

    @Override
    public void onToggleControls(BetterVideoPlayer betterVideoPlayer, boolean b) {
        
    }


    public boolean isGif() {
        return VideoMatcherUtil.isTwitterGifLink(videoUrl);
    }
}
