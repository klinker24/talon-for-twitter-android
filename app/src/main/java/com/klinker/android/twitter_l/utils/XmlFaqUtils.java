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

    private static final String TAG = "XmlFaqUtils";
    private static final String ns = null;

    private static List items;

    public static final class FaqCategory {
        public String categoryTitle;
        public List<FaqQuestion> items;

        public int getSize() {
            return items.size();
        }
    }

    public static final class FaqQuestion {
        public String question;
        public String url;
    }

    public static List<FaqCategory> parse(Context context) {
        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.faq);
            parser.next();
            parser.nextTag();
            return readFaq(parser);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<FaqCategory> readFaq(XmlPullParser parser) throws XmlPullParserException, IOException {
        items = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "faq");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if ("category".equals(name)) {
                items.add(readCategory(parser));
            } else {
                skip(parser);
            }
        }

        return items;
    }

    private static FaqCategory readCategory(XmlPullParser parser) throws XmlPullParserException, IOException {
        FaqCategory faq = new FaqCategory();
        faq.items = new ArrayList();
        parser.require(XmlPullParser.START_TAG, ns, "category");
        faq.categoryTitle = readCategoryName(parser);

        int next = parser.next();
        while (next != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if ("question".equals(name)) {
                faq.items.add(readQuestion(parser));
            } else {
                skip(parser);
            }

            next = parser.next();
        }

        return faq;
    }

    private static FaqQuestion readQuestion(XmlPullParser parser) throws XmlPullParserException, IOException {
        FaqQuestion question = new FaqQuestion();
        parser.require(XmlPullParser.START_TAG, ns, "question");
        question.question = readQuestionName(parser);
        question.url = readUrl(parser);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if ("text".equals(name)) {

            } else {
                skip(parser);
            }
        }

        return question;
    }

    private static String readCategoryName(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "category");
        return parser.getAttributeValue(null, "name");
    }

    private static String readQuestionName(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "question");
        return parser.getAttributeValue(null, "name");
    }

    private static String readUrl(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "question");
        return parser.getAttributeValue(null, "link");
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    public static void showFaqDialog(final Context context) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://klinkerapps.com/talon-overview/help"));
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
