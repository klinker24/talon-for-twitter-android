package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.media_viewer.image.ImageViewerActivity;
import com.klinker.android.twitter_l.views.badges.GifBadge;
import com.klinker.android.twitter_l.views.badges.VideoBadge;
import com.klinker.android.twitter_l.activities.media_viewer.VideoViewerActivity;
import com.klinker.android.twitter_l.activities.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;
import com.klinker.android.twitter_l.utils.VideoMatcherUtil;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;
import twitter4j.Status;

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
            holder.iv = (ImageView) convertView.findViewById(R.id.picture);
            holder.badge = (ImageView) convertView.findViewById(R.id.media_tag);
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

        try {
            Glide.with(context).load(url).into(holder.iv);
        } catch (Exception e) {

        }

        final long id = status != null ? status.getId() : 0;

        holder.iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setPics();
                List<Pair<String, Long>> linksWithIds = new ArrayList<>(text.size());
                String[] links = pics.split(" ");

                for (int i = 0; i < text.size(); i++) {
                    Status s = statuses.get(i);
                    linksWithIds.add(new Pair<>(links[i], (s == null) ? -1L : s.getId()));
                }

                ImageViewerActivity.Companion.startActivity(context, null, position, linksWithIds);
            }
        });


        if (status != null) {
            final String profilePic =status.getUser().getBiggerProfileImageURL();
            final String name = status.getUser().getName();
            final String screenname = status.getUser().getScreenName();

            String[] html = TweetLinkUtils.getLinksInStatus(status);
            final String tweetText = html[0];
            final String picUrl = html[1];
            final String otherUrl = html[2];
            final String hashtags = html[3];
            final String users = html[4];

            final TweetLinkUtils.TweetMediaInformation info = TweetLinkUtils.getGIFUrl(status, otherUrl);
            final String gifUrl = info.url;

            if (url.contains("youtube") || gifUrl != null && !gifUrl.isEmpty()) {
                if (VideoMatcherUtil.isTwitterGifLink(gifUrl)) {
                    holder.badge.setImageDrawable(new GifBadge(context));
                } else {
                    holder.badge.setImageDrawable(new VideoBadge(context, info.duration));
                }

                if (holder.badge.getVisibility() != View.VISIBLE) {
                    holder.badge.setVisibility(View.VISIBLE);
                }

                final long fStatusId = status.getId();
                holder.iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        VideoViewerActivity.startActivity(context, fStatusId, gifUrl, otherUrl);
                    }
                });
            } else if (holder.badge.getVisibility() != View.GONE) {
                holder.badge.setVisibility(View.GONE);
            }

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

                    TweetActivity.applyDragDismissBundle(context, viewTweet);

                    context.startActivity(viewTweet);

                    return false;
                }
            });

            return convertView;
        } else {
            holder.iv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });

            return convertView;
        }
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
        public ImageView iv;
        public ImageView badge;
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