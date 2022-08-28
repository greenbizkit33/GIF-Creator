package com.nathanhaze.gifcreator.gallery

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.event.ImageSelectedEvent
import com.nathanhaze.gifcreator.gallery.PhotoGalleryAdapter.PhotoHolder
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * Created by nathan on 6/24/2016.
 */
class PhotoGalleryAdapter(private var dataSet: List<String?>?) : RecyclerView.Adapter<PhotoHolder>() {
    private var recyclerView: RecyclerView? = null
    var imagesSelected = ArrayList<String?>()
        private set

    inner class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView
        val checkBox: ImageView
        val imageWrapper: FrameLayout
        var filePath: String? = null
        private var isChecked = false

        init {
            imageView = view.findViewById(R.id.iv_photo_cell)
            checkBox = view.findViewById(R.id.cb_photo_selection)
            imageWrapper = view.findViewById(R.id.fl_image_wrapper)
            checkBox.setOnClickListener { _: View? ->
                isChecked = !isChecked
                if (isChecked) {
                    EventBus.getDefault().post(ImageSelectedEvent(true))
                    val params = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(8, 8, 8, 8)
                    imageView.layoutParams = params
                    imagesSelected.add(filePath)
                    checkBox.setImageResource(R.drawable.ic_checked)
                    imageWrapper.foreground =
                        imageWrapper.resources.getDrawable(R.drawable.image_overlay)
                } else {
                    imagesSelected.remove(filePath)
                    if (imagesSelected.isEmpty()) {
                        EventBus.getDefault().post(ImageSelectedEvent(false))
                    }
                    imageWrapper.foreground = null
                    val params = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 0)
                    imageView.layoutParams = params
                    checkBox.setImageResource(R.drawable.ic_check)
                }
            }
            imageView.setOnClickListener { v: View? ->
                val intent = Intent(recyclerView!!.context, PhotoPagerActivity::class.java)
                intent.putExtra("path", filePath)
                recyclerView!!.context.startActivity(intent)
            }
        }
    }

    fun setDataSet(dataSet: List<String?>?) {
        this.dataSet = dataSet
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_photo_cell, parent, false)
        return PhotoHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
        val path = dataSet?.get(position)
        if (path != null) {
            if (path.isEmpty()) return
        }
        val fileImage = File(path)
        if (!fileImage.exists()) return
        Glide.with(recyclerView!!.context).load(fileImage).into(holder.imageView)
        holder.filePath = path
        if (imagesSelected.contains(path)) {
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 8, 8, 8)
            holder.imageView.layoutParams = params
            holder.checkBox.setImageResource(R.drawable.ic_checked)
            holder.imageWrapper.foreground =
                holder.imageView.resources.getDrawable(R.drawable.image_overlay)
        } else {
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 0)
            holder.imageView.layoutParams = params
            holder.imageWrapper.foreground = null
            holder.checkBox.setImageResource(R.drawable.ic_check)
        }
    }

    override fun getItemCount(): Int {
        if (dataSet != null) {
            return dataSet!!.size
        }
        return 0;
    }

    val selectedCount: Int
        get() = imagesSelected.size

    fun setImageCheckedMap(list: ArrayList<String?>?) {
        if (list == null) {
            return
        }
        imagesSelected = list
    }
}