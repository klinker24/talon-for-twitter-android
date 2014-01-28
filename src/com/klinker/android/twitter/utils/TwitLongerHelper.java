package com.klinker.android.twitter.utils;

import android.util.Log;
import com.klinker.android.twitter.settings.AppSettings;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.internal.http.BASE64Encoder;
import twitter4j.internal.http.HttpParameter;

/**
 * This is just a simple helper class to facilitate things with TwitLonger's 2.0 APIs
 * It is based off the assertion that you use Twitter4j, but could be easily adapted if you don't
 *
 * @author Luke Klinker (along with help from Twitter4j)
 */
public class TwitLongerHelper {

    public static final String TWITLONGER_API_KEY = "***REMOVED***";
    public static final String SERVICE_PROVIDER = "https://api.twitter.com/1.1/account/verify_credentials.json";
    public static final String POST_URL = "http://api.twitlonger.com/2/posts";
    public static final String PUT_URL = "http://api.twitlonger.com/2/posts/"; // will add the id to the end of this later

    public String tweetText;
    public long replyToId;
    public long replyToStatusId = 0;
    public String replyToScreenname;

    public Twitter twitter;

    /**
     * Used for a normal tweet, not a reply
     * @param tweetText the text of the tweet that you want to post
     */
	public TwitLongerHelper(String tweetText, Twitter twitter) {
        this.tweetText = tweetText;
        this.replyToId = 0;
        this.replyToScreenname = null;

        this.twitter = twitter;
    }

    /**
     * Used when repling to a user and you have their id number
     * @param tweetText the text of the tweet that you want to post
     * @param replyToId the id of the user your tweet is replying to
     */
    public TwitLongerHelper(String tweetText, long replyToId, Twitter twitter) {
        this.tweetText = tweetText;
        this.replyToId = replyToId;
        this.replyToScreenname = null;

        this.twitter = twitter;
    }

    /**
     * Used when repling to a user and you have their id number
     * @param tweetText the text of the tweet that you want to post
     * @param replyToScreenname the screenname of the user you are replying to
     */
    public TwitLongerHelper(String tweetText, String replyToScreenname, Twitter twitter) {
        this.tweetText = tweetText;
        this.replyToScreenname = replyToScreenname;
        this.replyToId = 0;

        this.twitter = twitter;
    }

    /**
     * Sets the tweet id if it is replying to another users tweet
     * @param replyToStatusId
     */
    public void setInReplyToStatusId(long replyToStatusId) {
        this.replyToStatusId = replyToStatusId;
    }


    /**
     * posts the status onto Twitlonger, it then posts the shortened status (with link) to the user's twitter and updates the status on twitlonger
     * to include the posted status's id.
     *
     * @return id of the status that was posted to twitter
     */
    public long createPost() {
        TwitLongerStatus status = postToTwitLonger();
        long statusId;
        try {
            Status postedStatus;
            if (replyToStatusId != 0) {
                StatusUpdate update = new StatusUpdate(status.getText());
                update.setInReplyToStatusId(replyToStatusId);
                postedStatus = twitter.updateStatus(update);
            } else {
                postedStatus = twitter.updateStatus(status.getText());
            }

            statusId = postedStatus.getId();
            updateTwitlonger(status, statusId);
        } catch (TwitterException e) {
            e.printStackTrace();
            statusId = 0;
        }

        // if zero, then it failed
        return statusId;
    }

