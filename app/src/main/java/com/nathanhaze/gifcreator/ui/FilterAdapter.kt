package com.nathanhaze.gifcreator.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.nathanhaze.gifcreator.R
import com.zomato.photofilters.imageprocessors.Filter

internal class FilterAdapter(
    private var filterList: List<Filter>,
    private var sample: Bitmap?,
    private var context: Context
) :
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
        Log.d("nathanx", "onbind view holder " + position)
        holder.image.setOnClickListener {
            holder.title.setBackgroundColor(
                context.resources.getColor(
                    R.color.accent
                )
            )
        }
        if (position == 0) {
            holder.title.text = "no filter"
            holder.image.setImageBitmap(
                sample
            )
        } else {
            val filter = filterList[position - 1]
            holder.title.text = filter.name
            Log.d("nathanx", "onbind view holder " + filter.name)
            try {
                holder.image.setImageBitmap(
                    filter.processFilter(
                        sample?.copy(
                            sample?.config,
                            true
                        )
                    )
                )
                //  holder.image.setImageBitmap(JavaUtil.doFilter(filter, sample))
            } catch (ex: Exception) {

            }
        }

    }

    override fun getItemCount(): Int {
        return filterList.size + 1
    }
}