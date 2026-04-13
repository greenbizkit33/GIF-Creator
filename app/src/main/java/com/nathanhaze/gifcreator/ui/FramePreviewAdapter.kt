package com.nathanhaze.gifcreator.ui

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.nathanhaze.gifcreator.R

/**
 * Shows bitmap thumbnails in a horizontal list.
 * Pass [onRemove] to display an × button on each item (per-frame editing, item 6).
 * Pass null for a read-only preview strip (item 1).
 */
class FramePreviewAdapter(
    private val frames: MutableList<Bitmap>,
    private val onRemove: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<FramePreviewAdapter.FrameHolder>() {

    inner class FrameHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.iv_frame_thumb)
        val removeBtn: ImageView = view.findViewById(R.id.iv_frame_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_frame_thumb, parent, false)
        return FrameHolder(view)
    }

    override fun onBindViewHolder(holder: FrameHolder, position: Int) {
        holder.thumbnail.setImageBitmap(frames[position])
        if (onRemove != null) {
            holder.removeBtn.visibility = View.VISIBLE
            holder.removeBtn.setOnClickListener {
                val pos = holder.absoluteAdapterPosition
                if (pos != RecyclerView.NO_ID.toInt()) {
                    frames.removeAt(pos)
                    notifyItemRemoved(pos)
                    notifyItemRangeChanged(pos, frames.size)
                    onRemove.invoke(pos)
                }
            }
        } else {
            holder.removeBtn.visibility = View.GONE
        }
    }

    override fun getItemCount() = frames.size

    fun updateFrames(newFrames: List<Bitmap>) {
        frames.clear()
        frames.addAll(newFrames)
        notifyDataSetChanged()
    }
}
