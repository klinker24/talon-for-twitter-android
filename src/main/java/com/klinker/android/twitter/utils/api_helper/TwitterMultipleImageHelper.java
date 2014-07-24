package com.klinker.android.twitter.utils.api_helper;

import android.graphics.Bitmap;
import android.util.Log;

import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.TweetLinkUtils;

import org.apache.http.*;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import twitter4j.BASE64Encoder;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;


public class TwitterMultipleImageHelper {

    public ArrayList<String> getImageURLs (Status status, Twitter twitter) {

        ArrayList<String> images = TweetLinkUtils.getAllExternalPictures(status);
        try {
            AccessToken token = twitter.getOAuthAccessToken();
            String oauth_token = token.getToken();
            String oauth_token_secret = token.getTokenSecret();

            // generate authorization header
            String get_or_post = "GET";
            String oauth_signature_method = "HMAC-SHA1";

            String uuid_string = UUID.randomUUID().toString();
            uuid_string = uuid_string.replaceAll("-", "");
            String oauth_nonce = uuid_string; // any relatively random alphanumeric string will work here

            // get the timestamp
            Calendar tempcal = Calendar.getInstance();
            long ts = tempcal.getTimeInMillis();// get current time in milliseconds
            String oauth_timestamp = (new Long(ts/1000)).toString(); // then divide by 1000 to get seconds

            // the parameter string must be in alphabetical order, "text" parameter added at end
            String parameter_string = "oauth_consumer_key=" + AppSettings.TWITTER_CONSUMER_KEY + "&oauth_nonce=" + oauth_nonce + "&oauth_signature_method=" + oauth_signature_method +
                    "&oauth_timestamp=" + oauth_timestamp + "&oauth_token=" + encode(oauth_token) + "&oauth_version=1.0";

            String twitter_endpoint = "https://api.twitter.com/1.1/statuses/show/" + status.getId() + ".json";
            String twitter_endpoint_host = "api.twitter.com";
            String twitter_endpoint_path = "/1.1/statuses/show/" + status.getId() + ".json";
            String signature_base_string = get_or_post + "&"+ encode(twitter_endpoint) + "&" + encode(parameter_string);
            String oauth_signature = computeSignature(signature_base_string, AppSettings.TWITTER_CONSUMER_SECRET + "&" + encode(oauth_token_secret));

            String authorization_header_string = "OAuth oauth_consumer_key=\"" + AppSettings.TWITTER_CONSUMER_KEY + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" + oauth_timestamp +
                    "\",oauth_nonce=\"" + oauth_nonce + "\",oauth_version=\"1.0\",oauth_signature=\"" + encode(oauth_signature) + "\",oauth_token=\"" + encode(oauth_token) + "\"";


            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "UTF-8");
            HttpProtocolParams.setUserAgent(params, "HttpCore/1.1");
            HttpProtocolParams.setUseExpectContinue(params, false);
            HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
                    // Required protocol interceptors
                    new RequestContent(),
                    new RequestTargetHost(),
                    // Recommended protocol interceptors
                    new RequestConnControl(),
                    new RequestUserAgent(),
                    new RequestExpectContinue()});

            HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
            HttpContext context = new BasicHttpContext(null);
            HttpHost host = new HttpHost(twitter_endpoint_host,443);
            DefaultHttpClientConnection conn = new DefaultHttpClientConnection();

            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);

            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, null, null);
            SSLSocketFactory ssf = sslcontext.getSocketFactory();
            Socket socket = ssf.createSocket();
            socket.connect(
                    new InetSocketAddress(host.getHostName(), host.getPort()), 0);
            conn.bind(socket, params);
            BasicHttpEntityEnclosingRequest request2 = new BasicHttpEntityEnclosingRequest("GET", twitter_endpoint_path);
            request2.setParams(params);
            request2.addHeader("Authorization", authorization_header_string);
            httpexecutor.preProcess(request2, httpproc, context);
            HttpResponse response2 = httpexecutor.execute(request2, conn, context);
            response2.setParams(params);
            httpexecutor.postProcess(response2, httpproc, context);
            String responseBody = EntityUtils.toString(response2.getEntity());
            conn.close();

            JSONObject fullJson = new JSONObject(responseBody);
            JSONObject extendedEntities = fullJson.getJSONObject("extended_entities");
            JSONArray media = extendedEntities.getJSONArray("media");

            Log.v("talon_images", media.toString());

            for (int i = 0; i < media.length(); i++) {
                JSONObject entity = media.getJSONObject(i);
                try {
                    // parse through the objects and get the media_url
                    String url = entity.getString("media_url");
                    String type = entity.getString("type");

                    // want to check to make sure it doesn't have it already
                    // this also checks to confirm that the entity is in fact a photo
                    if (!images.contains(url) && type.equals("photo")) {
                        images.add(url);
                    }
                } catch (Exception e) {

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return images;
    }

    public String encode(String value)
    {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
        }
        StringBuilder buf = new StringBuilder(encoded.length());
        char focus;
        for (int i = 0; i < encoded.length(); i++) {
            focus = encoded.charAt(i);
            if (focus == '*') {
                buf.append("%2A");
            } else if (focus == '+') {
                buf.append("%20");
            } else if (focus == '%' && (i + 1) < encoded.length()
                    && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
                buf.append('~');
                i += 2;
            } else {
                buf.append(focus);
            }
        }
        return buf.toString();
    }

    private static String computeSignature(String baseString, String keyString) throws GeneralSecurityException, UnsupportedEncodingException
    {
        SecretKey secretKey = null;

        byte[] keyBytes = keyString.getBytes();
        secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(secretKey);

        byte[] text = baseString.getBytes();

        return new String(BASE64Encoder.encode(mac.doFinal(text))).trim();
    }

    public boolean uploadPics(File[] pics, String text, Twitter twitter) {
        JSONObject jsonresponse = new JSONObject();

        try {
            AccessToken token = twitter.getOAuthAccessToken();
            String oauth_token = token.getToken();
            String oauth_token_secret = token.getTokenSecret();

            // generate authorization header
            String get_or_post = "GET";
            String oauth_signature_method = "HMAC-SHA1";

            String uuid_string = UUID.randomUUID().toString();
            uuid_string = uuid_string.replaceAll("-", "");
            String oauth_nonce = uuid_string; // any relatively random alphanumeric string will work here

            // get the timestamp
            Calendar tempcal = Calendar.getInstance();
            long ts = tempcal.getTimeInMillis();// get current time in milliseconds
            String oauth_timestamp = (new Long(ts / 1000)).toString(); // then divide by 1000 to get seconds

            // the parameter string must be in alphabetical order, "text" parameter added at end
            String parameter_string = "oauth_consumer_key=" + AppSettings.TWITTER_CONSUMER_KEY + "&oauth_nonce=" + oauth_nonce + "&oauth_signature_method=" + oauth_signature_method +
                    "&oauth_timestamp=" + oauth_timestamp + "&oauth_token=" + encode(oauth_token) + "&oauth_version=1.0";
            System.out.println("Twitter.updateStatusWithMedia(): parameter_string=" + parameter_string);

            String twitter_endpoint = "https://api.twitter.com/1.1/statuses/update_with_media.json";
            String twitter_endpoint_host = "api.twitter.com";
            String twitter_endpoint_path = "/1.1/statuses/update_with_media.json";
            String signature_base_string = get_or_post + "&" + encode(twitter_endpoint) + "&" + encode(parameter_string);
            System.out.println("Twitter.updateStatusWithMedia(): signature_base_string=" + signature_base_string);
            String oauth_signature = "";
            try {
                oauth_signature = computeSignature(signature_base_string, AppSettings.TWITTER_CONSUMER_SECRET + "&" + encode(oauth_token_secret));
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String authorization_header_string = "OAuth oauth_consumer_key=\"" + AppSettings.TWITTER_CONSUMER_KEY + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" + oauth_timestamp +
                    "\",oauth_nonce=\"" + oauth_nonce + "\",oauth_version=\"1.0\",oauth_signature=\"" + encode(oauth_signature) + "\",oauth_token=\"" + encode(oauth_token) + "\"";
            System.out.println("Twitter.updateStatusWithMedia(): authorization_header_string=" + authorization_header_string);


            HttpParams params = new SyncBasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "UTF-8");
            HttpProtocolParams.setUserAgent(params, "HttpCore/1.1");
            HttpProtocolParams.setUseExpectContinue(params, false);
            HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[]{
                    // Required protocol interceptors
                    new RequestContent(),
                    new RequestTargetHost(),
                    // Recommended protocol interceptors
                    new RequestConnControl(),
                    new RequestUserAgent(),
                    new RequestExpectContinue()});

            HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
            HttpContext context = new BasicHttpContext(null);
            HttpHost host = new HttpHost(twitter_endpoint_host, 443);
            DefaultHttpClientConnection conn = new DefaultHttpClientConnection();

            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);

            try {
                try {
                    SSLContext sslcontext = SSLContext.getInstance("TLS");
                    sslcontext.init(null, null, null);
                    SSLSocketFactory ssf = sslcontext.getSocketFactory();
                    Socket socket = ssf.createSocket();
                    socket.connect(
                            new InetSocketAddress(host.getHostName(), host.getPort()), 0);
                    conn.bind(socket, params);

                    // System.out.println("Twitter.updateStatusWithMedia(): params all set, creating socket.");
                    // Socket socket = new Socket(host.getHostName(), host.getPort());
                    // conn.bind(socket, params);

                    BasicHttpEntityEnclosingRequest request2 = new BasicHttpEntityEnclosingRequest("POST", twitter_endpoint_path);
                    // need to add status parameter to this POST
                    MultipartEntity reqEntity = new MultipartEntity();
                    StringBody sb_status = new StringBody(text);
                    reqEntity.addPart("status", sb_status);

                    // sets the media entities for up to 4 pictures
                    for (File file : pics) {
                        FileBody sb_image = new FileBody(file);
                        reqEntity.addPart("media[]", sb_image);
                    }

                    request2.setEntity(reqEntity);


                    //request2.setEntity( new StringEntity("media[]=" + encode(image_url) + "&status=" + encode(text), "multipart/form-data; boundary=---1234", "UTF-8"));
                    request2.setParams(params);

                    request2.addHeader("Authorization", authorization_header_string);
                    System.out.println("Twitter.updateStatusWithMedia(): Entity, params and header added to request. Preprocessing and executing...");
                    httpexecutor.preProcess(request2, httpproc, context);
                    HttpResponse response2 = httpexecutor.execute(request2, conn, context);
                    System.out.println("Twitter.updateStatusWithMedia(): ... done. Postprocessing...");
                    response2.setParams(params);
                    httpexecutor.postProcess(response2, httpproc, context);
                    String responseBody = EntityUtils.toString(response2.getEntity());
                    System.out.println("Twitter.updateStatusWithMedia(): done. response=" + responseBody);
                    // error checking here. Otherwise, status should be updated.
                    jsonresponse = new JSONObject(responseBody);
                    if (jsonresponse.has("errors")) {
                        JSONObject temp_jo = new JSONObject();
                        temp_jo.put("response_status", "error");
                        temp_jo.put("message", jsonresponse.getJSONArray("errors").getJSONObject(0).getString("message"));
                        temp_jo.put("twitter_code", jsonresponse.getJSONArray("errors").getJSONObject(0).getInt("code"));
                        jsonresponse = temp_jo;
                    }
                    conn.close();
                } catch (HttpException he) {
                    System.out.println(he.getMessage());
                    jsonresponse.put("response_status", "error");
                    jsonresponse.put("message", "updateStatusWithMedia HttpException message=" + he.getMessage());
                    return false;
                } catch (NoSuchAlgorithmException nsae) {
                    System.out.println(nsae.getMessage());
                    jsonresponse.put("response_status", "error");
                    jsonresponse.put("message", "updateStatusWithMedia NoSuchAlgorithmException message=" + nsae.getMessage());
                    return false;
                } catch (KeyManagementException kme) {
                    System.out.println(kme.getMessage());
                    jsonresponse.put("response_status", "error");
                    jsonresponse.put("message", "updateStatusWithMedia KeyManagementException message=" + kme.getMessage());
                    return false;
                } finally {
                    conn.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                jsonresponse.put("response_status", "error");
                jsonresponse.put("message", "updateStatusWithMedia IOException message=" + ioe.getMessage());
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
