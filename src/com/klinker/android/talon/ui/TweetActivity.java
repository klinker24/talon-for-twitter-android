package com.klinker.android.talon.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.klinker.android.talon.adapters.ArrayListLoader;
import com.klinker.android.talon.adapters.CursorListLoader;
import com.klinker.android.talon.adapters.TimelineArrayAdapter;
import com.klinker.android.talon.R;
import com.klinker.android.talon.manipulations.ExpansionAnimation;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.utilities.App;
import com.klinker.android.talon.utilities.IOUtils;
import com.klinker.android.talon.utilities.Utils;
import com.squareup.picasso.Picasso;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.*;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.photoview.PhotoViewAttacher;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class TweetActivity extends Activity {

    private AppSettings settings;
    private Context context;

    private String name;
    private String screenName;
    private String tweet;
    private long time;
    private String retweeter;
    private String webpage;
    private String proPic;
    private boolean picture;
    private long tweetId;

    private TextView timetv;

    private WebView website;
    private NetworkedCacheableImageView pictureIv;

    private ImageView attachImage;
    private String attachedFilePath = "";

    private boolean isMyTweet = false;
    private boolean isMyRetweet = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        settings = new AppSettings(context);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        setUpTheme();

        if (settings.advanceWindowed) {
            setUpWindow();
        }

        getFromIntent();

        setContentView(R.layout.tweet_activity);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);

        setUIElements();

    }

    public void setUpTheme() {

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight_Popup);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark_Popup);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack_Popup);
                break;
        }

    }

    public void setUpWindow() {

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .75f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .8));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

    }

    public void getFromIntent() {
        Intent from = getIntent();

        name = from.getStringExtra("name");
        screenName = from.getStringExtra("screenname");
        tweet = from.getStringExtra("tweet");
        time = from.getLongExtra("time", 0);
        retweeter = from.getStringExtra("retweeter");
        webpage = from.getStringExtra("webpage");
        tweetId = from.getLongExtra("tweetid", 0);
        picture = from.getBooleanExtra("picture", false);
        proPic = from.getStringExtra("proPic");

        if (screenName.equals(settings.myScreenName)) {
            isMyTweet = true;
        } else if (screenName.equals(retweeter)) {
            isMyRetweet = true;
        }
    }

    PhotoViewAttacher mAttacher;

    public void setUIElements() {
        TextView nametv = (TextView) findViewById(R.id.name);
        TextView screennametv = (TextView) findViewById(R.id.screen_name);
        TextView tweettv = (TextView) findViewById(R.id.tweet);
        timetv = (TextView) findViewById(R.id.time);
        final TextView retweetertv = (TextView) findViewById(R.id.retweeter);
        website = (WebView) findViewById(R.id.webview);
        pictureIv = (NetworkedCacheableImageView) findViewById(R.id.imageView);
        final AsyncListView replyList = (AsyncListView) findViewById(R.id.reply_list);
        LinearLayout progressSpinner = (LinearLayout) findViewById(R.id.list_progress);
        final LinearLayout background = (LinearLayout) findViewById(R.id.tweet_background);
        final ImageButton expand = (ImageButton) findViewById(R.id.switchViews);

        final ImageView profilePic = (ImageView) findViewById(R.id.profile_pic);

        final ImageButton favoriteButton = (ImageButton) findViewById(R.id.favorite);
        final ImageButton retweetButton = (ImageButton) findViewById(R.id.retweet);
        final TextView favoriteCount = (TextView) findViewById(R.id.fav_count);
        final TextView retweetCount = (TextView) findViewById(R.id.retweet_count);
        final EditText reply = (EditText) findViewById(R.id.reply);
        final ImageButton replyButton = (ImageButton) findViewById(R.id.reply_button);
        ImageButton attachButton = (ImageButton) findViewById(R.id.attach_button);

        nametv.setTextSize(settings.textSize +2);
        screennametv.setTextSize(settings.textSize);
        tweettv.setTextSize(settings.textSize);
        timetv.setTextSize(settings.textSize - 3);
        retweetertv.setTextSize(settings.textSize - 3);
        favoriteCount.setTextSize(settings.textSize + 1);
        retweetCount.setTextSize(settings.textSize + 1);
        reply.setTextSize(settings.textSize);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        replyList.setItemManager(builder.build());

        if (settings.theme == 0) {
            nametv.setTextColor(getResources().getColor(android.R.color.black));
            nametv.setShadowLayer(0,0,0, getResources().getColor(android.R.color.transparent));
            screennametv.setTextColor(getResources().getColor(android.R.color.black));
            screennametv.setShadowLayer(0,0,0, getResources().getColor(android.R.color.transparent));
        }

        if (name.contains(settings.myName)) {
            reply.setVisibility(View.GONE);
            replyButton.setVisibility(View.GONE);
            attachButton.setVisibility(View.GONE);
            attachButton.setEnabled(false);
            favoriteButton.setEnabled(false);
            retweetButton.setEnabled(false);
        }

        Picasso.with(context)
                .load(proPic)
                .transform(new CircleTransform())
                .into(profilePic);

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewProfile = new Intent(context, UserProfileActivity.class);
                viewProfile.putExtra("name", name);
                viewProfile.putExtra("screenname", screenName);
                viewProfile.putExtra("proPic", proPic);
                viewProfile.putExtra("tweetid", tweetId);
                viewProfile.putExtra("retweet", retweetertv.getVisibility() == View.VISIBLE);

                context.startActivity(viewProfile);
            }
        });

        if (tweet.contains("http://")) {
            String[] split = tweet.split(" ");

            for (String s : split) {
                if (s.contains("http://")) {
                    s.replaceAll("!", "");
                    s.replaceAll("\"", "");

                    if(webpage == null) {
                        webpage = s;
                    }

                    break;
                }
            }
        }

        // If there is a web page that isn't a picture already loaded
        if (webpage != null && !picture) {
            /*switchViews.setVisibility(View.VISIBLE);
            switchViews.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    website.setVisibility(View.GONE);
                    replyList.setVisibility(View.VISIBLE);
                }
            });*/

            progressSpinner.setVisibility(View.GONE);
            website.setVisibility(View.VISIBLE);
            website.getSettings().setJavaScriptEnabled(true);
            website.getSettings().setBuiltInZoomControls(true);

            final Activity activity = this;
            website.setWebChromeClient(new WebChromeClient() {
                public void onProgressChanged(WebView view, int progress) {
                    // Activities and WebViews measure progress with different scales.
                    // The progress meter will automatically disappear when we reach 100%
                    activity.setProgress(progress * 100);
                }
            });

            website.setWebViewClient(new WebViewClient() {
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(activity, getResources().getString(R.string.error_loading_page), Toast.LENGTH_SHORT).show();
                }
            });

            website.loadUrl(webpage);

        } else if(picture) { // if there is a picture already loaded
            /*switchViews.setVisibility(View.VISIBLE);
            switchViews.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pictureIv.setVisibility(View.GONE);
                    replyList.setVisibility(View.VISIBLE);
                }
            });*/

            progressSpinner.setVisibility(View.GONE);
            pictureIv.setVisibility(View.VISIBLE);

            pictureIv.loadImage(webpage, false, null);

            mAttacher = new PhotoViewAttacher(pictureIv);

        } else { // just show the replys
            progressSpinner.setVisibility(View.VISIBLE);
        }

        if (website.getVisibility() == View.VISIBLE || pictureIv.getVisibility() == View.VISIBLE) {
            expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(background.getVisibility() == View.VISIBLE) {
                        Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate);
                        ranim.setFillAfter(true);
                        expand.startAnimation(ranim);
                    } else {
                        Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate_back);
                        ranim.setFillAfter(true);
                        expand.startAnimation(ranim);
                    }

                    ExpansionAnimation expandAni = new ExpansionAnimation(background, 450);
                    background.startAnimation(expandAni);
                }
            });
        } else {
            expand.setVisibility(View.GONE);
        }

        nametv.setText(name);
        screennametv.setText("@" + screenName);
        tweettv.setText(tweet);
        //Date tweetDate = new Date(time);
        String timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM).format(time) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(time);
        timetv.setText(timeDisplay);

        tweettv.setLinksClickable(true);

        if (retweeter.length() > 0 ) {
            retweetertv.setText(getResources().getString(R.string.retweeter) + retweeter);
            retweetertv.setVisibility(View.VISIBLE);
            isRetweet = true;
        }

        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FavoriteStatus(favoriteCount, favoriteButton, tweetId).execute();
            }
        });

        retweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new RetweetStatus(retweetCount, tweetId).execute();
            }
        });

        new GetFavoriteCount(favoriteCount, favoriteButton, tweetId).execute();
        new GetRetweetCount(retweetCount, tweetId).execute();
        new GetReplies(replyList, screenName, tweetId, progressSpinner, expand, background).execute();

        String text = tweet;
        String extraNames = "";

        if (text.contains("@")) {
            String[] split = text.split(" ");

            for (String s : split) {
                if (s.endsWith(":")) {
                    s = s.substring(0, s.length() - 1);
                }

                if (s.contains("@") && !s.contains(settings.myScreenName) && !s.contains(screenName) && s.length() > 1) {
                    extraNames += s.substring(s.indexOf("@")) + " ";
                }
            }
        }

        reply.setText("@" + screenName + " " + extraNames);

        reply.setSelection(reply.getText().length());

        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ReplyToStatus(reply, tweetId).execute();
            }
        });

        attachImage = (ImageView) findViewById(R.id.attach);

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (attachedFilePath.equals("")) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                } else {
                    attachedFilePath = "";

                    attachImage.setVisibility(View.GONE);
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                }
            }
        });

    }

    private boolean isFavorited = false;
    private boolean isRetweet = false;

    class GetFavoriteCount extends AsyncTask<String, Void, Status> {

        private long tweetId;
        private TextView favs;
        private ImageButton favButton;

        public GetFavoriteCount(TextView favs, ImageButton favButton, long tweetId) {
            this.tweetId = tweetId;
            this.favButton = favButton;
            this.favs = favs;
        }

        protected twitter4j.Status doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                if (isRetweet) {
                    twitter4j.Status retweeted = twitter.showStatus(tweetId).getRetweetedStatus();
                    return retweeted;
                }
                return twitter.showStatus(tweetId);
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(twitter4j.Status status) {
            if (status != null) {
                favs.setText("- " + status.getFavoriteCount());

                if (status.isFavorited()) {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.favoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    favButton.setImageDrawable(context.getResources().getDrawable(resource));
                    isFavorited = true;
                } else {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.notFavoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    favButton.setImageDrawable(context.getResources().getDrawable(resource));
                    isFavorited = false;
                }
            }
        }
    }

    class GetRetweetCount extends AsyncTask<String, Void, String> {

        private long tweetId;
        private TextView retweetCount;
        private String via = "";

        public GetRetweetCount(TextView retweetCount, long tweetId) {
            this.retweetCount = retweetCount;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                twitter4j.Status status = twitter.showStatus(tweetId);

                via = android.text.Html.fromHtml(status.getSource()).toString();

                return "" + status.getRetweetCount();
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            if (count != null) {
                retweetCount.setText("- " + count);
            }

            try {
                timetv.append(" via " + via);
            } catch (Exception e) {

            }
        }
    }

    class FavoriteStatus extends AsyncTask<String, Void, String> {

        private long tweetId;
        private TextView favs;
        private ImageButton favButton;

        public FavoriteStatus(TextView favs, ImageButton favButton, long tweetId) {
            this.tweetId = tweetId;
            this.favButton = favButton;
            this.favs = favs;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                if (isFavorited) {
                    twitter.destroyFavorite(tweetId);
                } else {
                    twitter.createFavorite(tweetId);
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            new GetFavoriteCount(favs, favButton, tweetId).execute();
        }
    }

    class RetweetStatus extends AsyncTask<String, Void, String> {

        private long tweetId;
        private TextView retweetCount;

        public RetweetStatus(TextView retweetCount, long tweetId) {
            this.retweetCount = retweetCount;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                twitter.retweetStatus(tweetId);
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            new GetRetweetCount(retweetCount, tweetId).execute();
        }
    }

    class ReplyToStatus extends AsyncTask<String, Void, String> {

        private long tweetId;
        private EditText message;

        public ReplyToStatus(EditText message, long tweetId) {
            this.message = message;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(message.getText().toString());
                reply.setInReplyToStatusId(tweetId);

                if (!attachedFilePath.equals("")) {
                    reply.setMedia(new File(attachedFilePath));
                }

                twitter.updateStatus(reply);


                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            finish();
        }
    }

    class GetReplies extends AsyncTask<String, Void, ArrayList<Status>> {

        private String username;
        private ListView listView;
        private long tweetId;
        private LinearLayout progressSpinner;
        private LinearLayout background;
        private ImageButton expand;

        public GetReplies(ListView listView, String username, long tweetId, LinearLayout progressBar, ImageButton expand, LinearLayout background) {
            this.listView = listView;
            this.username = username;
            this.tweetId = tweetId;
            this.progressSpinner = progressBar;
            this.expand = expand;
            this.background = background;
        }

        protected ArrayList<twitter4j.Status> doInBackground(String... urls) {
            Twitter twitter = Utils.getTwitter(context);
            try {
                twitter4j.Status status = twitter.showStatus(tweetId);

                twitter4j.Status replyStatus = twitter.showStatus(status.getInReplyToStatusId());

                ArrayList<twitter4j.Status> replies = new ArrayList<twitter4j.Status>();

                try {
                    while(!replyStatus.getText().equals("")) {
                        replies.add(replyStatus);
                        Log.v("reply_status", replyStatus.getText());

                        replyStatus = twitter.showStatus(replyStatus.getInReplyToStatusId());
                    }
                } catch (Exception e) {
                    // the list of replies has ended, but we dont want to go to null
                }

                return replies;

            } catch (TwitterException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<twitter4j.Status> replies) {
            progressSpinner.setVisibility(View.GONE);

            try {
                if (replies.size() > 0) {
                    listView.setAdapter(new TimelineArrayAdapter(context, replies));
                    expand.setVisibility(View.VISIBLE);
                    expand.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(background.getVisibility() == View.VISIBLE) {
                                Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate);
                                ranim.setFillAfter(true);
                                expand.startAnimation(ranim);
                            } else {
                                Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate_back);
                                ranim.setFillAfter(true);
                                expand.startAnimation(ranim);
                            }

                            ExpansionAnimation expandAni = new ExpansionAnimation(background, 450);
                            background.startAnimation(expandAni);
                        }
                    });
                } else {

                }
            } catch (Exception e) {
                // none and it got the null object
            }

            if(!picture && webpage == null) {
                listView.setVisibility(View.VISIBLE);
            }
        }
    }

    class DeleteTweet extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = Utils.getTwitter(context);

            try {
                twitter.destroyStatus(tweetId);

                HomeDataSource source = new HomeDataSource(context);
                source.open();
                source.deleteTweet(tweetId);
                source.close();

                return true;
            } catch (TwitterException e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean deleted) {
            if (deleted) {
                Toast.makeText(context, getResources().getString(R.string.deleted_tweet), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
            }

            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.tweet_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        final int MENU_DELETE_TWEET = 0;
        final int MENU_SHARE = 1;
        final int MENU_COPY_TEXT = 2;
        final int MENU_OPEN_WEB = 3;
        final int MENU_SAVE_IMAGE = 4;

        if (!isMyTweet) {
            menu.getItem(MENU_DELETE_TWEET).setVisible(false);
        }

        if (website.getVisibility() != View.VISIBLE) {
            menu.getItem(MENU_OPEN_WEB).setVisible(false);
        }

        if (pictureIv.getVisibility() != View.VISIBLE) {
            menu.getItem(MENU_SAVE_IMAGE).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_delete_tweet:
                new DeleteTweet().execute();
                return true;

            case R.id.menu_share:
                String message = tweet;
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, message);

                startActivity(Intent.createChooser(share, getResources().getString(R.string.menu_share)));
                return true;

            case R.id.menu_copy_text:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("tweet_text", tweet);
                clipboard.setPrimaryClip(clip);
                return true;

            case R.id.menu_open_web:
                Uri weburi = Uri.parse(webpage);
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                startActivity(launchBrowser);

                return true;

            case R.id.menu_save_image:

                Bitmap bitmap = ((BitmapDrawable)pictureIv.getDrawable()).getBitmap();

                Random generator = new Random();
                int n = 10000;
                n = generator.nextInt(n);
                String fname = "Image-" + n;

                IOUtils.saveImage(bitmap, fname, context);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static final int SELECT_PHOTO = 100;

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String filePath = IOUtils.getPath(selectedImage, context);

                    Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

                    attachImage.setImageBitmap(yourSelectedImage);
                    attachImage.setVisibility(View.VISIBLE);

                    attachedFilePath = filePath;
                }
        }
    }
}
