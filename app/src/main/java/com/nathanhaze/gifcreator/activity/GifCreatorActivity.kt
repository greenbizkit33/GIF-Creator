package com.nathanhaze.gifcreator.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.event.GifCreationEvent
import com.nathanhaze.gifcreator.event.ProgressUpdateEvent
import com.nathanhaze.gifcreator.manager.ImageUtil
import com.nathanhaze.gifcreator.manager.Utils
import com.nathanhaze.gifcreator.manager.Utils.isGettingImages
import mehdi.sakout.fancybuttons.FancyButton
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import pl.droidsonroids.gif.GifImageView
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GifCreatorActivity : AppCompatActivity() {


    private var gifFile: File? = null
    private val PERMISSION_EXTRCT = 0
    private var numberFrames = 0

    lateinit var progressbar: LinearProgressIndicator

    lateinit var gifImage: GifImageView
    lateinit var llSelection: LinearLayout

    lateinit var btnShare: FancyButton
    lateinit var btnStartOver: FancyButton
    lateinit var btnExternalOpen: FancyButton
    lateinit var mAdView: AdView
    lateinit var mAdViewLoading: AdView

    lateinit var tvProgress: TextView
    var stopThread = false
    var totalFrames: Int = 0

    //val executorService: ExecutorService = Executors.newFixedThreadPool(4)
    //val mainThreadHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())


    lateinit var service: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif_creator)
        progressbar = findViewById<View>(R.id.progress_circular) as LinearProgressIndicator
        gifImage = findViewById<View>(R.id.iv_gif) as GifImageView
        llSelection = findViewById(R.id.ll_selection)
        btnShare = findViewById(R.id.button_share)
        btnStartOver = findViewById(R.id.button_start_over)
        btnExternalOpen = findViewById(R.id.button_open_external)

        tvProgress = findViewById(R.id.tv_progress)


        btnExternalOpen.setOnClickListener {
            val photoURI = gifFile?.absoluteFile?.let { it1 ->
                FileProvider.getUriForFile(
                    applicationContext,
                    applicationContext.packageName + ".GenericFileProvider",
                    it1
                )
            }
            intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) //must provide

            intent.data = photoURI
            startActivity(intent)
        }

        btnShare.setOnClickListener {
            share()
        }

        btnStartOver.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        stopThread = false
        extractPermission()

        mAdViewLoading = findViewById<View>(R.id.adView_loading) as AdView
