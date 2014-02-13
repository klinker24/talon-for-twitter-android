package com.klinker.android.twitter.ui.tweet_viewer.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.manipulations.ExpansionAnimation;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.BrowserActivity;
import com.klinker.android.twitter.ui.compose.ComposeActivity;
import com.klinker.android.twitter.ui.compose.RetryCompose;
import com.klinker.android.twitter.ui.UserProfileActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.ui.drawer_activities.trends.SearchedTrendsActivity;
import com.klinker.android.twitter.ui.widgets.EmojiKeyboard;
import com.klinker.android.twitter.ui.widgets.HoloEditText;
import com.klinker.android.twitter.ui.widgets.PhotoViewerDialog;
import com.klinker.android.twitter.ui.widgets.QustomDialogBuilder;
import com.klinker.android.twitter.utils.EmojiUtils;
import com.klinker.android.twitter.utils.HtmlUtils;
import com.klinker.android.twitter.utils.IOUtils;
import com.klinker.android.twitter.utils.ImageUtils;
import com.klinker.android.twitter.utils.api_helper.TwitLongerHelper;
import com.klinker.android.twitter.utils.Utils;
import com.klinker.android.twitter.utils.api_helper.TwitPicHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.GeoLocation;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import uk.co.senab.photoview.PhotoViewAttacher;

public class TweetFragment extends Fragment {

    public AppSettings settings;
    public Context context;
    public SharedPreferences sharedPrefs;
    public View layout;

    private TextView timetv;
    private ImageView pictureIv;
    private ImageButton emojiButton;
    private EmojiKeyboard emojiKeyboard;
    private PhotoViewAttacher mAttacher;

    private ImageView attachImage;
    private String attachedFilePath = "";

    private String name;
    private String screenName;
    private String tweet;
    private long time;
    private String retweeter;
    private String webpage;
    private String proPic;
    private boolean picture;
    private long tweetId;
    private String[] users;
    private String[] hashtags;
    private String[] otherLinks;
    private boolean isMyTweet;

    private boolean addonTheme;


    public TweetFragment(AppSettings settings, String name, String screenName, String tweet, long time, String retweeter, String webpage,
                         String proPic, long tweetId, boolean picture, String[] users, String[] hashtags, String[] links,
                         boolean isMyTweet, boolean isMyRetweet) {
        this.settings = settings;

        this.name = name;
        this.screenName = screenName;
        this.tweet = tweet;
        this.time = time;
        this.retweeter = retweeter;
        this.webpage = webpage;
        this.proPic = proPic;
        this.picture = picture;
        this.tweetId = tweetId;
        this.users = users;
        this.hashtags = hashtags;
        this.isMyTweet = isMyTweet;
        this.otherLinks = links;
    }

