package com.klinker.android.twitter_l.activities.profile_viewer;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.WhiteToolbarActivity;
import com.klinker.android.twitter_l.activities.compose.ComposeActivity;
import com.klinker.android.twitter_l.activities.compose.ComposeDMActivity;
import com.klinker.android.twitter_l.activities.media_viewer.image.ImageViewerActivity;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.ThemeColor;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.text.TextUtils;
import com.klinker.android.twitter_l.views.TweetView;
import com.klinker.android.twitter_l.views.popups.profile.PicturesPopup;
import com.klinker.android.twitter_l.views.popups.profile.ProfileFollowersPopup;
import com.klinker.android.twitter_l.views.popups.profile.ProfileFriendsPopup;
import com.klinker.android.twitter_l.views.popups.profile.ProfileTimelinePopupLayout;
import com.klinker.android.twitter_l.views.popups.profile.ProfileTweetsPopup;
import com.klinker.android.twitter_l.views.popups.profile.ProfileUsersListsPopup;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefEditText;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefTextView;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fisk.chipcloud.ChipCloud;
import fisk.chipcloud.ChipCloudConfig;
import fisk.chipcloud.ChipListener;
import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.UserList;
import xyz.klinker.android.drag_dismiss.DragDismissIntentBuilder;
import xyz.klinker.android.drag_dismiss.delegate.DragDismissDelegate;

public class ProfilePager extends WhiteToolbarActivity implements DragDismissDelegate.Callback {

    private static final int NUM_TWEETS_ON_TIMELINE = 15;
    private static final int LOAD_CAPACITY_PER_LIST = 20;

    public static void start(Context context, User user) {
        try {
            start(context, user.getName(), user.getScreenName(), user.getOriginalProfileImageURL());
        } catch (StringIndexOutOfBoundsException e) {
            start(context, user.getName(), user.getScreenName(), user.getProfileImageURL());
        }
    }

    public static void start(Context context, String screenname) {
        start(context, null, screenname, null);
    }

    public static void start(Context context, String name, String screenName, String profilePic) {
        Intent intent = new Intent(context, ProfilePager.class);

        DragDismissIntentBuilder.Theme theme = DragDismissIntentBuilder.Theme.LIGHT;
        AppSettings settings = AppSettings.getInstance(context);
        if (settings.blackTheme) {
            theme = DragDismissIntentBuilder.Theme.BLACK;
        } else if (settings.darkTheme) {
            theme = DragDismissIntentBuilder.Theme.DARK;
        }

        new DragDismissIntentBuilder(context)
                .setPrimaryColorValue(settings.themeColors.primaryColor)
                .setDragElasticity(DragDismissIntentBuilder.DragElasticity.XLARGE)
                .setShouldScrollToolbar(true)
                .setToolbarTitle(name)
                .setShowToolbar(true)
                .setTheme(theme)
                .setDrawUnderStatusBar(true)
                .build(intent);

        intent.putExtra("name", name);
        intent.putExtra("screenname", screenName);
        intent.putExtra("proPic", profilePic);

        context.startActivity(intent);
    }

    private Context context;
    private AppSettings settings;
    private SharedPreferences sharedPrefs;

    public ImageView followButton;
    public ImageView profilePic;
    public TextView followerCount;
    public TextView followingCount;
    public FontPrefTextView description;
    public FontPrefTextView location;
    public FontPrefTextView website;
    public View profileButtons;
    public LinearLayout chipLayout;
    public LinearLayout timelineContent;

    private PicturesPopup picsPopup;
    private ProfileFollowersPopup fol;
    private ProfileFriendsPopup fri;
    private ProfileUsersListsPopup usersListsPopup;
    public ProfileTweetsPopup tweetsPopup;
    public ProfileTimelinePopupLayout timelinePopup;

    // start with tweets, replies, retweets as checked. Likes and mentions as not checked.
    public boolean[] chipSelectedState = new boolean[] {true, true, true, false, false};
    public ChipCloud chipCloud;

    private String screenName;
    private String proPic;

    private boolean isMyProfile = false;
    private boolean isBlocking;
    private boolean isFollowing;
    private boolean followingYou;
    private boolean isFavorite;
    private boolean isMuted;
    private boolean isRTMuted;
    private boolean isMuffled;
    private boolean isFollowingSet = false;

    public List<Status> tweets = new ArrayList<>();
    public Paging tweetsPaging = new Paging(1, LOAD_CAPACITY_PER_LIST);

    public List<Status> favorites = new ArrayList<>();
    public Paging favoritesPaging = new Paging(1, LOAD_CAPACITY_PER_LIST);
    
    public List<Status> mentions = new ArrayList<>();
    public Query mentionsQuery = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        neverUseLightStatusBar = true;

