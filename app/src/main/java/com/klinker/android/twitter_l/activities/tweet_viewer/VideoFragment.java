package com.klinker.android.twitter_l.activities.tweet_viewer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.easyvideoplayer.EasyVideoCallback;
import com.afollestad.easyvideoplayer.EasyVideoPlayer;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.media_viewer.VideoViewerActivity;
import com.klinker.android.twitter_l.utils.TimeoutThread;
import com.klinker.android.twitter_l.utils.VideoMatcherUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoFragment extends Fragment implements EasyVideoCallback {

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

    public EasyVideoPlayer videoView;
    private GestureDetector gestureDetector;

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
        videoView = (EasyVideoPlayer) layout.findViewById(R.id.player);

        if (VideoMatcherUtil.isTwitterGifLink(tweetUrl)) {
            videoView.disableControls();
            videoView.hideControls();
        }

        getGif();

        gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if ((velocityY > 3000 || velocityY < -3000) &&
                        (velocityX < 7000 && velocityX > -7000)) {
                    getActivity().onBackPressed();
                    return true;
                } else {
                    return false;
                }
            }
        });

        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        return layout;
    }


    private void getGif() {
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {

                if (tweetUrl.contains("vine.co")) {
                    // have to get the html from the page and parse the surfaceView from there.

                    videoUrl = getVineLink();
                } else if (tweetUrl.contains("amp.twimg.com/v/")) {
                    videoUrl = getAmpTwimgLink();
                } else if (tweetUrl.contains("snpy.tv")) {
                    videoUrl = getSnpyTvLink();
                } else if (tweetUrl.contains("/photo/1") && tweetUrl.contains("twitter.com/")) {
                    // this is before it was added to the api.
                    // finds the surfaceView from the HTML on twitters website.

                    videoUrl = getGifLink();
                } else {
                    videoUrl = tweetUrl;
                }

                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (videoUrl != null) {
                            videoView.setCallback(VideoFragment.this);
                            videoView.setSource(Uri.parse(videoUrl));
                        }
                    }
                });

            }
        }).start();
    }

    private Document getDoc() {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet((tweetUrl.contains("http") ? "" : "https://") + tweetUrl);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null)
                sb.append(line + "\n");

            String docHtml = sb.toString();

            is.close();

            return Jsoup.parse(docHtml);
        } catch (Exception e) {
            return null;
        }
    }

    private String getGifLink() {
        try {
            Document doc = getDoc();

            if(doc != null) {
                Elements elements = doc.getElementsByAttributeValue("class", "animated-gif");

                for (Element e : elements) {
                    for (Element x : e.getAllElements()) {
                        if (x.nodeName().contains("source")) {
                            return x.attr("surfaceView-src");
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getVineLink() {
        try {
            Document doc = getDoc();

            if(doc != null) {
                Elements elements = doc.getElementsByAttributeValue("property", "twitter:player:stream");

                for (Element e : elements) {
                    return e.attr("content");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getSnpyTvLink() {
        try {
            String location = tweetUrl;
            HttpURLConnection connection = (HttpURLConnection) new URL(location).openConnection();
            connection.setInstanceFollowRedirects(false);
            while (connection.getResponseCode() / 100 == 3) {
                location = connection.getHeaderField("location");
                connection = (HttpURLConnection) new URL(location).openConnection();
            }

            tweetUrl = location;

            Log.v("talon_gif", "tweet_url: " + tweetUrl);

            Document doc = getDoc();

            if(doc != null) {
                Elements elements = doc.getElementsByAttributeValue("class", "snappy-surfaceView");

                for (Element e : elements) {
                    return e.attr("src");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getAmpTwimgLink() {
        try {
            Document doc = getDoc();

            if(doc != null) {
                Element element = doc.getElementById("iframe");
                return element.attr("src");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

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
    public void onStarted(EasyVideoPlayer player) {

    }

    @Override
    public void onPaused(EasyVideoPlayer player) {

    }

    @Override
    public void onPreparing(EasyVideoPlayer player) {

    }

    @Override
    public void onPrepared(EasyVideoPlayer player) {
        if (VideoMatcherUtil.isTwitterGifLink(videoUrl)) {
            //videoView.set(false);
            videoView.setHideControlsOnPlay(true);
            videoView.disableControls();
            videoView.setVolume(0,0);
        } else {
            videoView.setHideControlsOnPlay(true);
            videoView.enableControls(true);

            ((VideoViewerActivity) getActivity()).hideSystemUI();
        }
    }

    @Override
    public void onBuffering(int percent) {

    }

    @Override
    public void onError(EasyVideoPlayer player, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onCompletion(EasyVideoPlayer player) {
        if (VideoMatcherUtil.isTwitterGifLink(videoUrl)) {
            videoView.seekTo(0);
            videoView.start();
        } else {
            videoView.showControls();
        }
    }

    @Override
    public void onRetry(EasyVideoPlayer player, Uri source) {

    }

    @Override
    public void onSubmit(EasyVideoPlayer player, Uri source) {

    }

    public boolean isGif() {
        return VideoMatcherUtil.isTwitterGifLink(videoUrl);
    }
}
