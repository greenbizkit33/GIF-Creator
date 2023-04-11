package com.nathanhaze.gifcreator.gallery

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.SimpleTarget
import com.google.android.gms.ads.AdView
import com.nathanhaze.gifcreator.R
import java.io.File

/**
 * A simple [Fragment] subclass.
 */
class PictureFragment : Fragment() {
    private var parentView: LinearLayout? = null
    private val ad: AdView? = null
    private var filePath: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.picture, null)
        parentView = view.findViewById(R.id.picture_layout)
        if (filePath == null || filePath!!.isEmpty()) {
            Toast.makeText(
                view.context,
                resources.getString(R.string.sorry_wrong),
                Toast.LENGTH_LONG
            ).show()
            return view
        }
        val currentimg = filePath?.let { File(it) } //null
        if (currentimg == null || !currentimg.exists()) return view
        val gifImageView = view.findViewById<ImageView>(R.id.tv_gif)
        Glide.with(this).asGif().load(currentimg).into(gifImageView)

        return view
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (ad != null) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ad.visibility = View.GONE
            } else {
                ad.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(path: String?): PictureFragment {
            val fragment = PictureFragment()
            fragment.filePath = path
            return fragment
        }
    }
}