package com.afollestad.materialcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.support.annotation.AttrRes;
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

import com.afollestad.materialcamera.internal.CameraIntentKey;
import com.afollestad.materialcamera.util.CameraUtil;
import com.afollestad.materialdialogs.util.DialogUtils;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess")
public class MaterialCamera {

    @IntDef({QUALITY_HIGH, QUALITY_LOW, QUALITY_480P, QUALITY_720P, QUALITY_1080P})
    @Retention(RetentionPolicy.SOURCE)
    public @interface QualityProfile {
    }

    public static final int QUALITY_HIGH = CamcorderProfile.QUALITY_HIGH;
    public static final int QUALITY_LOW = CamcorderProfile.QUALITY_LOW;
    public static final int QUALITY_480P = CamcorderProfile.QUALITY_480P;
    public static final int QUALITY_720P = CamcorderProfile.QUALITY_720P;
    public static final int QUALITY_1080P = CamcorderProfile.QUALITY_1080P;

    public static final String ERROR_EXTRA = "mcam_error";
    public static final String STATUS_EXTRA = "mcam_status";

    public static final int STATUS_RECORDED = 1;
    public static final int STATUS_RETRY = 2;

    private Context mContext;
    private Activity mActivityContext;
    private android.app.Fragment mAppFragment;
    private android.support.v4.app.Fragment mSupportFragment;
    private boolean mIsFragment = false;
    private long mLengthLimit = -1;
    private boolean mAllowRetry = true;
    private boolean mAutoSubmit = false;
    private String mSaveDir;
    private int mPrimaryColor;
    private boolean mShowPortraitWarning = true;
    private boolean mDefaultToFrontFacing = false;
    private boolean mCountdownImmediately = false;
    private boolean mRetryExists = false;
    private boolean mRestartTimerOnRetry = false;
    private boolean mContinueTimerInPlayback = true;
    private boolean mForceCamera1 = false;
    private boolean mStillShot;
    private long mAutoRecord = -1;

    private int mVideoEncodingBitRate = -1;
    private int mAudioEncodingBitRate = -1;
    private int mVideoFrameRate = -1;
    private int mVideoPreferredHeight = -1;
    private float mVideoPreferredAspect = -1f;
    private long mMaxFileSize = -1;
    private int mQualityProfile = -1;

    private int mIconRecord;
    private int mIconStop;
    private int mIconFrontCamera;
    private int mIconRearCamera;
    private int mIconPlay;
    private int mIconPause;
    private int mIconRestart;

    private int mLabelRetry;
    private int mLabelConfirm;

    public MaterialCamera(@NonNull Activity context) {
        mContext = context;
        mActivityContext = context;
        mPrimaryColor = DialogUtils.resolveColor(context, R.attr.colorPrimary);
    }

    public MaterialCamera(@NonNull android.app.Fragment context) {
        mIsFragment = true;
        mContext = context.getActivity();
        mAppFragment = context;
        mSupportFragment = null;
        mPrimaryColor = DialogUtils.resolveColor(mContext, R.attr.colorPrimary);
    }

    public MaterialCamera(@NonNull android.support.v4.app.Fragment context) {
        mIsFragment = true;
        mContext = context.getContext();
        mSupportFragment = context;
        mAppFragment = null;
        mPrimaryColor = DialogUtils.resolveColor(mContext, R.attr.colorPrimary);
    }

    public MaterialCamera countdownMillis(long lengthLimitMs) {
        mLengthLimit = lengthLimitMs;
        return this;
    }

    public MaterialCamera countdownSeconds(float lengthLimitSec) {
        return countdownMillis((int) (lengthLimitSec * 1000f));
    }

    public MaterialCamera countdownMinutes(float lengthLimitMin) {
        return countdownMillis((int) (lengthLimitMin * 1000f * 60f));
    }

    public MaterialCamera allowRetry(boolean allowRetry) {
        mAllowRetry = allowRetry;
        return this;
    }

    public MaterialCamera autoSubmit(boolean autoSubmit) {
        mAutoSubmit = autoSubmit;
        return this;
    }

    public MaterialCamera countdownImmediately(boolean immediately) {
        mCountdownImmediately = immediately;
        return this;
    }

    public MaterialCamera saveDir(@Nullable File dir) {
        if (dir == null) return saveDir((String) null);
        return saveDir(dir.getAbsolutePath());
    }

    public MaterialCamera saveDir(@Nullable String dir) {
        mSaveDir = dir;
        return this;
    }

