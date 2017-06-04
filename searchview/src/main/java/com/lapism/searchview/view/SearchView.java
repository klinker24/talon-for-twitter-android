package com.lapism.searchview.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.lapism.arrow.ArrowDrawable;
import com.lapism.searchview.R;
import com.lapism.searchview.adapter.SearchAdapter;

import java.util.List;

// TODO
// clean code this
// Voice PERMISSION
// FIX EDIT TEXT PROPERTIES
// fix out animation
// CoordinatorLayout.Behavior
// database inside SearchView
// animace out, krizek, animace otevirani, zavirani toho okna, dabazi dovnitr
// zadne suggestions

public class SearchView extends FrameLayout implements Filter.FilterListener, View.OnClickListener {

    public static final int SPEECH_REQUEST_CODE = 1234;
    private final Context mContext;
    private int mVersion = SearchCodes.VERSION_TOOLBAR;
    private int mStyle = SearchCodes.STYLE_TOOLBAR_CLASSIC;
    private int ANIMATION_DURATION = 360;
    private boolean mVoice = true;
    private boolean mIsSearchOpen = false;
    private float mIsSearchArrowHamburgerState = ArrowDrawable.STATE_HAMBURGER;
    private String VOICE_SEARCH_TEXT = "Speak now";
    private View mDivider;
    private View mShadow;
    private Activity mActivity = null;
    private Fragment mFragment = null;
    private SearchAdapter mSearchAdapter;
    private CharSequence mOldQueryText;
    private ArrowDrawable mSearchArrow;
    private RecyclerView mRecyclerView;
    private CardView mCardView;
    private EditText mEditText;
    private ImageView mBackImageView;
    private ImageView mVoiceImageView;
    private ImageView mEmptyImageView;
    private OnQueryTextListener mOnQueryChangeListener;
    private SearchViewListener mSearchViewListener;
    private SearchMenuListener mSearchMenuListener;
    private CharSequence mUserQuery;
    private SavedState mSavedState;

    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context; // getContext();
        initView();
        initStyle(attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SearchView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        initView();
        initStyle(attrs, defStyleAttr, defStyleRes);
    }

    // init ----------------------------------------------------------------------------------------
    private void initView() {
        LayoutInflater.from(mContext).inflate((R.layout.search_view), this, true);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_result);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mBackImageView = (ImageView) findViewById(R.id.imageView_arrow_back);
        mBackImageView.setOnClickListener(this);

        mVoiceImageView = (ImageView) findViewById(R.id.imageView_mic);
        mVoiceImageView.setOnClickListener(this);

        mEmptyImageView = (ImageView) findViewById(R.id.imageView_clear);
        mEmptyImageView.setOnClickListener(this);
        mEmptyImageView.setVisibility(View.GONE);

        mShadow = findViewById(R.id.view_shadow);
        mShadow.setOnClickListener(this);
        mShadow.setVisibility(View.GONE);

        mDivider = findViewById(R.id.view_divider);
        mDivider.setVisibility(View.GONE);

        mCardView = (CardView) findViewById(R.id.cardView);

