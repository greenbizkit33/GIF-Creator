package com.nathanhaze.gifcreator.gallery;


import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdView;
import com.nathanhaze.gifcreator.R;

import java.io.File;

/**
 * A simple {@link Fragment} subclass.
 */
public class PictureFragment extends Fragment {


    private LinearLayout parentView;
    private AdView ad;
    private String filePath;

    public PictureFragment() {
        // Required empty public constructor
    }

    public static PictureFragment newInstance(String path) {
        PictureFragment fragment = new PictureFragment();
        fragment.filePath = path;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.picture, null);
        parentView = (LinearLayout) view.findViewById(R.id.picture_layout);

        if (null == savedInstanceState) {
            // set you initial fragment object
        }
        if (filePath == null || filePath.isEmpty()) {
            Toast.makeText(view.getContext(), getResources().getString(R.string.sorry_wrong), Toast.LENGTH_LONG).show();
            return view;
        }
        File currentimg = new File(filePath); //null

        if (currentimg == null || !currentimg.exists()) return view;

        ImageView gifImageView = (ImageView) view.findViewById(R.id.tv_gif);
        Glide.with(this).asGif().load(currentimg).into(gifImageView);

        return view;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (ad != null) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ad.setVisibility(View.GONE);
            } else {
                ad.setVisibility(View.VISIBLE);
            }
        }
    }
}
