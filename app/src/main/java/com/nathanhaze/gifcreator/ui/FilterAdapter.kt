package com.nathanhaze.gifcreator.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.manager.Utils
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
        var selected: Boolean = false
    }

    private var lastSelected = 0
    private lateinit var lastHolder: RecyclerView.ViewHolder

    @NonNull
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_filter_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.image.setOnClickListener {
//            holder.title.setBackgroundColor(
//                context.resources.getColor(
//                    R.color.accent
//                )
//            )
            holder.title.typeface = Typeface.DEFAULT_BOLD
            val filter = filterList[holder.absoluteAdapterPosition]
            Utils.filter = filter
            this.notifyItemChanged(lastSelected)
            lastSelected = holder.absoluteAdapterPosition
            holder.selected = true
            holder.image.setColorFilter(R.color.blue_btn_bg_color, PorterDuff.Mode.SRC_OVER)
        }
        if (holder.selected) {
            holder.title.typeface = Typeface.DEFAULT_BOLD
        }
        if (position == 0) {
            holder.title.text = "no filter"
            holder.image.setImageBitmap(
                sample
            )
        } else {
            val filter = filterList[position - 1]
            holder.title.text = filter.name
            try {
                holder.image.setImageBitmap(
                    filter.processFilter(
                        sample?.copy(
                            sample?.config,
                            true
                        )
                    )
                )
            } catch (ex: Exception) {

            }
        }
        holder.title.typeface = Typeface.DEFAULT

    }

    override fun getItemCount(): Int {
        return filterList.size + 1
    }
}