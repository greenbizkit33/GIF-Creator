package com.nathanhaze.gifcreator.activity

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import com.bumptech.glide.Glide
import com.nathanhaze.gifcreator.GifCreatorApp
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.event.GifCreationEvent
import com.nathanhaze.gifcreator.manager.ImageUtil
import com.nathanhaze.gifcreator.manager.Utils
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GifCreatorActivity : AppCompatActivity() {


    private val PERMISSION_EXTRCT = 0

    lateinit var progressbar: ProgressBar

    lateinit var gifImage: ImageView

    val executorService: ExecutorService = Executors.newFixedThreadPool(4)
    val mainThreadHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif_creator)
        progressbar = findViewById<View>(R.id.progress_circular) as ProgressBar
        gifImage = findViewById<View>(R.id.iv_gif) as ImageView

        extractPermission()
    }

    private fun getImages() {
        val filePath = Utils.getVideoPath(this)
        progressbar.visibility = View.VISIBLE
        Executors.newSingleThreadExecutor().execute {
            val frameList = ArrayList<String>()
            val mediaRetriever: MediaMetadataRetriever = MediaMetadataRetriever()
            mediaRetriever.setDataSource(filePath)

            val extractionType = MediaMetadataRetriever.OPTION_CLOSEST

            val videoLength = 9
            var currentFrame = 0L
            while (currentFrame < videoLength) {
                val bitmap = mediaRetriever.getFrameAtTime(
                    TimeUnit.MILLISECONDS.toMicros(currentFrame),
                    extractionType
                )
                bitmap?.let {
                    frameList.add(ImageUtil.saveBitmap(bitmap, this))
                }
                currentFrame++
            }

            runOnUiThread {
                frameList?.let {
                    ImageUtil.saveGif(frameList, this)
                    progressbar.visibility = View.GONE
                }
            }
        }
    }

    @Subscribe
    fun onEvent(event : GifCreationEvent) {
        Glide.with(this).asGif().load(event.filePath).into(gifImage)
    }

    fun extractPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_EXTRCT
                )
            } else {
                getImages()
            }
        } else {
            getImages()
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_EXTRCT -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    getImages()
                }
            }
        }
    }
}