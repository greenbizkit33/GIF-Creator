package com.nathanhaze.gifcreator.activity

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.OnBackPressedCallback
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.event.FramesExtractedEvent
import com.nathanhaze.gifcreator.event.GifCreationEvent
import com.nathanhaze.gifcreator.event.ProgressUpdateEvent
import com.nathanhaze.gifcreator.manager.ImageUtil
import com.nathanhaze.gifcreator.manager.Utils
import com.nathanhaze.gifcreator.manager.Utils.isGettingImages
import com.nathanhaze.gifcreator.manager.VideoExportUtil
import com.nathanhaze.gifcreator.ui.FramePreviewAdapter
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
    private var extractedFrames: ArrayList<Bitmap> = arrayListOf()

    lateinit var progressbar: LinearProgressIndicator
    lateinit var gifImage: GifImageView
    lateinit var llSelection: LinearLayout
    lateinit var llFrameEditing: LinearLayout
    lateinit var rvFrameEditor: RecyclerView
    lateinit var btnShare: Button
    lateinit var btnStartOver: Button
    lateinit var btnExternalOpen: Button
    lateinit var btnSaveMp4: Button
    lateinit var btnEncodeSelected: Button
    lateinit var mAdView: AdView
    lateinit var mAdViewLoading: AdView
    lateinit var tvProgress: TextView
    lateinit var tvEta: TextView

    var stopThread = false
    var totalFrames: Int = 0
    private var extractionStartTime: Long = 0L

    lateinit var service: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gif_creator)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        progressbar = findViewById<View>(R.id.progress_circular) as LinearProgressIndicator
        gifImage = findViewById<View>(R.id.iv_gif) as GifImageView
        llSelection = findViewById(R.id.ll_selection)
        llFrameEditing = findViewById(R.id.ll_frame_editing)
        rvFrameEditor = findViewById(R.id.rv_frame_editor)
        btnShare = findViewById(R.id.button_share)
        btnStartOver = findViewById(R.id.button_start_over)
        btnExternalOpen = findViewById(R.id.button_open_external)
        btnSaveMp4 = findViewById(R.id.button_save_mp4)
        btnEncodeSelected = findViewById(R.id.btn_encode_selected)
        tvProgress = findViewById(R.id.tv_progress)
        tvEta = findViewById(R.id.tv_eta)

        btnExternalOpen.setOnClickListener {
            val photoURI = gifFile?.absoluteFile?.let {
                FileProvider.getUriForFile(applicationContext, applicationContext.packageName + ".GenericFileProvider", it)
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(photoURI, "image/*")
            }
            startActivity(intent)
        }

        btnShare.setOnClickListener { share() }

        btnStartOver.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Item 14: MP4 export
        btnSaveMp4.setOnClickListener {
            btnSaveMp4.isEnabled = false
            val path = Utils.getVideoPath(this) ?: return@setOnClickListener
            Executors.newSingleThreadExecutor().execute {
                val result = VideoExportUtil.trimToMp4(
                    this,
                    path,
                    Utils.startTimeMilli.toLong(),
                    Utils.endTimeMilli.toLong()
                )
                runOnUiThread {
                    btnSaveMp4.isEnabled = true
                    if (result != null) {
                        Utils.trackEvent(Bundle(), "mp4_exported", this)
                    }
                    Toast.makeText(
                        this,
                        if (result != null) getString(R.string.mp4_saved) else getString(R.string.mp4_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Item 6: Encode after frame editing
        btnEncodeSelected.setOnClickListener {
            llFrameEditing.visibility = View.GONE
            progressbar.visibility = View.VISIBLE
            tvProgress.visibility = View.VISIBLE
            encodeFrames(extractedFrames)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                stopThread = true
                isGettingImages = false
                extractedFrames.clear()
                finish()
            }
        })

        Utils.trackScreenView(this, "GIF Creator")
        stopThread = false
        extractPermission()

        mAdViewLoading = findViewById<View>(R.id.adView_loading) as AdView
        mAdView = findViewById<View>(R.id.adView) as AdView
    }

    override fun onResume() {
        super.onResume()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        if (Utils.lastGifFilePath != null && !isGettingImages) {
            if (Utils.outOfMemory) {
                showDialog()
            } else {
                Glide.with(this).asGif().load(Utils.lastGifFilePath).into(gifImage)
            }
            progressbar.visibility = View.GONE
            llSelection.visibility = View.VISIBLE
            tvProgress.visibility = View.GONE
            tvEta.visibility = View.GONE
        }
    }

    private fun getImages() {
        if (isGettingImages) {
            if (Utils.lastGifFilePath != null) {
                Glide.with(this).asGif().load(Utils.lastGifFilePath).into(gifImage)
            }
            return
        }

        val filePath = Utils.getVideoPath(this)
        progressbar.visibility = View.VISIBLE
        tvProgress.visibility = View.VISIBLE
        service = Executors.newSingleThreadExecutor()

        totalFrames = ((Utils.endTimeMilli - Utils.startTimeMilli)
            .div(Utils.frameFrequencyMilli.toFloat())).toInt()

        val msgStarting = getString(R.string.progress_starting)
        service.execute {
            isGettingImages = true
            extractionStartTime = System.currentTimeMillis()
            EventBus.getDefault().post(ProgressUpdateEvent(msgStarting, 0, false, false))

            val frameList = ArrayList<Bitmap>()
            val mediaRetriever = MediaMetadataRetriever()
            mediaRetriever.setDataSource(filePath)

            var currentMilli = Utils.startTimeMilli
            val endMilli = Utils.endTimeMilli

            while (currentMilli < endMilli && !stopThread) {
                EventBus.getDefault().post(ProgressUpdateEvent("", currentMilli, true, false))

                var bitmap = mediaRetriever.getFrameAtTime(
                    TimeUnit.MILLISECONDS.toMicros(currentMilli.toLong()),
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                // Scale
                bitmap = bitmap?.let {
                    Bitmap.createScaledBitmap(
                        it,
                        (it.width * Utils.size).toInt().coerceAtLeast(1),
                        (it.height * Utils.size).toInt().coerceAtLeast(1),
                        true
                    )
                }

                // Item 4: Crop to aspect ratio
                bitmap = bitmap?.let { cropToAspectRatio(it) }

                // Filter
                if (Utils.filter != null) {
                    bitmap = Utils.filter?.processFilter(bitmap)
                }

                // Item 7: Caption overlay
                if (bitmap != null && Utils.captionText.isNotBlank()) {
                    bitmap = drawCaption(bitmap, Utils.captionText)
                }

                bitmap?.let { frameList.add(it) }
                currentMilli += Utils.frameFrequencyMilli
            }

            if (!stopThread) {
                if (Utils.reverseOrder) frameList.reverse()
                if (Utils.double) {
                    val reversed = ArrayList(frameList).also { it.reverse() }
                    frameList.addAll(reversed)
                }
                extractedFrames = frameList
                isGettingImages = false
                // Item 6: post frames for review before encoding
                EventBus.getDefault().post(FramesExtractedEvent(frameList))
            } else {
                isGettingImages = false
            }
        }
    }

    // Item 6: Show frame editing UI after extraction
    @Subscribe
    fun onEvent(event: FramesExtractedEvent) {
        numberFrames = event.frames.size
        runOnUiThread {
            progressbar.visibility = View.GONE
            tvProgress.visibility = View.GONE
            tvEta.visibility = View.GONE

            // Set up the frame editor RecyclerView
            val adapter = FramePreviewAdapter(event.frames) { _ ->
                // Update encode button count label
                btnEncodeSelected.text = getString(R.string.encode_selected)
            }
            rvFrameEditor.layoutManager = GridLayoutManager(this, 3)
            rvFrameEditor.adapter = adapter
            llFrameEditing.visibility = View.VISIBLE
        }
    }

    // Called by btnEncodeSelected or directly if user skips editing
    private fun encodeFrames(frames: ArrayList<Bitmap>) {
        numberFrames = frames.size
        Executors.newSingleThreadExecutor().execute {
            ImageUtil.saveGif(frames, this)
        }
    }

    @Subscribe
    fun onEvent(event: GifCreationEvent) {
        isGettingImages = false
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return

        gifFile = event.filePath

        // Handle Analytics
        if (event?.error == false && event.filePath != null) {
            trackGifCreated()
        }

        runOnUiThread {
            if (Utils.outOfMemory) {
                showDialog()
            } else {
                Glide.with(this)
                    .asGif()
                    .load(event.filePath)
                    .into(gifImage)
            }

            updateUIVisibility(isFinished = true)
        }

        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    private fun trackGifCreated() {
        val bundle = Bundle().apply {
            putInt("frame_count", numberFrames)
            putString("filter", Utils.filter?.name ?: "none")
            putBoolean("has_caption", Utils.captionText.isNotBlank())
            putInt("loop_count", Utils.loopCount)
        }
        Utils.trackEvent(bundle, "gif_created", this)
    }

    private fun updateUIVisibility(isFinished: Boolean) {
        val visibility = if (isFinished) View.GONE else View.VISIBLE
        progressbar.visibility = visibility
        tvProgress.visibility = visibility
        tvEta.visibility = visibility
        llFrameEditing.visibility = visibility

        // Show selection layout when finished
        if (isFinished) llSelection.visibility = View.VISIBLE
    }

    @Subscribe
    fun onEvent(event: ProgressUpdateEvent) {
        this.runOnUiThread {
            progressbar.max = 100
            if (event.creatingFrame) {
                val framesLeft =
                    ((Utils.endTimeMilli - event.currentMilli)
                        .div(Utils.frameFrequencyMilli.toFloat())).toInt()
                val done = totalFrames.toFloat() - framesLeft.toFloat()
                progressbar.progress = (done / totalFrames.toFloat() * 50).toInt()
                tvProgress.text = getString(R.string.frames_left, framesLeft)

                // Item 3: ETA estimate
                val elapsed = System.currentTimeMillis() - extractionStartTime
                val progressFraction = done / totalFrames.toFloat()
                if (progressFraction > 0.05f) {
                    val estimatedTotal = elapsed / progressFraction
                    val remainingSec = ((estimatedTotal - elapsed) / 1000).toLong().coerceAtLeast(0)
                    tvEta.text = getString(R.string.time_remaining, "${remainingSec}s")
                    tvEta.visibility = View.VISIBLE
                }
            } else if (event.addingFrames) {
                val done = numberFrames - (numberFrames - event.currentMilli)
                progressbar.progress = (done.toFloat() / numberFrames.toFloat() * 50).toInt() + 50
                tvProgress.text = event.message
                tvEta.visibility = View.GONE
            } else {
                tvProgress.text = event.message
                tvEta.visibility = View.GONE
            }
        }
    }

    private fun showDialog() {
        runOnUiThread {
            AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.out_memory_title))
                setMessage(getString(R.string.out_memory_message))
                setPositiveButton(R.string.ok) { _, _ -> finish() }
            }.create().show()
        }
    }

    private fun extractPermission() {
        getImages()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_EXTRCT
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getImages()
        }
    }

    fun share() {
        if (gifFile == null) return
        Utils.trackEvent(Bundle(), "gif_shared", this)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "image/gif"
            putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(this@GifCreatorActivity, packageName + ".GenericFileProvider", gifFile!!)
            )
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
    }

    // ── Item 4: Crop bitmap to the selected aspect ratio ─────────────────────
    private fun cropToAspectRatio(src: Bitmap): Bitmap {
        val (targetW, targetH) = when (Utils.aspectRatioIndex) {
            1 -> 1 to 1
            2 -> 4 to 3
            3 -> 16 to 9
            else -> return src
        }
        val srcW = src.width
        val srcH = src.height
        val srcRatio = srcW.toFloat() / srcH
        val targetRatio = targetW.toFloat() / targetH
        return if (srcRatio > targetRatio) {
            val newW = (srcH * targetRatio).toInt()
            Bitmap.createBitmap(src, (srcW - newW) / 2, 0, newW, srcH)
        } else {
            val newH = (srcW / targetRatio).toInt()
            Bitmap.createBitmap(src, 0, (srcH - newH) / 2, srcW, newH)
        }
    }

    // ── Item 7: Draw caption text at the bottom of the frame ─────────────────
    private fun drawCaption(src: Bitmap, text: String): Bitmap {
        val result = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val textSize = (src.height * 0.07f).coerceIn(14f, 48f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
            textAlign = Paint.Align.CENTER
        }
        val x = src.width / 2f
        val y = src.height - textSize * 0.5f
        canvas.drawText(text, x, y, paint)
        return result
    }
}
