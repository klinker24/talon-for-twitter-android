package com.afollestad.easyvideoplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Aidan Follestad (afollestad)
 */
public class EasyVideoPlayer extends FrameLayout implements IUserMethods, TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnErrorListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    @IntDef({LEFT_ACTION_NONE, LEFT_ACTION_RESTART, LEFT_ACTION_RETRY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LeftAction {
    }

    @IntDef({RIGHT_ACTION_NONE, RIGHT_ACTION_SUBMIT, RIGHT_ACTION_CUSTOM_LABEL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RightAction {
    }

    public static final int LEFT_ACTION_NONE = 0;
    public static final int LEFT_ACTION_RESTART = 1;
    public static final int LEFT_ACTION_RETRY = 2;
    public static final int RIGHT_ACTION_NONE = 3;
    public static final int RIGHT_ACTION_SUBMIT = 4;
    public static final int RIGHT_ACTION_CUSTOM_LABEL = 5;
    private static final int UPDATE_INTERVAL = 100;

    public EasyVideoPlayer(Context context) {
        super(context);
        init(context, null);
    }

    public EasyVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public EasyVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private TextureView mTextureView;
    private Surface mSurface;

    private View mControlsFrame;
    private View mProgressFrame;
    private View mClickFrame;

    private SeekBar mSeeker;
    private TextView mLabelPosition;
    private TextView mLabelDuration;
    private ImageButton mBtnRestart;
    private TextView mBtnRetry;
    private ImageButton mBtnPlayPause;
    private TextView mBtnSubmit;
    private TextView mLabelCustom;
    private TextView mLabelBottom;

    private MediaPlayer mPlayer;
    private boolean mSurfaceAvailable;
    private boolean mIsPrepared;
    private boolean mWasPlaying;
    private int mInitialTextureWidth;
    private int mInitialTextureHeight;

    private Handler mHandler;

    private Uri mSource;
    private EasyVideoCallback mCallback;
    private EasyVideoProgressCallback mProgressCallback;
    @LeftAction
    private int mLeftAction = LEFT_ACTION_RESTART;
    @RightAction
    private int mRightAction = RIGHT_ACTION_NONE;
    private CharSequence mRetryText;
    private CharSequence mSubmitText;
    private Drawable mRestartDrawable;
    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;
    private CharSequence mCustomLabelText;
    private CharSequence mBottomLabelText;
    private boolean mHideControlsOnPlay = true;
    private boolean mAutoPlay;
    private int mInitialPosition = -1;
    private boolean mControlsDisabled;
    private int mThemeColor = 0;
    private boolean mAutoFullscreen = false;

    // Runnable used to run code on an interval to update counters and seeker
    private final Runnable mUpdateCounters = new Runnable() {
        @Override
        public void run() {
            if (mHandler == null || !mIsPrepared || mSeeker == null || mPlayer == null)
                return;
            int pos = mPlayer.getCurrentPosition();
            final int dur = mPlayer.getDuration();
            if (pos > dur) pos = dur;
            mLabelPosition.setText(Util.getDurationString(pos, false));
            mLabelDuration.setText(Util.getDurationString(dur - pos, true));
            mSeeker.setProgress(pos);
            mSeeker.setMax(dur);

            if (mProgressCallback != null)
                mProgressCallback.onVideoProgressUpdate(pos, dur);
            if (mHandler != null)
                mHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };


    private void init(Context context, AttributeSet attrs) {
        setBackgroundColor(Color.BLACK);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.EasyVideoPlayer,
                    0, 0);
            try {
                String source = a.getString(R.styleable.EasyVideoPlayer_evp_source);
                if (source != null && !source.trim().isEmpty())
                    mSource = Uri.parse(source);

                //noinspection WrongConstant
                mLeftAction = a.getInteger(R.styleable.EasyVideoPlayer_evp_leftAction, LEFT_ACTION_RESTART);
                //noinspection WrongConstant
                mRightAction = a.getInteger(R.styleable.EasyVideoPlayer_evp_rightAction, RIGHT_ACTION_NONE);

                mCustomLabelText = a.getText(R.styleable.EasyVideoPlayer_evp_customLabelText);
                mRetryText = a.getText(R.styleable.EasyVideoPlayer_evp_retryText);
                mSubmitText = a.getText(R.styleable.EasyVideoPlayer_evp_submitText);
                mBottomLabelText = a.getText(R.styleable.EasyVideoPlayer_evp_bottomText);


                int restartDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_evp_restartDrawable, -1);
                int playDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_evp_playDrawable, -1);
                int pauseDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_evp_pauseDrawable, -1);

                if (restartDrawableResId != -1) {
                    mRestartDrawable = AppCompatResources.getDrawable(context, restartDrawableResId);
                }
                if (playDrawableResId != -1) {
                    mPlayDrawable = AppCompatResources.getDrawable(context, playDrawableResId);
                }
                if (pauseDrawableResId != -1) {
                    mPauseDrawable = AppCompatResources.getDrawable(context, pauseDrawableResId);
                }

                mHideControlsOnPlay = a.getBoolean(R.styleable.EasyVideoPlayer_evp_hideControlsOnPlay, true);
                mAutoPlay = a.getBoolean(R.styleable.EasyVideoPlayer_evp_autoPlay, false);
                mControlsDisabled = a.getBoolean(R.styleable.EasyVideoPlayer_evp_disableControls, false);

                mThemeColor = a.getColor(R.styleable.EasyVideoPlayer_evp_themeColor,
                        Util.resolveColor(context, R.attr.colorPrimary));

                mAutoFullscreen = a.getBoolean(R.styleable.EasyVideoPlayer_evp_autoFullscreen, false);
            } finally {
                a.recycle();
            }
        } else {
            mLeftAction = LEFT_ACTION_RESTART;
            mRightAction = RIGHT_ACTION_NONE;
            mHideControlsOnPlay = true;
            mAutoPlay = false;
            mControlsDisabled = false;
            mThemeColor = Util.resolveColor(context, R.attr.colorPrimary);
            mAutoFullscreen = false;
        }

        if (mRetryText == null)
            mRetryText = context.getResources().getText(R.string.evp_retry);
        if (mSubmitText == null)
            mSubmitText = context.getResources().getText(R.string.evp_submit);

        if (mRestartDrawable == null)
            mRestartDrawable = AppCompatResources.getDrawable(context, R.drawable.evp_action_restart);
        if (mPlayDrawable == null)
            mPlayDrawable = AppCompatResources.getDrawable(context, R.drawable.evp_action_play);
        if (mPauseDrawable == null)
            mPauseDrawable = AppCompatResources.getDrawable(context, R.drawable.evp_action_pause);
    }

    @Override
    public void setSource(@NonNull Uri source) {
        mSource = source;
        if (mPlayer != null) prepare();
    }

    @Override
    public void setCallback(@NonNull EasyVideoCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setProgressCallback(@NonNull EasyVideoProgressCallback callback) {
        mProgressCallback = callback;
    }

    @Override
    public void setLeftAction(@LeftAction int action) {
        if (action < LEFT_ACTION_NONE || action > LEFT_ACTION_RETRY)
            throw new IllegalArgumentException("Invalid left action specified.");
        mLeftAction = action;
        invalidateActions();
    }

    @Override
    public void setRightAction(@RightAction int action) {
        if (action < RIGHT_ACTION_NONE || action > RIGHT_ACTION_CUSTOM_LABEL)
            throw new IllegalArgumentException("Invalid right action specified.");
        mRightAction = action;
        invalidateActions();
    }

    @Override
    public void setCustomLabelText(@Nullable CharSequence text) {
        mCustomLabelText = text;
        mLabelCustom.setText(text);
        setRightAction(RIGHT_ACTION_CUSTOM_LABEL);
    }

    @Override
    public void setCustomLabelTextRes(@StringRes int textRes) {
        setCustomLabelText(getResources().getText(textRes));
    }

    @Override
    public void setBottomLabelText(@Nullable CharSequence text) {
        mBottomLabelText = text;
        mLabelBottom.setText(text);
        if (text == null || text.toString().trim().length() == 0)
            mLabelBottom.setVisibility(View.GONE);
        else mLabelBottom.setVisibility(View.VISIBLE);
    }

    @Override
    public void setBottomLabelTextRes(@StringRes int textRes) {
        setBottomLabelText(getResources().getText(textRes));
    }

    @Override
    public void setRetryText(@Nullable CharSequence text) {
        mRetryText = text;
        mBtnRetry.setText(text);
    }

    @Override
    public void setRetryTextRes(@StringRes int res) {
        setRetryText(getResources().getText(res));
    }

    @Override
    public void setSubmitText(@Nullable CharSequence text) {
        mSubmitText = text;
        mBtnSubmit.setText(text);
    }

    @Override
    public void setSubmitTextRes(@StringRes int res) {
        setSubmitText(getResources().getText(res));
    }

    @Override
    public void setRestartDrawable(@NonNull Drawable drawable) {
        mRestartDrawable = drawable;
        mBtnRestart.setImageDrawable(drawable);
    }

    @Override
    public void setRestartDrawableRes(@DrawableRes int res) {
        setRestartDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setPlayDrawable(@NonNull Drawable drawable) {
        mPlayDrawable = drawable;
        if (!isPlaying()) mBtnPlayPause.setImageDrawable(drawable);
    }

    @Override
    public void setPlayDrawableRes(@DrawableRes int res) {
        setPlayDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setPauseDrawable(@NonNull Drawable drawable) {
        mPauseDrawable = drawable;
        if (isPlaying()) mBtnPlayPause.setImageDrawable(drawable);
    }

    @Override
    public void setPauseDrawableRes(@DrawableRes int res) {
        setPauseDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setThemeColor(@ColorInt int color) {
        mThemeColor = color;
        invalidateThemeColors();
    }

    @Override
    public void setThemeColorRes(@ColorRes int colorRes) {
        setThemeColor(ContextCompat.getColor(getContext(), colorRes));
    }

    @Override
    public void setHideControlsOnPlay(boolean hide) {
        mHideControlsOnPlay = hide;
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;
    }

    @Override
    public void setInitialPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        mInitialPosition = pos;
    }

    private void prepare() {
        if (!mSurfaceAvailable || mSource == null || mPlayer == null || mIsPrepared)
            return;
        try {
            if (mCallback != null)
                mCallback.onPreparing(this);
            mPlayer.setSurface(mSurface);
            if (mSource.getScheme() != null &&
                    (mSource.getScheme().equals("http") || mSource.getScheme().equals("https"))) {
                LOG("Loading web URI: " + mSource.toString());
                mPlayer.setDataSource(mSource.toString());
            } else {
                LOG("Loading local URI: " + mSource.toString());
                mPlayer.setDataSource(getContext(), mSource);
            }
            mPlayer.prepareAsync();
        } catch (IOException e) {
            throwError(e);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        if (mSeeker == null) return;
        mSeeker.setEnabled(enabled);
        mBtnPlayPause.setEnabled(enabled);
        mBtnSubmit.setEnabled(enabled);
        mBtnRestart.setEnabled(enabled);
        mBtnRetry.setEnabled(enabled);

        final float disabledAlpha = .4f;
        mBtnPlayPause.setAlpha(enabled ? 1f : disabledAlpha);
        mBtnSubmit.setAlpha(enabled ? 1f : disabledAlpha);
        mBtnRestart.setAlpha(enabled ? 1f : disabledAlpha);

        mClickFrame.setEnabled(enabled);
    }

    @Override
    public void showControls() {
        if (mControlsDisabled || isControlsShown() || mSeeker == null)
            return;

        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(0f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame.animate().alpha(1f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mAutoFullscreen)
                            setFullscreen(false);
                    }
                }).start();
    }

    @Override
    public void hideControls() {
        if (mControlsDisabled || !isControlsShown() || mSeeker == null)
            return;
        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(1f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame.animate().alpha(0f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setFullscreen(true);

                        if (mControlsFrame != null)
                            mControlsFrame.setVisibility(View.INVISIBLE);
                    }
                }).start();
    }

    @CheckResult
    @Override
    public boolean isControlsShown() {
        return !mControlsDisabled && mControlsFrame != null && mControlsFrame.getAlpha() > .5f;
    }

    @Override
    public void toggleControls() {
        if (mControlsDisabled)
            return;
        if (isControlsShown()) {
            hideControls();
        } else {
            showControls();
        }
    }

    @Override
    public void enableControls(boolean andShow) {
        mControlsDisabled = false;
        if (andShow) showControls();
        mClickFrame.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleControls();
            }
        });
        mClickFrame.setClickable(true);
    }

    @Override
    public void disableControls() {
        mControlsDisabled = true;
        mControlsFrame.setVisibility(View.GONE);
        mClickFrame.setOnClickListener(null);
        mClickFrame.setClickable(false);
    }

    @CheckResult
    @Override
    public boolean isPrepared() {
        return mPlayer != null && mIsPrepared;
    }

    @CheckResult
    @Override
    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    @CheckResult
    @Override
    public int getCurrentPosition() {
        if (mPlayer == null) return -1;
        return mPlayer.getCurrentPosition();
    }

    @CheckResult
    @Override
    public int getDuration() {
        if (mPlayer == null) return -1;
        return mPlayer.getDuration();
    }

    @Override
    public void start() {
        if (mPlayer == null) return;
        mPlayer.start();
        if (mCallback != null) mCallback.onStarted(this);
        if (mHandler == null) mHandler = new Handler();
        mHandler.post(mUpdateCounters);
        mBtnPlayPause.setImageDrawable(mPauseDrawable);
    }

    @Override
    public void seekTo(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        if (mPlayer == null) return;
        mPlayer.seekTo(pos);
    }

    public void setVolume(@FloatRange(from = 0f, to = 1f) float leftVolume, @FloatRange(from = 0f, to = 1f) float rightVolume) {
        if (mPlayer == null || !mIsPrepared)
            throw new IllegalStateException("You cannot use setVolume(float, float) until the player is prepared.");
        mPlayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void pause() {
        if (mPlayer == null || !isPlaying()) return;
        mPlayer.pause();
        mCallback.onPaused(this);
        if (mHandler == null) return;
        mHandler.removeCallbacks(mUpdateCounters);
        mBtnPlayPause.setImageDrawable(mPlayDrawable);
    }

    @Override
    public void stop() {
        if (mPlayer == null) return;
        try {
            mPlayer.stop();
        } catch (Throwable ignored) {
        }
        if (mHandler == null) return;
        mHandler.removeCallbacks(mUpdateCounters);
        mBtnPlayPause.setImageDrawable(mPauseDrawable);
    }

    @Override
    public void reset() {
        if (mPlayer == null) return;
        mIsPrepared = false;
        mPlayer.reset();
        mIsPrepared = false;
    }

    @Override
    public void release() {
        mIsPrepared = false;

        if (mPlayer != null) {
            try {
                mPlayer.release();
            } catch (Throwable ignored) {
            }
            mPlayer = null;
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateCounters);
            mHandler = null;
        }

        LOG("Released player and Handler");
    }

    @Override
    public void setAutoFullscreen(boolean autoFullscreen) {
        this.mAutoFullscreen = autoFullscreen;
    }

    // Surface listeners

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        LOG("Surface texture available: %dx%d", width, height);
        mInitialTextureWidth = width;
        mInitialTextureHeight = height;
        mSurfaceAvailable = true;
        mSurface = new Surface(surfaceTexture);
        if (mIsPrepared) {
            mPlayer.setSurface(mSurface);
        } else {
            prepare();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        LOG("Surface texture changed: %dx%d", width, height);
        adjustAspectRatio(width, height, mPlayer.getVideoWidth(), mPlayer.getVideoHeight());
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        LOG("Surface texture destroyed");
        mSurfaceAvailable = false;
        mSurface = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    // Media player listeners

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        LOG("onPrepared()");
        mProgressFrame.setVisibility(View.INVISIBLE);
        mIsPrepared = true;
        if (mCallback != null)
            mCallback.onPrepared(this);
        mLabelPosition.setText(Util.getDurationString(0, false));
        mLabelDuration.setText(Util.getDurationString(mediaPlayer.getDuration(), false));
        mSeeker.setProgress(0);
        mSeeker.setMax(mediaPlayer.getDuration());
        setControlsEnabled(true);

        if (mAutoPlay) {
            if (!mControlsDisabled && mHideControlsOnPlay)
                hideControls();
            start();
            if (mInitialPosition > 0) {
                seekTo(mInitialPosition);
                mInitialPosition = -1;
            }
        } else {
            // Hack to show first frame, is there another way?
            mPlayer.start();
            mPlayer.pause();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        LOG("Buffering: %d%%", percent);
        if (mCallback != null)
            mCallback.onBuffering(percent);
        if (mSeeker != null) {
            if (percent == 100) mSeeker.setSecondaryProgress(0);
            else mSeeker.setSecondaryProgress(mSeeker.getMax() * (percent / 100));
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        LOG("onCompletion()");
        if (mCallback != null)
            mCallback.onCompletion(this);
        mBtnPlayPause.setImageDrawable(mPlayDrawable);
        if (mHandler != null)
            mHandler.removeCallbacks(mUpdateCounters);
        mSeeker.setProgress(mSeeker.getMax());
        showControls();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        LOG("Video size changed: %dx%d", width, height);
        adjustAspectRatio(mInitialTextureWidth, mInitialTextureHeight, width, height);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        if (what == -38) {
            // Error code -38 happens on some Samsung devices
            // Just ignore it
            return false;
        }
        String errorMsg = "Preparation/playback error (" + what + "): ";
        switch (what) {
            default:
                errorMsg += "Unknown error";
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                errorMsg += "I/O error";
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                errorMsg += "Malformed";
                break;
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                errorMsg += "Not valid for progressive playback";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                errorMsg += "Server died";
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                errorMsg += "Timed out";
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                errorMsg += "Unsupported";
                break;
        }
        throwError(new Exception(errorMsg));
        return false;
    }

    // View events

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setKeepScreenOn(true);

        mHandler = new Handler();
        mPlayer = new MediaPlayer();
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnVideoSizeChangedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // Instantiate and add TextureView for rendering
        final FrameLayout.LayoutParams textureLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mTextureView = new TextureView(getContext());
        addView(mTextureView, textureLp);
        mTextureView.setSurfaceTextureListener(this);

        final LayoutInflater li = LayoutInflater.from(getContext());

        // Inflate and add progress
        mProgressFrame = li.inflate(R.layout.evp_include_progress, this, false);
        addView(mProgressFrame);

        // Instantiate and add click frame (used to toggle controls)
        mClickFrame = new FrameLayout(getContext());
        //noinspection RedundantCast
        ((FrameLayout) mClickFrame).setForeground(Util.resolveDrawable(getContext(), R.attr.selectableItemBackground));
        addView(mClickFrame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Inflate controls
        mControlsFrame = li.inflate(R.layout.evp_include_controls, this, false);
        final FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsLp.gravity = Gravity.BOTTOM;
        addView(mControlsFrame, controlsLp);
        if (mControlsDisabled) {
            mClickFrame.setOnClickListener(null);
            mControlsFrame.setVisibility(View.GONE);
        } else {
            mClickFrame.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleControls();
                }
            });
        }


        // Retrieve controls
        mSeeker = (SeekBar) mControlsFrame.findViewById(R.id.seeker);
        mSeeker.setOnSeekBarChangeListener(this);

        mLabelPosition = (TextView) mControlsFrame.findViewById(R.id.position);
        mLabelPosition.setText(Util.getDurationString(0, false));

        mLabelDuration = (TextView) mControlsFrame.findViewById(R.id.duration);
        mLabelDuration.setText(Util.getDurationString(0, true));

        mBtnRestart = (ImageButton) mControlsFrame.findViewById(R.id.btnRestart);
        mBtnRestart.setOnClickListener(this);
        mBtnRestart.setImageDrawable(mRestartDrawable);

        mBtnRetry = (TextView) mControlsFrame.findViewById(R.id.btnRetry);
        mBtnRetry.setOnClickListener(this);
        mBtnRetry.setText(mRetryText);

        mBtnPlayPause = (ImageButton) mControlsFrame.findViewById(R.id.btnPlayPause);
        mBtnPlayPause.setOnClickListener(this);
        mBtnPlayPause.setImageDrawable(mPlayDrawable);

        mBtnSubmit = (TextView) mControlsFrame.findViewById(R.id.btnSubmit);
        mBtnSubmit.setOnClickListener(this);
        mBtnSubmit.setText(mSubmitText);

        mLabelCustom = (TextView) mControlsFrame.findViewById(R.id.labelCustom);
        mLabelCustom.setText(mCustomLabelText);

        mLabelBottom = (TextView) mControlsFrame.findViewById(R.id.labelBottom);
        setBottomLabelText(mBottomLabelText);

        invalidateThemeColors();

        setControlsEnabled(false);
        invalidateActions();
        prepare();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnPlayPause) {
            if (mPlayer.isPlaying()) {
                pause();
            } else {
                if (mHideControlsOnPlay && !mControlsDisabled)
                    hideControls();
                start();
            }
        } else if (view.getId() == R.id.btnRestart) {
            seekTo(0);
            if (!isPlaying()) start();
        } else if (view.getId() == R.id.btnRetry) {
            if (mCallback != null)
                mCallback.onRetry(this, mSource);
        } else if (view.getId() == R.id.btnSubmit) {
            if (mCallback != null)
                mCallback.onSubmit(this, mSource);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
        if (fromUser) seekTo(value);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mWasPlaying = isPlaying();
        if (mWasPlaying) mPlayer.pause(); // keeps the time updater running, unlike pause()
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mWasPlaying) mPlayer.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LOG("Detached from window");
        release();

        mSeeker = null;
        mLabelPosition = null;
        mLabelDuration = null;
        mBtnPlayPause = null;
        mBtnRestart = null;
        mBtnSubmit = null;

        mControlsFrame = null;
        mClickFrame = null;
        mProgressFrame = null;

        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateCounters);
            mHandler = null;
        }
    }

    // Utilities

    private static void LOG(String message, Object... args) {
        try {
            if (args != null)
                message = String.format(message, args);
            Log.d("EasyVideoPlayer", message);
        } catch (Exception ignored) {
        }
    }

    private void invalidateActions() {
        switch (mLeftAction) {
            case LEFT_ACTION_NONE:
                mBtnRetry.setVisibility(View.GONE);
                mBtnRestart.setVisibility(View.GONE);
                break;
            case LEFT_ACTION_RESTART:
                mBtnRetry.setVisibility(View.GONE);
                mBtnRestart.setVisibility(View.VISIBLE);
                break;
            case LEFT_ACTION_RETRY:
                mBtnRetry.setVisibility(View.VISIBLE);
                mBtnRestart.setVisibility(View.GONE);
                break;
        }
        switch (mRightAction) {
            case RIGHT_ACTION_NONE:
                mBtnSubmit.setVisibility(View.GONE);
                mLabelCustom.setVisibility(View.GONE);
                break;
            case RIGHT_ACTION_SUBMIT:
                mBtnSubmit.setVisibility(View.VISIBLE);
                mLabelCustom.setVisibility(View.GONE);
                break;
            case RIGHT_ACTION_CUSTOM_LABEL:
                mBtnSubmit.setVisibility(View.GONE);
                mLabelCustom.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void adjustAspectRatio(int viewWidth, int viewHeight, int videoWidth, int videoHeight) {
        final double aspectRatio = (double) videoHeight / videoWidth;
        int newWidth, newHeight;

        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }

        final int xoff = (viewWidth - newWidth) / 2;
        final int yoff = (viewHeight - newHeight) / 2;

        final Matrix txform = new Matrix();
        mTextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
    }

    private void throwError(Exception e) {
        if (mCallback != null)
            mCallback.onError(this, e);
        else throw new RuntimeException(e);
    }

    private static void setTint(@NonNull SeekBar seekBar, @ColorInt int color) {
        ColorStateList s1 = ColorStateList.valueOf(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            seekBar.setThumbTintList(s1);
            seekBar.setProgressTintList(s1);
            seekBar.setSecondaryProgressTintList(s1);
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            Drawable progressDrawable = DrawableCompat.wrap(seekBar.getProgressDrawable());
            seekBar.setProgressDrawable(progressDrawable);
            DrawableCompat.setTintList(progressDrawable, s1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Drawable thumbDrawable = DrawableCompat.wrap(seekBar.getThumb());
                DrawableCompat.setTintList(thumbDrawable, s1);
                seekBar.setThumb(thumbDrawable);
            }
        } else {
            PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                mode = PorterDuff.Mode.MULTIPLY;
            }
            if (seekBar.getIndeterminateDrawable() != null)
                seekBar.getIndeterminateDrawable().setColorFilter(color, mode);
            if (seekBar.getProgressDrawable() != null)
                seekBar.getProgressDrawable().setColorFilter(color, mode);
        }
    }

    private Drawable tintDrawable(@NonNull Drawable d, @ColorInt int color) {
        d = DrawableCompat.wrap(d.mutate());
        DrawableCompat.setTint(d, color);
        return d;
    }

    private void tintSelector(@NonNull View view, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && view.getBackground() instanceof RippleDrawable) {
            final RippleDrawable rd = (RippleDrawable) view.getBackground();
            rd.setColor(ColorStateList.valueOf(Util.adjustAlpha(color, 0.3f)));
        }
    }

    private void invalidateThemeColors() {
        final int labelColor = Util.isColorDark(mThemeColor) ? Color.WHITE : Color.BLACK;
        mControlsFrame.setBackgroundColor(Util.adjustAlpha(mThemeColor, 0.8f));
        tintSelector(mBtnRestart, labelColor);
        tintSelector(mBtnPlayPause, labelColor);
        mLabelDuration.setTextColor(labelColor);
        mLabelPosition.setTextColor(labelColor);
        setTint(mSeeker, labelColor);
        mBtnRetry.setTextColor(labelColor);
        tintSelector(mBtnRetry, labelColor);
        mBtnSubmit.setTextColor(labelColor);
        tintSelector(mBtnSubmit, labelColor);
        mLabelCustom.setTextColor(labelColor);
        mLabelBottom.setTextColor(labelColor);
        mPlayDrawable = tintDrawable(mPlayDrawable.mutate(), labelColor);
        mRestartDrawable = tintDrawable(mRestartDrawable.mutate(), labelColor);
        mPauseDrawable = tintDrawable(mPauseDrawable.mutate(), labelColor);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setFullscreen(boolean fullscreen) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mAutoFullscreen) {
                int flags = !fullscreen ? 0 : View.SYSTEM_UI_FLAG_LOW_PROFILE;

                ViewCompat.setFitsSystemWindows(mControlsFrame, !fullscreen);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                    if (fullscreen) {
                        flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE;
                    }
                }

                mClickFrame.setSystemUiVisibility(flags);
            }
        }
    }
}