package com.nathanhaze.gifcreator.activity

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.manager.Utils
import com.nathanhaze.gifcreator.ui.FilterAdapter
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import org.greenrobot.eventbus.EventBus
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class SimpleSetupActivity : AppCompatActivity() {

    private lateinit var filterAdapter: FilterAdapter

    private lateinit var tvInfo: TextView
    private lateinit var frequencyRange: Slider
    private lateinit var rangeSlider: RangeSlider
    private lateinit var ivWarning: ImageView
    private var videoLengthMilli: Float = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_simple_setup)

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

        Utils.size = 1f

        sliderSize.addOnChangeListener { slider, value, fromUser ->
            Utils.size = value / 100

            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            df.format(value).toString() + "%"

            tvSize.text = resources.getString(R.string.bitmap_size, df.format(value))

        }
        tvSize.text = resources.getString(R.string.bitmap_size, "100")

        sliderSize.setLabelFormatter(LabelFormatter { value ->
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            df.format(value).toString() + "%"
        })


        frequencyRange.addOnChangeListener { slider, value, fromUser ->
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            df.format(value).toString() + "%"

            tvFreq.text = resources.getString(R.string.range_frequency, df.format(value / 1000))
            updateInfo()
        }

        frequencyRange.setLabelFormatter(LabelFormatter { value ->
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            df.format(value / 1000).toString()
        })

        frequencyRange.value = 70F
        tvFreq.text =
            resources.getString(R.string.range_frequency, (frequencyRange.value / 1000).toString())


        val mediaRetriever: MediaMetadataRetriever = MediaMetadataRetriever()
        mediaRetriever.setDataSource(Utils.getVideoPath(this))

        val extractionType = MediaMetadataRetriever.OPTION_CLOSEST_SYNC

        val time: String? =
            mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        videoLengthMilli = time?.toFloat() ?: 0F

//        rangeSlider.valueFrom = 0F
//        if (videoLengthMilli != null) {
//          //  rangeSlider.valueTo = videoLengthMilli
//            rangeSlider.valueTo = 10F
//        }

        val start = String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours((videoLengthMilli.toLong() * 0.4f).toLong()),
            TimeUnit.MILLISECONDS.toMinutes(videoLengthMilli.toLong()) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(videoLengthMilli.toLong()) % TimeUnit.MINUTES.toSeconds(
                1
            )
        )

        val end = String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours((videoLengthMilli.toLong() * 0.6f).toLong()),
            TimeUnit.MILLISECONDS.toMinutes(videoLengthMilli.toLong()) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(videoLengthMilli.toLong()) % TimeUnit.MINUTES.toSeconds(
                1
            )
        )

