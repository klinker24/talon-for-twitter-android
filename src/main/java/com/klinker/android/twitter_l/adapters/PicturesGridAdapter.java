package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoPagerActivity;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.ui.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;

import java.util.ArrayList;

import twitter4j.Status;
import twitter4j.User;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class PicturesGridAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> text;
    private ArrayList<Status> statuses;
    private int gridWidth;
    protected String pics = "";

    public PicturesGridAdapter(Context context, ArrayList<String> text, ArrayList<Status> statuses, int gridWidth) {
        this.context = context;
        this.text = text;
        this.statuses = statuses;
        this.gridWidth = gridWidth;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.picture, null);

            AbsListView.LayoutParams params = new AbsListView.LayoutParams(gridWidth, gridWidth);
            convertView.setLayoutParams(params);

            ViewHolder holder = new ViewHolder();
            holder.iv = (NetworkedCacheableImageView) convertView.findViewById(R.id.picture);
            convertView.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) convertView.getTag();

        Status status = statuses.get(position);
        String url = text.get(position);

        final String retweeter;
        final long time = status != null ? status.getCreatedAt().getTime() : 0;
        final long originalTime;

        if (status != null && status.isRetweet()) {
            retweeter = status.getUser().getScreenName();

            status = status.getRetweetedStatus();
            originalTime = status.getCreatedAt().getTime();
        } else {
            retweeter = "";
            originalTime = 0;
        }

        if (url.contains(" ")) {
            url = url.split(" ")[0];
        }

        holder.url = url;

        holder.iv.loadImage(url, false, new NetworkedCacheableImageView.OnImageLoadedListener() {
            @Override
            public void onImageLoaded(CacheableBitmapDrawable result) {
                holder.iv.setBackgroundDrawable(null);
            }
        });

        final long id = status != null ? status.getId() : 0;

        holder.iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setPics();

                PhotoPagerActivity.startActivity(context, id, pics, position);
            }
        });

        if (status == null) {
            holder.iv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });
            
            return convertView;
        }

        final String profilePic = status != null ? status.getUser().getBiggerProfileImageURL() : "";
        final String name = status != null ? status.getUser().getName() : "";
        final String screenname = status != null ? status.getUser().getScreenName() : "";

        String[] html = TweetLinkUtils.getLinksInStatus(status);
        final String tweetText = html[0];
        final String picUrl = html[1];
        final String otherUrl = html[2];
        final String hashtags = html[3];
        final String users = html[4];

        holder.iv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String link;

                boolean displayPic = !picUrl.equals("");
                if (displayPic) {
                    link = picUrl;
                } else {
                    link = otherUrl.split("  ")[0];
                }

                Log.v("tweet_page", "clicked");
                Intent viewTweet = new Intent(context, TweetActivity.class);
                viewTweet.putExtra("name", name);
                viewTweet.putExtra("screenname", screenname);
                viewTweet.putExtra("time", time);
                viewTweet.putExtra("tweet", tweetText);
                viewTweet.putExtra("retweeter", retweeter);
                viewTweet.putExtra("webpage", link);
                viewTweet.putExtra("other_links", otherUrl);
                viewTweet.putExtra("picture", displayPic);
                viewTweet.putExtra("tweetid", id);
                viewTweet.putExtra("proPic", profilePic);
                viewTweet.putExtra("users", users);
                viewTweet.putExtra("hashtags", hashtags);
                viewTweet.putExtra("animated_gif", "");

                viewTweet = addDimensForExpansion(viewTweet, holder.iv);

                context.startActivity(viewTweet);

                return false;
            }
        });

        return convertView;
    }

    @Override
    public int getCount() {
        return text.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public static class ViewHolder {
        public NetworkedCacheableImageView iv;
        public String url;
    }

    public void setPics() {
        pics = "";

        for (int i = 0; i < text.size(); i++) {
            Status s = statuses.get(i);
            if (s == null) {
                pics += text.get(i) + " ";
            } else {
                String[] html = TweetLinkUtils.getLinksInStatus(s);
                String pic = html[1];

                if (pic.contains(" ")) {
                    pic = pic.split(" ")[0];
                }

                pics += pic + " ";
            }
        }
    }

    private Intent addDimensForExpansion(Intent i, View view) {
        i.putExtra(TweetActivity.USE_EXPANSION, true);

        int location[] = new int[2];
        view.getLocationOnScreen(location);

        i.putExtra(TweetActivity.EXPANSION_DIMEN_LEFT_OFFSET, location[0]);
        i.putExtra(TweetActivity.EXPANSION_DIMEN_TOP_OFFSET, location[1]);
        i.putExtra(TweetActivity.EXPANSION_DIMEN_HEIGHT, view.getHeight());
        i.putExtra(TweetActivity.EXPANSION_DIMEN_WIDTH, view.getWidth());

        return i;
    }
}