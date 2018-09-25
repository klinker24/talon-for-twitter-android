package com.klinker.android.twitter_l.adapters;

import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;

/**
 * Edited from https://github.com/afollestad/sectioned-recyclerview
 *
 * @author afollestad
 */
public abstract class SectionedRecyclerViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    protected final static int VIEW_TYPE_HEADER = -2;
    protected final static int VIEW_TYPE_ITEM = -1;

    private final ArrayMap<Integer, Integer> mHeaderLocationMap;
    private GridLayoutManager mLayoutManager;

    public SectionedRecyclerViewAdapter() {
        mHeaderLocationMap = new ArrayMap<>();
    }

    public abstract int getSectionCount();

    public abstract int getItemCount(int section);

    public abstract void onBindHeaderViewHolder(VH holder, int section);

    public abstract void onBindViewHolder(VH holder, int section, int relativePosition, int absolutePosition);

    @VisibleForTesting
    protected boolean isHeader(int position) {
        return mHeaderLocationMap.get(position) != null;
    }

    public void setLayoutManager(GridLayoutManager lm) {
        mLayoutManager = lm;
        lm.setSpanSizeLookup(getSizeLookup());
    }

    @VisibleForTesting
    protected GridLayoutManager.SpanSizeLookup getSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (isHeader(position))
                    return mLayoutManager.getSpanCount();
                return 1;
            }
        };
    }

    @VisibleForTesting
    protected ItemPosition getSectionIndexAndRelativePosition(int itemPosition) {
        Integer lastSectionIndex = -1;
        for (final Integer sectionIndex : mHeaderLocationMap.keySet()) {
            if (itemPosition > sectionIndex) {
                lastSectionIndex = sectionIndex;
            } else {
                break;
            }
        }
        return new ItemPosition(mHeaderLocationMap.get(lastSectionIndex),
                itemPosition - lastSectionIndex - 1);
    }

    @Override
    public final int getItemCount() {
        int count = 0;
        mHeaderLocationMap.clear();
        for (int s = 0; s < getSectionCount(); s++) {
            mHeaderLocationMap.put(count, s);
            count += getItemCount(s) + 1;
        }
        return count;
    }

    @Override
    public final int getItemViewType(int position) {
        if (isHeader(position)) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        StaggeredGridLayoutManager.LayoutParams layoutParams =
                new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (isHeader(position)) {
            layoutParams.setFullSpan(true);
            onBindHeaderViewHolder(holder, mHeaderLocationMap.get(position));
        } else {
            layoutParams.setFullSpan(false);
            final ItemPosition itemPosition = getSectionIndexAndRelativePosition(position);
            onBindViewHolder(holder, itemPosition.sectionIndex,
                    itemPosition.relativeIndex,
                    position - (itemPosition.sectionIndex + 1));
        }

        setLayoutParameters(holder.itemView, layoutParams);
    }

    @VisibleForTesting
    protected void setLayoutParameters(View view, ViewGroup.LayoutParams params) {
        if (view != null) {
            view.setLayoutParams(params);
        }
    }

    protected static class ItemPosition {
        public int sectionIndex;
        public int relativeIndex;

        public ItemPosition(int section, int relative) {
            sectionIndex = section;
            relativeIndex = relative;
        }
    }
}