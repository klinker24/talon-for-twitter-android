package com.klinker.android.twitter_l.utils.api_helper;

import android.os.AsyncTask;
import android.util.Log;

import com.klinker.android.twitter_l.data.WebPreview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ArticleParserHelper {

    public static void getArticle(String url, MercuryArticleParserHelper.Callback callback) {
        new ParseLink(url, callback).execute();
    }

    private static class ParseLink extends AsyncTask<Void, Void, WebPreview> {

        private String url;
        private MercuryArticleParserHelper.Callback callback;

        public ParseLink(String url, MercuryArticleParserHelper.Callback callback) {
            this.url = url;
            this.callback = callback;
        }

        @Override
        protected WebPreview doInBackground(Void... arg0) {
            try {
                long startTime = System.currentTimeMillis();

                String url = this.url;
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("HEAD");
                connection.setInstanceFollowRedirects(false);
                connection.getResponseCode();

                url = connection.getHeaderField("location");
                if (url == null || url.isEmpty()) {
                    url = this.url;
                }

                InputStream is = new URL(url).openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line = null;
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");

                    if (sb.toString().contains("</head>")) {
                        break;
                    }
                }

                sb.append("</html>");
                String docHtml = sb.toString();

                reader.close();
                is.close();

                Document document = Jsoup.parse(docHtml);
                if (document == null) {
                    return new WebPreview("", url, "", "", url);
                }

                String title = getTitle(document);
                String summary = getSummary(document);
                String leadImage = getImage(document);
                String webDomain = getDomain(url);

                Log.v("article_parser", System.currentTimeMillis() - startTime + " ms");

                if (title.contains("404")) {
                    throw new RuntimeException("No article found");
                }

                return new WebPreview(title, summary, leadImage, webDomain, this.url);
            } catch (Exception e) {
                e.printStackTrace();
                return new WebPreview("", getDomain(url), "", "", url);
            }
        }

        @Override
        protected void onPostExecute(WebPreview result) {
            if (callback != null) {
                callback.onResponse(result);
            }
        }

        private String getTitle(Document document) {
            Elements elements = document.getElementsByAttributeValue("property", "og:title");
            if (elements.size() > 0) {
                return elements.get(0).attr("content");
            }

            elements = document.getElementsByAttributeValue("property", "twitter:title");
            if (elements.size() > 0) {
                return elements.get(0).attr("content");
            }

            return "";
        }

        private String getImage(Document document) {
            Elements elements = document.getElementsByAttributeValue("property", "og:image");
            if (elements.size() > 0) {
                return elements.get(0).attr("content");
            }

            elements = document.getElementsByAttributeValue("property", "twitter:image");
            if (elements.size() > 0) {
                return elements.get(0).attr("content");
            }

            return "";
        }

        private String getSummary(Document document) {
            Elements elements = document.getElementsByAttributeValue("property", "og:description");
            if (elements.size() > 0) {
                return elements.get(0).attr("content");
            }

            elements = document.getElementsByAttributeValue("property", "twitter:description");
            if (elements.size() > 0) {
                return elements.get(0).attr("content");
            }

            return "";
        }

        private String getDomain(String url) {
            url = url.replace("https://", "").replace("http://", "");

            if (url.contains("/")) {
                url = url.substring(0, url.indexOf("/"));
            }

            return url;
        }
    }

}
