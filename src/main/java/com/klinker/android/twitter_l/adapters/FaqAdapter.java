package com.klinker.android.twitter_l.adapters;
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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.XmlFaqUtils;

import java.util.List;

public class FaqAdapter extends SectionedRecyclerViewAdapter<FaqAdapter.ViewHolder> {

    private Context context;

    List<XmlFaqUtils.FaqCategory> faq;

    public FaqAdapter(Context context, List<XmlFaqUtils.FaqCategory> faq) {
        this.context = context;
        this.faq = faq;
    }

    @Override
    public int getSectionCount() {
        return faq.size();
    }

    @Override
    public int getItemCount(int section) {
        return faq.get(section).getSize();
    }

    @Override
    public void onBindHeaderViewHolder(ViewHolder holder, int section) {
        holder.title.setText(faq.get(section).categoryTitle);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int section, final int relativePosition, final int absolutePosition) {
        holder.title.setText(faq.get(section).items.get(relativePosition).question);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.web.getVisibility() != View.VISIBLE) {
                    holder.web.setVisibility(View.VISIBLE);
                    holder.web.loadUrl(faq.get(section).items.get(relativePosition).url);
                } else {
                    holder.web.setVisibility(View.GONE);
                    holder.web.loadUrl("about:blank");
                }
            }
        });

        if (holder.web.getVisibility() != View.GONE) {
            holder.web.setVisibility(View.GONE);
            holder.web.loadUrl("about:blank");
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(viewType == VIEW_TYPE_HEADER ?
                        R.layout.faq_adapter_header :
                        R.layout.faq_adapter_item, parent, false);
        return new ViewHolder(v);
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public WebView web;
        public LinearLayout background;

        /**
         * Constructor accepting the inflated view.
         *
         * @param itemView inflated view
         */
        public ViewHolder(View itemView) {
            super(itemView);
            background = (LinearLayout) itemView.findViewById(R.id.faq_item);
            title = (TextView) itemView.findViewById(R.id.faq_title);
            web = (WebView) itemView.findViewById(R.id.faq_web);
        }
    }
}