    /**
     * Posts the status to twitlonger
     * @return returns an object containing the shortened text and the id for the twitlonger url
     */
    public TwitLongerStatus postToTwitLonger() {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(POST_URL);
            post.addHeader("X-API-KEY", TWITLONGER_API_KEY);
            post.addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER);
            post.addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter));

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("content", tweetText));

            if (replyToId != 0) {
                nvps.add(new BasicNameValuePair("reply_to_id", String.valueOf(replyToId)));
            } else if (replyToScreenname != null) {
                nvps.add(new BasicNameValuePair("reply_to_screen_name", replyToScreenname));
            }

            post.setEntity(new UrlEncodedFormEntity(nvps));
            HttpResponse response = client.execute(post);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String line;
            if ((line = rd.readLine()) != null) {
                String content = line.substring(line.indexOf("tweet_content"), line.length() - 2);
                content = content.replace("tweet_content", "");
                content = content.substring(3);
                content = content.replace("http:\\/\\/tl.gd\\/", "http://tl.gd/");
                Log.v("TwitLonger_Talon", "Status: " + content);

                String id = line.substring(7, line.indexOf(",") - 1);
                Log.v("TwitLonger_Talon", "ID: " + id);

                return new TwitLongerStatus(content, id);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Updates the status on twitlonger to include the tweet id from Twitter.
     * Helpful for threading.
     * @param status Object with the shortened text and the id
     * @param tweetId tweet id of the status posted to twitter
     * @return true if the update is sucessful
     */
    public boolean updateTwitlonger(TwitLongerStatus status, long tweetId) {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPut put = new HttpPut(PUT_URL + status.getId());
            put.addHeader("X-API-KEY", TWITLONGER_API_KEY);
            put.addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER);
            put.addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter));

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("twitter_status_id", tweetId + ""));

            put.setEntity(new UrlEncodedFormEntity(nvps));
            HttpResponse response = client.execute(put);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            if (rd.readLine() != null) {
                Log.v("twitlonger", "updated the status successfully");
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Gets the header to verify the user on Twitter
     * @param twitter Coming from Twitter.getInstance()
     * @return String of the header to be used with X-Verify-Credentials-Authorization
     */
    public String getAuthrityHeader(Twitter twitter) {
        try {
            // gets the system time for the header
            long time = System.currentTimeMillis() / 1000;
            long millis = time + 12;

            // set the necessary parameters
            List<HttpParameter> oauthHeaderParams = new ArrayList<HttpParameter>(5);
            oauthHeaderParams.add(new HttpParameter("oauth_consumer_key", AppSettings.TWITTER_CONSUMER_KEY));
            oauthHeaderParams.add(new HttpParameter("oauth_signature_method", "HMAC-SHA1"));
            oauthHeaderParams.add(new HttpParameter("oauth_timestamp", time + ""));
            oauthHeaderParams.add(new HttpParameter("oauth_nonce", millis + ""));
            oauthHeaderParams.add(new HttpParameter("oauth_version", "1.0"));
            oauthHeaderParams.add(new HttpParameter("oauth_token", twitter.getOAuthAccessToken().getToken()));
            List<HttpParameter> signatureBaseParams = new ArrayList<HttpParameter>(oauthHeaderParams.size());
            signatureBaseParams.addAll(oauthHeaderParams);

            // create the signature
            StringBuilder base = new StringBuilder("GET").append("&")
                    .append(HttpParameter.encode(constructRequestURL(SERVICE_PROVIDER))).append("&");
            base.append(HttpParameter.encode(normalizeRequestParameters(signatureBaseParams)));

            String oauthBaseString = base.toString();
            String signature = generateSignature(oauthBaseString, twitter.getOAuthAccessToken());

            oauthHeaderParams.add(new HttpParameter("oauth_signature", signature));

            // create the header to post
            return "OAuth " + encodeParameters(oauthHeaderParams, ",", true);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Generates the signature to use with the header
     * @param data base signature data
     * @param token the user's access token
     * @return String of the signature to use in your header
     */
    public String generateSignature(String data, AccessToken token) {
        byte[] byteHMAC = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec spec;
            String oauthSignature = HttpParameter.encode(AppSettings.TWITTER_CONSUMER_SECRET) + "&" + HttpParameter.encode(token.getTokenSecret());
            spec = new SecretKeySpec(oauthSignature.getBytes(), "HmacSHA1");
            mac.init(spec);
            byteHMAC = mac.doFinal(data.getBytes());
        } catch (InvalidKeyException ike) {
            throw new AssertionError(ike);
        } catch (NoSuchAlgorithmException nsae) {
            throw new AssertionError(nsae);
        }
        return BASE64Encoder.encode(byteHMAC);
    }

    /**
     * Sorts and prepares the parameters
     * @param params Your parameters to post
     * @return String of the encoded parameters
     */
    static String normalizeRequestParameters(List<HttpParameter> params) {
        Collections.sort(params);
        return encodeParameters(params, "&", false);
    }

    /**
     * Encodes the parameters
     * @param httpParams parameters you want to send
     * @param splitter character used to split the parameters
     * @param quot whether you should use quotations or not
     * @return string of the desired encoding
     */
    public static String encodeParameters(List<HttpParameter> httpParams, String splitter, boolean quot) {
        StringBuilder buf = new StringBuilder();
        for (HttpParameter param : httpParams) {
            if (!param.isFile()) {
                if (buf.length() != 0) {
                    if (quot) {
                        buf.append("\"");
                    }
                    buf.append(splitter);
                }
                buf.append(HttpParameter.encode(param.getName())).append("=");
                if (quot) {
                    buf.append("\"");
                }
                buf.append(HttpParameter.encode(param.getValue()));
            }
        }
        if (buf.length() != 0) {
            if (quot) {
                buf.append("\"");
            }
        }
        return buf.toString();
    }

    /**
     * Used to create the base signature text
     * @param url url of the post
     * @return string of the base signature
     */
    static String constructRequestURL(String url) {
        int index = url.indexOf("?");
        if (-1 != index) {
            url = url.substring(0, index);
        }
        int slashIndex = url.indexOf("/", 8);
        String baseURL = url.substring(0, slashIndex).toLowerCase();
        int colonIndex = baseURL.indexOf(":", 8);
        if (-1 != colonIndex) {
            // url contains port number
            if (baseURL.startsWith("http://") && baseURL.endsWith(":80")) {
                // http default port 80 MUST be excluded
                baseURL = baseURL.substring(0, colonIndex);
            } else if (baseURL.startsWith("https://") && baseURL.endsWith(":443")) {
                // http default port 443 MUST be excluded
                baseURL = baseURL.substring(0, colonIndex);
            }
        }
        url = baseURL + url.substring(slashIndex);

        return url;
    }

    class TwitLongerStatus {
        private String text;
        private String id;

        public TwitLongerStatus(String text, String id) {
            this.text = text;
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }
    }

}