//        tvStart.setText(start)
//        tvEnd.setText(end)

        rangeSlider.setLabelFormatter(LabelFormatter { value -> //It is just an example
            val milli = value.times(videoLengthMilli).toLong()

            String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(milli.toLong()),
                TimeUnit.MILLISECONDS.toMinutes(milli.toLong()) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(milli.toLong()) % TimeUnit.MINUTES.toSeconds(1)
            )
        })

        rangeSlider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) {

            }

            override fun onStopTrackingTouch(slider: RangeSlider) {
                val values = rangeSlider.values

                val startMilli = values[0].times(videoLengthMilli).toLong()
                val start = String.format(
                    "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(startMilli),
                    TimeUnit.MILLISECONDS.toMinutes(startMilli) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(startMilli) % TimeUnit.MINUTES.toSeconds(1)
                )

                val endMilli = values[1].times(videoLengthMilli).toLong()

                val end = String.format(
                    "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(endMilli),
                    TimeUnit.MILLISECONDS.toMinutes(endMilli) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(endMilli) % TimeUnit.MINUTES.toSeconds(1)
                )
                tvStart.setText(start)
                tvEnd.setText(end)

                Utils.startTimeMilli = startMilli.toInt()
                Utils.endTimeMilli = endMilli.toInt()
                updateInfo()
            }


        })


        rangeSlider.addOnChangeListener { slider, value, fromUser ->

            if (!fromUser) {
                val values = rangeSlider.values

                val startMilli = values[0].times(videoLengthMilli).toLong()
                val start = String.format(
                    "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(startMilli),
                    TimeUnit.MILLISECONDS.toMinutes(startMilli) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(startMilli) % TimeUnit.MINUTES.toSeconds(1)
                )

                val endMilli = values[1].times(videoLengthMilli).toLong()

                val end = String.format(
                    "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(endMilli),
                    TimeUnit.MILLISECONDS.toMinutes(endMilli) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(endMilli) % TimeUnit.MINUTES.toSeconds(1)
                )
                tvStart.setText(start)
                tvEnd.setText(end)

                Utils.startTimeMilli = startMilli.toInt()
                Utils.endTimeMilli = endMilli.toInt()
                updateInfo()
            }
        }

        Utils.startTimeMilli = 0
        Utils.endTimeMilli = (videoLengthMilli * 0.6f).toInt()

        rangeSlider.setValues(0.4f, 0.6f);

        var sample = mediaRetriever.getFrameAtTime(
            TimeUnit.SECONDS.toMicros(1000),
            extractionType
        )

        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            128F,
            resources.displayMetrics
        )

        val h = 128 // height in pixels

        val w = 128 // width in pixels

        sample = sample?.let { Bitmap.createScaledBitmap(it, h, w, true) }

        val filters: List<Filter> = FilterPack.getFilterPack(applicationContext)

        filterAdapter = FilterAdapter(filters, sample, applicationContext)

        Handler().postDelayed({
            val layoutManager =
                LinearLayoutManager(applicationContext, LinearLayoutManager.HORIZONTAL, false)
            rvFilter.layoutManager = layoutManager
            rvFilter.itemAnimator = DefaultItemAnimator()
            rvFilter.adapter = filterAdapter
        }, 2000)

        val reverseSwitcher = findViewById<SwitchCompat>(R.id.switch_reverse)
        val doubleSwitcher = findViewById<SwitchCompat>(R.id.switch_double)
        reverseSwitcher.setOnCheckedChangeListener { _, value ->
            Utils.reverseOrder = value
            if (value) {
                doubleSwitcher.isChecked = false;
            }
        }

        doubleSwitcher.setOnCheckedChangeListener { _, value ->
            Utils.double = value
            if (value) {
                reverseSwitcher.isChecked = false;
            }
        }
        val createGifButton = findViewById<Button>(R.id.btn_create_gif)

        createGifButton.setOnClickListener {
            Utils.frameFrequencyMilli = frequencyRange.value.toInt()
            startActivity(Intent(this, GifCreatorActivity::class.java))
        }
        updateInfo()

    }

    override fun onResume() {
        super.onResume()
        Utils.outOfMemory = false
    }

    fun updateInfo() {

        if (rangeSlider.values.size < 2) {
            return
        }
        val freq = frequencyRange.value
        val start = rangeSlider.values[0].times(videoLengthMilli).toLong()
        val end = rangeSlider.values[1].times(videoLengthMilli).toLong()

        val frames: Int = ((end - start) / freq).roundToInt()

        if (frames > 100) {
            ivWarning.visibility = View.VISIBLE
            tvInfo?.text =
                "Too many frame: " + frames.toString() + " Length: " + TimeUnit.MILLISECONDS.toSeconds(
                    end - start
                ) + " seconds"
        } else {
            tvInfo?.text =
                "Number of frames " + frames.toString() + " Length: " + TimeUnit.MILLISECONDS.toSeconds(
                    end - start
                ) + " seconds"
            ivWarning.visibility = View.INVISIBLE
        }

    }


//
//    class object {
//        {
//            System.loadLibrary("NativeImageProcessor")
//        }
//    }

}