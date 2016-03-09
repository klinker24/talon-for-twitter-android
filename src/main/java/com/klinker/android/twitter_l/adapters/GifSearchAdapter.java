package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bumptech.glide.Glide;
import com.klinker.android.simple_videoview.SimpleVideoView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.utils.api_helper.GiphyHelper;

import java.util.List;

public class GifSearchAdapter extends SectionedRecyclerViewAdapter<GifSearchAdapter.ViewHolder> {

    public interface Callback {
        void onClick(GiphyHelper.Gif item);
    }

    protected List<GiphyHelper.Gif> gifs;
    protected Callback callback;
    protected SimpleVideoView currentlyPlaying;

    public GifSearchAdapter(List<GiphyHelper.Gif> gifs, Callback callback) {
        this.gifs = gifs;
        this.callback = callback;
    }

    public void releaseVideo() {
        if (currentlyPlaying != null) {
            currentlyPlaying.release();
            currentlyPlaying.setVisibility(View.GONE);
        }
    }

    @Override
    public int getSectionCount() {
        return 1;
    }

    @Override
    public int getItemCount(int section) {
        return gifs.size();
    }

    @Override
    public void onBindHeaderViewHolder(ViewHolder holder, int section) {

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int section, final int relativePosition, final int absolutePosition) {

        Context context = holder.previewImage.getContext();

        Glide.with(context).load(gifs.get(relativePosition).previewImage).into(holder.previewImage);

        holder.play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.video != currentlyPlaying) {
                    releaseVideo();

                    holder.video.setVisibility(View.VISIBLE);
                    holder.video.start(gifs.get(relativePosition).mp4Url);
                    currentlyPlaying = holder.video;
                }
            }
        });

        holder.select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                releaseVideo();
                callback.onClick(gifs.get(relativePosition));
            }
        });
    }

    /**
     * Create the view holder object for the item
     *
     * @param parent   the recycler view parent
     * @param viewType VIEW_TYPE_HEADER or VIEW_TYPE_ITEM
     * @return ViewHolder to be used
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType == VIEW_TYPE_HEADER ?
                        R.layout.adapter_item_gif_header :
                        R.layout.adapter_item_gif, parent, false);
        return new ViewHolder(v);
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView previewImage;
        public SimpleVideoView video;
        public TextView play;
        public TextView select;

        public ViewHolder(View itemView) {
            super(itemView);
            previewImage = (ImageView) itemView.findViewById(R.id.image);
            video = (SimpleVideoView) itemView.findViewById(R.id.video);
            play = (TextView) itemView.findViewById(R.id.play_button);
            select = (TextView) itemView.findViewById(R.id.select_button);
        }
    }
}