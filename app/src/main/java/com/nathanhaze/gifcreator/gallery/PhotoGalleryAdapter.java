package com.nathanhaze.gifcreator.gallery;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nathanhaze.gifcreator.R;
import com.nathanhaze.gifcreator.event.ImageSelectedEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by nathan on 6/24/2016.
 */
public class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoGalleryAdapter.PhotoHolder> {

    private RecyclerView recyclerView;
    private List<String> dataSet;
    private ArrayList<String> imageCheckedMap = new ArrayList<>();

    class PhotoHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final ImageView checkBox;
        private final FrameLayout imageWrapper;
        private String filePath;

        private boolean isChecked;

        PhotoHolder(View view) {
            super(view);
            this.imageView = (ImageView) view.findViewById(R.id.iv_photo_cell);
            this.checkBox = (ImageView) view.findViewById(R.id.cb_photo_selection);
            this.imageWrapper = (FrameLayout) view.findViewById(R.id.fl_image_wrapper);
            this.checkBox.setOnClickListener(v -> {
                isChecked = !isChecked;
                if (isChecked) {
                    EventBus.getDefault().post(new ImageSelectedEvent(true));
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(8, 8, 8, 8);
                    imageView.setLayoutParams(params);
                    imageCheckedMap.add(filePath);
                    checkBox.setImageResource(R.drawable.ic_checked);
                    imageWrapper.setForeground(imageWrapper.getResources().getDrawable(R.drawable.image_overlay));
                } else {
                    imageCheckedMap.remove(filePath);
                    if (imageCheckedMap.isEmpty()) {
                        EventBus.getDefault().post(new ImageSelectedEvent(false));
                    }
                    imageWrapper.setForeground(null);
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 0);
                    imageView.setLayoutParams(params);
                    checkBox.setImageResource(R.drawable.ic_check);
                }
            });
            this.imageView.setOnClickListener(v -> {
                Intent intent;
                intent = new Intent(recyclerView.getContext(), PhotoPagerActivity.class);
                intent.putExtra("path", filePath);
                recyclerView.getContext().startActivity(intent);
            });
        }
    }

    public PhotoGalleryAdapter(List<String> dataSet) {
        this.dataSet = dataSet;
    }

    public void setDataSet(List<String> dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_photo_cell, parent, false);
        PhotoHolder holder = new PhotoHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(PhotoHolder holder, int position) {
        String path = dataSet.get(position);
        if (path == null || path.isEmpty()) return;
        File fileImage = new File(path);
        if (!fileImage.exists()) return;
        Glide.with(recyclerView.getContext()).load(fileImage).into(holder.imageView);
        holder.filePath = path;
        if (imageCheckedMap.contains(path)) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            holder.imageView.setLayoutParams(params);
            holder.checkBox.setImageResource(R.drawable.ic_checked);
            holder.imageWrapper.setForeground(holder.imageView.getResources().getDrawable(R.drawable.image_overlay));
        } else {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 0);
            holder.imageView.setLayoutParams(params);
            holder.imageWrapper.setForeground(null);
            holder.checkBox.setImageResource(R.drawable.ic_check);
        }
    }


    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public int getSelectedCount() {
        return imageCheckedMap.size();
    }

    public ArrayList<String> getImagesSelected() {
        return imageCheckedMap;
    }

    public void setImageCheckedMap(ArrayList<String> list) {
        if (list == null) {
            return;
        }
        imageCheckedMap = list;
    }
}
