package com.klinker.android.twitter_l.utils;
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
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.Uri;

import com.klinker.android.twitter_l.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XmlFaqUtils {

    public static void showFaqDialog(final Context context) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://klinkerapps.com/talon-overview/help"));
        context.startActivity(i);

//        final RecyclerView list = new RecyclerView(context);
//
//        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        int height = size.y;
//
//        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height - 200);
//        list.setLayoutParams(params);
//
//        new AsyncTask<Void, Void, List<FaqCategory>>() {
//            @Override
//            public List<FaqCategory> doInBackground(Void... params) {
//                return XmlFaqUtils.parse(context);
//            }
//
//            @Override
//            public void onPostExecute(List<FaqCategory> results) {
//                list.setLayoutManager(new LinearLayoutManager(context));
//                list.setAdapter(new FaqAdapter(context, results));
//            }
//        }.execute();
//
//        new AlertDialog.Builder(context)
//                .setView(list)
//                .setPositiveButton(R.string.ok, null)
//                .show();
    }
}