    public MaterialCamera primaryColor(@ColorInt int color) {
        mPrimaryColor = color;
        return this;
    }

    public MaterialCamera primaryColorRes(@ColorRes int colorRes) {
        return primaryColor(ContextCompat.getColor(mContext, colorRes));
    }

    public MaterialCamera primaryColorAttr(@AttrRes int colorAttr) {
        return primaryColor(DialogUtils.resolveColor(mContext, colorAttr));
    }

    public MaterialCamera showPortraitWarning(boolean show) {
        mShowPortraitWarning = show;
        return this;
    }

    public MaterialCamera defaultToFrontFacing(boolean frontFacing) {
        mDefaultToFrontFacing = frontFacing;
        return this;
    }

    public MaterialCamera retryExits(boolean exits) {
        mRetryExists = exits;
        return this;
    }

    public MaterialCamera restartTimerOnRetry(boolean restart) {
        mRestartTimerOnRetry = restart;
        return this;
    }

    public MaterialCamera continueTimerInPlayback(boolean continueTimer) {
        mContinueTimerInPlayback = continueTimer;
        return this;
    }

    public MaterialCamera forceCamera1() {
        mForceCamera1 = true;
        return this;
    }

    /**
     * @deprecated Renamed to videoEncodingBitRate(int).
     */
    @Deprecated
    public MaterialCamera videoBitRate(@IntRange(from = 1, to = Integer.MAX_VALUE) int rate) {
        return videoEncodingBitRate(rate);
    }

    public MaterialCamera videoEncodingBitRate(@IntRange(from = 1, to = Integer.MAX_VALUE) int rate) {
        mVideoEncodingBitRate = rate;
        return this;
    }

    public MaterialCamera audioEncodingBitRate(@IntRange(from = 1, to = Integer.MAX_VALUE) int rate) {
        mAudioEncodingBitRate = rate;
        return this;
    }

    public MaterialCamera videoFrameRate(@IntRange(from = 1, to = Integer.MAX_VALUE) int rate) {
        mVideoFrameRate = rate;
        return this;
    }

    public MaterialCamera videoPreferredHeight(@IntRange(from = 1, to = Integer.MAX_VALUE) int height) {
        mVideoPreferredHeight = height;
        return this;
    }

    public MaterialCamera videoPreferredAspect(@FloatRange(from = 0.1, to = Float.MAX_VALUE) float ratio) {
        mVideoPreferredAspect = ratio;
        return this;
    }

    public MaterialCamera maxAllowedFileSize(long size) {
        mMaxFileSize = size;
        return this;
    }

    public MaterialCamera qualityProfile(@QualityProfile int profile) {
        mQualityProfile = profile;
        return this;
    }

    public MaterialCamera iconRecord(@DrawableRes int iconRes) {
        mIconRecord = iconRes;
        return this;
    }

    public MaterialCamera iconStop(@DrawableRes int iconRes) {
        mIconStop = iconRes;
        return this;
    }

    public MaterialCamera iconFrontCamera(@DrawableRes int iconRes) {
        mIconFrontCamera = iconRes;
        return this;
    }

    public MaterialCamera iconRearCamera(@DrawableRes int iconRes) {
        mIconRearCamera = iconRes;
        return this;
    }

    public MaterialCamera iconPlay(@DrawableRes int iconRes) {
        mIconPlay = iconRes;
        return this;
    }

    public MaterialCamera iconPause(@DrawableRes int iconRes) {
        mIconPause = iconRes;
        return this;
    }

    public MaterialCamera iconRestart(@DrawableRes int iconRes) {
        mIconRestart = iconRes;
        return this;
    }

    public MaterialCamera labelRetry(@StringRes int stringRes) {
        mLabelRetry = stringRes;
        return this;
    }

    @Deprecated
    /*
        This has been replaced with labelConfirm
     */
    public MaterialCamera labelUseVideo(@StringRes int stringRes) {
        mLabelConfirm = stringRes;
        return this;
    }

    public MaterialCamera labelConfirm(@StringRes int stringRes) {
        mLabelConfirm = stringRes;
        return this;
    }

    /**
     * Will take a still shot instead of recording.
     */
    public MaterialCamera stillShot() {
        mStillShot = true;
        return this;
    }

    public MaterialCamera autoRecordWithDelayMs(@IntRange(from = -1, to = Long.MAX_VALUE) long delayMillis) {
        mAutoRecord = delayMillis;
        return this;
    }

