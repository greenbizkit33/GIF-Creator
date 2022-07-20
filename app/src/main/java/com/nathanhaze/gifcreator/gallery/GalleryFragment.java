package com.nathanhaze.gifcreator.gallery;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nathanhaze.gifcreator.GifCreatorApp;
import com.nathanhaze.gifcreator.R;
import com.nathanhaze.gifcreator.event.ImageSelectedEvent;
import com.nathanhaze.gifcreator.event.RefeshGalleryEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class GalleryFragment extends Fragment {

    private static final int CONTROLS_DISTANCE = 40;

    private RecyclerView recyclerview;
    private PhotoGalleryAdapter adapter;
    private TextView tvNoImages;
    private LinearLayout galleryControls;
    private ImageView trashButton;
    private GifCreatorApp application;
    private List<String> list;

    public String defaultImage;

    public GalleryFragment() {
        // Required empty public constructor
    }

    public GalleryFragment(String defaultImage) {
        this.defaultImage = defaultImage;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);


        AdView mAdView = (AdView) view.findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else {
            if (mAdView != null) {
                mAdView.setVisibility(View.GONE);
            }
        }

        application = (GifCreatorApp) getActivity().getApplication();

        GridLayoutManager layout = new GridLayoutManager(application.getApplicationContext(), 2);


        recyclerview = (RecyclerView) view.findViewById(R.id.rv_gallery);
        galleryControls = (LinearLayout) view.findViewById(R.id.ll_gallery_controls);
        galleryControls.setTranslationX(-CONTROLS_DISTANCE);
        trashButton = (ImageView) view.findViewById(R.id.iv_delete);
        ImageView ivUnSelectAll = (ImageView) view.findViewById(R.id.iv_unselect);
        ImageView tvSelectAll = (ImageView) view.findViewById(R.id.tv_select_all);
        list = Arrays.asList(application.getListFiles());

        tvNoImages = (TextView) view.findViewById(R.id.tv_no_images);

        if (list != null && list.size() != 0) {
            adapter = new PhotoGalleryAdapter(list);
            recyclerview.setAdapter(adapter);
            recyclerview.setLayoutManager(layout);

            if (defaultImage != null && !defaultImage.isEmpty()) {
                int index = 0;
                int i = 0;
                for (String s : list) {
                    if (defaultImage.equals(s)) {
                        index = i;
                        break;
                    }
                    i++;
                }
                final int indexImage = i;
                if (i > 1) {
                    recyclerview.getViewTreeObserver()
                            .addOnGlobalLayoutListener(
                                    new ViewTreeObserver.OnGlobalLayoutListener() {
                                        @Override
                                        public void onGlobalLayout() {
                                            if (recyclerview.getMeasuredHeight() > 0) {
                                                new Handler().postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        //  layout.scrollToPosition(20);
                                                        //  recyclerview.smoothScrollToPosition(10);
                                                        //  layout.scrollToPositionWithOffset(20, 0);
                                                        if (getActivity() == null) {
                                                            return;
                                                        }
                                                        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getActivity()) {
                                                            @Override
                                                            protected int getVerticalSnapPreference() {
                                                                return LinearSmoothScroller.SNAP_TO_START;
                                                            }
                                                        };

                                                        smoothScroller.setTargetPosition(indexImage);

                                                        layout.startSmoothScroll(smoothScroller);


                                                    }
                                                }, 100);
                                                recyclerview
                                                        .getViewTreeObserver()
                                                        .removeOnGlobalLayoutListener(this);
                                            }
                                        }
                                    });
                }
            }
            tvNoImages.setVisibility(View.GONE);
        } else {
            tvNoImages.setVisibility(View.VISIBLE);
        }

        tvSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> temp = new ArrayList<String>();
                for (String fileName : list) {
                    temp.add(fileName);
                }

                adapter.setImageCheckedMap(temp);
                adapter.notifyDataSetChanged();
            }
        });
        trashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(trashButton.getContext()).create();
                alertDialog.setCancelable(true);
                alertDialog.setMessage(getResources().getString(R.string.delete) + " " + adapter.getSelectedCount() + " " + getResources().getString(R.string.images).toLowerCase());
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<String> images = adapter.getImagesSelected();
                        for (String imagePath : images) {
                            File file = null;
                            try {
                                file = new File(imagePath);
                            } catch (IndexOutOfBoundsException ex) {

                            }
                            boolean deleted = file.delete();
                            if (deleted) {
                                trashButton.getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                            }
                        }
                        list = Arrays.asList(application.getListFiles());
                        if (list.size() > 0) {
                            adapter = new PhotoGalleryAdapter(list);
                            recyclerview.setAdapter(adapter);
                        } else {
                            recyclerview.setVisibility(View.GONE);
                            tvNoImages.setVisibility(View.VISIBLE);
                            galleryControls.setVisibility(View.GONE);
                            galleryControls.animate().translationY(-CONTROLS_DISTANCE);
                        }
                    }
                });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        });
        EventBus.getDefault().

                register(this);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        list = Arrays.asList(application.getListFiles());
        if (list != null && list.size() == 0) {
            tvNoImages.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Subscribe
    public void onEvent(ImageSelectedEvent event) {
        if (event.hasSelections) {
            // galleryControls.animate().setDuration(1000).
            galleryControls.setVisibility(View.VISIBLE);
        } else {
            //   galleryControls.animate().translationY(-CONTROLS_DISTANCE);
            galleryControls.setVisibility(View.GONE);
        }
    }

    @Subscribe
    public void onEvent(RefeshGalleryEvent event) {
        List<String> list = Arrays.asList(application.getListFiles());
        ((PhotoGalleryAdapter) recyclerview.getAdapter()).setDataSet(list);
        recyclerview.getAdapter().notifyDataSetChanged();
        if (list == null && list.size() == 0) {
            tvNoImages.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openImage(File photo) {
        if (getActivity() == null) {
            return;
        }
        Intent intent = new Intent(getActivity(), PhotoPagerActivity.class);
        intent.putExtra("path", photo.getAbsolutePath());
        startActivity(intent);
    }

}
