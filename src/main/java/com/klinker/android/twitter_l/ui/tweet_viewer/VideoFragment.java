package com.klinker.android.twitter_l.ui.tweet_viewer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.*;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.Utils;
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
import java.net.URLDecoder;
import java.net.URLEncoder;

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

    public VideoView video;

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
        video = (VideoView) layout.findViewById(R.id.gif);

        if (VideoMatcherUtil.isTwitterVideoLink(tweetUrl)) {
            MediaController mediaController = new MediaController(getActivity());
            mediaController.setAnchorView(layout.findViewById(R.id.frame_parent));

            video.setMediaController(mediaController);
            hasControls = true;
        }


        getGif();

        return layout;
    }

    private boolean hasControls = false;
    private void getGif() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (tweetUrl.contains("vine.co")) {
                    // have to get the html from the page and parse the video from there.

                    videoUrl = getVineLink();
                } else if (tweetUrl.contains("amp.twimg.com/v/")) {
                    videoUrl = getAmpTwimgLink();
                } else if (tweetUrl.contains("snpy.tv")) {
                    videoUrl = getSnpyTvLink();
                } else if (tweetUrl.contains("/photo/1") && tweetUrl.contains("twitter.com/")) {
                    // this is before it was added to the api.
                    // finds the video from the HTML on twitters website.

                    videoUrl = getGifLink();
                } else {
                    videoUrl = tweetUrl;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (videoUrl != null) {
                                final Uri videoUri = Uri.parse(videoUrl);

                                video.setVideoURI(videoUri);
                                video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    @Override
                                    public void onPrepared(MediaPlayer mp) {
                                        video.setBackgroundColor(getActivity().getResources().getColor(android.R.color.transparent));
                                        layout.findViewById(R.id.list_progress).setVisibility(View.GONE);
                                    }
                                });

                                video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        if (!hasControls) {
                                            mp.seekTo(0);
                                            mp.start();
                                        }
                                    }
                                });

                                video.start();
                            } else {
                                Toast.makeText(getActivity(), R.string.error_gif, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), R.string.error_gif, Toast.LENGTH_SHORT).show();

                            getActivity().finish();
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
                            return x.attr("video-src");
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
                Elements elements = doc.getElementsByAttributeValue("class", "snappy-video");

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

    class SwipeDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.v("talon_gesture", "fling detected");
            if ((velocityY > 3000 || velocityY < -3000) &&
                    (velocityX < 7000 && velocityX > -7000)) {
                getActivity().onBackPressed();
                Log.v("talon_gesture", "closing activity");
            }

            return super.onFling(event1, event2, velocityX, velocityY);
        }
    }
}
