package com.klinker.android.talon.Utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.TypedValue;
import com.klinker.android.talon.R;
import com.squareup.picasso.Transformation;

/**
 * Created with IntelliJ IDEA.
 * User: luke
 * Date: 11/12/13
 * Time: 10:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class DarkenTransform implements Transformation {

    private Context mContext;

    public DarkenTransform(Context context) {
        mContext = context;
    }
    @Override
    public Bitmap transform(Bitmap image) {

        RenderScript rs = RenderScript.create(mContext);
        Bitmap blurred = image;
        Allocation input = Allocation.createFromBitmap(rs, image, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation output = Allocation.createTyped(rs, input.getType());
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, mContext.getResources().getDisplayMetrics()));
        script.setInput(input);
        script.forEach(output);
        output.copyTo(blurred);

        return blurred;
    }

    @Override
    public String key() {
        return "darken()";
    }
}