    public MaterialCamera autoRecordWithDelaySec(@IntRange(from = -1, to = Long.MAX_VALUE) int delaySeconds) {
        mAutoRecord = delaySeconds * 1000;
        return this;
    }

    public Intent getIntent() {
        final Class<?> cls = !mForceCamera1 && CameraUtil.hasCamera2(mContext, mStillShot) ?
                CaptureActivity2.class : CaptureActivity.class;
        Intent intent = new Intent(mContext, cls)
                .putExtra(CameraIntentKey.LENGTH_LIMIT, mLengthLimit)
                .putExtra(CameraIntentKey.ALLOW_RETRY, mAllowRetry)
                .putExtra(CameraIntentKey.AUTO_SUBMIT, mAutoSubmit)
                .putExtra(CameraIntentKey.SAVE_DIR, mSaveDir)
                .putExtra(CameraIntentKey.PRIMARY_COLOR, mPrimaryColor)
                .putExtra(CameraIntentKey.SHOW_PORTRAIT_WARNING, mShowPortraitWarning)
                .putExtra(CameraIntentKey.DEFAULT_TO_FRONT_FACING, mDefaultToFrontFacing)
                .putExtra(CameraIntentKey.COUNTDOWN_IMMEDIATELY, mCountdownImmediately)
                .putExtra(CameraIntentKey.RETRY_EXITS, mRetryExists)
                .putExtra(CameraIntentKey.RESTART_TIMER_ON_RETRY, mRestartTimerOnRetry)
                .putExtra(CameraIntentKey.CONTINUE_TIMER_IN_PLAYBACK, mContinueTimerInPlayback)
                .putExtra(CameraIntentKey.STILL_SHOT, mStillShot)
                .putExtra(CameraIntentKey.AUTO_RECORD, mAutoRecord);

        if (mVideoEncodingBitRate > 0)
            intent.putExtra(CameraIntentKey.VIDEO_BIT_RATE, mVideoEncodingBitRate);
        if (mAudioEncodingBitRate > 0)
            intent.putExtra(CameraIntentKey.AUDIO_ENCODING_BIT_RATE, mAudioEncodingBitRate);
        if (mVideoFrameRate > 0)
            intent.putExtra(CameraIntentKey.VIDEO_FRAME_RATE, mVideoFrameRate);
        if (mVideoPreferredHeight > 0)
            intent.putExtra(CameraIntentKey.VIDEO_PREFERRED_HEIGHT, mVideoPreferredHeight);
        if (mVideoPreferredAspect > 0f)
            intent.putExtra(CameraIntentKey.VIDEO_PREFERRED_ASPECT, mVideoPreferredAspect);
        if (mMaxFileSize > -1)
            intent.putExtra(CameraIntentKey.MAX_ALLOWED_FILE_SIZE, mMaxFileSize);
        if (mQualityProfile > -1)
            intent.putExtra(CameraIntentKey.QUALITY_PROFILE, mQualityProfile);

        if (mIconRecord != 0)
            intent.putExtra(CameraIntentKey.ICON_RECORD, mIconRecord);
        if (mIconStop != 0)
            intent.putExtra(CameraIntentKey.ICON_STOP, mIconStop);
        if (mIconFrontCamera != 0)
            intent.putExtra(CameraIntentKey.ICON_FRONT_CAMERA, mIconFrontCamera);
        if (mIconRearCamera != 0)
            intent.putExtra(CameraIntentKey.ICON_REAR_CAMERA, mIconRearCamera);
        if (mIconPlay != 0)
            intent.putExtra(CameraIntentKey.ICON_PLAY, mIconPlay);
        if (mIconPause != 0)
            intent.putExtra(CameraIntentKey.ICON_PAUSE, mIconPause);
        if (mIconRestart != 0)
            intent.putExtra(CameraIntentKey.ICON_RESTART, mIconRestart);
        if (mLabelRetry != 0)
            intent.putExtra(CameraIntentKey.LABEL_RETRY, mLabelRetry);
        if (mLabelConfirm != 0)
            intent.putExtra(CameraIntentKey.LABEL_CONFIRM, mLabelConfirm);

        return intent;
    }

    public void start(int requestCode) {
        if (mIsFragment && mSupportFragment != null)
            mSupportFragment.startActivityForResult(getIntent(), requestCode);
        else if (mIsFragment && mAppFragment != null)
            mAppFragment.startActivityForResult(getIntent(), requestCode);
        else
            mActivityContext.startActivityForResult(getIntent(), requestCode);
    }
}
