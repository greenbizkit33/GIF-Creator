package com.nathanhaze.gifcreator.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codemybrainsout.ratingdialog.RatingDialog
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.filter.BitmapFilters
import com.nathanhaze.gifcreator.manager.Utils
import com.nathanhaze.gifcreator.ui.FilterAdapter
import com.nathanhaze.gifcreator.ui.FramePreviewAdapter
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class SimpleSetupActivity : AppCompatActivity() {

    private lateinit var filterAdapter: FilterAdapter
    private var tvInfo: TextView? = null
    private var frequencyRange: Slider? = null
    private var rangeSlider: RangeSlider? = null
    private var ivWarning: ImageView? = null
    private var videoLengthMilli: Float = 0F
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    // Item 1: frame preview strip
    private var previewAdapter: FramePreviewAdapter? = null
    private var previewExecutor: ExecutorService? = null
    private val previewHandler = Handler(Looper.getMainLooper())
    private var previewRunnable: Runnable? = null

    // Item 9: prefs
    private val PREF_NAME = "setup_prefs"
    private val PREF_FREQ = "freq"
    private val PREF_SIZE = "size"
    private val PREF_SPEED = "speed"
    private val PREF_LOOP = "loop"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_simple_setup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val tvFreq = findViewById<TextView>(R.id.tv_freq)
        tvInfo = findViewById<TextView>(R.id.tv_info)
        frequencyRange = findViewById<Slider>(R.id.frame_rate)
        rangeSlider = findViewById<RangeSlider>(R.id.range_time)
        ivWarning = findViewById<ImageView>(R.id.iv_warning)
        val tvStart = findViewById<TextView>(R.id.tv_start_time)
        val tvEnd = findViewById<TextView>(R.id.tv_end_time)
        val rvFilter = findViewById<RecyclerView>(R.id.rv_filters)
        val sliderSize = findViewById<Slider>(R.id.s_size)
        val tvSize = findViewById<TextView>(R.id.tv_size)

        // ── Item 2: Playback speed ───────────────────────────────────────────
        val sliderSpeed = findViewById<Slider>(R.id.s_speed)
        val tvSpeed = findViewById<TextView>(R.id.tv_speed)
        val df = DecimalFormat("#.##").apply { roundingMode = RoundingMode.HALF_UP }

        fun speedFromSlider(v: Float) = v * 0.5f

        sliderSpeed.addOnChangeListener { _, value, _ ->
            val speed = speedFromSlider(value)
            Utils.playbackSpeed = speed
            tvSpeed.text = getString(R.string.playback_speed, df.format(speed))
            savePrefs()
        }

        // ── Item 8: Loop count ───────────────────────────────────────────────
        val sliderLoop = findViewById<Slider>(R.id.s_loop_count)
        val tvLoop = findViewById<TextView>(R.id.tv_loop_count)
        sliderLoop.addOnChangeListener { _, value, _ ->
            Utils.loopCount = value.toInt()
            tvLoop.text = if (value.toInt() == 0)
                getString(R.string.loop_count_infinite)
            else
                getString(R.string.loop_count_n, value.toInt().toString())
            savePrefs()
        }

        // ── Item 4: Aspect ratio ─────────────────────────────────────────────
        val chipGroup = findViewById<ChipGroup>(R.id.cg_aspect_ratio)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            Utils.aspectRatioIndex = when (checkedIds.firstOrNull()) {
                R.id.chip_ar_square -> 1
                R.id.chip_ar_four_three -> 2
                R.id.chip_ar_sixteen_nine -> 3
                else -> 0
            }
        }
        chipGroup.check(R.id.chip_ar_original)

        // ── Item 7: Caption ──────────────────────────────────────────────────
        val etCaption = findViewById<TextInputEditText>(R.id.et_caption)

        // ── Size slider ──────────────────────────────────────────────────────
        sliderSize?.addOnChangeListener { _, value, _ ->
            Utils.size = value / 100
            tvSize.text = getString(R.string.bitmap_size, df.format(value)) + "%"
            updateInfo()
            savePrefs()
        }

        // ── Frame frequency slider ───────────────────────────────────────────
        frequencyRange?.addOnChangeListener { _, value, _ ->
            tvFreq.text = getString(R.string.range_frequency, df.format(value / 1000))
            updateInfo()
            schedulePreviewUpdate()
            savePrefs()
        }

        frequencyRange?.setLabelFormatter { value ->
            df.format(value / 1000)
        }

        // ── Retrieve video metadata ──────────────────────────────────────────
        val mediaRetriever = MediaMetadataRetriever()
        try {
            mediaRetriever.setDataSource(Utils.getVideoPath(this))
        } catch (exception: Exception) {
            val bundle = Bundle()
            bundle.putString("path", Utils.getVideoPath(this)?.replace(" ", "_"))
            FirebaseAnalytics.getInstance(applicationContext).logEvent("error_source", bundle)
            Toast.makeText(applicationContext, getString(R.string.sorry_wrong), Toast.LENGTH_LONG).show()
            FirebaseCrashlytics.getInstance().recordException(exception)
        }

        val time = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        videoLengthMilli = time?.toFloat() ?: 0F
        videoWidth = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        videoHeight = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0

        // ── Time range slider ────────────────────────────────────────────────
        rangeSlider?.setLabelFormatter { value ->
            val milli = value.times(videoLengthMilli).toLong()
            String.format(
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milli),
                TimeUnit.MILLISECONDS.toMinutes(milli) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(milli) % TimeUnit.MINUTES.toSeconds(1)
            )
        }

        val formatTime = { milli: Long ->
            String.format(
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milli),
                TimeUnit.MILLISECONDS.toMinutes(milli) % 60,
                TimeUnit.MILLISECONDS.toSeconds(milli) % 60
            )
        }

        val updateTimesFromSlider = {
            val values = rangeSlider?.values
            if (values != null && values.size >= 2) {
                val startMilli = (values[0] * videoLengthMilli).toLong()
                val endMilli = (values[1] * videoLengthMilli).toLong()

                tvStart.text = formatTime(startMilli)
                tvEnd.text = formatTime(endMilli)

                Utils.startTimeMilli = startMilli.toInt()
                Utils.endTimeMilli = endMilli.toInt()

                updateInfo()
                schedulePreviewUpdate()
            }
        }

        rangeSlider?.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) {}
            override fun onStopTrackingTouch(slider: RangeSlider) = updateTimesFromSlider()
        })
        rangeSlider?.addOnChangeListener { _, _, fromUser ->
            if (!fromUser) updateTimesFromSlider()
        }

        Utils.startTimeMilli = 0
        Utils.endTimeMilli = (videoLengthMilli * 0.6f).toInt()
        rangeSlider?.setValues(0.4f, 0.6f)

        // ── Item 9: Restore saved presets ────────────────────────────────────
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        frequencyRange?.value = prefs.getFloat(PREF_FREQ, 70f).coerceIn(40f, 1000f)
        sliderSize?.value = prefs.getFloat(PREF_SIZE, 70f).coerceIn(10f, 100f)
        sliderSpeed.value = prefs.getFloat(PREF_SPEED, 4f).coerceIn(1f, 8f)
        sliderLoop.value = prefs.getFloat(PREF_LOOP, 0f).coerceIn(0f, 10f)

        // Sync labels after restoring prefs
        frequencyRange?.value?.let { tvFreq.text = getString(R.string.range_frequency, df.format(it / 1000)) }
        sliderSize?.value?.let { v ->
            Utils.size = v / 100
            tvSize.text = getString(R.string.bitmap_size, df.format(v)) + "%"
        }
        sliderSpeed.value.let { v ->
            Utils.playbackSpeed = speedFromSlider(v)
            tvSpeed.text = getString(R.string.playback_speed, df.format(speedFromSlider(v)))
        }
        sliderLoop.value.let { v ->
            Utils.loopCount = v.toInt()
            tvLoop.text = if (v.toInt() == 0)
                getString(R.string.loop_count_infinite)
            else
                getString(R.string.loop_count_n, v.toInt().toString())
        }

        // ── Filter strip (2s delay to avoid jank on load) ────────────────────
        var sample = mediaRetriever.getFrameAtTime(
            TimeUnit.SECONDS.toMicros(1000),
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
        sample = sample?.let { Bitmap.createScaledBitmap(it, 128, 128, true) }

        val filters = BitmapFilters.getFilterPack()
        filterAdapter = FilterAdapter(filters, sample, applicationContext)
        Handler(Looper.getMainLooper()).postDelayed({
            val layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.HORIZONTAL, false)
            rvFilter?.layoutManager = layoutManager
            rvFilter?.itemAnimator = DefaultItemAnimator()
            rvFilter?.adapter = filterAdapter
        }, 2000)

        // ── Item 1: Frame preview strip ──────────────────────────────────────
        val rvPreview = findViewById<RecyclerView>(R.id.rv_frame_preview)
        val tvPreviewLoading = findViewById<TextView>(R.id.tv_preview_loading)
        previewAdapter = FramePreviewAdapter(mutableListOf())
        rvPreview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPreview.adapter = previewAdapter
        schedulePreviewUpdate()

        // ── Reverse / double switches ────────────────────────────────────────
        val reverseSwitcher = findViewById<SwitchCompat>(R.id.switch_reverse)
        val doubleSwitcher = findViewById<SwitchCompat>(R.id.switch_double)
        reverseSwitcher?.setOnCheckedChangeListener { _, value ->
            Utils.reverseOrder = value
            if (value) doubleSwitcher.isChecked = false
            updateInfo()
        }
        doubleSwitcher?.setOnCheckedChangeListener { _, value ->
            Utils.double = value
            if (value) reverseSwitcher.isChecked = false
            updateInfo()
        }

        // ── Create GIF button ────────────────────────────────────────────────
        val createGifButton = findViewById<Button>(R.id.btn_create_gif)
        createGifButton?.setOnClickListener {
            Utils.frameFrequencyMilli = frequencyRange?.value?.toInt() ?: 70
            Utils.captionText = etCaption.text?.toString()?.trim() ?: ""
            val bundle = Bundle().apply {
                putInt("frame_freq_ms", Utils.frameFrequencyMilli)
                putInt("size_pct", (Utils.size * 100).toInt())
                putInt("duration_ms", Utils.endTimeMilli - Utils.startTimeMilli)
                putInt("loop_count", Utils.loopCount)
                putString("filter", Utils.filter?.name ?: "none")
                putInt("aspect_ratio", Utils.aspectRatioIndex)
                putBoolean("has_caption", Utils.captionText.isNotBlank())
            }
            Utils.trackEvent(bundle, "create_gif_tapped", this)
            startActivity(Intent(this, GifCreatorActivity::class.java))
        }

        updateInfo()
        Utils.trackScreenView(this, "Setup Screen")

        val ratingDialog: RatingDialog = RatingDialog.Builder(this).session(6).build()
        ratingDialog.show()

        val mAdView = findViewById<View>(R.id.adView) as AdView?
        val adRequest = AdRequest.Builder().build()
        mAdView?.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        Utils.outOfMemory = false
        Utils.lastGifFilePath = null
    }

    override fun onDestroy() {
        super.onDestroy()
        previewExecutor?.shutdownNow()
        previewRunnable?.let { previewHandler.removeCallbacks(it) }
    }

    // ── Item 1: Schedule debounced frame preview reload ──────────────────────
    private fun schedulePreviewUpdate() {
        previewRunnable?.let { previewHandler.removeCallbacks(it) }
        previewRunnable = Runnable { loadFramePreviews() }
        previewHandler.postDelayed(previewRunnable!!, 600)
    }

    private fun loadFramePreviews() {
        val path = Utils.getVideoPath(this) ?: return
        val start = Utils.startTimeMilli.toLong()
        val end = Utils.endTimeMilli.toLong()
        if (end <= start) return

        val rvPreview = findViewById<RecyclerView>(R.id.rv_frame_preview)
        val tvPreviewLoading = findViewById<TextView>(R.id.tv_preview_loading)
        tvPreviewLoading?.visibility = View.VISIBLE
        rvPreview?.visibility = View.GONE

        previewExecutor?.shutdownNow()
        previewExecutor = Executors.newSingleThreadExecutor()
        previewExecutor?.execute {
            val count = 5
            val interval = (end - start) / count
            val retriever = MediaMetadataRetriever()
            val thumbs = mutableListOf<Bitmap>()
            try {
                retriever.setDataSource(path)
                for (i in 0 until count) {
                    val timeUs = TimeUnit.MILLISECONDS.toMicros(start + interval * i)
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        ?.let { bmp ->
                            thumbs.add(Bitmap.createScaledBitmap(bmp, 160, 160, true))
                        }
                }
            } catch (_: Exception) {
            } finally {
                retriever.release()
            }
            previewHandler.post {
                if (thumbs.isNotEmpty()) {
                    previewAdapter?.updateFrames(thumbs)
                    rvPreview?.visibility = View.VISIBLE
                }
                tvPreviewLoading?.visibility = View.GONE
            }
        }
    }

    // ── Item 9: Save slider prefs ────────────────────────────────────────────
    private fun savePrefs() {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().apply {
            putFloat(PREF_FREQ, frequencyRange?.value ?: 70f)
            putFloat(PREF_SIZE, (Utils.size * 100))
            putFloat(PREF_SPEED, Utils.playbackSpeed / 0.5f)
            putFloat(PREF_LOOP, Utils.loopCount.toFloat())
            apply()
        }
    }

    // ── Item 5 + 13: Info text with size estimate and frame order arrows ──────
    fun updateInfo() {
        if (rangeSlider == null || rangeSlider?.values?.size!! < 2) return
        val freq = frequencyRange?.value ?: return
        val start = rangeSlider?.values!![0].times(videoLengthMilli).toLong()
        val end = rangeSlider?.values!![1].times(videoLengthMilli).toLong()
        val frames: Int = ((end - start) / freq).roundToInt()

        // Item 13: frame order indicator
        val orderArrow = when {
            Utils.reverseOrder -> getString(R.string.info_order_reverse)
            Utils.double -> getString(R.string.info_order_bounce)
            else -> getString(R.string.info_order_forward)
        }

        // Item 5: rough GIF size estimate
        val frameW = (videoWidth * Utils.size).toInt().coerceAtLeast(1)
        val frameH = (videoHeight * Utils.size).toInt().coerceAtLeast(1)
        val estBytes = frames.toLong() * frameW * frameH * 1L  // 1 byte/pixel for 8-bit GIF
        val compressionRatio = 0.4f
        val estKb = (estBytes * compressionRatio / 1024).toInt()
        val estStr = if (estKb > 1024) "%.1f MB".format(estKb / 1024f) else "${estKb} KB"

        val lengthSec = TimeUnit.MILLISECONDS.toSeconds(end - start)
        val sizeNote = if (videoWidth > 0) getString(R.string.info_est_size, estStr) else ""

        if (frames > 100) {
            ivWarning?.visibility = View.VISIBLE
            tvInfo?.text = getString(R.string.info_too_many_frames, frames, lengthSec, sizeNote, orderArrow)
        } else {
            ivWarning?.visibility = View.INVISIBLE
            tvInfo?.text = getString(R.string.info_frames, frames, lengthSec, sizeNote, orderArrow)
        }
    }
}
