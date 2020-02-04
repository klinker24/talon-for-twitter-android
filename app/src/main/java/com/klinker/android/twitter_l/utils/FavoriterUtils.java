package com.klinker.android.twitter_l.utils;


import android.content.Context;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import twitter4j.Twitter;
import twitter4j.User;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FavoriterUtils {

    /**
     * This does not work. The user list is loaded via JS after the page has been created. We cannot scrape it like this.
     *
     * This used to use a different method, but Twitter removed the functionality required to get that user list.
     */
    public List<User> getFavoriters(Context context, String username, long tweetId) {
        List<User> users = new ArrayList<User>();

        Twitter twitter =  Utils.getTwitter(context, null);

//        try {
//            String[] names = getFavoritesScreennames(username, tweetId);
//            users = twitter.lookupUsers(names);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return users;
    }

    private String getPage(String screenname, long tweetId) {
        try {
            String url = "https://twitter.com/" + screenname + "/status/" + tweetId + "/likes";
            URL obj = new URL(url);

            HttpsURLConnection connection = (HttpsURLConnection) obj.openConnection();
            connection.setRequestProperty("Content-Type", "text/html");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestMethod("GET");
            connection.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.94 Safari/537.36");
            connection.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            String docHtml = sb.toString();

            try {
                connection.disconnect();
            } catch (Exception e) {

            }

            return docHtml;
        } catch (Exception e) {
            return null;
        }
    }

    private String[] getFavoritesScreennames(String username, long tweetId) {
        List<String> screennames = new ArrayList<>();
        try {
            String html = getPage(username, tweetId);
            Document doc = Jsoup.parse(html);

            if (doc != null) {
                Elements elements = doc.getElementsByAttributeValue("data-testid", "UserCell");

                for (Element e : elements) {
                    try {
                        Log.v("FavoriteUtils", e.toString());
//                        Long l = Long.parseLong(e.attr("data-user-id"));
//                        if (l != null) {
//                            idsList.add(l);
//                        }
                    } catch (Exception x) {
                        // doesn't have it, could be an emoji or something from the looks of it.
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        String[] names = new String[screennames.size()];

        for (int i = 0; i < names.length; i++) {
            names[i] = screennames.get(i);
        }

        return names;
    }
}
