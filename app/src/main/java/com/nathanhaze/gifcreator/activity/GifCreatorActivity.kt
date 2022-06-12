package com.nathanhaze.gifcreator.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.HandlerCompat
import com.bumptech.glide.Glide
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.event.GifCreationEvent
import com.nathanhaze.gifcreator.event.ProgressUpdateEvent
import com.nathanhaze.gifcreator.manager.ImageUtil
import com.nathanhaze.gifcreator.manager.Utils
import mehdi.sakout.fancybuttons.FancyButton
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GifCreatorActivity : AppCompatActivity() {


    private var gifFile: File? = null
    private val PERMISSION_EXTRCT = 0

    lateinit var progressbar: ProgressBar

    lateinit var gifImage: ImageView
    lateinit var llSelection: LinearLayout

    lateinit var btnShare: FancyButton
    lateinit var btnStartOver: FancyButton
    lateinit var tvProgress: TextView

    val executorService: ExecutorService = Executors.newFixedThreadPool(4)
    val mainThreadHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif_creator)
        progressbar = findViewById<View>(R.id.progress_circular) as ProgressBar
        gifImage = findViewById<View>(R.id.iv_gif) as ImageView
        llSelection = findViewById(R.id.ll_selection)
        btnShare = findViewById(R.id.button_share)
        btnStartOver = findViewById(R.id.button_start_over)

        tvProgress = findViewById(R.id.tv_progress)


        btnShare.setOnClickListener {
            share()
        }

        btnStartOver.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        extractPermission()
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    private fun getImages() {
        val filePath = Utils.getVideoPath(this)
        progressbar.visibility = View.VISIBLE
        Executors.newSingleThreadExecutor().execute {
            EventBus.getDefault().post(ProgressUpdateEvent("Starting up..."))
            var frameList = ArrayList<Bitmap>()
            val mediaRetriever: MediaMetadataRetriever = MediaMetadataRetriever()
            mediaRetriever.setDataSource(filePath)

            val extractionType = MediaMetadataRetriever.OPTION_CLOSEST

            val time: String? =
                mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val videoLengthMilli = time?.toLong()
            // val videoLengthSec = videoLengthMilli?.let { TimeUnit.MILLISECONDS.toSeconds(it) }

            //TODO  start here
            // val numberOfFrames = videoLengthMilli?.div(Utils.frameFrequencyMilli)

            var currentMilli = Utils.startTimeMilli
            val endMilli = Utils.endTimeMilli
            Log.d(
                "nathanx",
                "start " + currentMilli + " " + endMilli + " " + Utils.frameFrequencyMilli
            )

            while (currentMilli < endMilli) {
                EventBus.getDefault().post(
                    ProgressUpdateEvent(
                        "Grabbing image at milliseconds $currentMilli end time $endMilli"
                    )
                )


                var bitmap = mediaRetriever.getFrameAtTime(
                    TimeUnit.MILLISECONDS.toMicros(currentMilli.toLong()),
                    extractionType
                )

                bitmap = bitmap?.let {
                    Bitmap.createScaledBitmap(
                        it,
                        ((bitmap!!.getWidth() * Utils.size).toInt()),
                        ((bitmap!!.getHeight() * Utils.size).toInt()),
                        true
                    )
                };

                if (Utils.filter != null) {
                    bitmap = Utils.filter?.processFilter(
                        bitmap
                    )
                }
                bitmap?.let {
                    frameList.add(bitmap)
                }
                currentMilli = currentMilli + Utils.frameFrequencyMilli
            }

            runOnUiThread {
                frameList.let {
                    if (Utils.reverseOrder) {
                        frameList.reverse()
                    }
                    if (Utils.double) {
                        val temp = frameList.toArray()
                        temp.reverse()
                        frameList = (frameList + temp) as ArrayList<Bitmap>
                    }
                    ImageUtil.saveGif(frameList, this)
                }
            }
        }
    }

    @Subscribe
    fun onEvent(event: GifCreationEvent) {
        gifFile = event.filePath
        Glide.with(this).asGif().load(event.filePath).into(gifImage)
        progressbar.visibility = View.GONE
        llSelection.visibility = View.VISIBLE
        tvProgress.visibility = View.GONE
    }

    private fun extractPermission() {
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

    @Subscribe
    fun onEvent(event: ProgressUpdateEvent) {
        this.runOnUiThread {
            tvProgress.text = event.message
        }
    }


    fun share() {
        if (gifFile == null) {
            return
        }
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        shareIntent.type = "image/gif"
        val photoURI =
            FileProvider.getUriForFile(this, this.packageName + ".GenericFileProvider", gifFile!!)
        shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI)
        startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.share)))
    }
}