        DragDismissDelegate delegate = new DragDismissDelegate(this, this);
        delegate.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.activity_slide_up, 0);
    }

    @Override
    public View onCreateContent(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        Utils.setTaskDescription(this);
        Utils.setSharedContentTransition(this);

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        Utils.setUpProfileTheme(context, settings);
        getFromIntent();
        View root = inflater.inflate(R.layout.user_profile, parent, false);

        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        findViewById(R.id.dragdismiss_content).setBackgroundResource(resource);

        setUpContent(root);
        getUser();

        return root;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }

    public void setUpContent(View root) {
        profilePic = (ImageView) root.findViewById(R.id.profile_pic);

        followerCount = (TextView) root.findViewById(R.id.followers_number);
        followingCount = (TextView) root.findViewById(R.id.following_number);
        description = (FontPrefTextView) root.findViewById(R.id.user_description);
        location = (FontPrefTextView) root.findViewById(R.id.user_location);
        website = (FontPrefTextView) root.findViewById(R.id.user_webpage);

        profileButtons = root.findViewById(R.id.profile_buttons);
        chipLayout = (LinearLayout) root.findViewById(R.id.chip_layout);

        ChipCloudConfig config = new ChipCloudConfig()
                .selectMode(ChipCloud.SelectMode.multi)
                .checkedChipColor(settings.themeColors.primaryColor)
                .checkedTextColor(Color.WHITE)
                .uncheckedChipColor(settings.darkTheme ? getResources().getColor(R.color.dark_background) :
                        getResources().getColor(R.color.light_background))
                .uncheckedTextColor(settings.darkTheme ? getResources().getColor(R.color.dark_text) :
                        getResources().getColor(R.color.light_text))
                .useInsetPadding(true);

        if (settings.blackTheme && settings.themeColors.primaryColor == Color.BLACK) {
            config.checkedChipColor(settings.themeColors.accentColor);
        } else if (settings.darkTheme && settings.theme == AppSettings.THEME_DARK_BACKGROUND_COLOR) {
            config.checkedChipColor(settings.themeColors.accentColor);
        } else if (settings.theme == AppSettings.THEME_WHITE) {
            config.checkedChipColor(settings.themeColors.accentColor);
        }

        chipCloud = new ChipCloud(this, chipLayout, config);
        chipCloud.setListener(new ChipListener() {
            @Override
            public void chipCheckedChange(int index, boolean checked, boolean userClicked) {
                if (userClicked) {
                    chipSelectedState[index] = checked;
                    addTweetsToLayout(filterTweets());
                }
            }
        }, true);

        loadProfilePicture();
    }

    public void loadProfilePicture() {
        try {
            Glide.with(this).load(proPic)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(profilePic);

            profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (thisUser != null) {
                        ImageViewerActivity.Companion.startActivity(context, proPic, thisUser.getProfileBannerURL());
                    } else {
                        ImageViewerActivity.Companion.startActivity(context, proPic);
                    }
                }
            });

            findViewById(R.id.banner).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (thisUser != null) {
                        ImageViewerActivity.Companion.startActivity(context, proPic, thisUser.getProfileBannerURL());
                    } else {
                        ImageViewerActivity.Companion.startActivity(context, proPic);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getFromIntent() {
        Intent from = getIntent();

        screenName = from.getStringExtra("screenname");
        proPic = from.getStringExtra("proPic");

        if (screenName != null && screenName.equalsIgnoreCase(settings.myScreenName)) {
            isMyProfile = true;
        }
    }

    public void showProfileContent(final User user) {
        if (android.text.TextUtils.isEmpty(proPic)) {
            proPic = user.getOriginalProfileImageURL();
            loadProfilePicture();
        }

        CoordinatorLayout frameLayout = (CoordinatorLayout)
                findViewById(R.id.dragdismiss_background_view);

        FloatingActionButton fab = new FloatingActionButton(this);

        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                Utils.toDP(56, context), Utils.toDP(56, context));
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.bottomMargin = Utils.toDP(16, context);
        params.rightMargin = Utils.toDP(16, context);
        params.leftMargin = Utils.toDP(16, context);
        fab.setLayoutParams(params);

        fab.setImageResource(R.drawable.ic_fab_pencil);
        fab.setBackgroundTintList(ColorStateList.valueOf(settings.themeColors.accentColor));

        frameLayout.addView(fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent compose = new Intent(ProfilePager.this, ComposeActivity.class);
                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                        v.getMeasuredWidth(), v.getMeasuredHeight());
                compose.putExtra("user", "@" + screenName);
                compose.putExtra("already_animated", true);
                startActivity(compose, opts.toBundle());
            }
        });

        String des = user.getDescription();
        String loc = user.getLocation();
        String web = user.getURL();

        if (des != null && !des.equals("")) {
            description.setText(des);
        } else {
            description.setVisibility(View.GONE);
        }
        if (loc != null && !loc.equals("")) {
            location.setVisibility(View.VISIBLE);
            location.setText(loc);
        } else {
            location.setVisibility(View.GONE);
        }
        if (web != null && !web.equals("")) {
            website.setVisibility(View.VISIBLE);
            website.setText(user.getURLEntity().getDisplayURL());
            TextUtils.linkifyText(context, website, null, true, user.getURLEntity().getExpandedURL(), false);

            if (location.getVisibility() == View.GONE) {
                website.setPadding(0, Utils.toDP(16, context), 0, 0);
            }
        } else {
            website.setVisibility(View.GONE);
        }

        TextUtils.linkifyText(context, description, null, true, "", false);

        TextView followingStatus = (TextView) findViewById(R.id.follow_status);
        followButton = (ImageView) findViewById(R.id.follow_button);

        if (isFollowing) {
            followButton.setImageResource(R.drawable.ic_unfollow);
        } else {
            followButton.setImageResource(R.drawable.ic_follow);
        }

        if (isFollowing || !settings.crossAccActions) {
            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(context)
                            .setMessage(isFollowing ? R.string.are_you_sure_unfollow : R.string.are_you_sure_follow)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new FollowUser(TYPE_ACC_ONE).execute();
                                }
                            }).show();
                }
            });
        } else if (settings.crossAccActions) {
            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // dialog for favoriting
                    String[] options = new String[2];
//                    String[] options = new String[3];

                    options[0] = "@" + settings.myScreenName;
                    options[1] = "@" + settings.secondScreenName;
//                    options[2] = context.getString(R.string.both_accounts);

                    new android.app.AlertDialog.Builder(context)
                            .setItems(options, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int item) {
                                    new FollowUser(item + 1).execute();
                                }
                            })
                            .create().show();
                }
            });
        }

        if (followingYou) {
            followingStatus.setText(Html.fromHtml("<b>" + getString(R.string.follows_you) + "<b>"));
        } else {
            followingStatus.setText(Html.fromHtml("<b>" + getString(R.string.not_following_you) + "<b>"));
        }

        if (user.getScreenName().equals(settings.myScreenName)) {
            findViewById(R.id.follow_button).setVisibility(View.GONE);
            findViewById(R.id.follow_status).setVisibility(View.GONE);
        }

        View pictures = findViewById(R.id.media_button);
        View lists = findViewById(R.id.lists_button);

        usersListsPopup = new ProfileUsersListsPopup(context, thisUser);
        lists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usersListsPopup.setExpansionPointForAnim(view);
                usersListsPopup.show();
            }
        });

        picsPopup = new PicturesPopup(context, thisUser);
        pictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                picsPopup.setExpansionPointForAnim(view);
                picsPopup.show();
            }
        });

        if (user.getFriendsCount() < 1000) {
            followingCount.setText("" + user.getFriendsCount());
        } else {
            followingCount.setText("" + Utils.coolFormat(user.getFriendsCount(), 0));
        }

        if (user.getFollowersCount() < 1000) {
            followerCount.setText("" + user.getFollowersCount());
        } else {
            followerCount.setText("" + Utils.coolFormat(user.getFollowersCount(),0));
        }

        ImageView verified = (ImageView) findViewById(R.id.verified);
        if (settings.darkTheme) {
            verified.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        } else {
            verified.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
        }

        if (user.isVerified()) {
            verified.setVisibility(View.VISIBLE);
        }

        View openFollowers = findViewById(R.id.followers_button);
        openFollowers.setVisibility(View.VISIBLE);
        TextView followersText = (TextView) findViewById(R.id.followers_text);
        followersText.setText(Html.fromHtml("<b>" + followersText.getText().toString() + "</b>"));

        fol = new ProfileFollowersPopup(context, user);

        openFollowers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fol.setExpansionPointForAnim(view);
                fol.setOnTopOfView(view);
                fol.show();
            }
        });
        openFollowers.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ProfilePager.this,
                        getString(R.string.followers) + ": " + user.getFollowersCount(),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        View openFriends = findViewById(R.id.following_button);
        openFriends.setVisibility(View.VISIBLE);
        TextView followingText = (TextView) findViewById(R.id.following_text);
        followingText.setText(Html.fromHtml("<b>" + followingText.getText().toString() + "</b>"));

        fri = new ProfileFriendsPopup(context, user);

        openFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fri.setExpansionPointForAnim(view);
                fri.setOnTopOfView(view);
                fri.show();
            }
        });
        openFriends.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ProfilePager.this,
                        getString(R.string.following) + ": " + user.getFriendsCount(),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        animateIn(profileButtons);
    }

    private void setLongClickChipListener(final int index) {
        chipLayout.getChildAt(index).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                for (int i = 0; i < chipLayout.getChildCount(); i++) {
                    if (i == index) {
                        chipCloud.setChecked(i);
                        chipSelectedState[i] = true;
                    } else {
                        chipCloud.deselectIndex(i);
                        chipSelectedState[i] = false;
                    }

                    chipLayout.getChildAt(i).setEnabled(false);
                }

                chipLayout.setEnabled(false);
                chipLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < chipLayout.getChildCount(); i++) {
                            chipLayout.getChildAt(i).setEnabled(true);
                        }
                    }
                }, 2000);

                addTweetsToLayout(filterTweets());
                return false;
            }
        });
    }

    private void prepareTweetsLayout() {
        chipCloud.addChip(getString(R.string.tweets));
        chipCloud.addChip(getString(R.string.replies));
        chipCloud.addChip(getString(R.string.retweets));
        chipCloud.setSelectedIndexes(new int[] {0,1,2});

        for (int i = 0; i < 3; i++) {
            setLongClickChipListener(i);
        }


        timelineContent = (LinearLayout) findViewById(R.id.tweets_content);
        TextView tweetsTitle = (TextView) findViewById(R.id.tweets_title_text);
        Button showAllTweets = (Button) findViewById(R.id.show_all_tweets_button);


        if (tweetsTitle == null) {
            return;
        }

        final View tweetsLayout = getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);
        tweetsPopup = new ProfileTweetsPopup(context, tweetsLayout, thisUser);

        showAllTweets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tweetsPopup.setExpansionPointForAnim(view);
                tweetsPopup.show();
            }
        });
        showAllTweets.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ProfilePager.this,
                        getString(R.string.tweets) + ": " + thisUser.getStatusesCount(),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        if (thisUser.getStatusesCount() < 1000) {
            showAllTweets.setText(getString(R.string.show_all_tweets) + " (" + thisUser.getStatusesCount() + ")");
        } else {
            showAllTweets.setText(getString(R.string.show_all_tweets) + " (" + Utils.coolFormat(thisUser.getStatusesCount(), 0) + ")");
        }

        final View timelineLayout = getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);
        timelinePopup = new ProfileTimelinePopupLayout(this, timelineLayout, thisUser);

        View showAll = findViewById(R.id.show_all);
        showAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timelinePopup.setExpansionPointForAnim(view);
                timelinePopup.show();
            }
        });

        if (settings.darkTheme && settings.theme == AppSettings.THEME_DARK_BACKGROUND_COLOR) {
            ((TextView) showAll.findViewById(R.id.show_all_text))
                    .setTextColor(AppSettings.getInstance(context).themeColors.accentColor);
        } else if (!settings.darkTheme && settings.theme == AppSettings.THEME_WHITE) {
            ((TextView) showAll.findViewById(R.id.show_all_text))
                    .setTextColor(AppSettings.getInstance(context).themeColors.accentColor);
        }

        addTweetsToLayout(tweets);
    }

    private void addTweetsToLayout(List<Status> statuses) {
        boolean addShowAll = false;
        int size = 0;
        if (statuses.size() >= NUM_TWEETS_ON_TIMELINE) {
            size = NUM_TWEETS_ON_TIMELINE;

            if (statuses.size() > NUM_TWEETS_ON_TIMELINE) {
                addShowAll = true;
            }
        } else {
            size = statuses.size();
        }

        timelineContent.removeAllViews();

        if (size > 0) {
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    View tweetDivider = new View(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context));
                    tweetDivider.setLayoutParams(params);

                    if (settings.darkTheme) {
                        tweetDivider.setBackgroundColor(getResources().getColor(R.color.dark_text_drawer));
                    } else {
                        tweetDivider.setBackgroundColor(getResources().getColor(R.color.light_text_drawer));
                    }

                    timelineContent.addView(tweetDivider);
                }

                TweetView t = new TweetView(context, statuses.get(i));
                t.setCurrentUser(thisUser.getScreenName());
                t.setSmallImage(true);
                timelineContent.addView(t.getView());
            }
        }

        View showAll = findViewById(R.id.show_all);

        if (addShowAll) {
            showAll.setVisibility(View.VISIBLE);
            showAll.getLayoutParams().height = Utils.toDP(112, this);
        } else {
            showAll.setVisibility(View.INVISIBLE);
            showAll.getLayoutParams().height = Utils.toDP(16, this);
        }

        showAll.requestLayout();

        animateIn(timelineContent);
    }

    private void animateIn(final View v) {
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }

        ValueAnimator alpha = ValueAnimator.ofFloat(0f, 1f);
        alpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (Float) valueAnimator.getAnimatedValue();
                v.setAlpha(val);
            }
        });
        alpha.setDuration(200);
        alpha.setInterpolator(TimeLineCursorAdapter.ANIMATION_INTERPOLATOR);
        alpha.start();
    }

    public User thisUser;

    public void getUser() {
        TimeoutThread getUser = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Twitter twitter =  Utils.getTwitter(context, settings);

                try {
                    thisUser = twitter.showUser(screenName);
                    mentionsQuery = new Query("@" + screenName + " -RT");
                } catch (Exception e) {
                    thisUser = null;
                }

                if (thisUser == null) {
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                try {
                    FollowersDataSource.getInstance(context).createUser(thisUser, sharedPrefs.getInt("current_account", 1));

                } catch (Exception e) {
                    // the user already exists. don't know if this is more efficient than querying the db or not.
                }

                if (thisUser != null) {
                    final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context,
                            MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
                    suggestions.saveRecentQuery("@" + thisUser.getScreenName(), null);
                }

                // set the info to set up the action bar items
                if (isMyProfile) {
                    if (thisUser != null) {
                        // put in the banner and profile pic to shared prefs
                        sharedPrefs.edit().putString("profile_pic_url_" + settings.currentAccount, thisUser.getOriginalProfileImageURL()).apply();
                        sharedPrefs.edit().putString("twitter_background_url_" + settings.currentAccount, thisUser.getProfileBannerURL()).apply();
                        isMuffled = sharedPrefs.getStringSet("muffled_users", new HashSet<String>()).contains(screenName);
                        isMuted = sharedPrefs.getString("muted_users", "").contains(screenName);
                        isRTMuted = sharedPrefs.getString("muted_rts", "").contains(screenName);
                    }
                } else {
                    try {

                        String otherUserName = screenName;
                        Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);

                        isFollowing = friendship.isSourceFollowingTarget();
                        followingYou = friendship.isTargetFollowingSource();
                        isBlocking = friendship.isSourceBlockingTarget();
                        isMuted = sharedPrefs.getString("muted_users", "").contains(screenName);
                        isRTMuted = sharedPrefs.getString("muted_rts", "").contains(screenName);
                        isMuffled = sharedPrefs.getStringSet("muffled_users", new HashSet<String>()).contains(screenName);
                        isFavorite = FavoriteUsersDataSource.getInstance(context).isFavUser(otherUserName);

                        isFollowingSet = true;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (thisUser != null) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ActionBar actionBar = getSupportActionBar();
                            if (actionBar != null) {
                                actionBar.setTitle(thisUser.getName());
                                //actionBar.setSubtitle("@" + thisUser.getScreenName());
                            }

                            invalidateOptionsMenu();
                            showProfileContent(thisUser);

                            try {
                                Glide.with(context)
                                        .load(thisUser.getProfileBannerURL())
                                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                        .centerCrop()
                                        .into((ImageView) findViewById(R.id.banner));
                            } catch (Exception e) {

                            }
                        }
                    });
                }

                try {
                    fetchTweets(twitter);
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            prepareTweetsLayout();
                        }
                    });

                    fetchFavorites(twitter);
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chipCloud.addChip(getString(R.string.favorites));
                            setLongClickChipListener(3);
                        }
                    });
                    
                    fetchMentions(twitter);
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chipCloud.addChip(getString(R.string.mentions));
                            setLongClickChipListener(4);
                        }
                    });

                    fetchTweets(twitter);
                } catch (Exception e) {
                    if (thisUser != null && thisUser.isProtected()) {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, getString(R.string.protected_account), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, getString(R.string.rate_limit_reached), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

            }
        });

        getUser.setPriority(Thread.MAX_PRIORITY);
        getUser.start();
    }

    public List<Status> filterTweets() {
        final int tweetsIndex = 0;
        final int repliesIndex = 1;
        final int retweetsIndex = 2;
        final int likesIndex = 3;
        final int mentionsIndex = 4;

        List<Status> filteredStatuses = new ArrayList<>();

        for (Status status : tweets) {
            if (chipSelectedState[tweetsIndex] && !status.isRetweet() && !status.getText().startsWith("@")) {
                filteredStatuses.add(status);
            } else if (chipSelectedState[retweetsIndex] && status.isRetweet()) {
                filteredStatuses.add(status);
            } else if (chipSelectedState[repliesIndex] && status.getText().startsWith("@")) {
                filteredStatuses.add(status);
            }
        }
        
        if (chipSelectedState[likesIndex]) {
            filteredStatuses.addAll(favorites);
        }

        if (chipSelectedState[mentionsIndex]) {
            filteredStatuses.addAll(mentions);
        }

        Collections.sort(filteredStatuses, new Comparator<Status>() {
            public int compare(Status result1, Status result2) {
                return result2.getCreatedAt().compareTo(result1.getCreatedAt());
            }
        });

        return filteredStatuses;
    }

    public boolean fetchTweets(Twitter twitter) throws TwitterException {
        if (tweetsPaging != null) {
            List<Status> statuses = twitter.getUserTimeline(thisUser.getId(), tweetsPaging);
            if (statuses.size() == LOAD_CAPACITY_PER_LIST) {
                tweetsPaging.setPage(tweetsPaging.getPage() + 1);
            } else {
                tweetsPaging = null;
            }

            tweets.addAll(statuses);
            return true;
        }

        return false;
    }

    public boolean fetchFavorites(Twitter twitter) throws TwitterException {
        if (favoritesPaging != null) {
            List<Status> statuses = twitter.getFavorites(thisUser.getId(), favoritesPaging);
            if (statuses.size() == LOAD_CAPACITY_PER_LIST) {
                favoritesPaging.setPage(favoritesPaging.getPage() + 1);
            } else {
                favoritesPaging = null;
            }

            favorites.addAll(statuses);
            return true;
        }

        return false;
    }

    public boolean fetchMentions(Twitter twitter) throws TwitterException {
        if (mentionsQuery != null) {
            QueryResult result = twitter.search(mentionsQuery);
            List<Status> statuses = result.getTweets();

            boolean hasMore = result.hasNext();
            while (hasMore && statuses.size() < LOAD_CAPACITY_PER_LIST) {
                mentionsQuery = result.nextQuery();
                result = twitter.search(mentionsQuery);
                statuses.addAll(result.getTweets());

                if (!result.hasNext()) {
                    hasMore = false;
                }
            }

            if (!hasMore) {
                mentionsQuery = null;
            }

            mentions.addAll(statuses);
            return true;
        }

        return false;
    }

    private class GetActionBarInfo extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... urls) {
            if (isMyProfile) {
                if (thisUser != null) {
                    // put in the banner and profile pic to shared prefs
                    sharedPrefs.edit().putString("profile_pic_url_" + sharedPrefs.getInt("current_account", 1), thisUser.getOriginalProfileImageURL()).apply();
                    sharedPrefs.edit().putString("twitter_background_url_" + sharedPrefs.getInt("current_account", 1), thisUser.getProfileBannerURL()).apply();
                }
                return null;
            } else {
                try {
                    int currentAccount = sharedPrefs.getInt("current_account", 1);
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    String otherUserName = screenName;
                    Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);

                    isFollowing = friendship.isSourceFollowingTarget();
                    isBlocking = friendship.isSourceBlockingTarget();
                    isMuted = sharedPrefs.getString("muted_users", "").contains(screenName);
                    isRTMuted = sharedPrefs.getString("muted_rts", "").contains(screenName);
                    isFavorite = FavoriteUsersDataSource.getInstance(context).isFavUser(otherUserName);
                    isFollowingSet = true;

                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        protected void onPostExecute(Void none) {
            if (thisUser != null) {
                //actionBar.setTitle(thisUser.getName());
            }
            invalidateOptionsMenu();
        }
    }

    private final int TYPE_ACC_ONE = 1;
    private final int TYPE_ACC_TWO = 2;
    private final int TYPE_BOTH_ACC = 3;

    private class FollowUser extends AsyncTask<String, Void, Boolean> {

        private Exception e = null;

        private int followType;

        FollowUser(int followType) {
            this.followType = followType;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                if (thisUser != null) {
                    Twitter twitter = null;
                    Twitter secTwitter = null;
                    if (followType == TYPE_ACC_ONE) {
                        twitter = Utils.getTwitter(context, settings);
                    } else if (followType == TYPE_ACC_TWO) {
                        secTwitter = Utils.getSecondTwitter(context);
                    } else {
                        twitter = Utils.getTwitter(context, settings);
                        secTwitter = Utils.getSecondTwitter(context);
                    }

                    String otherUserName = thisUser.getScreenName();
                    boolean isFollowing = false;

                    if (twitter != null) {
                        Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);
                        isFollowing = friendship.isSourceFollowingTarget();
                    }

                    if (isFollowing) {
                        if (twitter != null) {
                            twitter.destroyFriendship(otherUserName);
                        }

                        if (secTwitter != null) {
                            secTwitter.createFriendship(otherUserName);
                        }

                        return false;
                    } else {
                        if (twitter != null) {
                            twitter.createFriendship(otherUserName);
                        }

                        if (secTwitter != null) {
                            secTwitter.createFriendship(otherUserName);
                        }

                        FollowersDataSource.getInstance(context).createUser(thisUser, sharedPrefs.getInt("current_account", 1));

                        return true;
                    }
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                this.e = e;
                return null;
            }
        }

        protected void onPostExecute(Boolean created) {
            // add a toast - now following or unfollowed
            // true = followed
            // false = unfollowed
            if (created != null) {
                if (created) {
                    Toast.makeText(context, getResources().getString(R.string.followed_user), Toast.LENGTH_SHORT).show();
                    followButton.setImageResource(R.drawable.ic_unfollow);
                } else {
                    Toast.makeText(context, getResources().getString(R.string.unfollowed_user), Toast.LENGTH_SHORT).show();
                    followButton.setImageResource(R.drawable.ic_follow);
                }
            } else {
                Toast.makeText(context, getResources().getString(R.string.error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            new GetActionBarInfo().execute();
        }
    }

    private class BlockUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                if (thisUser != null) {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    String otherUserName = thisUser.getScreenName();

                    Relationship friendship = twitter.showFriendship(settings.myScreenName, otherUserName);

                    boolean isBlocking = friendship.isSourceBlockingTarget();

                    if (isBlocking) {
                        twitter.destroyBlock(otherUserName);
                        return false;
                    } else {
                        twitter.createBlock(otherUserName);
                        return true;
                    }
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Boolean isBlocked) {
            // true = followed
            // false = unfollowed
            if (isBlocked != null) {
                if (isBlocked) {
                    Toast.makeText(context, getResources().getString(R.string.blocked_user), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, getResources().getString(R.string.unblocked_user), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }

            new GetActionBarInfo().execute();
        }
    }

    private class FavoriteUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                int currentAccount = sharedPrefs.getInt("current_account", 1);
                if (thisUser != null) {
                    if (isFavorite) {
                        // destroy favorite
                        FavoriteUsersDataSource.getInstance(context).deleteUser(thisUser.getId());

                        String favs = sharedPrefs.getString("favorite_user_names_" + currentAccount, "");
                        favs = favs.replaceAll(thisUser.getScreenName() + " ", "");
                        sharedPrefs.edit().putString("favorite_user_names_" + currentAccount, favs).apply();

                        isFavorite = false;

                        return false;

                    } else {
                        FavoriteUsersDataSource.getInstance(context).createUser(thisUser, currentAccount);

                        sharedPrefs.edit().putString("favorite_user_names_" + currentAccount, sharedPrefs.getString("favorite_user_names_" + currentAccount, "") + thisUser.getScreenName() + " ").apply();

                        isFavorite = true;

                        return true;
                    }
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Boolean isFavorited) {
            new GetActionBarInfo().execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.profile_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        final int MENU_FAVORITE = 0;
        final int MENU_BLOCK = 1;
        final int MENU_UNBLOCK = 2;
        final int MENU_ADD_LIST = 3;
        final int MENU_DM = 4;
        final int MENU_CHANGE_PICTURE = 5;
        final int MENU_CHANGE_BANNER = 6;
        final int MENU_CHANGE_BIO = 7;
        final int MENU_MUTE = 8;
        final int MENU_UNMUTE = 9;
        final int MENU_MUTE_RT = 10;
        final int MENU_UNMUTE_RT = 11;
        final int MENU_MUFFLE = 12;
        final int MENU_UNMUFFLE = 13;
        final int MENU_SHARE_LINK = 14;

        if (isFavorite) {
            menu.getItem(MENU_FAVORITE).setIcon(getResources().getDrawable(R.drawable.ic_heart_light));
            menu.getItem(MENU_FAVORITE).setTitle(getString(R.string.menu_unfavorite));
            menu.getItem(MENU_FAVORITE).setVisible(true);
        } else {
            menu.getItem(MENU_FAVORITE).setIcon(getResources().getDrawable(R.drawable.ic_heart_outline));
            menu.getItem(MENU_FAVORITE).setTitle(getString(R.string.menu_favorite));
            menu.getItem(MENU_FAVORITE).setVisible(true);
        }

        if (isMyProfile) {
            menu.getItem(MENU_BLOCK).setVisible(false);
            menu.getItem(MENU_UNBLOCK).setVisible(false);
            menu.getItem(MENU_ADD_LIST).setVisible(false);
            menu.getItem(MENU_DM).setVisible(false);
        } else {
            if (isFollowingSet) {
                if (isBlocking) {
                    menu.getItem(MENU_BLOCK).setVisible(false);
                } else {
                    menu.getItem(MENU_UNBLOCK).setVisible(false);
                }
            } else {
                menu.getItem(MENU_BLOCK).setVisible(false);
                menu.getItem(MENU_UNBLOCK).setVisible(false);
                menu.getItem(MENU_MUTE).setVisible(false);
                menu.getItem(MENU_UNMUTE).setVisible(false);
                menu.getItem(MENU_MUTE_RT).setVisible(false);
                menu.getItem(MENU_UNMUTE_RT).setVisible(false);
                menu.getItem(MENU_MUFFLE).setVisible(false);
                menu.getItem(MENU_UNMUFFLE).setVisible(false);
            }

            menu.getItem(MENU_CHANGE_BIO).setVisible(false);
            menu.getItem(MENU_CHANGE_BANNER).setVisible(false);
            menu.getItem(MENU_CHANGE_PICTURE).setVisible(false);
        }

        if (isFollowingSet || isMyProfile) {
            if (isMuffled) {
                menu.getItem(MENU_MUFFLE).setVisible(false);
            } else {
                menu.getItem(MENU_UNMUFFLE).setVisible(false);
            }
            if (isMuted) {
                menu.getItem(MENU_MUTE).setVisible(false);
            } else {
                menu.getItem(MENU_UNMUTE).setVisible(false);
            }
            if (isRTMuted) {
                menu.getItem(MENU_MUTE_RT).setVisible(false);
            } else {
                menu.getItem(MENU_UNMUTE_RT).setVisible(false);
            }
        } else {
            menu.getItem(MENU_MUFFLE).setVisible(false);
            menu.getItem(MENU_UNMUFFLE).setVisible(false);
            menu.getItem(MENU_MUTE).setVisible(false);
            menu.getItem(MENU_UNMUTE).setVisible(false);
            menu.getItem(MENU_MUTE_RT).setVisible(false);
            menu.getItem(MENU_UNMUTE_RT).setVisible(false);
            menu.getItem(MENU_UNMUTE_RT).setVisible(false);
        }

        return true;
    }

    @Override
    public void finish() {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        // this is used in the onStart() for the home fragment to tell whether or not it should refresh
        // tweetmarker. Since coming out of this will only call onResume(), it isn't needed.
        //sharedPrefs.edit().putBoolean("from_activity", true).apply();

        super.finish();
        overridePendingTransition(0, R.anim.activity_slide_down);

        try {
            if (isMyProfile) {
                AppSettings.invalidate();
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onBackPressed() {
        if (tweetsPopup != null && tweetsPopup.isShowing()) {
            tweetsPopup.hide();
        } else if (usersListsPopup != null && usersListsPopup.isShowing()) {
            usersListsPopup.hide();
        } else if (picsPopup != null && picsPopup.isShowing()) {
            picsPopup.hide();
        } else if (fol != null && fol.isShowing()) {
            fol.hide();
        } else if (fri != null && fri.isShowing()) {
            fri.hide();
        } else if (timelinePopup != null && timelinePopup.isShowing()) {
            timelinePopup.hide();
        } else {
            super.onBackPressed();
        }
    }

    private final int SELECT_PRO_PIC = 57;
    private final int SELECT_BANNER = 58;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_favorite:
                new FavoriteUser().execute();
                return true;

            case R.id.menu_block:
                new BlockUser().execute();
                sharedPrefs.edit().putBoolean("just_muted", true).apply();
                return true;

            case R.id.menu_unblock:
                new BlockUser().execute();
                return true;

            case R.id.menu_add_to_list:
                new GetLists().execute();
                return true;

            case R.id.menu_dm:
                Intent dm = new Intent(context, ComposeDMActivity.class);
                dm.putExtra("screenname", screenName);
                startActivity(dm);
                return true;

            case R.id.menu_change_picture:
                Intent photoPickerIntent = new Intent();
                photoPickerIntent.setType("image/*");
                photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                try {
                    startActivityForResult(Intent.createChooser(photoPickerIntent,
                            "Select Picture"), SELECT_PRO_PIC);
                } catch (Throwable t) {
                    // no app to preform this..? hmm, tell them that I guess
                    Toast.makeText(context, "No app available to select pictures!", Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.menu_change_banner:
                Intent bannerPickerIntent = new Intent();
                bannerPickerIntent.setType("image/*");
                bannerPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                try {
                    startActivityForResult(Intent.createChooser(bannerPickerIntent,
                            "Select Picture"), SELECT_BANNER);
                } catch (Throwable t) {
                    // no app to preform this..? hmm, tell them that I guess
                    Toast.makeText(context, "No app available to select pictures!", Toast.LENGTH_SHORT).show();
                }
                return true;

            case  R.id.menu_change_bio:
                updateProfile();
                return true;

            case R.id.menu_mute:
                String current = sharedPrefs.getString("muted_users", "");
                sharedPrefs.edit().putString("muted_users", current + screenName.replaceAll(" ", "").replaceAll("@", "") + " ").apply();
                sharedPrefs.edit().putBoolean("refresh_me", true).apply();
                sharedPrefs.edit().putBoolean("just_muted", true).apply();
                finish();
                return true;

            case R.id.menu_unmute:
                String muted = sharedPrefs.getString("muted_users", "");
                muted = muted.replace(screenName + " ", "");
                sharedPrefs.edit().putString("muted_users", muted).apply();
                sharedPrefs.edit().putBoolean("refresh_me", true).apply();
                sharedPrefs.edit().putBoolean("just_muted", true).apply();
                finish();
                return true;

            case R.id.menu_mute_rt:
                String muted_rts = sharedPrefs.getString("muted_rts", "");
                sharedPrefs.edit().putString("muted_rts", muted_rts + screenName.replaceAll(" ", "").replaceAll("@", "") + " ").apply();
                sharedPrefs.edit().putBoolean("refresh_me", true).apply();
                sharedPrefs.edit().putBoolean("just_muted", true).apply();
                finish();
                return true;

            case R.id.menu_unmute_rt:
                String curr_muted = sharedPrefs.getString("muted_rts", "");
                curr_muted = curr_muted.replace(screenName + " ", "");
                sharedPrefs.edit().putString("muted_rts", curr_muted).apply();
                sharedPrefs.edit().putBoolean("refresh_me", true).apply();
                sharedPrefs.edit().putBoolean("just_muted", true).apply();
                finish();
                return true;

            case R.id.menu_muffle_user:
                Set<String> muffled = sharedPrefs.getStringSet("muffled_users", new HashSet<String>());
                muffled.add(screenName);
                sharedPrefs.edit().putStringSet("muffled_users", muffled).apply();
                sharedPrefs.edit().putBoolean("refresh_me", true).apply();
                sharedPrefs.edit().putBoolean("just_muted", true).apply();
                finish();
                return true;

            case R.id.menu_unmuffle_user:
                muffled = sharedPrefs.getStringSet("muffled_users", new HashSet<String>());
                muffled.remove(screenName);
                sharedPrefs.edit().putStringSet("muffled_users", muffled).apply();
                sharedPrefs.edit().putBoolean("refresh_me", true).apply();
                sharedPrefs.edit().putBoolean("just_muted", true).apply();
                finish();
                return true;

            case R.id.menu_share_link:
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, "http://twitter.com/" + screenName.replace("@", ""));

                startActivity(Intent.createChooser(share, "Share with:"));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateProfile() {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.change_profile_info_dialog);
        dialog.setTitle(getResources().getString(R.string.change_profile_info) + ":");

        final FontPrefEditText name = (FontPrefEditText) dialog.findViewById(R.id.name);
        final FontPrefEditText url = (FontPrefEditText) dialog.findViewById(R.id.url);
        final FontPrefEditText location = (FontPrefEditText) dialog.findViewById(R.id.location);
        final FontPrefEditText description = (FontPrefEditText) dialog.findViewById(R.id.description);


        try {
            name.setText(thisUser.getName());
            url.setText(thisUser.getURLEntity().getDisplayURL());
            location.setText(thisUser.getLocation());
            description.setText(thisUser.getDescription());
        } catch (Exception e) {

        }

        Button cancel = (Button) dialog.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button change = (Button) dialog.findViewById(R.id.change);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean ok = true;
                String nameS = null;
                String urlS = null;
                String locationS = null;
                String descriptionS = null;

                if(name.getText().length() <= 20 && ok) {
                    if (name.getText().length() > 0){
                        nameS = name.getText().toString();
                        sharedPrefs.edit().putString("twitter_users_name_" + sharedPrefs.getInt("current_account", 1), nameS).apply();
                    }
                } else {
                    ok = false;
                    Toast.makeText(context, getResources().getString(R.string.name_char_length), Toast.LENGTH_SHORT).show();
                }

                if(url.getText().length() <= 100 && ok) {
                    if (url.getText().length() > 0){
                        urlS = url.getText().toString();
                    }
                } else {
                    ok = false;
                    Toast.makeText(context, getResources().getString(R.string.url_char_length), Toast.LENGTH_SHORT).show();
                }

                if(location.getText().length() <= 30 && ok) {
                    if (location.getText().length() > 0){
                        locationS = location.getText().toString();
                    }
                } else {
                    ok = false;
                    Toast.makeText(context, getResources().getString(R.string.location_char_length), Toast.LENGTH_SHORT).show();
                }

                if(description.getText().length() <= 160 && ok) {
                    if (description.getText().length() > 0){
                        descriptionS = description.getText().toString();
                    }
                } else {
                    ok = false;
                    Toast.makeText(context, getResources().getString(R.string.description_char_length), Toast.LENGTH_SHORT).show();
                }

                if (ok) {
                    new UpdateInfo(nameS, urlS, locationS, descriptionS).execute();
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    class UpdateInfo extends AsyncTask<String, Void, Boolean> {

        String name;
        String url;
        String location;
        String description;

        public UpdateInfo(String name, String url, String location, String description) {
            this.name = name;
            this.url = url;
            this.location = location;
            this.description = description;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                twitter.updateProfile(name, url, location, description);

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean added) {
            if (added) {
                Toast.makeText(context, getResources().getString(R.string.updated_profile), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean bannerUpdate = false;

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case UCrop.REQUEST_CROP:
                if(resultCode == RESULT_OK) {
                    try {
                        Uri selectedImage = UCrop.getOutput(imageReturnedIntent);

                        if (bannerUpdate) {
                            new UpdateBanner(selectedImage).execute();
                        } else {
                            new UpdateProPic(selectedImage).execute();
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    final Throwable cropError = UCrop.getError(imageReturnedIntent);
                    cropError.printStackTrace();
                }
                break;
            case SELECT_PRO_PIC:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    bannerUpdate = false;
                    startUcrop(selectedImage);
                }
                break;
            case SELECT_BANNER:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    bannerUpdate = true;
                    startUcrop(selectedImage);
                }
        }
    }

    private Bitmap getBitmapToSend(Uri uri) throws FileNotFoundException, IOException {
        InputStream input = getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > 500) ? (originalSize / 500) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true;//optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;
        input = this.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);

        ExifInterface exif = new ExifInterface(IOUtils.getPath(uri, context));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        input.close();

        return bitmap;
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    class UpdateBanner extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        private Uri image = null;
        private InputStream stream = null;

        public UpdateBanner(Uri image) {
            this.image = image;
        }

        public UpdateBanner(InputStream stream) {
            this.stream = stream;
        }

        protected void onPreExecute() {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.updating_banner_pic) + "...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                if (stream == null) {
                    //create a file to write bitmap data
                    File outputDir = context.getCacheDir(); // context being the Activity pointer
                    File f = File.createTempFile("compose", "picture", outputDir);

                    Bitmap bitmap = getBitmapToSend(image);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    byte[] bitmapdata = bos.toByteArray();

                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();

                    twitter.updateProfileBanner(f);
                } else {
                    twitter.updateProfileBanner(stream);
                }

                String profileURL = thisUser.getProfileBannerURL();
                sharedPrefs.edit().putString("twitter_background_url_" + sharedPrefs.getInt("current_account", 1), profileURL).apply();

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean uploaded) {

            try {
                stream.close();
            } catch (Exception e) {

            }

            try {
                pDialog.dismiss();
            } catch (Exception e) {

            }

            if (uploaded) {
                Toast.makeText(context, getResources().getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startUcrop(Uri sourceUri) {
        try {
            UCrop.Options options = new UCrop.Options();

            if (settings.theme == AppSettings.THEME_WHITE) {
                ThemeColor color = new ThemeColor("darkTheme", this);
                options.setToolbarColor(color.primaryColor);
                options.setStatusBarColor(color.primaryColorDark);
            } else {
                options.setToolbarColor(settings.themeColors.primaryColor);
                options.setStatusBarColor(settings.themeColors.primaryColorDark);
            }

            options.setActiveWidgetColor(settings.themeColors.accentColor);
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(90);
            options.setFreeStyleCropEnabled(true);

            File destination = File.createTempFile("ucrop", "jpg", getCacheDir());
            UCrop.of(sourceUri, Uri.fromFile(destination))
                    .withOptions(options)
                    .start(ProfilePager.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class UpdateProPic extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        private InputStream stream = null;
        private Uri image;

        public UpdateProPic(Uri image) {
            this.image = image;
        }

        protected void onPreExecute() {

            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.updating_pro_pic) + "...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                User user;

                if (stream != null) {
                    user = twitter.updateProfileImage(stream);
                } else {
                    //create a file to write bitmap data
                    File outputDir = context.getCacheDir(); // context being the Activity pointer
                    File f = File.createTempFile("compose", "picture", outputDir);

                    Bitmap bitmap = getBitmapToSend(image);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    byte[] bitmapdata = bos.toByteArray();

                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();

                    user = twitter.updateProfileImage(f);
                }


                String profileURL = user.getOriginalProfileImageURL();
                sharedPrefs.edit().putString("profile_pic_url_" + sharedPrefs.getInt("current_account", 1), profileURL).apply();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean uploaded) {

            try {
                stream.close();
            } catch (Exception e) {

            }

            try {
                pDialog.dismiss();
            } catch (Exception e) {

            }

            if (uploaded) {
                Toast.makeText(context, getResources().getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    class GetLists extends AsyncTask<String, Void, ResponseList<UserList>> {

        ProgressDialog pDialog;

        protected void onPreExecute() {

            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.finding_lists));
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected ResponseList<UserList> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                ResponseList<UserList> lists = twitter.getUserLists(settings.myScreenName);

                return lists;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(final ResponseList<UserList> lists) {

            if (lists != null) {
                Collections.sort(lists, new Comparator<UserList>() {
                    public int compare(UserList result1, UserList result2) {
                        return result1.getName().compareTo(result2.getName());
                    }
                });

                ArrayList<String> names = new ArrayList<String>();
                for(UserList l : lists) {
                    names.add(l.getName());
                }

                try {
                    pDialog.dismiss();

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setItems(names.toArray(new CharSequence[lists.size()]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (thisUser != null) {
                                new AddToList(lists.get(i).getId(), thisUser.getId()).execute();
                            }
                        }
                    });
                    builder.setTitle(getResources().getString(R.string.choose_list) + ":");
                    builder.create();
                    builder.show();

                } catch (Exception e) {
                    // closed the window
                }


            } else {
                try {
                    pDialog.dismiss();
                } catch (Exception e) {

                }
                Toast.makeText(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    class AddToList extends AsyncTask<String, Void, Boolean> {

        long listId;
        long userId;

        public AddToList(long listId, long userId) {
            this.listId = listId;
            this.userId = userId;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                twitter.createUserListMember(listId, userId);

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean added) {
            if (added) {
                Toast.makeText(context, getResources().getString(R.string.added_to_list), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
