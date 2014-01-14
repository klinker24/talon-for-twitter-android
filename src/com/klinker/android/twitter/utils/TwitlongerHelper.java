package com.klinker.android.twitter.utils;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * A helper class for using the Twitlonger API.
 * @author Aidan Follestad
 */
public class TwitlongerHelper {

	private TwitlongerHelper(String application, String apiKey, String username) {
		_app = application;
		_key = apiKey;
		_user = username;
	}
	
	private String _app;
	private String _key;
	private String _user;
	
	/**
	 * Creates a new TwitlongerHelper instance.
	 * @param application The application identifier given to you by Twitlonger.
	 * @param apiKey The API key given to you by Twitlonger.
	 * @param username The screenname of the user that will be posting long tweets.
	 * @return A new TwitlongerHelper instance ready for posting and reading Twitlonger tweets.
	 */
	public static TwitlongerHelper create(String application, String apiKey, String username) { return new TwitlongerHelper(application, apiKey, username); }
	
	/**
	 * Makes a post to Twitlonger, returning the text that should be posted in a tweet on Twitter.
	 * @param message The full content of the tweet, should be over 140 characters since you're using Twitlonger.
	 * @param inReplyTo The optional ID of the tweet this post is in reply to.
	 * @param inReplyToName The optional username of the user that posted the tweet that this post is in reply to.
	 * @return The text that should be posted on Twitter.
	 * @throws Exception
	 */
	public TwitlongerPostResponse post(String message, long inReplyTo, String inReplyToName) throws Exception {
		ArrayList<NameValuePair> args = new ArrayList<NameValuePair>(2);
	    args.add(new BasicNameValuePair("application", _app));
	    args.add(new BasicNameValuePair("api_key", _key));
	    args.add(new BasicNameValuePair("username", _user));
	    args.add(new BasicNameValuePair("message", message));
	    if(inReplyTo > 0) {
	    	args.add(new BasicNameValuePair("in_reply", Long.toString(inReplyTo)));
	    	if(inReplyToName != null && !inReplyToName.trim().isEmpty()) args.add(new BasicNameValuePair("in_reply_user", inReplyToName));
	    }
	    Element xml = makeXmlPost("http://www.twitlonger.com/api_post", args);
	    return new TwitlongerPostResponse(xml);
	}
	
	/**
	 * Performs the Twitlonger callback, should be done after successfully using the 'post' method.
	 * @param twitterId The ID of the tweet posted directly to Twitter by you using the results of 'post'. 
	 * @param response The TwitlongerPostResponse instance returned from your previous use of 'post'.
	 * @return True if successful.
	 * @throws Exception 
	 */
	public boolean callback(long twitterId, TwitlongerPostResponse response) throws Exception {
		ArrayList<NameValuePair> args = new ArrayList<NameValuePair>(2);
	    args.add(new BasicNameValuePair("application", _app));
	    args.add(new BasicNameValuePair("api_key", _key));
	    args.add(new BasicNameValuePair("message_id", response.getId()));
	    args.add(new BasicNameValuePair("twitter_id ", Long.toString(twitterId)));
	    return makePost("http://www.twitlonger.com/api_set_id", args);
	}
	
	/**
	 * Retrieves the full expanded content of a tweet longer post.
	 * @param id The ID (at the end of a shortened URL such as tl.gd/id).
	 * @return The full expanded content.
	 * @throws Exception
	 */
	public String readPost(String id) throws Exception {
		return makeGet("http://www.twitlonger.com/api_read/" + id).getElementsByTagName("content").item(0).getTextContent();
	}
	
	private Element makeXmlPost(String url, ArrayList<NameValuePair> entities) throws Exception {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(url);
	    httppost.setEntity(new UrlEncodedFormEntity(entities));
	    HttpResponse response = httpclient.execute(httppost);
	    String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
	    Element toReturn = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(responseStr))).getDocumentElement();
	    if(toReturn.getElementsByTagName("error") != null && toReturn.getElementsByTagName("error").getLength() > 0) {
	    	throw new Exception(toReturn.getElementsByTagName("error").item(0).getTextContent());
	    }
	    return toReturn;
	}
	private boolean makePost(String url, ArrayList<NameValuePair> entities) throws Exception {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(url);
	    httppost.setEntity(new UrlEncodedFormEntity(entities));
	    HttpResponse response = httpclient.execute(httppost);
	    String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
	    try { 
	    	Element toReturn = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(responseStr))).getDocumentElement();
	    	if(toReturn.getElementsByTagName("error") != null && toReturn.getElementsByTagName("error").getLength() > 0) {
	    		throw new Exception(toReturn.getElementsByTagName("error").item(0).getTextContent());
	    	}
	    } catch(Exception e) { return (response.getStatusLine().getStatusCode() == 200); }
	    return true;
	}
	private Element makeGet(String url) throws Exception {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpGet httpget = new HttpGet(url);
	    HttpResponse response = httpclient.execute(httpget);
	    String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
	    try { 
	    	Element toReturn = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(responseStr))).getDocumentElement();
	    	if(toReturn.getElementsByTagName("error") != null && toReturn.getElementsByTagName("error").getLength() > 0) {
	    		throw new Exception(toReturn.getElementsByTagName("error").item(0).getTextContent());
	    	}
	    	return toReturn;
	    } catch(Exception e) { e.printStackTrace(); }
	    return null;
	}

	/**
	 * Contains a response returned from posting to Twitlonger.
	 * @author Aidan Follestad
	 */
	public static class TwitlongerPostResponse {
		
		/**
		 * Initializes a new TwitlongerResponse instance by parsing returned XML.
		 * @param xml The XML returned from a Twitlonger request.
		 */
		public TwitlongerPostResponse(Element xml) {
			Element post = (Element)xml.getElementsByTagName("post").item(0);			
			content = post.getElementsByTagName("content").item(0).getTextContent();
			id = post.getElementsByTagName("id").item(0).getTextContent();
		}
		
		private String content;
		private String id;
		
		/**
		 * Gets the content of the Twitlonge post, which is the shortened version of your over-140-character tweet.
		 * @return Twitlonger post content
		 */
		public String getContent() { return content; }
		/**
		 * Gets the ID of the Twitlonger post.
		 * @return The Twitlonger post ID.
		 */
		public String getId() { return id; }
	}
}
