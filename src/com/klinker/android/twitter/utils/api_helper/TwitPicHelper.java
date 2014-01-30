package com.klinker.android.twitter.utils.api_helper;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;

public class TwitPicHelper extends APIHelper {

    public static final String TWITPIC_API_KEY = "8cd3757bb6acb94c61e3cbf840c91872";
    public static final String POST_URL = "http://api.twitpic.com/2/upload.json";

    private Twitter twitter;
    private String message;
    private File file;
    private long replyToStatusId = 0;
    private GeoLocation location = null;

    public TwitPicHelper(Twitter twitter, String message, File picture) {
        this.twitter = twitter;
        this.message = message;
        this.file = picture;
    }

    /**
     * Sets the tweet id if it is replying to another users tweet
     * @param replyToStatusId
     */
    public void setInReplyToStatusId(long replyToStatusId) {
        this.replyToStatusId = replyToStatusId;
    }

    public void setLocation(GeoLocation location) {
        this.location = location;
    }

    /**
     * posts the status onto Twitlonger, it then posts the shortened status (with link) to the user's twitter and updates the status on twitlonger
     * to include the posted status's id.
     *
     * @return id of the status that was posted to twitter
     */
    public long createPost() {
        TwitPicStatus status = uploadToTwitPic();
        Log.v("talon_twitpic", "past upload");
        long statusId;
        try {
            Status postedStatus;
            StatusUpdate update = new StatusUpdate(status.getText());

            if (replyToStatusId != 0) {
                update.setInReplyToStatusId(replyToStatusId);
            }
            if (location != null) {
                update.setLocation(location);
            }

            postedStatus = twitter.updateStatus(update);

            statusId = postedStatus.getId();
        } catch (Exception e) {
            e.printStackTrace();
            statusId = 0;
        }

        // if zero, then it failed
        return statusId;
    }

    private TwitPicStatus uploadToTwitPic() {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(POST_URL);
            post.addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER);
            post.addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter));

            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            entity.addPart("key", new StringBody(TWITPIC_API_KEY));
            entity.addPart("media", new FileBody(file));
            entity.addPart("message", new StringBody(message));


            Log.v("talon_twitpic", "uploading now");

            post.setEntity(entity);
            HttpResponse response = client.execute(post);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String line;
            String url = "";
            StringBuilder builder = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                Log.v("talon_twitpic", line);
                builder.append(line);
            }

            try {
                // there is only going to be one thing returned ever
                JSONObject jsonObject = new JSONObject(builder.toString());
                url = jsonObject.getString("url");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.v("talon_twitpic", "url: " + url);
            Log.v("talon_twitpic", "message: " + message);

            return new TwitPicStatus(message, url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    class TwitPicStatus {
        private String tweetText;
        private String picUrl;
        private String totalTweet;

        public TwitPicStatus(String text, String url) {
            this.tweetText = text;
            this.picUrl = url;

            this.totalTweet = text + " " + url;
        }

        public String getText() {
            return totalTweet;
        }
    }

}
