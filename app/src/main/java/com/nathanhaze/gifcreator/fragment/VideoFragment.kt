package com.nathanhaze.gifcreator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.manager.Utils

class VideoFragment : Fragment() {

    lateinit var videoView: VideoView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video, container, false)

        videoView = view.findViewById(R.id.video_view)
        videoView.setVideoPath(activity?.let { Utils.getVideoPath(it) })

        videoView.setOnPreparedListener { mp ->
            mp.setVolume(0f, 0f)
            val mediaController = MediaController(context)
            mediaController.setPadding(0, 0, 0, 5)
            videoView.setMediaController(mediaController)
            mediaController.setAnchorView(videoView)

            val videoWidth = mp.videoWidth
            val videoHeight = mp.videoHeight
            if (videoWidth > 0 && videoHeight > 0) {
                videoView.post {
                    val availableWidth = videoView.width.takeIf { it > 0 } ?: return@post
                    val maxHeightPx = (300 * resources.displayMetrics.density).toInt()
                    val scaledHeight = (availableWidth * videoHeight.toFloat() / videoWidth).toInt()
                    videoView.layoutParams = videoView.layoutParams.also {
                        it.height = scaledHeight.coerceAtMost(maxHeightPx)
                    }
                }
            }
        }

        videoView.start()
        return view
    }
}
