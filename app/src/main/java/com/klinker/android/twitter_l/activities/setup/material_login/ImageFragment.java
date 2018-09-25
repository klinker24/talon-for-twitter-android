package com.klinker.android.twitter_l.activities.setup.material_login;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.Utils;

public class ImageFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_DESC = "desc";
    private static final String ARG_IMAGE_URL = "drawable";
    private static final String ARG_COLOUR = "colour";

    public static ImageFragment newInstance(String title, String description, String imageUrl, int colour) {
        ImageFragment sampleSlide = new ImageFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, description);
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putInt(ARG_COLOUR, colour);
        sampleSlide.setArguments(args);

        return sampleSlide;
    }

    private int colour;
    private String title, description, drawable;

    public ImageFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null && getArguments().size() != 0){
            drawable = getArguments().getString(ARG_IMAGE_URL);
            title = getArguments().getString(ARG_TITLE);
            description = getArguments().getString(ARG_DESC);
            colour = getArguments().getInt(ARG_COLOUR);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_intro, container, false);
        TextView t = (TextView) v.findViewById(R.id.title);
        TextView d = (TextView) v.findViewById(R.id.description);
        ImageView i = (ImageView) v.findViewById(R.id.image);
        LinearLayout m = (LinearLayout) v.findViewById(R.id.main);
        t.setText(title);
        d.setText(description);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) i.getLayoutParams();
        params.height = Utils.toDP(196, getActivity());
        params.width = params.height;
        i.setLayoutParams(params);

        Glide.with(this).load(drawable).into(i);

        m.setBackgroundColor(colour);
        return v;
    }

}