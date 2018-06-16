package com.klinker.android.twitter_l.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import com.klinker.android.twitter_l.BuildConfig;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.EmojiStyle;
import com.klinker.android.twitter_l.data.ThemeColor;
import com.klinker.android.twitter_l.utils.EmojiUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.text.EmojiInitializer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AppSettings {

    public static AppSettings settings;

    public static AppSettings getInstance(Context context) {
        if (settings == null) {
            settings = new AppSettings(context);
        }
        return settings;
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        if (context == null) {
            return null;
        }

        return context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_PRIVATE);
    }

    public static void invalidate() {
        settings = null;
    }

    public SharedPreferences sharedPrefs;

    public static final int THEME_RED = 0;
    public static final int THEME_PINK = 1;
    public static final int THEME_PURPLE = 2;
    public static final int THEME_DEEP_PURPLE = 3;
    public static final int THEME_INDIGO = 4;
    public static final int THEME_BLUE = 5;
    public static final int THEME_LIGHT_BLUE = 6;
    public static final int THEME_CYAN = 7;
    public static final int THEME_TEAL = 8;
    public static final int THEME_GREEN = 9;
    public static final int THEME_LIGHT_GREEN = 10;
    public static final int THEME_LIME = 11;
    public static final int THEME_YELLOW = 12;
    public static final int THEME_AMBER = 13;
    public static final int THEME_ORANGE = 14;
    public static final int THEME_DEEP_ORANGE = 15;
    public static final int THEME_BROWN = 16;
    public static final int THEME_GREY = 17;
    public static final int THEME_BLUE_GREY = 18;
    public static final int THEME_BLACK = 19;
    public static final int THEME_DARK_BACKGROUND_COLOR = 20;
    public static final int THEME_WHITE = 21;

    public static final int DEFAULT_THEME = THEME_LIGHT_BLUE;
    public static final int DEFAULT_MAIN_THEME = 1; // 0 = light, 1 = dark, 2 = black

    public static final int WIDGET_LIGHT = 0;
    public static final int WIDGET_DARK = 1;
    public static final int WIDGET_TRANS_LIGHT = 2;
    public static final int WIDGET_TRANS_BLACK = 3;
    public static final int WIDGET_MATERIAL_LIGHT = 4;
    public static final int WIDGET_MATERIAL_DARK = 5;

    public static final int PAGE_TWEET = 0;
    public static final int PAGE_WEB = 1;
    public static final int PAGE_CONVO = 2;

    public static final int PICTURES_NORMAL = 0;
    public static final int PICTURES_SMALL = 1;
    public static final int PICTURES_NONE = 2;
    public static final int CONDENSED_TWEETS = 3;
    public static final int CONDENSED_NO_IMAGES = 4;
    public static final int REVAMPED_TWEETS = 5;

    public static final int PAGE_TYPE_NONE = 0;
    public static final int PAGE_TYPE_PICS = 1;
    public static final int PAGE_TYPE_LINKS = 2;
    public static final int PAGE_TYPE_LIST = 3;
    public static final int PAGE_TYPE_FAV_USERS = 4;
    public static final int PAGE_TYPE_HOME = 5;
    public static final int PAGE_TYPE_MENTIONS = 6;
    public static final int PAGE_TYPE_DMS = 7;
    public static final int PAGE_TYPE_SECOND_MENTIONS = 8;
    public static final int PAGE_TYPE_WORLD_TRENDS = 9;
    public static final int PAGE_TYPE_LOCAL_TRENDS = 10;
    public static final int PAGE_TYPE_SAVED_SEARCH = 11;
    public static final int PAGE_TYPE_ACTIVITY = 12;
    public static final int PAGE_TYPE_FAVORITE_STATUS = 13;
    public static final int PAGE_TYPE_USER_TWEETS = 14;
    public static final int PAGE_TYPE_SAVED_TWEETS = 15;

    public static final int LAYOUT_TALON = 0;
    public static final int LAYOUT_HANGOUT = 1;
    public static final int LAYOUT_FULL_SCREEN = 2;

    public static final int QUOTE_STYLE_TWITTER = 0;
    public static final int QUOTE_STYLE_TALON = 1;
    public static final int QUOTE_STYLE_RT = 2;
    public static final int QUOTE_STYLE_VIA = 3;

    public static final int NAV_BAR_AUTOMATIC = 0;
    public static final int NAV_BAR_PRESENT = 1;
    public static final int NAV_BAR_NONE = 2;

    public static final int AUTOPLAY_ALWAYS = 0;
    public static final int AUTOPLAY_WIFI = 1;
    public static final int AUTOPLAY_NEVER = 2;

    public String authenticationToken;
    public String authenticationTokenSecret;
    public String secondAuthToken;
    public String secondAuthTokenSecret;
    public String myScreenName;
    public String secondScreenName;
    public String myName;
    public String secondName;
    public String myBackgroundUrl;
    public String myProfilePicUrl;
    public String secondProfilePicUrl;
    public String favoriteUserNames;
    public String locale;

    public boolean transpartSystemBars;
    public boolean darkTheme;
    public boolean blackTheme;
    public boolean isTwitterLoggedIn;
    public boolean reverseClickActions;
    public boolean advanceWindowed;
    public boolean notifications;
    public boolean interceptTwitterNotifications;
    public boolean led;
    public boolean vibrate;
    public boolean sound;
    public boolean refreshOnStart;
    public boolean autoTrim;
    public boolean uiExtras;
    public boolean wakeScreen;
    public boolean nightMode;
    public boolean militaryTime;
    public boolean syncMobile;
    public boolean useEmoji;
    public boolean inlinePics;
    public boolean extraPages;
    public boolean fullScreenBrowser;
    public boolean favoriteUserNotifications;
    public boolean syncSecondMentions;
    public boolean displayScreenName;
    public boolean liveStreaming;
    public boolean pushNotifications;
    public boolean inAppBrowser;
    public boolean showBoth;
    public boolean absoluteDate;
    public boolean useToast;
    public boolean autoInsertHashtags;
    public boolean alwaysCompose;
    public boolean twitlonger;
    public boolean twitpic;
    public boolean tweetmarker;
    public boolean tweetmarkerManualOnly;
    public boolean jumpingWorkaround;
    public boolean floatingCompose;
    public boolean openKeyboard;
    public boolean alwaysMobilize;
    public boolean mobilizeOnData;
    public boolean preCacheImages;
    public boolean fastTransitions;
    public boolean topDown;
    public boolean headsUp;
    public boolean useSnackbar;
    public boolean crossAccActions;
    public boolean useInteractionDrawer;
    public boolean staticUi;
    public boolean higherQualityImages;
    public boolean useMentionsOnWidget;
    public boolean widgetImages;
    public boolean autoDismissNotifications;
    public boolean usePeek;
    public boolean dualPanels;
    public boolean detailedQuotes;
    public boolean fingerprintLock;
    public boolean followersOnlyAutoComplete;
    public boolean autoCompleteHashtags;
    public boolean largerWidgetImages;
    public boolean showProfilePictures;
    public boolean compressReplies;
    public boolean cropImagesOnTimeline;
    public boolean webPreviews;
    public boolean widgetDisplayScreenname;
    public boolean onlyAutoPlayGifs;

    // notifications
    public boolean timelineNot;
    public boolean mentionsNot;
    public boolean dmsNot;
    public boolean followersNot;
    public boolean favoritesNot;
    public boolean retweetNot;
    public boolean activityNot;
    public String ringtone;

    // theme stuff
    public boolean addonTheme;
    public String addonThemePackage;
    public boolean roundContactImages;
    public int backgroundColor;
    public boolean translateProfileHeader;
    public boolean nameAndHandleOnTweet = false;
    public boolean combineProPicAndImage = false;
    public boolean sendToComposeWindow = false;
    public boolean showTitleStrip = true;
    public String accentColor;
    public int accentInt;
    public int pagerTitleInt;
    public Drawable actionBar = null;
    public Drawable customBackground = null;

    public int baseTheme;
    public int theme;
    public int layout;
    public int currentAccount;
    public int textSize;
    public int widgetTextSize;
    public int maxTweetsRefresh;
    public int timelineSize;
    public int mentionsSize;
    public int dmSize;
    public int listSize;
    public int userTweetsSize;
    public int numberOfAccounts = 0;
    public int pageToOpen;
    public int quoteStyle;
    public int navBarOption;
    public int picturesType;
    public int autoplay;
    public int widgetAccountNum;
    public int lineSpacingScalar;

    public long timelineRefresh;
    public long mentionsRefresh;
    public long dmRefresh;
    public long activityRefresh;
    public long listRefresh;
    public long myId;

    public String translateUrl;
    public String browserSelection;

    public EmojiStyle emojiStyle;
    public int tweetCharacterCount = 280;

    public AppSettings(Context context) {
        sharedPrefs = getSharedPreferences(context);
        setPrefs(sharedPrefs, context);
    }

    public AppSettings(SharedPreferences sharedPrefs, Context context) {
        setPrefs(sharedPrefs, context);
    }

    public void setPrefs(SharedPreferences sharedPrefs, Context context) {
        // Strings
        if (sharedPrefs.getInt("current_account", 1) == 1) {
            authenticationToken = sharedPrefs.getString("authentication_token_1", "none");
            authenticationTokenSecret = sharedPrefs.getString("authentication_token_secret_1", "none");
            secondAuthToken = sharedPrefs.getString("authentication_token_2", "none");
            secondAuthTokenSecret = sharedPrefs.getString("authentication_token_secret_2", "none");
            myScreenName = sharedPrefs.getString("twitter_screen_name_1", "");
            secondScreenName = sharedPrefs.getString("twitter_screen_name_2", "");
            myName = sharedPrefs.getString("twitter_users_name_1", "");
            secondName = sharedPrefs.getString("twitter_users_name_2", "");
            myBackgroundUrl = sharedPrefs.getString("twitter_background_url_1", "");
            myProfilePicUrl = sharedPrefs.getString("profile_pic_url_1", "");
            secondProfilePicUrl = sharedPrefs.getString("profile_pic_url_2", "");
            favoriteUserNames = sharedPrefs.getString("favorite_user_names_1", "");
            myId = sharedPrefs.getLong("twitter_id_1", 0);
        } else {
            authenticationToken = sharedPrefs.getString("authentication_token_2", "none");
            authenticationTokenSecret = sharedPrefs.getString("authentication_token_secret_2", "none");
            secondAuthToken = sharedPrefs.getString("authentication_token_1", "none");
            secondAuthTokenSecret = sharedPrefs.getString("authentication_token_secret_1", "none");
            myScreenName = sharedPrefs.getString("twitter_screen_name_2", "");
            secondScreenName = sharedPrefs.getString("twitter_screen_name_1", "");
            myName = sharedPrefs.getString("twitter_users_name_2", "");
            secondName = sharedPrefs.getString("twitter_users_name_1", "");
            myBackgroundUrl = sharedPrefs.getString("twitter_background_url_2", "");
            myProfilePicUrl = sharedPrefs.getString("profile_pic_url_2", "");
            secondProfilePicUrl = sharedPrefs.getString("profile_pic_url_1", "");
            favoriteUserNames = sharedPrefs.getString("favorite_user_names_2", "");
            myId = sharedPrefs.getLong("twitter_id_2", 0);
        }

        // Booleans
        baseTheme = Integer.parseInt(sharedPrefs.getString("main_theme_string", "" + DEFAULT_MAIN_THEME));
        switch (baseTheme) {
            case 0:
                darkTheme = false;
                blackTheme = false;
                break;
            case 1:
                darkTheme = true;
                blackTheme = false;
                break;
            case 2:
                darkTheme = true;
                blackTheme = true;
                break;
        }

        isTwitterLoggedIn = sharedPrefs.getBoolean("is_logged_in_1", false) || sharedPrefs.getBoolean("is_logged_in_2", false);
        reverseClickActions = sharedPrefs.getBoolean("reverse_click_actions", false);
        advanceWindowed = sharedPrefs.getBoolean("advance_windowed", true);
        led = sharedPrefs.getBoolean("led", true);
        sound = sharedPrefs.getBoolean("sound", true);
        vibrate = sharedPrefs.getBoolean("vibrate", true);
        headsUp = sharedPrefs.getBoolean("heads_up", false);
        refreshOnStart = sharedPrefs.getBoolean("refresh_on_start", false);
        autoTrim = sharedPrefs.getBoolean("auto_trim", true);
        uiExtras = sharedPrefs.getBoolean("ui_extras", true);
        wakeScreen = sharedPrefs.getBoolean("wake", true) && !Utils.isAndroidO();
        militaryTime = sharedPrefs.getBoolean("military_time", false);
        syncMobile = sharedPrefs.getBoolean("sync_mobile_data", true);
        extraPages = sharedPrefs.getBoolean("extra_pages", true);
        fullScreenBrowser = sharedPrefs.getBoolean("full_screen_browser", true);
        favoriteUserNotifications = sharedPrefs.getBoolean("favorite_users_notifications", true);
        syncSecondMentions = sharedPrefs.getBoolean("sync_second_mentions", true);
        displayScreenName = sharedPrefs.getBoolean("display_screen_name", false);
        inAppBrowser = sharedPrefs.getBoolean("inapp_browser", true);
        showBoth = sharedPrefs.getBoolean("both_handle_name", false);
        timelineNot = sharedPrefs.getBoolean("timeline_notifications", false);
        mentionsNot = sharedPrefs.getBoolean("mentions_notifications", true);
        dmsNot = sharedPrefs.getBoolean("direct_message_notifications", true);
        favoritesNot = sharedPrefs.getBoolean("favorite_notifications", true);
        retweetNot = sharedPrefs.getBoolean("retweet_notifications", true);
        followersNot = sharedPrefs.getBoolean("follower_notifications", true);
        absoluteDate = sharedPrefs.getBoolean("absolute_date", false);
        useToast = sharedPrefs.getBoolean("use_toast", true);
        autoInsertHashtags = sharedPrefs.getBoolean("auto_insert_hashtags", false);
        alwaysCompose = sharedPrefs.getBoolean("always_compose", false);
        twitlonger = true;
        twitpic = false;//sharedPrefs.getBoolean("twitpic", false);
        jumpingWorkaround = sharedPrefs.getBoolean("jumping_workaround", false);
        floatingCompose = sharedPrefs.getBoolean("floating_compose", true);
        openKeyboard = sharedPrefs.getBoolean("open_keyboard", false);
        preCacheImages = !sharedPrefs.getString("pre_cache", "1").equals("0");
        topDown = sharedPrefs.getBoolean("top_down_mode", false);
        useSnackbar = sharedPrefs.getBoolean("use_snackbar", false);
        crossAccActions = sharedPrefs.getBoolean("fav_rt_multiple_accounts", true);
        activityNot = sharedPrefs.getBoolean("activity_notifications", true);
        useInteractionDrawer = sharedPrefs.getBoolean("interaction_drawer", true);
        transpartSystemBars = sharedPrefs.getBoolean("transparent_system_bars", true);
        staticUi = sharedPrefs.getBoolean("static_ui", false);
        higherQualityImages = sharedPrefs.getBoolean("high_quality_images", true);
        useMentionsOnWidget = sharedPrefs.getString("widget_timeline", "0").equals("1");
        widgetImages = sharedPrefs.getBoolean("widget_images", true);
        autoDismissNotifications = sharedPrefs.getBoolean("auto_dismiss_notifications", true);
        usePeek = sharedPrefs.getBoolean("use_peek", true);
        dualPanels = sharedPrefs.getBoolean("dual_panel", context.getResources().getBoolean(R.bool.dual_panels));
        detailedQuotes = sharedPrefs.getBoolean("detailed_quotes", false);
        browserSelection = sharedPrefs.getString("browser_selection", "article");
        fingerprintLock = sharedPrefs.getBoolean("fingerprint_lock", false);
        followersOnlyAutoComplete = sharedPrefs.getBoolean("followers_only_auto_complete", false);
        autoCompleteHashtags = sharedPrefs.getBoolean("hashtag_auto_complete", true);
        largerWidgetImages = sharedPrefs.getBoolean("widget_larger_images", false);
        showProfilePictures = sharedPrefs.getBoolean("show_profile_pictures", true);
        compressReplies = sharedPrefs.getBoolean("new_twitter_replies", true);
        cropImagesOnTimeline = sharedPrefs.getBoolean("crop_images_timeline", true);
        webPreviews = sharedPrefs.getBoolean("web_previews_timeline", true);
        widgetDisplayScreenname = sharedPrefs.getBoolean("widget_display_screenname", true);
        onlyAutoPlayGifs = sharedPrefs.getBoolean("autoplay_gifs", false);

        if (EmojiInitializer.INSTANCE.isAlreadyUsingGoogleAndroidO()) {
            this.emojiStyle = EmojiStyle.DEFAULT;
        } else {
            String emojiStyle = sharedPrefs.getString("emoji_style", "android_o");
            switch (emojiStyle) {
                case "android_o":
                    this.emojiStyle = EmojiStyle.ANDROID_O;
                    break;
                default:
                    this.emojiStyle = EmojiStyle.DEFAULT;
            }
        }

        String notificationsOption = sharedPrefs.getString("notification_options", "legacy");
        if (notificationsOption.equals("never")) {
            notifications = false;
            interceptTwitterNotifications = false;
        } else {
            notifications = true;
            if (notificationsOption.equals("push")) {
                interceptTwitterNotifications = true;
            } else {
                interceptTwitterNotifications = false;
            }
        }

        if (sharedPrefs.getString("pre_cache", "1").equals("2")) {
            sharedPrefs.edit().putBoolean("pre_cache_wifi_only", true).apply();
        } else {
            sharedPrefs.edit().putBoolean("pre_cache_wifi_only", false).apply();
        }

        // set up tweetmarker
        String val = sharedPrefs.getString("tweetmarker_options", "0");
        if (val.equals("0")) {
            tweetmarker = false;
            tweetmarkerManualOnly = false;
        } else if (val.equals("1")) {
            tweetmarker = true;
            tweetmarkerManualOnly = false;
        } else {
            tweetmarkerManualOnly = true;
            tweetmarker = true;
        }

        // set up the mobilized (plain text) browser
        String mobilize = sharedPrefs.getString("plain_text_browser", "0");
        if (mobilize.equals("0")) {
            alwaysMobilize = false;
            mobilizeOnData = false;
        } else if (mobilize.equals("1")) {
            alwaysMobilize = true;
            mobilizeOnData = true;
        } else {
            alwaysMobilize = false;
            mobilizeOnData = true;
        }

        picturesType = Integer.parseInt(sharedPrefs.getString("timeline_pictures", "0"));
        if (picturesType == PICTURES_NONE || picturesType == CONDENSED_NO_IMAGES) {
            inlinePics = false;
        } else {
            inlinePics = true;
        }

        locale = sharedPrefs.getString("locale", "none");

        ringtone = sharedPrefs.getString("ringtone",
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());

        // if they have the keyboard trial installed, then go from their preference
        if (EmojiUtils.checkEmojisEnabled(context)) {
            useEmoji = sharedPrefs.getBoolean("use_emojis", false);
        } else { // otherwise it is false
            useEmoji = false;
        }

        String pull = sharedPrefs.getString("talon_pull", "0");
        if (pull.equals("0")) {
            liveStreaming = false;
            pushNotifications = false;
        } else if (pull.equals("1")) {
            pushNotifications = true;
            liveStreaming = false;
        } else {
            pushNotifications = true;
            liveStreaming = true;
        }

        // Integers
        currentAccount = sharedPrefs.getInt("current_account", 1);
        theme = sharedPrefs.getInt("material_theme_" + currentAccount, DEFAULT_THEME);
        layout = LAYOUT_FULL_SCREEN;
        textSize = Integer.parseInt(sharedPrefs.getString("text_size", "14"));
        widgetTextSize = Integer.parseInt(sharedPrefs.getString("widget_text_size", "14"));
        maxTweetsRefresh = Integer.parseInt(sharedPrefs.getString("max_tweets", "1"));
        timelineSize = Integer.parseInt(sharedPrefs.getString("timeline_size", "500"));
        mentionsSize = Integer.parseInt(sharedPrefs.getString("mentions_size", "100"));
        dmSize = Integer.parseInt(sharedPrefs.getString("dm_size", "100"));
        listSize = Integer.parseInt(sharedPrefs.getString("list_size", "200"));
        userTweetsSize = Integer.parseInt(sharedPrefs.getString("user_tweets_size", "200"));
        pageToOpen = Integer.parseInt(sharedPrefs.getString("viewer_page", "0"));
        quoteStyle = Integer.parseInt(sharedPrefs.getString("quote_style", "0"));
        navBarOption = Integer.parseInt(sharedPrefs.getString("nav_bar_option", "0"));
        autoplay = Integer.parseInt(sharedPrefs.getString("autoplay", AUTOPLAY_NEVER + ""));
        lineSpacingScalar = picturesType == CONDENSED_TWEETS ? 3 : Integer.parseInt(sharedPrefs.getString("line_spacing", "5"));

        String widgetAccount = sharedPrefs.getString("widget_account", "").replace("@", "");
        if (widgetAccount.equals(myScreenName.replace("@","")) || widgetAccount.isEmpty()) {
            widgetAccountNum = currentAccount;
        } else {
            if (currentAccount == 1) {
                widgetAccountNum = 2;
            } else {
                widgetAccountNum = 1;
            }
        }

        // Longs
        timelineRefresh = Long.parseLong(sharedPrefs.getString("timeline_sync_interval", "0"));
        mentionsRefresh = Long.parseLong(sharedPrefs.getString("mentions_sync_interval", "1800000"));
        dmRefresh = Long.parseLong(sharedPrefs.getString("dm_sync_interval", "0"));
        activityRefresh = Long.parseLong(sharedPrefs.getString("activity_sync_interval", "0"));
        listRefresh = Long.parseLong(sharedPrefs.getString("list_sync_interval", "0"));

        translateUrl = sharedPrefs.getString("translate_url", "https://translate.google.com/#auto|en|");

        if (baseTheme != 2 && sharedPrefs.getBoolean("night_mode", false)) {
            int startHour = sharedPrefs.getInt("night_start_hour", 22);
            int startMin = sharedPrefs.getInt("night_start_min", 0);
            int endHour = sharedPrefs.getInt("day_start_hour", 6);
            int endMin = sharedPrefs.getInt("day_start_min", 0);

            if (startHour == -1 || isInsideRange(startHour, startMin, endHour, endMin)) {
                darkTheme = true;
                baseTheme = 1;

                if (sharedPrefs.getBoolean("night_mode_black", false)) {
                    blackTheme = true;
                    baseTheme = 2;
                }
            }
        }

        if (sharedPrefs.getBoolean("quiet_hours", false)) {
            int quietStartHour = sharedPrefs.getInt("quiet_start_hour", 22);
            int quietStartMin = sharedPrefs.getInt("quiet_start_min", 0);
            int quietEndHour = sharedPrefs.getInt("quiet_end_hour", 6);
            int quietEndMin = sharedPrefs.getInt("quiet_end_min", 0);

            if (isInsideRange(quietStartHour, quietStartMin, quietEndHour, quietEndMin)) {
                Log.v("quiet_hours", "quiet hours on");
                notifications = false;
                timelineNot = false;
                mentionsNot = false;
                favoritesNot = false;
                retweetNot = false;
                followersNot = false;
                dmsNot = false;
                activityNot = false;
            }
        }

        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            numberOfAccounts++;
        }
        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            numberOfAccounts++;
        }

        if (numberOfAccounts != 2) {
            syncSecondMentions = false;
            crossAccActions = false;
        }

        setColors(context);

        if (revampedTweets()) {
            detailedQuotes = true;
        }

        if (sharedPrefs.getBoolean("data_saver_mode", false)) {
            refreshOnStart = false;
            syncMobile = false;
            syncSecondMentions = false;
            liveStreaming = false;
            pushNotifications = false;
            higherQualityImages = false;
            webPreviews = false;

            if (autoplay == AUTOPLAY_ALWAYS) {
                autoplay = AUTOPLAY_WIFI;
            }
        }
    }

    private static boolean isInsideRange(int startHour, int startMin, int endHour, int endMin) {

        String pattern = "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minutes = c.get(Calendar.MINUTE);

        try {
            Date start = sdf.parse(startHour + ":" + startMin);
            Date end = sdf.parse(endHour + ":" + endMin);
            Date current = sdf.parse(hour + ":" + minutes);

            Log.v("date_range", "current: " + current.toString() + ", start: " + start.toString() + ", end: " + end.toString());

            // we expect that the start date will be something like 22 and the end will be 6
            if (start.after(end)) {
                return current.after(start) || current.before(end);
            } else { // but some people could do quiet hours during the day, so start = 9 and end = 17
                return current.after(start) && current.before(end);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static int getCurrentTheme(SharedPreferences sharedPrefs) {
        boolean dark = false;
        boolean black = false;
        int mainTheme = Integer.parseInt(sharedPrefs.getString("main_theme_string", "" + DEFAULT_MAIN_THEME));
        switch (mainTheme) {
            case 0:
                dark = false;
                break;
            case 1:
                dark = true;
                break;
            case 2:
                dark = true;
                black = true;
                break;
        }

        if (sharedPrefs.getBoolean("night_mode", false)) {
            int startHour = sharedPrefs.getInt("night_start_hour", 22);
            int startMin = sharedPrefs.getInt("night_start_min", 0);
            int endHour = sharedPrefs.getInt("day_start_hour", 6);
            int endMin = sharedPrefs.getInt("day_start_min", 0);

            if (startHour == -1 || isInsideRange(startHour, startMin, endHour, endMin)) {
                dark = true;
                if (sharedPrefs.getBoolean("night_mode_black", false)) {
                    black = true;
                }
            }
        }

        if (black) {
            return 2;
        } else if (dark) {
            return 1;
        } else {
            return 0;
        }
    }

    protected void setValue(String key, boolean value, Context context) {
        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(context);


        sharedPreferences.edit()
                .putBoolean(key, value)
                .apply();
    }

    protected void setValue(String key, int value, Context context) {
        try {
            SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(context);


            sharedPreferences.edit()
                    .putInt(key, value)
                    .apply();
        } catch (Exception e) {

        }

    }

    protected void setValue(String key, String value, Context context) {
        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(context);


        sharedPreferences.edit()
                .putString(key, value)
                .apply();

    }

    public ThemeColor themeColors;
    private void setColors(Context context) {
        String[] themePrefixes = context.getResources().getStringArray(R.array.theme_colors);
        String prefix = themePrefixes[theme];

        themeColors = new ThemeColor(prefix, context);
    }

    public static boolean dualPanels(Context context) {
        AppSettings settings = AppSettings.getInstance(context);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                settings.dualPanels) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isWhiteToolbar(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        return settings.theme == AppSettings.THEME_WHITE || settings.theme == THEME_YELLOW;
    }

    public boolean condensedTweets() {
        return picturesType == CONDENSED_NO_IMAGES || picturesType == CONDENSED_TWEETS;
    }

    public boolean revampedTweets() {
        return picturesType == REVAMPED_TWEETS;
    }

    public static boolean isLimitedTweetCharLanguage() {
        String systemLanguage = Locale.getDefault().getLanguage();
        String[] limitingLanguages = new String[] { "ko" };

        for (String limitingLanguage : limitingLanguages) {
            if (limitingLanguage.equals(systemLanguage)) {
                return true;
            }
        }

        return false;
    }

}
