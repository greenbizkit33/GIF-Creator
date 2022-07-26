package com.nathanhaze.gifcreator.gallery;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.nathanhaze.gifcreator.GifCreatorApp;
import com.nathanhaze.gifcreator.R;
import com.nathanhaze.gifcreator.event.RefeshGalleryEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.Arrays;

public class PhotoPagerActivity extends FragmentActivity {

    static int index = 0;
    ViewPager2 viewPager;
    GifCreatorApp application;
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_pager);

        viewPager = findViewById(R.id.view_pager);

        application = (GifCreatorApp) this.getApplication();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        path = "";
        if (bundle != null) {
            path = bundle.getString("path");
        }

        if (path == null || path.isEmpty()) {
            return;
        }

        String[] fileList = application.getListFiles();

        if (fileList == null || fileList.length < 1) {
            return;
        }

        if (path != null) {
            index = Arrays.asList(fileList).indexOf(path);
        }

        viewPager.setAdapter(new ScreenSlidePagerAdapter(this, application.getListFiles()));

        viewPager.setCurrentItem(index);

        ImageView shareIcon = findViewById(R.id.iv_share);
        shareIcon.setOnClickListener(view -> share(fileList[viewPager.getCurrentItem()]));

        ImageView ivOpen = findViewById(R.id.iv_open);
        ivOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File photo = new File(path);

                Uri photoURI = FileProvider.getUriForFile(ivOpen.getContext(), ivOpen.getContext().getPackageName() + ".GenericFileProvider", photo);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //must provide
                intent.setData(photoURI);
                startActivity(intent);
            }
        });

        FragmentActivity act = this;
        ImageView deleteIcon = findViewById(R.id.iv_delete);
        deleteIcon.setOnClickListener(view -> {
            String[] temp = application.getListFiles();
            File file = null;
            try {
                file = new File(temp[viewPager.getCurrentItem()]);
            } catch (IndexOutOfBoundsException ex) {
                FirebaseCrashlytics.getInstance().recordException(ex);
                try {
                    Toast.makeText(deleteIcon.getContext(), getResources().getString(R.string.problem_deleting), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            }
            if (file == null) {
                Toast.makeText(deleteIcon.getContext(), getResources().getString(R.string.problem_deleting), Toast.LENGTH_LONG).show();
                return;
            }
            boolean deleted = file.delete();
            if (deleted) {
                deleteIcon.getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                String[] temp2 = application.getListFiles();
                viewPager.setAdapter(new ScreenSlidePagerAdapter(act, temp2));
                EventBus.getDefault().post(new RefeshGalleryEvent());
                int currentItem = viewPager.getCurrentItem();
                if (currentItem < temp2.length) {
                    viewPager.setCurrentItem(currentItem);
                } else {
                    finish();
                }

            } else {
                Toast.makeText(deleteIcon.getContext(), getResources().getString(R.string.problem_deleting), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == ORIENTATION_LANDSCAPE) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void share(String path) {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setType("image/jpeg");
        File photo = new File(path);
        Uri photoURI = FileProvider.getUriForFile(this, this.getPackageName() + ".GenericFileProvider", photo);
        shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI);
        startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share)));
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        private String[] pictures;

        public ScreenSlidePagerAdapter(FragmentActivity fa, String[] data) {
            super(fa);
            pictures = data;

        }

        @Override
        public Fragment createFragment(int position) {
            return PictureFragment.newInstance(pictures[position]);
        }

        public void setPictures(String[] pictures) {
            this.pictures = pictures;
        }

        @Override
        public int getItemCount() {
            return pictures.length;

        }
    }
}
