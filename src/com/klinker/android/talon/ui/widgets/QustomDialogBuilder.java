package com.klinker.android.talon.ui.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.klinker.android.talon.R;

public class QustomDialogBuilder extends AlertDialog.Builder{

        /** The custom_body layout */
        private View mDialogView;
        
        /** optional dialog title layout */
        private TextView mTitle;

        public HoloEditText text;

        /** The colored holo divider. You can set its color with the setDividerColor method */
        private View mDivider;
        
    public QustomDialogBuilder(Context context) {
        super(context);

        mDialogView = View.inflate(context, R.layout.qustom_dialog_layout, null);
        setView(mDialogView);

        mTitle = (TextView) mDialogView.findViewById(R.id.alertTitle);
        mDivider = mDialogView.findViewById(R.id.titleDivider);
        text = (HoloEditText) mDialogView.findViewById(R.id.content);
    }

    /** 
     * Use this method to color the divider between the title and content.
     * Will not display if no title is set.
     */
    public QustomDialogBuilder setDividerColor(int color) {
            mDivider.setBackgroundColor(color);
            return this;
    }
 
    @Override
    public QustomDialogBuilder setTitle(CharSequence text) {
        mTitle.setText(text);
        return this;
    }

    public QustomDialogBuilder setTitleColor(int color) {
            mTitle.setTextColor(color);
            return this;
    }
    
    @Override
    public AlertDialog show() {
        if (mTitle.getText().equals("")) mDialogView.findViewById(R.id.topPanel).setVisibility(View.GONE);
        return super.show();
    }

}