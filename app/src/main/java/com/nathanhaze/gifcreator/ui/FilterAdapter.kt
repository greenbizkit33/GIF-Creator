package com.nathanhaze.gifcreator.ui

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.nathanhaze.gifcreator.R
import com.zomato.photofilters.imageprocessors.Filter

internal class FilterAdapter(private var filterList: List<Filter>, private var sample: Bitmap?) :
    RecyclerView.Adapter<FilterAdapter.MyViewHolder>() {

    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.tv_filter_title)
        var image: ImageView = view.findViewById(R.id.iv_filter_image)
    }

    @NonNull
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_filter_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (position == 0) {
            holder.title.text = "no filter"
        } else {
            val filter = filterList[position]
            holder.title.text = filter.name
            holder.image.setImageBitmap(filter.processFilter(sample))
        }
    }

    override fun getItemCount(): Int {
        return filterList.size + 1
    }
}