        mEditText = (EditText) findViewById(R.id.editText_input);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                onSubmitQuery();
                return true;
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mUserQuery = s;
                startFilter(s);
                SearchView.this.onTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    in();
                } else {
                    out();
                }
            }
        });
    }

    private void initStyle(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray attr = mContext.obtainStyledAttributes(attrs, R.styleable.SearchView, defStyleAttr, defStyleRes);
        if (attr != null) {
            if (attr.hasValue(R.styleable.SearchView_search_version)) {
                setVersion(attr.getInt(R.styleable.SearchView_search_version, SearchCodes.VERSION_TOOLBAR));
            }
            if (attr.hasValue(R.styleable.SearchView_search_style)) {
                setStyle(attr.getInt(R.styleable.SearchView_search_style, SearchCodes.STYLE_TOOLBAR_CLASSIC));
            }
            if (attr.hasValue(R.styleable.SearchView_search_theme)) {
                setTheme(attr.getInt(R.styleable.SearchView_search_theme, SearchCodes.THEME_LIGHT));
            }
            if (attr.hasValue(R.styleable.SearchView_search_divider)) {
                setDivider(attr.getBoolean(R.styleable.SearchView_search_divider, false));
            }
            if (attr.hasValue(R.styleable.SearchView_search_hint)) {
                setHint(attr.getString(R.styleable.SearchView_search_hint));
            }
            if (attr.hasValue(R.styleable.SearchView_search_hint_size)) {
                setHintSize(attr.getDimensionPixelSize(R.styleable.SearchView_search_hint_size, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_voice)) {
                setVoice(attr.getBoolean(R.styleable.SearchView_search_voice, false));
            }
            if (attr.hasValue(R.styleable.SearchView_search_voice_text)) {
                setVoiceText(attr.getString(R.styleable.SearchView_search_voice_text));
            }
            if (attr.hasValue(R.styleable.SearchView_search_animation_duration)) {
                setAnimationDuration(attr.getInt(R.styleable.SearchView_search_animation_duration, ANIMATION_DURATION));
            }
            if (attr.hasValue(R.styleable.SearchView_search_shadow_color)) {
                setShadowColor(attr.getColor(R.styleable.SearchView_search_shadow_color, 0));
            }
            attr.recycle();
        }
    }

    // parameters ----------------------------------------------------------------------------------
    public void setVersion(int version) {
        mVersion = version;

        CardView.LayoutParams params = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.WRAP_CONTENT
        );

        if (mVersion == SearchCodes.VERSION_TOOLBAR) {
            mIsSearchOpen = true;
            mEditText.clearFocus();

            int top = mContext.getResources().getDimensionPixelSize(R.dimen.search_toolbar_margin_top);
            int leftStart = mContext.getResources().getDimensionPixelSize(R.dimen.search_toolbar_margin_left);
            int rightEnd = mContext.getResources().getDimensionPixelSize(R.dimen.search_toolbar_margin_right);
            int bottom = 0;

            params.setMargins(leftStart, top, rightEnd, bottom);
        }

        if (mVersion == SearchCodes.VERSION_MENU_ITEM) {
            setVisibility(View.GONE);

            int top = mContext.getResources().getDimensionPixelSize(R.dimen.search_menu_item_margin_top);
            int leftStart = mContext.getResources().getDimensionPixelSize(R.dimen.search_menu_item_margin_left);
            int rightEnd = mContext.getResources().getDimensionPixelSize(R.dimen.search_menu_item_margin_right);
            int bottom = mContext.getResources().getDimensionPixelSize(R.dimen.search_menu_item_margin_bottom);

            params.setMargins(leftStart, top, rightEnd, bottom);
        }

        mCardView.setLayoutParams(params);
    }

    public void setStyle(int style) {
        if (mVersion == SearchCodes.VERSION_TOOLBAR) {
            if (style == SearchCodes.STYLE_TOOLBAR_CLASSIC) {
                mSearchArrow = new ArrowDrawable(mContext);
                mBackImageView.setImageDrawable(mSearchArrow);
                mVoiceImageView.setImageResource(R.drawable.search_ic_mic_black_24dp);
                mEmptyImageView.setImageResource(R.drawable.search_ic_clear_black_24dp);
            }
        }
        if (mVersion == SearchCodes.VERSION_MENU_ITEM) {
            if (style == SearchCodes.STYLE_MENU_ITEM_CLASSIC) {
                mBackImageView.setImageResource(R.drawable.search_ic_arrow_back_black_24dp);
                mVoiceImageView.setImageResource(R.drawable.search_ic_mic_black_24dp);
                mEmptyImageView.setImageResource(R.drawable.search_ic_clear_black_24dp);
            }
            if (style == SearchCodes.STYLE_MENU_ITEM_COLOR) {
                mBackImageView.setImageResource(R.drawable.search_ic_arrow_back_color_24dp);
                mVoiceImageView.setImageResource(R.drawable.search_ic_mic_color_24dp);
                mEmptyImageView.setImageResource(R.drawable.search_ic_clear_color_24dp);
            }
        }
        mStyle = style;
    }

    public void setTheme(int theme) {
        if (theme == SearchCodes.THEME_LIGHT) {
            if (mVersion == SearchCodes.VERSION_TOOLBAR) {
                mSearchArrow.setColor(ContextCompat.getColor(mContext, R.color.search_light_icon));
                mVoiceImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_light_icon));
                mEmptyImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_light_icon));
            }
            if (mVersion == SearchCodes.VERSION_MENU_ITEM) {
                if (mStyle == SearchCodes.STYLE_MENU_ITEM_CLASSIC) {
                    mBackImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_light_icon));
                    mVoiceImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_light_icon));
                    mEmptyImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_light_icon));
                }
            }
            mRecyclerView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.search_light_background));
            mCardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.search_light_background));
            mEditText.setTextColor(ContextCompat.getColor(mContext, R.color.search_light_text));
            mEditText.setHintTextColor(ContextCompat.getColor(mContext, R.color.search_light_text_hint));
        }

        if (theme == SearchCodes.THEME_DARK) {
            if (mVersion == SearchCodes.VERSION_TOOLBAR) {
                mSearchArrow.setColor(ContextCompat.getColor(mContext, R.color.search_dark_icon));
                mVoiceImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_dark_icon));
                mEmptyImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_dark_icon));
            }
            if (mVersion == SearchCodes.VERSION_MENU_ITEM) {
                if (mStyle == SearchCodes.STYLE_MENU_ITEM_CLASSIC) {
                    mBackImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_dark_icon));
                    mVoiceImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_dark_icon));
                    mEmptyImageView.setColorFilter(ContextCompat.getColor(mContext, R.color.search_dark_icon));
                }
            }
            mRecyclerView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.search_dark_background));
            mCardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.search_dark_background));
            mEditText.setTextColor(ContextCompat.getColor(mContext, R.color.search_dark_text));
            mEditText.setHintTextColor(ContextCompat.getColor(mContext, R.color.search_dark_text_hint));
        }
    }

    public void setDivider(boolean divider) {
        if (divider) {
            mRecyclerView.addItemDecoration(new SearchDivider(mContext));
        } else {
            mRecyclerView.removeItemDecoration(new SearchDivider(mContext));
        }
    }

    public void setHint(CharSequence hint) {
        mEditText.setHint(hint);
    }

    public void setHint(@StringRes int hint) {
        mEditText.setHint(hint);
    }

    public void setHintSize(float size) {
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void setVoice(boolean voice) {
        mVoice = voice;
        if (voice && isVoiceAvailable()) {
            mVoiceImageView.setVisibility(View.VISIBLE);
        } else {
            mVoiceImageView.setVisibility(View.GONE);
        }
    }

    public void setVoice(boolean voice, Activity context) {
        mActivity = context;
        setVoice(voice);
    }

    public void setVoice(boolean voice, Fragment context) {
        mFragment = context;
        setVoice(voice);
    }

    public void setVoiceText(String voice_text) {
        VOICE_SEARCH_TEXT = voice_text;
    }

    public void setAnimationDuration(int animation_duration) {
        ANIMATION_DURATION = animation_duration;
    }

    public void setShadowColor(int color) {
        mShadow.setBackgroundColor(color);
    }

    // public --------------------------------------------------------------------------------------
    public void show(boolean animate) {
        setVisibility(View.VISIBLE);

        mEditText.requestFocus();
        mEditText.setText(null);

        mIsSearchOpen = true;

        if (mVersion == SearchCodes.VERSION_MENU_ITEM) {
            if (animate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    revealInAnimation();
                } else {
                    SearchAnimator.fadeInAnimation(mCardView, ANIMATION_DURATION);
                }
            } else {
                mCardView.setVisibility(View.VISIBLE);
            }
            if (mSearchViewListener != null) {
                mSearchViewListener.onSearchViewShown();
            }
        }
    }

    public void hide(boolean animate) {
        mEditText.clearFocus();
        mEditText.setText(null);

        mIsSearchOpen = false;

        if (mVersion == SearchCodes.VERSION_MENU_ITEM) {
            if (animate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    SearchAnimator.revealOutAnimation(mContext, mCardView, ANIMATION_DURATION);
                } else {
                    SearchAnimator.fadeOutAnimation(mCardView, ANIMATION_DURATION);
                }
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setVisibility(View.GONE);
                        if (mSearchViewListener != null) {
                            mSearchViewListener.onSearchViewClosed();
                        }
                    }
                }, ANIMATION_DURATION);
            } else {
                setVisibility(View.GONE);
                if (mSearchViewListener != null) {
                    mSearchViewListener.onSearchViewClosed();
                }
            }
        }
    }

    public boolean isSearchOpen() {
        return mIsSearchOpen;
    }

    public void setQuery(CharSequence query) {
        mEditText.setText(query);
        if (query != null) {
            mEditText.setSelection(mEditText.length());
            mUserQuery = query;
        }
        if (!TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    public void setAdapter(SearchAdapter adapter) {
        mSearchAdapter = adapter;
        mRecyclerView.setAdapter(adapter);
        startFilter(mEditText.getText());
    }

    // private -------------------------------------------------------------------------------------
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void revealInAnimation() {
        mCardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mCardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                SearchAnimator.revealInAnimation(mContext, mCardView, ANIMATION_DURATION);
            }
        });
    }

    private void showSuggestions() {
        if (mSearchAdapter != null && mSearchAdapter.getItemCount() > 0 && mRecyclerView.getVisibility() == View.GONE) {
            mDivider.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
            mRecyclerView.setAlpha(0.0f);
            mRecyclerView.animate().alpha(1.0f);
        }
    }

    private void hideSuggestions() {
        if (mRecyclerView.getVisibility() == View.VISIBLE) {
            mRecyclerView.setVisibility(View.GONE);
            mDivider.setVisibility(View.GONE);
        }
    }

    private void in() {
        showKeyboard();
        showSuggestions();
        mShadow.setVisibility(View.VISIBLE);
        if (mSearchArrow != null && mVersion == SearchCodes.VERSION_TOOLBAR) {
            mSearchArrow.setVerticalMirror(false);
            mSearchArrow.animate(ArrowDrawable.STATE_ARROW);
            mIsSearchArrowHamburgerState = ArrowDrawable.STATE_ARROW;
        }
    }

    public void out() {
        hideKeyboard();
        hideSuggestions();
        mShadow.setVisibility(View.GONE);
        if (mSearchArrow != null && mVersion == SearchCodes.VERSION_TOOLBAR) {
            mSearchArrow.setVerticalMirror(true);
            mSearchArrow.animate(ArrowDrawable.STATE_HAMBURGER);
            mIsSearchArrowHamburgerState = ArrowDrawable.STATE_HAMBURGER;
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditText, 0);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

    private boolean isVoiceAvailable() {
        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        return activities.size() != 0;
    }

    private void onVoiceClicked() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, VOICE_SEARCH_TEXT);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        if (mActivity != null) {
            mActivity.startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } else if (mFragment != null) {
            mFragment.startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } else {
            if (mContext instanceof Activity) {
                ((Activity) mContext).startActivityForResult(intent, SPEECH_REQUEST_CODE);
            }
        }
    }

    private void onSubmitQuery() {
        CharSequence query = mEditText.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                mEditText.setText(null);
            }
        }
    }

    private void startFilter(CharSequence s) {
        if (mSearchAdapter != null) {
            (mSearchAdapter).getFilter().filter(s, this);
        }
    }

    private void checkVoiceStatus(boolean status) {
        if (mVoice && status && isVoiceAvailable()) {
            mVoiceImageView.setVisibility(View.VISIBLE);
        } else {
            mVoiceImageView.setVisibility(View.GONE);
        }
    }

    private void onTextChanged(CharSequence newText) {
        CharSequence text = mEditText.getText();
        mUserQuery = text;
        boolean hasText = !TextUtils.isEmpty(text);

        if (hasText) {
            mEmptyImageView.setVisibility(View.VISIBLE);
            checkVoiceStatus(false);
        } else {
            mEmptyImageView.setVisibility(View.GONE);
            checkVoiceStatus(true);
        }

        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
    }

    // interfaces ----------------------------------------------------------------------------------
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    public void setOnSearchViewListener(SearchViewListener listener) {
        mSearchViewListener = listener;
    }

    public void setOnSearchMenuListener(SearchMenuListener listener) {
        mSearchMenuListener = listener;
    }

    // implements ----------------------------------------------------------------------------------
    @Override
    public void onFilterComplete(int text) {
        if (text > 0) {
            showSuggestions();
        } else {
            hideSuggestions();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBackImageView || v == mShadow) {

            if (mVersion == SearchCodes.VERSION_TOOLBAR) {
                if (mIsSearchArrowHamburgerState == ArrowDrawable.STATE_HAMBURGER) {
                    mSearchMenuListener.onMenuClick();
                } else {
                    hide(false);
                }
            }

            if (mVersion == SearchCodes.VERSION_MENU_ITEM) {
                hide(true);
            }
        }

        if (v == mVoiceImageView) {
            onVoiceClicked();
        }

        if (v == mEmptyImageView) {
            mEditText.setText(null);
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        mSavedState = new SavedState(superState);
        mSavedState.query = mUserQuery != null ? mUserQuery.toString() : null;
        mSavedState.isSearchOpen = mIsSearchOpen;
        return mSavedState;
    }

    // TODO http://onegullibull.com/WP-OneGulliBull/?p=252
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        mSavedState = (SavedState) state;
        if (mSavedState.isSearchOpen) {
            show(true);
            setQuery(mSavedState.query);
        }
        super.onRestoreInstanceState(mSavedState.getSuperState());
    }

    public interface OnQueryTextListener {
        boolean onQueryTextSubmit(String query);

        boolean onQueryTextChange(String newText);
    }

    public interface SearchViewListener {
        void onSearchViewShown();

        void onSearchViewClosed();
    }

    public interface SearchMenuListener {
        void onMenuClick();
    }

    // class ---------------------------------------------------------------------------------------
    private static class SavedState extends View.BaseSavedState {
        // TODO EDIT TEXT FOCUS ON ROTATION
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        String query;
        boolean isSearchOpen;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.query = in.readString();
            this.isSearchOpen = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(query);
            out.writeInt(isSearchOpen ? 1 : 0);
        }

    }

}