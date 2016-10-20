package com.klinker.android.twitter_l.views.widgets;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

public class ImageKeyboardEditText extends FontPrefEditText {

    private InputConnectionCompat.OnCommitContentListener commitContentListener;

    public ImageKeyboardEditText(Context context) {
        super(context);
    }

    public ImageKeyboardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageKeyboardEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCommitContentListener(InputConnectionCompat.OnCommitContentListener listener) {
        this.commitContentListener = listener;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection con = super.onCreateInputConnection(outAttrs);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            outAttrs.contentMimeTypes = new String[]{ "image/gif", "image/jpeg",
                    "image/jpg", "image/png", "video/mp4" };
        }

        return InputConnectionCompat.createWrapper(con, outAttrs, new InputConnectionCompat.OnCommitContentListener() {
            @Override
            public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
                if (commitContentListener != null) {
                    return commitContentListener.onCommitContent(inputContentInfo, flags, opts);
                } else {
                    return false;
                }
            }
        });
    }

}