    public TweetFragment() {
        this.settings = null;

        this.name = "";
        this.screenName = "";
        this.tweet = "";
        this.time = 0;
        this.retweeter = "";
        this.webpage = "";
        this.proPic = "";
        this.picture = false;
        this.tweetId = 0;
        this.users = null;
        this.hashtags = null;
        this.isMyTweet = false;
        this.otherLinks = null;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        layout = inflater.inflate(R.layout.tweet_fragment, null);
        addonTheme = false;

        if(settings == null) {
            settings = new AppSettings(context);
        }

        if (settings.addonTheme) {
            try {
                Context viewContext = null;
                Resources res = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);

                try {
                    viewContext = context.createPackageContext(settings.addonThemePackage, Context.CONTEXT_IGNORE_SECURITY);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (res != null && viewContext != null) {
                    int id = res.getIdentifier("tweet_fragment", "layout", settings.addonThemePackage);
                    layout = LayoutInflater.from(viewContext).inflate(res.getLayout(id), null);
                    addonTheme = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                layout = inflater.inflate(R.layout.tweet_fragment, null);
            }
        }
        setUIElements(layout);

        return layout;
    }


    public void setUIElements(final View layout) {
        TextView nametv;
        TextView screennametv;
        TextView tweettv;
        ImageButton attachButton;
        ImageButton at;
        ImageButton quote = null;
        final TextView retweetertv;
        final LinearLayout background;
        final ImageButton expand;
        final ImageView profilePic;
        final ImageButton favoriteButton;
        final ImageButton retweetButton;
        final TextView favoriteCount;
        final TextView retweetCount;
        final EditText reply;
        final ImageButton replyButton;
        final ImageButton overflow;
        final LinearLayout buttons;
        final TextView charRemaining;

        if (!addonTheme) {
            nametv = (TextView) layout.findViewById(R.id.name);
            screennametv = (TextView) layout.findViewById(R.id.screen_name);
            tweettv = (TextView) layout.findViewById(R.id.tweet);
            retweetertv = (TextView) layout.findViewById(R.id.retweeter);
            background = (LinearLayout) layout.findViewById(R.id.linLayout);
            expand = (ImageButton) layout.findViewById(R.id.expand);
            profilePic = (ImageView) layout.findViewById(R.id.profile_pic);
            favoriteButton = (ImageButton) layout.findViewById(R.id.favorite);
            quote = (ImageButton) layout.findViewById(R.id.quote_button);
            retweetButton = (ImageButton) layout.findViewById(R.id.retweet);
            favoriteCount = (TextView) layout.findViewById(R.id.fav_count);
            retweetCount = (TextView) layout.findViewById(R.id.retweet_count);
            reply = (EditText) layout.findViewById(R.id.reply);
            replyButton = (ImageButton) layout.findViewById(R.id.reply_button);
            attachButton = (ImageButton) layout.findViewById(R.id.attach_button);
            overflow = (ImageButton) layout.findViewById(R.id.overflow_button);
            buttons = (LinearLayout) layout.findViewById(R.id.buttons);
            charRemaining = (TextView) layout.findViewById(R.id.char_remaining);
            at = (ImageButton) layout.findViewById(R.id.at_button);
            emojiButton = (ImageButton) layout.findViewById(R.id.emoji);
            emojiKeyboard = (EmojiKeyboard) layout.findViewById(R.id.emojiKeyboard);
            timetv = (TextView) layout.findViewById(R.id.time);
            pictureIv = (ImageView) layout.findViewById(R.id.imageView);
            attachImage = (ImageView) layout.findViewById(R.id.attach);
        } else {
            Resources res;
            try {
                res = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
            } catch (Exception e) {
                res = null;
            }

            nametv = (TextView) layout.findViewById(res.getIdentifier("name", "id", settings.addonThemePackage));
            screennametv = (TextView) layout.findViewById(res.getIdentifier("screen_name", "id", settings.addonThemePackage));
            tweettv = (TextView) layout.findViewById(res.getIdentifier("tweet", "id", settings.addonThemePackage));
            retweetertv = (TextView) layout.findViewById(res.getIdentifier("retweeter", "id", settings.addonThemePackage));
            background = (LinearLayout) layout.findViewById(res.getIdentifier("linLayout", "id", settings.addonThemePackage));
            expand = (ImageButton) layout.findViewById(res.getIdentifier("expand", "id", settings.addonThemePackage));
            profilePic = (ImageView) layout.findViewById(res.getIdentifier("profile_pic", "id", settings.addonThemePackage));
            favoriteButton = (ImageButton) layout.findViewById(res.getIdentifier("favorite", "id", settings.addonThemePackage));
            try {
                quote = (ImageButton) layout.findViewById(res.getIdentifier("quote_button", "id", settings.addonThemePackage));
            } catch (Exception e) {
                // didn't exist when the theme was created.
            }
            retweetButton = (ImageButton) layout.findViewById(res.getIdentifier("retweet", "id", settings.addonThemePackage));
            favoriteCount = (TextView) layout.findViewById(res.getIdentifier("fav_count", "id", settings.addonThemePackage));
            retweetCount = (TextView) layout.findViewById(res.getIdentifier("retweet_count", "id", settings.addonThemePackage));
            reply = (EditText) layout.findViewById(res.getIdentifier("reply", "id", settings.addonThemePackage));
            replyButton = (ImageButton) layout.findViewById(res.getIdentifier("reply_button", "id", settings.addonThemePackage));
            attachButton = (ImageButton) layout.findViewById(res.getIdentifier("attach_button", "id", settings.addonThemePackage));
            overflow = (ImageButton) layout.findViewById(res.getIdentifier("overflow_button", "id", settings.addonThemePackage));
            buttons = (LinearLayout) layout.findViewById(res.getIdentifier("buttons", "id", settings.addonThemePackage));
            charRemaining = (TextView) layout.findViewById(res.getIdentifier("char_remaining", "id", settings.addonThemePackage));
            at = (ImageButton) layout.findViewById(res.getIdentifier("at_button", "id", settings.addonThemePackage));
            emojiButton = null;
            emojiKeyboard = null;
            timetv = (TextView) layout.findViewById(res.getIdentifier("time", "id", settings.addonThemePackage));
            pictureIv = (ImageView) layout.findViewById(res.getIdentifier("imageView", "id", settings.addonThemePackage));
            attachImage = (ImageView) layout.findViewById(res.getIdentifier("attach", "id", settings.addonThemePackage));
        }

        nametv.setTextSize(settings.textSize +2);
        screennametv.setTextSize(settings.textSize);
        tweettv.setTextSize(settings.textSize);
        timetv.setTextSize(settings.textSize - 3);
        retweetertv.setTextSize(settings.textSize - 3);
        favoriteCount.setTextSize(settings.textSize + 1);
        retweetCount.setTextSize(settings.textSize + 1);
        reply.setTextSize(settings.textSize);

        if (settings.addonTheme) {
            try {
                Resources resourceAddon = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                int back = resourceAddon.getIdentifier("reply_entry_background", "drawable", settings.addonThemePackage);
                reply.setBackgroundDrawable(resourceAddon.getDrawable(back));
            } catch (Exception e) {
                // theme does not include a reply entry box
            }
        }

        if (quote != null) {
            quote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String text = tweet;

                    text = HtmlUtils.removeColorHtml(text, settings);
                    text = restoreLinks(text);

                    if (!settings.preferRT) {
                        text = "\"@" + screenName + ": " + text + "\" ";
                    } else {
                        text = " RT @" + screenName + ": " + text;
                    }

                    Intent intent = new Intent(context, ComposeActivity.class);
                    intent.putExtra("user", text);
                    startActivity(intent);
                }
            });
        }

        overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (buttons.getVisibility() == View.VISIBLE) {

                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_out_left);
                    anim.setDuration(300);
                    buttons.startAnimation(anim);

                    buttons.setVisibility(View.GONE);
                } else {
                    buttons.setVisibility(View.VISIBLE);

                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
                    anim.setDuration(300);
                    buttons.startAnimation(anim);
                }
            }
        });

        if (settings.theme == 0 && !addonTheme) {
            nametv.setTextColor(getResources().getColor(android.R.color.black));
            nametv.setShadowLayer(0,0,0, getResources().getColor(android.R.color.transparent));
            screennametv.setTextColor(getResources().getColor(android.R.color.black));
            screennametv.setShadowLayer(0,0,0, getResources().getColor(android.R.color.transparent));
        }

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

        if(picture) { // if there is a picture already loaded

            pictureIv.setVisibility(View.VISIBLE);
            //pictureIv.loadImage(webpage, false, null);
            ImageUtils.loadImage(context, pictureIv, webpage, App.getInstance(context).getBitmapCache());

            mAttacher = new PhotoViewAttacher(pictureIv);
            mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {
                    context.startActivity(new Intent(context, PhotoViewerDialog.class).putExtra("url", webpage));
                }
            });

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
        if (settings.addonTheme) {
            tweettv.setText(Html.fromHtml(tweet.replaceAll("FF8800", settings.accentColor).replaceAll("\n", "<br/>")));
        } else {
            tweettv.setText(Html.fromHtml(tweet.replaceAll("\n", "<br/>")));
        }

        if (settings.useEmoji && (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || EmojiUtils.ios)) {
            if (EmojiUtils.emojiPattern.matcher(tweet).find()) {
                final Spannable span = EmojiUtils.getSmiledText(context, Html.fromHtml(tweet.replaceAll("\n", "<br/>")));
                tweettv.setText(span);
            }
        }

        // sets the click listener to display the dialog for the highlighted text
        if (tweet.contains("<font")) {
            tweettv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    ArrayList<String> strings = new ArrayList<String>();

                    if (users != null) {
                        for (String s : users) {
                            if (!s.equals("")) {
                                strings.add("@" + s);
                            }
                        }
                    }

                    if (hashtags != null) {
                        for (String s : hashtags) {
                            if (!s.equals("")) {
                                strings.add("#" + s);
                            }
                        }
                    }

                    if (otherLinks != null) {
                        for (String s : otherLinks) {
                            if (!s.equals("")) {
                                strings.add(s);
                            }
                        }
                    }

                    if (!webpage.equals("") && !webpage.contains("youtu") && !webpage.contains("insta") && picture) {
                        strings.add(webpage);
                    }

                    CharSequence[] items = new CharSequence[strings.size()];

                    for (int i = 0; i < items.length; i++) {
                        String s = strings.get(i);
                        if (s != null) {
                            items[i] = s;
                        }
                    }

                    final CharSequence[] fItems = items;

                    if (fItems.length > 1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                String touched = fItems[item] + "";

                                if (touched.contains("http")) { //weblink
                                    sharedPrefs.edit().putBoolean("should_refresh", false).commit();
                                    if (settings.inAppBrowser) {
                                        Intent launchBrowser = new Intent(context, BrowserActivity.class);
                                        launchBrowser.putExtra("url", touched);
                                        startActivity(launchBrowser);
                                    } else {
                                        Uri weburi = Uri.parse(touched);
                                        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                                        startActivity(launchBrowser);
                                    }
                                } else if (touched.contains("@")) { //username
                                    Intent user = new Intent(context, UserProfileActivity.class);
                                    user.putExtra("screenname", touched.replace("@", ""));
                                    user.putExtra("proPic", "");
                                    context.startActivity(user);
                                } else { // hashtag
                                    Intent search = new Intent(context, SearchedTrendsActivity.class);
                                    search.setAction(Intent.ACTION_SEARCH);
                                    search.putExtra(SearchManager.QUERY, touched);
                                    context.startActivity(search);
                                }

                                dialog.dismiss();
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        String touched = fItems[0] + "";

                        if (touched.contains("http")) { //weblink
                            if (settings.inAppBrowser) {
                                Intent launchBrowser = new Intent(context, BrowserActivity.class);
                                launchBrowser.putExtra("url", touched);
                                startActivity(launchBrowser);
                            } else {
                                Uri weburi = Uri.parse(touched);
                                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                                startActivity(launchBrowser);
                            }
                        } else if (touched.contains("@")) { //username
                            Intent user = new Intent(context, UserProfileActivity.class);
                            user.putExtra("screenname", touched.replace("@", ""));
                            user.putExtra("proPic", "");
                            context.startActivity(user);
                        } else { // hashtag
                            Intent search = new Intent(context, SearchedTrendsActivity.class);
                            search.setAction(Intent.ACTION_SEARCH);
                            search.putExtra(SearchManager.QUERY, touched);
                            context.startActivity(search);
                        }
                    }
                }
            });
        }

        //Date tweetDate = new Date(time);
        String timeDisplay;

        if (!settings.militaryTime) {
             timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(time) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(time);
        } else {
            timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(time) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN).format(time);
        }

        timetv.setText(timeDisplay);

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
                new RetweetStatus(retweetCount, tweetId, retweetButton).execute();
            }
        });

        retweetButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.remove_retweet))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new RemoveRetweet(tweetId, retweetButton).execute();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create()
                        .show();
                return false;
            }
        });

        if(settings.roundContactImages) {
            //profilePic.loadImage(proPic, false, null, NetworkedCacheableImageView.CIRCLE);
            ImageUtils.loadCircleImage(context, profilePic, proPic, App.getInstance(context).getBitmapCache());
        } else {
            //profilePic.loadImage(proPic, false, null);
            ImageUtils.loadImage(context, profilePic, proPic, App.getInstance(context).getBitmapCache());
        }

        new GetFavoriteCount(favoriteCount, favoriteButton, tweetId).execute();
        new GetRetweetCount(retweetCount, tweetId, retweetButton).execute();


        String text = tweet;
        String extraNames = "";

        if (text.contains("@")) {
            for (String s : users) {
                if (!s.equals(settings.myScreenName) && !extraNames.contains(s)  && !s.equals(screenName)) {
                    extraNames += "@" + s + " ";
                }
            }
        }

        if (!screenName.equals(settings.myScreenName)) {
            reply.setText("@" + screenName + " " + extraNames);
        } else {
            reply.setText(extraNames);
        }

        if (settings.autoInsertHashtags && hashtags != null) {
            for (String s : hashtags) {
                if (!s.equals("")) {
                    reply.append("#" + s + " ");
                }
            }
        }

        reply.setSelection(reply.getText().length());

        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (reply.getText().length() + (attachedFilePath.equals("") ? 0 : 22) <= 140 || settings.twitlonger) {
                        if (reply.getText().length() + (attachedFilePath.equals("") ? 0 : 22) > 140) {
                            new AlertDialog.Builder(context)
                                    .setTitle(context.getResources().getString(R.string.twitlonger))
                                    .setMessage(context.getResources().getString(R.string.post_with_twitlonger))
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            new ReplyToStatus(reply, tweetId, Integer.parseInt(charRemaining.getText().toString())).execute();
                                        }
                                    })
                                    .setNegativeButton(R.string.edit, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    })
                                    .create()
                                    .show();
                        } else {
                            new ReplyToStatus(reply, tweetId, Integer.parseInt(charRemaining.getText().toString())).execute();
                        }
                    } else {
                        Toast.makeText(context, getResources().getString(R.string.tweet_to_long), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                //builder.setTitle(getResources().getString(R.string.open_what) + "?");
                builder.setItems(R.array.attach_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if(item == 0) { // take picture
                            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            File f = new File(Environment.getExternalStorageDirectory() + "/Talon/", "photoToTweet.jpg");

                            if (!f.exists()) {
                                try {
                                    f.getParentFile().mkdirs();
                                    f.createNewFile();
                                } catch (IOException e) {

                                }
                            }

                            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                            startActivityForResult(captureIntent, CAPTURE_IMAGE);
                        } else { // attach picture
                            if (attachedFilePath == null || attachedFilePath.equals("")) {
                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                photoPickerIntent.setType("image/*");
                                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                            } else {
                                attachedFilePath = "";

                                TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.attachButton});
                                int resource = a.getResourceId(0, 0);
                                a.recycle();
                                attachImage.setImageDrawable(context.getResources().getDrawable(resource));
                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                photoPickerIntent.setType("image/*");
                                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                            }
                        }

                        overflow.performClick();
                    }
                });

                builder.create().show();
            }
        });

        charRemaining.setText(140 - reply.getText().length() + "");

        reply.setHint(context.getResources().getString(R.string.reply));
        String regex = "\\(?\\b(http://|www[.]|https://)[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
        final Pattern p = Pattern.compile(regex);
        reply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = reply.getText().toString();

                if (!text.contains("http")) { // no links, normal tweet
                    try {
                        charRemaining.setText(140 - reply.getText().length() - (attachedFilePath.equals("") ? 0 : 22) + "");
                    } catch (Exception e) {
                        charRemaining.setText("0");
                    }
                } else {
                    int count = text.length();
                    Matcher m = p.matcher(text);
                    while(m.find()) {
                        String url = m.group();
                        count -= url.length(); // take out the length of the url
                        count += 22; // add 22 for the shortened url
                    }

                    if (!attachedFilePath.equals("")) {
                        count += 22;
                    }

                    charRemaining.setText(140 - count + "");
                }
            }
        });


        if (!settings.useEmoji || emojiButton == null) {
            try {
                emojiButton.setVisibility(View.GONE);
            } catch (Exception e) {
                // it is a custom layout, so the emoji isn't gonna work :(
            }
        } else {
            emojiKeyboard.setAttached((HoloEditText) reply);

            reply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);

                        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_emoji_keyboard_dark));
                    }
                }
            });

            emojiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                InputMethodManager imm = (InputMethodManager)context.getSystemService(
                                        Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(reply, 0);
                            }
                        }, 250);

                        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_emoji_keyboard_dark));
                    } else {
                        InputMethodManager imm = (InputMethodManager)context.getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(reply.getWindowToken(), 0);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                emojiKeyboard.setVisibility(true);
                            }
                        }, 250);

                        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.keyboardButton});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_keyboard_light));
                    }
                }
            });
        }

        at.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final QustomDialogBuilder qustomDialogBuilder = new QustomDialogBuilder(context, sharedPrefs.getInt("current_account", 1)).
                        setTitle(getResources().getString(R.string.type_user)).
                        setTitleColor(getResources().getColor(R.color.app_color)).
                        setDividerColor(getResources().getColor(R.color.app_color));

                qustomDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                qustomDialogBuilder.setPositiveButton(getResources().getString(R.string.add_user), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        reply.append(qustomDialogBuilder.text.getText().toString());
                    }
                });

                qustomDialogBuilder.show();

                overflow.performClick();
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
                Twitter twitter =  Utils.getTwitter(context, settings);
                twitter4j.Status status = twitter.showStatus(tweetId);
                if (status.isRetweet()) {
                    twitter4j.Status retweeted = status.getRetweetedStatus();
                    return retweeted;
                }
                return twitter.showStatus(tweetId);
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(twitter4j.Status status) {
            if (status != null) {
                favs.setText(" " + status.getFavoriteCount());

                if (status.isFavorited()) {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.favoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    if (!settings.addonTheme) {
                        favButton.setColorFilter(context.getResources().getColor(R.color.app_color));
                    } else {
                        favButton.setColorFilter(settings.accentInt);
                    }

                    favButton.setImageDrawable(context.getResources().getDrawable(resource));
                    isFavorited = true;
                } else {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.notFavoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    favButton.setImageDrawable(context.getResources().getDrawable(resource));
                    isFavorited = false;

                    favButton.clearColorFilter();
                }
            }
        }
    }

    class RemoveRetweet extends AsyncTask<String, Void, Boolean> {

        private long tweetId;
        private ImageButton retweetButton;

        public RemoveRetweet(long tweetId, ImageButton retweetButton) {
            this.tweetId = tweetId;
            this.retweetButton = retweetButton;
        }

        protected void onPreExecute() {
            Toast.makeText(context, getResources().getString(R.string.removing_retweet), Toast.LENGTH_SHORT).show();
        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
                ResponseList<twitter4j.Status> retweets = twitter.getRetweets(tweetId);
                for (twitter4j.Status retweet : retweets) {
                    if(retweet.getUser().getId() == settings.myId)
                        twitter.destroyStatus(retweet.getId());
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean deleted) {

            retweetButton.clearColorFilter();

            try {
                if (deleted) {
                    Toast.makeText(context, getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // user has gone away from the window
            }
        }
    }

    class GetRetweetCount extends AsyncTask<String, Void, String> {

        private long tweetId;
        private TextView retweetCount;
        private String via = "";
        private String location = "";
        private long realTime = 0;
        private boolean retweetedByMe = false;
        private ImageButton retweetButton;

        public GetRetweetCount(TextView retweetCount, long tweetId, ImageButton retweetButton) {
            this.retweetCount = retweetCount;
            this.tweetId = tweetId;
            this.retweetButton = retweetButton;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
                twitter4j.Status status = twitter.showStatus(tweetId);

                GeoLocation loc = status.getGeoLocation();
                try {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    if (addresses.size() > 0) {
                        Address address = addresses.get(0);
                        location += address.getLocality() + ", " + address.getCountryName();
                    } else {
                        location = "";
                    }
                } catch (Exception x) {
                    location = "";
                }

                via = android.text.Html.fromHtml(status.getSource()).toString();

                if (status.isRetweet()) {
                    twitter4j.Status status2 = status.getRetweetedStatus();
                    via = android.text.Html.fromHtml(status2.getSource()).toString();
                    realTime = status2.getCreatedAt().getTime();
                }

                retweetedByMe = status.isRetweetedByMe();

                return "" + status.getRetweetCount();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(String count) {
            if (count != null) {
                retweetCount.setText(" " + count);
            }

            if (retweetedByMe) {
                if (!settings.addonTheme) {
                    retweetButton.setColorFilter(context.getResources().getColor(R.color.app_color));
                } else {
                    retweetButton.setColorFilter(settings.accentInt);
                }
            } else {
                retweetButton.clearColorFilter();
            }

            if (realTime != 0) {
                String timeDisplay;

                if (!settings.militaryTime) {
                    timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(realTime) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(realTime);
                } else {
                    timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(realTime) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN).format(realTime);
                }

                timetv.setText(timeDisplay);
            }

            try {
                if (!timetv.getText().toString().contains(getResources().getString(R.string.via))) {
                    timetv.append(" " + getResources().getString(R.string.via) + " " + via);
                }

                if (!location.equals("")) {
                    timetv.append("\n" + location);
                }
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

        protected void onPreExecute() {
            if (!isFavorited) {
                Toast.makeText(context, getResources().getString(R.string.favoriting_status), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.removing_favorite), Toast.LENGTH_SHORT).show();
            }
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
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
            try {
                Toast.makeText(context, getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                new GetFavoriteCount(favs, favButton, tweetId).execute();
            } catch (Exception e) {
                // they quit out of the activity
            }
        }
    }

    class RetweetStatus extends AsyncTask<String, Void, String> {

        private long tweetId;
        private TextView retweetCount;
        private ImageButton retweetButton;

        public RetweetStatus(TextView retweetCount, long tweetId, ImageButton retweetButton) {
            this.retweetCount = retweetCount;
            this.tweetId = tweetId;
            this.retweetButton = retweetButton;
        }

        protected void onPreExecute() {
            Toast.makeText(context, getResources().getString(R.string.retweeting_status), Toast.LENGTH_SHORT).show();
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
                twitter.retweetStatus(tweetId);
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            try {
                Toast.makeText(context, getResources().getString(R.string.retweet_success), Toast.LENGTH_SHORT).show();
                new GetRetweetCount(retweetCount, tweetId, retweetButton).execute();
            } catch (Exception e) {

            }
        }
    }

    public void removeKeyboard(EditText reply) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(reply.getWindowToken(), 0);
    }

    class ReplyToStatus extends AsyncTask<String, Void, Boolean> {

        private long tweetId;
        private String text;
        private EditText message;
        private int remainingChars;
        private boolean messageToLong = false;

        public ReplyToStatus(EditText message, long tweetId, int remainingChars) {
            this.text = message.getText().toString();
            this.message = message;
            this.tweetId = tweetId;
            this.remainingChars = remainingChars;
        }

        protected void onPreExecute() {
            removeKeyboard(message);
            Toast.makeText(context, getResources().getString(R.string.sending) + "...", Toast.LENGTH_SHORT).show();
            ((Activity)context).finish();
        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                if (remainingChars < 0) {
                    // twitlonger goes here
                    TwitLongerHelper helper = new TwitLongerHelper(text, twitter);
                    helper.setInReplyToStatusId(tweetId);

                    return helper.createPost() != 0;
                } else {
                    twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(text);
                    reply.setInReplyToStatusId(tweetId);

                    if (!attachedFilePath.equals("")) {
                        File f = new File(attachedFilePath);

                        Bitmap bitmap = decodeSampledBitmapFromResourceMemOpt(new FileInputStream(new File(attachedFilePath)), 500, 500);

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, bos);
                        byte[] bitmapdata = bos.toByteArray();

                        try {
                            //write the bytes in file
                            FileOutputStream fos = new FileOutputStream(f);
                            fos.write(bitmapdata);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // couldn't find file
                        }

                        if (!settings.twitpic) {
                            reply.setMedia(f);
                            twitter.updateStatus(reply);
                            return true;
                        } else {
                            TwitPicHelper helper = new TwitPicHelper(twitter, text, f);
                            helper.setInReplyToStatusId(tweetId);
                            return helper.createPost() != 0;
                        }
                    } else {
                        // no picture
                        twitter.updateStatus(reply);
                        return true;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean sent) {
            if (sent) {
                Toast.makeText(context, context.getResources().getString(R.string.tweet_success), Toast.LENGTH_SHORT).show();
            } else {
                makeFailedNotification(text);
            }
        }

        public Bitmap decodeSampledBitmapFromResourceMemOpt(
                InputStream inputStream, int reqWidth, int reqHeight) {

            byte[] byteArr = new byte[0];
            byte[] buffer = new byte[1024];
            int len;
            int count = 0;

            try {
                while ((len = inputStream.read(buffer)) > -1) {
                    if (len != 0) {
                        if (count + len > byteArr.length) {
                            byte[] newbuf = new byte[(count + len) * 2];
                            System.arraycopy(byteArr, 0, newbuf, 0, count);
                            byteArr = newbuf;
                        }

                        System.arraycopy(buffer, 0, byteArr, count, len);
                        count += len;
                    }
                }

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(byteArr, 0, count, options);

                options.inSampleSize = calculateInSampleSize(options, reqWidth,
                        reqHeight);
                options.inPurgeable = true;
                options.inInputShareable = true;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            } catch (Exception e) {
                e.printStackTrace();

                return null;
            }
        }

        public int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = opt.outHeight;
            final int width = opt.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > reqHeight
                        && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }

    public void makeFailedNotification(String text) {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.timeline_dark)
                            .setContentTitle(getResources().getString(R.string.tweet_failed))
                            .setContentText(getResources().getString(R.string.tap_to_retry));

            Intent resultIntent = new Intent(context, RetryCompose.class);
            resultIntent.setAction(Intent.ACTION_SEND);
            resultIntent.setType("text/plain");
            resultIntent.putExtra(Intent.EXTRA_TEXT, text);
            resultIntent.putExtra("failed_notification", true);

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            context,
                            0,
                            resultIntent,
                            0
                    );

            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(5, mBuilder.build());
        } catch (Exception e) {

        }
    }

    private static final int SELECT_PHOTO = 100;
    private static final int CAPTURE_IMAGE = 101;

    public void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == ((Activity)context).RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String filePath = IOUtils.getPath(selectedImage, context);

                    try {
                        Bitmap yourSelectedImage = decodeSampledBitmapFromResourceMemOpt(new FileInputStream(new File(filePath)), 200, 200);

                        attachImage.setImageBitmap(yourSelectedImage);
                        attachImage.setVisibility(View.VISIBLE);

                        attachedFilePath = filePath;
                    } catch (FileNotFoundException e) {

                    }
                }
                break;
            case CAPTURE_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Uri selectedImage = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Talon/", "photoToTweet.jpg"));
                        String filePath = selectedImage.getPath();
                        //String filePath = IOUtils.getPath(selectedImage, context);
                        Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

                        attachImage.setImageBitmap(yourSelectedImage);
                        attachImage.setVisibility(View.VISIBLE);

                        attachedFilePath = filePath;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                        attachImage.setVisibility(View.GONE);
                    }
                }
                break;
        }
    }

    public Bitmap decodeSampledBitmapFromResourceMemOpt(
            InputStream inputStream, int reqWidth, int reqHeight) {

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = inputStream.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth,
                    reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = opt.outHeight;
        final int width = opt.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public String restoreLinks(String text) {
        String full = text;

        String[] split = text.split(" ");

        boolean changed = false;

        if (otherLinks.length > 0) {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];

                if (s.contains("http") && s.contains("...")) { // we know the link is cut off
                    String f = s.replace("...", "").replace("http", "");

                    for (int x = 0; x < otherLinks.length; x++) {
                        Log.v("recreating_links", "other link first: " + otherLinks[x]);
                        if (otherLinks[x].contains(f)) {
                            changed = true;
                            f = otherLinks[x];
                            break;
                        }
                    }

                    if (changed) {
                        split[i] = f;
                    } else {
                        split[i] = s;
                    }
                } else {
                    split[i] = s;
                }

            }
        }

        Log.v("talon_picture", ":" + webpage + ":");

        if (!webpage.equals("")) {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];

                Log.v("talon_picture_", s);

                if (s.contains("http") && s.contains("...")) { // we know the link is cut off
                    split[i] = webpage;
                    changed = true;
                    Log.v("talon_picture", split[i]);
                }
            }
        }



        if(changed) {
            full = "";
            for (String p : split) {
                full += p + " ";
            }

            full = full.substring(0, full.length() - 1);
        }

        return full;
    }
}