//        val adRequestLoading = AdRequest.Builder().build()
//        mAdViewLoading.loadAd(adRequestLoading)

        mAdView = findViewById<View>(R.id.adView) as AdView
    }

    override fun onResume() {
        super.onResume()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        Log.d("nathanx", "onresume " + Utils.lastGifFilePath + " " + isGettingImages)
        if (Utils.lastGifFilePath != null && !isGettingImages) {
            if (Utils.outOfMemory) {
                showDialog()
            } else {
                Glide.with(this).asGif().load(Utils.lastGifFilePath).into(gifImage)
            }
            Log.d("nathanx", "do your thing")
            progressbar.visibility = View.GONE
            llSelection.visibility = View.VISIBLE
            tvProgress.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("nathanx", "unreg")
        //EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("nathanx", "destory")
        //  service.shutdownNow()
        //  service.shutdown()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        stopThread = true
        Log.d("nathanx", "back pressed")
    }

    private fun getImages() {
//        if (this::service.isInitialized && service != null) {
//            Log.d("nathanx", "get images " + service.isShutdown + " " + service.isTerminated)
//        }
        if (isGettingImages) {
            if (Utils.lastGifFilePath != null) {
                Glide.with(this).asGif().load(Utils.lastGifFilePath).into(gifImage)
            }
            return
        }
        val filePath = Utils.getVideoPath(this)
        progressbar.visibility = View.VISIBLE
        service = Executors.newSingleThreadExecutor()

        totalFrames =
            ((Utils.endTimeMilli - Utils.startTimeMilli).div(Utils.frameFrequencyMilli.toFloat())).toInt()
        service.execute {
            Log.d("nathanx", "starting thread")
            isGettingImages = true
            EventBus.getDefault().post(ProgressUpdateEvent("Starting up...", 0, false, false))
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

            while (currentMilli < endMilli && !stopThread) {
                EventBus.getDefault().post(
                    ProgressUpdateEvent(
                        "",
                        currentMilli, true, false
                    )
                )


                var bitmap = mediaRetriever.getFrameAtTime(
                    TimeUnit.MILLISECONDS.toMicros(currentMilli.toLong()),
                    extractionType
                )

                bitmap = bitmap?.let {
                    Bitmap.createScaledBitmap(
                        it,
                        ((bitmap!!.width * Utils.size).toInt()),
                        ((bitmap!!.height * Utils.size).toInt()),
                        true
                    )
                }

                if (Utils.filter != null) {
                    bitmap = Utils.filter?.processFilter(
                        bitmap
                    )
                }
                bitmap?.let {
                    frameList.add(bitmap)
                }
//                Log.d("nathanx", "before  " + currentMilli + " " + Utils.frameFrequencyMilli);

                currentMilli += Utils.frameFrequencyMilli
                Log.d("nathanx", "after" + currentMilli + " " + stopThread)

            }

            if (!stopThread) {
//                runOnUiThread {
                frameList.let {
                    if (Utils.reverseOrder) {
                        frameList.reverse()
                    }
                    if (Utils.double) {
                        val temp = frameList.toArray()
                        temp.reverse()
                        frameList = (frameList + temp) as ArrayList<Bitmap>
                    }
                    numberFrames = frameList.size
                    ImageUtil.saveGif(frameList, this)
                    //     }
                }
            } else {
                isGettingImages = false
            }
        }
    }

    @Subscribe
    fun onEvent(event: GifCreationEvent) {
        Log.d("nathanx", "got gif")
        isGettingImages = false
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }
        gifFile = event.filePath
        runOnUiThread {
            if (Utils.outOfMemory) {
                showDialog()
            } else {
                Glide.with(this).asGif().load(event.filePath).into(gifImage)
            }
            progressbar.visibility = View.GONE
            llSelection.visibility = View.VISIBLE
            tvProgress.visibility = View.GONE
        }

      //  mAdViewLoading.visibility = View.GONE
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    private fun showDialog() {
        runOnUiThread {
            Log.d("nathanx", "show dialog")
            val alertDialog = AlertDialog.Builder(this)

            alertDialog.apply {
                //   setIcon(R.drawable.ic_hello)
                setTitle(getString(R.string.out_memory_title))
                setMessage(getString(R.string.out_memory_message))
                setPositiveButton("OK") { _, _ ->
                    finish()
                }
            }.create().show()
        }


    }

    private fun extractPermission() {
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            )
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                PERMISSION_EXTRCT
//            )
//        } else {
            getImages()
     //   }

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
        Log.d("nathanx", "progress " + event.message)
        this.runOnUiThread {
            progressbar.max = 100
            if (event.creatingFrame) {
                val framesLeft =
                    ((Utils.endTimeMilli - event.currentMilli).div(Utils.frameFrequencyMilli.toFloat())).toInt()

                val difference = totalFrames.toFloat() - framesLeft.toFloat()
                progressbar.progress =
                    (difference.toFloat().div(totalFrames.toFloat()) * 50).toInt()

                tvProgress.text = "Frames Left " + framesLeft
            } else if (event.addingFrames) {

                val difference = numberFrames - (numberFrames - event.currentMilli)
                progressbar.progress =
                    (difference.toFloat().div(numberFrames.toFloat()) * 50).toInt() + 50

                tvProgress.text = event.message
            } else {
                tvProgress.text = event.message
            }
            Log.d("nathanx", "get message " + tvProgress.text)
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